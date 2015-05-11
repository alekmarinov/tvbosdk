/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureEPG.java
 * Author:      alek
 * Date:        1 Dec 2013
 * Description: Component feature providing EPG data
 */

package com.aviq.tv.android.sdk.feature.epg;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.system.FeatureTimeZone;
import com.aviq.tv.android.sdk.utils.Calendars;

/**
 * Component feature providing EPG data
 */
@Author("alek")
public abstract class FeatureEPG extends FeatureComponent
{
	public static final String TAG = FeatureEPG.class.getSimpleName();

	public static enum Param
	{
		/**
		 * The main url to the EPG server
		 */
		EPG_SERVER("http://avtv.intelibo.com"),

		/**
		 * The EPG service version
		 */
		EPG_VERSION(1),

		/**
		 * EPG channels url format
		 */
		EPG_CHANNELS_URL("${SERVER}/v${VERSION}/channels/${PROVIDER}"),

		/**
		 * EPG provider name
		 */
		EPG_PROVIDER("generic"),

		/**
		 * EPG channel logo url format
		 */
		EPG_CHANNEL_IMAGE_URL("${SERVER}/static/${PROVIDER}/epg/${CHANNEL}/${IMAGE}"),

		/**
		 * EPG programs url format
		 */
		EPG_PROGRAMS_URL("${SERVER}/v${VERSION}/programs/${PROVIDER}"),

		/**
		 * EPG program details url format
		 */
		EPG_PROGRAM_DETAILS_URL("${SERVER}/v${VERSION}/programs/${PROVIDER}/${CHANNEL}/${ID}"),

		/**
		 * Channel logo width
		 */
		CHANNEL_LOGO_WIDTH(80),

		/**
		 * Channel logo height
		 */
		CHANNEL_LOGO_HEIGHT(50),

		/**
		 * Schedule interval
		 */
		UPDATE_INTERVAL(24 * 60 * 60 * 1000),

		/**
		 * The number of maximum EPG channels
		 */
		MAX_CHANNELS(0),

		/**
		 * The number of days in past the program is allowed to be imported by
		 * EPG
		 */
		PROGRAM_RANGE_MIN_DAYS(7),

		/**
		 * The number of days in future the program is allowed to be imported by
		 * EPG
		 */
		PROGRAM_RANGE_MAX_DAYS(7),

		/**
		 * Time to live for the channel data before being invalidated
		 */
		CHANNELS_TTL(24 * 3600 * 1000);

		Param(boolean value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.EPG).put(name(), value);
		}

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.EPG).put(name(), value);
		}

		Param(String value)
		{
			Log.w(TAG, "put " + name() + " -> " + value);
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.EPG).put(name(), value);
		}
	}

	protected RequestQueue _requestQueue;
	private String _epgProvider;
	private int _epgVersion;
	private String _epgServer;
	protected int _channelLogoWidth;
	protected int _channelLogoHeight;
	private Calendar _minDate;
	private Calendar _maxDate;
	private FeatureTimeZone _featureTimeZone;
	private int _maxChannels = 0;
	private ChannelsResponse _channelsResponse;
	private ProgramsCache _programsCache = new ProgramsCache();
	private JsonObjectRequest _programDetailsRequest;

	private class ProgramsCache
	{
		private String _channelId;
		private Calendar _when;
		private int _offset;
		private int _count;
		private List<Program> _programs;

		List<Program> getPrograms(String channelId, Calendar when, int offset, int count)
		{
			if (_channelId == null || !_channelId.equals(channelId) || _offset != offset || _count != count)
			{
				Log.i(TAG, ".getPrograms: _channelId = " + _channelId + ", channelId = " + channelId + ", _offset = "
				        + _offset + ", offset = " + offset + ", _count= " + _count + ", count = " + count);
				return null;
			}

			if (_programs != null)
			{
				Log.i(TAG, ".getPrograms: _programs.size() = " + _programs.size() + ", count = " + count
				        + ", _offset = " + _offset);

				if (_programs.size() == count && _offset < 0 && -_offset < count)
				{
					Program currentProgram = _programs.get(-_offset);
					Log.i(TAG, ".getPrograms: currentProgram = " + currentProgram);
					if (currentProgram != null)
					{
						if (currentProgram.getStartTime().before(when) && currentProgram.getStopTime().after(when))
						{
							Log.i(TAG, ".getPrograms: RETURN " + _programs);
							return _programs;
						}
						else
						{
							Log.i(TAG,
							        ".getPrograms: getStartTime() = "
							                + Calendars.makeString(currentProgram.getStartTime())
							                + ", getStopTime() = " + Calendars.makeString(currentProgram.getStopTime())
							                + ", when = " + Calendars.makeString(when));
						}
					}
				}
			}
			else
			{
				Log.i(TAG, ".getPrograms: null programs");
			}
			return null;
		}

		void putPrograms(String channelId, Calendar when, int offset, int count, List<Program> programs)
		{
			Log.i(TAG, ".putPrograms: channelId = " + channelId + ", when = " + Calendars.makeString(when)
			        + ", offset = " + offset + ", count= " + count + ", programs = " + programs);
			_channelId = channelId;
			_when = when;
			_offset = offset;
			_count = count;
			_programs = programs;
		}
	}

	public FeatureEPG() throws FeatureNotFoundException
	{
		require(FeatureName.Scheduler.INTERNET);
		require(FeatureName.State.NETWORK_WIZARD);
		require(FeatureName.Component.TIMEZONE);
	}

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");

		_featureTimeZone = (FeatureTimeZone) Environment.getInstance().getFeatureComponent(
		        FeatureName.Component.TIMEZONE);
		_epgProvider = getPrefs().getString(Param.EPG_PROVIDER);
		_epgVersion = getPrefs().getInt(Param.EPG_VERSION);
		_epgServer = getPrefs().getString(Param.EPG_SERVER);
		_channelLogoWidth = getPrefs().getInt(Param.CHANNEL_LOGO_WIDTH);
		_channelLogoHeight = getPrefs().getInt(Param.CHANNEL_LOGO_HEIGHT);
		_requestQueue = Environment.getInstance().getRequestQueue();
		_maxChannels = getPrefs().getInt(Param.MAX_CHANNELS);
		super.initialize(onFeatureInitialized);
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.EPG;
	}

	/**
	 * Retrieve channels list
	 *
	 * @param onResultReceived
	 */
	public void getChannels(OnResultReceived onResultReceived)
	{
		if (_channelsResponse != null && !_channelsResponse.isExpired())
		{
			// return cached channels
			onResultReceived.onReceiveResult(FeatureError.OK(this), _channelsResponse.getChannels());
		}
		else
		{
			// retrieve new channels from server
			_channelsResponse = new ChannelsResponse(onResultReceived);
			JsonObjectRequest request = new JsonObjectRequest(getChannelsUrl(), null, _channelsResponse,
			        _channelsResponse);
			_requestQueue.add(request);
		}
	}

	public void getChannelById(final String channelId, final OnResultReceived onResultReceived)
	{
		if (_channelsResponse != null && !_channelsResponse.isExpired())
		{
			onResultReceived.onReceiveResult(FeatureError.OK(this), _channelsResponse.getChannelById(channelId));
		}
		else
		{
			getChannels(new OnResultReceived()
			{
				@Override
				public void onReceiveResult(FeatureError error, Object object)
				{
					if (!error.isError())
					{
						getChannelById(channelId, onResultReceived);
					}
					else
					{
						onResultReceived.onReceiveResult(error, null);
					}
				}
			});
		}
	}

	private class ChannelsResponse implements Response.Listener<JSONObject>, Response.ErrorListener
	{
		private List<Channel> _channels = new ArrayList<Channel>();
		private Map<String, Channel> _channelsMap = new HashMap<String, Channel>();
		private OnResultReceived _onResultReceived;
		private long _responseAge;

		ChannelsResponse(OnResultReceived onResultReceived)
		{
			_onResultReceived = onResultReceived;
		}

		List<Channel> getChannels()
		{
			return _channels;
		}

		Channel getChannelById(String channelId)
		{
			return _channelsMap.get(channelId);
		}

		boolean isExpired()
		{
			return System.currentTimeMillis() - _responseAge > getPrefs().getInt(Param.CHANNELS_TTL);
		}

		@Override
		public void onResponse(JSONObject response)
		{
			try
			{
				Channel.MetaData metaData = createChannelMetaData();
				JSONArray jsonArr = response.getJSONArray("meta");
				String[] meta = new String[jsonArr.length()];
				for (int i = 0; i < jsonArr.length(); i++)
					meta[i] = jsonArr.get(i).toString();
				indexChannelMetaData(metaData, meta);
				_channels.clear();
				_channelsMap.clear();
				parseChannelData(_channels, metaData, response.getJSONArray("data"));
				// hash channels by id
				for (Channel channel : _channels)
				{
					_channelsMap.put(channel.getChannelId(), channel);
				}
				_responseAge = System.currentTimeMillis();
				_onResultReceived.onReceiveResult(FeatureError.OK(FeatureEPG.this), _channels);
			}
			catch (JSONException e)
			{
				// Load channels failed, notify error
				Log.e(TAG, e.getMessage(), e);
				_onResultReceived
				        .onReceiveResult(new FeatureError(FeatureEPG.this, ResultCode.PROTOCOL_ERROR, e), null);
			}
		}

		@Override
		public void onErrorResponse(VolleyError error)
		{
			int statusCode = error.networkResponse != null ? error.networkResponse.statusCode
			        : ResultCode.GENERAL_FAILURE;
			Log.e(TAG, "Error retrieving EPG channels with code " + statusCode + ": " + error);
			_onResultReceived.onReceiveResult(new FeatureError(FeatureEPG.this, statusCode, error), null);
		}
	}

	public void getChannelLogoBitmap(Channel channel, int logoType, OnResultReceived onResultReceived)
	{
		String logoBase64 = channel.getLogo(logoType);
		byte[] decodedString = Base64.decode(logoBase64, Base64.DEFAULT);
		Bitmap logoBmp = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
		onResultReceived.onReceiveResult(FeatureError.OK(this), logoBmp);
	}

	public void getPrograms(Channel channel, Calendar when, int offset, int count, OnResultReceived onResultReceived)
	{
		// check cache
		String channelId = null;
		if (channel != null)
			channelId = channel.getChannelId();
		List<Program> cachedPrograms = _programsCache.getPrograms(channelId, when, offset, count);
		if (cachedPrograms != null)
		{
			// return programs from cache
			onResultReceived.onReceiveResult(FeatureError.OK(this), cachedPrograms);
			return;
		}

		// retrieve desired programs from server
		ProgramsResponse programsResponse = new ProgramsResponse(channelId, when, offset, count, onResultReceived);
		String programsUrl = getProgramsUrl(channelId, when, offset, count);
		Log.i(TAG, ".getPrograms: channel = " + channel + ", when = " + Calendars.makeString(when) + ", offset = "
		        + offset + ", count = " + count + " -> " + programsUrl);
		JsonObjectRequest request = new JsonObjectRequest(programsUrl, null, programsResponse, programsResponse);
		_requestQueue.add(request);
	}

	public void getPrograms(Calendar when, int offset, int count, OnResultReceived onResultReceived)
	{
		getPrograms(null, when, offset, count, onResultReceived);
	}

	public void getPrograms(Channel channel, OnResultReceived onResultReceived)
	{
		getPrograms(channel, null, 0, 0, onResultReceived);
	}

	private class ProgramsResponse implements Response.Listener<JSONObject>, Response.ErrorListener
	{
		private OnResultReceived _onResultReceived;
		private String _channelId;
		private Calendar _when;
		private int _offset;
		private int _count;

		ProgramsResponse(String channelId, Calendar when, int offset, int count, OnResultReceived onResultReceived)
		{
			_channelId = channelId;
			_when = when;
			_offset = offset;
			_count = count;
			_onResultReceived = onResultReceived;
		}

		@Override
		public void onResponse(JSONObject response)
		{
			try
			{
				Program.MetaData metaData = createProgramMetaData();
				JSONArray jsonArr = response.getJSONArray("meta");
				String[] meta = new String[jsonArr.length()];
				for (int i = 0; i < jsonArr.length(); i++)
					meta[i] = jsonArr.get(i).toString();
				indexProgramMetaData(metaData, meta);
				List<Program> programs = parseProgramData(metaData, _channelId, _when, _offset, _count,
				        response.getJSONArray("data"));
				_onResultReceived.onReceiveResult(FeatureError.OK(FeatureEPG.this), programs);

				// add programs to cache
				_programsCache.putPrograms(_channelId, _when, _offset, _count, programs);
			}
			catch (JSONException e)
			{
				// Load channels failed, notify error
				Log.e(TAG, e.getMessage(), e);
				_onResultReceived
				        .onReceiveResult(new FeatureError(FeatureEPG.this, ResultCode.PROTOCOL_ERROR, e), null);
			}
		}

		@Override
		public void onErrorResponse(VolleyError error)
		{
			int statusCode = error.networkResponse != null ? error.networkResponse.statusCode
			        : ResultCode.GENERAL_FAILURE;
			Log.e(TAG, "Error retrieving EPG channels with code " + statusCode + ": " + error);
			_onResultReceived.onReceiveResult(new FeatureError(FeatureEPG.this, statusCode, error), null);
		}
	}

	public void getEpgTimeRange(OnResultReceived onResultReceived)
	{
		if (_minDate == null || _maxDate == null)
			throw new IllegalStateException("Programs must be loaded prior obtaining epg time range");

		List<Calendar> timeFromTo = new ArrayList<Calendar>();
		timeFromTo.add(_minDate);
		timeFromTo.add(_maxDate);
		onResultReceived.onReceiveResult(FeatureError.OK(this), timeFromTo);
	}

	/**
	 * Return stream url by channel channel, play position and duration in
	 * seconds
	 *
	 * @param channel
	 *            the channel to obtain the stream from
	 * @param playTime
	 *            timestamp in seconds or 0 for live stream
	 * @param playDuration
	 *            stream duration in seconds
	 * @param onStreamURLReceived
	 *            callback interface where the stream will be returned
	 */
	public abstract void getStreamUrl(Channel channel, long playTime, long playDuration,
	        OnResultReceived onResultReceived);

	/**
	 * Return stream buffer size by channel
	 *
	 * @param channel
	 *            the channel to obtain stream buffer size from
	 * @return stream buffer size in seconds, or 0 if there is no buffer
	 */
	public abstract long getStreamBufferSize(Channel channel);

	/**
	 * Return URL to channel logo or program image
	 *
	 * @param channelId
	 *            the channel id
	 * @param imageName
	 *            the name of the image
	 * @return URL to the image
	 */
	public String getChannelImageUrl(String channelId, String imageName)
	{
		Bundle bundle = new Bundle();
		bundle.putString("SERVER", _epgServer);
		bundle.putString("CHANNEL", channelId);
		bundle.putString("PROVIDER", _epgProvider);
		bundle.putString("CHANNEL", channelId);
		bundle.putString("IMAGE", imageName);
		return getPrefs().getString(Param.EPG_CHANNEL_IMAGE_URL, bundle);
	}

	/**
	 * @return create channel instance
	 * @param index
	 *            is the channel position in the global Channels list
	 */
	protected abstract Channel createChannel(int index);

	/**
	 * Creates program instance associated with Channel and program Id
	 *
	 * @param channel
	 * @param id
	 * @return new program instance
	 */
	protected abstract Program createProgram(String id, Channel channel);

	protected Channel.MetaData createChannelMetaData()
	{
		return new Channel.MetaData();
	}

	protected Program.MetaData createProgramMetaData()
	{
		return new Program.MetaData();
	}

	protected void indexChannelMetaData(Channel.MetaData metaData, String[] meta)
	{
		for (int j = 0; j < meta.length; j++)
		{
			String key = meta[j];

			if ("id".equals(key))
				metaData.metaChannelId = j;
			else if ("title".equals(key))
				metaData.metaChannelTitle = j;
			else if ("thumbnail".equals(key))
				metaData.metaChannelLogo = j;
		}
	}

	private void parseChannelData(List<Channel> channels, Channel.MetaData metaData, JSONArray data)
	        throws JSONException
	{
		int nChannels = (_maxChannels > 0) ? _maxChannels : data.length();
		for (int i = 0; i < nChannels; i++)
		{
			JSONArray jsonArr = data.getJSONArray(i);
			String[] values = new String[jsonArr.length()];
			for (int j = 0; j < jsonArr.length(); j++)
			{
				if (jsonArr.get(j) != null)
				{
					if (!jsonArr.isNull(j))
						values[j] = jsonArr.get(j).toString();
				}
			}

			Channel channel = createChannel(i);
			channel.setChannelId(values[metaData.metaChannelId]);
			channel.setTitle(values[metaData.metaChannelTitle]);
			channel.setLogo(Channel.LOGO_NORMAL, values[metaData.metaChannelLogo]);
			channel.setAttributes(metaData, values);
			channels.add(channel);
		}
	}

	protected void indexProgramMetaData(Program.MetaData metaData, String[] meta)
	{
		for (int j = 0; j < meta.length; j++)
		{
			String key = meta[j];

			if ("channelid".equals(key))
				metaData.metaChannel = j;
			else if ("start".equals(key))
				metaData.metaStart = j;
			else if ("stop".equals(key))
				metaData.metaStop = j;
			else if ("title".equals(key))
				metaData.metaTitle = j;
		}
	}

	private List<Program> parseProgramData(Program.MetaData metaData, final String channelId, Calendar when,
	        int offset, int count, final JSONArray data) throws JSONException
	{
		long processStart = System.nanoTime();

		List<Program> programList = new ArrayList<Program>(data.length());
		if (_channelsResponse == null)
		{
			throw new IllegalStateException("Channels must be loaded prior programs");
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

		Calendar programRangeMin = Calendar.getInstance(_featureTimeZone.getTimeZone());
		Calendar programRangeMax = Calendar.getInstance(_featureTimeZone.getTimeZone());
		programRangeMin.add(Calendar.DATE, -getPrefs().getInt(Param.PROGRAM_RANGE_MIN_DAYS));
		programRangeMax.add(Calendar.DATE, getPrefs().getInt(Param.PROGRAM_RANGE_MAX_DAYS));

		int skippedPrograms = 0;
		for (int i = 0; i < data.length(); i++)
		{
			try
			{
				JSONArray jsonArr = data.getJSONArray(i);
				Calendar startTime = Calendar.getInstance();
				startTime.setTime(sdf.parse(jsonArr.getString(metaData.metaStart)));

				// skip programs outside the desired limit
				if (startTime.before(programRangeMin) || startTime.after(programRangeMax))
				{
					skippedPrograms++;
					continue;
				}

				if (_minDate != null && _minDate.after(startTime))
				{
					_minDate = startTime;
				}

				if (_maxDate != null && _maxDate.before(startTime))
				{
					_maxDate = startTime;
				}

				Calendar stopTime = Calendar.getInstance();
				stopTime.setTime(sdf.parse(jsonArr.getString(metaData.metaStop)));

				String id = new String(jsonArr.getString(metaData.metaStart));

				Channel channel;
				if (channelId != null)
					channel = _channelsResponse.getChannelById(channelId);
				else
					channel = _channelsResponse.getChannelById(data.getJSONArray(i).getString(metaData.metaChannel));

				Program program = createProgram(id, channel);
				program.setTitle(new String(jsonArr.getString(metaData.metaTitle)));
				program.setStartTime(startTime);
				program.setStopTime(stopTime);

				String[] values = new String[jsonArr.length()];
				for (int j = 0; j < jsonArr.length(); j++)
				{
					if (jsonArr.get(j) != null)
					{
						if (!jsonArr.isNull(j))
							values[j] = jsonArr.get(j).toString();
					}
				}

				// set custom provider attributes
				program.setDetailAttributes(metaData, values);
				programList.add(program);
			}
			catch (ParseException e)
			{
				Log.w(TAG, e.getMessage(), e);
			}
		}

		long processEnd = System.nanoTime();
		double processTime = (processEnd - processStart) / 1000000000.0;
		Log.d(TAG, "Parsed " + data.length() + " program items from channel " + channelId + " for " + processTime
		        + " sec, skipped = " + skippedPrograms);

		return programList;
	}

	protected String getChannelsUrl()
	{
		Bundle bundle = new Bundle();
		bundle.putString("SERVER", _epgServer);
		bundle.putInt("VERSION", _epgVersion);
		bundle.putString("PROVIDER", _epgProvider);

		return getPrefs().getString(Param.EPG_CHANNELS_URL, bundle);
	}

	protected String getProgramsUrl(String channelId, Calendar when, int offset, int count)
	{
		Bundle bundle = new Bundle();
		bundle.putString("SERVER", _epgServer);
		bundle.putInt("VERSION", _epgVersion);
		bundle.putString("PROVIDER", _epgProvider);

		StringBuffer sb = new StringBuffer(getPrefs().getString(Param.EPG_PROGRAMS_URL, bundle));
		if (channelId != null)
		{
			sb.append('/').append(channelId);
		}
		if (when != null)
		{
			sb.append("?when=");

			// format when and convert to GMT
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			sb.append(sdf.format(when.getTime()));

			sb.append("&offset=").append(offset);
			sb.append("&count=").append(count);
		}
		return sb.toString();
	}

	/**
	 * Fill program details in program object
	 *
	 * @param channelId
	 * @param program
	 * @param onProgramDetails
	 */
	public void getProgramDetails(Program program, OnResultReceived onResultReceived)
	{
		if (program.hasDetails())
		{
			onResultReceived.onReceiveResult(FeatureError.OK, program);
			return;
		}

		String programDetailsUrl = getProgramDetailsUrl(program.getChannel().getChannelId(), program.getId());
		Log.i(TAG, "Retrieving program details of " + program.getTitle() + ", id = " + program.getId() + " from "
		        + programDetailsUrl);

		ProgramDetailsResponseCallback responseCallback = new ProgramDetailsResponseCallback(program, onResultReceived);

		if (_programDetailsRequest != null)
			_programDetailsRequest.cancel();
		_programDetailsRequest = new JsonObjectRequest(Request.Method.GET, programDetailsUrl, null, responseCallback,
		        responseCallback)
		{
			@Override
			public Map<String, String> getHeaders() throws AuthFailureError
			{
				Map<String, String> headers = new HashMap<String, String>();
				headers.put("Connection", "close");
				return headers;
			}
		};

		// retrieves program details from the global request queue
		_requestQueue.add(_programDetailsRequest);
	}

	private String getProgramDetailsUrl(String channelId, String programId)
	{
		Bundle bundle = new Bundle();
		bundle.putString("SERVER", _epgServer);
		bundle.putInt("VERSION", _epgVersion);
		bundle.putString("PROVIDER", _epgProvider);
		bundle.putString("CHANNEL", channelId);
		bundle.putString("ID", programId);

		return getPrefs().getString(Param.EPG_PROGRAM_DETAILS_URL, bundle);
	}

	private class ProgramDetailsResponseCallback implements Response.Listener<JSONObject>, Response.ErrorListener
	{
		private OnResultReceived _onResultReceived;
		private Program _program;

		ProgramDetailsResponseCallback(Program program, OnResultReceived onResultReceived)
		{
			_program = program;
			_onResultReceived = onResultReceived;
		}

		@Override
		public void onResponse(JSONObject response)
		{
			_program.setDetails(response);
			_onResultReceived.onReceiveResult(FeatureError.OK, _program);
			_programDetailsRequest = null;
		}

		@Override
		public void onErrorResponse(VolleyError error)
		{
			_onResultReceived.onReceiveResult(new FeatureError(error), null);
			_programDetailsRequest = null;
		}
	}
}

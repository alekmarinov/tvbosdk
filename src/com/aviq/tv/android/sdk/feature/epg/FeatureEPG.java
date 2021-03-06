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

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.command.CommandHandler;
import com.aviq.tv.android.sdk.feature.system.FeatureTimeZone;
import com.aviq.tv.android.sdk.utils.Calendars;

/**
 * Component feature providing EPG data
 */
@Author("alek")
public abstract class FeatureEPG extends FeatureComponent
{
	public static final String TAG = FeatureEPG.class.getSimpleName();
	public static final int ON_CHANNELS_UPDATING = EventMessenger.ID("ON_CHANNELS_UPDATING");
	public static final int ON_CHANNELS_UPDATED = EventMessenger.ID("ON_CHANNELS_UPDATED");
	public static final int ON_CHANNELS_CHANGED = EventMessenger.ID("ON_CHANNELS_CHANGED");

	public static enum Command
	{
		GET_PROGRAMS, GET_PROGRAM_DETAILS
	}

	public static enum CommandGetProgramsExtras
	{
		CHANNEL_ID, DAY_OFFSET
	}

	public static enum CommandGetProgramDetailsExtras
	{
		CHANNEL_ID, PROGRAM_ID
	}

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
		PROGRAM_RANGE_MAX_DAYS(7);

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
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.EPG).put(name(), value);
		}
	}

	protected String _epgProvider;
	protected int _epgVersion;
	protected String _epgServer;
	protected int _channelLogoWidth;
	protected int _channelLogoHeight;
	private Calendar _minDate;
	private Calendar _maxDate;
	private FeatureTimeZone _featureTimeZone;
	private int _maxChannels = 0;
	private ProgramsCache _programsCache = new ProgramsCache();
	private JsonObjectRequest _programDetailsRequest;
	// List of all channels from the EPG provider
	protected List<Channel> _channels = new ArrayList<Channel>();
	// Maps channel id to channel object
	protected Map<String, Channel> _channelsMap = new HashMap<String, Channel>();
	private SimpleDateFormat _sdfUTC;

	private class ProgramsCache
	{
		private String _channelId;
		private int _offset;
		private int _count;
		private List<Program> _programs;

		void reset()
		{
			_channelId = null;
			_programs = null;
			_offset = _count = 0;
		}

		List<Program> getPrograms(String channelId, Calendar when, int offset, int count)
		{
			if (_channelId == null || !_channelId.equals(channelId) || _offset != offset || _count != count)
			{
				Log.i(TAG, "ProgramsCache.getPrograms: _channelId = " + _channelId + ", channelId = " + channelId
				        + ", _offset = " + _offset + ", offset = " + offset + ", _count= " + _count + ", count = "
				        + count);
				return null;
			}

			if (_programs != null)
			{
				Log.i(TAG, ".getPrograms: _programs.size() = " + _programs.size() + ", count = " + count
				        + ", _offset = " + _offset);

				if (_programs.size() == count && _offset < 0 && -_offset < count)
				{
					Program currentProgram = _programs.get(-_offset);
					if (currentProgram != null)
					{
						if (currentProgram.getStartTime().before(when) && currentProgram.getStopTime().after(when))
						{
							return _programs;
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

		void putPrograms(String channelId, int offset, int count, List<Program> programs)
		{
			Log.i(TAG, ".putPrograms: channelId = " + channelId + ", offset = " + offset + ", count= " + count
			        + ", programs = " + programs);
			_channelId = channelId;
			_offset = offset;
			_count = count;
			_programs = programs;
		}
	}

	public FeatureEPG() throws FeatureNotFoundException
	{
		require(FeatureName.Scheduler.INTERNET);
		require(FeatureName.Component.NETWORK_TIME);
		require(FeatureName.Component.TIMEZONE);
		require(FeatureName.Component.COMMAND);

		_sdfUTC = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
		_sdfUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
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
		_maxChannels = getPrefs().getInt(Param.MAX_CHANNELS);

		loadChannels(new OnResultReceived()
		{
			@Override
			public void onReceiveResult(FeatureError error, Object object)
			{
				onFeatureInitialized.onInitialized(error);
			}
		});

		registerCommands();

	} // initialize

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.EPG;
	}

	protected void registerCommands()
	{
		_feature.Component.COMMAND.addCommandHandler(new OnCommandGetPrograms());
		_feature.Component.COMMAND.addCommandHandler(new OnCommandGetProgramDetails());
	}

	/**
	 * Retrieve channels list
	 *
	 * @param onResultReceived
	 */
	public void loadChannels(OnResultReceived onResultReceived)
	{
		getEventMessenger().trigger(ON_CHANNELS_UPDATING);

		// retrieve new channels from server
		ChannelsResponse channelsResponse = new ChannelsResponse(onResultReceived);
		JsonObjectRequest request = new JsonObjectRequest(getChannelsUrl(), null, channelsResponse, channelsResponse);
		Environment.getInstance().getRequestQueue().add(request);
	}

	public List<Channel> getChannels()
	{
		return _channels;
	}

	public Channel getChannelById(String channelId)
	{
		return _channelsMap.get(channelId);
	}

	protected void addChannel(Channel channel)
	{
		Log.d(TAG, ".addChannel: " + channel.getChannelId());
		_channelsMap.put(channel.getChannelId(), channel);
		_channels.add(channel);
	}

	private class ChannelsResponse implements Response.Listener<JSONObject>, Response.ErrorListener
	{
		private OnResultReceived _onResultReceived;

		ChannelsResponse(OnResultReceived onResultReceived)
		{
			_onResultReceived = onResultReceived;
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
				parseChannelData(metaData, response.getJSONArray("data"));
				_onResultReceived.onReceiveResult(FeatureError.OK(FeatureEPG.this), null);
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
		// FIXME:
		// onResultReceived.onReceiveResult(FeatureError.OK(this),
		// getChannelLogoBitmap(channel, logoType));
		throw new RuntimeException("FIXME:");
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
		Environment.getInstance().getRequestQueue().add(request);
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
				_programsCache.putPrograms(_channelId, _offset, _count, programs);
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
			Log.e(TAG, "Error retrieving EPG programs with code " + statusCode + ": " + error);
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
	 * @return creates new channel
	 */
	protected abstract Channel createChannel();

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
			else if ("thumbnail_base64".equals(key))
				metaData.metaChannelLogo = j;
		}
	}

	private void parseChannelData(Channel.MetaData metaData, JSONArray data) throws JSONException
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

			Channel channel = createChannel();
			channel.setChannelId(values[metaData.metaChannelId]);
			channel.setTitle(values[metaData.metaChannelTitle]);

			if (values[metaData.metaChannelLogo] != null)
			{
				channel.setChannelImageBase64(Channel.LOGO_NORMAL, values[metaData.metaChannelLogo]);
				byte[] decodedString = Base64.decode(values[metaData.metaChannelLogo], Base64.DEFAULT);
				channel.setChannelImage(Channel.LOGO_NORMAL,
				        BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
			}
			try
			{
				channel.setAttributes(metaData, values);
			}
			catch (Exception e)
			{
				// invalid channel attributes, skip this channel
				Log.w(TAG, e.getMessage(), e);
			}

			// add channel must be called after channel object has assigned id
			addChannel(channel);
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
				startTime.setTime(_sdfUTC.parse(jsonArr.getString(metaData.metaStart)));

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
				stopTime.setTime(_sdfUTC.parse(jsonArr.getString(metaData.metaStop)));

				String id = new String(jsonArr.getString(metaData.metaStart));

				Channel channel;
				if (channelId != null)
					channel = getChannelById(channelId);
				else
					channel = getChannelById(data.getJSONArray(i).getString(metaData.metaChannel));

				if (channel == null)
				{
					// program channel is not available
					continue;
				}

				Program program = createProgram(id, channel);
				if (!jsonArr.isNull(metaData.metaTitle))
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
	 * Load detailed program
	 *
	 * @param channelId
	 * @param program
	 * @param onResultReceived
	 */
	public void getProgramDetails(String channelId, String programId, OnResultReceived onResultReceived)
	{
		String programDetailsUrl = getProgramDetailsUrl(channelId, programId);
		Log.i(TAG, "Retrieving program details of " + channelId + "/" + programId + " from " + programDetailsUrl);

		Channel channel = getChannelById(channelId);

		Log.i(TAG, "Channel created:" + channel);
		ProgramDetailsResponseCallback responseCallback = new ProgramDetailsResponseCallback(channel, programId,
		        onResultReceived);

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
		Log.i(TAG, " _programDetailsRequest:" + _programDetailsRequest.toString());

		// retrieves program details from the global request queue
		Environment.getInstance().getRequestQueue().add(_programDetailsRequest);
	}

	/**
	 * Load multiple detailed programs
	 *
	 * @param channelIds
	 *            String array with corresponding channel ids
	 * @param programIds
	 *            String array with corresponding program ids
	 * @param onResultReceived
	 */
	public void getMultiplePrograms(final List<String> channelIds, final List<String> programIds,
	        final OnResultReceived onResultReceived)
	{
		if (channelIds.size() != programIds.size())
			throw new IllegalArgumentException("Number of channel ids must be equal to the number of program ids");

		if (channelIds.size() == 0)
		{
			// return empty list with success status
			onResultReceived.onReceiveResult(FeatureError.OK(this), new ArrayList<Program>());
			return;
		}

		final int nPrograms = programIds.size();
		final Map<String, Program> programsMap = new HashMap<String, Program>();
		final int[] responseCount = new int[1];

		for (int i = 0; i < nPrograms; i++)
		{
			final String channelId = channelIds.get(i);
			final String programId = programIds.get(i);

			String programDetailsUrl = getProgramDetailsUrl(channelId, programId);
			Channel channel = getChannelById(channelId);
			ProgramDetailsResponseCallback responseCallback = new ProgramDetailsResponseCallback(channel, programId,
			        new OnResultReceived()
			        {
				        @Override
				        public void onReceiveResult(FeatureError error, Object object)
				        {
					        if (!error.isError())
					        {
						        programsMap.put(channelId + programId, (Program) object);
					        }
					        else
					        {
						        Log.e(TAG, error.getMessage(), error);
					        }
					        responseCount[0]++;
					        if (responseCount[0] == nPrograms)
					        {
						        List<Program> programs = new ArrayList<Program>();
						        for (int j = 0; j < nPrograms; j++)
						        {
							        programs.add(programsMap.get(channelIds.get(j) + programIds.get(j)));
						        }
						        onResultReceived.onReceiveResult(FeatureError.OK(FeatureEPG.this), programs);
					        }
				        }
			        });

			JsonObjectRequest programDetailsRequest = new JsonObjectRequest(Request.Method.GET, programDetailsUrl,
			        null, responseCallback, responseCallback)
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
			Environment.getInstance().getRequestQueue().add(programDetailsRequest);
		}
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
		private Channel _channel;
		String _programId;
		private OnResultReceived _onResultReceived;

		ProgramDetailsResponseCallback(Channel channel, String programId, OnResultReceived onResultReceived)
		{
			_channel = channel;
			_programId = programId;
			_onResultReceived = onResultReceived;
		}

		@Override
		public void onResponse(JSONObject response)
		{
			Program program = createProgram(_programId, _channel);
			try
			{
				program.setTitle(response.getString("title"));
				Calendar startTime = Calendar.getInstance();
				startTime.setTime(_sdfUTC.parse(_programId));
				program.setStartTime(startTime);
				Calendar stopTime = Calendar.getInstance();
				stopTime.setTime(_sdfUTC.parse(response.getString("stop")));
				program.setStopTime(stopTime);
				program.setDetails(response);
				_onResultReceived.onReceiveResult(FeatureError.OK, program);
			}
			catch (Exception e)
			{
				Log.e(TAG, e.getMessage(), e);
				_onResultReceived.onReceiveResult(new FeatureError(FeatureEPG.this, e), null);
			}
			_programDetailsRequest = null;
		}

		@Override
		public void onErrorResponse(VolleyError error)
		{
			int statusCode = error.networkResponse != null ? error.networkResponse.statusCode
			        : ResultCode.GENERAL_FAILURE;
			Log.e(TAG, "Error retrieving program details of " + _channel.getChannelId() + "/" + _programId + ": " + statusCode + ": " + error);
			_onResultReceived.onReceiveResult(new FeatureError(FeatureEPG.this, statusCode, error), null);
			_programDetailsRequest = null;
		}
	}

	// Command handlers

	protected class OnCommandGetPrograms implements CommandHandler
	{
		public OnCommandGetPrograms()
		{
		}

		@Override
		public String getId()
		{
			return Command.GET_PROGRAMS.name();
		}

		@Override
		public void execute(Bundle params, final OnResultReceived onResultReceived)
		{
			String channelId = params.getString(CommandGetProgramsExtras.CHANNEL_ID.name());
			String dayOffsetStr = params.getString(CommandGetProgramsExtras.DAY_OFFSET.name());

			final int dayOffset[] = new int[1];
			try
			{
				dayOffset[0] = Integer.parseInt(dayOffsetStr);
			}
			catch (NumberFormatException e)
			{
				Log.w(TAG, e.getMessage(), e);
			}
			Log.i(TAG, ".OnCommandGetPrograms.execute: channelId = " + channelId + ", dayOffset = " + dayOffset[0]);
			Channel channel = getChannelById(channelId);
			getPrograms(channel, new OnResultReceived()
			{
				private final String DATETIME_FORMAT = "yyyyMMddHHmmss";

				@Override
				public void onReceiveResult(FeatureError error, Object object)
				{
					if (error.isError())
					{
						onResultReceived.onReceiveResult(error, null);
					}
					else
					{
						@SuppressWarnings("unchecked")
						List<Program> programs = (List<Program>) object;
						Log.i(TAG, ".OnCommandGetPrograms.execute.onReceiveResult: programs count = " + programs.size());
						JSONArray jsonPrograms = new JSONArray();
						try
						{
							for (Program program : programs)
							{
								int programDayOffset = Calendars.getDayOffsetByDate(program.getStartTime());
								if (dayOffset[0] == programDayOffset)
								{
									JSONObject jsonProgram = new JSONObject();
									jsonProgram.put("id", program.getId());
									jsonProgram.put("start",
									        Calendars.makeString(program.getStartTime(), DATETIME_FORMAT));
									jsonProgram.put("stop",
									        Calendars.makeString(program.getStopTime(), DATETIME_FORMAT));
									jsonProgram.put("title", program.getTitle());
									jsonPrograms.put(jsonProgram);
								}
							}
							onResultReceived.onReceiveResult(FeatureError.OK(FeatureEPG.this), jsonPrograms);
						}
						catch (JSONException e)
						{
							onResultReceived.onReceiveResult(new FeatureError(FeatureEPG.this, e), null);
						}
					}
				}
			});
		}
	}

	protected class OnCommandGetProgramDetails implements CommandHandler
	{
		public OnCommandGetProgramDetails()
		{
		}

		@Override
		public void execute(Bundle params, final OnResultReceived onResultReceived)
		{
			String channelId = params.getString(CommandGetProgramDetailsExtras.CHANNEL_ID.name());
			String programId = params.getString(CommandGetProgramDetailsExtras.PROGRAM_ID.name());
			Log.i(TAG, ".OnCommandGetProgramDetails.execute: channelId = " + channelId + ", programId = " + programId);

			getProgramDetails(channelId, programId, new OnResultReceived()
			{
				@Override
				public void onReceiveResult(FeatureError error, Object object)
				{
					if (error.isError())
					{
						onResultReceived.onReceiveResult(error, null);
					}
					else
					{
						Program program = (Program) object;
						try
						{
							JSONObject jsonProgram = new JSONObject();
							jsonProgram.put("length", program.getLengthMin());
							jsonProgram.put("title", program.getTitle());
							String description = program.getDetailAttribute(ProgramAttribute.DESCRIPTION);
							if (description != null)
								jsonProgram.put("description", description);
							jsonProgram.put("image", program.getDetailAttribute(ProgramAttribute.IMAGE));

							onResultReceived.onReceiveResult(FeatureError.OK(FeatureEPG.this), jsonProgram);
						}
						catch (JSONException e)
						{
							onResultReceived.onReceiveResult(new FeatureError(FeatureEPG.this, e), null);
						}
					}
				}
			});
		}

		@Override
		public String getId()
		{
			return Command.GET_PROGRAM_DETAILS.name();
		}
	}
}

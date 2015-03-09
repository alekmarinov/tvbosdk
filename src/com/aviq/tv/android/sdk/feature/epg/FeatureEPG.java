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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TimeZone;
import java.util.TreeMap;

import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.Bundle;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.EventReceiver;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Scheduler;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.FeatureScheduler;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;
import com.aviq.tv.android.sdk.feature.system.FeatureStandBy;
import com.aviq.tv.android.sdk.feature.system.FeatureTimeZone;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Component feature providing EPG data
 */
@Author("alek")
public abstract class FeatureEPG extends FeatureScheduler
{
	public static final String TAG = FeatureEPG.class.getSimpleName();

	public static final int ON_EPG_UPDATED = EventMessenger.ID("ON_EPG_UPDATED");

	public enum Provider
	{
		rayv, wilmaa, bulsat, zattoo
	}

	public static enum Param
	{
		/**
		 * The main url to the EPG server
		 */
		EPG_SERVER("http://epg.aviq.bg"),

		/**
		 * The EPG service version
		 */
		EPG_VERSION(1),

		/**
		 * EPG channels url format
		 */
		EPG_CHANNELS_URL("${SERVER}/v${VERSION}/channels/${PROVIDER}"),

		/**
		 * EPG channel logo url format
		 */
		EPG_CHANNEL_IMAGE_URL("${SERVER}/static/${PROVIDER}/epg/${CHANNEL}/${IMAGE}"),

		/**
		 * EPG programs url format
		 */
		EPG_PROGRAMS_URL("${SERVER}/v${VERSION}/programs/${PROVIDER}/${CHANNEL}"),

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
		 * Enable/disable local epg cache
		 */
		USE_LOCAL_CACHE(false),

		/**
		 * epg cache file
		 */
		EPG_CACHE_PATH("cache" + File.separator + "epg.data"),

		/**
		 * epg cache expire time
		 */
		EPG_CACHE_EXPIRE(6 * 60 * 60 * 1000),

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
			Environment.getInstance().getFeaturePrefs(FeatureName.Scheduler.EPG).put(name(), value);
		}

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Scheduler.EPG).put(name(), value);
		}

		Param(String value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Scheduler.EPG).put(name(), value);
		}
	}

	public enum UserParam
	{
		/**
		 * Timestamp in milliseconds when the EPG cache file was created on.
		 */
		EPG_CACHE_CREATED_ON;
	}

	/**
	 * Callback interface invoked when program details are loaded
	 */
	public interface IOnProgramDetails
	{
		void onProgramDetails(FeatureError error, Program program);
	}

	protected RequestQueue _requestQueue;
	private OnFeatureInitialized _onFeatureInitialized;
	private int _epgVersion;
	private String _epgServer;
	private String _epgProvider;
	protected int _channelLogoWidth;
	protected int _channelLogoHeight;
	private Calendar _minDate;
	private Calendar _maxDate;

	// used to detect when all channel logos are retrieved with success or error
	private int _retrievedChannelLogos;

	// used to detect when all channel programs are retrieved with success or
	// error
	private int _retrievedChannelPrograms;

	protected EpgData _epgData;
	protected EpgData _epgDataBeingLoaded;
	private JsonObjectRequest _programDetailsRequest;
	private FeatureTimeZone _featureTimeZone;
	private int _maxChannels = 0;

	public interface OnStreamURLReceived
	{
		void onStreamURL(FeatureError error, String streamUrl);
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
		_epgProvider = getEPGProvider().name();
		_epgVersion = getPrefs().getInt(Param.EPG_VERSION);
		_epgServer = getPrefs().getString(Param.EPG_SERVER);
		_channelLogoWidth = getPrefs().getInt(Param.CHANNEL_LOGO_WIDTH);
		_channelLogoHeight = getPrefs().getInt(Param.CHANNEL_LOGO_HEIGHT);
		_requestQueue = Environment.getInstance().getRequestQueue();
		_maxChannels = getPrefs().getInt(Param.MAX_CHANNELS);

		// update epg on exiting standby
		// FIXME: ...
		FeatureStandBy featureStandBy = (FeatureStandBy)Environment.getInstance().getFeatureComponent(FeatureName.Component.STANDBY);
		if (featureStandBy != null)
		{
			featureStandBy.getEventMessenger().register(new EventReceiver()
			{
				@Override
				public void onEvent(int msgId, Bundle bundle)
				{
					getEventMessenger().trigger(ON_SCHEDULE);
				}
			}, FeatureStandBy.ON_STANDBY_LEAVE);
		}

		boolean loadedFromCache = uncacheEpgData();
		Log.i(TAG, "Is EPG loaded from cache: " + loadedFromCache);
		if (!loadedFromCache)
		{
			onSchedule(onFeatureInitialized);
		}
		else
		{
			// Download bitmaps
			int nChannels = _epgData.getChannelCount();
			_retrievedChannelPrograms = nChannels;
			for (int i = 0; i < nChannels; i++)
			{
				Channel channel = _epgData.getChannel(i);
				retrieveChannelLogo(channel, i);
			}

			_onFeatureInitialized = onFeatureInitialized;
			scheduleDelayed(getPrefs().getInt(Param.UPDATE_INTERVAL));
		}
	}

	@Override
	protected void onSchedule(OnFeatureInitialized onFeatureInitialized)
	{
		_onFeatureInitialized = onFeatureInitialized;

		// Update EPG data from server.
		String channelsUrl = getChannelsUrl();
		Log.i(TAG, "Retrieving EPG channels from " + channelsUrl);
		ChannelListResponseCallback responseCallback = new ChannelListResponseCallback();

		GsonRequest<ChannelListResponse> channelListRequest = new GsonRequest<ChannelListResponse>(Request.Method.GET,
		        channelsUrl, ChannelListResponse.class, responseCallback, responseCallback);

		resetMinMaxDates();

		_requestQueue.add(channelListRequest);

		// schedule update later
		scheduleDelayed(getPrefs().getInt(Param.UPDATE_INTERVAL));
	}

	public IEpgDataProvider getEpgData()
	{
		return _epgData;
	}

	/**
	 * Fill program details in program object
	 *
	 * @param channelId
	 * @param program
	 * @param onProgramDetails
	 */
	public void getProgramDetails(String channelId, Program program, IOnProgramDetails onProgramDetails)
	{
		if (program.hasDetails())
		{
			onProgramDetails.onProgramDetails(FeatureError.OK, program);
			return;
		}

		String programDetailsUrl = getProgramDetailsUrl(channelId, program.getId());
		Log.i(TAG, "Retrieving program details of " + program.getTitle() + ", id = " + program.getId() + " from "
		        + programDetailsUrl);
		ProgramDetailsResponseCallback responseCallback = new ProgramDetailsResponseCallback(program, onProgramDetails);

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

	@Override
	public Scheduler getSchedulerName()
	{
		return FeatureName.Scheduler.EPG;
	}

	/**
	 * @return the name of EPG provider implementation, e.g. rayv, wilmaa,
	 *         generic
	 */
	protected abstract Provider getEPGProvider();

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
	        OnStreamURLReceived onStreamURLReceived);

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
	 * @param channelId the channel id
	 * @param imageName the name of the image
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

	protected void retrieveChannelLogo(Channel channel, int channelIndex)
	{
		String channelId = channel.getChannelId();
		String channelLogo = channel.getThumbnail();

		String channelLogoUrl = getChannelImageUrl(channelId, channelLogo);
		Log.d(TAG, "Retrieving channel logo from " + channelLogoUrl);

		LogoResponseCallback responseCallback = new LogoResponseCallback(channelId, channelIndex);

		ImageRequest imageRequest = new ImageRequest(channelLogoUrl, responseCallback, _channelLogoWidth,
		        _channelLogoHeight, Config.ARGB_8888, responseCallback)
		{
			@Override
			public Map<String, String> getHeaders() throws AuthFailureError
			{
				Map<String, String> headers = new HashMap<String, String>();
				headers.put("Connection", "close");
				return headers;
			}
		};

		_requestQueue.add(imageRequest);
	}

	private void retrievePrograms(Channel channel)
	{
		String channelId = channel.getChannelId();
		String programsUrl = getProgramsUrl(channelId);
		Log.d(TAG, "Retrieving programs from " + programsUrl);

		ProgramsResponseCallback responseCallback = new ProgramsResponseCallback(channelId);

		GsonRequest<ProgramsResponse> programsRequest = new GsonRequest<ProgramsResponse>(Request.Method.GET,
		        programsUrl, ProgramsResponse.class, responseCallback, responseCallback);

		_requestQueue.add(programsRequest);
	}

	private class ChannelListResponseCallback implements Response.Listener<ChannelListResponse>, Response.ErrorListener
	{
		@Override
		public void onResponse(ChannelListResponse response)
		{
			Channel.MetaData metaData = createChannelMetaData();
			indexChannelMetaData(metaData, response.meta);
			parseChannelData(metaData, response.data);

			if (_epgDataBeingLoaded == null)
			{
				Log.w(TAG, "LogoResponseCallback.onResponse: _epgDataBeingLoaded is null, that should not happens!");
				return;
			}
			final int nChannels = _epgDataBeingLoaded.getChannelCount();
			Log.i(TAG, "Response with " + nChannels + " channels received");
			_retrievedChannelLogos = 0;
			_retrievedChannelPrograms = 0;

			for (int i = 0; i < nChannels; i++)
			{
				Channel channel = _epgDataBeingLoaded.getChannel(i);
				retrieveChannelLogo(channel, i);
				retrievePrograms(channel);
			}
		}

		@Override
		public void onErrorResponse(VolleyError error)
		{
			// FIXME: This error occurs when the EPG server is down. Show
			// appropriate message
			int statusCode = error.networkResponse != null ? error.networkResponse.statusCode
			        : ResultCode.GENERAL_FAILURE;
			Log.e(TAG, "Error retrieving channels with code " + statusCode + ": " + error);
			_onFeatureInitialized.onInitialized(new FeatureError(FeatureEPG.this, statusCode, error));
		}
	}

	private class LogoResponseCallback implements Response.Listener<Bitmap>, Response.ErrorListener
	{
		private int _index;
		private String _channelId;

		LogoResponseCallback(String channelId, int index)
		{
			_channelId = channelId;
			_index = index;
		}

		@Override
		public void onResponse(Bitmap response)
		{
			Log.d(TAG, "Received bitmap " + response.getWidth() + "x" + response.getHeight());
			if (_epgDataBeingLoaded == null)
			{
				Log.w(TAG, "LogoResponseCallback.onResponse: _epgDataBeingLoaded is null, that should not happens!");
				return;
			}
			_epgDataBeingLoaded.setChannelLogo(_index, response);
			logoProcessed();
		}

		@Override
		public void onErrorResponse(VolleyError error)
		{
			Log.d(TAG, "Retrieve channel logo " + _channelId + " with error: " + error);
			logoProcessed();
		}

		private void logoProcessed()
		{
			_retrievedChannelLogos++;
			checkInitializeFinished();
		}
	};

	private class ProgramsResponseCallback implements Response.Listener<ProgramsResponse>, Response.ErrorListener
	{
		private String _channelId;

		ProgramsResponseCallback(String channelId)
		{
			_channelId = channelId;
		}

		@Override
		public void onResponse(ProgramsResponse response)
		{
			Log.d(TAG, "Received programs for channel " + _channelId);
			Program.MetaData metaData = createProgramMetaData();
			indexProgramMetaData(metaData, response.meta);
			parseProgramsData(metaData, _channelId, response.data);
			programsProcessed();
		}

		@Override
		public void onErrorResponse(VolleyError error)
		{
			Log.w(TAG, "Error " + error + " retrieving programs for " + _channelId);
			programsProcessed();
		}

		private void programsProcessed()
		{
			_retrievedChannelPrograms++;
			checkInitializeFinished();
		}
	}

	private class ProgramDetailsResponseCallback implements Response.Listener<JSONObject>, Response.ErrorListener
	{
		private IOnProgramDetails _onProgramDetails;
		private Program _program;

		ProgramDetailsResponseCallback(Program program, IOnProgramDetails onProgramDetails)
		{
			_program = program;
			_onProgramDetails = onProgramDetails;
		}

		@Override
		public void onResponse(JSONObject response)
		{
			_program.setDetails(response);
			_onProgramDetails.onProgramDetails(FeatureError.OK, _program);
			_programDetailsRequest = null;
		}

		@Override
		public void onErrorResponse(VolleyError error)
		{
			_onProgramDetails.onProgramDetails(new FeatureError(error), null);
			_programDetailsRequest = null;
		}
	}

	private void checkInitializeFinished()
	{
		if (_epgDataBeingLoaded == null)
		{
			Log.w(TAG, ".checkInitializeFinished: _epgDataBeingLoaded is null, that should not happen!");
			return;
		}

		int numChannels = _epgDataBeingLoaded.getChannelCount();
		final int processedCount = _retrievedChannelPrograms + _retrievedChannelLogos;
		Log.i(TAG, ".checkInitializeFinished: processedCount = " + processedCount + ", numChannels = " + numChannels);

		if (_retrievedChannelPrograms == numChannels && _retrievedChannelLogos == numChannels)
		{
			// Forget the old EpgData object, from now on work with the new
			// one. Anyone else holding a reference to the old object will
			// be able to finish its job. Then the garbage collector will
			// free up the memory.

			_epgData = _epgDataBeingLoaded;
			_epgDataBeingLoaded = null;
			_retrievedChannelPrograms = 0;
			_retrievedChannelLogos = 0;

			if (getPrefs().getBool(Param.USE_LOCAL_CACHE))
			{
				new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						cacheEpgData();
					}
				}).start();
			}
			getEventMessenger().trigger(ON_EPG_UPDATED);

			// _minDate and _maxDate will be null when the EPG
			if (_minDate != null && _maxDate != null)
			{
				SimpleDateFormat ddf = new SimpleDateFormat("yyyyMMdd HH:mm", Locale.getDefault());
				Log.d(TAG,
				        "EPG programs in range " + ddf.format(_minDate.getTime()) + " - "
				                + ddf.format(_maxDate.getTime()));
				resetMinMaxDates();
			}
			super.initialize(_onFeatureInitialized);
		}
		else
		{
			final float totalCount = 2 * numChannels; // The number of all
			                                          // programs
			// and logos queries
			getEventMessenger().post(new Runnable()
			{
				@Override
				public void run()
				{
					_onFeatureInitialized.onInitializeProgress(FeatureEPG.this, processedCount / totalCount);
				}
			});
		}
	}

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
				metaData.metaChannelThumbnail = j;
		}
	}

	private void parseChannelData(Channel.MetaData metaData, String[][] data)
	{
		List<Channel> newChannelList = new ArrayList<Channel>();
		if (_maxChannels == 0)
			_maxChannels = data.length;

		for (int i = 0; i < _maxChannels; i++)
		{
			Channel channel = createChannel(i);
			channel.setChannelId(new String(data[i][metaData.metaChannelId]));
			channel.setTitle(new String(data[i][metaData.metaChannelTitle]));
			channel.setThumbnail(new String(data[i][metaData.metaChannelThumbnail]));
			channel.setAttributes(metaData, data[i]);
			newChannelList.add(channel);
		}

		_epgDataBeingLoaded = new EpgData(newChannelList);
	}

	protected void indexProgramMetaData(Program.MetaData metaData, String[] meta)
	{
		for (int j = 0; j < meta.length; j++)
		{
			String key = meta[j];

			if ("start".equals(key))
				metaData.metaStart = j;
			else if ("stop".equals(key))
				metaData.metaStop = j;
			else if ("title".equals(key))
				metaData.metaTitle = j;
		}
	}

	private void parseProgramsData(Program.MetaData metaData, final String channelId, final String[][] data)
	{
		long processStart = System.nanoTime();

		NavigableMap<Calendar, Integer> programMap = new TreeMap<Calendar, Integer>();
		List<Program> programList = new ArrayList<Program>(data.length);
		Channel channel = _epgDataBeingLoaded.getChannel(channelId);

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

		Calendar programRangeMin = Calendar.getInstance(_featureTimeZone.getTimeZone());
		Calendar programRangeMax = Calendar.getInstance(_featureTimeZone.getTimeZone());
		programRangeMin.add(Calendar.DATE, -getPrefs().getInt(Param.PROGRAM_RANGE_MIN_DAYS));
		programRangeMax.add(Calendar.DATE, getPrefs().getInt(Param.PROGRAM_RANGE_MAX_DAYS));

		for (int i = 0; i < data.length; i++)
		{
			try
			{
				Calendar startTime = Calendar.getInstance();
				startTime.setTime(sdf.parse(data[i][metaData.metaStart]));

				if (startTime.before(programRangeMin) || startTime.after(programRangeMax))
				{
					continue;
				}

				if (_minDate.after(startTime))
				{
					_minDate = startTime;
				}

				if (_maxDate.before(startTime))
				{
					_maxDate = startTime;
				}

				Calendar stopTime = Calendar.getInstance();
				stopTime.setTime(sdf.parse(data[i][metaData.metaStop]));

				String id = new String(data[i][metaData.metaStart]);
				Program program = createProgram(id, channel);
				program.setTitle(new String(data[i][metaData.metaTitle]));
				program.setStartTime(startTime);
				program.setStopTime(stopTime);

				// set custom provider attributes
				program.setDetailAttributes(metaData, data[i]);
				programList.add(program);
				program.setIndex(programList.size() - 1);
				programMap.put(program.getStartTime(), program.getIndex());
			}
			catch (ParseException e)
			{
				Log.w(TAG, e.getMessage(), e);
			}
		}

		_epgDataBeingLoaded.addProgramData(channelId, programMap, programList);

		long processEnd = System.nanoTime();
		double processTime = (processEnd - processStart) / 1000000000.0;
		Log.d(TAG, "Parsed " + data.length + " program items for channel " + channelId + " for " + processTime + " sec");
	}

	protected String getChannelsUrl()
	{
		Bundle bundle = new Bundle();
		bundle.putString("SERVER", _epgServer);
		bundle.putInt("VERSION", _epgVersion);
		bundle.putString("PROVIDER", _epgProvider);

		return getPrefs().getString(Param.EPG_CHANNELS_URL, bundle);
	}

	protected String getProgramsUrl(String channelId)
	{
		Bundle bundle = new Bundle();
		bundle.putString("SERVER", _epgServer);
		bundle.putInt("VERSION", _epgVersion);
		bundle.putString("PROVIDER", _epgProvider);
		bundle.putString("CHANNEL", channelId);

		return getPrefs().getString(Param.EPG_PROGRAMS_URL, bundle);
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

	private File getEpgDataCacheFile()
	{
		String path = Environment.getInstance().getFilesDir() + File.separator
		        + getPrefs().getString(Param.EPG_CACHE_PATH);
		File cacheFile = new File(path);
		if (!cacheFile.getParentFile().exists())
			cacheFile.getParentFile().mkdirs();
		return cacheFile;
	}

	protected void cacheEpgData()
	{
		Log.i(TAG, ".cacheEpgData");

		// Load serialized cached data
		boolean success = serializeData();
		if (!success)
		{
			Log.w(TAG, "EPG data could not be cached.");
			return;
		}

		// Save time of serialization to user settings
		Prefs userPrefs = Environment.getInstance().getUserPrefs();
		userPrefs.put(UserParam.EPG_CACHE_CREATED_ON, System.currentTimeMillis());
	}

	protected boolean uncacheEpgData()
	{
		Log.i(TAG, ".uncacheEpgData");

		Prefs userPrefs = Environment.getInstance().getUserPrefs();
		if (userPrefs.has(UserParam.EPG_CACHE_CREATED_ON))
		{
			long cacheCreatedOn = userPrefs.getLong(UserParam.EPG_CACHE_CREATED_ON);
			int cacheTimeElapsed = (int) (System.currentTimeMillis() - cacheCreatedOn);
			int epgCacheExpire = getPrefs().getInt(Param.EPG_CACHE_EXPIRE);
			Log.i(TAG, "EPG cache time elapsed " + (cacheTimeElapsed / 1000) + "s, EPG_CACHE_EXPIRE = " + (epgCacheExpire / 1000) + "s");
			if (cacheTimeElapsed > epgCacheExpire)
			{
				Log.i(TAG, "EPG cache expired");
				return false;
			}
		}

		return deserializeData();
	}

	private boolean serializeData()
	{
		Log.i(TAG, ".serializeData");

		if (_epgData == null)
			return false;

		Kryo kryo = new Kryo();
		try
		{
			File cacheFile = getEpgDataCacheFile();
			Output output = new Output(new FileOutputStream(cacheFile));
			kryo.writeObject(output, _epgData);
			output.close();
		}
		catch (FileNotFoundException e)
		{
			Log.e(TAG, "Cannot create EPG data cache file.", e);
			return false;
		}
		catch (Exception e)
		{
			Log.e(TAG, "Cannot create EPG data cache file.", e);
			return false;
		}
		return true;
	}

	private boolean deserializeData()
	{
		Log.i(TAG, ".deserializeData");

		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try
		{
			File cacheFile = getEpgDataCacheFile();
			if (!cacheFile.exists())
			{
				Log.i(TAG, "No EPG cached data exists. Nothing to do.");
				return false;
			}

			Kryo kryo = new Kryo();
			Input input = new Input(new FileInputStream(cacheFile));
			_epgData = kryo.readObject(input, EpgData.class);
			_epgDataBeingLoaded = _epgData;
			input.close();
		}
		catch (FileNotFoundException e)
		{
			Log.e(TAG, "Cannot open EPG data cache file.", e);
			return false;
		}
		catch (Exception e)
		{
			Log.e(TAG, "Cannot open EPG data cache file.", e);
			return false;
		}
		finally
		{
			try
			{
				if (fis != null)
					fis.close();
			}
			catch (IOException e)
			{
				Log.e(TAG, "Cannot close file output stream.", e);
			}

			try
			{
				if (ois != null)
					ois.close();
			}
			catch (IOException e)
			{
				Log.e(TAG, "Cannot close object output stream.", e);
			}
		}
		return true;
	}

	// GSON entity class of channel list response
	private class ChannelListResponse
	{
		public String[] meta;
		public String[][] data;
	}

	// GSON entity class of programs response
	private class ProgramsResponse
	{
		public String[] meta;
		public String[][] data;
	}

	// GSON volley request
	private class GsonRequest<T> extends Request<T>
	{
		private final Gson mGson;
		private final Class<T> mClazz;
		private final Listener<T> mListener;

		public GsonRequest(int method, String url, Class<T> clazz, Listener<T> listener, ErrorListener errorListener)
		{
			super(Method.GET, url, errorListener);
			this.mClazz = clazz;
			this.mListener = listener;
			mGson = new Gson();
		}

		public GsonRequest(int method, String url, Class<T> clazz, Listener<T> listener, ErrorListener errorListener,
		        Gson gson)
		{
			super(Method.GET, url, errorListener);
			this.mClazz = clazz;
			this.mListener = listener;
			mGson = gson;
		}

		@Override
		public Map<String, String> getHeaders() throws AuthFailureError
		{
			Map<String, String> headers = new HashMap<String, String>();
			headers.put("Connection", "close");
			return headers;
		}

		@Override
		protected void deliverResponse(T response)
		{
			mListener.onResponse(response);
		}

		@Override
		protected Response<T> parseNetworkResponse(NetworkResponse response)
		{
			try
			{
				String json = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
				return Response.success(mGson.fromJson(json, mClazz), HttpHeaderParser.parseCacheHeaders(response));
			}
			catch (UnsupportedEncodingException e)
			{
				return Response.error(new ParseError(e));
			}
			catch (JsonSyntaxException e)
			{
				return Response.error(new ParseError(e));
			}
		}
	}

	private void resetMinMaxDates()
	{
		_minDate = Calendar.getInstance();
		_minDate.setTimeInMillis(Long.MAX_VALUE);
		_maxDate = Calendar.getInstance();
		_maxDate.setTimeInMillis(Long.MIN_VALUE);
	}
}

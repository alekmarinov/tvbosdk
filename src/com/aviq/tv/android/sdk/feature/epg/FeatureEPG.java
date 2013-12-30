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

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.util.Log;

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
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Component feature providing EPG data
 */
public abstract class FeatureEPG extends FeatureComponent
{
	public static final String TAG = FeatureEPG.class.getSimpleName();

	public enum Provider
	{
		rayv, wilmaa
	}

	public enum Param
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
		EPG_CHANNEL_LOGO_URL("${SERVER}/static/${PROVIDER}/${CHANNEL}/${LOGO}"),

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
		CHANNEL_LOGO_HEIGHT(50);

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.EPG).put(name(), value);
		}

		Param(String value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.EPG).put(name(), value);
		}
	}

	/**
	 * Callback interface invoked when program details are loaded
	 */
	public interface IOnProgramDetails
	{
		void onProgramDetails(Program program);

		void onError(int resultCode);
	}

	private RequestQueue _httpQueue;
	private OnFeatureInitialized _onFeatureInitialized;
	private int _epgVersion;
	private String _epgServer;
	private String _epgProvider;
	private int _channelLogoWidth;
	private int _channelLogoHeight;

	// used to detect when all channel logos are retrieved with success or error
	private int _retrievedChannelLogos;

	// used to detect when all channel programs are retrieved with success or
	// error
	private int _retrievedChannelPrograms;

	private EpgData _epgData;
	private EpgData _epgDataBeingLoaded;

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");

		_onFeatureInitialized = onFeatureInitialized;

		_epgProvider = getEPGProvider().name();
		_epgVersion = getPrefs().getInt(Param.EPG_VERSION);
		_epgServer = getPrefs().getString(Param.EPG_SERVER);
		_channelLogoWidth = getPrefs().getInt(Param.CHANNEL_LOGO_WIDTH);
		_channelLogoHeight = getPrefs().getInt(Param.CHANNEL_LOGO_HEIGHT);

		_httpQueue = Environment.getInstance().getRequestQueue();

		retrieveChannels();
	}

	public EpgData getEpgData()
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
			onProgramDetails.onProgramDetails(program);
			return;
		}

		String programDetailsUrl = getProgramDetailsUrl(channelId, program.getId());
		Log.i(TAG, "Retrieving program details of " + program.getTitle() + ", id = " + program.getId() + " from "
		        + programDetailsUrl);
		ProgramDetailsResponseCallback responseCallback = new ProgramDetailsResponseCallback(program, onProgramDetails);
		JsonObjectRequest programDetailsRequest = new JsonObjectRequest(Request.Method.GET, programDetailsUrl, null,
		        responseCallback, responseCallback);
		_httpQueue.add(programDetailsRequest);
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.EPG;
	}

	/**
	 * @return the name of EPG provider implementation, e.g. rayv, wilmaa,
	 *         generic
	 */
	protected abstract Provider getEPGProvider();

	/**
	 * @param channelIndex
	 * @return URL to video stream corresponding to the requested channel index
	 */
	public abstract String getChannelStreamUrl(int channelIndex);

	/**
	 * @return create channel instance
	 *
	 * @param index is the channel position in the global Channels list
	 */
	protected abstract Channel createChannel(int index);

	/**
	 * @return create program instance
	 */
	protected abstract Program createProgram(Channel channel);

	private void retrieveChannels()
	{
		String channelsUrl = getChannelsUrl();
		Log.i(TAG, "Retrieving EPG channels from " + channelsUrl);
		ChannelListResponseCallback responseCallback = new ChannelListResponseCallback();

		GsonRequest<ChannelListResponse> channelListRequest = new GsonRequest<ChannelListResponse>(Request.Method.GET,
		        channelsUrl, ChannelListResponse.class, responseCallback, responseCallback);

		_httpQueue.add(channelListRequest);
	}

	private void retrieveChannelLogo(Channel channel, int channelIndex)
	{
		String channelId = channel.getChannelId();
		String channelLogo = channel.getThumbnail();

		String channelLogoUrl = getChannelsLogoUrl(channelId, channelLogo);
		Log.d(TAG, "Retrieving channel logo from " + channelLogoUrl);

		LogoResponseCallback responseCallback = new LogoResponseCallback(channelId, channelIndex);

		ImageRequest imageRequest = new ImageRequest(channelLogoUrl, responseCallback, _channelLogoWidth,
		        _channelLogoHeight, Config.ARGB_8888, responseCallback);

		_httpQueue.add(imageRequest);
	}

	private void retrievePrograms(Channel channel)
	{
		String channelId = channel.getChannelId();
		String programsUrl = getProgramsUrl(channelId);
		Log.d(TAG, "Retrieving programs from " + programsUrl);

		ProgramsResponseCallback responseCallback = new ProgramsResponseCallback(channelId);

		GsonRequest<ProgramsResponse> programsRequest = new GsonRequest<ProgramsResponse>(Request.Method.GET,
		        programsUrl, ProgramsResponse.class, responseCallback, responseCallback);

		_httpQueue.add(programsRequest);
	}

	private class ChannelListResponseCallback implements Response.Listener<ChannelListResponse>, Response.ErrorListener
	{
		@Override
		public void onResponse(ChannelListResponse response)
		{
			Channel.MetaData metaData = createChannelMetaData();
			indexChannelMetaData(metaData, response.meta);
			parseChannelData(metaData, response.data);

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
			int statusCode = error.networkResponse != null ? error.networkResponse.statusCode
			        : ResultCode.GENERAL_FAILURE;
			Log.e(TAG, "Error retrieving channels with code " + statusCode + ": " + error);
			_onFeatureInitialized.onInitialized(FeatureEPG.this, statusCode);
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
			_onProgramDetails.onProgramDetails(_program);
		}

		@Override
		public void onErrorResponse(VolleyError error)
		{
			int resultCode = ResultCode.GENERAL_FAILURE;
			if (error.networkResponse != null)
				resultCode = error.networkResponse.statusCode;
			_onProgramDetails.onError(resultCode);
		}
	}

	private void checkInitializeFinished()
	{
		int numChannels = _epgDataBeingLoaded.getChannelCount();
		if (_retrievedChannelPrograms == numChannels && _retrievedChannelLogos == numChannels)
		{
			// Forget the old EpgData object, from now on work with the new
			// one. Anyone else holding a reference to the old object will
			// be able to finish its job. Then the garbage collector will
			// free up the memory.

			_epgData = _epgDataBeingLoaded;
			// TODO: Uncomment this if "parseProgramData()" is going to work
			// without the AsyncTask logic
			_epgDataBeingLoaded = null;
			_retrievedChannelPrograms = 0;
			_retrievedChannelLogos = 0;
			_onFeatureInitialized.onInitialized(FeatureEPG.this, ResultCode.OK);
		}
		else
		{
			float processedCount = _retrievedChannelPrograms + _retrievedChannelLogos;
			float totalCount = 2 * numChannels; // The number of all programs and logos queries
			_onFeatureInitialized.onInitializeProgress(FeatureEPG.this, processedCount / totalCount);
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
		for (int i = 0; i < data.length; i++)
		{
			Channel channel = createChannel(i);
			channel.setChannelId(data[i][metaData.metaChannelId]);
			channel.setTitle(data[i][metaData.metaChannelTitle]);
			channel.setThumbnail(data[i][metaData.metaChannelThumbnail]);
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

		NavigableMap<String, Integer> programMap = new TreeMap<String, Integer>();
		List<Program> programList = new ArrayList<Program>();
		Channel channel = _epgDataBeingLoaded.getChannel(channelId);

		for (int i = 0; i < data.length; i++)
		{
			Program program = createProgram(channel);
			program.setTitle(data[i][metaData.metaTitle]);

			try
			{
				program.setStartTime(data[i][metaData.metaStart]);
			}
			catch (ParseException e)
			{
				Log.w(TAG, "Undefined start time for program: " + program.getTitle() + " on channel: " + channelId);
			}

			try
			{
				program.setStopTime(data[i][metaData.metaStop]);
			}
			catch (ParseException e)
			{
				Log.w(TAG, "Undefined stop time for program: " + program.getTitle() + " on channel: " + channelId);
			}

			// set custom provider attributes
			program.setDetailAttributes(metaData, data[i]);
			programList.add(program);
			programMap.put(program.getStartTime(), i);
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

	private String getChannelsLogoUrl(String channelId, String channelLogo)
	{
		Bundle bundle = new Bundle();
		bundle.putString("SERVER", _epgServer);
		bundle.putString("CHANNEL", channelId);
		bundle.putString("PROVIDER", _epgProvider);
		bundle.putString("CHANNEL", channelId);
		bundle.putString("LOGO", channelLogo);

		return getPrefs().getString(Param.EPG_CHANNEL_LOGO_URL, bundle);
	}

	private String getProgramsUrl(String channelId)
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
}

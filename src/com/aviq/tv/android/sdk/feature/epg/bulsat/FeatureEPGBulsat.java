/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureEPGBulsat.java
 * Author:      alek
 * Date:        1 Dec 2013
 * Description: Bulsat specific extension of EPG feature
 */

package com.aviq.tv.android.sdk.feature.epg.bulsat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.command.CommandHandler;
import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.epg.FeatureEPG;
import com.aviq.tv.android.sdk.feature.epg.Program;
import com.aviq.tv.android.sdk.feature.epg.ProgramAttribute;
import com.aviq.tv.android.sdk.feature.epg.bulsat.ProgramBulsat.ImageSize;
import com.aviq.tv.android.sdk.feature.internet.FeatureInternet;
import com.aviq.tv.android.sdk.feature.recording.FeatureRecordingScheduler;
import com.aviq.tv.android.sdk.utils.Calendars;

/**
 * Bulsat specific extension of EPG feature
 */
@Author("alek")
public class FeatureEPGBulsat extends FeatureEPG
{
	public static final String TAG = FeatureEPGBulsat.class.getSimpleName();
	private static final int DEFAULT_STREAM_PLAY_DURATION = 3600;

	private enum TransportFormat
	{
		XML, JSON
	}

	public static enum Param
	{
		/**
		 * Which trasport format to use for updating EPG data - XML or JSON
		 */
		TRANSPORT_FORMAT(TransportFormat.JSON.name()),

		/**
		 * Direct url to Bulsatcom channels in XML format
		 */
		BULSAT_CHANNELS_URL_XML("http://api.iptv.bulsat.com/?xml&tv"),

		/**
		 * Direct url to Bulsatcom channels in JSON format
		 */
		BULSAT_CHANNELS_URL_JSON("https://api.iptv.bulsat.com/tv/full/limit"),

		/**
		 * Direct url to Bulsatcom genres in XML format
		 */
		BULSAT_GENRES_URL_XML("http://api.iptv.bulsat.com/?xml&chantypes"),

		/**
		 * Direct url to Bulsatcom genres in JSON format
		 */
		BULSAT_GENRES_URL_JSON("https://api.iptv.bulsat.com/chantypes/links"),

		/**
		 * Update interval for updating channel streams directly from bulsat
		 * server and refresh channels list if changed
		 */
		CHANNELS_UPDATE_INTERVAL(2 * 3600 * 1000),

		/**
		 * EPG provider name
		 */
		EPG_PROVIDER("bulsat"),

		/**
		 * Maximum update try attempts
		 */
		UPDATE_ATTEMPTS_MAX(10);

		Param(long value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.EPG).put(name(), value);
		}

		Param(String value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.EPG).put(name(), value);
		}
	}

	// hack to trigger reference to Param enum and force java to initialize it
	public static Param ParamIniter = Param.EPG_PROVIDER;

	private List<Bitmap> _programImages = null;
	private UpdateInterface _updateChannels;
	private UpdateInterface _updateGenres;

	public FeatureEPGBulsat() throws FeatureNotFoundException
	{
		super();
	}

	@Override
	protected void registerCommands()
	{
		_feature.Component.COMMAND.addCommandHandler(new OnCommandGetPrograms());
		_feature.Component.COMMAND.addCommandHandler(new OnCommandGetProgramBulsatDetails());
	}

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		_feature.Scheduler.INTERNET.getEventMessenger().register(this, FeatureInternet.ON_EXTERNAL_IP_CHANGED);

		if (TransportFormat.XML.name().equals(getPrefs().getString(Param.TRANSPORT_FORMAT)))
		{
			_updateChannels = new UpdateChannelsXML();
		}
		else
		{
			_updateChannels = new UpdateChannelsJSON();
		}

		if (TransportFormat.XML.name().equals(getPrefs().getString(Param.TRANSPORT_FORMAT)))
		{
			_updateGenres = new UpdateGenresXML();
		}
		else
		{
			_updateGenres = new UpdateGenresJSON();
			// updateGenres = new UpdateGenresJSONWithHttpClient();
		}
		super.initialize(onFeatureInitialized);

		/*
		 * final long maxAttempts =
		 * getPrefs().getLong(Param.UPDATE_ATTEMPTS_MAX);
		 * tryUpdate(new OnFeatureInitialized()
		 * {
		 * int _attempts = 0;
		 * @Override
		 * public void onInitialized(FeatureError error)
		 * {
		 * if (error.isError() && _attempts < maxAttempts)
		 * {
		 * _attempts++;
		 * final OnFeatureInitialized _this = this;
		 * getEventMessenger().postDelayed(new Runnable()
		 * {
		 * @Override
		 * public void run()
		 * {
		 * tryUpdate(_this);
		 * }
		 * }, 1000);
		 * }
		 * else
		 * {
		 * onFeatureInitialized.onInitialized(error);
		 * }
		 * }
		 * @Override
		 * public void onInitializeProgress(IFeature feature, float progress)
		 * {
		 * onFeatureInitialized.onInitializeProgress(feature, progress);
		 * }
		 * });
		 */
	}

	@Override
	public void loadChannels(final OnResultReceived onResultReceived)
	{
		Log.i(TAG, ".loadChannels");

		// load channel genres
		_updateGenres.update(new OnResultReceived()
		{
			@Override
			public void onReceiveResult(FeatureError error, Object object)
			{
				Log.i(TAG, ".loadChannels: _updateGenres received: " + error);

				if (error.isError())
				{
					onResultReceived.onReceiveResult(error, null);
				}
				else
				{
					Genres genres = (Genres) object;
					Genres.getInstance().clear();
					Genres.getInstance().addAll(genres);

					_updateChannels.update(onResultReceived);

					// update channel streams periodically
					// directly from the bulsat server
					final long updateInterval = getPrefs().getLong(Param.CHANNELS_UPDATE_INTERVAL);
					getEventMessenger().postDelayed(new Runnable()
					{
						@Override
						public void run()
						{
							_updateGenres.update(new OnResultReceived()
							{
								@Override
								public void onReceiveResult(FeatureError error, Object object)
								{
									Log.i(TAG, "Genres periodically updated: " + error);
									if (!error.isError())
									{
										_updateChannels.update(new OnResultReceived()
										{
											@SuppressWarnings("unchecked")
											@Override
											public void onReceiveResult(FeatureError error, Object object)
											{
												Log.i(TAG, "Channels periodically updated: " + error);
											}
										});
									}
								}
							});
							getEventMessenger().postDelayed(this, updateInterval);
						}
					}, updateInterval);
				}
			}
		});
	}

	@Override
	public void onEvent(final int msgId, Bundle bundle)
	{
		super.onEvent(msgId, bundle);
		if (FeatureInternet.ON_EXTERNAL_IP_CHANGED == msgId)
		{
			Log.i(TAG, "Updating channel streams on changed external IP");

			// refresh channel streams
			_updateChannels.update(new OnResultReceived()
			{
				@Override
				public void onReceiveResult(FeatureError error, Object object)
				{
					Log.i(TAG, "Channel streams updated caused by IP change with status: " + error);
				}
			});
		}
	}

	@Override
	protected Channel.MetaData createChannelMetaData()
	{
		return new ChannelBulsat.MetaData();
	}

	@Override
	protected void indexChannelMetaData(Channel.MetaData metaData, String[] meta)
	{
		ChannelBulsat.MetaData bulsatMetaData = (ChannelBulsat.MetaData) metaData;
		super.indexChannelMetaData(bulsatMetaData, meta);

		for (int j = 0; j < meta.length; j++)
		{
			String key = meta[j];
			if ("channel".equals(key))
				bulsatMetaData.metaChannelChannelNo = j;
			else if ("genre".equals(key))
				bulsatMetaData.metaChannelGenre = j;
			else if ("ndvr".equals(key))
				bulsatMetaData.metaChannelNdvr = j;
			else if ("streams.1.url".equals(key))
				bulsatMetaData.metaChannelStreamUrl = j;
			else if ("streams.2.url".equals(key))
				bulsatMetaData.metaChannelSeekUrl = j;
			else if ("pg".equals(key))
				bulsatMetaData.metaChannelPG = j;
			else if ("recordable".equals(key))
				bulsatMetaData.metaChannelRecordable = j;
			else if ("radio".equals(key))
				bulsatMetaData.metaChannelRadio = j;
			else if ("thumbnail_base64".equals(key))
				bulsatMetaData.metaChannelLogo = j;
			else if ("thumbnail_selected_base64".equals(key))
				bulsatMetaData.metaChannelLogoSelected = j;
			else if ("thumbnail_favorite_base64".equals(key))
				bulsatMetaData.metaChannelLogoFavorite = j;
			else if ("program_image_medium".equals(key))
				bulsatMetaData.metaChannelProgramImageMedium = j;
			else if ("program_image_large".equals(key))
				bulsatMetaData.metaChannelProgramImageLarge = j;
		}
	}

	@Override
	protected String getProgramsUrl(String channelId, Calendar when, int offset, int count)
	{
		String baseUrl = super.getProgramsUrl(channelId, when, offset, count);
		StringBuffer url = new StringBuffer(baseUrl);
		if (baseUrl.indexOf('?') > 0)
			url.append('&');
		else
			url.append('?');
		url.append("attr=description,image_medium,image_large");
		return url.toString();
	}

	@Override
	protected Channel createChannel()
	{
		return new ChannelBulsat(getChannels().size());
	}

	@Override
	protected String getChannelsUrl()
	{
		String url = super.getChannelsUrl();
		return url
		        + "?attr=channel,genre,ndvr,streams.1.url,streams.2.url,pg,recordable,radio,thumbnail_base64,thumbnail_selected_base64,thumbnail_favorite_base64,program_image_medium,program_image_large";
	}

	@Override
	protected Program createProgram(String id, Channel channel)
	{
		return new ProgramBulsat(id, channel);
	}

	@Override
	protected Program.MetaData createProgramMetaData()
	{
		return new ProgramBulsat.MetaData();
	}

	@Override
	protected void indexProgramMetaData(Program.MetaData metaData, String[] meta)
	{
		ProgramBulsat.MetaData bulsatMetaData = (ProgramBulsat.MetaData) metaData;
		super.indexProgramMetaData(metaData, meta);

		for (int j = 0; j < meta.length; j++)
		{
			String key = meta[j];
			if ("description".equals(key))
				bulsatMetaData.metaDescription = j;
			else if ("image_medium".equals(key))
				bulsatMetaData.metaImageMedium = j;
			else if ("image_large".equals(key))
				bulsatMetaData.metaImageLarge = j;
		}
	}

	/**
	 * Return stream url by channel index and delta from real time in seconds
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
	@Override
	public void getStreamUrl(Channel channel, long playTime, long playDuration, OnResultReceived onResultReceived)
	{
		ChannelBulsat channelBulsat = (ChannelBulsat) channel;
		long playTimeDelta = System.currentTimeMillis() / 1000 - playTime;
		String streamUrl;
		if (playTime > 0 && playTimeDelta > 0 && channelBulsat.getSeekUrl() != null)
		{
			Calendar startTime = Calendar.getInstance();
			startTime.setTimeInMillis(1000 * playTime);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			String startTimeFormat = sdf.format(startTime.getTime());
			String seekUrl = channelBulsat.getSeekUrl();
			if (seekUrl.indexOf('?') > 0)
				seekUrl += '&';
			else
				seekUrl += '?';

			if (playDuration == 0)
				playDuration = DEFAULT_STREAM_PLAY_DURATION;
			seekUrl += "wowzadvrplayliststart=" + startTimeFormat + "&wowzadvrplaylistduration=" + playDuration * 1000;

			// set seek url
			streamUrl = seekUrl;
		}
		else
		{
			// set live url
			streamUrl = channelBulsat.getStreamUrl();
		}
		Log.d(TAG, ".getStreamUrl: channel = " + channel.getChannelId() + ", playTime = " + playTime
		        + ", playDuration = " + playDuration + " -> " + streamUrl);
		onResultReceived.onReceiveResult(FeatureError.OK, streamUrl);
	}

	@Override
	public long getStreamBufferSize(Channel channel)
	{
		ChannelBulsat channelBulsat = (ChannelBulsat) channel;
		return channelBulsat.getNDVR();
	}

	private String getProgramImagesUrl()
	{
		Bundle bundle = new Bundle();
		bundle.putString("SERVER", _epgServer);
		bundle.putInt("VERSION", _epgVersion);
		bundle.putString("PROVIDER", _epgProvider);

		StringBuffer sb = new StringBuffer(getPrefs().getString(FeatureEPG.Param.EPG_CHANNELS_URL, bundle));
		sb.append("?attr=program_image_medium_base64");
		return sb.toString();
	}

	public void getProgramImages(OnResultReceived onResultReceived)
	{
		if (_programImages != null)
		{
			onResultReceived.onReceiveResult(FeatureError.OK(FeatureEPGBulsat.this), _programImages);
		}
		else
		{
			String programImagesUrl = getProgramImagesUrl();
			Log.i(TAG, ".getProgramImages: programImagesUrl = " + programImagesUrl);
			// retrieve program images from server
			ProgramImagesResponse programImagesResponse = new ProgramImagesResponse(onResultReceived);
			JsonObjectRequest request = new JsonObjectRequest(programImagesUrl, null, programImagesResponse,
			        programImagesResponse);
			Environment.getInstance().getRequestQueue().add(request);
		}
	}

	private class ProgramImagesResponse implements Response.Listener<JSONObject>, Response.ErrorListener
	{
		private OnResultReceived _onResultReceived;

		ProgramImagesResponse(OnResultReceived onResultReceived)
		{
			_onResultReceived = onResultReceived;
		}

		@Override
		public void onResponse(JSONObject response)
		{
			try
			{
				JSONArray meta = response.getJSONArray("meta");
				int programImageIndex = -1;
				for (int i = 0; i < meta.length(); i++)
				{
					if ("program_image_medium_base64".equals(meta.getString(i)))
					{
						programImageIndex = i;
						break;
					}
				}
				if (programImageIndex >= 0)
				{
					JSONArray data = response.getJSONArray("data");
					_programImages = new ArrayList<Bitmap>();
					for (int i = 0; i < data.length(); i++)
					{
						JSONArray jsonArr = data.getJSONArray(i);
						try
						{
							byte[] decodedString = Base64.decode(jsonArr.getString(programImageIndex), Base64.DEFAULT);
							_programImages.add(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
						}
						catch (Exception e)
						{
							_programImages.add(null);
							Log.w(TAG, e.getMessage(), e);
						}
					}
				}
				_onResultReceived.onReceiveResult(FeatureError.OK(FeatureEPGBulsat.this), _programImages);
			}
			catch (JSONException e)
			{
				// Load channels failed, notify error
				Log.e(TAG, e.getMessage(), e);
				_onResultReceived.onReceiveResult(
				        new FeatureError(FeatureEPGBulsat.this, ResultCode.PROTOCOL_ERROR, e), null);
			}
		}

		@Override
		public void onErrorResponse(VolleyError error)
		{
			int statusCode = error.networkResponse != null ? error.networkResponse.statusCode
			        : ResultCode.GENERAL_FAILURE;
			Log.e(TAG, "Error retrieving EPG program images with code " + statusCode + ": " + error);
			_onResultReceived.onReceiveResult(new FeatureError(FeatureEPGBulsat.this, statusCode, error), null);
		}
	}

	private class XMLTVContentHandler extends DefaultHandler
	{
		static final String TAG_TVLISTS = "tvlists";
		static final String TAG_TV = "tv";
		static final String TAG_EPG_NAME = "epg_name";
		static final String TAG_SOURCES = "sources";
		static final String TAG_NDVR = "ndvr";

		private String _channelId;
		private String _streamUrl;
		private String _seekUrl;
		private StringBuilder _buffer = new StringBuilder();
		private boolean _valid;
		private Map<String, Boolean> _updatedChannels = new HashMap<String, Boolean>();
		private List<String> _addChannels = new ArrayList<String>();
		private List<String> _delChannels = new ArrayList<String>();

		public List<String> getAddChannels()
		{
			return _addChannels;
		}

		public List<String> getDelChannels()
		{
			return _delChannels;
		}

		/**
		 * @return true if the xml is valid
		 */
		boolean isValid()
		{
			return _valid;
		}

		@Override
		public void characters(char[] ch, int start, int length)
		{
			_buffer.append(ch, start, length);
		}

		@Override
		public void endElement(String namespaceURI, String localName, String qName) throws SAXException
		{
			if (TAG_EPG_NAME.equals(localName))
			{
				_channelId = _buffer.toString().trim();
			}
			else if (TAG_SOURCES.equals(localName))
			{
				_streamUrl = _buffer.toString().trim();
			}
			else if (TAG_NDVR.equals(localName))
			{
				_seekUrl = _buffer.toString().trim();
			}
			else if (TAG_TV.equals(localName))
			{
				_valid = true;
				ChannelBulsat channel = (ChannelBulsat) getChannelById(_channelId);
				_updatedChannels.put(_channelId, Boolean.TRUE);
				if (channel == null)
				{
					if (!com.aviq.tv.android.sdk.utils.TextUtils.isEmpty(_streamUrl))
					{
						Log.w(TAG,
						        "Got new channel " + _channelId + " from "
						                + getPrefs().getString(Param.BULSAT_CHANNELS_URL_XML)
						                + " missing on AVTV server ( "
						                + getPrefs().getString(FeatureEPG.Param.EPG_SERVER) + ")");
						_addChannels.add(_channelId);
					}
				}
				else
				{
					if (!TextUtils.equals(_streamUrl, channel.getStreamUrl()))
					{
						Log.d(TAG, "Updating " + channel.getChannelId() + " stream url from " + channel.getStreamUrl()
						        + " to " + _streamUrl);
						channel.setStreamUrl(_streamUrl);
					}
					if (!TextUtils.equals(_seekUrl, channel.getSeekUrl()))
					{
						Log.d(TAG, "Updating " + channel.getChannelId() + " seek url from " + channel.getSeekUrl()
						        + " to " + _seekUrl);
						channel.setSeekUrl(_seekUrl);
					}
				}

				_channelId = _streamUrl = _seekUrl = null;
			}
			else if (TAG_TVLISTS.equals(localName))
			{
				// verify channels for removal
				for (Channel channel : getChannels())
				{
					if (_updatedChannels.get(channel.getChannelId()) == null)
						_delChannels.add(channel.getChannelId());
				}
			}

			// clear buffer
			_buffer.setLength(0);
		}
	}

	private class GenresContentHandler extends DefaultHandler
	{
		static final String TAG_GENRES = "chan_types";
		static final String TAG_GENRE = "type";
		static final String TAG_ID = "id";
		static final String TAG_TITLE = "name";
		static final String TAG_LOGO = "logo";
		static final String TAG_LOGO_SELECTED = "logo_selected";

		private OnResultReceived _onResultReceived;
		private Genre _genre = new Genre();
		private StringBuilder _buffer = new StringBuilder();
		private boolean _valid;
		private int _logosLoaded = 0;
		private int _logosRequested = 0;
		private boolean _parsed;
		private Genres _genres = new Genres();

		GenresContentHandler(OnResultReceived onResultReceived)
		{
			_onResultReceived = onResultReceived;
		}

		public Genres getGenres()
		{
			return _genres;
		}

		private void callBackOnFinish()
		{
			Log.i(TAG, ".callBackOnFinish: _parsed = " + _parsed + ", _logosRequested = " + _logosRequested
			        + ", _logosLoaded = " + _logosLoaded);
			if (_parsed && _logosRequested == _logosLoaded)
				_onResultReceived.onReceiveResult(FeatureError.OK(FeatureEPGBulsat.this), _genres);
		}

		/**
		 * @return true if the xml is valid
		 */
		boolean isValid()
		{
			return _valid;
		}

		@Override
		public void characters(char[] ch, int start, int length)
		{
			_buffer.append(ch, start, length);
		}

		@Override
		public void endElement(String namespaceURI, String localName, String qName) throws SAXException
		{
			if (TAG_GENRE.equals(localName))
			{
				_genres.addGenre(_genre);
				_genre = new Genre();
				_valid = true;
			}
			if (TAG_ID.equals(localName))
			{
				_genre.setId(_buffer.toString().trim());
			}
			else if (TAG_TITLE.equals(localName))
			{
				_genre.setTitle(_buffer.toString().trim());
			}
			else if (TAG_LOGO.equals(localName))
			{
				String url = _buffer.toString().trim();
				GenreImageListener genreImageListener = new GenreImageListener(_genre, false);
				ImageRequest imageRequest = new ImageRequest(url, genreImageListener, 0, 0, Config.ARGB_8888,
				        genreImageListener);
				Environment.getInstance().getRequestQueue().add(imageRequest);
				_logosRequested++;
			}
			else if (TAG_LOGO_SELECTED.equals(localName))
			{
				String url = _buffer.toString().trim();
				GenreImageListener genreImageListener = new GenreImageListener(_genre, true);
				ImageRequest imageRequest = new ImageRequest(url, genreImageListener, 0, 0, Config.ARGB_8888,
				        genreImageListener);
				Environment.getInstance().getRequestQueue().add(imageRequest);
				_logosRequested++;
			}
			else if (TAG_GENRES.equals(localName))
			{
				_parsed = true;
			}

			// clear buffer
			_buffer.setLength(0);
		}

		private class GenreImageListener implements Response.Listener<Bitmap>, ErrorListener
		{
			private Genre _genre;
			private boolean _isSelected;

			GenreImageListener(Genre genre, boolean isSelected)
			{
				_genre = genre;
				_isSelected = isSelected;
			}

			@Override
			public void onResponse(Bitmap bitmap)
			{
				if (_isSelected)
					_genre.setLogoSelected(bitmap);
				else
					_genre.setLogo(bitmap);

				_logosLoaded++;
				callBackOnFinish();
			}

			@Override
			public void onErrorResponse(VolleyError arg0)
			{
				_logosLoaded++;
				callBackOnFinish();
			}
		}
	}

	private class ChannelJSONResponse implements Response.Listener<JSONArray>, Response.ErrorListener
	{
		private OnResultReceived _onResultReceived;
		private int _logosRequested;
		private int _logosLoaded;

		ChannelJSONResponse(OnResultReceived onResultReceived)
		{
			_onResultReceived = onResultReceived;
		}

		@Override
		public void onResponse(JSONArray jsonArr)
		{
			try
			{
				Map<String, Boolean> receivedChannelsMap = new HashMap<String, Boolean>();
				boolean channelsChanged = false;

				for (int i = 0; i < jsonArr.length(); i++)
				{
					ChannelImageListener channelImageListener;
					ImageRequest imageRequest;

					JSONObject jsonChannel = jsonArr.getJSONObject(i);
					String channelId = jsonChannel.getString("epg_name");
					receivedChannelsMap.put(channelId, Boolean.TRUE);
					ChannelBulsat channel = (ChannelBulsat) getChannelById(channelId);
					if (channel == null)
					{
						// new channel arrived
						channel = (ChannelBulsat) createChannel();
						channel.setChannelId(channelId);
						addChannel(channel);
						channelsChanged = true;
					}

					channel.setTitle(jsonChannel.getString("title"));
					channel.setChannelImageUrl(ChannelBulsat.LOGO_NORMAL, jsonChannel.getString("logo"));
					channel.setChannelImageUrl(ChannelBulsat.LOGO_SELECTED, jsonChannel.getString("logo_selected"));
					channel.setChannelImageUrl(ChannelBulsat.LOGO_FAVORITE, jsonChannel.getString("logo_favorite"));
					channel.setProgramImageUrl(jsonChannel.getString("logo_epg"), ImageSize.LARGE);
					channel.setProgramImageUrl(jsonChannel.getString("logo_epg"), ImageSize.MEDIUM);

					if (jsonChannel.has("sources"))
						channel.setStreamUrl(jsonChannel.getString("sources"));

					if (jsonChannel.has("ndvr"))
						channel.setSeekUrl(jsonChannel.getString("ndvr"));

					if (jsonChannel.has("genre"))
					{
						String genreTitle = jsonChannel.getString("genre");
						Genre genre = Genres.getInstance().getGenreByTitle(genreTitle);
						channel.setGenre(genre);
					}

					if (jsonChannel.has("pg"))
					{
						boolean parentControl = !"free".equals(jsonChannel.getString("pg"));
						channel.setParentControl(parentControl);
					}

					if (jsonChannel.has("channel"))
						try
						{
							channel.setChannelNo(Integer.valueOf(jsonChannel.getString("channel")));
						}
						catch (NumberFormatException nfe)
						{
							Log.w(TAG, nfe.getMessage(), nfe);
						}

					if (jsonChannel.has("can_record"))
					{
						int canRecord = Integer.valueOf(jsonChannel.getString("can_record"));
						channel.setRecordable(canRecord > 0);
					}

					if (jsonChannel.has("has_dvr"))
					{
						int hasDdvr = Integer.valueOf(jsonChannel.getString("has_dvr"));
						channel.setPlayable(hasDdvr > 0);
						channel.setNDVR(hasDdvr);
					}

					channel.setRadio(jsonChannel.optBoolean("radio"));

					channelImageListener = new ChannelImageListener(channel, ChannelBulsat.LOGO_NORMAL);
					imageRequest = new ImageRequest(channel.getChannelImageUrl(ChannelBulsat.LOGO_NORMAL),
					        channelImageListener, 0, 0, Config.ARGB_8888, channelImageListener);
					Environment.getInstance().getRequestQueue().add(imageRequest);
					_logosRequested++;

					channelImageListener = new ChannelImageListener(channel, ChannelBulsat.LOGO_SELECTED);
					imageRequest = new ImageRequest(channel.getChannelImageUrl(ChannelBulsat.LOGO_SELECTED),
					        channelImageListener, 0, 0, Config.ARGB_8888, channelImageListener);
					Environment.getInstance().getRequestQueue().add(imageRequest);
					_logosRequested++;

					channelImageListener = new ChannelImageListener(channel, ChannelBulsat.LOGO_FAVORITE);
					imageRequest = new ImageRequest(channel.getChannelImageUrl(ChannelBulsat.LOGO_FAVORITE),
					        channelImageListener, 0, 0, Config.ARGB_8888, channelImageListener);
					Environment.getInstance().getRequestQueue().add(imageRequest);
					_logosRequested++;
				}

				for (Channel existingChannel : getChannels())
				{
					if (!receivedChannelsMap.containsKey(existingChannel.getChannelId()))
					{
						// detected removed channel
						channelsChanged = true;
						break;
					}
				}

				if (channelsChanged)
				{
					getEventMessenger().trigger(ON_CHANNELS_CHANGED);
				}
			}
			catch (JSONException e)
			{
				// Load channels failed, notify error
				Log.e(TAG, e.getMessage(), e);
				_onResultReceived.onReceiveResult(
				        new FeatureError(FeatureEPGBulsat.this, ResultCode.PROTOCOL_ERROR, e), null);
			}
		}

		@Override
		public void onErrorResponse(VolleyError error)
		{
			int statusCode = error.networkResponse != null ? error.networkResponse.statusCode
			        : ResultCode.GENERAL_FAILURE;
			Log.e(TAG, "Error retrieving channels with code " + statusCode + ": " + error);
			_onResultReceived.onReceiveResult(new FeatureError(FeatureEPGBulsat.this, statusCode, error), null);
		}

		private void callBackOnFinish()
		{
			Log.i(TAG, ".callBackOnFinish: _logosRequested = " + _logosRequested + ", _logosLoaded = " + _logosLoaded);
			if (_logosRequested == _logosLoaded)
				_onResultReceived.onReceiveResult(FeatureError.OK(FeatureEPGBulsat.this), null);
		}

		private class ChannelImageListener implements Response.Listener<Bitmap>, ErrorListener
		{
			private Channel _channel;
			private int _imageType;

			ChannelImageListener(Channel channel, int imageType)
			{
				_channel = channel;
				_imageType = imageType;
			}

			@Override
			public void onResponse(Bitmap bitmap)
			{
				_channel.setChannelImage(_imageType, bitmap);

				_logosLoaded++;
				callBackOnFinish();
			}

			@Override
			public void onErrorResponse(VolleyError arg0)
			{
				_logosLoaded++;
				callBackOnFinish();
			}
		}
	}

	private interface UpdateInterface
	{
		void update(final OnResultReceived onResultReceived);
	}

	private class UpdateChannelsJSON implements UpdateInterface
	{
		@Override
		public void update(OnResultReceived onResultReceived)
		{
			Log.i(TAG, ".UpdateChannelsJSON.update");
			String url = getPrefs().getString(Param.BULSAT_CHANNELS_URL_JSON);

			// retrieve channel streams from server
			ChannelJSONResponse channelJSONResponse = new ChannelJSONResponse(onResultReceived);
			JsonArrayRequest request = new JsonArrayRequest(url, channelJSONResponse, channelJSONResponse)
			{
				@Override
				public Map<String, String> getHeaders() throws AuthFailureError
				{
					Map<String, String> headers = new HashMap<String, String>();
					headers.put("STBDEVEL", "INTELIBO");
					return headers;
				}
			};
			Environment.getInstance().getRequestQueue().add(request);
		}
	}

	private class UpdateChannelsXML implements UpdateInterface
	{
		@Override
		public void update(final OnResultReceived onResultReceived)
		{
			Log.i(TAG, ".UpdateChannelsXML.update");
			final String url = getPrefs().getString(Param.BULSAT_CHANNELS_URL_XML);
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					FeatureError error = null;
					HttpUriRequest httpGet = new HttpGet(url);
					Log.i(TAG, "Opening " + httpGet.getURI());

					XMLTVContentHandler contentHandler = new XMLTVContentHandler();
					HttpClient httpClient = new DefaultHttpClient(httpGet.getParams());
					try
					{
						HttpResponse response = httpClient.execute(httpGet);
						HttpEntity entity = response.getEntity();
						if (entity != null)
						{
							InputStream content = entity.getContent();

							// Setup SAX parser
							SAXParserFactory spf = SAXParserFactory.newInstance();
							spf.setNamespaceAware(true);
							SAXParser saxParser = spf.newSAXParser();

							XMLReader xmlReader = saxParser.getXMLReader();
							xmlReader.setContentHandler(contentHandler);
							Log.i(TAG, "Parsing XML TV xml");
							xmlReader.parse(new InputSource(content));
							if (!contentHandler.isValid())
								error = new FeatureError(FeatureEPGBulsat.this, ResultCode.PROTOCOL_ERROR,
								        "Unexpected XML format by EPG service at " + url);
							else
								error = FeatureError.OK(FeatureEPGBulsat.this);
						}
						else
						{
							error = new FeatureError(FeatureEPGBulsat.this, ResultCode.PROTOCOL_ERROR,
							        "No entity returned by " + httpGet.getURI());
						}
					}
					catch (SAXException e)
					{
						Log.e(TAG, e.getMessage(), e);
						error = new FeatureError(FeatureEPGBulsat.this, ResultCode.PROTOCOL_ERROR, e);
					}
					catch (Exception e)
					{
						Log.e(TAG, e.getMessage(), e);
						error = new FeatureError(FeatureEPGBulsat.this, e);
					}

					if (onResultReceived != null)
					{
						if (!error.isError()
						        && (contentHandler.getAddChannels().size() > 0 || contentHandler.getDelChannels()
						                .size() > 0))
						{
							// channels set changed
							getEventMessenger().trigger(ON_CHANNELS_CHANGED);
						}
						final FeatureError fError = error;
						Environment.getInstance().runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								onResultReceived.onReceiveResult(fError, null);
							}
						});
					}
				}
			}).start();
		}
	}

	private class UpdateGenresXML implements UpdateInterface
	{
		@Override
		public void update(final OnResultReceived onResultReceived)
		{
			Log.i(TAG, ".UpdateGenresXML.update");
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					FeatureError error = null;
					String url = getPrefs().getString(Param.BULSAT_GENRES_URL_XML);
					HttpUriRequest httpGet = new HttpGet(url);
					Log.i(TAG, "Opening " + httpGet.getURI());

					final GenresContentHandler contentHandler = new GenresContentHandler(onResultReceived);
					HttpClient httpClient = new DefaultHttpClient(httpGet.getParams());
					try
					{
						HttpResponse response = httpClient.execute(httpGet);
						HttpEntity entity = response.getEntity();
						if (entity != null)
						{
							InputStream content = entity.getContent();

							// Setup SAX parser
							SAXParserFactory spf = SAXParserFactory.newInstance();
							spf.setNamespaceAware(true);
							SAXParser saxParser = spf.newSAXParser();

							XMLReader xmlReader = saxParser.getXMLReader();
							// Response to initialize callback will come from
							// the
							// XML content handler after retrieving all related
							// images
							xmlReader.setContentHandler(contentHandler);
							Log.i(TAG, "Parsing Genres xml");
							xmlReader.parse(new InputSource(content));
							if (!contentHandler.isValid())
								error = new FeatureError(FeatureEPGBulsat.this, ResultCode.PROTOCOL_ERROR,
								        "Unexpected XML format by Genre service at " + url);
							else
							{
								if (!Genres.getInstance().isEmpty()
								        && !contentHandler.getGenres().isEqualTo(Genres.getInstance()))
								{
									// genres set changed
									getEventMessenger().trigger(ON_CHANNELS_CHANGED);
								}
							}
						}
						else
						{
							error = new FeatureError(FeatureEPGBulsat.this, ResultCode.PROTOCOL_ERROR,
							        "No entity returned by " + httpGet.getURI());
						}
					}
					catch (SAXException e)
					{
						Log.e(TAG, e.getMessage(), e);
						error = new FeatureError(FeatureEPGBulsat.this, ResultCode.PROTOCOL_ERROR, e);
					}
					catch (Exception e)
					{
						Log.e(TAG, e.getMessage(), e);
						error = new FeatureError(FeatureEPGBulsat.this, e);
					}

					if (error != null && onResultReceived != null)
					{
						// response only error
						final FeatureError fError = error;
						Environment.getInstance().runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								Log.e(TAG, fError.getMessage(), fError);
								onResultReceived.onReceiveResult(fError, contentHandler.getGenres());
							}
						});
					}
				}
			}).start();
		}
	}

	private class UpdateGenresJSONWithHttpClient implements UpdateInterface
	{
		@Override
		public void update(final OnResultReceived onResultReceived)
		{
			Log.i(TAG, ".UpdateGenresJSONWithHttpClient.update");
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					FeatureError error = null;
					String url = getPrefs().getString(Param.BULSAT_GENRES_URL_JSON);
					HttpUriRequest httpGet = new HttpGet(url);
					Log.i(TAG, "Opening " + httpGet.getURI());

					ChannelGenreResponse channelGenreResponse = new ChannelGenreResponse(onResultReceived);
					HttpClient httpClient = new DefaultHttpClient(httpGet.getParams());
					httpGet.setHeader("STBDEVEL", "INTELIBO");
					try
					{
						HttpResponse response = httpClient.execute(httpGet);
						HttpEntity entity = response.getEntity();
						if (entity != null)
						{
							InputStream content = entity.getContent();

							BufferedReader reader = new BufferedReader(new InputStreamReader(content, "UTF-8"));
							StringBuilder sb = new StringBuilder();
							String line = null;
							while ((line = reader.readLine()) != null)
							{
								sb.append(line + "n");
							}
							content.close();

							channelGenreResponse.onResponse(new JSONArray(sb.toString()));
						}
						else
						{
							error = new FeatureError(FeatureEPGBulsat.this, ResultCode.PROTOCOL_ERROR,
							        "No entity returned by " + httpGet.getURI());
						}
					}
					catch (Exception e)
					{
						Log.e(TAG, e.getMessage(), e);
						error = new FeatureError(FeatureEPGBulsat.this, e);
					}

					if (error != null && onResultReceived != null)
					{
						// response only error
						final FeatureError fError = error;
						Environment.getInstance().runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								Log.e(TAG, fError.getMessage(), fError);
								onResultReceived.onReceiveResult(fError, null);
							}
						});
					}
				}
			}).start();
		}
	}

	private class UpdateGenresJSON implements UpdateInterface
	{
		@Override
		public void update(final OnResultReceived onResultReceived)
		{
			Log.i(TAG, ".UpdateGenresJSON.update");
			String url = getPrefs().getString(Param.BULSAT_GENRES_URL_JSON);

			// retrieve channel genres from server
			ChannelGenreResponse channelGenreResponse = new ChannelGenreResponse(onResultReceived);
			JsonArrayRequest request = new JsonArrayRequest(url, channelGenreResponse, channelGenreResponse)
			{
				@Override
				public Map<String, String> getHeaders() throws AuthFailureError
				{
					Map<String, String> headers = new HashMap<String, String>();
					headers.put("STBDEVEL", "INTELIBO");
					return headers;
				}
			};
			Environment.getInstance().getRequestQueue().add(request);
		}
	}

	private class ChannelGenreResponse implements Response.Listener<JSONArray>, Response.ErrorListener
	{
		private OnResultReceived _onResultReceived;
		private int _logosRequested;
		private int _logosLoaded;
		private Genres _genres = new Genres();

		ChannelGenreResponse(OnResultReceived onResultReceived)
		{
			_onResultReceived = onResultReceived;
		}

		@Override
		public void onResponse(JSONArray jsonArr)
		{
			try
			{
				for (int i = 0; i < jsonArr.length(); i++)
				{
					GenreImageListener genreImageListener;
					ImageRequest imageRequest;
					Genre genre = new Genre();

					JSONObject jsonGenre = jsonArr.getJSONObject(i);

					genre.setId(jsonGenre.getString("id"));
					genre.setTitle(jsonGenre.getString("name"));
					genre.setHidden(jsonGenre.has("visible")
					        && "false".equalsIgnoreCase(jsonGenre.getString("visible")));

					String logoUrl = jsonGenre.getString("logo");
					genreImageListener = new GenreImageListener(genre, false);
					imageRequest = new ImageRequest(logoUrl, genreImageListener, 0, 0, Config.ARGB_8888,
					        genreImageListener);
					Environment.getInstance().getRequestQueue().add(imageRequest);
					_logosRequested++;

					String logoSelectedUrl = jsonGenre.getString("logo_selected");
					genreImageListener = new GenreImageListener(genre, true);
					imageRequest = new ImageRequest(logoSelectedUrl, genreImageListener, 0, 0, Config.ARGB_8888,
					        genreImageListener);
					Environment.getInstance().getRequestQueue().add(imageRequest);
					_logosRequested++;

					if (!genre.isHidden())
						_genres.addGenre(genre);
				}

				// onResultReceived callback will be called by last genre logo
				// response
			}
			catch (JSONException e)
			{
				// Load channels failed, notify error
				Log.e(TAG, e.getMessage(), e);
				_onResultReceived.onReceiveResult(
				        new FeatureError(FeatureEPGBulsat.this, ResultCode.PROTOCOL_ERROR, e), null);
			}
		}

		@Override
		public void onErrorResponse(VolleyError error)
		{
			int statusCode = error.networkResponse != null ? error.networkResponse.statusCode
			        : ResultCode.GENERAL_FAILURE;
			Log.e(TAG, "Error retrieving EPG channel genres with code " + statusCode + ": " + error);
			_onResultReceived.onReceiveResult(new FeatureError(FeatureEPGBulsat.this, statusCode, error), null);
		}

		private void callBackOnFinish()
		{
			Log.i(TAG, ".callBackOnFinish: _logosRequested = " + _logosRequested + ", _logosLoaded = " + _logosLoaded);
			if (_logosRequested == _logosLoaded)
				_onResultReceived.onReceiveResult(FeatureError.OK(FeatureEPGBulsat.this), _genres);
		}

		private class GenreImageListener implements Response.Listener<Bitmap>, ErrorListener
		{
			private Genre _genre;
			private boolean _isSelected;

			GenreImageListener(Genre genre, boolean isSelected)
			{
				_genre = genre;
				_isSelected = isSelected;
			}

			@Override
			public void onResponse(Bitmap bitmap)
			{
				if (_isSelected)
					_genre.setLogoSelected(bitmap);
				else
					_genre.setLogo(bitmap);

				_logosLoaded++;
				callBackOnFinish();
			}

			@Override
			public void onErrorResponse(VolleyError arg0)
			{
				_logosLoaded++;
				callBackOnFinish();
			}
		}
	}

	/** Determines whether to display the Record button */
	public boolean isProgramRecordable(ProgramBulsat program)
	{
		ChannelBulsat channel = ((ChannelBulsat) program.getChannel());
		Calendar now = _feature.Component.TIMEZONE.getCurrentTime();
		boolean recordable = channel.isRecordable();
		boolean inFuture = program.getStopTime().after(now);
		int ndvr = channel.getNDVR();
		Calendar ndvrTime = Calendar.getInstance();
		ndvrTime.add(Calendar.SECOND, -ndvr);
		return !channel.isParentControl() && recordable
		        && (inFuture || (channel.isPlayable() && program.getStartTime().after(ndvrTime)));
	}

	/** Determines whether to display the Play button */
	public boolean isProgramPlayable(ProgramBulsat program)
	{
		ChannelBulsat channel = ((ChannelBulsat) program.getChannel());
		Calendar now = _feature.Component.TIMEZONE.getCurrentTime();
		Calendar ndvrStart = _feature.Component.TIMEZONE.getCurrentTime();
		ndvrStart.add(Calendar.SECOND, -channel.getNDVR());

		boolean playable = channel.isPlayable();
		FeatureRecordingScheduler recordingScheduler = (FeatureRecordingScheduler) Environment.getInstance()
		        .getFeatureComponent(FeatureName.Component.RECORDING_SCHEDULER);
		boolean recorded = recordingScheduler != null ? recordingScheduler.isProgramRecorded(program) : false;
		boolean inNdvr = program.getStopTime().before(now) && program.getStartTime().after(ndvrStart);
		return !channel.isParentControl() && inNdvr && (recorded || playable);
	}

	private class OnCommandGetProgramBulsatDetails implements CommandHandler
	{
		@Override
		public void execute(Bundle params, final OnResultReceived onResultReceived)
		{
			String channelId = params.getString(CommandGetProgramDetailsExtras.CHANNEL_ID.name());
			String programId = params.getString(CommandGetProgramDetailsExtras.PROGRAM_ID.name());
			Log.i(TAG, ".OnCommandGetProgramBulsatDetails.execute: channelId = " + channelId + ", programId = "
			        + programId);

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
						ProgramBulsat program = (ProgramBulsat) object;
						try
						{
							JSONObject jsonProgram = new JSONObject();
							jsonProgram.put("length", program.getLengthMin());
							jsonProgram.put("title", program.getTitle());
							String description = program.getDetailAttribute(ProgramAttribute.DESCRIPTION);
							if (description != null)
								jsonProgram.put("description", description);
							jsonProgram.put("image", program.getDetailAttribute(ProgramAttribute.IMAGE));
							jsonProgram.put("playable", isProgramPlayable(program));
							jsonProgram.put("recordable", isProgramRecordable(program));
							jsonProgram.put("start", Calendars.makeString(program.getStartTime(), "yyyyMMddHHmmss"));
							jsonProgram.put("stop", Calendars.makeString(program.getStopTime(), "yyyyMMddHHmmss"));

							FeatureRecordingScheduler recordingScheduler = (FeatureRecordingScheduler) Environment
							        .getInstance().getFeatureComponent(FeatureName.Component.RECORDING_SCHEDULER);
							boolean recorded = recordingScheduler != null ? recordingScheduler
							        .isProgramRecorded(program) : false;
							jsonProgram.put("recorded", recorded);
							onResultReceived.onReceiveResult(FeatureError.OK(FeatureEPGBulsat.this), jsonProgram);
						}
						catch (JSONException e)
						{
							onResultReceived.onReceiveResult(new FeatureError(FeatureEPGBulsat.this, e), null);
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

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

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
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
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.IFeature;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.epg.FeatureEPG;
import com.aviq.tv.android.sdk.feature.epg.Program;

/**
 * RayV specific extension of EPG feature
 */
@Author("alek")
public class FeatureEPGBulsat extends FeatureEPG
{
	public static final String TAG = FeatureEPGBulsat.class.getSimpleName();
	private static final int DEFAULT_STREAM_PLAY_DURATION = 3600;

	public static enum Param
	{
		/**
		 * Direct url to Bulsatcom channels
		 */
		BULSAT_CHANNELS_URL("http://api.iptv.bulsat.com/?xml&tv"),

		/**
		 * Update interval for updating channel streams directly from bulsat
		 * server
		 */
		STREAMS_UPDATE_INTERVAL(2 * 3600 * 1000),

		/**
		 * EPG provider name
		 */
		EPG_PROVIDER("bulsat");

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

	public FeatureEPGBulsat() throws FeatureNotFoundException
	{
		super();
	}

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		super.initialize(new OnFeatureInitialized()
		{
			@Override
			public void onInitialized(final FeatureError error)
			{
				if (!error.isError())
				{
					updateBulsatChannelStreams(new OnResultReceived()
					{
						@Override
						public void onReceiveResult(FeatureError error, Object object)
						{
							onFeatureInitialized.onInitialized(error);

							// update channel streams periodically directly from
							// the bulsat server
							final long updateInterval = getPrefs().getLong(Param.STREAMS_UPDATE_INTERVAL);
							getEventMessenger().postDelayed(new Runnable()
							{
								@Override
								public void run()
								{
									updateBulsatChannelStreams(null);
									getEventMessenger().postDelayed(this, updateInterval);
								}
							}, updateInterval);
						}
					});
				}
				else
				{
					onFeatureInitialized.onInitialized(error);
				}
			}

			@Override
			public void onInitializeProgress(IFeature feature, float progress)
			{
				onFeatureInitialized.onInitializeProgress(feature, progress);
			}
		});
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
	protected Channel createChannel(int index)
	{
		return new ChannelBulsat(index);
	}

	@Override
	protected String getChannelsUrl()
	{
		String url = super.getChannelsUrl();
		return url
		        + "?attr=channel,genre,ndvr,streams.1.url,streams.2.url,pg,recordable,thumbnail_base64,thumbnail_selected_base64,thumbnail_favorite_base64,program_image_medium,program_image_large";
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
			// retrieve program images from server
			ProgramImagesResponse programImagesResponse = new ProgramImagesResponse(onResultReceived);
			JsonObjectRequest request = new JsonObjectRequest(programImagesUrl, null, programImagesResponse,
			        programImagesResponse);
			_requestQueue.add(request);
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
						byte[] decodedString = Base64.decode(jsonArr.getString(programImageIndex), Base64.DEFAULT);
						_programImages.add(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
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
			Log.e(TAG, "Error retrieving EPG channels with code " + statusCode + ": " + error);
			_onResultReceived.onReceiveResult(new FeatureError(FeatureEPGBulsat.this, statusCode, error), null);
		}
	}

	private void updateBulsatChannelStreams(final OnResultReceived onResultReceived)
	{
		Log.i(TAG, ".updateBulsatChannelStreams");

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				FeatureError error = null;
				HttpUriRequest httpGet = new HttpGet(getPrefs().getString(Param.BULSAT_CHANNELS_URL));
				Log.i(TAG, "Opening " + httpGet.getURI());

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
						xmlReader.setContentHandler(new XMLTVContentHandler());
						Log.i(TAG, "Parsing XML TV xml");
						xmlReader.parse(new InputSource(content));
						error = FeatureError.OK(FeatureEPGBulsat.this);
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

				if (onResultReceived != null)
				{
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

	private class XMLTVContentHandler extends DefaultHandler
	{
		static final String TAG_TV = "tv";
		static final String TAG_EPG_NAME = "epg_name";
		static final String TAG_SOURCES = "sources";
		static final String TAG_NDVR = "ndvr";

		private String _channelId;
		private String _streamUrl;
		private String _seekUrl;
		private StringBuilder _buffer = new StringBuilder();

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
				ChannelBulsat channel = (ChannelBulsat) getChannelById(_channelId);
				if (channel == null)
				{
					Log.w(TAG,
					        "Got channel " + _channelId + " on Bulsatcom server ("
					                + getPrefs().getString(Param.BULSAT_CHANNELS_URL) + ") missing on AVTV server ( "
					                + getPrefs().getString(FeatureEPG.Param.EPG_SERVER) + ")");
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

			// clear buffer
			_buffer.setLength(0);
		}
	}
}

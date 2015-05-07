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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import android.os.Bundle;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
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
		 * EPG provider name
		 */
		EPG_PROVIDER("bulsat");

		Param(String value)
		{
			Log.w(TAG, "put " + name() + " -> " + value);
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.EPG).put(name(), value);
		}
	}

	public static Param ParamIniter = Param.EPG_PROVIDER;

	public FeatureEPGBulsat() throws FeatureNotFoundException
	{
		super();
	}

	@Override
	public void onEvent(int msgId, Bundle bundle)
	{
		super.onEvent(msgId, bundle);

		// FIXME: refactor
/*
		if (ON_EPG_UPDATED == msgId)
		{
			// Load and parse channel streams directly from the Bulsatcom
			// server as they are expiring
			updateBulsatChannelStreams();
		}
		*/
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
			else if ("thumbnail_selected".equals(key))
				bulsatMetaData.metaChannelLogoSelected = j;
			else if ("thumbnail_favorite".equals(key))
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
		        + "?attr=channel,genre,ndvr,streams.1.url,streams.2.url,pg,recordable,thumbnail_selected,thumbnail_favorite,program_image_medium,program_image_large";
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

	// FIXME: refactor consideration
	/*
	@Override
	protected void retrieveChannelLogo(Channel channel, int channelIndex)
	{
		ImageRequest imageRequest;
		LogoResponseCallback responseCallback;
		String channelLogoUrl;

		// download selected channel logo
		String channelId = channel.getChannelId();
		ChannelBulsat channelBulsat = (ChannelBulsat) channel;

		// retrieves selected channel logo
		channelLogoUrl = getChannelImageUrl(channelId, channelBulsat.getLogo(ChannelBulsat.LOGO_SELECTED));
		Log.d(TAG, "Retrieving selected channel logo for index " + channelIndex + " from " + channelLogoUrl);
		responseCallback = new LogoResponseCallback(channelId, channelIndex, IEpgDataProvider.ChannelLogoType.SELECTED);
		imageRequest = new ImageRequest(channelLogoUrl, responseCallback, _channelLogoWidth, _channelLogoHeight,
		        Config.ARGB_8888, responseCallback);
		_requestQueue.add(imageRequest);

		// retrieves favorite channel logo
		channelLogoUrl = getChannelImageUrl(channelId, channelBulsat.getLogo(ChannelBulsat.LOGO_FAVORITE));
		responseCallback = new LogoResponseCallback(channelId, channelIndex, IEpgDataProvider.ChannelLogoType.FAVORITE);
		imageRequest = new ImageRequest(channelLogoUrl, responseCallback, _channelLogoWidth, _channelLogoHeight,
		        Config.ARGB_8888, responseCallback);
		_requestQueue.add(imageRequest);

		// FIXME: if the request of the selected or favorite logo finishes after
		// the request
		// of the normal logo, the last will not register in the EpgDataCompat

		// retrieves normal channel logo
		super.retrieveChannelLogo(channel, channelIndex);
	}

	private class LogoResponseCallback implements Response.Listener<Bitmap>, Response.ErrorListener
	{
		private int _index;
		private String _channelId;
		private ChannelLogoType _logoType;

		LogoResponseCallback(String channelId, int index, ChannelLogoType logoType)
		{
			_channelId = channelId;
			_index = index;
			_logoType = logoType;
		}

		@Override
		public void onResponse(Bitmap response)
		{
			Log.d(TAG,
			        "Received selected logo for _index = " + _index + " " + response.getWidth() + "x"
			                + response.getHeight() + ", _epgDataBeingLoaded = " + _epgDataBeingLoaded);
			if (_epgDataBeingLoaded != null)
			{
				_epgDataBeingLoaded.setChannelLogo(_index, response, _logoType);
			}
		}

		@Override
		public void onErrorResponse(VolleyError error)
		{
			Log.d(TAG, "Retrieve channel logo " + _channelId + " with error: " + error);
		}
	};

	private void updateBulsatChannelStreams()
	{
		Log.i(TAG, ".updateBulsatChannelStreams");

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
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
					}
				}
				catch (ClientProtocolException e)
				{
					Log.e(TAG, e.getMessage(), e);
				}
				catch (IOException e)
				{
					Log.e(TAG, e.getMessage(), e);
				}
				catch (ParserConfigurationException e)
				{
					Log.e(TAG, e.getMessage(), e);
				}
				catch (SAXException e)
				{
					Log.e(TAG, e.getMessage(), e);
				}
			}
		}).start();
	}
	*/

	// FIXME: refactor
	/*
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
				ChannelBulsat channel = (ChannelBulsat) _epgData.getChannel(_channelId);
				if (channel == null)
				{
					Log.w(TAG,
					        "Got channel " + _channelId + " on Bulsatcom server ("
					                + getPrefs().getString(Param.BULSAT_CHANNELS_URL) + ") missing on AVTV server ( "
					                + getPrefs().getString(FeatureEPGCompat.Param.EPG_SERVER) + ")");
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
	*/
}

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

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;
import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.epg.FeatureEPG;
import com.aviq.tv.android.sdk.feature.epg.IEpgDataProvider;
import com.aviq.tv.android.sdk.feature.epg.IEpgDataProvider.ChannelLogoType;
import com.aviq.tv.android.sdk.feature.epg.Program;

/**
 * RayV specific extension of EPG feature
 */
@Author("alek")
public class FeatureEPGBulsat extends FeatureEPG
{
	private static final int DEFAULT_STREAM_PLAY_DURATION = 3600;

	public FeatureEPGBulsat() throws FeatureNotFoundException
	{
		super();
	}

	public static final String TAG = FeatureEPGBulsat.class.getSimpleName();

	/**
	 * @return Bulsat EPG provider name
	 */
	@Override
	protected FeatureEPG.Provider getEPGProvider()
	{
		return FeatureEPG.Provider.bulsat;
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
				bulsatMetaData.metaChannelThumbnailSelected = j;
			else if ("thumbnail_favorite".equals(key))
				bulsatMetaData.metaChannelThumbnailFavorite = j;
		}
	}

	@Override
	protected String getProgramsUrl(String channelId)
	{
		String url = super.getProgramsUrl(channelId) + "?attr=description,image";
		return url;
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
		return url + "?attr=channel,genre,ndvr,streams.1.url,streams.2.url,pg,recordable,thumbnail_selected,thumbnail_favorite";
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
			else if ("image".equals(key))
				bulsatMetaData.metaImage = j;
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
	public void getStreamUrl(Channel channel, long playTime, long playDuration, OnStreamURLReceived onStreamURLReceived)
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
		onStreamURLReceived.onStreamURL(FeatureError.OK, streamUrl);
	}

	@Override
	public long getStreamBufferSize(Channel channel)
	{
		ChannelBulsat channelBulsat = (ChannelBulsat) channel;
		return channelBulsat.getNDVR();
	}

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
		channelLogoUrl = getChannelImageUrl(channelId, channelBulsat.getThumbnailSelected());
		Log.d(TAG, "Retrieving selected channel logo for index " + channelIndex + " from " + channelLogoUrl);
		responseCallback = new LogoResponseCallback(channelId, channelIndex, IEpgDataProvider.ChannelLogoType.SELECTED);
		imageRequest = new ImageRequest(channelLogoUrl, responseCallback, _channelLogoWidth,
		        _channelLogoHeight, Config.ARGB_8888, responseCallback);
		_httpQueue.add(imageRequest);

		// retrieves favorite channel logo
		channelLogoUrl = getChannelImageUrl(channelId, channelBulsat.getThumbnailFavorite());
		responseCallback = new LogoResponseCallback(channelId, channelIndex, IEpgDataProvider.ChannelLogoType.FAVORITE);
		imageRequest = new ImageRequest(channelLogoUrl, responseCallback, _channelLogoWidth,
		        _channelLogoHeight, Config.ARGB_8888, responseCallback);
		_httpQueue.add(imageRequest);

		// FIXME: if the request of the selected or favorite logo finishes after the request
		// of the normal logo, the last will not register in the EpgData

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

}

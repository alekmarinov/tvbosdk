package com.aviq.tv.android.sdk.feature.player;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.os.Bundle;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.player.FeatureStreamerRayV.Param;

public class FeatureStreamerBulsat extends FeatureStreamer
{
	private static final String TAG = FeatureStreamer.class.getSimpleName();
		
	public enum Param
	{
	
		/**
		 * Pattern composing channel stream seek url for Bulsat provider
		 */
		BULSAT_STREAM_URL_PATTERN(
		        "http://185.4.83.194:1935/dvr/${CHANNEL_ID}.stream/playlist.m3u8?DVR&wowzadvrplayliststart=${START_TIME}");

		Param(String value)
		{
			if (value != null)
				Environment.getInstance().getFeaturePrefs(FeatureName.Component.STREAMER).put(name(), value);
		}

	}
	
	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		super.initialize(onFeatureInitialized);
	}

	@Override
	public void getUrlByStreamId(String streamId, long playTimeDelta, OnStreamURLReceived onStreamURLReceived)
	{
		
		onStreamURLReceived.onStreamURL(streamId);
	}
	
	public void getUrlByStreamTestId(Channel channel, long playTimeDelta, OnStreamURLReceived onStreamURLReceived)
	{
		long startTimeInMs = System.currentTimeMillis() / 1000 - playTimeDelta;		
		Calendar startTime = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());	
		String formatStartTime =  sdf.format(startTime.getTime());
		startTime.setTimeInMillis(startTimeInMs);		
		Bundle bundle = new Bundle();
		bundle.putString("CHANNEL_ID", channel.getChannelId());		
		bundle.putString("START_TIME",formatStartTime);
		String streamUrl = getPrefs().getString(Param.BULSAT_STREAM_URL_PATTERN, bundle);		
		onStreamURLReceived.onStreamURL(streamUrl);
	}
	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.STREAMER;
	}
}

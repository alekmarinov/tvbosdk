package com.aviq.tv.android.sdk.feature.player;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import android.os.Bundle;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.epg.bulsat.ChannelBulsat;

public class FeatureStreamerBulsat extends FeatureStreamer
{
	private static final String TAG = FeatureStreamer.class.getSimpleName();

	public enum Param
	{

		/**
		 * Pattern composing channel stream seek url for Bulsat provider
		 */
		BULSAT_STREAM_URL_PATTERN("${SEEK_URL}&wowzadvrplayliststart=${START_TIME}&wowzadvrplaylistduration=3600000");

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
		ChannelBulsat channelBulsat = (ChannelBulsat) channel;
		if (playTimeDelta > 0)
		{
			long startTimeInMs = System.currentTimeMillis() / 1000 - playTimeDelta;
			Calendar startTime = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			startTime.setTimeInMillis(1000 * startTimeInMs);
			String formatStartTime = sdf.format(startTime.getTime());
			Bundle bundle = new Bundle();
			bundle.putString("SEEK_URL", channelBulsat.getSeekUrl());
			bundle.putString("START_TIME", formatStartTime);
			String streamUrl = getPrefs().getString(Param.BULSAT_STREAM_URL_PATTERN, bundle);
			onStreamURLReceived.onStreamURL(streamUrl);
		}
		else
		{
			getUrlByStreamId(channelBulsat.getStreamUrl(), 0, onStreamURLReceived);
		}
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.STREAMER;
	}
}

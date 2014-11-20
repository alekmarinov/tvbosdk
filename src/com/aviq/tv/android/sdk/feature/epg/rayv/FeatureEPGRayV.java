/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureEPGRayV.java
 * Author:      alek
 * Date:        1 Dec 2013
 * Description: RayV specific extension of EPG feature
 */

package com.aviq.tv.android.sdk.feature.epg.rayv;

import android.os.Bundle;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.epg.FeatureEPG;
import com.aviq.tv.android.sdk.feature.epg.Program;
import com.aviq.tv.android.sdk.feature.system.FeatureDevice.DeviceAttribute;
import com.rayv.StreamingAgent.Loader;

/**
 * RayV specific extension of EPG feature
 */
public class FeatureEPGRayV extends FeatureEPG
{
	public static final String TAG = FeatureEPGRayV.class.getSimpleName();
	public static final int DEFAULT_STREAM_PORT = 1234;

	private int _streamPort = DEFAULT_STREAM_PORT;

	public static enum Param
	{
		/**
		 * RayV streamer initialization
		 */
		STREAMER_INI("port=" + DEFAULT_STREAM_PORT + "\n" + "distributor=vtx\n" + "product_id=test\n"
		        + "allow_external_streams=1\n" + "localhost=127.0.0.1\n" + "thread_pool=10\n" + "udp_port=0\n"
		        + "udp_port_pairs=1\n" + "StopBrokenFrames=0\n" + "BitrateOverhead=50\n" + "ManualBitrateSwitching=1\n"
		        + "MinBitrate=0\n" + "MaxBitrate=0\n" + "MinStartBitrate=0\n" + "MaxStartBitrate=0\n"
		        + "MediaQueueSize=100\n"),

		/**
		 * Registered RayV user account name
		 */
		RAYV_USER(null), // 1C6F65F9DE8B

		/**
		 * Registered RayV account password
		 */
		RAYV_PASS(null), // 1C6F65F9DE8B

		/**
		 * RayV stream bitrate
		 */
		RAYV_STREAM_BITRATE(1200),

		/**
		 * RayV service port
		 */
		RAYV_STREAM_PORT(DEFAULT_STREAM_PORT),

		/**
		 * Pattern composing channel stream url for RayV provider
		 */
		RAYV_STREAM_URL_PATTERN(
		        "http://localhost:${PORT}/RayVAgent/v1/RAYV/${USER}:${PASS}@${STREAM_ID}?streams=${STREAM_ID}:${BITRATE}");

		Param(String value)
		{
			if (value != null)
				Environment.getInstance().getFeaturePrefs(FeatureName.Scheduler.EPG).put(name(), value);
		}

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Scheduler.EPG).put(name(), value);
		}
	}

	public FeatureEPGRayV() throws FeatureNotFoundException
	{
		require(FeatureName.Component.DEVICE);
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		String boxId = _feature.Component.DEVICE.getDeviceAttribute(DeviceAttribute.MAC);
		if (!getPrefs().has(Param.RAYV_USER))
			getPrefs().put(Param.RAYV_USER, boxId);
		if (!getPrefs().has(Param.RAYV_PASS))
			getPrefs().put(Param.RAYV_PASS, boxId);

		_streamPort = getPrefs().getInt(Param.RAYV_STREAM_PORT);

		// Start streaming agent
		Log.i(TAG, "Start RayV streaming agent");
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				Loader.startStreamer(getPrefs().getString(Param.STREAMER_INI));
			}
		}).start();
		super.initialize(onFeatureInitialized);
	}

	public void setStreamPort(int streamPort)
	{
		Log.i(TAG, ".setStreamPort: streamPort = " + streamPort);
		_streamPort = streamPort;
	}

	public int getStreamPort()
	{
		return _streamPort;
	}

	/**
	 * @return rayv EPG provider name
	 */
	@Override
	protected FeatureEPG.Provider getEPGProvider()
	{
		return FeatureEPG.Provider.rayv;
	}

	@Override
	protected Program createProgram(String id, Channel channel)
	{
		return new ProgramRayV(id, channel);
	}

	@Override
	protected Channel createChannel(int index)
	{
		return new ChannelRayV(index);
	}

	@Override
	public void getStreamUrl(Channel channel, long playTime, long playDuration, OnStreamURLReceived onStreamURLReceived)
	{
		Bundle bundle = new Bundle();
		bundle.putString("USER", getPrefs().getString(Param.RAYV_USER));
		bundle.putString("PASS", getPrefs().getString(Param.RAYV_PASS));
		bundle.putString("STREAM_ID", channel.getChannelId());
		bundle.putInt("BITRATE", getPrefs().getInt(Param.RAYV_STREAM_BITRATE));
		bundle.putInt("PORT", _streamPort);

		String streamUrl = getPrefs().getString(Param.RAYV_STREAM_URL_PATTERN, bundle);
		long playTimeDelta = System.currentTimeMillis() / 1000 - playTime;
		if (playTime > 0 && playTimeDelta > 0)
			streamUrl += "&timeshift=" + playTimeDelta;
		onStreamURLReceived.onStreamURL(FeatureError.OK, streamUrl);
	}

	@Override
	public long getStreamBufferSize(Channel channel)
	{
		// RayV channels have no buffer, but recorded on client
		return 0;
	}
}

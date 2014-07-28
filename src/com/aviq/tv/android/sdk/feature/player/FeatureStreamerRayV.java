/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeaturePlayerRayV.java
 * Author:      alek
 * Date:        1 Dec 2013
 * Description: Component feature providing RayV player
 */

package com.aviq.tv.android.sdk.feature.player;

import android.os.Bundle;
import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.rayv.StreamingAgent.Loader;

/**
 * Component feature providing RayV streamer
 */
public class FeatureStreamerRayV extends FeatureStreamer
{
	public static final String TAG = FeatureStreamerRayV.class.getSimpleName();
	public static final int DEFAULT_STREAM_PORT = 1234;

	public enum Param
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
				Environment.getInstance().getFeaturePrefs(FeatureName.Component.STREAMER).put(name(), value);
		}

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.STREAMER).put(name(), value);
		}
	}

	private int _streamPort = DEFAULT_STREAM_PORT;

	public FeatureStreamerRayV() throws FeatureNotFoundException
	{
		require(FeatureName.Component.REGISTER);
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		if (!getPrefs().has(Param.RAYV_USER))
			getPrefs().put(Param.RAYV_USER, _feature.Component.REGISTER.getBoxId());
		if (!getPrefs().has(Param.RAYV_PASS))
			getPrefs().put(Param.RAYV_PASS, _feature.Component.REGISTER.getBoxId());

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

	@Override
	public void getUrlByStreamId(String streamId, long playTimeDelta, OnStreamURLReceived onStreamURLReceived)
	{
		Bundle bundle = new Bundle();
		bundle.putString("USER", getPrefs().getString(Param.RAYV_USER));
		bundle.putString("PASS", getPrefs().getString(Param.RAYV_PASS));
		bundle.putString("STREAM_ID", streamId);
		bundle.putInt("BITRATE", getPrefs().getInt(Param.RAYV_STREAM_BITRATE));
		bundle.putInt("PORT", _streamPort);

		String streamUrl = getPrefs().getString(Param.RAYV_STREAM_URL_PATTERN, bundle);
		if (playTimeDelta > 0)
			streamUrl += "&timeshift=" + playTimeDelta;
		onStreamURLReceived.onStreamURL(streamUrl);
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

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.STREAMER;
	}
}

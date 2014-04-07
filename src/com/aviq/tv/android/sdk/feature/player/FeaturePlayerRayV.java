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

import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.rayv.StreamingAgent.Loader;

/**
 * Component feature providing RayV player
 */
public class FeaturePlayerRayV extends FeaturePlayer
{
	public static final String TAG = FeaturePlayerRayV.class.getSimpleName();

	public enum Param
	{
		/**
		 * RayV streamer initialization
		 */
		STREAMER_INI("port=1234\n" + "distributor=vtx\n" + "product_id=test\n" + "allow_external_streams=1\n"
		        + "localhost=127.0.0.1\n" + "thread_pool=10\n" + "udp_port=0\n" + "udp_port_pairs=1\n"
		        + "StopBrokenFrames=0\n" + "BitrateOverhead=50\n" + "ManualBitrateSwitching=1\n" + "MinBitrate=0\n"
		        + "MaxBitrate=0\n" + "MinStartBitrate=0\n" + "MaxStartBitrate=0\n" + "MediaQueueSize=100\n");

		Param(String value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.PLAYER).put(name(), value);
		}
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		// Start streaming agent
		Log.i(TAG, "Start streaming agent");
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
}

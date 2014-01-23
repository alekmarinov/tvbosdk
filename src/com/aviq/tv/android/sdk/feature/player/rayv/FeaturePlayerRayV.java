/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeaturePlayerRayV.java
 * Author:      alek
 * Date:        1 Dec 2013
 * Description: Component feature providing RayV player
 */

package com.aviq.tv.android.sdk.feature.player.rayv;

import android.util.Log;

import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.feature.player.FeaturePlayer;
import com.rayv.StreamingAgent.Loader;

/**
 * Component feature providing RayV player
 */
public class FeaturePlayerRayV extends FeaturePlayer
{
	public static final String TAG = FeaturePlayerRayV.class.getSimpleName();
	private String _streamerIni;

	public FeaturePlayerRayV()
	{
		_dependencies.Components.add(FeatureName.Component.REGISTER);
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
				Loader.startStreamer(_streamerIni);
			}
		}).start();
		super.initialize(onFeatureInitialized);
	}

	public void setStreamerIni(String streamerIni)
	{
		_streamerIni = streamerIni;
	}
}

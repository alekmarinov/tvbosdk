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

import android.os.Bundle;
import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.feature.player.FeaturePlayer;
import com.aviq.tv.android.sdk.feature.register.FeatureRegister;
import com.rayv.StreamingAgent.Loader;

/**
 * Component feature providing RayV player
 */
public class FeaturePlayerRayV extends FeaturePlayer
{
	public static final String TAG = FeaturePlayerRayV.class.getSimpleName();
	private String _streamerIni;

	public enum Param
	{
		/**
		 * Registered RayV user account name
		 */
		RAYV_USER(null),

		/**
		 * Registered RayV account password
		 */
		RAYV_PASS(null),

		/**
		 * RayV stream bitrate
		 */
		RAYV_STREAM_BITRATE(1200),

		/**
		 * Pattern composing channel stream url for RayV CDN provider
		 */
		RAYV_STREAM_URL_PATTERN("http://localhost:1234/RayVAgent/v1/RAYV/${USER}:${PASS}@${STREAM_ID}?streams=${STREAM_ID}:${BITRATE}");

		Param(String value)
		{
			if (value != null)
				Environment.getInstance().getFeaturePrefs(FeatureName.Component.EPG).put(name(), value);
		}

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.EPG).put(name(), value);
		}
	}

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

        try
		{
			FeatureRegister featureRegister = (FeatureRegister) Environment.getInstance().getFeatureComponent(
			        FeatureName.Component.REGISTER);
			getPrefs().put(Param.RAYV_USER, featureRegister.getBoxId());
			getPrefs().put(Param.RAYV_PASS, featureRegister.getBoxId());
			super.initialize(onFeatureInitialized);
		}
        catch (FeatureNotFoundException e)
        {
        	Log.e(TAG, e.getMessage(), e);
    		onFeatureInitialized.onInitialized(this, ResultCode.GENERAL_FAILURE);
        }
	}

	public void play(String channelId)
	{
		Bundle bundle = new Bundle();
		bundle.putString("USER", getPrefs().getString(Param.RAYV_USER));
		bundle.putString("PASS", getPrefs().getString(Param.RAYV_PASS));
		bundle.putString("STREAM_ID", channelId);
		bundle.putInt("BITRATE", getPrefs().getInt(Param.RAYV_STREAM_BITRATE));
		String url = getPrefs().getString(Param.RAYV_STREAM_URL_PATTERN, bundle);
		_player.play(url);
	}

	public void setStreamerIni(String streamerIni)
	{
		_streamerIni = streamerIni;
	}
}

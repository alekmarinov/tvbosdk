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
import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.epg.FeatureEPG;
import com.aviq.tv.android.sdk.feature.epg.Program;
import com.aviq.tv.android.sdk.feature.register.FeatureRegister;

/**
 * RayV specific extension of EPG feature
 */
public class FeatureEPGRayV extends FeatureEPG
{
	public static final String TAG = FeatureEPGRayV.class.getSimpleName();

	public enum Param
	{
		/**
		 * Registered RayV user account name
		 */
		RAYV_USER(null), //1C6F65F9DE8B

		/**
		 * Registered RayV account password
		 */
		RAYV_PASS(null), //1C6F65F9DE8B

		/**
		 * RayV stream bitrate
		 */
		RAYV_STREAM_BITRATE(1200),

		/**
		 * Pattern composing channel stream url for RayV provider
		 */
		RAYV_STREAM_URL_PATTERN("http://localhost:1234/RayVAgent/v1/RAYV/${USER}:${PASS}@${STREAM_ID}?streams=${STREAM_ID}:${BITRATE}");

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

	public FeatureEPGRayV()
	{
		_dependencies.Components.add(FeatureName.Component.REGISTER);
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
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
	/**
	 * @return rayv EPG provider name
	 */
	@Override
    protected FeatureEPG.Provider getEPGProvider()
	{
		return FeatureEPG.Provider.rayv;
	}

	/**
	 * Return stream url for specified channel
	 *
	 * @param channelIndex
	 * @return stream url
	 */
	@Override
    public String getChannelStreamUrl(int channelIndex)
	{
		String channelId = getEpgData().getChannel(channelIndex).getChannelId();
		Bundle bundle = new Bundle();
		bundle.putString("USER", getPrefs().getString(Param.RAYV_USER));
		bundle.putString("PASS", getPrefs().getString(Param.RAYV_PASS));
		bundle.putString("STREAM_ID", channelId);
		bundle.putInt("BITRATE", getPrefs().getInt(Param.RAYV_STREAM_BITRATE));
		return getPrefs().getString(Param.RAYV_STREAM_URL_PATTERN, bundle);
	}

	@Override
    protected Program createProgram(Channel channel)
    {
	    return new ProgramRayV(channel);
    }

	@Override
    protected Channel createChannel(int index)
    {
	    return new ChannelRayV(index);
    }
}

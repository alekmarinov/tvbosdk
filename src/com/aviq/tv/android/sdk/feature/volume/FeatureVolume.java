/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureVolume.java
 * Author:      alek
 * Date:        16 Sep 2014
 * Description: Feature providing increasing/decreasing volume functionality
 */

package com.aviq.tv.android.sdk.feature.volume;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;

/**
 * Feature providing increasing/decreasing volume functionality
 */
public class FeatureVolume extends FeatureComponent
{
	private static final String TAG = FeatureVolume.class.getSimpleName();
	public static final int ON_VOLUME_CHANGED = EventMessenger.ID("ON_VOLUME_CHANGED");
	public static enum VolumeExtras
	{
		CURRENT_LEVEL,
		MAX_LEVEL
	}

	private AudioManager _audioManager;
	private int _maxVolume;

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		_audioManager = (AudioManager) Environment.getInstance().getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
		_maxVolume = _audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		super.initialize(onFeatureInitialized);
	}

	/**
	 * Raise volume one level up
	 */
	public void raise()
	{
		_audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0);
		volumeChanged();
	}

	/**
	 * Lower volume one level down
	 */
	public void lower()
	{
		_audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0);
		volumeChanged();
	}

	/**
	 * @return current volume level
	 */
	public int getCurrentLevel()
	{
		return _audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
	}

	/**
	 * @return max volume level
	 */
	public int getMaxLevel()
	{
		return _maxVolume;
	}

	private void volumeChanged()
	{
		Bundle bundle = new Bundle();
		bundle.putInt(VolumeExtras.CURRENT_LEVEL.name(), getCurrentLevel());
		bundle.putInt(VolumeExtras.MAX_LEVEL.name(), getMaxLevel());
		getEventMessenger().trigger(ON_VOLUME_CHANGED, bundle);
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.VOLUME;
	}
}
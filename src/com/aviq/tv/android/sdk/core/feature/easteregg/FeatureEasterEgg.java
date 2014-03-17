/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureEasterEgg.java
 * Author:      alek
 * Date:        11 Mar 2014
 * Description: Opens the Settings app when detected special key sequence by the RCU
 */

package com.aviq.tv.android.sdk.core.feature.easteregg;

import android.os.Bundle;
import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.EventReceiver;
import com.aviq.tv.android.sdk.core.Key;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Opens the Settings app when detected special key sequence by the RCU
 */
public class FeatureEasterEgg extends FeatureComponent implements EventReceiver
{
	public static final String TAG = FeatureEasterEgg.class.getSimpleName();
	private long lastKeyPress = 0;
	private StringBuffer _sequence = new StringBuffer();
	private long _keyDelay;
	private String _keySequence;
	private String _startPackage;

	public enum Param
	{
		/**
		 * The minimum delay between key presses
		 */
		MIN_KEY_DELAY(1000),

		/**
		 * The expected key sequence for the easter egg
		 */
		KEY_SEQUENCE("YGRB"),

		/**
		 * The expected app package to start
		 */
		START_PACKAGE("com.android.settings");

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.EASTER_EGG).put(name(), value);
		}

		Param(String value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.EASTER_EGG).put(name(), value);
		}
	}

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		Environment.getInstance().getEventMessenger().register(this, Environment.ON_KEY_PRESSED);
		_keyDelay = getPrefs().getInt(Param.MIN_KEY_DELAY);
		_keySequence = getPrefs().getString(Param.KEY_SEQUENCE);
		_startPackage = getPrefs().getString(Param.START_PACKAGE);
		super.initialize(onFeatureInitialized);
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.EASTER_EGG;
	}

	@Override
	public void onEvent(int msgId, Bundle bundle)
	{
		Log.i(TAG, ".onEvent: " + EventMessenger.idName(msgId) + TextUtils.implodeBundle(bundle));
		if (Environment.ON_KEY_PRESSED == msgId)
		{
			long delay = System.currentTimeMillis() - lastKeyPress;
			if (delay > _keyDelay)
			{
				_sequence.setLength(0);
			}
			Key key = Key.valueOf(bundle.getString(Environment.EXTRA_KEY));
			char chr = expectedKeyToChar(key);
			if (chr != '\0')
			{
				_sequence.append(chr);
				Log.e(TAG, "_sequence = " + _sequence + ", _keySequence = " + _keySequence);
				if (_sequence.toString().equals(_keySequence))
				{
					Environment.getInstance().startAppPackage(_startPackage);
				}
				lastKeyPress = System.currentTimeMillis();
			}
		}
	}

	private char expectedKeyToChar(Key key)
	{
		switch (key)
		{
			case RED:
				return 'R';
			case GREEN:
				return 'G';
			case YELLOW:
				return 'Y';
			case BLUE:
				return 'B';
			default:
				return '\0';
		}
	}
}

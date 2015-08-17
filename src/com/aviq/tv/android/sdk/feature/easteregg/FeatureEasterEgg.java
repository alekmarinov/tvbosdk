/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureEasterEgg.java
 * Author:      alek
 * Date:        11 Mar 2014
 * Description: Opens the Settings app when detected special key sequence by the RCU
 */

package com.aviq.tv.android.sdk.feature.easteregg;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import android.os.Bundle;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.EventReceiver;
import com.aviq.tv.android.sdk.core.Key;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;
import com.aviq.tv.android.sdk.core.feature.annotation.Priority;
import com.aviq.tv.android.sdk.feature.rcu.FeatureRCU;
import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Opens the Settings app when detected special key sequence by the RCU
 */
@Priority
@Author("zheliazko")
public class FeatureEasterEgg extends FeatureComponent implements EventReceiver
{
	public static final String TAG = FeatureEasterEgg.class.getSimpleName();

	public static int ON_KEY_SEQUENCE = EventMessenger.ID("ON_KEY_SEQUENCE");
	public static final String EXTRA_KEY_SEQUENCE = "KEY_SEQUENCE";
	private static final int SEQUENCE_PREFIX_NUM_CHARS = 2;
	private long lastKeyPress = 0;
	private StringBuffer _sequence = new StringBuffer();
	private List<String> _sequenceList = new ArrayList<String>();
	private Set<String> _sequencePrefixes = new TreeSet<String>();
	private boolean _inEasterEggMode = false;
	private int _minKeyDelay;

	public static enum Param
	{
		/**
		 * The minimum delay between key presses
		 */
		MIN_KEY_DELAY(3000),;

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.EASTER_EGG).put(name(), value);
		}

		Param(String value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.EASTER_EGG).put(name(), value);
		}
	}

	public FeatureEasterEgg() throws FeatureNotFoundException
	{
		require(FeatureName.Component.RCU);
	}

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		_feature.Component.RCU.getEventMessenger().register(this, FeatureRCU.ON_KEY_PRESSED);
		_minKeyDelay = getPrefs().getInt(Param.MIN_KEY_DELAY);
		super.initialize(onFeatureInitialized);
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.EASTER_EGG;
	}

	public void addEasterEgg(String keySeq)
	{
		_sequenceList.add(keySeq);
		_sequencePrefixes.add(keySeq.substring(0, SEQUENCE_PREFIX_NUM_CHARS));
	}

	@Override
	public void onEvent(int msgId, Bundle bundle)
	{
		super.onEvent(msgId, bundle);
		Log.i(TAG, ".onEvent: " + EventMessenger.idName(msgId) + TextUtils.implodeBundle(bundle));
		if (FeatureRCU.ON_KEY_PRESSED == msgId)
		{
			long delay = System.currentTimeMillis() - lastKeyPress;
			if (delay > _minKeyDelay)
			{
				_sequence.setLength(0);
				_disableEasterEggModeRunnable.run();
			}

			Key key = Key.valueOf(bundle.getString(Environment.EXTRA_KEY));
			char chr = expectedKeyToChar(key);
			if (chr != '\0')
			{
				_sequence.append(chr);

				if (_sequence.length() == SEQUENCE_PREFIX_NUM_CHARS && _sequencePrefixes.contains(_sequence.toString()))
				{
					_inEasterEggMode = true;
					getEventMessenger().postDelayed(_disableEasterEggModeRunnable, _minKeyDelay);
				}

				String keySeq = _sequence.toString();
				if (_sequenceList.contains(keySeq))
				{
					Bundle params = new Bundle();
					params.putString(EXTRA_KEY_SEQUENCE, keySeq);
					getEventMessenger().trigger(ON_KEY_SEQUENCE, params);
					Log.d(TAG, "Sequence " + keySeq + " is present, notify listeners");
				}
				else
				{
					Log.d(TAG, "Sequence " + keySeq + " is not present");
				}

				lastKeyPress = System.currentTimeMillis();
			}
		}
	}

	public boolean isInEasterEggMode()
	{
		return _inEasterEggMode;
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
			case NUM_0:
				return '0';
			case NUM_1:
				return '1';
			case NUM_2:
				return '2';
			case NUM_3:
				return '3';
			case NUM_4:
				return '4';
			case NUM_5:
				return '5';
			case NUM_6:
				return '6';
			case NUM_7:
				return '7';
			case NUM_8:
				return '8';
			case NUM_9:
				return '9';
			default:
				return '\0';
		}
	}

	private Runnable _disableEasterEggModeRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			_inEasterEggMode = false;
		}
	};
}

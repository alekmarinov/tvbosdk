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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.Bundle;
import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.EventReceiver;
import com.aviq.tv.android.sdk.core.Key;
import com.aviq.tv.android.sdk.core.ResultCode;
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

	public static int ON_KEY_SEQUENCE = EventMessenger.ID("ON_KEY_SEQUENCE");
	public static final String EXTRA_KEY_SEQUENCE = "KEY_SEQUENCE";

	private long lastKeyPress = 0;
	private StringBuffer _sequence = new StringBuffer();
	private List<String> _sequenceList = new ArrayList<String>();
	private Map<String, String> _globalSequenceMap = new HashMap<String, String>();

	public enum Param
	{
		/**
		 * The minimum delay between key presses
		 */
		MIN_KEY_DELAY(2000),

		/**
		 * The expected comma-separated key sequences for the easter egg.
		 */
		KEY_SEQUENCES(""),

		/**
		 * Key sequence global to the application using this feature.
		 */
		KEY_SEQUENCE_YGRB("YGRB"),

		/**
		 * The expected app package to start
		 */
		KEY_SEQUENCE_ACTION_YGRB("com.android.settings");

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.EASTER_EGG).put(name(), value);
		}

		Param(String value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.EASTER_EGG).put(name(), value);
		}
	}

	public FeatureEasterEgg()
	{
		Environment.getInstance().getEventMessenger().register(this, Environment.ON_KEY_PRESSED);
	}

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");

		// A list of the application-specific key sequences

		String sequences = getPrefs().getString(Param.KEY_SEQUENCES);
		String[] seqArray = sequences.split(",");
		_sequenceList = Arrays.asList(seqArray);

		// Process global key sequence mapping

		String keySeq = null;
		String keySeqAction = null;

		keySeq = getPrefs().getString(Param.KEY_SEQUENCE_YGRB);
		keySeqAction = getPrefs().getString(Param.KEY_SEQUENCE_ACTION_YGRB);
		_globalSequenceMap.put(keySeq, keySeqAction);

		onFeatureInitialized.onInitialized(this, ResultCode.OK);
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
			if (delay > getPrefs().getInt(Param.MIN_KEY_DELAY))
			{
				_sequence.setLength(0);
			}
			Key key = Key.valueOf(bundle.getString(Environment.EXTRA_KEY));
			char chr = expectedKeyToChar(key);
			if (chr != '\0')
			{
				_sequence.append(chr);
				String keySeq = _sequence.toString();

				if (_globalSequenceMap.containsKey(keySeq))
				{
					Environment.getInstance().startAppPackage(_globalSequenceMap.get(keySeq));
				}
				else if (_sequenceList.contains(keySeq))
				{
					Bundle params = new Bundle();
					params.putString(EXTRA_KEY_SEQUENCE, keySeq);
					getEventMessenger().trigger(ON_KEY_SEQUENCE, params);
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
}

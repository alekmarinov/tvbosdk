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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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

	/** 738 = SET */
	public static final String KEY_SEQ_SET = "RR738RR";
	/** 564 = LOG */
	public static final String KEY_SEQ_LOG = "RR564RR";
	/** 272 = ASB -> Auto StandBy */
	public static final String KEY_SEQ_AUTO_STANDBY = "RR272RR";
	/** 847 = VHR */
	public static final String KEY_SEQ_VHR = "RR847RR";

	public static int ON_KEY_SEQUENCE = EventMessenger.ID("ON_KEY_SEQUENCE");
	public static final String EXTRA_KEY_SEQUENCE = "KEY_SEQUENCE";

	private static final int SEQUENCE_PREFIX_NUM_CHARS = 2;

	private long lastKeyPress = 0;
	private StringBuffer _sequence = new StringBuffer();
	private List<String> _sequenceList = new ArrayList<String>();
	private Map<String, String> _globalSequenceMap = new HashMap<String, String>();
	private Set<String> _sequencePrefixes = new TreeSet<String>();
	private boolean _inEasterEggMode = false;
	private int _minKeyDelay;

	public enum Param
	{
		/**
		 * The minimum delay between key presses
		 */
		MIN_KEY_DELAY(3000),

		/**
		 * The expected comma-separated key sequences for the easter egg.
		 */
		KEY_SEQUENCES(""),

		/**
		 * Key sequences global to the application using this feature.
		 */
		KEY_SEQUENCE_SET(KEY_SEQ_SET), KEY_SEQUENCE_LOG(KEY_SEQ_LOG), KEY_SEQUENCE_AUTO_STANDBY(KEY_SEQ_AUTO_STANDBY), KEY_SEQUENCE_VHR(
		        KEY_SEQ_VHR),

		/**
		 * The expected app package to start
		 */
		KEY_SEQUENCE_ACTION_SET("com.android.settings");

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
		if (sequences.length() > 0)
		{
			String[] seqArray = sequences.split(",");
			_sequenceList.addAll(Arrays.asList(seqArray));
			for (String seq : seqArray)
				_sequencePrefixes.add(seq.substring(0, SEQUENCE_PREFIX_NUM_CHARS));
		}

		// Process global key sequence mapping

		String keySeq = null;
		String keySeqAction = null;

		// Settings
		keySeq = getPrefs().getString(Param.KEY_SEQUENCE_SET);
		keySeqAction = getPrefs().getString(Param.KEY_SEQUENCE_ACTION_SET);
		_globalSequenceMap.put(keySeq, keySeqAction);
		_sequencePrefixes.add(keySeq.substring(0, SEQUENCE_PREFIX_NUM_CHARS));

		// Logcat
		keySeq = getPrefs().getString(Param.KEY_SEQUENCE_LOG);
		_sequenceList.add(keySeq);
		_sequencePrefixes.add(keySeq.substring(0, SEQUENCE_PREFIX_NUM_CHARS));

		// Auto StandBy
		keySeq = getPrefs().getString(Param.KEY_SEQUENCE_AUTO_STANDBY);
		_sequenceList.add(keySeq);
		_sequencePrefixes.add(keySeq.substring(0, SEQUENCE_PREFIX_NUM_CHARS));

		// View Hierarchy
		keySeq = getPrefs().getString(Param.KEY_SEQUENCE_VHR);
		_sequenceList.add(keySeq);
		_sequencePrefixes.add(keySeq.substring(0, SEQUENCE_PREFIX_NUM_CHARS));

		_minKeyDelay = getPrefs().getInt(Param.MIN_KEY_DELAY);

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

				if (_globalSequenceMap.containsKey(keySeq))
				{
					Environment.getInstance().startAppPackage(_globalSequenceMap.get(keySeq));
				}
				else if (_sequenceList.contains(keySeq))
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

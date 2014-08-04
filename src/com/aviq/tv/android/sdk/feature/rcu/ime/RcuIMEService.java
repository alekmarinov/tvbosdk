/**
 * Copyright (c) 2003-2014, AVIQ Systems AG
 *
 * Project:     LiveTV
 * Filename:    RcuIMEService.java
 * Author:      alek
 * Date:        04.04.2012, 10.07.2014
 * Description:
 */
package com.aviq.tv.android.sdk.feature.rcu.ime;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.inputmethodservice.InputMethodService;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.aviq.tv.android.sdk.core.Key;
import com.aviq.tv.android.sdk.feature.rcu.FeatureRCU;
import com.aviq.tv.android.sdk.feature.system.FeatureSystem;

public abstract class RcuIMEService extends InputMethodService
{
	private static final String TAG = RcuIMEService.class.getSimpleName();
	public static final String BROADCAST_ACTION_SLEEP = "com.aviq.tv.android.sdk.feature.rcu.ime.broadcast.SLEEP";
	private static final SparseArray<String> _sRecs = new SparseArray<String>();
	private static final SparseArray<String> _nRecs = new SparseArray<String>();
	private static final int ROTATE_INTERVAL = 1000;
	private static final int MAX_FREQ_GLOBAL_INTERVAL = 110;
	private static final String MAX_FREQ_KEYS = "UP,DOWN";
	private static final String MAX_FREQ_INTERVALS = "175,175";

	static
	{
		_sRecs.put(Key.NUM_1.ordinal(), "@/1_!?");
		_sRecs.put(Key.NUM_2.ordinal(), "abc2ABC");
		_sRecs.put(Key.NUM_3.ordinal(), "def3DEF");
		_sRecs.put(Key.NUM_4.ordinal(), "ghi4GHI");
		_sRecs.put(Key.NUM_5.ordinal(), "jkl5JKL");
		_sRecs.put(Key.NUM_6.ordinal(), "mno6MNO");
		_sRecs.put(Key.NUM_7.ordinal(), "pqrs7PQRS");
		_sRecs.put(Key.NUM_8.ordinal(), "tuv8TUV");
		_sRecs.put(Key.NUM_9.ordinal(), "wxyz9WXYZ");
		_sRecs.put(Key.NUM_0.ordinal(), " 0+");
		_sRecs.put(Key.CHARACTERS.ordinal(), "*.-,!:=/\\#$%^&'()");

		_nRecs.put(Key.NUM_1.ordinal(), "1");
		_nRecs.put(Key.NUM_2.ordinal(), "2");
		_nRecs.put(Key.NUM_3.ordinal(), "3");
		_nRecs.put(Key.NUM_4.ordinal(), "4");
		_nRecs.put(Key.NUM_5.ordinal(), "5");
		_nRecs.put(Key.NUM_6.ordinal(), "6");
		_nRecs.put(Key.NUM_7.ordinal(), "7");
		_nRecs.put(Key.NUM_8.ordinal(), "8");
		_nRecs.put(Key.NUM_9.ordinal(), "9");
		_nRecs.put(Key.NUM_0.ordinal(), "0 +");
		_nRecs.put(Key.CHARACTERS.ordinal(), ".-");
	};

	private InputConnection _inputConnection;
	private boolean _editMode = false;
	private boolean _isNumeric = false;
	private long _lastPressed = 0;
	private Key _lastKey;
	private int _keyIdx = 0;
	private int _keyRotateInterval;
	private int _keyMaxFreqInterval;

	private long _lastEventTime = 0;
	private FeatureSystem _featureSystem;
	private FeatureRCU _featureRCU;
	private SharedPreferences _sharedPrefs;
	private List<Key> _slowKeys = new ArrayList<Key>();
	private List<Integer> _slowKeyFreqs = new ArrayList<Integer>();

	protected abstract FeatureRCU createFeatureRCU();

	@Override
	public void onCreate()
	{
		Log.d(TAG, ".onCreate");
		//
		super.onCreate();
		this.setCandidatesViewShown(false);

		_featureRCU = createFeatureRCU();
		_featureRCU.initialize(null);
		_featureSystem = new FeatureSystem(FeatureSystem.DEFAULT_HOST, FeatureSystem.DEFAULT_PORT);
		_featureSystem.initialize(null);

		_sharedPrefs = getSharedPreferences("RcuIME", Context.MODE_PRIVATE);
		_keyRotateInterval = getIntPref("ROTATE_INTERVAL", ROTATE_INTERVAL);
		_keyMaxFreqInterval = getIntPref("MAX_FREQ_GLOBAL_INTERVAL", MAX_FREQ_GLOBAL_INTERVAL);
		String keyListStr = getStrPref("MAX_FREQ_KEYS", MAX_FREQ_KEYS);
		String[] keyListArr = keyListStr.split(",");
		for (String keyName : keyListArr)
		{
			Key key = Key.valueOf(keyName);
			if (key == null)
				Log.e(TAG, "Unknown key name `" + keyName + "' in pref MAX_FREQ_KEYS");
			else
			{
				Log.i(TAG, "Registering key `" + key + "' for slowing down");
				_slowKeys.add(key);
			}
		}

		String keyListFreqStr = getStrPref("MAX_FREQ_INTERVALS", MAX_FREQ_INTERVALS);
		String[] keyListFreqArr = keyListFreqStr.split(",");
		if (keyListFreqArr.length != keyListArr.length)
		{
			Log.e(TAG, "Number of elements " + keyListFreqArr.length
			        + " in pref MAX_FREQ_INTERVALS should be equal to number of elements " + keyListArr.length
			        + " in MAX_FREQ_KEYS");
			_slowKeys.clear();
		}
		else
			for (String keyFreqStr : keyListFreqArr)
			{
				try
				{
					Integer keyFreq = Integer.valueOf(keyFreqStr);
					Log.i(TAG, "Key `" + _slowKeys.get(_slowKeyFreqs.size()) + "' will get maximum " + keyFreq
					        + " ms frequency");
					_slowKeyFreqs.add(keyFreq);
				}
				catch (NumberFormatException nfe)
				{
					Log.e(TAG, nfe.getMessage(), nfe);
					_slowKeys.clear();
					_slowKeyFreqs.clear();
					break;
				}
			}
	}

	private int getIntPref(String name, int defValue)
	{
		int value = _sharedPrefs.getInt(name, defValue);
		Editor edit = _sharedPrefs.edit();
		edit.putInt(name, value);
		edit.commit();
		return value;
	}

	private String getStrPref(String name, String defValue)
	{
		String value = _sharedPrefs.getString(name, defValue);
		Editor edit = _sharedPrefs.edit();
		edit.putString(name, value);
		edit.commit();
		return value;
	}

	private InputConnection getInputConnection()
	{
		if (null == _inputConnection)
			_inputConnection = this.getCurrentInputConnection();

		Log.d(TAG, ".getInputConnection() -> " + _inputConnection);
		return _inputConnection;
	}

	@Override
	public void onBindInput()
	{
		Log.d(TAG, ".onBindInput");
		super.onBindInput();
		_inputConnection = this.getCurrentInputConnection();
	}

	/**
	 * This is the main point where we do our initialization of the input method
	 * to begin operating on an application. At this point we have been bound to
	 * the client, and are now receiving all of the detailed information about
	 * the target of our edits.
	 */
	@Override
	public void onStartInput(EditorInfo attribute, boolean restarting)
	{
		Log.d(TAG, ".onStartInput: attribute = " + attribute + ", restarting = " + restarting);
		super.onStartInput(attribute, restarting);
		_editMode = (attribute.inputType & EditorInfo.TYPE_CLASS_TEXT) != 0;
		_isNumeric = (attribute.inputType & EditorInfo.TYPE_CLASS_NUMBER) != 0
		        || (attribute.fieldId == 2131492944 || attribute.fieldId == 2131492946
		                || attribute.fieldId == 2131492948 || attribute.fieldId == 2131492950);
		Log.d(TAG, "onStartInput: _editMode = " + _editMode + ", fieldId = " + attribute.fieldId + ", _isNumeric = "
		        + _isNumeric);
	}

	@Override
	public void onInitializeInterface()
	{
		Log.d(TAG, ".onInitializeInterface");
	}

	/**
	 * This is called when the user is done editing a field. We can use this to
	 * reset our state.
	 */
	@Override
	public void onFinishInput()
	{
		Log.d(TAG, ".onFinishInput");
		super.onFinishInput();
	}

	private void delete()
	{
		Log.d(TAG, ".delete");
		getInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
		getInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
		getInputConnection().commitText("", 1);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		Key key = _featureRCU.getKey(keyCode);
		Log.d(TAG, ".onKeyDown: scan code = " + event.getScanCode() + ", keyCode=" + keyCode + ", key = " + key);

		if (Key.SLEEP.equals(key))
		{
			Log.i(TAG, "Sending broadcast " + BROADCAST_ACTION_SLEEP);
			Intent intent = new Intent();
			intent.setAction(BROADCAST_ACTION_SLEEP);
			sendBroadcast(intent);
			// register for the broadcasted
			// intent instead handling SLEEP key directly
			return true;
		}

		int freqInterval = _keyMaxFreqInterval;
		int keyIdx = _slowKeys.indexOf(key);
		if (keyIdx >= 0)
			freqInterval = _slowKeyFreqs.get(keyIdx);

		event.startTracking();
		if (event.getEventTime() - _lastEventTime < freqInterval)
		{
			return true;
		}
		else
		{
			_lastEventTime = event.getEventTime();
		}

		switch (key)
		{
			case BACK:
			case LEFT:
			case RIGHT:
			case UP:
			case DOWN:
			case OK:
			case VOLUME_UP:
			case VOLUME_DOWN:
				return false;
		}

		if (_editMode)
		{
			String chars = _isNumeric ? _nRecs.get(key.ordinal()) : _sRecs.get(key.ordinal());

			if (Key.DELETE.equals(key))
			{
				delete();
				return true;
			}

			if (chars != null)
			{
				Log.i(TAG, "Delta: " + (int) (SystemClock.uptimeMillis() - _lastPressed));
				if (key.equals(_lastKey) && ((int) (SystemClock.uptimeMillis() - _lastPressed)) < _keyRotateInterval)
				{
					_keyIdx++;
					delete();
				}
				else
				{
					_keyIdx = 0;
				}
				_keyIdx = _keyIdx % chars.length();
				char c = chars.charAt(_keyIdx);
				getInputConnection().commitText("" + c, 1);
				_lastPressed = SystemClock.uptimeMillis();
				_lastKey = key;
				return true;
			}
			else
			{
				Log.w(TAG, "Unmapped: " + key);
			}
		}
		return false;
	}
}

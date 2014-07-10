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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Key;
import com.aviq.tv.android.sdk.feature.rcu.FeatureRCU;
import com.aviq.tv.android.sdk.feature.system.FeatureSystem;

public abstract class RcuIMEService extends InputMethodService
{
	private static final String TAG = RcuIMEService.class.getSimpleName();
	private static final String INTENT_ACTION_DISABLE_STANDBY = "com.aviq.tipc.android.ime.DISABLE_STANDBY";
	private static final String INTENT_ACTION_ENABLE_STANDBY = "com.aviq.tipc.android.ime.ENABLE_STANDBY";
	private static final SparseArray<String> _sRecs = new SparseArray<String>();
	private static final SparseArray<String> _nRecs = new SparseArray<String>();
	private static final int KEY_ROTATE_INTERVAL = 1000;

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

	private long _lastEventTime = 0;
	private boolean _enableStandby = false;
	private Handler _handler;
	private FeatureSystem _featureSystem;
	private FeatureRCU _featureRCU;

	protected abstract FeatureRCU createFeatureRCU();

	@Override
	public void onCreate()
	{
		Log.d(TAG, ".onCreate");
		//
		super.onCreate();
		this.setCandidatesViewShown(false);

		_handler = new Handler();

		new Environment(this);
		_featureRCU = createFeatureRCU();
		_featureRCU.initialize(null);
		_featureSystem = new FeatureSystem();
		_featureSystem.initialize(null);

		// Register custom application lifecycle hook
		IntentFilter filter = new IntentFilter();
		filter.addAction(INTENT_ACTION_DISABLE_STANDBY);
		filter.addAction(INTENT_ACTION_ENABLE_STANDBY);
		registerReceiver(_globalReceiver, filter);
	}

	private InputConnection getInputConnection()
	{
		if (null == _inputConnection)
			_inputConnection = this.getCurrentInputConnection();

		Log.d(TAG, ".getInputConnection() -> " + _inputConnection);
		return _inputConnection;
	}

	@Override
	public void onDestroy()
	{
		Log.d(TAG, ".onDestroy()");
		super.onDestroy();
		unregisterReceiver(_globalReceiver);
	}

	@Override
	public void onInitializeInterface()
	{
		Log.d(TAG, ".onInitializeInterface");
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
			Log.i(TAG, "_enableStandby = " + _enableStandby);

			if (_enableStandby)
			{
				_handler.removeCallbacks(_detectStandbyLeave);
				_handler.post(_detectStandbyLeave);

				_featureSystem.command("input keyevent 26");
				return true;
			}
		}

		event.startTracking();
		if (event.getEventTime() - _lastEventTime < 110)
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
				if (key.equals(_lastKey) && ((int) (SystemClock.uptimeMillis() - _lastPressed)) < KEY_ROTATE_INTERVAL)
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

	private BroadcastReceiver _globalReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();
			Log.i(TAG, ".onReceive: action = " + action);

			if (INTENT_ACTION_DISABLE_STANDBY.equals(action))
			{
				_enableStandby = false;
				Log.i(TAG, "StandBy disabled");
			}
			else if (INTENT_ACTION_ENABLE_STANDBY.equals(action))
			{
				_enableStandby = true;
				Log.i(TAG, "StandBy enabled");
			}
		}
	};

	private final Runnable _detectStandbyLeave = new Runnable()
	{
		private long _lastCurrentTime = 0;

		@Override
		public void run()
		{
			if (_lastCurrentTime == 0)
			{
				// Set last current time to now
				_lastCurrentTime = System.currentTimeMillis();
			}

			// time left since the last current time was set
			long timeLeft = (System.currentTimeMillis() - _lastCurrentTime);
			Log.i(TAG, "_detectStandByExit: timeLeft = " + timeLeft);
			if (timeLeft > 1500)
			{
				Log.i(TAG, "_detectStandByExit: Detected leaving standing by");

				// self suicide
				Log.w(TAG, "Comitting suicide");
				_featureSystem.command("killall " + getPackageName());
				_lastCurrentTime = 0;
			}
			else
			{
				// loop with one second delay and determine if the
				// elapsed time is as expected unless standby has
				// interrupted the loop
				_lastCurrentTime = System.currentTimeMillis();
				_handler.postDelayed(this, 1000);
			}
		}
	};
}
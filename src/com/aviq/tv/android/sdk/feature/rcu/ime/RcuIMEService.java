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

import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.aviq.tv.android.sdk.core.Key;
import com.aviq.tv.android.sdk.feature.rcu.FeatureRCU;

public abstract class RcuIMEService extends InputMethodService
{
	private static final String TAG = RcuIMEService.class.getSimpleName();
	public static final String BROADCAST_ACTION_SLEEP = "com.aviq.tv.android.sdk.feature.rcu.ime.broadcast.SLEEP";

	private InputConnection _inputConnection;
	private FeatureRCU _featureRCU;

	protected abstract FeatureRCU createFeatureRCU();

	@Override
	public void onCreate()
	{
		super.onCreate();
		Log.d(TAG, ".onCreate");
		this.setCandidatesViewShown(false);

		_featureRCU = createFeatureRCU();
		_featureRCU.initialize(null);
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
	}

	@Override
	public void onInitializeInterface()
	{
		Log.d(TAG, ".onInitializeInterface");
		super.onInitializeInterface();
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

		return false;
	}
}

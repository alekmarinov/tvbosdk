/**
 * Copyright (c) 2007-2015, Intelibo Ltd
 *
 * Project:     tvbosdk
 * Filename:    CommandSendKey.java
 * Author:      Hari
 * Date:        30.06.2015 ã.
 * Description: Injects RCU key events to application
 */

package com.aviq.tv.android.sdk.feature.command.handlers;

import java.io.IOException;

import android.app.Instrumentation;
import android.os.Bundle;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Key;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.command.CommandHandler;
import com.aviq.tv.android.sdk.feature.rcu.FeatureRCU;

/**
 * Injects RCU key events to application
 */
public class CommandSendKey implements CommandHandler
{
	private static final String TAG = CommandSendKey.class.getSimpleName();
	public static final String ID = "SEND_KEY";

	public static enum Extras
	{
		KEY, CODE
	}

	private Instrumentation _instrumentation = new Instrumentation();
	private FeatureRCU _featureRCU;

	public CommandSendKey(FeatureRCU featureRCU)
	{
		_featureRCU = featureRCU;
	}

	@Override
	public void execute(Bundle params, final OnResultReceived onResultReceived)
	{
		String keyName = params.getString(Extras.KEY.name());
		String code = params.getString(Extras.CODE.name());
		Log.i(TAG, ".execute: KEY = " + keyName + ", CODE = " + code);
		final Key key = Key.valueOf(keyName);
		final int keyCode = code != null ? Integer.valueOf(code) : _featureRCU.getCode(key);

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				// inject key event via Android instrumentation
				_instrumentation.sendKeyDownUpSync(keyCode);

				if (Key.SLEEP.equals(key))
				{
					try
					{
						Runtime.getRuntime().exec("input keyevent 26");
					}
					catch (IOException e)
					{
						Log.e(TAG, e.getMessage(), e);
					}
				}

				// call back with success
				Environment.getInstance().runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						onResultReceived.onReceiveResult(FeatureError.OK, null);
					}
				});
			}
		}).start();
	}

	@Override
	public String getId()
	{
		return ID;
	}
}

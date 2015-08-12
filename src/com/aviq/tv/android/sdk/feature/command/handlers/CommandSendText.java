/**
 * Copyright (c) 2007-2015, Intelibo Ltd
 *
 * Project:     tvbosdk
 * Filename:    CommandSendText.java
 * Author:      alek
 * Date:        30.06.2015 ã.
 * Description: Injects text to application
 */

package com.aviq.tv.android.sdk.feature.command.handlers;

import android.app.Instrumentation;
import android.os.Bundle;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.command.CommandHandler;

/**
 * Injects text to application
 */
public class CommandSendText implements CommandHandler
{
	private static final String TAG = CommandSendText.class.getSimpleName();
	public static final String ID = "SEND_TEXT";

	public static enum Extras
	{
		TEXT
	}

	private Instrumentation _instrumentation = new Instrumentation();

	@Override
	public void execute(Bundle params, final OnResultReceived onResultReceived)
	{
		final String text = params.getString(Extras.TEXT.name());
		Log.i(TAG, ".execute: TEXT = " + text);

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				// inject key event via Android instrumentation
				_instrumentation.sendStringSync(text);

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

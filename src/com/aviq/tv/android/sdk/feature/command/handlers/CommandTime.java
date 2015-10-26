/**
 * Copyright (c) 2007-2015, Intelibo Ltd
 *
 * Project:     tvbosdk
 * Filename:    CommandTime.java
 * Author:      alek
 * Date:        30.06.2015 ã.
 * Description: Command returning current time
 */

package com.aviq.tv.android.sdk.feature.command.handlers;

import java.util.Calendar;

import android.os.Bundle;

import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.command.CommandHandler;

/**
 * Injects text to application
 */
public class CommandTime implements CommandHandler
{
	private static final String TAG = CommandTime.class.getSimpleName();
	public static final String ID = "TIME";

	@Override
	public void execute(Bundle params, final OnResultReceived onResultReceived)
	{
		Log.i(TAG, ".execute:");
		onResultReceived.onReceiveResult(FeatureError.OK, Calendar.getInstance().getTime().getTime());
	}

	@Override
	public String getId()
	{
		return ID;
	}
}

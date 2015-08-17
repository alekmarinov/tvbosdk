/**
 * Copyright (c) 2007-2015, Intelibo Ltd
 *
 * Project:     tvbosdk
 * Filename:    CommandFeatures.java
 * Author:      alek
 * Date:        30.06.2015 ã.
 * Description: Return application feature structure
 */

package com.aviq.tv.android.sdk.feature.command.handlers;

import org.json.JSONException;

import android.os.Bundle;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.command.CommandHandler;

/**
 * Injects text to application
 */
public class CommandFeatures implements CommandHandler
{
	private static final String TAG = CommandFeatures.class.getSimpleName();
	public static final String ID = "FEATURES";

	@Override
	public void execute(Bundle params, final OnResultReceived onResultReceived)
	{
		Log.i(TAG, ".execute:");
		try
		{
			onResultReceived.onReceiveResult(FeatureError.OK, Environment.getInstance().getFeatureManager().toJson());
		}
		catch (JSONException e)
		{
			onResultReceived.onReceiveResult(
			        new FeatureError(Environment.getInstance().getFeatureComponent(FeatureName.Component.COMMAND), e),
			        null);
		}
	}

	@Override
	public String getId()
	{
		return ID;
	}
}

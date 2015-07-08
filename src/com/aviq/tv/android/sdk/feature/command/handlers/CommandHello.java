/**
 * Copyright (c) 2007-2015, Intelibo Ltd
 *
 * Project:     tvbosdk
 * Filename:    CommandHello.java
 * Author:      alek
 * Date:        06.07.2015 ã.
 * Description: Hello command responding back with 'hi' used to check if the service is alive
 */

package com.aviq.tv.android.sdk.feature.command.handlers;

import android.os.Bundle;

import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.command.CommandHandler;

/**
 * Hello command responding back with 'hi' used to check if the service is alive
 */
public class CommandHello implements CommandHandler
{
	public static final String ID = "HELLO";
	public static final String RESULT = "HI";

	@Override
    public void execute(Bundle params, final OnResultReceived onResultReceived)
    {
		onResultReceived.onReceiveResult(FeatureError.OK, RESULT);
    }

	@Override
	public String getId()
	{
		return ID;
	}
}

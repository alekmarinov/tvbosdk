/**
 * Copyright (c) 2007-2015, Intelibo Ltd
 * 
 * Project:     tvbosdk
 * Filename:    CommandHandler.java
 * Author:      Hari
 * Date:        24.06.2015 ã.
 * Description: Command handler interface
 */

package com.aviq.tv.android.sdk.feature.command;

import android.os.Bundle;

import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;

/**
 * Command handler interface
 */
public interface CommandHandler
{
	void execute(Bundle params, OnResultReceived onResultReceived);
	String getId();
}

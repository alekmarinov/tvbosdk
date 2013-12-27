/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    BaseService.java
 * Author:      alek
 * Date:        15 Oct 2013
 * Description: Base for all application intent services
 */

package com.aviq.tv.android.sdk.core.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.ResultReceiver;

/**
 * Base for all application intent services
 */
public abstract class BaseService extends IntentService
{
	public static final String EXTRA_RESULT_RECEIVER = "EXTRA_RESULT_RECEIVER";

	public BaseService(String arg0)
	{
		super(arg0);
	}

	@Override
	protected final void onHandleIntent(Intent intent)
	{
		ResultReceiver resultReceiver = (ResultReceiver) intent.getParcelableExtra(EXTRA_RESULT_RECEIVER);
		onHandleIntent(intent, resultReceiver);
	}

	protected abstract void onHandleIntent(Intent intent, ResultReceiver resultReceiver);
}

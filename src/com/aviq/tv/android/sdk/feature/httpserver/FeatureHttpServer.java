/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureHttpServer.java
 * Author:      alek
 * Date:        1 Dec 2013
 * Description: Component feature providing http server
 */

package com.aviq.tv.android.sdk.feature.httpserver;

import java.io.IOException;

import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;

/**
 * Component feature providing http server
 *
 */
public class FeatureHttpServer extends FeatureComponent
{
	public static final String TAG = FeatureHttpServer.class.getSimpleName();

	private HttpServer _httpServer;

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		super.initialize(onFeatureInitialized);

		// Start HTTP server
		Log.i(TAG, "Start HTTP server");
		_httpServer = new HttpServer(Environment.getInstance().getContext());
		try
		{
			_httpServer.create();
			onFeatureInitialized.onInitialized(this, ResultCode.OK);
		}
		catch (IOException e)
		{
			Log.e(TAG, e.getMessage(), e);
			onFeatureInitialized.onInitialized(this, ResultCode.GENERAL_FAILURE);
		}
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.HTTP_SERVER;
	}

	public int getListenPort()
	{
		return _httpServer.getPort();
	}
}

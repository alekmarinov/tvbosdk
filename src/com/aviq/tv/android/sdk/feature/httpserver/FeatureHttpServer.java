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

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;

/**
 * Component feature providing http server
 */
@Author("alek")
public class FeatureHttpServer extends FeatureComponent
{
	public static final String TAG = FeatureHttpServer.class.getSimpleName();

	private HttpServer _httpServer;

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		// Start HTTP server
		Log.i(TAG, "Start HTTP server");
		_httpServer = new HttpServer(Environment.getInstance());
		try
		{
			_httpServer.create();
			_httpServer.start();
			super.initialize(onFeatureInitialized);
		}
		catch (IOException e)
		{
			Log.e(TAG, e.getMessage(), e);
			onFeatureInitialized.onInitialized(new FeatureError(this, e));
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

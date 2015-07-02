/**
 * Copyright (c) 2007-2015, Intelibo Ltd
 *
 * Project:     tvbosdk
 * Filename:    FeatureHttpServerJetty.java
 * Author:      Hari
 * Date:        01.07.2015 ã.
 * Description: jetty based HTTP server feature
 */

package com.aviq.tv.android.sdk.feature.httpserver.jetty;

import javax.servlet.Servlet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;

import android.os.Bundle;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventReceiver;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;
import com.aviq.tv.android.sdk.feature.httpserver.FeatureHttpServer;

/**
 * jetty based HTTP server feature
 */
@Author("hari")
public class FeatureHttpServerJetty extends FeatureHttpServer
{
	public static final String TAG = FeatureHttpServerJetty.class.getSimpleName();

	public static enum Param
	{
		/**
		 * HTTP port number
		 */
		PORT(8080);

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.HTTP_SERVER).put(name(), value);
		}
	}

	private Server _server;

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");

		// Configure HTTP server
		final int port = getPrefs().getInt(Param.PORT);
		_server = new Server(port);
		super.initialize(onFeatureInitialized);

		Environment.getInstance().getEventMessenger().register(new EventReceiver()
		{
			@Override
			public void onEvent(int msgId, Bundle bundle)
			{
				try
				{
					Log.i(TAG, "Starting jetty http server on port " + port);
					// Start HTTP server
					_server.start();
				}
				catch (Exception e)
				{
					Log.e(TAG, e.getMessage(), e);
				}

				Environment.getInstance().getEventMessenger().unregister(this, Environment.ON_LOADED);
			}
		}, Environment.ON_LOADED);
	}

	public void setHandler(Class<? extends Servlet> servletClass, String path)
	{
		ServletHandler handler = new ServletHandler();
		_server.setHandler(handler);
		handler.addServletWithMapping(servletClass, path);
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.HTTP_SERVER;
	}
}

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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.Servlet;

import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletHandler;

import android.os.Bundle;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventReceiver;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;
import com.aviq.tv.android.sdk.feature.httpserver.FeatureHttpServer;
import com.aviq.tv.android.sdk.utils.Files;

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
	private HandlerList _handlers = new HandlerList();

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
					_server.setHandler(_handlers);

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

	public void setServlet(Class<? extends Servlet> servletClass, String path)
	{
		ServletHandler handler = new ServletHandler();
		handler.addServletWithMapping(servletClass, path);
		_handlers.addHandler(handler);
	}

	public void setStaticContext(String staticDir, String contextPath)
	{
		Log.i(TAG, ".setStaticContext: staticDir = " + staticDir + ", path = " + contextPath);
		ContextHandler contextHandler = new ContextHandler();
		contextHandler.setContextPath(contextPath);
		contextHandler.setResourceBase(staticDir);
		contextHandler.setClassLoader(Thread.currentThread().getContextClassLoader());

		ResourceHandler resourceHandler = new ResourceHandler();
		resourceHandler.setDirectoriesListed(true);
		resourceHandler.setWelcomeFiles(new String[]
		{ "index.html" });

		HandlerList handlers = new HandlerList();
		handlers.setHandlers(new Handler[]
		{ resourceHandler, new DefaultHandler() });

		contextHandler.setHandler(handlers);
		_handlers.addHandler(contextHandler);
	}

	/**
	 * Extracts zip file to directory in files/{contextPath}/{app version} and adds
	 * http context {contextPath} serving app's static files
	 *
	 * @param inputStreamToZipFile
	 * @param appName
	 * @param contextPath
	 * @throws IOException
	 */
	public void deployStaticApp(InputStream inputStreamToZipFile, String contextPath) throws IOException
	{
		String appName = Files.baseName(contextPath);
		File appDir = new File(Environment.getInstance().getFilesDir(), appName);

		String appVersion = Environment.getInstance().getBuildVersion();
		File versionDir = new File(appDir, appVersion);
		if (!versionDir.isDirectory())
		{
			FileUtils.forceMkdir(appDir);

			// clean old version directories
			FileUtils.cleanDirectory(appDir);

			// extract zip file to appDir with current app version
			Files.unzip(inputStreamToZipFile, versionDir.getAbsolutePath());
		}
		setStaticContext(versionDir.getAbsolutePath(), contextPath);
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.HTTP_SERVER;
	}
}

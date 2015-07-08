/**
 * Copyright (c) 2007-2015, Intelibo Ltd
 *
 * Project:     tvbosdk
 * Filename:    FeatureCommand.java
 * Author:      Hari
 * Date:        24 June 2015
 * Description: Feature implementing common command interface
 */

package com.aviq.tv.android.sdk.feature.command;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import android.os.Bundle;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.command.handlers.CommandHello;
import com.aviq.tv.android.sdk.feature.command.handlers.CommandSendKey;
import com.aviq.tv.android.sdk.feature.httpserver.jetty.FeatureHttpServerJetty;

/**
 * Feature implementing common command interface
 */
@Author("hari")
public class FeatureCommand extends FeatureComponent
{
	public static final String TAG = FeatureCommand.class.getSimpleName();
	private Map<String, CommandHandler> _commandHandlers = new HashMap<String, CommandHandler>();
	private static final String HTTP_CONTEXT = "/command";

	public FeatureCommand() throws FeatureNotFoundException
	{
		require(FeatureName.Component.RCU);
		require(FeatureName.Component.HTTP_SERVER);
	}

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");

		// add global command handlers
		addCommandHandler(new CommandHello());
		addCommandHandler(new CommandSendKey(_feature.Component.RCU));

		if (_feature.Component.HTTP_SERVER instanceof FeatureHttpServerJetty)
		{
			// add handler to http server
			FeatureHttpServerJetty featureHttpServer = (FeatureHttpServerJetty) _feature.Component.HTTP_SERVER;
			featureHttpServer.setServlet(JettyHttpHandlerCommand.class, HTTP_CONTEXT + "/*");
		}

		super.initialize(onFeatureInitialized);
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.COMMAND;
	}

	/**
	 * Registers new command handler
	 *
	 * @param commandHandler
	 */
	public void addCommandHandler(CommandHandler commandHandler)
	{
		if (_commandHandlers.get(commandHandler.getId()) != null)
		{
			throw new RuntimeException("Command id " + commandHandler.getId() + " is already defined");
		}
		_commandHandlers.put(commandHandler.getId(), commandHandler);
	}

	/**
	 * Removes existing command handler
	 *
	 * @param commandHandler
	 */
	public void removeCommandHandler(CommandHandler commandHandler)
	{
		_commandHandlers.remove(commandHandler.getId());
	}

	/**
	 * Executes command handler specified by cmdId
	 *
	 * @param cmdId
	 *            command handler id
	 * @param params
	 *            Bundle with command params
	 * @param onResultReceived
	 *            callback on command execution finished
	 * @throws CommandNotFoundException
	 */
	public void execute(String cmdId, Bundle params, OnResultReceived onResultReceived)
	{
		CommandHandler commandHandler = _commandHandlers.get(cmdId);
		if (commandHandler == null)
		{
			onResultReceived.onReceiveResult(new CommandNotFoundException(cmdId), null);
		}
		else
		{
			commandHandler.execute(params, onResultReceived);
		}
	}

	/**
	 * HTTP handler to jetty web server for http based commands execution
	 */
	public static class JettyHttpHandlerCommand extends HttpServlet
	{
		private static final long serialVersionUID = -9072456498681933444L;

		@Override
		protected void doGet(HttpServletRequest request, final HttpServletResponse response) throws ServletException,
		        IOException
		{
			Log.i(TAG, ".doGet");
			doGetOrPost(request, response);
		}

		@Override
		protected void doPost(HttpServletRequest request, final HttpServletResponse response) throws ServletException,
		        IOException
		{
			Log.i(TAG, ".doPost");
			doGetOrPost(request, response);
		}

		private void doGetOrPost(HttpServletRequest request, final HttpServletResponse response)
		        throws ServletException, IOException
		{
			final String cmdId = request.getRequestURI().substring(1 + HTTP_CONTEXT.length()).toUpperCase();
			Log.i(TAG, ".doGetOrPost: cmdId = " + cmdId + ", current thread = " + Thread.currentThread());

			response.setContentType("application/json;charset=utf-8");

			final Bundle params = new Bundle();
			Enumeration<String> names = request.getParameterNames();
			while (names.hasMoreElements())
			{
				String name = names.nextElement();
				String value = request.getParameter(name);
				params.putString(name.toUpperCase(), value);
			}
			final FeatureCommand featureCommand = (FeatureCommand) Environment.getInstance().getFeatureComponent(
			        FeatureName.Component.COMMAND);

			// Object to store JSON result from command execution
			final JSONObject jsonResult = new JSONObject();

			// Keep reference to servlet thread
			final Thread servletThread = Thread.currentThread();

			// Flag indicating if command execute method finished before next
			// statement in the current function
			final boolean executed[] = new boolean[1];

			// Start command execute method in the servlet thread
			featureCommand.execute(cmdId, params, new OnResultReceived()
			{
				@Override
				public void onReceiveResult(FeatureError error, Object object)
				{
					// Now the current thread could be the same servletThread or
					// other, .e.g
					// android main thread when the command uses Volley
					Log.i(TAG, ".doGetOrPost.onReceiveResult: object = " + object);
					try
					{
						jsonResult.put("success", !error.isError());
						if (error.isError())
						{
							response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
							JSONObject jsonError = new JSONObject();
							jsonError.put("code", error.getCode());
							jsonError.put("message", error.getMessage());
							jsonResult.put("error", jsonError);
						}
						else
						{
							response.setStatus(HttpServletResponse.SC_OK);
							jsonResult.put("result", object);
						}
						// The servlet thread is now waiting for the JSON since
						// we are running from separate thread.
						synchronized (jsonResult)
						{
							// Notify JSON object as completed
							jsonResult.notify();
						}
						executed[0] = servletThread == Thread.currentThread();
					}
					catch (Exception e)
					{
						Log.w(TAG, e.getMessage(), e);
					}
				}
			});

			try
			{
				// executed flag will be true in case the command execute method
				// finished
				// and will be false if command execute method runs in parallel
				// thread
				if (!executed[0])
				{
					Log.i(TAG, "Waiting");
					synchronized (jsonResult)
					{
						jsonResult.wait();
					}
				}
			}
			catch (InterruptedException e)
			{
				Log.w(TAG, e.getMessage(), e);
			}
			Log.i(TAG, "Print json `" + jsonResult + "'");

			response.getWriter().print(jsonResult);
		}
	}
}

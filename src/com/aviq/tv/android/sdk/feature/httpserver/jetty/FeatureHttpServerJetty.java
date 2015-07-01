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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;

import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.command.FeatureCommand;

/**
 * jetty based HTTP server feature
 */
@Author("hari")
public class FeatureHttpServerJetty extends FeatureComponent
{
	public static final String TAG = FeatureHttpServerJetty.class.getSimpleName();

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");

		// FIXME: Configure HTTP server
		Server server = new Server(8081);
	    ContextHandler context = new ContextHandler();
        context.setContextPath("/command");
        context.setResourceBase(".");
        context.setClassLoader(Thread.currentThread().getContextClassLoader()); 
        context.setHandler(new HelloHandler());
        server.setHandler(context);

        
		// FIXME: Start HTTP server
	    try
        {
	        server.start();
        }
        catch (Exception e)
        {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
	    try
        {
	        server.join();
        }
        catch (InterruptedException e)
        {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }

		super.initialize(onFeatureInitialized);
	}
	
	
	
	
	
	public class HelloHandler extends AbstractHandler
	{
		@Override
		public void handle(String target, Request baseRequest, HttpServletRequest request, final HttpServletResponse response)
		        throws IOException, ServletException
		{		
			String cmdId = target.substring(1);
			System.out.println("cmdId = " + cmdId);
			
			System.out.println("baseRequest = " + baseRequest.toString());
			System.out.println("request = " + request.toString());
			
			

			response.setContentType("application/json");
			response.setStatus(HttpServletResponse.SC_OK);
			baseRequest.setHandled(true);
			
			OnResultReceived onResultReceived = new OnResultReceived()
			{
				@Override
				public void onReceiveResult(FeatureError error, Object object)
				{
					Log.i(TAG , "testCommands");
					if (error.isError())
					{
						Log.e(TAG, error.getMessage(), error);
					}
					else
					{
						JSONArray jsonArr = (JSONArray) object;
						try
						{
							Log.i(TAG, "== " + jsonArr.length() + " objects returned");
							for (int i = 0; i < jsonArr.length(); i++)
							{
								JSONObject jsonObj = jsonArr.getJSONObject(i);
								response.getWriter().println(jsonObj);
								Log.i(TAG, jsonObj.toString());
							}
						}
						catch (JSONException e)
						{
							Log.e(TAG, e.getMessage(), e);
						}
                        catch (IOException e)
                        {
	                        // TODO Auto-generated catch block
	                        e.printStackTrace();
                        }
					}
				}
			};
			
			FeatureCommand featureCommand = null;
			Bundle params = new Bundle();
	        featureCommand.execute(cmdId, params, onResultReceived);
	        
		        
		        
		}
	}

	
	
	
	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.HTTP_SERVER;
	}
	
}



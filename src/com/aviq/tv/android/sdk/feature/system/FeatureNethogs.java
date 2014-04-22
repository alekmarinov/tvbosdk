/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureNethogs.java
 * Author:      alek
 * Date:        19 Apr 2014
 * Description: Client to the nethogs service collecting incomming traffic from networm interface
 */

package com.aviq.tv.android.sdk.feature.system;

import android.os.Bundle;
import android.os.Process;
import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;

/**
 * Client to the nethogs service collecting incomming traffic from networm
 * interface
 */
public class FeatureNethogs extends FeatureComponent
{
	public static final String TAG = FeatureNethogs.class.getSimpleName();
	public static final int ON_DATA_RECEIVED = EventMessenger.ID("ON_DATA_RECEIVED");
	public static final String EXTRA_BITRATE_UP = "EXTRA_BITRATE_UP";
	public static final String EXTRA_BITRATE_DOWN = "EXTRA_BITRATE_DOWN";

	private NetworkClient _networkClient;
	private FeatureSystem _featureSystem;

	public enum Param
	{
		/**
		 * Nethogs port number
		 */
		PORT(6868),

		/**
		 * Nethogs host address
		 */
		HOST("127.0.0.1"),

		/**
		 * Nethogs interface to capture traffic
		 */
		NETWORK_INTERFACE("lo");

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.NETHOGS).put(name(), value);
		}

		Param(String value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.NETHOGS).put(name(), value);
		}
	}

	public FeatureNethogs()
	{
		_dependencies.Components.add(FeatureName.Component.SYSTEM);
	}

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		String host = getPrefs().getString(Param.HOST);
		int port = getPrefs().getInt(Param.PORT);
		final String netInterface = getPrefs().getString(Param.NETWORK_INTERFACE);

		try
		{
			_featureSystem = (FeatureSystem) Environment.getInstance()
			        .getFeatureComponent(FeatureName.Component.SYSTEM);

			_networkClient = new NetworkClient(host, port, new NetworkClient.OnNetworkEvent()
			{
				@Override
				public void onDisconnected()
				{
				}

				@Override
				public void onDataReceived(String data)
				{
					if (data != null)
					{
						// data =
						// lo:219996.0,219996.0,eth0:228326.0,16167.0,sit0:0.0,0.0,ip6tnl0:0.0,0.0
						int npos = data.indexOf(netInterface + ":");
						if (npos >= 0)
						{
							int spos = data.indexOf(':', npos + 1);
							int cpos = data.indexOf(',', npos + 1);
							if (cpos >= 0)
							{
								try
								{
									String rcvdStr = data.substring(spos + 1, cpos - 1);
									rcvdStr = rcvdStr.substring(0, rcvdStr.indexOf('.'));
									long bytesRcvd = Long.parseLong(rcvdStr);
									spos = cpos;
									cpos = data.indexOf(',', spos + 1);
									String sentStr = data.substring(spos + 1, cpos - 1);
									sentStr = sentStr.substring(0, sentStr.indexOf('.'));
									long bytesSent = Long.parseLong(sentStr);

									Bundle bundle = new Bundle();
									bundle.putLong(EXTRA_BITRATE_DOWN, bytesRcvd);
									bundle.putLong(EXTRA_BITRATE_UP, bytesSent);
									getEventMessenger().trigger(ON_DATA_RECEIVED, bundle);
								}
								catch (NumberFormatException nfe)
								{
									Log.w(TAG, nfe.getMessage());
								}
							}
						}
					}
				}

				@Override
				public void onConnected(boolean success)
				{
					Log.i(TAG, ".onConnected: success = " + success);
					if (success)
					{
						_networkClient.command(String.format("%d,%s", Process.myPid(), netInterface));
						FeatureNethogs.super.initialize(onFeatureInitialized);
					}
					else
					{
						onFeatureInitialized.onInitialized(FeatureNethogs.this, ResultCode.GENERAL_FAILURE);
					}
				}
			});
			_featureSystem.command("stop nethogs");
			_featureSystem.command("start nethogs");

			getEventMessenger().postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					_networkClient.connect();
				}
			}, 1000);
		}
		catch (FeatureNotFoundException e)
		{
			onFeatureInitialized.onInitialized(this, ResultCode.GENERAL_FAILURE);
		}
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.NETHOGS;
	}
}

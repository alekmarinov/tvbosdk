/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureEthernet.java
 * Author:      alek
 * Date:        8 Jan 2015
 * Description: Provides access to network adapters
 */
package com.aviq.tv.android.sdk.feature.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;
import com.aviq.tv.android.sdk.feature.system.FeatureDevice.IStatusFieldGetter;

/**
 * Provides access to network adapters
 */
@Author("alek")
public class FeatureNetwork extends FeatureComponent
{
	private static final String TAG = FeatureNetwork.class.getSimpleName();

	public static final int ON_NETWORK_CHANGE = EventMessenger.ID("ON_NETWORK_CHANGE");

	public static enum OnNetworkChangeExtras
	{
		IS_ATTEMPT_FAILOVER, IS_LOST_CONNECTIVITY
	}

	public enum NetworkType
	{
		ETHERNET, WIRELESS, UNKNOWN, NONE
	}

	public FeatureNetwork() throws FeatureNotFoundException
	{
		require(FeatureName.Component.DEVICE);
		require(FeatureName.Component.ETHERNET);
		require(FeatureName.Component.WIRELESS);
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		_feature.Component.DEVICE.addStatusField("network", new IStatusFieldGetter()
		{
			@Override
			public String getStatusField()
			{
				return getActiveNetwork().name();
			}
		});

		IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		Environment.getInstance().registerReceiver(new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				Bundle bundle = new Bundle();
				if (intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER, false))
				{
					bundle.putBoolean(OnNetworkChangeExtras.IS_ATTEMPT_FAILOVER.name(), Boolean.TRUE);
				}
				else if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false))
				{
					bundle.putBoolean(OnNetworkChangeExtras.IS_LOST_CONNECTIVITY.name(), Boolean.TRUE);
				}
				getEventMessenger().trigger(ON_NETWORK_CHANGE, bundle);
			}
		}, intentFilter);

		super.initialize(onFeatureInitialized);
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.NETWORK;
	}

	public NetworkType getActiveNetwork()
	{
		final ConnectivityManager connectivityManager = (ConnectivityManager) Environment.getInstance()
		        .getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
		if (networkInfo != null && networkInfo.isConnected())
			return NetworkType.ETHERNET;
		networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (networkInfo != null && networkInfo.isAvailable())
			return NetworkType.WIRELESS;
		networkInfo = connectivityManager.getActiveNetworkInfo();
		if (networkInfo != null)
			return NetworkType.UNKNOWN;
		return NetworkType.NONE;
	}

	public NetworkConfig getNetworkConfig(NetworkType networkType)
	{
		switch (networkType)
		{
			case ETHERNET:
				return _feature.Component.ETHERNET.getNetworkConfig();
			case WIRELESS:
				return _feature.Component.WIRELESS.getNetworkConfig();
			default:
				return null;
		}
	}
}

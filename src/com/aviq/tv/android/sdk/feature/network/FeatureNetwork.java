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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;

/**
 * Provides access to network adapters
 */
@Author("alek")
public class FeatureNetwork extends FeatureComponent
{
	private static final String TAG = FeatureNetwork.class.getSimpleName();

	public enum NetworkType
	{
		ETHERNET, WIRELESS, UNKNOWN
	}

	public FeatureNetwork() throws FeatureNotFoundException
	{
		require(FeatureName.Component.ETHERNET);
		require(FeatureName.Component.WIRELESS);
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
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
		final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		if (networkInfo != null)
		{
			switch (networkInfo.getType())
			{
				case ConnectivityManager.TYPE_ETHERNET:
					return NetworkType.ETHERNET;
				case ConnectivityManager.TYPE_WIFI:
					return NetworkType.WIRELESS;
				default:
					return NetworkType.UNKNOWN;
			}
		}
		return null;
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

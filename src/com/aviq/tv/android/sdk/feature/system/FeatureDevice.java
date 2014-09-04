/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureDevice.java
 * Author:      elmira
 * Date:        4 Sep 2014
 * Description: Defines device parameters
 */
package com.aviq.tv.android.sdk.feature.system;

import android.content.pm.PackageManager.NameNotFoundException;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;

/**
 * Defines device parameters
 */
public class FeatureDevice extends FeatureComponent
{
	public static final String TAG = FeatureDevice.class.getSimpleName();

	public enum Param
	{
		CUSTOMER(""),
		BRAND(""),
		RELEASE("devel");

		Param(int value)
		{
			Environment.getInstance().getFeature(FeatureDevice.class).getPrefs().put(name(), value);
		}

		Param(String value)
		{
			Environment.getInstance().getFeature(FeatureDevice.class).getPrefs().put(name(), value);
		}
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.DEVICE;
	}

	/**
	 * Parses the version string from the manifest.
	 *
	 * @return build version
	 * @throws NameNotFoundException
	 */
	public String getBuildVersion()
	{
		return Environment.getInstance().getBuildVersion();
	}

	/**
	 * Get customer
	 *
	 * @return customer
	 */
	public String getCustomer()
	{
		return getPrefs().getString(Param.CUSTOMER);
	}

	/**
	 * Get device brand
	 *
	 * @return device brand
	 */
	public String getBrand()
	{
		return getPrefs().getString(Param.BRAND);
	}

	/**
	 * Get device build
	 *
	 * @return  build
	 */
	public String getBuildKind()
	{
		return getPrefs().getString(Param.RELEASE);
	}
}

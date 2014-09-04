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

import java.util.ArrayList;
import java.util.List;

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
		/**
		 * Box customer
		 */
		CUSTOMER(""),

		/**
		 * Application brand name
		 */
		BRAND("generic"),

		/**
		 * Release or development build kind
		 */
		BUILD("devel"),

		/**
		 * Box mac address
		 */
		MAC("00:00:00:00:00:00");

		Param(int value)
		{
			Environment.getInstance().getFeature(FeatureDevice.class).getPrefs().put(name(), value);
		}

		Param(String value)
		{
			Environment.getInstance().getFeature(FeatureDevice.class).getPrefs().put(name(), value);
		}
	}

	public enum DeviceAttribute
	{
		CUSTOMER, BRAND, BUILD, MAC
	}

	private List<String> _deviceAttributeNames = new ArrayList<String>();

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.DEVICE;
	}

	/**
	 * Parses the version string from the manifest.
	 *
	 * @return application version
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
		return getPrefs().getString(Param.BUILD);
	}
}

/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
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
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.feature.eventcollector.FeatureEventCollectorBase;



public class FeatureDevice extends FeatureComponent
{
	
	
	public static final String TAG = FeatureDevice.class.getSimpleName();
	
	public enum Param
	{
		
		EVENT_CUSTOMER(""), EVENT_BRAND("");
		
		Param(int value)
		{
			try
			{
				Environment.getInstance().getFeatureManager().getFeature(FeatureEventCollectorBase.class).getPrefs()
				        .put(name(), value);
			}
			catch (FeatureNotFoundException e)
			{
			}
		}
		
		Param(String value)
		{
			try
			{
				Environment.getInstance().getFeatureManager().getFeature(FeatureEventCollectorBase.class).getPrefs()
				        .put(name(), value);
			}
			catch (FeatureNotFoundException e)
			{
			}
		}
	}
		
	@Override
	public Component getComponentName()
	{
		// TODO Auto-generated method stub
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
	 * @return  customer	 * 
	 */
	public String getCustomer()
	{
		return getPrefs().getString(Param.EVENT_CUSTOMER);
	}
	
	/**
	 * Get device brand
	 *
	 * @return device brand	 * 
	 */
	public String getBrand()
	{
		return getPrefs().getString(Param.EVENT_BRAND);
	}
	
	/**
	 * Get device build
	 *
	 * @return  build * 
	 */
	public String getKind()
	{
		return Environment.getInstance().getPrefs().getString(Environment.Param.RELEASE);
	}
	
	
}

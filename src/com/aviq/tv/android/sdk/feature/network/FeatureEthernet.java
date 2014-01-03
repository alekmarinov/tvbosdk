/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureEthernet.java
 * Author:      alek
 * Date:        3 Jan 2014
 * Description: Ethernet Settings component
 */

package com.aviq.tv.android.sdk.feature.network;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;

/**
 * Ethernet Settings component
 */
public class FeatureEthernet extends FeatureComponent
{
	public static final String TAG = FeatureEthernet.class.getSimpleName();

	private Prefs _userPrefs;

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		_userPrefs = Environment.getInstance().getUserPrefs();
		try
        {
			Object ethernetManager = Environment.getInstance().getContext().getSystemService("ethernet");
			Method setEthEnabled = ethernetManager.getClass().getMethod("setEthEnabled", boolean.class);
			setEthEnabled.invoke(ethernetManager, Boolean.TRUE);

			onFeatureInitialized.onInitialized(this, ResultCode.OK);
			return ;
        }
        catch (NoSuchMethodException e)
        {
        	Log.e(TAG, e.getMessage(), e);
        }
        catch (IllegalArgumentException e)
        {
        	Log.e(TAG, e.getMessage(), e);
        }
        catch (IllegalAccessException e)
        {
        	Log.e(TAG, e.getMessage(), e);
        }
        catch (InvocationTargetException e)
        {
        	Log.e(TAG, e.getMessage(), e);
        }
		onFeatureInitialized.onInitialized(this, ResultCode.GENERAL_FAILURE);
	}

	@Override
    public Component getComponentName()
    {
	    return FeatureName.Component.ETHERNET;
    }
}

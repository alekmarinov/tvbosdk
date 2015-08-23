/**
 * Copyright (c) 2007-2015, Intelibo Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureTestInit.java
 * Author:      alek
 * Date:        23 Aug 2015
 * Description:
 */

package com.aviq.tv.android.sdk.feature.test;

import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;

/**
 * Test features initialization
 */
public class FeatureTestInit extends FeatureComponent
{
	public static final String TAG = FeatureTestInit.class.getSimpleName();

	public FeatureTestInit()
	{
	}

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		super.initialize(onFeatureInitialized);

		// should throw exception in 2 secs as calling onInitialized more than once
		getEventMessenger().postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				FeatureTestInit.super.initialize(onFeatureInitialized);
			}
		}, 2000);
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.SPECIAL;
	}

}

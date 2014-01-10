/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureRCU.java
 * Author:      alek
 * Date:        9 Jan 2014
 * Description: Defines RCU specific keys mapping
 */

package com.aviq.tv.android.sdk.feature.rcu;

import com.aviq.tv.android.sdk.core.Key;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;

/**
 * Defines RCU specific keys mapping
 *
 */
public abstract class FeatureRCU extends FeatureComponent
{
	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.RCU;
	}

	abstract public Key getKey(int keyCode);
}

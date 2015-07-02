/**
 * Copyright (c) 2007-2015, Intelibo Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureHttpServerCustom.java
 * Author:      alek
 * Date:        1 Jul 2015
 * Description: Base component feature providing http server
 */

package com.aviq.tv.android.sdk.feature.httpserver;

import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;

/**
 * Base component feature providing http server
 */
public class FeatureHttpServer extends FeatureComponent
{
	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.HTTP_SERVER;
	}
}

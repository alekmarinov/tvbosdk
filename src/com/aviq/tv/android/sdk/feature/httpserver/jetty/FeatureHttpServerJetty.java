/**
 * Copyright (c) 2007-2015, Intelibo Ltd
 *
 * Project:     tvbosdk
 * Filename:    FeatureHttpServerJetty.java
 * Author:      Hari
 * Date:        01.07.2015 ã.
 * Description: jetty based HTTP server feature
 */

package com.aviq.tv.android.sdk.feature.httpserver.jetty;

import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;

/**
 * jetty based HTTP server feature
 */
@Author("hari")
public class FeatureHttpServerJetty extends FeatureComponent
{
	public static final String TAG = FeatureHttpServerJetty.class.getSimpleName();

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");

		// FIXME: Configure HTTP server

		// FIXME: Start HTTP server

		super.initialize(onFeatureInitialized);
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.HTTP_SERVER;
	}
}

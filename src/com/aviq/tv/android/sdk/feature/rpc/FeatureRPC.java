/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    LuaRPC.java
 * Author:      alek
 * Date:        7 Feb 2014
 * Description: Lua based RPC scripting service
 */

package com.aviq.tv.android.sdk.feature.rpc;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;


/**
 * Lua based RPC scripting service
 */
@Author("alek")
public abstract class FeatureRPC extends FeatureComponent
{
	public static final String TAG = FeatureRPC.class.getSimpleName();

	@Override
	public FeatureName.Component getComponentName()
	{
		return FeatureName.Component.RPC;
	}

	/**
	 * Executes script as input stream synchronously. This method needs to be
	 * overwritten by specific RPC implementation
	 *
	 * @param inputStream
	 */
	public abstract void execute(InputStream inputStream);

	/**
	 * Executes remote script from url
	 *
	 * @param url
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	public void executeUrl(final String url) throws IOException
	{
		Log.i(TAG, ".executeUrl: " + url);
		executeAsync(new URL(url).openStream());
	}

	/**
	 * Executes script as input stream asynchronously
	 *
	 * @param inputStream
	 */
	private void executeAsync(final InputStream inputStream)
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				execute(inputStream);
			}
		}).start();
	}
}

/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureDebug.java
 * Author:      zhelyazko
 * Date:        2 Jul 2014
 * Description: Feature with debugging functionalities
 */

package com.aviq.tv.android.sdk.feature.debug;

import android.os.Bundle;
import android.widget.Toast;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.EventReceiver;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;
import com.aviq.tv.android.sdk.feature.easteregg.FeatureEasterEgg;
import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Feature for debugging
 */
@Author("alek")
public class FeatureDebug extends FeatureComponent implements EventReceiver
{
	private static final String TAG = FeatureDebug.class.getSimpleName();

	public static enum Param
	{
		/**
		 * Key sequence for debug mode
		 */
		KEY_SEQUENCE_DEBUG("33284");

		Param(String value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.DEBUG).put(name(), value);
		}
	}

	private String _keySeqDebug;

	public FeatureDebug() throws FeatureNotFoundException
	{
		require(FeatureName.Component.EASTER_EGG);
		require(FeatureName.Component.DEVICE);
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		_keySeqDebug = getPrefs().getString(Param.KEY_SEQUENCE_DEBUG);
		_feature.Component.EASTER_EGG.addEasterEgg(_keySeqDebug);
		_feature.Component.EASTER_EGG.getEventMessenger().register(this, FeatureEasterEgg.ON_KEY_SEQUENCE);
		super.initialize(onFeatureInitialized);
	}

	@Override
	public FeatureName.Component getComponentName()
	{
		return FeatureName.Component.DEBUG;
	}

	@Override
	public void onEvent(int msgId, Bundle bundle)
	{
		Log.v(TAG, ".onEvent: msgId = " + EventMessenger.idName(msgId) + TextUtils.implodeBundle(bundle));

		if (FeatureEasterEgg.ON_KEY_SEQUENCE == msgId)
		{
			String keySeq = bundle.getString(FeatureEasterEgg.EXTRA_KEY_SEQUENCE);
			if (keySeq.equals(_keySeqDebug))
			{
				Prefs userPrefs = Environment.getInstance().getUserPrefs();
				boolean isDebug = userPrefs.getBool(Environment.Param.DEBUG);
				isDebug = !isDebug;
				if (isDebug)
				{
					Toast.makeText(Environment.getInstance(), "Entering DEBUG mode", Toast.LENGTH_LONG).show();
				}
				else
				{
					Toast.makeText(Environment.getInstance(), "Leaving DEBUG mode", Toast.LENGTH_LONG).show();
				}
				userPrefs.put(Environment.Param.DEBUG, isDebug);

				final boolean debug = isDebug;
				getEventMessenger().postDelayed(new Runnable()
				{
					@Override
					public void run()
					{
						_feature.Component.DEVICE.suicide("Switching to DEBUG mode = " + debug);
					}
				}, 2000);
			}
		}
	}
}

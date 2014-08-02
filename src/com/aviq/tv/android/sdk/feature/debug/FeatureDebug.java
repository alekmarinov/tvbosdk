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

import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.EventReceiver;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.feature.easteregg.FeatureEasterEgg;
import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Feature for debugging
 */
public class FeatureDebug extends FeatureComponent implements EventReceiver
{
	private static final String TAG = FeatureDebug.class.getSimpleName();

	private int _vhLevel = 0;

	public FeatureDebug() throws FeatureNotFoundException
	{
		require(FeatureName.Component.EASTER_EGG);
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
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
			if (FeatureEasterEgg.KEY_SEQ_VHR.equals(keySeq))
			{
				Log.i(TAG, "------------ VIEW HIERARCHY DUMP ------------");
				_vhLevel = 0;
				View v = ((ViewGroup) Environment.getInstance().findViewById(android.R.id.content)).getChildAt(0);
				recurseView(v);
				Log.i(TAG, "------------ END OF VIEW HIERARCHY DUMP ------------");
			}
		}
	}

	private void recurseView(View v)
	{
		String treeLine = getTreeLine(_vhLevel);

		String id = getStringId(v);
		Log.i(TAG, treeLine + v.getClass() + " [tag = " + v.getTag() + ", id = " + id + ", top = " + v.getTop()
		        + ", bottom = " + v.getBottom() + ", left = " + v.getLeft() + ", right = " + v.getRight() + "]");

		if (v instanceof ViewGroup)
		{
			ViewGroup vg = (ViewGroup) v;
			for (int i = 0; i < vg.getChildCount(); i++)
			{
				View child = vg.getChildAt(i);
				if (child instanceof ViewGroup)
				{
					_vhLevel++;
					recurseView(child);
				}
				else
				{
					treeLine = getTreeLine(_vhLevel + 1);
					id = getStringId(child);
					Log.i(TAG, treeLine + child.getClass() + " [tag = " + v.getTag() + ", id = " + id + ", top = "
					        + child.getTop() + ", bottom = " + child.getBottom() + ", left = " + child.getLeft()
					        + ", right = " + child.getRight() + "]");
				}
			}
			_vhLevel--;
		}
	}

	private String getStringId(View v)
	{
		String id = null;
		try
		{
			id = v.getResources().getResourceName(v.getId());
		}
		catch (NotFoundException e)
		{
			id = v.getId() + "";
		}
		return id;
	}

	private String getTreeLine(int level)
	{
		String tree = "+";
		for (int i = 0; i < level; i++)
			tree += "----";
		tree += " ";
		return tree;
	}
}

/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureRCUSDMC.java
 * Author:      alek
 * Date:        9 Jan 2014
 * Description: Defines Wilmaa RCU specific keys mapping
 */

package com.aviq.tv.android.sdk.feature.rcu;

import android.view.KeyEvent;

import com.aviq.tv.android.sdk.core.Key;

/**
 * Defines Wilmaa RCU specific keys mapping
 */
public class FeatureRCUWilmaa extends FeatureRCU
{
	@Override
	public Key getKey(int keyCode)
	{
		switch (keyCode)
		{
			case KeyEvent.KEYCODE_POWER:
				return Key.SLEEP;
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				return Key.PLAY_PAUSE;
			case KeyEvent.KEYCODE_DPAD_LEFT:
				return Key.LEFT;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				return Key.RIGHT;
			case KeyEvent.KEYCODE_DPAD_UP:
				return Key.UP;
			case KeyEvent.KEYCODE_DPAD_DOWN:
				return Key.DOWN;
			case KeyEvent.KEYCODE_ENTER:
				return Key.OK;
			case KeyEvent.KEYCODE_BACK:
				return Key.BACK;
			case 132:
				return Key.MENU;
			case KeyEvent.KEYCODE_VOLUME_UP:
				return Key.VOLUME_UP;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				return Key.VOLUME_DOWN;
		}
		return Key.UNKNOWN;
	}
}

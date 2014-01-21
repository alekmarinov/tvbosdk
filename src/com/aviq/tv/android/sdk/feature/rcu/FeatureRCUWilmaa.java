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
			case KeyEvent.KEYCODE_U:
				return Key.VOLUME_UP;
			case KeyEvent.KEYCODE_D:
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				return Key.VOLUME_DOWN;

				// FIXME: used for testing
			case KeyEvent.KEYCODE_Y:
				return Key.YELLOW;
			case KeyEvent.KEYCODE_R:
				return Key.RED;
			case KeyEvent.KEYCODE_G:
				return Key.GREEN;
			case KeyEvent.KEYCODE_B:
				return Key.BLUE;
			case KeyEvent.KEYCODE_E:
				return Key.EPG;
			case KeyEvent.KEYCODE_H:
				return Key.HOME;
			case KeyEvent.KEYCODE_F:
				return Key.FAVORITE;
			case KeyEvent.KEYCODE_L:
				return Key.LAST_CHANNEL;
			case KeyEvent.KEYCODE_T:
				return Key.TXT;
			case KeyEvent.KEYCODE_PAGE_DOWN:
				return Key.PAGE_DOWN;
			case KeyEvent.KEYCODE_PAGE_UP:
				return Key.PAGE_UP;

			case KeyEvent.KEYCODE_0:
				return Key.NUM_0;
			case KeyEvent.KEYCODE_1:
				return Key.NUM_1;
			case KeyEvent.KEYCODE_2:
				return Key.NUM_2;
			case KeyEvent.KEYCODE_3:
				return Key.NUM_3;
			case KeyEvent.KEYCODE_4:
				return Key.NUM_4;
			case KeyEvent.KEYCODE_5:
				return Key.NUM_5;
			case KeyEvent.KEYCODE_6:
				return Key.NUM_6;
			case KeyEvent.KEYCODE_7:
				return Key.NUM_7;
			case KeyEvent.KEYCODE_8:
				return Key.NUM_8;
			case KeyEvent.KEYCODE_9:
				return Key.NUM_9;
		}
		return Key.UNKNOWN;
	}
}

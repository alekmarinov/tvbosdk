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
public class FeatureRCUVTX extends FeatureRCU
{
	@Override
	public Key getKey(int keyCode)
	{
		switch (keyCode)
		{
			case KeyEvent.KEYCODE_POWER:
				return Key.SLEEP;
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
			case 1017: // FIXME: detect key code
				return Key.CHARACTERS;
			case KeyEvent.KEYCODE_0:
				return Key.NUM_0;
			case KeyEvent.KEYCODE_DEL:
				return Key.DELETE;
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
				return Key.PLAY_BACKWARD;
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				return Key.PLAY_PAUSE;
			case KeyEvent.KEYCODE_MEDIA_STOP:
				return Key.PLAY_STOP;
			case KeyEvent.KEYCODE_MEDIA_NEXT:
				return Key.PLAY_FORWARD;
			case KeyEvent.KEYCODE_MUTE:
				return Key.MUTE;
			case KeyEvent.KEYCODE_VOLUME_UP:
				return Key.VOLUME_UP;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				return Key.VOLUME_DOWN;
			case KeyEvent.KEYCODE_PAGE_UP:
				return Key.PAGE_UP;
			case KeyEvent.KEYCODE_PAGE_DOWN:
				return Key.PAGE_DOWN;
			case 1011: // FIXME: detect key code
				return Key.LAST_CHANNEL;
			case KeyEvent.KEYCODE_TV:
				return Key.TV;
			case 1012: // FIXME: detect key code
				return Key.VOD;
			case KeyEvent.KEYCODE_MUSIC:
				return Key.MEDIA;
			case 1013: // FIXME: detect key code
				return Key.EPG;
			case KeyEvent.KEYCODE_BACK:
				return Key.BACK;
			case KeyEvent.KEYCODE_MENU:
				return Key.MENU;
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
			case KeyEvent.KEYCODE_PROG_RED:
				return Key.RED;
			case KeyEvent.KEYCODE_PROG_GREEN:
				return Key.GREEN;
			case KeyEvent.KEYCODE_PROG_YELLOW:
				return Key.YELLOW;
			case KeyEvent.KEYCODE_PROG_BLUE:
				return Key.BLUE;
			case KeyEvent.KEYCODE_MEDIA_RECORD:
				return Key.REC;
			case 1014: // FIXME: detect key code
				return Key.FUNCTION1; // Lib
			case 1015: // FIXME: detect key code
				return Key.TXT;
			case 1016: // FIXME: detect key code
				return Key.APPS;
		}
		return Key.UNKNOWN;
	}
}
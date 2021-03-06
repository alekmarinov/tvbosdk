/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureRCUSDMC.java
 * Author:      alek
 * Date:        9 Jan 2014
 * Description: Defines SDMC RCU specific keys mapping
 */

package com.aviq.tv.android.sdk.feature.rcu;

import android.view.KeyEvent;

import com.aviq.tv.android.sdk.core.Key;

/**
 * Defines SDMC RCU specific keys mapping
 */
public class FeatureRCUSDMC extends FeatureRCUKeyboard
{
	@Override
	public Key getKey(int keyCode)
	{
		switch (keyCode)
		{
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
			case KeyEvent.KEYCODE_STAR:
				return Key.CHARACTERS;
			case KeyEvent.KEYCODE_0:
				return Key.NUM_0;
			case KeyEvent.KEYCODE_DEL:
				return Key.DELETE;
			case KeyEvent.KEYCODE_MEDIA_REWIND:
				return Key.PLAY_BACKWARD;
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				return Key.PLAY_PAUSE;
			case KeyEvent.KEYCODE_MEDIA_STOP:
				return Key.PLAY_STOP;
			case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
				return Key.PLAY_FORWARD;
			case KeyEvent.KEYCODE_VOLUME_MUTE:
				return Key.MUTE;
			case KeyEvent.KEYCODE_VOLUME_UP:
				return Key.VOLUME_UP;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				return Key.VOLUME_DOWN;
			case KeyEvent.KEYCODE_PAGE_UP:
				return Key.PAGE_UP;
			case KeyEvent.KEYCODE_PAGE_DOWN:
				return Key.PAGE_DOWN;
			case 303: // DVB__RECALL
				return Key.LAST_CHANNEL;
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
			case KeyEvent.KEYCODE_DPAD_CENTER:
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

			// Added for AVIQ apps
			case 304:
				return Key.TV;
			case 305:
				return Key.TXT;
			case 306:
				return Key.APPS;
			case 307:
				return Key.VOD;
			case 308:
				return Key.WEBTV;
			case 309:
				return Key.MEDIA;
			case 310:
				return Key.YOUTUBE;
			case 311:
				return Key.LIB;
			case 312:
				return Key.EPG;
			case 313:
				return Key.FAVORITE;
			case 314:
			case 26:
				return Key.SLEEP;
		}
		return super.getKey(keyCode);
	}
}

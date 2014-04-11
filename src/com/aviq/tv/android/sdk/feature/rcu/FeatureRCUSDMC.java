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
public class FeatureRCUSDMC extends FeatureRCU
{
	@Override
	public Key getKey(int keyCode)
	{
		switch (keyCode)
		{
			case KeyEvent.KEYCODE_POWER:
				return Key.SLEEP;
			case 164:
				return Key.MUTE;
			case 312:
				return Key.RED;
			case 313:
				return Key.GREEN;
			case 314:
				return Key.YELLOW;
			case 315:
				return Key.BLUE;
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				return Key.PLAY_PAUSE;
			case 86:
				return Key.PLAY_STOP;
			case 89:
				return Key.PLAY_BACKWARD;
			case 90:
				return Key.PLAY_FORWARD;
			case 323:
				return Key.REC;
			case 319:
				return Key.FUNCTION1; // Picture Size
			case 326:
				return Key.DVR;
			case 303: // FIXME: detect key code
				return Key.LAST_CHANNEL;
			case 316: // FIXME: detect key code
				return Key.FUNCTION2; // Subtitles
			case 318: // FIXME: detect key code
				return Key.FUNCTION3; // Audio
			case 317: // FIXME: detect key code
				return Key.TXT;
			case 17: // FIXME: detect key code
				return Key.FUNCTION4; // Timer
			case 82:
				return Key.MENU;
			case KeyEvent.KEYCODE_BACK:
				return Key.BACK;
			case 307:
				return Key.INFO;
			case 306:
				return Key.EPG;
			case KeyEvent.KEYCODE_DPAD_LEFT:
				return Key.LEFT;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				return Key.RIGHT;
			case KeyEvent.KEYCODE_DPAD_UP:
				return Key.UP;
			case KeyEvent.KEYCODE_DPAD_DOWN:
				return Key.DOWN;
			case 23:
				return Key.OK;
			case KeyEvent.KEYCODE_VOLUME_UP:
				return Key.VOLUME_UP;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				return Key.VOLUME_DOWN;
			case KeyEvent.KEYCODE_HOME: /* FIXME: SDMC is not sending this event! */
				return Key.HOME;
			case 304:
				return Key.FAVORITE;
			case 92:
				return Key.PAGE_UP;
			case 93:
				return Key.PAGE_DOWN;
			case 8:
				return Key.NUM_1;
			case 9:
				return Key.NUM_2;
			case 10:
				return Key.NUM_3;
			case 11:
				return Key.NUM_4;
			case 12:
				return Key.NUM_5;
			case 13:
				return Key.NUM_6;
			case 14:
				return Key.NUM_7;
			case 15:
				return Key.NUM_8;
			case 16:
				return Key.NUM_9;
			case 301:
				return Key.FUNCTION5; // TV/Radio
			case 7:
				return Key.NUM_0;
			case 67:
				return Key.DELETE;

			// FIXME: Test mappings to test with keyboard
			case KeyEvent.KEYCODE_E:
				return Key.EPG;
		}
		return Key.UNKNOWN;
	}
}

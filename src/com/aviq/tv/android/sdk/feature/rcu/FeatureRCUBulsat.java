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
 * Defines Bulsat RCU specific keys mapping
 */
public class FeatureRCUBulsat extends FeatureRCUKeyboard
{
	@Override
	public Key getKey(int keyCode)
	{
		switch (keyCode)
		{
			case 258:
				return Key.TIME;
			case KeyEvent.KEYCODE_VOLUME_UP:
				return Key.VOLUME_UP;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				return Key.VOLUME_DOWN;
			case 164:
				return Key.MUTE;
			case KeyEvent.KEYCODE_BACK:
				return Key.BACK;
			case KeyEvent.KEYCODE_MENU:
				return Key.MENU;
			case 126:
				return Key.PLAY;
			case 127:
				return Key.PAUSE;
			case 170:
				return Key.TV;
			case 171:
				return Key.PIP;
			case 172:
				return Key.EPG;
			case 174:
				return Key.FAVORITE;
			case 165:
				return Key.INFO;
			case 209:
				return Key.VOD;

		}
		return super.getKey(keyCode);
	}

	@Override
	public int getCode(Key key)
	{
		switch (key)
		{
			case VOD:
				return 209;
			case TV:
				return 170;
			case TIME:
				return 258;
			case MUTE:
				return 164;
			case PLAY:
				return 126;
			case PAUSE:
				return 127;
			case PIP:
				return 171;
			case EPG:
				return 172;
			case FAVORITE:
				return 174;
			case INFO:
				return 165;
			case BACK:
				return KeyEvent.KEYCODE_BACK;
			case MENU:
				return KeyEvent.KEYCODE_MENU;
			case VOLUME_UP:
				return KeyEvent.KEYCODE_VOLUME_UP;
			case VOLUME_DOWN:
				return KeyEvent.KEYCODE_VOLUME_DOWN;
			default:
				return super.getCode(key);
		}
	}
}

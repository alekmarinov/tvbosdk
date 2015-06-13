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

import com.aviq.tv.android.sdk.core.Key;

/**
 * Defines Bulsat RCU specific keys mapping
 */
public class FeatureRCUBulsat extends FeatureRCUSDMC
{
	@Override
	public Key getKey(int keyCode)
	{

		switch (keyCode)
		{
			case 126:
				return Key.PLAY;
			case 127:
				return Key.PAUSE;
			case 171:
				return Key.PIP;
			case 172:
				return Key.EPG;
			case 174:
				return Key.FAVORITE;
			case 165:
				return Key.INFO;

		}
		return super.getKey(keyCode);
	}
}

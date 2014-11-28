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
public class FeatureRCUKeyboard extends FeatureRCU
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
			case KeyEvent.KEYCODE_PAGE_UP:
				return Key.PAGE_UP;
			case KeyEvent.KEYCODE_PAGE_DOWN:
				return Key.PAGE_DOWN;
			case KeyEvent.KEYCODE_ESCAPE:
				return Key.BACK;
			case KeyEvent.KEYCODE_DPAD_LEFT:
				return Key.LEFT;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				return Key.RIGHT;
			case KeyEvent.KEYCODE_DPAD_UP:
				return Key.UP;
			case KeyEvent.KEYCODE_DPAD_DOWN:
				return Key.DOWN;
			case KeyEvent.KEYCODE_DPAD_CENTER:
			case KeyEvent.KEYCODE_ENTER:
				return Key.OK;
			case KeyEvent.KEYCODE_F1:
				return Key.FUNCTION1;
			case KeyEvent.KEYCODE_F2:
				return Key.MENU;
			case KeyEvent.KEYCODE_F3:
				return Key.FUNCTION3;
			case KeyEvent.KEYCODE_F4:
				return Key.FUNCTION4;
			case KeyEvent.KEYCODE_F5:
				return Key.FUNCTION5;
			case KeyEvent.KEYCODE_F6:
				return Key.FUNCTION6;
			case KeyEvent.KEYCODE_F7:
				return Key.FUNCTION7;
			case KeyEvent.KEYCODE_F8:
				return Key.FUNCTION8;
			case KeyEvent.KEYCODE_F9:
				return Key.FUNCTION9;
			case KeyEvent.KEYCODE_F10:
				return Key.FUNCTION10;
			case KeyEvent.KEYCODE_F11:
				return Key.FUNCTION11;
			case KeyEvent.KEYCODE_F12:
				return Key.FUNCTION12;

			case KeyEvent.KEYCODE_A:
				return Key.A;
			case KeyEvent.KEYCODE_B:
				return Key.BLUE;
			case KeyEvent.KEYCODE_C:
				return Key.C;
			case KeyEvent.KEYCODE_D:
				return Key.D;
			case KeyEvent.KEYCODE_E:
				return Key.EPG;
			case KeyEvent.KEYCODE_F:
				return Key.F;
			case KeyEvent.KEYCODE_G:
				return Key.GREEN;
			case KeyEvent.KEYCODE_H:
				return Key.H;
			case KeyEvent.KEYCODE_I:
				return Key.I;
			case KeyEvent.KEYCODE_J:
				return Key.J;
			case KeyEvent.KEYCODE_K:
				return Key.K;
			case KeyEvent.KEYCODE_L:
				return Key.L;
			case KeyEvent.KEYCODE_M:
				return Key.M;
			case KeyEvent.KEYCODE_N:
				return Key.N;
			case KeyEvent.KEYCODE_O:
				return Key.O;
			case KeyEvent.KEYCODE_P:
				return Key.P;
			case KeyEvent.KEYCODE_Q:
				return Key.Q;
			case KeyEvent.KEYCODE_R:
				return Key.RED;
			case KeyEvent.KEYCODE_S:
				return Key.S;
			case KeyEvent.KEYCODE_T:
				return Key.T;
			case KeyEvent.KEYCODE_U:
				return Key.U;
			case KeyEvent.KEYCODE_V:
				return Key.V;
			case KeyEvent.KEYCODE_W:
				return Key.W;
			case KeyEvent.KEYCODE_X:
				return Key.X;
			case KeyEvent.KEYCODE_Y:
				return Key.YELLOW;
			case KeyEvent.KEYCODE_Z:
				return Key.Z;
		}
		return Key.UNKNOWN;
	}
}

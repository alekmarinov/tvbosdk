/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    AVKeyEvent.java
 * Author:      alek
 * Date:        9 Jan 2014
 * Description: KeyEvent wrapper class
 */

package com.aviq.tv.android.sdk.core;

import android.view.KeyEvent;

/**
 * KeyEvent wrapper class
 */
public class AVKeyEvent
{
	/**
	 * The original Android KeyEvent
	 */
	public KeyEvent Event;

	/**
	 * Mapped code
	 */
	public Key Code;

	public AVKeyEvent(KeyEvent keyEvent, Key key)
	{
		Event = keyEvent;
		Code = key;
	}

	/**
	 * @param key
	 *
	 * @return true if the event corresponds to the passed key
	 */
	public boolean is(Key key)
	{
		return key.equals(Code);
	}

	@Override
	public String toString()
	{
		StringBuffer buf = new StringBuffer();
		buf.append(Code + " (" + Event.getKeyCode() + ")");
		return buf.toString();
	}
}

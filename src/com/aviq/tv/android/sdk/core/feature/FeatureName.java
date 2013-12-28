/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureName.java
 * Author:      alek
 * Date:        1 Dec 2013
 * Description: Enumerate all feature names
 */

package com.aviq.tv.android.sdk.core.feature;

/**
 * Enumerate all feature names
 *
 */
public class FeatureName
{
	public static enum Component
	{
		EPG,
		PLAYER,
		HTTP_SERVER,
		REGISTER,
		WATCHLIST,
		CHANNELS
	}

	public static enum Scheduler
	{
		INTERNET
	}

	public static enum State
	{
		MENU,
		LOADING,
		TV,
		EPG,
		MESSAGE_BOX,
		PROGRAM_INFO,
		WATCHLIST,
		CHANNELS
	}
}

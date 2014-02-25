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
		RCU,
		PLAYER,
		HTTP_SERVER,
		REGISTER,
		WATCHLIST,
		CHANNELS,
		ETHERNET,
		LANGUAGE,
		WEBTV,
		MENU,
		RPC
	}

	public static enum Scheduler
	{
		INTERNET,
		EPG,
		TICKER,
		SOFTWARE_UPDATE,
		DATA_LOADER,
		TEST
	}

	public static enum State
	{
		MENU,
		LOADING,
		TV,
		VOD,
		EPG,
		MESSAGE_BOX,
		PROGRAM_INFO,
		WATCHLIST,
		CHANNELS,
		CHANNELS_ALERT,
		SETTINGS,
		SETTINGS_ETHERNET,
		SETTINGS_WIFI,
		KEYBOARD,
		PROGRAMS,
		LANGUAGE,
		TELETEXT,
		HOLLYSTAR,
		HOLLYSTAR_VIDEO,
		WEBTV,
		WEBTV_VIDEO,
		SOFTWARE_UPDATE,
		RCU_WIZARD,
		NETWORK,
		BOOT_WIZARD
	}
}

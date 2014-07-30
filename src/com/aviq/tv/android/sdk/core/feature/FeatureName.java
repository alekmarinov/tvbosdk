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
		SPECIAL,
		RCU,
		PLAYER,
		TIMESHIFT,
		HTTP_SERVER,
		REGISTER,
		WATCHLIST,
		CHANNELS,
		ETHERNET,
		WIRELESS,
		LANGUAGE,
		WEBTV,
		RPC,
		SYSTEM,
		EASTER_EGG,
		CRASHLOG,
		TIMEZONE,
		NETHOGS,
		STANDBY,
		DEBUG,
		RECORDING_SCHEDULER
	}

	public static enum Scheduler
	{
		SPECIAL,
		INTERNET,
		EPG,
		TICKER,
		UPGRADE,
		MENU,
		TEST
	}

	public static enum State
	{
		SPECIAL,
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
		SETTINGS_WIRELESS,
		KEYBOARD,
		PROGRAMS,
		LANGUAGE,
		TELETEXT,
		HOLLYSTAR,
		HOLLYSTAR_VIDEO,
		WEBTV,
		WEBTV_VIDEO,
		YOUTUBE,
		YOUTUBE_VIDEO,
		UPGRADE_WIZARD,
		RCU_WIZARD,
		LANGUAGE_WIZARD,
		NETWORK_WIZARD,
		BOOT_WIZARD,
		STANDBY,
		MEDIA,
		NOTIFICATION
	}
}

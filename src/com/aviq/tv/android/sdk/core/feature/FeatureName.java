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
 */
public class FeatureName
{
	public static enum Component
	{
		// FIXME: Redesign special concept by using project specific feature space
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
		SYSTEM_STAT,
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
		// FIXME: Redesign special concept by using project specific feature space
		SPECIAL,
		INTERNET,
		EPG,
		// FIXME: Scheduler TICKER is project specific
		TICKER,
		UPGRADE,
		// FIXME: Scheduler MENU is project specific
		MENU,
		EVENT_COLLECTOR,
		VOD
	}

	public static enum State
	{
		// FIXME: Redesign special concept by using project specific feature space
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
		// FIXME: Duplicate feature name, use VOD instead
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
		// FIXME: Deprecated
		@Deprecated
		BOOT_WIZARD,
		STANDBY,
		MEDIA,
		// FIXME: What is this feature?
		NOTIFICATION,
		RECORDING_SCHEDULER
	}
}

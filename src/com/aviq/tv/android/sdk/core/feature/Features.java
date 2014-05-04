/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    Feature.java
 * Author:      alek
 * Date:        2 May 2014
 * Description:
 */

package com.aviq.tv.android.sdk.core.feature;

import java.lang.reflect.Field;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.easteregg.FeatureEasterEgg;
import com.aviq.tv.android.sdk.feature.channels.FeatureChannels;
import com.aviq.tv.android.sdk.feature.crashlog.FeatureCrashLog;
import com.aviq.tv.android.sdk.feature.epg.FeatureEPG;
import com.aviq.tv.android.sdk.feature.httpserver.FeatureHttpServer;
import com.aviq.tv.android.sdk.feature.internet.FeatureInternet;
import com.aviq.tv.android.sdk.feature.language.FeatureLanguage;
import com.aviq.tv.android.sdk.feature.network.FeatureEthernet;
import com.aviq.tv.android.sdk.feature.network.FeatureWireless;
import com.aviq.tv.android.sdk.feature.player.FeaturePlayer;
import com.aviq.tv.android.sdk.feature.player.FeatureStreamer;
import com.aviq.tv.android.sdk.feature.player.FeatureTimeshift;
import com.aviq.tv.android.sdk.feature.rcu.FeatureRCU;
import com.aviq.tv.android.sdk.feature.register.FeatureRegister;
import com.aviq.tv.android.sdk.feature.rpc.FeatureRPC;
import com.aviq.tv.android.sdk.feature.system.FeatureNethogs;
import com.aviq.tv.android.sdk.feature.system.FeatureSystem;
import com.aviq.tv.android.sdk.feature.system.FeatureTimeZone;
import com.aviq.tv.android.sdk.feature.upgrade.FeatureUpgrade;
import com.aviq.tv.android.sdk.feature.watchlist.FeatureWatchlist;
import com.aviq.tv.android.sdk.feature.webtv.FeatureWebTV;

/**
 * Class with references to all features
 */
public class Features
{
	private static final String TAG = Features.class.getSimpleName();

	public Components Component = new Components();
	public Schedulers Scheduler = new Schedulers();
	public States State = new States();

	public class Components
	{
		public FeatureRCU RCU;
		public FeaturePlayer PLAYER;
		public FeatureTimeshift TIMESHIFT;
		public FeatureHttpServer HTTP_SERVER;
		public FeatureRegister REGISTER;
		public FeatureWatchlist WATCHLIST;
		public FeatureChannels CHANNELS;
		public FeatureEthernet ETHERNET;
		public FeatureWireless WIRELESS;
		public FeatureLanguage LANGUAGE;
		public FeatureWebTV WEBTV;
		public FeatureRPC RPC;
		public FeatureSystem SYSTEM;
		public FeatureEasterEgg EASTER_EGG;
		public FeatureCrashLog CRASHLOG;
		public FeatureTimeZone TIMEZONE;
		public FeatureNethogs NETHOGS;
		public FeatureStreamer STREAMER;
	}

	public class Schedulers
	{
		public FeatureInternet INTERNET;
		public FeatureEPG EPG;
		public FeatureUpgrade UPGRADE;
	}

	public class States
	{
		public FeatureState MENU;
		public FeatureState LOADING;
		public FeatureState TV;
		public FeatureState VOD;
		public FeatureState EPG;
		public FeatureState MESSAGE_BOX;
		public FeatureState PROGRAM_INFO;
		public FeatureState WATCHLIST;
		public FeatureState CHANNELS;
		public FeatureState CHANNELS_ALERT;
		public FeatureState SETTINGS;
		public FeatureState SETTINGS_ETHERNET;
		public FeatureState SETTINGS_WIRELESS;
		public FeatureState KEYBOARD;
		public FeatureState PROGRAMS;
		public FeatureState LANGUAGE;
		public FeatureState TELETEXT;
		public FeatureState HOLLYSTAR;
		public FeatureState HOLLYSTAR_VIDEO;
		public FeatureState WEBTV;
		public FeatureState WEBTV_VIDEO;
		public FeatureState YOUTUBE;
		public FeatureState YOUTUBE_VIDEO;
		public FeatureState UPGRADE_WIZARD;
		public FeatureState RCU_WIZARD;
		public FeatureState LANGUAGE_WIZARD;
		public FeatureState NETWORK_WIZARD;
		public FeatureState BOOT_WIZARD;
		public FeatureState STANDBY;
		public FeatureState MEDIA;
	}

	public Features(FeatureSet featureSet) throws FeatureNotFoundException
	{
		for (FeatureName.Component component : featureSet.Components)
		{
			setComponent(component);
		}
		for (FeatureName.Scheduler scheduler : featureSet.Schedulers)
		{
			setScheduler(scheduler);
		}
		for (FeatureName.State state : featureSet.States)
		{
			setState(state);
		}
	}

	// public class Components
	// {
	// public FeatureRCU RCU = (FeatureRCU)
	// Environment.getInstance().getFeatureComponent(FeatureName.Component.RCU);
	// public FeaturePlayer PLAYER = (FeaturePlayer)
	// Environment.getInstance().getFeatureComponent(
	// FeatureName.Component.PLAYER);
	// public FeatureTimeshift TIMESHIFT = (FeatureTimeshift)
	// Environment.getInstance().getFeatureComponent(
	// FeatureName.Component.TIMESHIFT);
	// public FeatureHttpServer HTTP_SERVER = (FeatureHttpServer)
	// Environment.getInstance().getFeatureComponent(
	// FeatureName.Component.HTTP_SERVER);
	// public FeatureRegister REGISTER = (FeatureRegister)
	// Environment.getInstance().getFeatureComponent(
	// FeatureName.Component.REGISTER);
	// public FeatureWatchlist WATCHLIST = (FeatureWatchlist)
	// Environment.getInstance().getFeatureComponent(
	// FeatureName.Component.WATCHLIST);
	// public FeatureChannels CHANNELS = (FeatureChannels)
	// Environment.getInstance().getFeatureComponent(
	// FeatureName.Component.CHANNELS);
	// public FeatureEthernet ETHERNET = (FeatureEthernet)
	// Environment.getInstance().getFeatureComponent(
	// FeatureName.Component.ETHERNET);
	// public FeatureWireless WIRELESS = (FeatureWireless)
	// Environment.getInstance().getFeatureComponent(
	// FeatureName.Component.WIRELESS);
	// public FeatureLanguage LANGUAGE = (FeatureLanguage)
	// Environment.getInstance().getFeatureComponent(
	// FeatureName.Component.LANGUAGE);
	// public FeatureWebTV WEBTV = (FeatureWebTV)
	// Environment.getInstance().getFeatureComponent(
	// FeatureName.Component.WEBTV);
	// public FeatureRPC RPC = (FeatureRPC)
	// Environment.getInstance().getFeatureComponent(FeatureName.Component.RPC);
	// public FeatureSystem SYSTEM = (FeatureSystem)
	// Environment.getInstance().getFeatureComponent(
	// FeatureName.Component.SYSTEM);
	// public FeatureEasterEgg EASTER_EGG = (FeatureEasterEgg)
	// Environment.getInstance().getFeatureComponent(
	// FeatureName.Component.EASTER_EGG);
	// public FeatureCrashLog CRASHLOG = (FeatureCrashLog)
	// Environment.getInstance().getFeatureComponent(
	// FeatureName.Component.CRASHLOG);
	// public FeatureTimeZone TIMEZONE = (FeatureTimeZone)
	// Environment.getInstance().getFeatureComponent(
	// FeatureName.Component.TIMEZONE);
	// public FeatureNethogs NETHOGS = (FeatureNethogs)
	// Environment.getInstance().getFeatureComponent(
	// FeatureName.Component.NETHOGS);
	// public FeatureStreamer STREAMER = (FeatureStreamer)
	// Environment.getInstance().getFeatureComponent(
	// FeatureName.Component.STREAMER);
	// }
	//
	// public class Schedulers
	// {
	// public FeatureInternet INTERNET = (FeatureInternet)
	// Environment.getInstance().getFeatureScheduler(
	// FeatureName.Scheduler.INTERNET);
	// public FeatureEPG EPG = (FeatureEPG)
	// Environment.getInstance().getFeatureScheduler(FeatureName.Scheduler.EPG);
	// public FeatureUpgrade UPGRADE = (FeatureUpgrade)
	// Environment.getInstance().getFeatureScheduler(
	// FeatureName.Scheduler.UPGRADE);
	// }
	//
	// public class States
	// {
	// public FeatureState MENU =
	// Environment.getInstance().getFeatureState(FeatureName.State.MENU);
	// public FeatureState LOADING =
	// Environment.getInstance().getFeatureState(FeatureName.State.LOADING);
	// public FeatureState TV =
	// Environment.getInstance().getFeatureState(FeatureName.State.TV);
	// public FeatureState VOD =
	// Environment.getInstance().getFeatureState(FeatureName.State.VOD);
	// public FeatureState EPG =
	// Environment.getInstance().getFeatureState(FeatureName.State.EPG);
	// public FeatureState MESSAGE_BOX =
	// Environment.getInstance().getFeatureState(FeatureName.State.MESSAGE_BOX);
	// public FeatureState PROGRAM_INFO =
	// Environment.getInstance().getFeatureState(FeatureName.State.PROGRAM_INFO);
	// public FeatureState WATCHLIST =
	// Environment.getInstance().getFeatureState(FeatureName.State.WATCHLIST);
	// public FeatureState CHANNELS =
	// Environment.getInstance().getFeatureState(FeatureName.State.CHANNELS);
	// public FeatureState CHANNELS_ALERT = Environment.getInstance()
	// .getFeatureState(FeatureName.State.CHANNELS_ALERT);
	// public FeatureState SETTINGS =
	// Environment.getInstance().getFeatureState(FeatureName.State.SETTINGS);
	// public FeatureState SETTINGS_ETHERNET =
	// Environment.getInstance().getFeatureState(
	// FeatureName.State.SETTINGS_ETHERNET);
	// public FeatureState SETTINGS_WIRELESS =
	// Environment.getInstance().getFeatureState(
	// FeatureName.State.SETTINGS_WIRELESS);
	// public FeatureState KEYBOARD =
	// Environment.getInstance().getFeatureState(FeatureName.State.KEYBOARD);
	// public FeatureState PROGRAMS =
	// Environment.getInstance().getFeatureState(FeatureName.State.PROGRAMS);
	// public FeatureState LANGUAGE =
	// Environment.getInstance().getFeatureState(FeatureName.State.LANGUAGE);
	// public FeatureState TELETEXT =
	// Environment.getInstance().getFeatureState(FeatureName.State.TELETEXT);
	// public FeatureState HOLLYSTAR =
	// Environment.getInstance().getFeatureState(FeatureName.State.HOLLYSTAR);
	// public FeatureState HOLLYSTAR_VIDEO =
	// Environment.getInstance().getFeatureState(
	// FeatureName.State.HOLLYSTAR_VIDEO);
	// public FeatureState WEBTV =
	// Environment.getInstance().getFeatureState(FeatureName.State.WEBTV);
	// public FeatureState WEBTV_VIDEO =
	// Environment.getInstance().getFeatureState(FeatureName.State.WEBTV_VIDEO);
	// public FeatureState YOUTUBE =
	// Environment.getInstance().getFeatureState(FeatureName.State.YOUTUBE);
	// public FeatureState YOUTUBE_VIDEO =
	// Environment.getInstance().getFeatureState(FeatureName.State.YOUTUBE_VIDEO);
	// public FeatureState UPGRADE_WIZARD = Environment.getInstance()
	// .getFeatureState(FeatureName.State.UPGRADE_WIZARD);
	// public FeatureState RCU_WIZARD =
	// Environment.getInstance().getFeatureState(FeatureName.State.RCU_WIZARD);
	// public FeatureState LANGUAGE_WIZARD =
	// Environment.getInstance().getFeatureState(
	// FeatureName.State.LANGUAGE_WIZARD);
	// public FeatureState NETWORK_WIZARD = Environment.getInstance()
	// .getFeatureState(FeatureName.State.NETWORK_WIZARD);
	// public FeatureState BOOT_WIZARD =
	// Environment.getInstance().getFeatureState(FeatureName.State.BOOT_WIZARD);
	// public FeatureState STANDBY =
	// Environment.getInstance().getFeatureState(FeatureName.State.STANDBY);
	// public FeatureState MEDIA =
	// Environment.getInstance().getFeatureState(FeatureName.State.MEDIA);
	// }

	private void setComponent(FeatureName.Component featureName) throws FeatureNotFoundException
	{
		setField(Component, featureName.name(), Environment.getInstance().getFeatureManager().use(featureName));
	}

	private void setScheduler(FeatureName.Scheduler featureName) throws FeatureNotFoundException
	{
		setField(Scheduler, featureName.name(), Environment.getInstance().getFeatureManager().use(featureName));
	}

	private void setState(FeatureName.State featureName) throws FeatureNotFoundException
	{
		setField(State, featureName.name(), Environment.getInstance().getFeatureManager().use(featureName));
	}

	private void setField(Object featureType, String fieldName, Object object)
	{
		try
		{
			Log.d(TAG, ".setField: clazz = " + featureType.getClass().getSimpleName() + ", fieldName = " + fieldName + ", object = "
			        + object);
			Field field = featureType.getClass().getField(fieldName);
			field.set(featureType, object);
		}
		catch (IllegalArgumentException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IllegalAccessException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (NoSuchFieldException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
	}
}

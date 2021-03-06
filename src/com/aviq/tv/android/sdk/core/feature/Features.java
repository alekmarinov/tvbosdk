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
import com.aviq.tv.android.sdk.feature.channels.FeatureChannels;
import com.aviq.tv.android.sdk.feature.command.FeatureCommand;
import com.aviq.tv.android.sdk.feature.display.FeatureDisplay;
import com.aviq.tv.android.sdk.feature.dlna.FeatureDLNA;
import com.aviq.tv.android.sdk.feature.easteregg.FeatureEasterEgg;
import com.aviq.tv.android.sdk.feature.epg.FeatureEPG;
import com.aviq.tv.android.sdk.feature.epg.FeatureEPGCompat;
import com.aviq.tv.android.sdk.feature.httpserver.FeatureHttpServer;
import com.aviq.tv.android.sdk.feature.internet.FeatureInternet;
import com.aviq.tv.android.sdk.feature.internet.FeatureNetworkTime;
import com.aviq.tv.android.sdk.feature.language.FeatureLanguage;
import com.aviq.tv.android.sdk.feature.network.FeatureEthernet;
import com.aviq.tv.android.sdk.feature.network.FeatureNetwork;
import com.aviq.tv.android.sdk.feature.network.FeatureWireless;
import com.aviq.tv.android.sdk.feature.player.FeaturePlayer;
import com.aviq.tv.android.sdk.feature.player.FeatureTimeshift;
import com.aviq.tv.android.sdk.feature.rcu.FeatureRCU;
import com.aviq.tv.android.sdk.feature.recording.FeatureRecordingScheduler;
import com.aviq.tv.android.sdk.feature.register.FeatureRegister;
import com.aviq.tv.android.sdk.feature.rpc.FeatureRPC;
import com.aviq.tv.android.sdk.feature.system.FeatureDevice;
import com.aviq.tv.android.sdk.feature.system.FeatureExecutor;
import com.aviq.tv.android.sdk.feature.system.FeatureStandBy;
import com.aviq.tv.android.sdk.feature.system.FeatureTimeZone;
import com.aviq.tv.android.sdk.feature.upgrade.FeatureUpgrade;
import com.aviq.tv.android.sdk.feature.vod.FeatureVOD;
import com.aviq.tv.android.sdk.feature.volume.FeatureVolume;
import com.aviq.tv.android.sdk.feature.watchlist.FeatureWatchlist;
import com.aviq.tv.android.sdk.feature.weather.FeatureWeather;
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
		public FeatureEPG EPG;
		public FeatureHttpServer HTTP_SERVER;
		public FeatureRegister REGISTER;
		public FeatureWatchlist WATCHLIST;
		public FeatureChannels CHANNELS;
		public FeatureNetwork NETWORK;
		public FeatureDisplay DISPLAY;
		public FeatureEthernet ETHERNET;
		public FeatureWireless WIRELESS;
		public FeatureLanguage LANGUAGE;
		public FeatureWebTV WEBTV;
		public FeatureRPC RPC;
		public FeatureExecutor SYSTEM;
		public FeatureEasterEgg EASTER_EGG;
		public FeatureTimeZone TIMEZONE;
		public FeatureStandBy STANDBY;
		public FeatureRecordingScheduler RECORDING_SCHEDULER;
		public FeatureDevice DEVICE;
		public FeatureVolume VOLUME;
		public FeatureComponent CRASHLOG;
		public FeatureCommand COMMAND;
		public FeatureDLNA DLNA;
		public FeatureNetworkTime NETWORK_TIME;
	}

	public class Schedulers
	{
		public FeatureInternet INTERNET;
		public FeatureEPGCompat EPG;
		public FeatureUpgrade UPGRADE;
		public FeatureScheduler EVENT_COLLECTOR;
		public FeatureVOD VOD;
		public FeatureWeather WEATHER;
	}

	public class States
	{
		public FeatureState MENU;
		public FeatureState LOADING;
		public FeatureState TV;
		public FeatureState VOD_GROUPS;
		public FeatureState VOD_ITEMS;
		public FeatureState VOD_DETAILS;
		public FeatureState VOD_PLAYER;
		public FeatureState VOD_SEARCH;
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
		public FeatureState RECORDINGS;
		public FeatureState RECORDING_ITEMS;
		public FeatureState RECORDING_DETAILS;
		public FeatureState ERROR;
		public FeatureState PASSWORD;
		public FeatureState FAVOURITE_CHANNELS;
		public FeatureState PROGRAM_ITEMS;
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
			Log.d(TAG, ".setField: clazz = " + featureType.getClass().getSimpleName() + ", fieldName = " + fieldName
			        + ", object = " + object);
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

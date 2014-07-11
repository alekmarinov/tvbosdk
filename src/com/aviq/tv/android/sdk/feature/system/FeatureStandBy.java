/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureStandBy.java
 * Author:      alek
 * Date:        15 May 2014
 * Description: Controls standby logic
 */

package com.aviq.tv.android.sdk.feature.system;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.EventReceiver;
import com.aviq.tv.android.sdk.core.Key;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.PriorityFeature;
import com.aviq.tv.android.sdk.core.feature.easteregg.FeatureEasterEgg;
import com.aviq.tv.android.sdk.feature.rcu.ime.RcuIMEService;
import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Controls standby logic
 */
@PriorityFeature
public class FeatureStandBy extends FeatureComponent implements EventReceiver
{
	public static final String TAG = FeatureStandBy.class.getSimpleName();

	// Triggered very soon (determined by param STANDBY_DELAY) before getting
	// into stand by mode.
	public static final int ON_STANDBY_ENTER = EventMessenger.ID("ON_STANDBY_ENTER");
	public static final String EXTRA_IS_AUTO_STANDBY = "IS_AUTO_STANDBY";

	// Triggered to warn about auto standing by in a certain short time
	public static final int ON_STANDBY_AUTO_WARNING = EventMessenger.ID("ON_STANDBY_AUTO_WARNING");
	public static final String EXTRA_TIME_TO_STANDBY = "TIME_TO_STANDBY";

	// Triggered on leaving standby mode
	public static final int ON_STANDBY_LEAVE = EventMessenger.ID("ON_STANDBY_LEAVE");

	// Triggered when enabling or disabling the auto standby feature
	public static final int ON_STANDBY_AUTO_ENABLED = EventMessenger.ID("ON_STANDBY_AUTO_ENABLED");
	public static final int ON_STANDBY_AUTO_DISABLED = EventMessenger.ID("ON_STANDBY_AUTO_DISABLED");

	public enum Param
	{
		/** Turn on/off HDMI on standing by */
		IS_STANDBY_HDMI(false),

		/** Time delay before sending the box to real StandBy on standby request */
		STANDBY_DELAY(2500),

		/**
		 * Put the box in standby automatically after the specified time without
		 * user activity. Set 0 to disable auto-standby
		 */
		AUTO_STANDBY_TIMEOUT(0),

		/** Time to keep sending auto-standby warning events */
		AUTO_STANDBY_WARN_TIMEOUT(60 * 1000);

		Param(boolean value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.State.STANDBY).put(name(), value);
		}

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.State.STANDBY).put(name(), value);
		}

		Param(String value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.State.STANDBY).put(name(), value);
		}
	}

	private int _autoStandByWarnTimeout;
	private int _autoStandbyTimeout;
	private boolean _isStandByHDMI;

	public FeatureStandBy() throws FeatureNotFoundException
	{
		require(FeatureName.Scheduler.INTERNET);
		require(FeatureName.Component.SYSTEM);
		require(FeatureName.Component.EASTER_EGG);
	}

	@Override
	public FeatureName.Component getComponentName()
	{
		return FeatureName.Component.STANDBY;
	}

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");

		// Environment.getInstance().getEventMessenger().register(this,
		// Environment.ON_KEY_PRESSED);
		_feature.Component.EASTER_EGG.getEventMessenger().register(this, FeatureEasterEgg.ON_KEY_SEQUENCE);

		// RcuIMEService will broadcast BROADCAST_ACTION_SLEEP in response to
		// sleep button pressed. This event may occur at any time even when the
		// current activity holding this fragment is inactive (e.g. on pause)
		Log.i(TAG, "Registering on " + RcuIMEService.BROADCAST_ACTION_SLEEP);
		IntentFilter intentFilter = new IntentFilter(RcuIMEService.BROADCAST_ACTION_SLEEP);
		Environment.getInstance().registerReceiver(new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				Log.i(TAG, ".onReceive: action = " + intent.getAction());
				if (RcuIMEService.BROADCAST_ACTION_SLEEP.equals(intent.getAction()))
				{
					if (_isStandByHDMI && isHDMIOff())
					{
						Log.i(TAG, "Resume from standing by requested by user");
						getEventMessenger().trigger(ON_STANDBY_LEAVE);
						setHDMIEnabled(true);
						postponeAutoStandBy(_autoStandbyTimeout);
						Environment.getInstance().setKeyEventsEnabled();
					}
					else
					{
						Log.i(TAG, "Standing by requested by user");
						startStandBy(false);
						Environment.getInstance().setKeyEventsDisabled().except(Key.SLEEP);
					}
				}
			}
		}, intentFilter);

		_isStandByHDMI = getPrefs().getBool(Param.IS_STANDBY_HDMI);
		_autoStandbyTimeout = getPrefs().getInt(Param.AUTO_STANDBY_TIMEOUT);
		postponeAutoStandBy(_autoStandbyTimeout);

		super.initialize(onFeatureInitialized);
	}

	/**
	 * Go to StandBy mode. This method doesn't sends the box immediately to
	 * standby but after some time in which the standby can be interrupted by
	 * cancelStandBy(). The method will trigger ON_STANDBY_ENTER event with
	 * parameter EXTRA_IS_AUTO_STANDBY set to the value of isAuto parameter
	 *
	 * @param isAuto
	 */
	public void startStandBy(boolean isAuto)
	{
		Log.d(TAG, ".startStandBy: isAuto = " + isAuto);
		Bundle bundle = new Bundle();
		bundle.putBoolean(EXTRA_IS_AUTO_STANDBY, isAuto);
		getEventMessenger().trigger(ON_STANDBY_ENTER, bundle);

		getEventMessenger().removeCallbacks(_autoStandByRunnable);
		getEventMessenger().removeCallbacks(_autoStandByWarningRunnable);
		getEventMessenger().removeCallbacks(_enterStandByRunnable);
		getEventMessenger().postDelayed(_enterStandByRunnable, getPrefs().getInt(Param.STANDBY_DELAY));
	}

	/**
	 * Calls startStandBy(false)
	 */
	public void startStandBy()
	{
		startStandBy(false);
	}

	/**
	 * Interrupt standing by
	 */
	public void cancelStandBy()
	{
		getEventMessenger().removeCallbacks(_enterStandByRunnable);

		// remove warnings trigger
		getEventMessenger().removeCallbacks(_autoStandByWarningRunnable);
	}

	/**
	 * @return true if HDMI is enabled or false otherwise
	 */
	public boolean isHDMIOff()
	{
		try
		{
			String dispMode = TextUtils.inputStreamToString(new FileInputStream(
			        "/sys/class/amhdmitx/amhdmitx0/disp_mode"));
			return "VIC:0".equals(dispMode);
		}
		catch (FileNotFoundException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		return false;
	}

	/**
	 * Enable or disable HDMI
	 *
	 * @param isOn
	 *            true to enable or false to disable
	 */
	public void setHDMIEnabled(boolean isOn)
	{
		Log.i(TAG, ".setHDMIEnabled: isOn = " + isOn);
		if (isOn)
		{
			_feature.Component.SYSTEM.command("echo 720p > /sys/class/amhdmitx/amhdmitx0/disp_mode");
		}
		else
		{
			_feature.Component.SYSTEM.command("echo > /sys/class/amhdmitx/amhdmitx0/disp_mode");
		}
	}

	public void setStandByHDMI(boolean isStandByHDMI)
	{
		Log.i(TAG, ".setStandByHDMI: isStandByHDMI = " + isStandByHDMI);
		_isStandByHDMI = isStandByHDMI;
	}

	public boolean isStandByHDMI()
	{
		return _isStandByHDMI;
	}

	@Override
	public void onEvent(int msgId, Bundle bundle)
	{
		Log.i(TAG, ".onEvent: " + EventMessenger.idName(msgId) + TextUtils.implodeBundle(bundle));
		if (Environment.ON_KEY_PRESSED == msgId)
		{
			// Postpone auto standby on user activity
			postponeAutoStandBy(_autoStandbyTimeout);
		}
		else if (FeatureEasterEgg.ON_KEY_SEQUENCE == msgId)
		{
			String keySeq = bundle.getString(FeatureEasterEgg.EXTRA_KEY_SEQUENCE);
			// FIXME: declare key sequences as constants
			if ("RR272RR".equals(keySeq) || "YGRB11".equals(keySeq))
			{
				if (_autoStandbyTimeout > 0)
				{
					Log.i(TAG, "Auto standby disabled.");
					_autoStandbyTimeout = 0;
					getEventMessenger().removeCallbacks(_autoStandByRunnable);
					getEventMessenger().trigger(ON_STANDBY_AUTO_DISABLED);
				}
				else
				{
					Log.i(TAG, "Auto standby enabled.");
					_autoStandbyTimeout = getPrefs().getInt(Param.AUTO_STANDBY_TIMEOUT);
					postponeAutoStandBy(_autoStandbyTimeout);
					getEventMessenger().trigger(ON_STANDBY_AUTO_ENABLED);
				}
			}
		}
	}

	private void postponeAutoStandBy(int autoStandbyTimeout)
	{
		if (autoStandbyTimeout > 0)
		{
			// postpones auto standby
			getEventMessenger().removeCallbacks(_autoStandByRunnable);
			getEventMessenger().postDelayed(_autoStandByRunnable, autoStandbyTimeout);
			Log.i(TAG, ".postponeAutoStandBy: timeout = " + (autoStandbyTimeout / 1000) + " secs");

			// remove warnings trigger
			getEventMessenger().removeCallbacks(_autoStandByWarningRunnable);
		}
	}

	private final Runnable _enterStandByRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			if (_isStandByHDMI)
			{
				setHDMIEnabled(false);
			}
			else
			{
				// Go to StandBy now
				Log.i(TAG, "Entering standby mode...");

				// Send device to standby by emulating a key press for key 26
				_feature.Component.SYSTEM.command("input keyevent 26");
			}
		}
	};

	// Runnable callback executed when the auto standby timeout elapses which
	// will start triggering auto standby warning events for time determined by
	// AUTO_STANDBY_WARN_TIMEOUT param
	private final Runnable _autoStandByRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			Log.i(TAG, "Trigger auto standby warning event");

			// Start runnable to trigger auto-standby warning events

			_autoStandByWarnTimeout = getPrefs().getInt(Param.AUTO_STANDBY_WARN_TIMEOUT);
			getEventMessenger().post(_autoStandByWarningRunnable);
		}
	};

	private final Runnable _autoStandByWarningRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			Bundle bundle = new Bundle();
			bundle.putInt(EXTRA_TIME_TO_STANDBY, _autoStandByWarnTimeout);
			getEventMessenger().trigger(ON_STANDBY_AUTO_WARNING, bundle);

			_autoStandByWarnTimeout -= 1000;
			if (_autoStandByWarnTimeout > -1)
			{
				getEventMessenger().postDelayed(this, 1000);
			}
			else
			{
				startStandBy(true);
			}
		}
	};
}

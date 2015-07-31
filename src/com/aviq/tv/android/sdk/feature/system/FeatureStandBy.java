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
import java.io.IOException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.EventReceiver;
import com.aviq.tv.android.sdk.core.Key;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;
import com.aviq.tv.android.sdk.core.feature.annotation.Priority;
import com.aviq.tv.android.sdk.feature.rcu.FeatureRCU;
import com.aviq.tv.android.sdk.feature.rcu.ime.RcuIMEService;
import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Controls standby logic
 */
@Priority
@Author("alek")
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

	// Triggered when the standby is canceled by user
	public static final int ON_STANDBY_CANCEL = EventMessenger.ID("ON_STANDBY_CANCEL");

	private static final String ACTION_HDMI_HW_PLUGGED = "android.intent.action.HDMI_HW_PLUGGED";
	private static final String EXTRA_HDMI_HW_PLUGGED_STATE = "state";

	public static enum Param
	{
		/** Enter standby when HDMI cable is unplugged */
		IS_STANDBY_BY_HDMI_OFF(false),

		/** Turn on/off HDMI on standing by */
		IS_STANDBY_HDMI(false),

		/** Time delay before sending the box to real StandBy on standby request */
		STANDBY_DELAY(1500),

		/**
		 * Put the box in standby automatically after the specified time
		 * (milliseconds) without
		 * user activity. Set 0 to disable auto-standby
		 */
		AUTO_STANDBY_TIMEOUT(0),

		/** Time to keep sending auto-standby warning events */
		AUTO_STANDBY_WARN_TIMEOUT(60 * 1000),

		/** Automatically postpone standby on user activity (key pressed) */
		AUTO_POSTPONE_STANDBY(true);

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

	private boolean _isWarningProgress;
	private int _autoStandByWarnTimeout;
	private long _autoStandbyTimeout;
	private boolean _isStandByHDMI;
	private boolean _autoPostponeStandby;

	public FeatureStandBy() throws FeatureNotFoundException
	{
		require(FeatureName.Scheduler.INTERNET);
		require(FeatureName.Component.RCU);
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

		_autoPostponeStandby = getPrefs().getBool(Param.AUTO_POSTPONE_STANDBY);
		if (_autoPostponeStandby)
			_feature.Component.RCU.getEventMessenger().register(this, FeatureRCU.ON_KEY_PRESSED);

		// RcuIMEService will broadcast BROADCAST_ACTION_SLEEP in response to
		// sleep button pressed. This event may occur at any time even when the
		// current activity holding this fragment is inactive (e.g. on pause)
		Log.i(TAG, "Registering on " + RcuIMEService.BROADCAST_ACTION_SLEEP);
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(RcuIMEService.BROADCAST_ACTION_SLEEP);
		intentFilter.addAction(Intent.ACTION_SCREEN_ON);
		intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
		if (getPrefs().getBool(Param.IS_STANDBY_BY_HDMI_OFF))
		{
			intentFilter.addAction(ACTION_HDMI_HW_PLUGGED);
		}
		Environment.getInstance().registerReceiver(new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				String action = intent.getAction();
				Log.i(TAG, ".onReceive: action = " + action);
				if (RcuIMEService.BROADCAST_ACTION_SLEEP.equals(action))
				{
					if (_isStandByHDMI && isHDMIOff())
					{
						Log.i(TAG, "Resume from standing by requested by user");
						getEventMessenger().trigger(ON_STANDBY_LEAVE);
						setHDMIEnabled(true);
						postponeAutoStandBy();
						Environment.getInstance().setKeyEventsEnabled();
					}
					else
					{
						Log.i(TAG, "Standing by requested by user");
						startStandBy(false);
						Environment.getInstance().setKeyEventsDisabled().except(Key.SLEEP);
					}
				}
				else if (ACTION_HDMI_HW_PLUGGED.equals(action))
				{
					boolean plugged = intent.getBooleanExtra(EXTRA_HDMI_HW_PLUGGED_STATE, false);
					if (!plugged)
					{
						// This method is not reliable on some TV
						// enter standby immediately
						// _enterStandByRunnable.run();
					}
				}
				else if (Intent.ACTION_SCREEN_ON.equals(action))
				{
					getEventMessenger().trigger(ON_STANDBY_LEAVE);
					postponeAutoStandBy();
				}
				else if (Intent.ACTION_SCREEN_OFF.equals(action))
				{
					getEventMessenger().trigger(ON_STANDBY_ENTER);
				}
			}
		}, intentFilter);

		_isStandByHDMI = getPrefs().getBool(Param.IS_STANDBY_HDMI);
		int autoStandByTimeout = getPrefs().getInt(Param.AUTO_STANDBY_TIMEOUT);
		if (autoStandByTimeout > 0)
			setAutoStandByTimeout(autoStandByTimeout);

		super.initialize(onFeatureInitialized);
	}

	/**
	 * Sets new auto-standby timeout and postpone standby with the new period
	 *
	 * @param autoStandbyTimeout
	 */
	public void setAutoStandByTimeout(long autoStandbyTimeout)
	{
		Log.i(TAG, ".setAutoStandByTimeout: autoStandbyTimeout = " + (autoStandbyTimeout / 1000) + " secs");
		_autoStandbyTimeout = autoStandbyTimeout;
		postponeAutoStandBy();
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
		_isWarningProgress = false;
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
		_isWarningProgress = false;
	}

	/**
	 * @return true if HDMI is disabled or false otherwise
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
			sendSystemCommand("echo 720p > /sys/class/amhdmitx/amhdmitx0/disp_mode");
		}
		else
		{
			sendSystemCommand("echo > /sys/class/amhdmitx/amhdmitx0/disp_mode");
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
		super.onEvent(msgId, bundle);
		Log.i(TAG, ".onEvent: " + EventMessenger.idName(msgId) + TextUtils.implodeBundle(bundle));
		if (FeatureRCU.ON_KEY_PRESSED == msgId)
		{
			Log.i(TAG, "_autoPostponeStandby -> " + _autoPostponeStandby+ ", _isWarningProgress -> " + _isWarningProgress);
			if (_autoPostponeStandby || _isWarningProgress)
			{
				getEventMessenger().trigger(ON_STANDBY_CANCEL);
				postponeAutoStandBy();
			}
		}
	}

	public void postponeAutoStandBy()
	{
		Log.i(TAG, ".postponeAutoStandBy: timeout = " + (_autoStandbyTimeout / 1000)
		        + " secs, _isWarningProgress = " + _isWarningProgress);
		if (_autoStandbyTimeout > 0)
		{
			// postpones auto standby
			getEventMessenger().removeCallbacks(_autoStandByRunnable);
			getEventMessenger().postDelayed(_autoStandByRunnable, _autoStandbyTimeout);

			// remove warnings trigger
			getEventMessenger().removeCallbacks(_autoStandByWarningRunnable);
			_isWarningProgress = false;
		}
		else
		{
			// cancels standby
			getEventMessenger().removeCallbacks(_autoStandByRunnable);
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
				doStandBy();
			}
		}
	};

	protected void doStandBy()
	{
		// Send device to standby by emulating a key press for key 26
		sendSystemCommand("input keyevent 26");
	}

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
			_isWarningProgress = true;

			_autoStandByWarnTimeout -= 1000;
			if (_autoStandByWarnTimeout >= 0)
			{
				getEventMessenger().postDelayed(this, 1000);
			}
			else
			{
				startStandBy(true);
				_isWarningProgress = false;
			}
		}
	};

	private void sendSystemCommand(String cmd)
	{
		try
		{
			Log.i(TAG, ".sendSystemCommand: " + cmd);
			Runtime.getRuntime().exec(cmd);
		}
		catch (IOException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
	}
}

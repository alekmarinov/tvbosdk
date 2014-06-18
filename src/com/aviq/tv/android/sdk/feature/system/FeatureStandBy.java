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
	private boolean _isStandByHDMI;

	public FeatureStandBy() throws FeatureNotFoundException
	{
		require(FeatureName.Scheduler.INTERNET);
		require(FeatureName.Component.SYSTEM);
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
		Environment.getInstance().getEventMessenger().register(this, Environment.ON_KEY_PRESSED);
		_isStandByHDMI = getPrefs().getBool(Param.IS_STANDBY_HDMI);
		postponeAutoStandBy();
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

		// remove warnings triggerer
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

	@Override
	public void onEvent(int msgId, Bundle bundle)
	{
		Log.i(TAG, ".onEvent: " + EventMessenger.idName(msgId) + TextUtils.implodeBundle(bundle));
		if (Environment.ON_KEY_PRESSED == msgId)
		{
			Key key = Key.valueOf(bundle.getString(Environment.EXTRA_KEY));
			if (Key.SLEEP.equals(key))
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
			else
			{
				// Postpone auto standby on user activity
				postponeAutoStandBy();
			}
		}
	}

	private void postponeAutoStandBy()
	{
		int timeout = getPrefs().getInt(Param.AUTO_STANDBY_TIMEOUT);
		if (timeout > 0)
		{
			// postpones auto standby
			getEventMessenger().removeCallbacks(_autoStandByRunnable);
			getEventMessenger().postDelayed(_autoStandByRunnable, timeout);
			Log.i(TAG, ".postponeAutoStandBy: timeout = " + (timeout / 1000) + " secs");

			// remove warnings triggerer
			getEventMessenger().removeCallbacks(_autoStandByWarningRunnable);
		}
	}

	private final Runnable _detectStandbyLeave = new Runnable()
	{
		private long _lastCurrentTime = 0;

		@Override
		public void run()
		{
			if (_lastCurrentTime == 0)
			{
				// Set last current time to now
				_lastCurrentTime = System.currentTimeMillis();
			}

			// time left since the last current time was set
			long timeLeft = (System.currentTimeMillis() - _lastCurrentTime);
			Log.i(TAG, "_detectStandByExit: timeLeft = " + timeLeft);
			if (timeLeft > 1500)
			{
				Log.i(TAG, "_detectStandByExit: Detected leaving standing by");

				Environment.getInstance().suicide();

//				if (!Environment.getInstance().isInitialized())
//				{
//					/**
//					 * If we come back from standby when the application has not
//					 * been fully initialized, then we restart it. This may happen
//					 * if the user puts the STB into standby while
//					 * FeatureStateLoading is active.
//					 */
//					Environment.getInstance().suicide();
//				}
//				else
//				{
//					getEventMessenger().trigger(ON_STANDBY_LEAVE);
//					postponeAutoStandBy();
//					Environment.getInstance().setKeyEventsEnabled();
//				}
			}
			else
			{
				// loop with one second delay and determine if the
				// elapsed time is as expected unless standby has
				// interrupted the loop
				_lastCurrentTime = System.currentTimeMillis();
				getEventMessenger().postDelayed(this, 1000);
			}
		}
	};

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
				// Start detection of StandBy leave
				getEventMessenger().removeCallbacks(_detectStandbyLeave);
				getEventMessenger().post(_detectStandbyLeave);

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

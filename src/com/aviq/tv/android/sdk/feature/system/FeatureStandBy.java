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

import android.os.Bundle;
import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.EventReceiver;
import com.aviq.tv.android.sdk.core.Key;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Controls standby logic
 */
public class FeatureStandBy extends FeatureComponent implements EventReceiver
{
	public static final String TAG = FeatureStandBy.class.getSimpleName();

	// Triggered very soon (determined by param STANDBY_DELAY) before getting
	// into stand by mode.
	public static final int ON_STANDBY_ENTER = EventMessenger.ID("ON_STANDBY_ENTER");

	// Triggered to warn about auto standing by in a certain short time
	public static final int ON_STANDBY_AUTO_WARNING = EventMessenger.ID("ON_STANDBY_AUTO_WARNING");
	public static final String EXTRA_TIME_TO_STANDBY = "TIME_TO_STANDBY";

	// Triggered on leaving standby mode
	public static final int ON_STANDBY_LEAVE = EventMessenger.ID("ON_STANDBY_LEAVE");

	public enum Param
	{
		/** Time delay before sending the box to real StandBy on standby request */
		STANDBY_DELAY(2500),

		/**
		 * Put the box in standby automatically after the specified time without
		 * user activity
		 */
		AUTO_STANDBY_TIMEOUT(3 * 60 * 60 * 1000),

		/** Time to keep sending auto-standby warning events */
		AUTO_STANDBY_WARN_TIMEOUT(60 * 1000);

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
		postponeAutoStandBy();
		super.initialize(onFeatureInitialized);
	}

	/**
	 * Go to StandBy mode. This method doesn't sends the box immediately to
	 * standby but after some time in which the standby can be interrupted by
	 * cancelStandBy()
	 */
	public void startStandBy()
	{
		Log.d(TAG, ".startStandBy");
		getEventMessenger().trigger(ON_STANDBY_ENTER);

		getEventMessenger().removeCallbacks(_autoStandByRunnable);
		getEventMessenger().removeCallbacks(_autoStandByWarningRunnable);
		getEventMessenger().removeCallbacks(_enterStandByRunnable);
		getEventMessenger().postDelayed(_enterStandByRunnable, getPrefs().getInt(Param.STANDBY_DELAY));
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

	@Override
	public void onEvent(int msgId, Bundle bundle)
	{
		Log.i(TAG, ".onEvent: " + EventMessenger.idName(msgId) + TextUtils.implodeBundle(bundle));
		if (Environment.ON_KEY_PRESSED == msgId)
		{
			Key key = Key.valueOf(bundle.getString(Environment.EXTRA_KEY));
			if (Key.SLEEP.equals(key))
			{
				Log.i(TAG, "Standing by requested by user");
				startStandBy();
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
		// postpones auto standby
		getEventMessenger().removeCallbacks(_autoStandByRunnable);
		int timeout = getPrefs().getInt(Param.AUTO_STANDBY_TIMEOUT);
		getEventMessenger().postDelayed(_autoStandByRunnable, timeout);
		Log.i(TAG, ".postponeAutoStand: timeout = " + (timeout / 1000) + " secs");

		// remove warnings triggerer
		getEventMessenger().removeCallbacks(_autoStandByWarningRunnable);
	}

	private final Runnable _enterStandByRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			// Start detection of StandBy leave
			getEventMessenger().post(new Runnable()
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
					if (timeLeft > 2000)
					{
						Log.i(TAG, "_detectStandByExit: Detected leaving standing by");
						getEventMessenger().trigger(ON_STANDBY_LEAVE);
						postponeAutoStandBy();
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
			});

			// Go to StandBy now
			Log.i(TAG, "Entering standby mode...");

			// Send device to standby by emulating a key press for key 26
			_feature.Component.SYSTEM.command("input keyevent 26");
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
				startStandBy();
			}
		}
	};
}

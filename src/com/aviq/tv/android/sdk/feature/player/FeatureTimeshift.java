/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    Timeshift.java
 * Author:      alek
 * Date:        21 Mar 2014
 * Description: Timeshift logic component
 */

package com.aviq.tv.android.sdk.feature.player;

import android.os.Bundle;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.EventReceiver;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Timeshift logic component
 */
public class FeatureTimeshift extends FeatureComponent implements EventReceiver
{
	public static final String TAG = FeatureTimeshift.class.getSimpleName();

	/**
	 * triggered when the player must be resumed due to exceeded timeshift
	 * buffer during pause
	 */
	public static final int ON_AUTO_RESUME = EventMessenger.ID("ON_AUTO_RESUME");
	public static final int ON_SEEK = EventMessenger.ID("ON_SEEK");
	public static final String EXTRA_PLAY_TIME_DELTA = "PLAY_TIME_DELTA";
	private long _timeshiftTimeStart;
	private long _pauseTimeStart;
	private long _playTimeDelta;
	private long _timeshiftDuration;
	private AutoResumer _autoResumer = new AutoResumer();

	public enum Param
	{
		/**
		 * Timeshift max buffer size in seconds. If > 0 then the buffer
		 * increases from 0 to parameter value, otherwise timeshiftDuration is
		 * determined by setTimeshiftDuration method
		 */
		TIMESHIFT_DURATION(0);

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.TIMESHIFT).put(name(), value);
		}
	}

	public FeatureTimeshift() throws FeatureNotFoundException
	{
		require(FeatureName.Component.PLAYER);
	}

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		_feature.Component.PLAYER.getEventMessenger().register(this, FeaturePlayer.ON_PLAY_PAUSE);
		super.initialize(onFeatureInitialized);
	}

	/**
	 * Set timeshift buffer size
	 */
	public void setTimeshiftDuration(long timeshiftDuration)
	{
		Log.i(TAG, ".setTimeshiftDuration: timeshiftDuration = " + timeshiftDuration);
		_timeshiftDuration = timeshiftDuration;
	}

	/**
	 * Start timeshift buffer recording
	 */
	public void startTimeshift()
	{
		Log.i(TAG, ".startTimeshift");
		_timeshiftTimeStart = currentTime();
		setTimeshiftDuration(0);
	}

	/**
	 * Seek at live position
	 *
	 * @return current time
	 */
	public long seekLive()
	{
		Log.i(TAG, ".seekLive");
		return seekAt(currentTime());
	}

	/**
	 * Seek at play time delta from live
	 *
	 * @param playTimeDelta
	 *            delta from real time in seconds
	 * @return adjusted timestamp to not exceed the timeshift buffer
	 */
	public long seekDelta(long playTimeDelta)
	{
		Log.i(TAG, ".seekDelta: " + playTimeDelta + " secs from live");
		return seekAt(currentTime() - playTimeDelta);
	}

	/**
	 * Seek at playable buffer by relative time
	 *
	 * @param secs
	 *            the number of secs to seek relatively
	 * @return adjusted timestamp to not exceed the timeshift buffer
	 */
	public long seekRel(long secs)
	{
		Log.i(TAG, ".seekRel: secs = " + secs);
		return seekAt(getPlayingTime() + secs);
	}

	/**
	 * Seek at playable buffer by absolute time
	 *
	 * @param timestamp
	 *            the time moment where to set the cursor position.
	 * @return adjusted timestamp to not exceed the timeshift buffer
	 */
	public long seekAt(long timestamp)
	{
		Log.i(TAG, ".seekAt: " + (timestamp - currentTime()) + " secs relative to current time");
		long adjustedTime = adjustInTimeshift(timestamp);
		setPlayTimeDelta(currentTime() - adjustedTime);
		return adjustedTime;
	}

	/**
	 * @return the current playing absolute time
	 */
	public long getPlayingTime()
	{
		if (_feature.Component.PLAYER.isPaused())
		{
			// returns constant position from the moment of pause subtracted
			// with the current delay from live
			return _pauseTimeStart - _playTimeDelta;
		}
		else
		{
			// returns moving position from current time subtracted with the
			// current delay from live
			return currentTime() - _playTimeDelta;
		}
	}

	/**
	 * @return the playable duration in timeshift buffer (secs)
	 */
	public long getTimeshiftDuration()
	{
		if (getPrefs().getInt(Param.TIMESHIFT_DURATION) > 0)
			return Math.min(getPrefs().getInt(Param.TIMESHIFT_DURATION), currentTime() - _timeshiftTimeStart);
		else
			return _timeshiftDuration;
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.TIMESHIFT;
	}

	/**
	 * @return current time stamp in seconds
	 */
	public long currentTime()
	{
		return System.currentTimeMillis() / 1000;
	}

	@Override
	public void onEvent(int msgId, Bundle bundle)
	{
		Log.i(TAG, ".onEvent: " + EventMessenger.idName(msgId) + TextUtils.implodeBundle(bundle));
		if (FeaturePlayer.ON_PLAY_PAUSE == msgId)
		{
			if (_feature.Component.PLAYER.isPaused())
				onPause();
			else
				onResume();
		}
	}

	/**
	 * Pause cursor
	 */
	private void onPause()
	{
		Log.i(TAG, ".pause");
		_pauseTimeStart = currentTime();
		_autoResumer.start();
	}

	/**
	 * Resume from pause
	 */
	private void onResume()
	{
		Log.i(TAG, ".resume");
		_autoResumer.stop();
		setPlayTimeDelta(_playTimeDelta + currentTime() - _pauseTimeStart);
	}

	/**
	 * @return adjust the given timestamp in current timeshift frame limits
	 */
	private long adjustInTimeshift(long timestamp)
	{
		timestamp = Math.min(timestamp, currentTime());
		timestamp = Math.max(timestamp, currentTime() - getTimeshiftDuration());
		return timestamp;
	}

	/**
	 * call to change playTimeDelta which will trigger notification event to
	 * tell the client to seek in stream
	 *
	 * @param playTimeDelta
	 */
	private void setPlayTimeDelta(long playTimeDelta)
	{
		if (_playTimeDelta != playTimeDelta)
		{
			_playTimeDelta = playTimeDelta;
			Bundle bundle = new Bundle();
			bundle.putLong(EXTRA_PLAY_TIME_DELTA, playTimeDelta);
			getEventMessenger().trigger(ON_SEEK, bundle);
		}
	}

	private class AutoResumer implements Runnable
	{
		@Override
		public void run()
		{
			long delta = currentTime() - getPlayingTime();
			Log.i(TAG, "Auto resum check: delta = " + delta + ", timeshift duration = " + getTimeshiftDuration());
			if (delta > getTimeshiftDuration())
			{
				Log.i(TAG, "Auto resuming");
				_feature.Component.PLAYER.resume();
				// seekAt(currentTime() - getTimeshiftDuration());
				getEventMessenger().trigger(ON_AUTO_RESUME);
			}

			getEventMessenger().postDelayed(this, 1000);
		}

		public void start()
		{
			Log.i(TAG, "AutoResumer.start");
			getEventMessenger().removeCallbacks(this);
			getEventMessenger().post(this);
		}

		public void stop()
		{
			Log.i(TAG, "AutoResumer.stop");
			getEventMessenger().removeCallbacks(this);
		}
	}
}

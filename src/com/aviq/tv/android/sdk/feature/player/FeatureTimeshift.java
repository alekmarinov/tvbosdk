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
import com.aviq.tv.android.sdk.core.ResultCode;
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
	private long _timeshiftTimeStart;
	private long _pauseTimeStart;
	private long _playTimeDelta;
	private FeaturePlayer _featurePlayer;
	private int _timeshiftMaxBufSize;
	private AutoResumer _autoResumer = new AutoResumer();

	public enum Param
	{
		/**
		 * Timeshift max buffer size in seconds
		 */
		TIMESHIFT_DURATION(60 * 5);

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.TIMESHIFT).put(name(), value);
		}
	}

	public FeatureTimeshift()
	{
		dependencies().Components.add(FeatureName.Component.PLAYER);
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		try
		{
			_featurePlayer = (FeaturePlayer) Environment.getInstance()
			        .getFeatureComponent(FeatureName.Component.PLAYER);
			_featurePlayer.getEventMessenger().register(this, FeaturePlayer.ON_PLAY_URL);
			_featurePlayer.getEventMessenger().register(this, FeaturePlayer.ON_PLAY_STOP);
			_featurePlayer.getEventMessenger().register(this, FeaturePlayer.ON_PLAY_PAUSE);
			_timeshiftMaxBufSize = getPrefs().getInt(Param.TIMESHIFT_DURATION);
			reset();
			super.initialize(onFeatureInitialized);
		}
		catch (FeatureNotFoundException e)
		{
			Log.e(TAG, e.getMessage(), e);
			onFeatureInitialized.onInitialized(this, ResultCode.GENERAL_FAILURE);
		}
	}

	/**
	 * Reset the timeshift buffer
	 */
	public void reset()
	{
		Log.i(TAG, ".reset");
		_timeshiftTimeStart = currentTime();
		_playTimeDelta = 0;
	}

	/**
	 * Seek at live position
	 */
	public void seekLive()
	{
		Log.i(TAG, ".seekLive");
		seekAt(currentTime());
	}

	/**
	 * Seek at playable buffer by absolute time
	 *
	 * @param timestamp
	 *            the time moment where to set the cursor position.
	 */
	public void seekAt(long timestamp)
	{
		Log.i(TAG, ".seekAt: " + (timestamp - currentTime()) + " secs relative to current time");
		_playTimeDelta = currentTime() - adjustInTimeshift(timestamp);
	}

	/**
	 * Seek at playable buffer by relative time
	 *
	 * @param secs
	 *            the number of secs to seek relatively.
	 */
	public void seekRel(long secs)
	{
		Log.i(TAG, ".seekRel: secs = " + secs);
		seekAt(getPlayingTime() + secs);
	}

	/**
	 * @return the current playing absolute time
	 */
	public long getPlayingTime()
	{
		if (_featurePlayer.isPaused())
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
		return Math.min(_timeshiftMaxBufSize, currentTime() - _timeshiftTimeStart);
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
		if (FeaturePlayer.ON_PLAY_URL == msgId || FeaturePlayer.ON_PLAY_STOPPING == msgId)
		{
			reset();
		}
		else if (FeaturePlayer.ON_PLAY_PAUSE == msgId)
		{
			if (_featurePlayer.isPaused())
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
		_playTimeDelta += currentTime() - _pauseTimeStart;
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

	private class AutoResumer implements Runnable
	{
		@Override
		public void run()
		{
			if (_playTimeDelta > getTimeshiftDuration())
			{
				Log.i(TAG, "Auto resuming");
				_featurePlayer.resume();
				seekAt(currentTime() - getTimeshiftDuration());
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

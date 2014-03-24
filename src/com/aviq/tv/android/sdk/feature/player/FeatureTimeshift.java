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
	private long _timeshiftTimeStart;
	private long _pauseTimeStart;
	private long _playTimeDelta;
	private boolean _isPause;
	private FeaturePlayer _FeaturePlayer;

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
			_FeaturePlayer = (FeaturePlayer) Environment.getInstance()
			        .getFeatureComponent(FeatureName.Component.PLAYER);
			_FeaturePlayer.getEventMessenger().register(this, FeaturePlayer.ON_PLAY_STARTED);
			_FeaturePlayer.getEventMessenger().register(this, FeaturePlayer.ON_PLAY_STOP);
			_FeaturePlayer.getEventMessenger().register(this, FeaturePlayer.ON_PLAY_PAUSE);
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
		_isPause = false;
	}

	/**
	 * Pause cursor
	 */
	public void pause()
	{
		Log.i(TAG, ".pause");
		_pauseTimeStart = currentTime();
		_isPause = true;
	}

	/**
	 * Resume from pause
	 */
	public void resume()
	{
		Log.i(TAG, ".resume");
		_isPause = false;
		_playTimeDelta += currentTime() - _pauseTimeStart;
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
		if (_isPause)
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
		return currentTime() - _timeshiftTimeStart;
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.TIMESHIFT;
	}

	@Override
	public void onEvent(int msgId, Bundle bundle)
	{
		Log.i(TAG, ".onEvent: " + EventMessenger.idName(msgId) + TextUtils.implodeBundle(bundle));
		if (FeaturePlayer.ON_PLAY_STARTED == msgId || FeaturePlayer.ON_PLAY_STOP == msgId)
			reset();
		else if (FeaturePlayer.ON_PLAY_PAUSE == msgId)
		{
			if (_FeaturePlayer.isPaused())
				pause();
			else
				resume();
		}
	}

	/**
	 * @return current time stamp in seconds
	 */
	public long currentTime()
	{
		return System.currentTimeMillis() / 1000;
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
}

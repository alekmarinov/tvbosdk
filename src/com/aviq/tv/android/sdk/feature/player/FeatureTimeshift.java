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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.os.Bundle;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.EventReceiver;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.feature.system.NetworkClient;
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
	private int _timeshiftMaxBufSize;
	private AutoResumer _autoResumer = new AutoResumer();
	private String _timeshiftUrl;
	private String _timeshiftStartUrl;
	private DateFormat _timeshiftTimeFormat;
	private NetworkClient _networkClient;
	private boolean _timeshiftAvailable = false;

	private Runnable _seeker = new Runnable()
	{
		@Override
		public void run()
		{
			String timeshiftTime = _timeshiftTimeFormat.format(new Date(1000 * getPlayingTime()));
			Bundle bundle = new Bundle();
			bundle.putString("TIME", timeshiftTime);
			String seekUrl = _timeshiftUrl + getPrefs().getString(Param.TIMESHIFT_SEEK_URL_PARAM, bundle);
			_feature.Component.PLAYER.play(seekUrl);
		}
	};

//	private Runnable _switcher = new Runnable()
//	{
//		@Override
//		public void run()
//		{
//			int status = ffmpeg_running();
//			Log.d(TAG, "waiting ffmpeg for status playing (" + FF_PLAYING + "): current is " + status);
//			if (status == FF_PLAYING)
//			{
//				_featurePlayer.play(_timeshiftStartUrl);
//			}
//			else
//			{
//				if (status != FF_IDLE)
//				{
//					getEventMessenger().postDelayed(this, 100);
//				}
//			}
//		}
//	};

	public enum Param
	{
		/**
		 * Timeshift max buffer size in seconds
		 */
		TIMESHIFT_DURATION(60 * 60),

		/**
		 * timeshift base url
		 */
		TIMESHIFT_URL("http://localhost:8090/live.ts"),

		/**
		 * Start timeshift param
		 */
		TIMESHIFT_START_URL_PARAM(""), // ?buffer=1

		/**
		 * Seek in timeshift param
		 */
		TIMESHIFT_SEEK_URL_PARAM("?date=${TIME}"),

		/**
		 * Timeshift time format
		 */
		// TIME_FORMAT("yyyy-MM-dd'T'HH:mm:ss")
		TIME_FORMAT("HH:mm:ss.000"),

		/**
		 * Timeshift server port number
		 */
		PORT(7070),

		/**
		 * Timeshift host address
		 */
		HOST("127.0.0.1");

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.TIMESHIFT).put(name(), value);
		}

		Param(String value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.TIMESHIFT).put(name(), value);
		}
	}

	public FeatureTimeshift()
	{
		dependencies().Components.add(FeatureName.Component.PLAYER);
		dependencies().Components.add(FeatureName.Component.SYSTEM);
	}

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");

		_feature.Component.PLAYER.getEventMessenger().register(this, FeaturePlayer.ON_PLAY_PAUSE);
		_timeshiftMaxBufSize = getPrefs().getInt(Param.TIMESHIFT_DURATION);

		_timeshiftUrl = getPrefs().getString(Param.TIMESHIFT_URL);
		_timeshiftStartUrl = _timeshiftUrl + getPrefs().getString(Param.TIMESHIFT_START_URL_PARAM);
		_timeshiftTimeFormat = new SimpleDateFormat(getPrefs().getString(Param.TIME_FORMAT), Locale.getDefault());

		reset();

		final String host = getPrefs().getString(Param.HOST);
		final int port = getPrefs().getInt(Param.PORT);
		_networkClient = new NetworkClient(host, port, new NetworkClient.OnNetworkEvent()
		{
			@Override
			public void onDisconnected()
			{
			}

			@Override
			public void onDataReceived(String data)
			{
			}

			@Override
			public void onConnected(boolean success)
			{
				if (success)
				{
					Log.i(TAG, ".onConnected: " + host + ":" + port);
				}
				else
				{
					Log.e(TAG, ".onConnected: Unable to connect to nethogs service on " + host + ":" + port);
				}
				_timeshiftAvailable = success;
				FeatureTimeshift.super.initialize(onFeatureInitialized);
			}
		});

		_feature.Component.SYSTEM.command("stop timeshifter");
		_feature.Component.SYSTEM.command("start timeshifter");

		getEventMessenger().postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				_networkClient.connect();
			}
		}, 1000);
	}

	/**
	 * Play url with timeshift buffer
	 *
	 * @param streamUrl
	 */
//	public void playJni(final String streamUrl)
//	{
//		Log.i(TAG, ".play: " + streamUrl);
//		reset();
//
//		if (!isTimeshiftAvailable())
//		{
//			_featurePlayer.play(streamUrl);
//		}
//		else
//		{
//			if (_featurePlayer.isPlaying())
//				_featurePlayer.stop();
//			getEventMessenger().postDelayed(new Runnable()
//			{
//				@Override
//				public void run()
//				{
//					new Thread(new Runnable()
//					{
//						@Override
//						public void run()
//						{
//							Log.i(TAG, ".play: " + streamUrl);
//							int exitCode = ffmpeg_start(streamUrl);
//							Log.i(TAG, "ffmpeg_start exited with code " + exitCode);
//						}
//					}).start();
//
//					Log.i(TAG, ".play: prepare timeshift");
//					getEventMessenger().removeCallbacks(_switcher);
//					getEventMessenger().post(_switcher);
//				}
//			}, 1000);
//		}
//	}

	/**
	 * Play url with timeshift buffer
	 *
	 * @param streamUrl
	 */
	public void play(final String streamUrl)
	{
		Log.i(TAG, ".play: " + streamUrl);
		reset();

		_networkClient.command(streamUrl);
		getEventMessenger().postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				_feature.Component.PLAYER.play(_timeshiftStartUrl);
			}
		}, 1500);
	}

	public boolean isTimeshiftAvailable()
	{
		Log.d(TAG, ".isTimeshiftAvailable: " + _timeshiftAvailable);
		return _timeshiftAvailable;
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
		long adjustedTime = adjustInTimeshift(timestamp);
		getEventMessenger().removeCallbacks(_seeker);
		getEventMessenger().postDelayed(_seeker, 1000);
		_playTimeDelta = currentTime() - adjustedTime;
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

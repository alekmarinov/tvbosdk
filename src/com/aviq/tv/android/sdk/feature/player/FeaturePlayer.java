/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeaturePlayer.java
 * Author:      alek
 * Date:        1 Dec 2013
 * Description: Component feature providing player
 */

package com.aviq.tv.android.sdk.feature.player;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.MediaController;
import android.widget.VideoView;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.EventReceiver;
import com.aviq.tv.android.sdk.core.Key;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.player.AndroidPlayer;
import com.aviq.tv.android.sdk.player.BasePlayer;
import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Component feature providing player
 */
public class FeaturePlayer extends FeatureComponent implements EventReceiver, AndroidPlayer.OnPlayerStatusListener
{
	public static final String TAG = FeaturePlayer.class.getSimpleName();
	public static final int ON_PLAY_URL = EventMessenger.ID("ON_PLAY_URL");
	public static final int ON_PLAY_STOP = EventMessenger.ID("ON_PLAY_STOP");
	public static final int ON_PLAY_STOPPING = EventMessenger.ID("ON_PLAY_STOPPING");
	public static final int ON_PLAY_PAUSE = EventMessenger.ID("ON_PLAY_PAUSE");
	public static final int ON_PLAY_PAUSING = EventMessenger.ID("ON_PLAY_PAUSING");
	public static final int ON_PLAY_RESUMING = EventMessenger.ID("ON_PLAY_RESUMING");
	public static final int ON_PLAY_STARTED = EventMessenger.ID("ON_PLAY_STARTED");
	public static final int ON_PLAY_TIMEOUT = EventMessenger.ID("ON_PLAY_TIMEOUT");
	public static final int ON_PLAY_ERROR = EventMessenger.ID("ON_PLAY_ERROR");
	public static final String EXTRA_TIME_ELAPSED = "TIME_ELAPSED";
	private PlayerStatusPoller _playerStartPoller = new PlayerStatusPoller(new PlayerStartVerifier());
	private PlayerStatusPoller _playerStopPoller = new PlayerStatusPoller(new PlayerStopVerifier());
	private PlayerStatusPoller _playerPausePoller = new PlayerStatusPoller(new PlayerPauseVerifier());
	private PlayerStatusPoller _playerResumePoller = new PlayerStatusPoller(new PlayerResumeVerifier());
	private boolean _isError;
	private int _errWhat;
	private int _errExtra;

	public enum Extras
	{
		URL
	}

	public enum Param
	{
		/**
		 * Start playing timeout in seconds
		 */
		PLAY_TIMEOUT(30),

		/**
		 * Pause timeout in seconds
		 */
		PLAY_PAUSE_TIMEOUT(2000);

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.PLAYER).put(name(), value);
		}
	}

	public enum UserParam
	{
		/**
		 * Keep the last played URL
		 */
		LAST_URL
	}

	protected BasePlayer _player;
	private Prefs _userPrefs;

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		_userPrefs = Environment.getInstance().getUserPrefs();
		Environment.getInstance().getEventMessenger().register(this, Environment.ON_KEY_PRESSED);

		_player = createPlayer();

		super.initialize(onFeatureInitialized);
	}

	/**
	 * Creates backend player instance. Override this method to create custom
	 * player implementation.
	 *
	 * @return BasePlayer
	 */
	protected BasePlayer createPlayer()
	{
		VideoView videoView = new VideoView(Environment.getInstance());
		Environment.getInstance().getStateManager().addViewLayer(videoView, true);
		return new AndroidPlayer(videoView, this);
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.PLAYER;
	}

	public void play(String url)
	{
		Log.i(TAG, ".play: url = " + url);
		_isError = false;

		_userPrefs.put(UserParam.LAST_URL, url);
		_player.play(url);

		// trigger event on new url
		Bundle bundle = new Bundle();
		bundle.putString(Extras.URL.name(), url);
		getEventMessenger().trigger(ON_PLAY_URL, bundle);

		// start polling for playing status
		_playerStartPoller.start();
	}

	public void stop()
	{
		Log.i(TAG, ".stop");
		getEventMessenger().trigger(ON_PLAY_STOPPING);
		_player.stop();
		// start polling for stopped status
		_playerStopPoller.start();
	}

	public void pause()
	{
		Log.i(TAG, ".pause");
		getEventMessenger().trigger(ON_PLAY_PAUSING);
		_player.pause();
		// start polling for paused status
		_playerPausePoller.start();
	}

	public void resume()
	{
		Log.i(TAG, ".resume");
		getEventMessenger().trigger(ON_PLAY_RESUMING);
		_player.resume();
		// start polling for resumed status
		_playerResumePoller.start();
	}

	/**
	 * @return true if player is in playing state
	 */
	public boolean isPlaying()
	{
		boolean playing = _player.isPlaying();
		Log.i(TAG, ".isPlaying -> " + playing);
		return playing;
	}

	/**
	 * @return true if player is in pause state
	 */
	public boolean isPaused()
	{
		boolean paused = _player.isPaused();
		Log.i(TAG, ".isPaused -> " + paused);
		return paused;
	}

	/**
	 * @return true if error has occurred from last played media
	 */
	public boolean isError()
	{
		return _isError;
	}

	/**
	 * @return the error what parameter from last played media
	 */
	public int getErrorWhat()
	{
		return _errWhat;
	}

	/**
	 * @return the error extra parameter from last played media
	 */
	public int getErrorExtra()
	{
		return _errExtra;
	}

	/**
	 * This can be either a VideoView or a SurfaceView.
	 *
	 * @return SurfaceView
	 */
	public SurfaceView getView()
	{
		return _player.getView();
	}

	public void hide()
	{
		_player.hide();
	}

	public void setPositionAndSize(int x, int y, int w, int h)
	{
		_player.setPositionAndSize(x, y, w, h);
	}

	public void setFullScreen()
	{
		_player.setFullScreen();
	}

	public MediaController createMediaController(boolean useFastForward)
	{
		return _player.createMediaController(useFastForward);
	}

	public void removeMediaController()
	{
		_player.removeMediaController();
	}

	private interface PlayerStatusVerifier
	{
		/**
		 * returns true if the player reached the expected status
		 */
		boolean checkStatus(long timeElapsed);

		/**
		 * returns the timeout for waiting of player status
		 */
		int getTimeout();

		/**
		 * invoked on timeout waiting for player status
		 */
		void onTimeout(long timeElapsed);
	}

	private class PlayerStatusPoller implements Runnable
	{
		private PlayerStatusVerifier _playerStatusVerifier;
		protected long _startPolling;

		PlayerStatusPoller(PlayerStatusVerifier playerStatusVerifier)
		{
			_playerStatusVerifier = playerStatusVerifier;
		}

		/**
		 * start polling player status
		 */
		public void start()
		{
			_startPolling = System.currentTimeMillis();
			getEventMessenger().removeCallbacks(this);
			getEventMessenger().post(this);
		}

		@Override
		public void run()
		{
			// Call only once to prevent multiple triggering of events
			long timeElapsed = System.currentTimeMillis() - _startPolling;
			boolean isPlaying = _playerStatusVerifier.checkStatus(timeElapsed);
			Log.v(TAG, "waiting player for status: " + _playerStatusVerifier + " -> " + isPlaying);

			if (!isPlaying)
			{
				if (System.currentTimeMillis() - _startPolling > _playerStatusVerifier.getTimeout())
				{
					// on timeout
					_playerStatusVerifier.onTimeout(timeElapsed);
				}
				else
				{
					// continue polling
					getEventMessenger().postDelayed(this, 100);
				}
			}
		}
	}

	private class PlayerStartVerifier implements PlayerStatusVerifier
	{
		@Override
		public boolean checkStatus(long timeElapsed)
		{
			if (_player.getPosition() > 0)
			{
				// trigger player started
				Bundle bundle = new Bundle();
				bundle.putLong(EXTRA_TIME_ELAPSED, timeElapsed);
				getEventMessenger().trigger(ON_PLAY_STARTED, bundle);
				return true;
			}
			return false;
		}

		@Override
		public int getTimeout()
		{
			return 1000 * getPrefs().getInt(Param.PLAY_TIMEOUT);
		}

		@Override
		public void onTimeout(long timeElapsed)
		{
			// trigger timeout
			Bundle bundle = new Bundle();
			bundle.putLong(EXTRA_TIME_ELAPSED, timeElapsed);
			getEventMessenger().trigger(ON_PLAY_TIMEOUT, bundle);
		}

		@Override
		public String toString()
		{
			return "playing";
		}
	};

	private class PlayerStopVerifier implements PlayerStatusVerifier
	{
		@Override
		public boolean checkStatus(long timeElapsed)
		{
			if (!_player.isPlaying())
			{
				// trigger player stopped
				Bundle bundle = new Bundle();
				bundle.putLong(EXTRA_TIME_ELAPSED, timeElapsed);
				getEventMessenger().trigger(ON_PLAY_STOP, bundle);
				return true;
			}
			return false;
		}

		@Override
		public int getTimeout()
		{
			return getPrefs().getInt(Param.PLAY_PAUSE_TIMEOUT);
		}

		@Override
		public void onTimeout(long elapsed)
		{
			Log.w(TAG, "PlayerStopVerifier.onTimeout");
		}

		@Override
		public String toString()
		{
			return "stopped";
		}
	};

	private class PlayerPauseVerifier implements PlayerStatusVerifier
	{
		@Override
		public boolean checkStatus(long timeElapsed)
		{
			if (_player.isPaused())
			{
				// trigger player paused
				Bundle bundle = new Bundle();
				bundle.putLong(EXTRA_TIME_ELAPSED, timeElapsed);
				getEventMessenger().trigger(ON_PLAY_PAUSE, bundle);
				return true;
			}
			return false;
		}

		@Override
		public int getTimeout()
		{
			return 1000 * getPrefs().getInt(Param.PLAY_TIMEOUT);
		}

		@Override
		public void onTimeout(long timeElapsed)
		{
			Log.w(TAG, "PlayerPauseVerifier.onTimeout: timeElapsed = " + timeElapsed);
		}

		@Override
		public String toString()
		{
			return "paused";
		}
	};

	private class PlayerResumeVerifier implements PlayerStatusVerifier
	{
		@Override
		public boolean checkStatus(long timeElapsed)
		{
			if (!_player.isPaused())
			{
				// trigger player resume
				Bundle bundle = new Bundle();
				bundle.putLong(EXTRA_TIME_ELAPSED, timeElapsed);
				getEventMessenger().trigger(ON_PLAY_PAUSE, bundle);
				return true;
			}
			return false;
		}

		@Override
		public int getTimeout()
		{
			return getPrefs().getInt(Param.PLAY_PAUSE_TIMEOUT);
		}

		@Override
		public void onTimeout(long timeElapsed)
		{
			Log.w(TAG, "PlayerResumeVerifier.onTimeout: timeElapsed = " + timeElapsed);
		}

		@Override
		public String toString()
		{
			return "resumed";
		}
	}

	@Override
	public void onEvent(int msgId, Bundle bundle)
	{
		Log.i(TAG, ".onEvent: " + EventMessenger.idName(msgId) + TextUtils.implodeBundle(bundle));
		if (Environment.ON_KEY_PRESSED == msgId)
		{
			Key key = Key.valueOf(bundle.getString(Environment.EXTRA_KEY));
			if (Key.PLAY_PAUSE.equals(key))
			{
				if (isPaused())
					resume();
				else
					pause();
			}
		}
	}

	@Override
	public void onCompletion(AndroidPlayer player)
	{
		Log.i(TAG, ".onCompletion");
		getEventMessenger().trigger(ON_PLAY_STOP);
	}

	@Override
	public void onError(AndroidPlayer player, int what, int extra)
	{
		Log.w(TAG, ".onError: what = " + what + ", extra = " + extra);
		Bundle bundle = new Bundle();
		bundle.putInt("WHAT", what);
		bundle.putInt("EXTRA", extra);
		getEventMessenger().trigger(ON_PLAY_ERROR, bundle);
		_errWhat = what;
		_errExtra = extra;
		_isError = true;
	};
}

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

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.EventReceiver;
import com.aviq.tv.android.sdk.core.Key;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.player.BasePlayer;
import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Component feature providing player
 */
public class FeaturePlayer extends FeatureComponent implements EventReceiver
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
	private PlayerStatusPoller _playerStartPoller = new PlayerStatusPoller(new PlayerStartVerifier());
	private PlayerStatusPoller _playerStopPoller = new PlayerStatusPoller(new PlayerStopVerifier());
	private PlayerStatusPoller _playerPausePoller = new PlayerStatusPoller(new PlayerPauseVerifier());
	private PlayerStatusPoller _playerResumePoller = new PlayerStatusPoller(new PlayerResumeVerifier());

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
		PLAY_PAUSE_TIMEOUT(2);

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
		if (_player == null)
			throw new RuntimeException("Set AndroidPlayer first via method setPlayer");
		_userPrefs = Environment.getInstance().getUserPrefs();
		Environment.getInstance().getEventMessenger().register(this, Environment.ON_KEY_PRESSED);
		super.initialize(onFeatureInitialized);
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.PLAYER;
	}

	public void play(String url)
	{
		Log.i(TAG, ".play: url = " + url);
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

	public boolean isPlaying()
	{
		boolean playing = _player.isPlaying();
		Log.i(TAG, ".isPlaying -> " + playing);
		return playing;
	}

	public boolean isPaused()
	{
		boolean paused = _player.isPaused();
		Log.i(TAG, ".isPaused -> " + paused);
		return paused;
	}

	public void setPlayer(BasePlayer player)
	{
		Log.i(TAG, "Using player " + player.getClass().getName());
		_player = player;
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
		boolean isStatus();

		/**
		 * returns the timeout for waiting of player status
		 */
		int getTimeout();

		/**
		 * invoked on timeout waiting for player status
		 */
		void onTimeout();
	}

	private class PlayerStatusPoller implements Runnable
	{
		private PlayerStatusVerifier _playerStatusVerifier;
		private long _startPolling;

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
			boolean isPlaying = _playerStatusVerifier.isStatus();
			Log.v(TAG, "waiting player for status: " + _playerStatusVerifier + " -> " + isPlaying);

			if (!isPlaying)
			{
				if (System.currentTimeMillis() - _startPolling > _playerStatusVerifier.getTimeout())
				{
					// on timeout
					_playerStatusVerifier.onTimeout();
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
        public boolean isStatus()
        {
	        if (_player.getPosition() > 0)
	        {
				// trigger player started
				getEventMessenger().trigger(ON_PLAY_STARTED);
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
        public void onTimeout()
        {
			// trigger timeout
			getEventMessenger().trigger(ON_PLAY_TIMEOUT);
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
        public boolean isStatus()
        {
	        if (!_player.isPlaying())
	        {
				// trigger player stopped
				getEventMessenger().trigger(ON_PLAY_STOP);
				return true;
	        }
	        return false;
        }

		@Override
        public int getTimeout()
        {
			return 1000 * getPrefs().getInt(Param.PLAY_PAUSE_TIMEOUT);
        }

		@Override
        public void onTimeout()
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
        public boolean isStatus()
        {
	        if (_player.isPaused())
	        {
				// trigger player paused
				getEventMessenger().trigger(ON_PLAY_PAUSE);
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
        public void onTimeout()
        {
			Log.w(TAG, "PlayerPauseVerifier.onTimeout");
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
        public boolean isStatus()
        {
	        if (!_player.isPaused())
	        {
				// trigger player resume
				getEventMessenger().trigger(ON_PLAY_PAUSE);
				return true;
	        }
	        return false;
        }

		@Override
        public int getTimeout()
        {
			return 1000 * getPrefs().getInt(Param.PLAY_PAUSE_TIMEOUT);
        }

		@Override
        public void onTimeout()
        {
			Log.w(TAG, "PlayerResumeVerifier.onTimeout");
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
    };
}

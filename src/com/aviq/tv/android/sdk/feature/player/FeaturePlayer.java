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
import android.view.SurfaceView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.EventReceiver;
import com.aviq.tv.android.sdk.core.Key;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;
import com.aviq.tv.android.sdk.feature.rcu.FeatureRCU;
import com.aviq.tv.android.sdk.player.AndroidPlayer;
import com.aviq.tv.android.sdk.player.BasePlayer;
import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Component feature providing player
 */
@Author("alek")
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
	public static final int ON_PLAY_FREEZE = EventMessenger.ID("ON_PLAY_FREEZE");
	protected BasePlayer _player;
	protected VideoView _videoView;
	private PlayerStatusPoller _playerStartPoller = new PlayerStatusPoller(new PlayerStartVerifier());
	private PlayerStatusPoller _playerStopPoller = new PlayerStatusPoller(new PlayerStopVerifier());
	private PlayerStatusPoller _playerPausePoller = new PlayerStatusPoller(new PlayerPauseVerifier());
	private PlayerStatusPoller _playerResumePoller = new PlayerStatusPoller(new PlayerResumeVerifier());
	private PlayerStatusPoller _playerPlayingPoller = new PlayerStatusPoller(new PlayerPlayingVerifier());
	private boolean _isError;
	private int _errWhat;
	private int _errExtra;
	private long _playTimeElapsed;
	private boolean _playPauseEnabled;
	private boolean _isFullscreen;
	private MediaType _mediaType;
	private long _playFreezeTimeout;

	public enum Extras
	{
		URL, TIME_ELAPSED, WHAT, EXTRA
	}

	public enum MediaType
	{
		TV, VIDEO // VOD, WebTV, YouTube, etc.
	}

	public static enum Param
	{
		/**
		 * Start playing timeout in seconds
		 */
		PLAY_TIMEOUT(30),

		/**
		 * Pause timeout in seconds
		 */
		PLAY_PAUSE_TIMEOUT(2000),

		/**
		 * True when the player will be able to switch between play and pause
		 * features. False otherwise.
		 */
		PLAY_PAUSE_ENABLED(true),

		/**
		 * Duration in not playing status considering freezed playback
		 */
		PLAY_FREEZE_TIMEOUT(5000);

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.PLAYER).put(name(), value);
		}

		Param(boolean value)
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

	public FeaturePlayer() throws FeatureNotFoundException
	{
		require(FeatureName.Component.RCU);
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		_feature.Component.RCU.getEventMessenger().register(this, FeatureRCU.ON_KEY_PRESSED);
		_player = createPlayer();

		_playPauseEnabled = getPrefs().has(Param.PLAY_PAUSE_ENABLED) ? getPrefs().getBool(Param.PLAY_PAUSE_ENABLED)
		        : true;
		_playFreezeTimeout = getPrefs().getInt(Param.PLAY_FREEZE_TIMEOUT);

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
		_videoView = new VideoView(Environment.getInstance());
		Environment.getInstance().getStateManager().addViewLayer(_videoView, true);
		useVideoView(_videoView);
		return _player;
	}

	/**
	 * Creates backend player instance with attached VideoView. Override this
	 * method to create custom
	 * player implementation.
	 */
	public void useVideoView(VideoView videoView)
	{
		_player = new AndroidPlayer(videoView, this);
	}

	/**
	 * Restores initial video view
	 */
	public void restoreVideoView()
	{
		useVideoView(_videoView);
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.PLAYER;
	}

	public void play(String url, MediaType mediaType)
	{
		Log.i(TAG, ".play: url = " + url + ", mediaType = " + mediaType);
		if (_player == null)
			return;
		_mediaType = mediaType;
		_isError = false;
		_playTimeElapsed = System.currentTimeMillis();

		Environment.getInstance().getUserPrefs().put(UserParam.LAST_URL, url);
		_player.play(url);

		if (isPaused())
			resume();

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
		if (_player == null)
			return;
		// set undetermined media type
		_mediaType = null;
		getEventMessenger().trigger(ON_PLAY_STOPPING);
		_player.stop();
		// start polling for stopped status
		_playerStopPoller.start();
	}

	public void pause()
	{
		Log.i(TAG, ".pause");
		if (_player == null)
			return;
		getEventMessenger().trigger(ON_PLAY_PAUSING);
		_player.pause();
		// start polling for paused status
		_playerPausePoller.start();
	}

	public void resume()
	{
		Log.i(TAG, ".resume");
		if (_player == null)
			return;
		getEventMessenger().trigger(ON_PLAY_RESUMING);
		_player.resume();
		// start polling for resumed status
		_playerResumePoller.start();
	}

	/**
	 * @return the media type of last played url
	 */
	public MediaType getMediaType()
	{
		return _mediaType;
	}

	/**
	 * @return true if player is in playing state
	 */
	public boolean isPlaying()
	{
		if (_player == null)
			return false;
		boolean playing = _player.isPlaying() || _player.isPaused();
		Log.i(TAG, ".isPlaying -> " + playing);
		return playing;
	}

	/**
	 * @return true if player is in pause state
	 */
	public boolean isPaused()
	{
		if (_player == null)
			return false;
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
	 * @return Returns playback position in milliseconds
	 */
	public int getPosition()
	{
		if (_player == null)
			return 0;

		return _player.getPosition();
	}

	/**
	 * @return Returns media duration in milliseconds
	 */
	public int getDuration()
	{
		if (_player == null)
			return 0;
		return _player.getDuration();
	}

	/**
	 * This can be either a VideoView or a SurfaceView.
	 *
	 * @return SurfaceView
	 */
	public SurfaceView getView()
	{
		if (_player == null)
			return null;
		return _player.getView();
	}

	public void hide()
	{
		if (_player == null)
			return ;
		_player.hide();
	}

	public void setPositionAndSize(int x, int y, int w, int h)
	{
		if (_player == null)
			return ;
		_player.setPositionAndSize(x, y, w, h);

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(w, h);
		params.leftMargin = x;
		params.topMargin = y;
		_videoView.setLayoutParams(params);
		_isFullscreen = false;
	}

	/**
	 * Seeks in stream position at the specified offset in milliseconds
	 */
	public void seekTo(int offset)
	{
		if (_player == null)
			return ;
		_player.seekTo(offset);
	}

	public void setFullScreen()
	{
		if (_player == null)
			return ;
		_player.setFullScreen();

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
		        RelativeLayout.LayoutParams.MATCH_PARENT);
		params.leftMargin = 0;
		params.rightMargin = 0;
		_videoView.setLayoutParams(params);
		_isFullscreen = true;
	}

	public boolean isFullScreen()
	{
		return _isFullscreen;
	}

	public MediaController createMediaController(boolean useFastForward)
	{
		if (_player == null)
			return null;
		return _player.createMediaController(useFastForward);
	}

	public void removeMediaController()
	{
		if (_player == null)
			return ;
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
			boolean isStatus = _playerStatusVerifier.checkStatus(timeElapsed);
			Log.v(TAG, "waiting player for status: " + _playerStatusVerifier + " -> " + isStatus);

			if (!isStatus)
			{
				if (_playerStatusVerifier.getTimeout() > 0
				        && System.currentTimeMillis() - _startPolling > _playerStatusVerifier.getTimeout())
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
				bundle.putLong(Extras.TIME_ELAPSED.name(), timeElapsed);
				getEventMessenger().trigger(ON_PLAY_STARTED, bundle);

				// start verifying of resistant playback
				_playerPlayingPoller.start();

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
			bundle.putLong(Extras.TIME_ELAPSED.name(), timeElapsed);
			getEventMessenger().trigger(ON_PLAY_TIMEOUT, bundle);
		}

		@Override
		public String toString()
		{
			return "playing";
		}
	};

	private class PlayerPlayingVerifier implements PlayerStatusVerifier
	{
		private boolean _hasPlayed;
		private long _timeSinceLastPlay;
		private long _positionSinceLastPlay;

		@Override
		public boolean checkStatus(long timeElapsed)
		{
			// eliminate player statuses not considered by this verifier
			if (!_player.isPlaying() || _player.isPaused() || _player.getPosition() == 0)
			{
				_timeSinceLastPlay = _positionSinceLastPlay = 0;
				_hasPlayed = false;
				Log.v(TAG, "PlayerPlayingVerifier.checkStatus: reset counting");
				return false;
			}

			if (_hasPlayed)
			{
				long elapsedTimeSinceLastPlay = System.currentTimeMillis() - _timeSinceLastPlay;
				Log.v(TAG, "PlayerPlayingVerifier.checkStatus: elapsedTimeSinceLastPlay = " + elapsedTimeSinceLastPlay
				        + ", _positionSinceLastPlay = " + _positionSinceLastPlay + ", _player.getPosition() = "
				        + _player.getPosition());
				if (elapsedTimeSinceLastPlay > _playFreezeTimeout)
				{
					if (_positionSinceLastPlay == _player.getPosition())
					{
						// trigger player freeze event
						getEventMessenger().trigger(ON_PLAY_FREEZE);
					}
					else
					{
						_timeSinceLastPlay = System.currentTimeMillis();
						_positionSinceLastPlay = _player.getPosition();
					}
				}
			}
			else
			{
				Log.i(TAG, "PlayerPlayingVerifier.checkStatus: start counting");
				_timeSinceLastPlay = System.currentTimeMillis();
				_positionSinceLastPlay = _player.getPosition();
			}
			_hasPlayed = true;

			return false;
		}

		@Override
		public int getTimeout()
		{
			return 0;
		}

		@Override
		public void onTimeout(long timeElapsed)
		{
		}

		@Override
		public String toString()
		{
			return "play freezed";
		}
	};

	private class PlayerStopVerifier implements PlayerStatusVerifier
	{
		@Override
		public boolean checkStatus(long timeElapsed)
		{
			if (!isPlaying())
			{
				// trigger player stopped
				Bundle bundle = new Bundle();
				bundle.putLong(Extras.TIME_ELAPSED.name(), timeElapsed);
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
				bundle.putLong(Extras.TIME_ELAPSED.name(), timeElapsed);
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
				bundle.putLong(Extras.TIME_ELAPSED.name(), timeElapsed);
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
		if (FeatureRCU.ON_KEY_PRESSED == msgId)
		{
			Key key = Key.valueOf(bundle.getString(Environment.EXTRA_KEY));
			if (Key.PLAY_PAUSE.equals(key) && _playPauseEnabled)
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
		bundle.putInt(Extras.WHAT.name(), what);
		bundle.putInt(Extras.EXTRA.name(), extra);
		bundle.putInt(Extras.TIME_ELAPSED.name(), (int) (System.currentTimeMillis() - _playTimeElapsed));
		bundle.putString(Extras.URL.name(), Environment.getInstance().getUserPrefs().getString(UserParam.LAST_URL));
		getEventMessenger().trigger(ON_PLAY_ERROR, bundle);
		_errWhat = what;
		_errExtra = extra;
		_isError = true;
	};
}

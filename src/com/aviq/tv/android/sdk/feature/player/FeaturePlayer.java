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
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.player.BasePlayer;
import com.aviq.tv.android.sdk.player.IPlayer;

/**
 * Component feature providing player
 */
public class FeaturePlayer extends FeatureComponent
{
	public static final String TAG = FeaturePlayer.class.getSimpleName();
	public static final int ON_PLAY_URL = EventMessenger.ID("ON_PLAY_URL");
	public static final int ON_PLAY_STOP = EventMessenger.ID("ON_PLAY_STOP");
	public static final int ON_PLAY_PAUSE = EventMessenger.ID("ON_PLAY_PAUSE");

	public static final int ON_PLAY_STARTED = EventMessenger.ID("ON_PLAY_STARTED");
	public static final int ON_PLAY_TIMEOUT = EventMessenger.ID("ON_PLAY_TIMEOUT");
	private VideoStartPoller _videoStartPoller = new VideoStartPoller();

	public enum Extras
	{
		URL
	}

	public enum Param
	{
		/**
		 * Start playing timeout in seconds
		 */
		TIMEOUT(20);

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

		// restart polling player status
		_videoStartPoller.stop();
		_videoStartPoller.poll();
	}

	public void stop()
	{
		Log.i(TAG, ".stop");
		_player.stop();
		getEventMessenger().trigger(ON_PLAY_STOP);
	}

	public void pause()
	{
		Log.i(TAG, ".pause");
		_player.pause();
		getEventMessenger().trigger(ON_PLAY_PAUSE);
	}

	public IPlayer getPlayer()
	{
		return _player;
	}

	public void setPlayer(BasePlayer player)
	{
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

	private class VideoStartPoller implements Runnable
	{
		private long _startPolling = System.currentTimeMillis();

		@Override
		public void run()
		{
			if (_player.getPosition() > 0)
			{
				// trigger play started
				getEventMessenger().trigger(ON_PLAY_STARTED);
			}
			else
			{
				long timeout = getPrefs().getInt(Param.TIMEOUT) * 1000;
				if (System.currentTimeMillis() - _startPolling > timeout)
				{
					// trigger timeout
					getEventMessenger().trigger(ON_PLAY_TIMEOUT);
				}
				else
				{
					// continue polling
					Log.v(TAG, "waiting player to start: position = " + _player.getPosition());
					poll();
				}
			}
		}

		/**
		 * poll to check player status in next moment
		 */
		public void poll()
		{
			getEventMessenger().postDelayed(this, 100);
		}

		/**
		 * forcing polling stop
		 */
		public void stop()
		{
			getEventMessenger().removeCallbacks(this);
		}
	};
}

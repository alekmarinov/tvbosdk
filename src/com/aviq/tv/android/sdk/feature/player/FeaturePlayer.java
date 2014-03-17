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

import android.util.Log;
import android.view.SurfaceView;
import android.widget.MediaController;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.ResultCode;
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
	public static final int ON_PLAY_STARTED = EventMessenger.ID("ON_PLAY_STARTED");
	public static final int ON_PLAY_TIMEOUT = EventMessenger.ID("ON_PLAY_TIMEOUT");

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
		onFeatureInitialized.onInitialized(this, ResultCode.OK);
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

		getEventMessenger().post(_videoStartedPoller);
		getEventMessenger().postDelayed(_videoStartTimeout, getPrefs().getInt(Param.TIMEOUT) * 1000);
	}

	public void stop()
	{
		Log.i(TAG, ".stop");
		_player.stop();
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

	private Runnable _videoStartedPoller = new Runnable()
	{
		@Override
		public void run()
		{
			if (_player.getPosition() > 0)
			{
				Log.i(TAG, "trigger ON_PLAY_STARTED (" + ON_PLAY_STARTED + ")");
				getEventMessenger().trigger(ON_PLAY_STARTED);
				getEventMessenger().removeCallbacks(_videoStartTimeout);
			}
			else
			{
				Log.v(TAG, "waiting player to start: position = " + _player.getPosition());
				getEventMessenger().postDelayed(this, 100);
			}
		}
	};

	private Runnable _videoStartTimeout = new Runnable()
	{
		@Override
		public void run()
		{
			Log.i(TAG, "trigger ON_PLAY_TIMEOUT (" + ON_PLAY_TIMEOUT + ")");
			getEventMessenger().trigger(ON_PLAY_TIMEOUT);
			getEventMessenger().removeCallbacks(_videoStartedPoller);
		}
	};
}

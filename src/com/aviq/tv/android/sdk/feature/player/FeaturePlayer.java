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
import android.widget.RelativeLayout;
import android.widget.VideoView;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.player.AndroidPlayer;
import com.aviq.tv.android.sdk.player.IPlayer;

/**
 * Component feature providing player
 */
public class FeaturePlayer extends FeatureComponent
{
	public static final String TAG = FeaturePlayer.class.getSimpleName();
	public static final int ON_PLAY_STARTED = EventMessenger.ID();
	public static final int ON_PLAY_TIMEOUT = EventMessenger.ID();

	public enum Param
	{
		/**
		 * Keep the last played URL
		 */
		LAST_URL
	}

	protected AndroidPlayer _player;
	private VideoView _videoView;
	private Prefs _userPrefs;

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		if (_videoView == null)
			throw new RuntimeException("Set VideoView first via method setVideoView");
		_userPrefs = Environment.getInstance().getUserPrefs();
		_player = new AndroidPlayer(_videoView);
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
		_userPrefs.put(Param.LAST_URL, url);
		_player.play(url);

		getEventMessenger().post(_videoStartedPoller);
		getEventMessenger().postDelayed(_videoStartTimeout, 10000);
	}

	public IPlayer getPlayer()
	{
		return _player;
	}

	public void setVideoView(VideoView videoView)
	{
		_videoView = videoView;
	}

	public VideoView getVideoView()
	{
		return _videoView;
	}

	public void hideVideoView()
	{
		Log.i(TAG, ".hideVideoView");
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(0, 0);
		params.leftMargin = 0;
		params.topMargin = 0;
		_videoView.setLayoutParams(params);
	}

	public void setVideoViewPositionAndSize(int x, int y, int w, int h)
	{
		Log.i(TAG, ".setVideoViewPositionAndSize: " + x + "," + y + " " + w + "x" + h);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(w, h);
		params.leftMargin = x;
		params.topMargin = y;
		_videoView.setLayoutParams(params);
	}

	public void setVideoViewFullScreen()
	{
		Log.i(TAG, ".setVideoViewFullScreen");
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
		        RelativeLayout.LayoutParams.MATCH_PARENT);
		params.leftMargin = 0;
		params.topMargin = 0;
		_videoView.setLayoutParams(params);
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
				Log.d(TAG, "waiting player to start: position = " + _player.getPosition());
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

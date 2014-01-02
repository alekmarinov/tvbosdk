/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    AndroidPlayer.java
 * Author:      alek
 * Date:        2 Sep 2013
 * Description: Player implementation based on Android VideoView
 */

package com.aviq.tv.android.sdk.player;

import android.net.Uri;
import android.widget.VideoView;

import com.aviq.tv.android.sdk.core.Log;

/**
 * Player implementation based on Android VideoView
 */
public class AndroidPlayer extends BasePlayer
{
	public static final String TAG = AndroidPlayer.class.getSimpleName();
	private final VideoView _videoView;

	/**
	 * AndroidPlayer constructor
	 *
	 * @param videoView
	 */
	public AndroidPlayer(VideoView videoView)
	{
		_videoView = videoView;
		Log.i(TAG, "constructed");
	}

	/**
	 * Starts playing URL
	 *
	 * @see com.aviq.tv.android.sdk.player.IPlayer#play(java.lang.String)
	 */
	@Override
    public void play(String url)
	{
		Log.i(TAG, ".play: url = " + url);
		super.play(url);
		_videoView.setVideoURI(Uri.parse(url));
		_videoView.start();
	}

	/**
	 * Stops playing
	 *
	 * @see com.aviq.tv.android.sdk.player.IPlayer#stop()
	 */
	@Override
    public void stop()
	{
		Log.i(TAG, ".stop");
		super.stop();
		_videoView.stopPlayback();
	}

	/**
	 * Pause/Resume media playback
	 *
	 * @see com.aviq.tv.android.sdk.player.IPlayer#pause()
	 */
	@Override
	public void pause()
	{
		Log.i(TAG, ".pause");
		super.pause();
		_videoView.pause();
	}

	@Override
	public boolean isPlaying()
	{
		return _videoView.isPlaying();
	}

	@Override
	public int getPosition()
	{
		return _videoView.getCurrentPosition();
	}
}

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

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.feature.player.AviqMediaController;

/**
 * Player implementation based on Android VideoView
 */
public class AndroidPlayer extends BasePlayer
{
	public static final String TAG = AndroidPlayer.class.getSimpleName();
	private final VideoView _videoView;
	private MediaController _mediaController;

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

	@Override
	public VideoView getView()
	{
		return _videoView;
	}

	@Override
	public void hide()
	{
		Log.i(TAG, ".hide");
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(0, 0);
		params.leftMargin = 0;
		params.topMargin = 0;
		_videoView.setLayoutParams(params);
	}

	@Override
	public void setPositionAndSize(int x, int y, int w, int h)
	{
		Log.i(TAG, ".setPositionAndSize: " + x + "," + y + " " + w + "x" + h);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(w, h);
		params.leftMargin = x;
		params.topMargin = y;
		_videoView.setLayoutParams(params);
	}

	@Override
	public void setFullScreen()
	{
		Log.i(TAG, ".setFullScreen");
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
		        RelativeLayout.LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.CENTER_HORIZONTAL);
		params.addRule(RelativeLayout.CENTER_VERTICAL);
		_videoView.setLayoutParams(params);
	}

	@Override
	public MediaController createMediaController(boolean useFastForward)
	{
		Context context = Environment.getInstance().getActivity();
		_mediaController = new AviqMediaController(context, useFastForward);
		_mediaController.setVisibility(View.VISIBLE);
		_mediaController.setAnchorView(_videoView);
		_mediaController.setMediaPlayer(_videoView);
		_videoView.setMediaController(_mediaController);

		_mediaController.setFocusable(false);

		return _mediaController;
	}

	@Override
    public void removeMediaController()
	{
		_mediaController.setVisibility(View.GONE);
		_videoView.setMediaController(null);
	}
}

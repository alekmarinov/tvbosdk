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
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.view.View;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.feature.player.AviqMediaController;

/**
 * Player implementation based on Android VideoView
 */
public class AndroidPlayer extends BasePlayer implements OnCompletionListener, OnErrorListener
{
	public static final String TAG = AndroidPlayer.class.getSimpleName();
	private final VideoView _videoView;
	private MediaController _mediaController;
	private OnPlayerStatusListener _onPlayerStatusListener;

	/**
	 * Callback interface for player completion or error
	 */
	public interface OnPlayerStatusListener
	{
		void onCompletion(AndroidPlayer player);

		void onError(AndroidPlayer player, int what, int extra);
	}

	/**
	 * AndroidPlayer constructor
	 *
	 * @param videoView
	 */
	public AndroidPlayer(VideoView videoView, OnPlayerStatusListener onPlayerStatusListener)
	{
		_videoView = videoView;
		_videoView.setOnCompletionListener(this);
		_videoView.setOnErrorListener(this);
		_onPlayerStatusListener = onPlayerStatusListener;
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
	 * Starts playing from a paused state
	 *
	 * @see com.aviq.tv.android.sdk.player.IPlayer#play()
	 */
	@Override
	public void play()
	{
		Log.i(TAG, ".play");
		super.play();
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
	 * Pause media playback
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

	/**
	 * Resume paused media playback
	 *
	 * @see com.aviq.tv.android.sdk.player.IPlayer#seekAt()
	 */
	@Override
	public void resume()
	{
		Log.i(TAG, ".resume");
		super.resume();
		_videoView.start();
	}

	/**
	 * Seeks in stream position at the specified offset in milliseconds
	 *
	 * @see com.aviq.tv.android.sdk.player.IPlayer#seekTo(int)
	 */
	@Override
	public void seekTo(int offset)
	{
		Log.i(TAG, ".seekTo: offset = " + offset);
		super.seekTo(offset);
		_videoView.seekTo(offset);
	}

	@Override
	public boolean isPlaying()
	{
		Log.i(TAG, "isPlaying -> " + _videoView.isPlaying());
		return _videoView.isPlaying();
	}

	@Override
	public boolean isPaused()
	{
		return super.isPaused();
	}

	/**
	 * @return playback position in milliseconds
	 */
	@Override
	public int getPosition()
	{
		return _videoView.getCurrentPosition();
	}

	/**
	 * @return media duration in milliseconds
	 */
	@Override
    public int getDuration()
    {
		return _videoView.getDuration();
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
		setPlayerLayoutParams(params);
	}

	@Override
	public void setFullScreen()
	{
		Log.i(TAG, ".setFullScreen");
//		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
//		        RelativeLayout.LayoutParams.WRAP_CONTENT);
//		params.addRule(RelativeLayout.CENTER_HORIZONTAL);
//		params.addRule(RelativeLayout.CENTER_VERTICAL);
//		setPlayerLayoutParams(params);
//
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
		        RelativeLayout.LayoutParams.MATCH_PARENT);
		params.leftMargin = 0;
		params.rightMargin = 0;
		_videoView.setLayoutParams(params);

	}

	public void setPlayerLayoutParams(RelativeLayout.LayoutParams params)
	{
		_videoView.setLayoutParams(params);
	}

	@Override
	public MediaController createMediaController(boolean useFastForward)
	{
		Context context = _videoView.getContext();
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

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra)
	{
		_onPlayerStatusListener.onError(this, what, extra);
		return true;
	}

	@Override
	public void onCompletion(MediaPlayer mp)
	{
		_onPlayerStatusListener.onCompletion(this);
	}
}

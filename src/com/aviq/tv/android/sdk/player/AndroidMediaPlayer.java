/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    AndroidPlayer.java
 * Author:      zhelyazko
 * Date:        3 Feb 2014
 * Description: Player implementation based on Android MediaPlayer and SurfaceView
 */

package com.aviq.tv.android.sdk.player;

import java.io.IOException;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.os.Build;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.MediaController;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.RelativeLayout;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.feature.player.AviqMediaController;

/**
 * Player implementation based on Android MediaPlayer and SurfaceView
 */
public class AndroidMediaPlayer extends BasePlayer implements OnBufferingUpdateListener, OnCompletionListener,
        OnPreparedListener, OnVideoSizeChangedListener, SurfaceHolder.Callback, OnErrorListener, OnInfoListener,
        OnSeekCompleteListener, MediaPlayerControl
{
	public static final String TAG = AndroidMediaPlayer.class.getSimpleName();

	private final SurfaceView _surfaceView;
	private SurfaceHolder _surfaceHolder;
	private MediaPlayer _mediaPlayer;
	private MediaController _mediaController;
	private int _currentBufferPercentage;

	/**
	 * AndroidPlayer constructor
	 *
	 * @param surfaceView
	 */
	public AndroidMediaPlayer(SurfaceView surfaceView)
	{
		_surfaceView = surfaceView;
		_mediaPlayer = new MediaPlayer();

		_surfaceHolder = _surfaceView.getHolder();
		_surfaceHolder.addCallback(this);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
			_surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

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

		boolean error = false;
		int what = Integer.MIN_VALUE;
		int extra = Integer.MIN_VALUE;
		try
		{
			_mediaPlayer.reset();
			_mediaPlayer.setDataSource(url);
		}
		catch (IllegalArgumentException e)
		{
			error = true;
			what = extra = MediaPlayer.MEDIA_ERROR_UNKNOWN;
			Log.e(TAG, "Error: setDataSource() throws IllegalArgumentException.", e);
		}
		catch (IllegalStateException e)
		{
			error = true;
			what = extra = MediaPlayer.MEDIA_ERROR_UNKNOWN;
			Log.e(TAG, "Error: setDataSource() throws IllegalStateException.", e);
		}
		catch (IOException e)
		{
			error = true;
			what = extra = MediaPlayer.MEDIA_ERROR_IO;
			Log.e(TAG, "Error: setDataSource() throws IOException.", e);
		}
		finally
		{
			if (error)
			{
				onError(_mediaPlayer, what, extra);
				return;
			}
		}

		_mediaPlayer.setOnBufferingUpdateListener(this);
		_mediaPlayer.setOnCompletionListener(this);
		_mediaPlayer.setOnPreparedListener(this);
		_mediaPlayer.setOnVideoSizeChangedListener(this);
		_mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		_mediaPlayer.setOnErrorListener(this);
		_mediaPlayer.setOnInfoListener(this);

       	_mediaPlayer.prepareAsync();
	}

	/**
	 * Starts playing from a paused state
	 *
	 * @see com.aviq.tv.android.sdk.player.IPlayer#play()
	 */
	@Override
	public void play()
	{
		onPrepared(_mediaPlayer);
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
		if (_mediaPlayer != null && _mediaPlayer.isPlaying())
			_mediaPlayer.stop();
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
		if (_mediaPlayer != null)
			_mediaPlayer.pause();
	}

	/**
	 * Resume paused media playback
	 *
	 * @see com.aviq.tv.android.sdk.player.IPlayer#resume()
	 */
	@Override
	public void resume()
	{
		Log.i(TAG, ".resume");
		super.resume();
		if (_mediaPlayer != null)
			_mediaPlayer.start();
	}

	@Override
	public boolean isPlaying()
	{
		return _mediaPlayer != null ? _mediaPlayer.isPlaying() : false;
	}

	@Override
	public int getPosition()
	{
		return _mediaPlayer != null ? _mediaPlayer.getCurrentPosition() : 0;
	}

	@Override
	public SurfaceView getView()
	{
		return _surfaceView;
	}

	@Override
	public void hide()
	{
		Log.i(TAG, ".hide");
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(0, 0);
		params.leftMargin = 0;
		params.topMargin = 0;
		_surfaceView.setLayoutParams(params);
	}

	@Override
	public void setPositionAndSize(int x, int y, int w, int h)
	{
		Log.i(TAG, ".setPositionAndSize: " + x + "," + y + " " + w + "x" + h);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(w, h);
		params.leftMargin = x;
		params.topMargin = y;
		_surfaceView.setLayoutParams(params);
	}

	@Override
	public void setFullScreen()
	{
		Log.i(TAG, ".setFullScreen");
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
		        RelativeLayout.LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.CENTER_HORIZONTAL);
		params.addRule(RelativeLayout.CENTER_VERTICAL);
		_surfaceView.setLayoutParams(params);
	}

	@Override
	public MediaController createMediaController(boolean useFastForward)
	{
		Context context = _surfaceView.getContext();
		_mediaController = new AviqMediaController(context, useFastForward);
		_mediaController.setVisibility(View.VISIBLE);
		_mediaController.setAnchorView(_surfaceView);
		_mediaController.setMediaPlayer(this);
		_mediaController.setFocusable(false);
		return _mediaController;
	}

	@Override
	public void removeMediaController()
	{
		_mediaController.setVisibility(View.GONE);
		_mediaController = null;
	}

	@Override
    public void onSeekComplete(MediaPlayer mp)
    {
		Environment env = (Environment)_surfaceView.getContext();
		env.getEventMessenger().trigger(ON_SEEK_COMPLETED);
    }

	@Override
    public boolean onInfo(MediaPlayer mp, int what, int extra)
    {
		Bundle bundle = new Bundle();
		bundle.putInt(PARAM_WHAT, what);
		bundle.putInt(PARAM_EXTRA, extra);
		Environment env = (Environment)_surfaceView.getContext();
		env.getEventMessenger().trigger(ON_INFO, bundle);

		if (getOnInfoListener() != null)
			getOnInfoListener().onInfo(mp, what, extra);

	    return false;
    }

	@Override
    public boolean onError(MediaPlayer mp, int what, int extra)
    {
		if (_mediaController != null)
		{
            _mediaController.hide();
        }

		_mediaPlayer.reset();

		String error;
		if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN && extra == 404)
			error = "File not found (404): what = " + what + ", extra = " + extra;
		else if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN)
			error = "Error: what = " + what + ", extra = " + extra;
		else if (what == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK)
			error = "Video not suitable for streaming: what = " + what + ", extra = " + extra;
		else
			error = "Media player playback error: what = " + what + ", extra = " + extra;
		Log.e(TAG, error);

		Bundle bundle = new Bundle();
		bundle.putInt(PARAM_WHAT, what);
		bundle.putInt(PARAM_EXTRA, extra);
		bundle.putString(PARAM_ERROR, error);
		Environment env = (Environment)_surfaceView.getContext();
		env.getEventMessenger().trigger(ON_ERROR, bundle);

		if (getOnErrorListener() != null)
			getOnErrorListener().onError(mp, what, extra);

	    return true;
    }

	@Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
    }

	@Override
    public void surfaceCreated(SurfaceHolder holder)
    {
		final Surface surface = holder.getSurface();
		if (surface == null || !surface.isValid())
			return;

		if (_mediaPlayer != null)
		{
			_mediaPlayer.setDisplay(holder);
			_mediaPlayer.setScreenOnWhilePlaying(true);
		}
    }

	@Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
    }

	@Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height)
    {
    }

	@Override
    public void onPrepared(MediaPlayer mp)
    {
		_mediaPlayer.start();

		if (_mediaController != null)
		{
			_mediaController.setEnabled(true);
			_surfaceView.post(new Runnable()
			{
				@Override
				public void run()
				{
					_mediaController.show();
				}
			});
		}

		Environment env = (Environment)_surfaceView.getContext();
		env.getEventMessenger().trigger(ON_PREPARED);

		if (getOnPreparedListener() != null)
			getOnPreparedListener().onPrepared(mp);
    }

	@Override
    public void onCompletion(MediaPlayer mp)
    {
		if (_mediaController != null)
		{
            _mediaController.hide();
        }

		_mediaPlayer.stop();

		Environment env = (Environment)_surfaceView.getContext();
		env.getEventMessenger().trigger(ON_COMPLETION);

		if (getOnCompletionListener() != null)
			getOnCompletionListener().onCompletion(mp);
    }

	@Override
    public void onBufferingUpdate(MediaPlayer mp, int percent)
    {
		if (_mediaPlayer == null)
		{
			_currentBufferPercentage = percent;
		}

		if (getOnBufferingUpdateListener() != null)
			getOnBufferingUpdateListener().onBufferingUpdate(mp, percent);
    }

	@Override
    public boolean canPause()
    {
	    return true;
    }

	@Override
    public boolean canSeekBackward()
    {
	    return false;
    }

	@Override
    public boolean canSeekForward()
    {
	    return false;
    }

	@Override
    public int getBufferPercentage()
    {
	    if (_mediaPlayer != null)
	    {
	    	return _currentBufferPercentage;
	    }
	    return 0;
    }

	@Override
    public int getCurrentPosition()
    {
	    return _mediaPlayer.getCurrentPosition();
    }

	@Override
    public int getDuration()
    {
	    return _mediaPlayer != null ? _mediaPlayer.getDuration() : -1;
    }

	@Override
    public void seekTo(int pos)
    {
		_mediaPlayer.seekTo(pos);
    }

	@Override
    public void start()
    {
		_mediaPlayer.start();
    }

    public int getAudioSessionId()
    {
	    // TODO Auto-generated method stub
	    return 0;
    }
}

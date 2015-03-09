/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTV
 * Filename:    BasePlayer.java
 * Author:      alek
 * Date:        17 Jul 2013
 * Description: Abstract player class
 */

package com.aviq.tv.android.sdk.player;

import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.view.SurfaceView;
import android.widget.MediaController;

import com.aviq.tv.android.sdk.core.EventMessenger;

/**
 * Defines abstract player
 */
public abstract class BasePlayer implements IPlayer
{
	public static final int ON_ERROR = EventMessenger.ID("ON_ERROR");
	public static final int ON_INFO = EventMessenger.ID("ON_INFO");
	public static final int ON_SEEK_COMPLETED = EventMessenger.ID("ON_SEEK_COMPLETED");
	public static final int ON_PREPARED = EventMessenger.ID("ON_PREPARED");
	public static final int ON_COMPLETION = EventMessenger.ID("ON_COMPLETION");

	public static final String PARAM_WHAT = "what";
	public static final String PARAM_EXTRA = "extra";
	public static final String PARAM_ERROR = "error";

	protected OnErrorListener _onErrorListener;
	protected OnBufferingUpdateListener _onBufferingUpdateListener;
	protected OnPreparedListener _onPreparedListener;
	protected OnCompletionListener _onCompletionListener;
	protected OnInfoListener _onInfoListener;

	private boolean _isPause = false;

	public abstract SurfaceView getView();

	public abstract void hide();

	public abstract void setPositionAndSize(int x, int y, int w, int h);

	public abstract void setFullScreen();

	public abstract MediaController createMediaController(boolean useFastForward);

	public abstract void removeMediaController();

	/**
	 * Starts playing URL and triggers EventEnum.PLAY with param URL=url
	 *
	 * @see com.aviq.tv.android.sdk.player.IPlayer#play(java.lang.String)
	 */
	@Override
	public void play(String url)
	{
	}

	/**
	 * Starts playing from a paused state and triggers EventEnum.PLAY with param
	 * URL=url
	 *
	 * @see com.aviq.tv.android.sdk.player.IPlayer#play(java.lang.String)
	 */
	@Override
	public void play()
	{
	}

	/**
	 * Stops playing and triggers EventEnum.STOP
	 *
	 * @see com.aviq.tv.android.sdk.player.IPlayer#stop()
	 */
	@Override
	public void stop()
	{
	}

	/**
	 * Pause media playback
	 *
	 * @see com.aviq.tv.android.sdk.player.IPlayer#pause()
	 */
	@Override
	public void pause()
	{
		_isPause = true;
	}

	/**
	 * Resume paused media playback
	 *
	 * @see com.aviq.tv.android.sdk.player.IPlayer#resume()
	 */
	@Override
	public void resume()
	{
		_isPause = false;
	}

	/**
	 * Seeks in stream position at the specified offset in milliseconds
	 *
	 * @see com.aviq.tv.android.sdk.player.IPlayer#seekTo(int)
	 */
	@Override
	public void seekTo(int offset)
	{
	}

	/**
	 * Return player's playback status. This method must be implemented by
	 * player backend
	 *
	 * @see com.aviq.tv.android.sdk.player.IPlayer#isPlaying()
	 */
	@Override
	public abstract boolean isPlaying();

	/**
	 * @see com.aviq.tv.android.sdk.player.IPlayer#isPaused()
	 */
	@Override
	public boolean isPaused()
	{
		return _isPause;
	}

	public void setOnErrorListener(OnErrorListener errorListener)
	{
		_onErrorListener = errorListener;
	}

	public OnErrorListener getOnErrorListener()
	{
		return _onErrorListener;
	}

	public void setOnBufferingUpdateListener(OnBufferingUpdateListener bufferingUpdateListener)
	{
		_onBufferingUpdateListener = bufferingUpdateListener;
	}

	public OnBufferingUpdateListener getOnBufferingUpdateListener()
	{
		return _onBufferingUpdateListener;
	}

	public void setOnPreparedListener(OnPreparedListener preparedListener)
	{
		_onPreparedListener = preparedListener;
	}

	public OnPreparedListener getOnPreparedListener()
	{
		return _onPreparedListener;
	}

	public void setOnCompletionListener(OnCompletionListener completionListener)
	{
		_onCompletionListener = completionListener;
	}

	public OnCompletionListener getOnCompletionListener()
	{
		return _onCompletionListener;
	}

	public void setOnInfoListener(OnInfoListener infoListener)
	{
		_onInfoListener = infoListener;
	}

	public OnInfoListener getOnInfoListener()
	{
		return _onInfoListener;
	}
}

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

import android.view.SurfaceView;
import android.widget.MediaController;

import com.aviq.tv.android.sdk.core.EventMessenger;



/**
 * Defines abstract player
 *
 */
public abstract class BasePlayer implements IPlayer
{
	public static final int ON_ERROR = EventMessenger.ID();
	public static final int ON_INFO = EventMessenger.ID();
	public static final int ON_SEEK_COMPLETED = EventMessenger.ID();
	public static final int ON_PREPARED = EventMessenger.ID();
	public static final int ON_COMPLETION = EventMessenger.ID();

	public static final String PARAM_WHAT = "what";
	public static final String PARAM_EXTRA = "extra";

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
	 * Starts playing from a paused state and triggers EventEnum.PLAY with param URL=url
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
	 * Pause/Resume media playback
	 *
	 * @see com.aviq.tv.android.sdk.player.IPlayer#pause()
	 */
	@Override
	public void pause()
	{
		if (isPlaying())
		{
			_isPause = !_isPause;
		}
	}

	/**
	 * Return player's playback status. This method must be implemented by player backend
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
}

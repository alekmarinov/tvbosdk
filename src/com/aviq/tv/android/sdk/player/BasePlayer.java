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


/**
 * Defines abstract player
 *
 */
public abstract class BasePlayer implements IPlayer
{
	private boolean _isPause = false;

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

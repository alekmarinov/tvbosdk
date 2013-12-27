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

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;

/**
 * Defines abstract player
 *
 */
public abstract class BasePlayer implements IPlayer
{
	private final List<EventListener> _eventListeners = new ArrayList<EventListener>();
	private boolean _isPause = false;

	/**
	 * Starts playing URL and triggers EventEnum.PLAY with param URL=url
	 *
	 * @see com.aviq.tv.android.sdk.player.IPlayer#play(java.lang.String)
	 */
	@Override
	public void play(String url)
	{
		Bundle playBundle = new Bundle();
		playBundle.putString("URL", url);
		triggerEvent(EventEnum.PLAY, playBundle);
	}

	/**
	 * Stops playing and triggers EventEnum.STOP
	 *
	 * @see com.aviq.tv.android.sdk.player.IPlayer#stop()
	 */
	@Override
	public void stop()
	{
		if (isPlaying())
		{
			triggerEvent(EventEnum.STOP, null);
		}
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
			Bundle pauseParams = new Bundle();
			pauseParams.putBoolean(EventEnum.PAUSE.name(), _isPause);
			triggerEvent(EventEnum.PAUSE, pauseParams);
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

	/**
	 * @see com.aviq.tv.android.sdk.player.IPlayer#addEventListener(com.aviq.tv.android.sdk.player.IPlayer.EventListener)
	 */
	@Override
	public void addEventListener(EventListener eventListener)
	{
		_eventListeners.add(eventListener);
	}

	/**
	 * @see com.aviq.tv.android.sdk.player.IPlayer#removeEventListener(com.aviq.tv.android.sdk.player.IPlayer.EventListener)
	 */
	@Override
	public void removeEventListener(EventListener eventListener)
	{
		_eventListeners.remove(eventListener);
	}

	/**
	 * notify player event listeners
	 *
	 * @param eventEnum the player event enum
	 * @param params event specific params bundle
	 */
	private void triggerEvent(EventEnum eventEnum, Bundle params)
	{
		// TODO: Consider using a LocalBroadcastManager
		// (http://developer.android.com/reference/android/support/v4/content/LocalBroadcastManager.html)

		for (EventListener eventListener: _eventListeners)
		{
			eventListener.onEvent(this, eventEnum, params);
		}
	}
}

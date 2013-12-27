/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTV
 * Filename:    IPlayer.java
 * Author:      alek
 * Date:        17 Jul 2013
 * Description: Defines player interface
 */

package com.aviq.tv.android.sdk.player;

import android.os.Bundle;

/**
 * Player interface
 */
public interface IPlayer
{
	/**
	 * Enumerates all player events
	 */
	public enum EventEnum
	{
		START, PLAY, STOP, PAUSE, ERROR
	}

	/**
	 * Event listener interface responding to player events
	 */
	public interface EventListener
	{
		void onEvent(IPlayer player, EventEnum eventEnum, Bundle params);
	}

	/**
	 * Starts playing URL
	 */
	void play(String url);

	/**
	 * Stops playing
	 */
	void stop();

	/**
	 * Pause/Resume media playback
	 */
	void pause();

	/**
	 * Returns true if player is currently playing media
	 */
	boolean isPlaying();

	/**
	 * Returns true if player pauses playback
	 */
	boolean isPaused();

	/**
	 * Register event listener called on player event
	 * @param eventListener the event listener to be registered
	 */
	void addEventListener(EventListener eventListener);

	/**
	 * Unregister event listener registered with addEventListener
	 * @param eventListener the event listener to be unregistered
	 */
	void removeEventListener(EventListener eventListener);
}

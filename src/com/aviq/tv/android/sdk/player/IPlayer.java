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


/**
 * Player interface
 */
public interface IPlayer
{
	/**
	 * Starts playing URL
	 */
	void play(String url);

	/**
	 * Starts playing from a paused state
	 */
	void play();

	/**
	 * Stops playing
	 */
	void stop();

	/**
	 * Pause media playback
	 */
	void pause();

	/**
	 * Resume paused media playback
	 */
	void resume();

	/**
	 * Returns true if player is currently playing media
	 */
	boolean isPlaying();

	/**
	 * Returns true if player pauses playback
	 */
	boolean isPaused();

	/**
	 * Returns playback position
	 */
	int getPosition();
}

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
	 * Returns playback position
	 */
	int getPosition();
}

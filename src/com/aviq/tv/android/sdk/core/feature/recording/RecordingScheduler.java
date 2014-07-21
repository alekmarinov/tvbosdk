/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    RecordingScheduler.java
 * Author:      elmira
 * Date:        14 Jul 2014
 * Description: Holds data representing scheduled recording
 */
package com.aviq.tv.android.sdk.core.feature.recording;

/**
 * Holds data representing scheduled recording
 */
public class RecordingScheduler
{
	private String _channelID;
	private String _startTime;
	private int _duration;

	public RecordingScheduler(String channelID, String start, int duration)
	{
		_channelID = channelID;
		_startTime = start;
		_duration = duration;
	}

	public String getChannelID()
	{
		return _channelID;
	}

	public void setChannelID(String channelID)
	{
		_channelID = channelID;
	}

	public String getStartTime()
	{
		return _startTime;
	}

	public void setStartTime(String startTime)
	{
		_startTime = startTime;
	}

	public int getDuration()
	{
		return _duration;
	}

	public void setDuration(int duration)
	{
		_duration = duration;
	}
}

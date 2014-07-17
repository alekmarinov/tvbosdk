package com.aviq.tv.android.sdk.feature.epg;

public class RecordSched
{
	private String _channelID;
	private String _startTime;
	private int _duration;
	
	public RecordSched(String channelID, String start, int duration)
	{
		_channelID = channelID;
		_startTime = start;
		_duration = duration;
		
	}
	
	public String get_channelID()
	{
		return _channelID;
	}
	
	public void set_channelID(String _channelID)
	{
		this._channelID = _channelID;
	}
	
	public String get_startTime()
	{
		return _startTime;
	}
	
	public void set_startTime(String _startTime)
	{
		this._startTime = _startTime;
	}
	
	public int get_duration()
	{
		return _duration;
	}
	
	public void set_duration(int _duration)
	{
		this._duration = _duration;
	}
	
}

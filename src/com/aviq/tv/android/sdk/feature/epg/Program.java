/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    Program.java
 * Author:      alek
 * Date:        19 Dec 2013
 * Description: Program data holder class
 */
package com.aviq.tv.android.sdk.feature.epg;

import java.util.Calendar;

import org.json.JSONObject;

import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.feature.recording.FeatureRecordingScheduler;

/**
 * Program data holder class
 */
public abstract class Program implements Comparable<Program>
{
    private static final long serialVersionUID = 7712628341257180116L;
    private static final String TAG = Program.class.getSimpleName();
    
	// Bean properties

	// FIXME: Make id as long type in order to be used natively for recommendation without conversions
	private String _id;
	private Channel _channel;
	private String _title;
	private int _index;
	
	// Other internal properties
	private Calendar _startTime;
	private Calendar _stopTime;
	private boolean _isWatched;

	public static class MetaData
	{
		public int metaChannel;
		public int metaStart;
		public int metaStop;
		public int metaTitle;
	}

	/**
	 * No-arg constructor added for Kryo serialization. Do not use for anything else.
	 */
	public Program()
	{
	}

	public Program(String id, Channel channel)
	{
		_id = id;
		_channel = channel;
	}

	@Override
	public int compareTo(Program another)
	{
		return _id.compareTo(_id);
	}

	public String getId()
	{
		return _id;
	}

	public Channel getChannel()
	{
		return _channel;
	}

	public Calendar getStartTime()
	{
		return _startTime;
	}
	
	public boolean getIsWatched()
	{
		return _isWatched;
	}

	public void setStartTime(Calendar startTime)
	{
		_startTime = startTime;
	}
	
	
	public void setIsWatched()
	{
		_isWatched = true;
	}
	
	public void setIsWatched(boolean isWatched)
	{
		_isWatched = isWatched;
	}

	public Calendar getStopTime()
	{
		return _stopTime;
	}

	public void setStopTime(Calendar stopTime)
	{
		_stopTime = stopTime;
	}

	public String getTitle()
	{
		return _title;
	}

	public void setTitle(String title)
	{
		_title = title;
	}

	public int getIndex()
	{
		return _index;
	}

	public void setIndex(int index)
	{
		_index = index;
	}

	public void setChannel(Channel channel)
	{
		_channel = channel;
	}

	/** Return the program length in milliseconds */
	public long getLengthMillis()
	{
		return _stopTime.getTimeInMillis() - _startTime.getTimeInMillis();
	}

	/** Return the program length in minutes */
	public int getLengthMin()
	{
		return (int) (getLengthMillis() / 60000);
	}

	/**
	 * @return true if program detail attributes has been set
	 */
	public abstract boolean hasDetails();

	/**
	 * Set program detail attributes
	 *
	 * @param JSONObject with program detail attributes
	 */
	public abstract void setDetails(JSONObject details);

	/**
	 * @param programAttribute
	 *
	 * @return attribute string value
	 */
	public abstract String getDetailAttribute(ProgramAttribute programAttribute);

	/**
	 * Sets provider's specific program attributes
	 *
	 * @param metaData indexed meta data
	 * @param attributes String array with the essential data positioned according the meta data indices
	 */
    public abstract void setDetailAttributes(MetaData metaData, String[] attributes);
}

package com.aviq.tv.android.sdk.feature.epg;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import android.graphics.Bitmap;

import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.utils.Calendars;

public class EpgDataCompat implements IEpgDataProvider, Serializable
{
	private static final long serialVersionUID = -1450062885594378941L;

	private static final String TAG = EpgDataCompat.class.getSimpleName();

	private List<Channel> _channelList;
	private transient Bitmap[] _channelLogos;
	private Calendar _maxEpgStopTime;
	private Calendar _minEpgStartTime;

	/** key = start time; value = index in program list for a specific channel */
	private Map<String, NavigableMap<Calendar, Integer>> _channelToProgramNavigableMap = new HashMap<String, NavigableMap<Calendar, Integer>>();

	/** key = channel id; value = program list for the specific channel */
	private Map<String, List<Program>> _channelToProgramListMap = new LinkedHashMap<String, List<Program>>();

	/**
	 * No-arg constructor added for Kryo serialization. Do not use for anything
	 * else.
	 */
	public EpgDataCompat()
	{
	}

	public EpgDataCompat(List<Channel> newChannelList)
	{
		// Keep first value far in the past
		_maxEpgStopTime = Calendar.getInstance();
		_maxEpgStopTime.add(Calendar.YEAR, -1);

		// Keep first value far in the future
		_minEpgStartTime = Calendar.getInstance();
		_minEpgStartTime.add(Calendar.YEAR, 1);

		_channelList = newChannelList;
		_channelLogos = new Bitmap[ChannelLogoType.values().length * _channelList.size()];
	}

	public synchronized boolean setChannelLogo(int channelIndex, Bitmap newLogo)
	{
		return setChannelLogo(channelIndex, newLogo, ChannelLogoType.NORMAL);
	}

	public synchronized boolean setChannelLogo(int channelIndex, Bitmap newLogo, ChannelLogoType logoType)
	{
		// Added for Kryo's serialization since the Bitmap array is transient
		if (_channelLogos == null)
			_channelLogos = new Bitmap[ChannelLogoType.values().length * _channelList.size()];

		if (newLogo == null || channelIndex < 0 || channelIndex > _channelList.size())
		{
			Log.w(TAG, ".setChannelLogo: newLogo = " + newLogo + ", channelIndex = " + channelIndex + ", _channelList size = " + _channelList.size());
			return false;
		}

		int arrindex = logoType.ordinal() * _channelList.size() + channelIndex;
		Log.d(TAG, ".setChannelLogo: newLogo = " + newLogo + ", logoType = " + logoType + ", channelIndex = " + channelIndex + ", _channelList size = " + _channelList.size() + ", arrindex = " + arrindex);
		_channelLogos[arrindex] = newLogo;

		return true;
	}

	public synchronized boolean addProgramData(String channelId, NavigableMap<Calendar, Integer> newProgramNavigableMap,
	        List<Program> newProgramList)
	{
		Log.d(TAG, ".addProgramData: channelId = " + channelId + ", newProgramList:size = " + newProgramList.size());

		if (newProgramNavigableMap == null || newProgramNavigableMap.size() == 0)
			return false;

		if (newProgramList == null || newProgramList.size() == 0)
			return false;

		_channelToProgramListMap.put(channelId, newProgramList);
		_channelToProgramNavigableMap.put(channelId, newProgramNavigableMap);

		// Keep EPG program min start time
		Map.Entry<Calendar, Integer> firstEntry = newProgramNavigableMap.firstEntry();
		Program firstProgram = newProgramList.get(firstEntry.getValue());

		if (_minEpgStartTime.after(firstProgram.getStartTime()))
			_minEpgStartTime = firstProgram.getStartTime();

		// Keep EPG program max start time
		Map.Entry<Calendar, Integer> lastEntry = newProgramNavigableMap.lastEntry();
		Program lastProgram = newProgramList.get(lastEntry.getValue());

		if (_maxEpgStopTime.before(lastProgram.getStopTime()))
			_maxEpgStopTime = lastProgram.getStopTime();

		Log.d(TAG,
		        "_minEpgStartTime = " + Calendars.makeString(_minEpgStartTime) + ", _maxEpgStopTime = "
		                + Calendars.makeString(_maxEpgStopTime) + ", firstProgram:getStartTime = "
		                + Calendars.makeString(firstProgram.getStartTime()));

		return true;
	}

	/**
	 * @param index
	 *            Channel position in the list
	 * @return the Channel at location 'index' in the channel list
	 */
	@Override
	public Channel getChannel(int index)
	{
		return _channelList.get(index);
	}

	/**
	 * @param channel
	 *            id
	 * @return the Channel with specified id
	 */
	@Override
	public Channel getChannel(String channelId)
	{
		for (Channel channel : _channelList)
		{
			if (channelId.equals(channel.getChannelId()))
				return channel;
		}
		return null;
	}

	/**
	 * @return the number of all channels
	 */
	@Override
	public int getChannelCount()
	{
		return _channelList.size();
	}

	@Override
	public Bitmap getChannelLogoBitmap(int index)
	{
		return getChannelLogoBitmap(index, ChannelLogoType.NORMAL);
	}

	@Override
	public Bitmap getChannelLogoBitmap(int index, ChannelLogoType logoType)
	{
		int arrindex = logoType.ordinal() * _channelList.size() + index;
		Bitmap bmp = _channelLogos[arrindex];
		Log.i(TAG, ".getChannelLogoBitmap: index = " + index + ", arrindex = " + arrindex + ", logoType = " + logoType + " -> " + bmp);
		return bmp;
	}

	/**
	 * Return list with all EPG channels
	 */
	@Override
	public List<Channel> getChannels()
	{
		return _channelList;
	}

	@Override
	public List<Program> getProgramList(String channelId, Calendar startTime, Calendar endTime)
	{
		if (startTime.compareTo(endTime) > 0)
		{
			Log.w(TAG, ".getProgramList: startTime > endTime: " + Calendars.makeString(startTime) + " > " + Calendars.makeString(endTime)
			        + ", ignoring method call");
			return new ArrayList<Program>();
		}

		NavigableMap<Calendar, Integer> programMap = _channelToProgramNavigableMap.get(channelId);
		NavigableMap<Calendar, Integer> subMap = programMap != null ? programMap
		        .subMap(startTime, true, endTime, false) : null;
		if (subMap == null)
		{
			Log.w(TAG, ".getProgramList: no EPG data for period: startTime = " + Calendars.makeString(startTime) + ", endTime " + Calendars.makeString(endTime));
			return new ArrayList<Program>();
		}

		List<Program> list = new ArrayList<Program>(subMap.size());
		for (Map.Entry<Calendar, Integer> entry : subMap.entrySet())
		{
			int index = entry.getValue();
			list.add(_channelToProgramListMap.get(channelId).get(index));
		}
		return list;
	}

	@Override
	public List<Program> getProgramList(String channelId)
	{
		return _channelToProgramListMap.get(channelId);
	}

	private int getProgramIndex(String channelId, Calendar when)
	{
		NavigableMap<Calendar, Integer> programMap = _channelToProgramNavigableMap.get(channelId);
		if (programMap == null)
		{

			Log.w(TAG, "No program map for " + channelId);
			return -1;
		}

		Map.Entry<Calendar, Integer> programEntry = programMap.floorEntry(when);
		if (programEntry != null)
		{
			int programIndex = programEntry.getValue();
			return programIndex;
		}
		Log.w(TAG, "No program entry for " + channelId + " at " + Calendars.makeString(when));
		return -1;
	}

	private Program getProgramByIndex(String channelId, int programIndex)
	{
		List<Program> programsList = _channelToProgramListMap.get(channelId);
		if (programsList == null)
		{
			Log.w(TAG, "No program list for " + channelId);
			return null;
		}

		if (programIndex < 0 || programIndex >= programsList.size())
		{
			Log.w(TAG, "Program index " + programIndex + " exceeds range [0," + programsList.size() + ")");
			return null;
		}

		return programsList.get(programIndex);
	}

	@Override
	public Program getProgram(String channelId, Calendar when)
	{
		return getProgramByIndex(channelId, getProgramIndex(channelId, when));
	}

	@Override
	public Program getProgramByOffset(String channelId, Calendar when, int offset)
	{
		return getProgramByIndex(channelId, getProgramIndex(channelId, when) + offset);
	}

	@Override
	public Program getProgramById(String channelId, String programId)
	{
		List<Program> programsList = _channelToProgramListMap.get(channelId);
		if (programsList == null)
			return null;
		// FIXME: optimize this method
		for (Program program : programsList)
		{
			if (program.getId().equals(programId))
				return program;
		}
		return null;
	}

	@Override
	public Calendar getMaxEpgStopTime()
	{
		return _maxEpgStopTime;
	}

	@Override
	public Calendar getMinEpgStartTime()
	{
		return _minEpgStartTime;
	}
}

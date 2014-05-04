package com.aviq.tv.android.sdk.feature.epg;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import android.graphics.Bitmap;
import android.util.Log;

public class EpgData implements IEpgDataProvider
{
	private static final String TAG = EpgData.class.getSimpleName();

	private List<Channel> _channelList;
	private Bitmap[] _channelLogos;
	private Calendar _maxEpgStopTime;
	private Calendar _minEpgStartTime;

	/** key = start time; value = index in program list for a specific channel */
	private Map<String, NavigableMap<Calendar, Integer>> _channelToProgramNavigableMap = new HashMap<String, NavigableMap<Calendar, Integer>>();

	/** key = channel id; value = program list for the specific channel */
	private Map<String, List<Program>> _channelToProgramListMap = new LinkedHashMap<String, List<Program>>();

	public EpgData(List<Channel> newChannelList)
	{
		// Keep first value far in the past
		_maxEpgStopTime = Calendar.getInstance();
		_maxEpgStopTime.set(Calendar.YEAR, _maxEpgStopTime.getMinimum(Calendar.YEAR));

		// Keep first value far in the future
		_minEpgStartTime = Calendar.getInstance();
		_minEpgStartTime.set(Calendar.YEAR, _minEpgStartTime.getMaximum(Calendar.YEAR));

		_channelList = newChannelList;
		_channelLogos = new Bitmap[_channelList.size()];
	}

	synchronized boolean setChannelLogo(int channelIndex, Bitmap newLogo)
	{
		if (newLogo == null || channelIndex < 0 || channelIndex > _channelList.size())
			return false;

		_channelLogos[channelIndex] = newLogo;

		return true;
	}

	synchronized boolean addProgramData(String channelId, NavigableMap<Calendar, Integer> newProgramNavigableMap,
	        List<Program> newProgramList)
	{
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
		return _channelLogos[index];
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
			Log.w(TAG, ".getProgramList: startTime > endTime: " + startTime + " > " + endTime
			        + ", ignoring method call");
			return new ArrayList<Program>();
		}

		NavigableMap<Calendar, Integer> programMap = _channelToProgramNavigableMap.get(channelId);
		NavigableMap<Calendar, Integer> subMap = programMap != null ? programMap.subMap(startTime, true, endTime, false)
		        : null;
		if (subMap == null)
		{
			Log.w(TAG, ".getProgramList: no EPG data for period: startTime = " + startTime + ", endTime " + endTime);
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

	public int getProgramIndex(String channelId, Calendar when)
	{
		NavigableMap<Calendar, Integer> programMap = _channelToProgramNavigableMap.get(channelId);
		if (programMap == null)
			return -1;

		Map.Entry<Calendar, Integer> programEntry = programMap.floorEntry(when);
		if (programEntry != null)
		{
			int programIndex = programEntry.getValue();
			return programIndex;
		}
		return -1;
	}

	public Program getProgramByIndex(String channelId, int programIndex)
	{
		List<Program> programsList = _channelToProgramListMap.get(channelId);
		if (programsList == null)
			return null;

		if (programIndex < 0 || programIndex >= programsList.size())
			return null;

		return programsList.get(programIndex);
	}

	@Override
	public Program getProgram(String channelId, Calendar when)
	{
		return getProgramByIndex(channelId, getProgramIndex(channelId, when));
	}

	@Override
	public Program getProgramById(String channelId, String programId)
	{
		List<Program> programsList = _channelToProgramListMap.get(channelId);
		if (programsList == null)
			return null;
		// FIXME: optimize this method
		for (Program program: programsList)
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

	public Calendar getMinEpgStartTime()
	{
		return _minEpgStartTime;
	}
}

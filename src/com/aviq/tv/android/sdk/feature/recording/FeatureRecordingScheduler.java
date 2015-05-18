/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureRecordingScheduler.java
 * Author:      elmira
 * Date:        14 Jul 2014
 * Description: Feature managing scheduled recordings
 */
package com.aviq.tv.android.sdk.feature.recording;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;

import android.annotation.SuppressLint;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.epg.Program;
import com.aviq.tv.android.sdk.utils.Calendars;

/**
 * Feature managing scheduled recordings
 */
@Author("elmira")
public class FeatureRecordingScheduler extends FeatureComponent
{
	private static final String TAG = FeatureRecordingScheduler.class.getSimpleName();
	private static final String RECORD_DELIMITER = ";";
	private static final String ITEM_DELIMITER = ",";
	private static final String PROGRAM_TIME_ID = "yyyyMMddHHmmss";

	/** key = chanelID; value = map between record endTime and schedule record */
	private Map<String, NavigableMap<Calendar, Program>> _channelToRecordsNavigableMap = null;

	private Set<Integer> _dayOffsets = new TreeSet<Integer>(Collections.reverseOrder());
	private Prefs _userPrefs;

	public static enum UserParam
	{
		/**
		 * Store all scheduled recordings
		 */
		RECORDINGS
	}

	private Comparator<Program> _recordingSchedulerComparator = new Comparator<Program>()
	{
		@Override
		public int compare(Program lhs, Program rhs)
		{
			return lhs.getStartTime().compareTo(rhs.getStartTime());
		}
	};

	@SuppressLint("SimpleDateFormat")
	public FeatureRecordingScheduler() throws FeatureNotFoundException
	{
		require(FeatureName.Component.TIMEZONE);
		require(FeatureName.Component.EPG);
	}

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		_userPrefs = Environment.getInstance().getUserPrefs();

		loadRecordFromDataProvider(new OnResultReceived()
		{
			@SuppressWarnings("unchecked")
			@Override
			public void onReceiveResult(FeatureError error, Object object)
			{
				if (!error.isError())
					_channelToRecordsNavigableMap = (Map<String, NavigableMap<Calendar, Program>>) object;
				onFeatureInitialized.onInitialized(error);
			}
		});
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.RECORDING_SCHEDULER;
	}

	/**
	 * Add new program recording
	 *
	 * @param program
	 *            for recording
	 * @return true if record is added successfully, otherwise false
	 */
	public boolean addRecord(Program program)
	{
		if (isDateInFuture(program.getStartTime()))
		{
			String channelId = program.getChannel().getChannelId();
			NavigableMap<Calendar, Program> navMap = _channelToRecordsNavigableMap.get(channelId);
			if (navMap == null)
			{
				navMap = new TreeMap<Calendar, Program>();
				_channelToRecordsNavigableMap.put(channelId, navMap);
			}

			navMap.put(program.getStartTime(), program);
			int dayOffset = Calendars.getDayOffsetByDate(program.getStartTime());
			Log.i(TAG, "Adding dayoffset " + dayOffset + " for " + program.getChannel().getChannelId() + "/"
			        + Calendars.makeString(program.getStartTime()));
			_dayOffsets.add(dayOffset);
		}
		else
		{
			Log.w(TAG, "Try to record schedule in the past " + Calendars.makeString(program.getStartTime())
			        + "on channel " + program.getChannel().getChannelId());
			return false;
		}

		return saveRecords();
	}

	/**
	 * Remove schedule record for given program
	 *
	 * @param program
	 * @return true if record is removed successfully, otherwise false
	 */
	public boolean removeRecord(Program program)
	{
		NavigableMap<Calendar, Program> navMap = _channelToRecordsNavigableMap.get(program.getChannel().getChannelId());
		if (navMap != null)
		{
			navMap.remove(program.getStartTime());
			int dayOffset = Calendars.getDayOffsetByDate(program.getStartTime());
			if (getRecordsByDate(dayOffset).size() == 0)
			{
				Log.i(TAG, "Removing dayoffset " + dayOffset + " for " + program.getChannel().getChannelId() + "/"
				        + Calendars.makeString(program.getStartTime()));
				_dayOffsets.remove(dayOffset);
			}
			return saveRecords();
		}
		return true;
	}

	/**
	 * Return all programs for recording by date
	 *
	 * @param dateOffset
	 *            - offset to current day, ex: 0 - current day, +1 (next day)
	 */
	public List<Program> getRecordsByDate(int dateOffset)
	{
		List<Program> programs = new ArrayList<Program>();
		Calendar date = Calendars.getDateByDayOffsetFrom(
		        Calendar.getInstance(_feature.Component.TIMEZONE.getTimeZone()), dateOffset);
		// String strDate = String.format("%04d%02d%02d",
		// date.get(Calendar.YEAR), 1 + date.get(Calendar.MONTH),
		// date.get(Calendar.DAY_OF_MONTH));
		Log.d(TAG, ".getRecordsByDate: dateOffset = " + dateOffset + " -> " + Calendars.makeString(date));

		for (NavigableMap<Calendar, Program> map : _channelToRecordsNavigableMap.values())
		{
			for (Program program : map.values())
			{
				Calendar programTime = program.getStartTime();
				Log.d(TAG,
				        "Comparing entry offset " + Calendars.makeString(programTime) + " - "
				                + Calendars.getDayOffsetByDate(programTime) + " with dateOffset = " + dateOffset);
				if (Calendars.getDayOffsetByDate(programTime) == dateOffset)
				{
					programs.add(program);
				}
			}
		}
		Collections.sort(programs, _recordingSchedulerComparator);
		return programs;
	}

	/**
	 * Checks if program is scheduled for recording
	 *
	 * @param program
	 */
	public boolean isProgramRecorded(Program program)
	{
		if (program == null)
		{
			Log.e(TAG, ".isProgramRecorded(null) is not allowed!");
			return false;
		}

		if (_channelToRecordsNavigableMap == null)
		{
			Log.e(TAG, ".isProgramRecorded: _channelToRecordsNavigableMap = null is not allowed!");
			return false;
		}

		Channel channel = program.getChannel();
		NavigableMap<Calendar, Program> navMap = _channelToRecordsNavigableMap.get(channel.getChannelId());
		if (navMap == null)
		{
			return false;
		}
		return navMap.containsKey(program.getStartTime());
	}

	/**
	 * Check if schedule record is in future
	 *
	 * @param date
	 * @return true if schedule record date is valid, false otherwise
	 */
	private boolean isDateInFuture(Calendar date)
	{
		Calendar now = _feature.Component.TIMEZONE.getCurrentTime();
		return !date.before(now);
	}

	/**
	 * Checks if there exists records on given channel
	 *
	 * @param channel
	 * @return true if there exists records on given channel, otherwise false
	 */
	public boolean isAvailableRecordOnChannel(Channel channel)
	{
		NavigableMap<Calendar, Program> navMap = _channelToRecordsNavigableMap.get(channel.getChannelId());
		if (navMap == null)
		{
			return false;
		}
		return navMap.size() > 0;
	}

	/**
	 * Returns iterator of integers representing those day offsets having one or
	 * more recordings
	 *
	 * @return Iterator<Integer>
	 */
	public Iterator<Integer> getRecordedDayOffsets()
	{
		return _dayOffsets.iterator();
	}

	/**
	 * Stores records to data repository
	 */
	private boolean saveRecords()
	{
		SimpleDateFormat sdfUTC = new SimpleDateFormat(PROGRAM_TIME_ID, Locale.US);
		TimeZone utc = TimeZone.getTimeZone("UTC");
		sdfUTC.setTimeZone(utc);

		StringBuilder buffer = new StringBuilder();
		for (NavigableMap<Calendar, Program> map : _channelToRecordsNavigableMap.values())
		{
			for (Program program : map.values())
			{
				buffer.append(program.getChannel().getChannelId());
				buffer.append(ITEM_DELIMITER);
				buffer.append(sdfUTC.format(program.getStartTime().getTime()));
				buffer.append(ITEM_DELIMITER);
				buffer.append(Integer.toString((int) (program.getLengthMillis() / 1000)));
				buffer.append(RECORD_DELIMITER);
			}
		}
		return saveRecordsToDataProvider(buffer.toString());
	}

	/**
	 * Load records from data repository
	 *
	 * @param onLoadRecordings
	 *            - OnLoadRecordings callback
	 */
	protected void loadRecordFromDataProvider(final OnResultReceived onResultReceived)
	{
		final HashMap<String, NavigableMap<Calendar, Program>> channelToRecordsNavigableMap = new HashMap<String, NavigableMap<Calendar, Program>>();
		if (!_userPrefs.has(UserParam.RECORDINGS))
		{
			onResultReceived.onReceiveResult(FeatureError.OK(this), channelToRecordsNavigableMap);
			return;
		}

		SimpleDateFormat sdfUTC = new SimpleDateFormat(PROGRAM_TIME_ID, Locale.US);
		TimeZone utc = TimeZone.getTimeZone("UTC");
		sdfUTC.setTimeZone(utc);

		List<String> channelIds = new ArrayList<String>();
		List<String> programIds = new ArrayList<String>();
		String recordings = _userPrefs.getString(UserParam.RECORDINGS);
		Log.i(TAG, "Load recordings: " + recordings);
		String[] records = recordings.split(RECORD_DELIMITER);
		for (String record : records)
		{
			String[] items = record.split(ITEM_DELIMITER);
			if (items.length != 3)
			{
				Log.e(TAG, "Invalid recording format! Expected chnid,start,duration + got " + record);
				continue;
			}
			final String channelId = items[0];
			final String programId = items[1];

			Calendar calStartTime = Calendar.getInstance(utc);
			try
			{
				calStartTime.setTime(sdfUTC.parse(programId));
			}
			catch (ParseException e)
			{
				Log.e(TAG, e.getMessage(), e);
				continue;
			}

			channelIds.add(channelId);
			programIds.add(programId);
		}

		_feature.Component.EPG.getMultiplePrograms(channelIds, programIds, new OnResultReceived()
		{
			@Override
			public void onReceiveResult(FeatureError error, Object object)
			{
				if (!error.isError())
				{
					@SuppressWarnings("unchecked")
					List<Program> programs = (List<Program>) object;
					for (Program program : programs)
					{
						String channelId = program.getChannel().getChannelId();
						NavigableMap<Calendar, Program> navigableMap = null;
						if (!channelToRecordsNavigableMap.containsKey(channelId))
						{
							navigableMap = new TreeMap<Calendar, Program>();
							channelToRecordsNavigableMap.put(channelId, navigableMap);
						}
						else
						{
							navigableMap = channelToRecordsNavigableMap.get(channelId);
						}
						navigableMap.put(program.getStartTime(), program);

						int dayOffset = Calendars.getDayOffsetByDate(program.getStartTime());
						Log.i(TAG,
						        "Adding dayOffset = " + dayOffset + " from "
						                + Calendars.makeString(program.getStartTime()) + " into the set");
						_dayOffsets.add(dayOffset);
					}
					onResultReceived.onReceiveResult(FeatureError.OK(FeatureRecordingScheduler.this),
					        channelToRecordsNavigableMap);
				}
				else
				{
					onResultReceived.onReceiveResult(error, channelToRecordsNavigableMap);
				}
			}
		});
	}

	protected boolean saveRecordsToDataProvider(String recordings)
	{
		_userPrefs.put(UserParam.RECORDINGS, recordings);
		return true;
	}

	/**
	 * Loading records callback interface
	 */
	protected interface OnLoadRecordings
	{
		public void onRecordingLoaded(FeatureError error, Map<String, NavigableMap<String, Program>> map);
	}
}

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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
	private static final int DATE_FORMAT_LEN = 8;

	/** key = chanelID; value = map between record endTime and schedule record */
	private Map<String, NavigableMap<String, RecordingScheduler>> _channelToRecordsNavigableMap = null;

	private Set<Integer> _dayOffsets = new TreeSet<Integer>();

	/**
	 * FIXME: Obtain from more general place
	 */
	private SimpleDateFormat _sdfUTC;
	private TimeZone _utc;
	private Prefs _userPrefs;

	public static enum UserParam
	{
		/**
		 * Store all scheduled recordings
		 */
		RECORDINGS
	}

	public static enum Param
	{
		/**
		 * Program expiration period in hours
		 */
		EXPIRE_PERIOD(24);

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.RECORDING_SCHEDULER).put(name(), value);
		}
	}

	@SuppressLint("SimpleDateFormat")
	public FeatureRecordingScheduler() throws FeatureNotFoundException
	{
		require(FeatureName.Component.TIMEZONE);
		_sdfUTC = new SimpleDateFormat("yyyyMMddHHmmss");
		_utc = TimeZone.getTimeZone("UTC");
	}

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		_userPrefs = Environment.getInstance().getUserPrefs();
		_sdfUTC.setTimeZone(_utc);

		loadRecordFromDataProvider(new OnLoadRecordings()
		{
			@Override
			public void onRecordingLoaded(FeatureError error,
			        Map<String, NavigableMap<String, RecordingScheduler>> channelToRecordsNavigableMap)
			{
				_channelToRecordsNavigableMap = channelToRecordsNavigableMap;
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
	 * Add new schedule record for given time range
	 *
	 * @param channelID
	 *            channel ID
	 * @param start
	 *            range start in UTC
	 * @param duration
	 *            range duration in seconds
	 * @return true if record is added successfully, otherwise false
	 */
	public boolean addRecord(String channelID, Calendar start, int duration)
	{
		if (duration <= 0)
		{
			Log.w(TAG, "Try to record schedule with invalid duration " + duration);
			return false;
		}

		String startTime = _sdfUTC.format(start.getTime());
		Calendar calTime = Calendar.getInstance();
		calTime.setTime(start.getTime());
		calTime.add(Calendar.SECOND, duration);
		String endTime = _sdfUTC.format(calTime.getTime());

		if (isDateInFuture(start))
		{
			NavigableMap<String, RecordingScheduler> navMap = _channelToRecordsNavigableMap.get(channelID);
			String nextRecord = null;
			if (navMap == null)
			{
				navMap = new TreeMap<String, RecordingScheduler>();
				_channelToRecordsNavigableMap.put(channelID, navMap);
			}

			// get record with least start time great than record's start time
			nextRecord = navMap.ceilingKey(startTime);

			// check if such record doen't exist or it starts after newly added
			// record
			// has finished
			if ((nextRecord == null) || (nextRecord.compareTo(endTime) >= 0))
			{
				try
				{
					// get record with greatest start time less than newly added
					// record's start time
					String prevRecord = navMap.floorKey(startTime);
					boolean isRecordValid = false;

					// check if such prevRecord () doen't exist
					if (prevRecord != null)
					{
						calTime.setTime(_sdfUTC.parse(prevRecord));
						int secDuration = navMap.get(prevRecord).getDuration();
						calTime.add(Calendar.SECOND, secDuration);
						String prevEndTime = _sdfUTC.format(calTime.getTime());
						// check if newly added record start after prevRecord
						// has finished
						isRecordValid = (startTime.compareTo(prevEndTime) >= 0);
					}
					else
					{
						isRecordValid = true;
					}
					if (isRecordValid)
					{
						RecordingScheduler rc = new RecordingScheduler(channelID, startTime, duration);
						navMap.put(startTime, rc);
						int dayOffset = Calendars.getDayOffsetByDate(start);
						Log.i(TAG,
						        "Adding dayoffset " + dayOffset + " for " + channelID + "/"
						                + Calendars.makeString(start));
						_dayOffsets.add(dayOffset);
					}
					else
					{
						Log.w(TAG, "Overlapped channel intervals. Impossible to add interval " + startTime);
						return false;
					}
				}
				catch (ParseException e)
				{
					Log.e(TAG, e.getMessage(), e);
					return false;

				}
			}
			else
			{
				Log.w(TAG, "Overlapped channel intervals.Impossible to add interval " + startTime);
				return false;
			}
		}
		else
		{
			Log.w(TAG, "Try to record schedule in the past " + start + "on channel " + channelID);
			return false;
		}

		return saveRecords();
	}

	/**
	 * Add new schedule record for given program
	 *
	 * @param program
	 * @return true if record is added successfully, otherwise false
	 */
	public boolean addRecord(Program program)
	{
		if (program == null)
		{
			Log.e(TAG, ".addRecord(null) is not allowed");
			return false;
		}
		int duration = program.getLengthMin() * 60;
		return addRecord(program.getChannel().getChannelId(), program.getStartTime(), duration);
	}

	/**
	 * Remove schedule record for given program
	 *
	 * @param program
	 * @return true if record is removed successfully, otherwise false
	 */
	public boolean removeRecord(Program program)
	{
		int duration = program.getLengthMin() * 60;
		return removeRecord(program.getChannel().getChannelId(), program.getStartTime(), duration);
	}

	/**
	 * Remove record by channel Id and start time
	 *
	 * @param channelID
	 * @param start
	 * @param duration
	 * @return
	 */
	public boolean removeRecord(String channelID, Calendar start, int duration)
	{
		NavigableMap<String, RecordingScheduler> navMap = _channelToRecordsNavigableMap.get(channelID);
		if (navMap != null)
		{
			String startTime = _sdfUTC.format(start.getTime());
			navMap.remove(startTime);
			int dayOffset = Calendars.getDayOffsetByDate(start);
			if (getRecordsByDate(dayOffset).size() == 0)
			{
				Log.i(TAG, "Removing dayoffset " + dayOffset + " for " + channelID + "/" + Calendars.makeString(start));
				_dayOffsets.remove(dayOffset);
			}
			return saveRecords();
		}
		return true;
	}

	/**
	 * Return all records by date
	 *
	 * @param dateOffset
	 *            - offset to current day, ex: 0 - current day, +1 (next day)
	 */
	public List<RecordingScheduler> getRecordsByDate(int dateOffset)
	{
		List<RecordingScheduler> ls = new ArrayList<RecordingScheduler>();
		Calendar date = Calendars.getDateByDayOffsetFrom(
		        Calendar.getInstance(_feature.Component.TIMEZONE.getTimeZone()), dateOffset);
		// String strDate = String.format("%04d%02d%02d",
		// date.get(Calendar.YEAR), 1 + date.get(Calendar.MONTH),
		// date.get(Calendar.DAY_OF_MONTH));
		Log.d(TAG, ".getRecordsByDate: dateOffset = " + dateOffset + " -> " + Calendars.makeString(date));

		for (NavigableMap<String, RecordingScheduler> map : _channelToRecordsNavigableMap.values())
		{
			for (RecordingScheduler entry : map.values())
			{
				String startTime = entry.getStartTime();
				try
				{
					Date programDate = _sdfUTC.parse(startTime);
					Calendar programTime = Calendar.getInstance(_feature.Component.TIMEZONE.getTimeZone());
					programTime.setTime(programDate);
					Log.d(TAG,
					        "Comparing entry offset " + Calendars.makeString(programTime) + " - "
					                + Calendars.getDayOffsetByDate(programTime) + " with dateOffset = " + dateOffset);
					if (Calendars.getDayOffsetByDate(programTime) == dateOffset)
					{
						ls.add(entry);
					}
				}
				catch (ParseException e)
				{
					Log.e(TAG, e.getMessage(), e);
				}
			}
		}
		return ls;
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
		NavigableMap<String, RecordingScheduler> navMap = _channelToRecordsNavigableMap.get(channel.getChannelId());
		if (navMap == null)
		{
			return false;
		}
		String progId = program.getId();
		return navMap.containsKey(progId);
	}

	/**
	 * Check if schedule record is in future
	 *
	 * @param date
	 * @return true if schedule record date is valid, false otherwise
	 */
	private boolean isDateInFuture(Calendar date)
	{
		Calendar now = Calendar.getInstance(_utc);
		return !date.before(now);
	}

	/**
	 * Checks if scheduled recording expired
	 *
	 * @param date
	 * @param channelID
	 *            - when argument is not set to null, take into account channel
	 *            expire period (FIXME: not implemented)
	 * @return true if schedule record expires, false otherwise
	 */
	private boolean isDateExpiredForRecordings(Calendar date, String channelID)
	{
		Calendar now = Calendar.getInstance(_utc);
		now.add(Calendar.HOUR, -getPrefs().getInt(Param.EXPIRE_PERIOD));
		return date.before(now);
	}

	/**
	 * Checks if there exists records on given channel
	 *
	 * @param channel
	 * @return true if there exists records on given channel, otherwise false
	 */
	public boolean isAvailableRecordOnChannel(Channel channel)
	{
		NavigableMap<String, RecordingScheduler> navMap = _channelToRecordsNavigableMap.get(channel.getChannelId());
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
		StringBuilder buffer = new StringBuilder();
		Calendar calStartTime = _feature.Component.TIMEZONE.getCurrentTime();

		for (NavigableMap<String, RecordingScheduler> map : _channelToRecordsNavigableMap.values())
		{
			for (RecordingScheduler entry : map.values())
			{
				try
				{
					calStartTime.setTime(_sdfUTC.parse(entry.getStartTime()));
					if (isDateExpiredForRecordings(calStartTime, null))
					{
						continue;
					}
					buffer.append(entry.getChannelID());
					buffer.append(ITEM_DELIMITER);
					buffer.append(entry.getStartTime());
					buffer.append(ITEM_DELIMITER);
					buffer.append(Integer.toString(entry.getDuration()));
					buffer.append(RECORD_DELIMITER);
				}
				catch (ParseException e)
				{
					Log.e(TAG, e.getMessage(), e);
					return false;
				}
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
	protected void loadRecordFromDataProvider(OnLoadRecordings onLoadRecordings)
	{
		FeatureError error = FeatureError.OK(this);
		HashMap<String, NavigableMap<String, RecordingScheduler>> channelToRecordsNavigableMap = new HashMap<String, NavigableMap<String, RecordingScheduler>>();
		try
		{
			if (_userPrefs.has(UserParam.RECORDINGS))
			{
				String recordings = _userPrefs.getString(UserParam.RECORDINGS);
				Log.i(TAG, "Load recordings: " + recordings);
				String[] records = recordings.split(RECORD_DELIMITER);
				for (String record : records)
				{
					String[] items = record.split(ITEM_DELIMITER);
					String chnId = items[0];
					String startTime = items[1];
					int duration = Integer.parseInt(items[2]);

					Calendar calStartTime = Calendar.getInstance(_utc);
					calStartTime.setTime(_sdfUTC.parse(startTime));

					if (isDateExpiredForRecordings(calStartTime, null))
					{
						Log.i(TAG, "Record since " + Calendars.makeString(calStartTime) + " has expired date");
						continue;
					}

					NavigableMap<String, RecordingScheduler> navigableMap = null;
					RecordingScheduler rc = new RecordingScheduler(chnId, startTime, duration);

					if (!channelToRecordsNavigableMap.containsKey(chnId))
					{
						navigableMap = new TreeMap<String, RecordingScheduler>();
						channelToRecordsNavigableMap.put(chnId, navigableMap);
					}
					else
					{
						navigableMap = channelToRecordsNavigableMap.get(chnId);
					}

					navigableMap.put(startTime, rc);
					int dayOffset = Calendars.getDayOffsetByDate(calStartTime);
					Log.i(TAG, "Adding dayOffset = " + dayOffset + " from " + Calendars.makeString(calStartTime)
					        + " into the set");
					_dayOffsets.add(dayOffset);
				}
			}
		}
		catch (ParseException e)
		{
			error = new FeatureError(this, e);
		}
		onLoadRecordings.onRecordingLoaded(error, channelToRecordsNavigableMap);
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
		public void onRecordingLoaded(FeatureError error, Map<String, NavigableMap<String, RecordingScheduler>> map);
	}
}

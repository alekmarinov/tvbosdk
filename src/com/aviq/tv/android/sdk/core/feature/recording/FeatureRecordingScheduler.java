/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureRecordingScheduler.java
 * Author:      elmira
 * Date:        14 Jul 2014
 * Description: Feature managing scheduled recordings
 */
package com.aviq.tv.android.sdk.core.feature.recording;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TimeZone;
import java.util.TreeMap;

import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.feature.epg.Program;

/**
 * Feature managing scheduled recordings
 */
public class FeatureRecordingScheduler extends FeatureComponent
{
	private static final String TAG = FeatureRecordingScheduler.class.getSimpleName();
	private static final String RECORD_DELIMITER = ";";
	private static final String ITEM_DELIMITER = ",";
	private static final int DATE_FORMAT_LEN = 8;

	/** key = chanelID; value = map between record endTime and schedule record */
	private Map<String, NavigableMap<String, RecordingScheduler>> _channelToRecordsNavigableMap = new HashMap<String, NavigableMap<String, RecordingScheduler>>();

	/**
	 * FIXME: Obtain from more general place
	 */
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
	private Prefs _userPrefs;

	public enum UserParam
	{
		/**
		 * Store all scheduled recordings
		 */
		RECORDINGS
	}

	public enum Param
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

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		_userPrefs = Environment.getInstance().getUserPrefs();
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

		// FIXME: no sense to clear this map here, it is more likely to be
		// returned as value of loadRecords

		_channelToRecordsNavigableMap.clear();

		// FIXME:
		// 1. load records may fail and should notify onFeatureInitialized
		// callback with appropriate result code
		// 2. better delegate onFeatureInitialized to loadRecords
		loadRecords();

		if (onFeatureInitialized != null)
		{
			onFeatureInitialized.onInitialized(FeatureRecordingScheduler.this, ResultCode.OK);
		}
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
	 *            range start
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

		String startTime = sdf.format(start.getTime());
		Calendar calTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calTime.setTime(start.getTime());
		calTime.add(Calendar.SECOND, duration);
		String endTime = sdf.format(calTime.getTime());

		if (isValidDate(start))
		{
			NavigableMap<String, RecordingScheduler> navMap = _channelToRecordsNavigableMap.get(channelID);
			String nextRecord = null;
			if (navMap == null)
			{
				navMap = new TreeMap<String, RecordingScheduler>();
				_channelToRecordsNavigableMap.put(channelID, navMap);
			}

			nextRecord = navMap.ceilingKey(startTime);

			// FIXME: comment this condition in english
			if ((nextRecord == null) || (nextRecord.compareTo(endTime) >= 0))
			{
				try
				{
					String prevRecord = navMap.floorKey(startTime);
					boolean isRecordValid = false;

					// FIXME: comment this condition in english
					if (prevRecord != null)
					{
						calTime.setTime(sdf.parse(prevRecord));
						int secDuration = navMap.get(prevRecord).getDuration();
						calTime.add(Calendar.SECOND, secDuration);
						String prevEndTime = sdf.format(calTime.getTime());
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
					// FIXME: no return?
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

		// FIXME: save is evil, prepare to handle errors
		saveRecords();
		return true;
	}

	/**
	 * Add new schedule record for given program
	 *
	 * @param program
	 * @return true if record is added successfully, otherwise false
	 */
	public boolean addRecord(Program program)
	{
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
	 * FIXME: comment this method
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
			String startTime = sdf.format(start.getTime());
			navMap.remove(startTime);

			// FIXME: save is evil, prepare to handle errors
			saveRecords();
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
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
		Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		now.add(Calendar.DATE, dateOffset);
		String strNow = df.format(now.getTime());
		for (NavigableMap<String, RecordingScheduler> map : _channelToRecordsNavigableMap.values())
		{
			for (RecordingScheduler entry : map.values())
			{

				String startTime = entry.getStartTime();
				startTime = startTime.substring(0, DATE_FORMAT_LEN);
				if (strNow.compareTo(startTime) == 0)
				{
					ls.add(entry);
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
		NavigableMap<String, RecordingScheduler> navMap = _channelToRecordsNavigableMap.get(program.getChannel()
		        .getChannelId());
		if (navMap == null)
		{
			return false;
		}
		String progId = program.getId();
		return navMap.containsKey(progId);
	}

	/**
	 * FIXME: Consider renaming to isDateInFuture
	 *
	 * Check if schedule record is valid
	 *
	 * @param date
	 * @return true if schedule record date is valid, false otherwise
	 */
	private boolean isValidDate(Calendar date)
	{
		Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		return !date.before(now);
	}

	/**
	 * FIXME: Consider renaming to isDateExpiredForRecordings
	 * the method should also depends on Channel in general
	 *
	 * Checks if scheduled recording expired
	 *
	 * @param date
	 * @return true if schedule record expires, false otherwise
	 */
	private boolean isExpireRecord(Calendar date)
	{
		Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		now.add(Calendar.HOUR, -getPrefs().getInt(Param.EXPIRE_PERIOD));
		return date.before(now);
	}

	/**
	 * FIXME: It's good to have callback param reporting when finished with what
	 * status
	 *
	 * Load records from data repository and parse data
	 */
	private void loadRecords()
	{
		String strRecords = loadRecordFromDataProvider();
		if (strRecords == null)
		{
			Log.w(TAG, "No available data from data provider");
			return;
		}

		String[] records = strRecords.split(RECORD_DELIMITER);

		for (String record : records)
		{
			try
			{
				String[] items = record.split(ITEM_DELIMITER);
				String chnId = items[0];
				String startTime = items[1];
				int duration = Integer.parseInt(items[2]);

				Calendar calStartTime = Calendar.getInstance();
				calStartTime.setTime(sdf.parse(startTime));

				if (isExpireRecord(calStartTime))
				{
					// FIXME: log this interesting case
					continue;
				}

				NavigableMap<String, RecordingScheduler> navigableMap = null;
				RecordingScheduler rc = new RecordingScheduler(chnId, startTime, duration);

				if (!_channelToRecordsNavigableMap.containsKey(chnId))
				{
					navigableMap = new TreeMap<String, RecordingScheduler>();
					_channelToRecordsNavigableMap.put(chnId, navigableMap);
				}
				else
				{
					navigableMap = _channelToRecordsNavigableMap.get(chnId);
				}

				navigableMap.put(startTime, rc);
			}
			catch (ParseException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
		}
	}

	/**
	 * FIXME: this method should handle errors by saveRecordsToDataProvider
	 *
	 * Stores records to data repository
	 */
	private void saveRecords()
	{
		StringBuilder buffer = new StringBuilder();
		Calendar calStartTime = Calendar.getInstance();

		for (NavigableMap<String, RecordingScheduler> map : _channelToRecordsNavigableMap.values())
		{
			for (RecordingScheduler entry : map.values())
			{
				try
				{
					calStartTime.setTime(sdf.parse(entry.getStartTime()));
					if (isExpireRecord(calStartTime))
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
				}
			}
		}

		saveRecordsToDataProvider(buffer.toString());
	}

	/**
	 * FIXME: it's good to have callback param
	 *
	 * Load records from data repository
	 */
	protected String loadRecordFromDataProvider()
	{
		if (_userPrefs.has(UserParam.RECORDINGS))
			return _userPrefs.getString(UserParam.RECORDINGS);
		return null;
	}

	/**
	 * FIXME: this method should handle errors
	 *
	 * Store records to data repository
	 */
	protected void saveRecordsToDataProvider(String serObject)
	{
		_userPrefs.put(UserParam.RECORDINGS, serObject);
	}
}

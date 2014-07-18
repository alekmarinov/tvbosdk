package com.aviq.tv.android.sdk.feature.epg;

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

import android.os.Bundle;
import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;

public class FeatureScheduleRecorder extends FeatureComponent
{
	public static final String TAG = FeatureScheduleRecorder.class.getSimpleName();
	
	private static final String RECORD_DELIMITER = ";";
	private static final String ITEM_DELIMITER = ",";
	private static final int DATE_FORMAT_LEN = 8;
	
	private Prefs _userPrefs;
	
	/** key = chanelID; value = map between record endTime and schedule record */
	private Map<String, NavigableMap<String, RecordSched>> _channelToRecordsNavigableMap = new HashMap<String, NavigableMap<String, RecordSched>>();
	
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
	
	public enum UserParam
	{
		/**
		 * Store all schedule records
		 */
		SCHEDULE_RECORDS
	}
	
	public enum Param
	{
		/**
		 * Program expiration period in hours
		 */
		EXPIRE_PERIOD(24);
		
		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.SCHEDULE_RECORDER).put(name(), value);
		}
		
	}
	
	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Environment env = Environment.getInstance();
		_userPrefs = env.getUserPrefs();
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		_channelToRecordsNavigableMap.clear();
		loadRecords();
		if (onFeatureInitialized != null)
		{
			onFeatureInitialized.onInitialized(FeatureScheduleRecorder.this, ResultCode.OK);
		}
	}
	
	@Override
	public Component getComponentName()
	{
		// TODO Auto-generated method stub
		return FeatureName.Component.SCHEDULE_RECORDER;
	}
	
	/**
	 * Add new schedule record for give time range
	 * 
	 * @param channelID
	 *            channel ID
	 * @param start
	 *            range start
	 * @param duration
	 *            range duration in seconds
	 * @return true if record is finished successfully, otherwise false
	 */
	public boolean addRecord(String channelID, Calendar start, int duration)
	{
		if (duration == 0)
		{
			Log.w(TAG, "Try to record schedule with duration 0 ");
			return false;
		}
		
		String startTime = sdf.format(start.getTime());
		Calendar calTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calTime.setTime(start.getTime());
		calTime.add(Calendar.SECOND, duration);
		String endTime = sdf.format(calTime.getTime());
		
		if (isValidDate(start))
		{
			NavigableMap<String, RecordSched> navMap = _channelToRecordsNavigableMap.get(channelID);
			String nextRecord = null;
			if (navMap == null)
			{
				navMap = new TreeMap<String, RecordSched>();
				_channelToRecordsNavigableMap.put(channelID, navMap);
			}
			
			nextRecord = navMap.ceilingKey(startTime);
			
			if ((nextRecord == null) || (nextRecord.compareTo(endTime) >= 0))
			{
				
				try
				{
					String prevRecord = navMap.floorKey(startTime);
					boolean isRecordValid = false;
					if (prevRecord != null)
					{
						calTime.setTime(sdf.parse(prevRecord));
						int secDuration = navMap.get(prevRecord).get_duration();
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
						RecordSched rc = new RecordSched(channelID, startTime, duration);
						navMap.put(startTime, rc);
					}
					else
					{
						Log.w(TAG,
						        String.format("Overlapped channel intervals.Impossible to add interval %s ", startTime));
						return false;
					}
				}
				catch (ParseException e)
				{
					// TODO Auto-generated catch block
					Log.e(TAG, e.getMessage(), e);
				}
				
			}
			else
			{
				Log.w(TAG, String.format("Overlapped channel intervals.Impossible to add interval %s ", startTime));
				return false;
			}
			
		}
		else
		{
			Log.w(TAG, "Try to record schedule in the past " + start + "on channel " + channelID);
			return false;
		}
		
		saveRecords();
		
		return true;
		
	}
	
	/**
	 * Add new schedule record for give program
	 * 
	 * @param program
	 * @return true if record is finished successfully, otherwise false
	 */
	public boolean addRecord(Program program)
	{
		int duration = program.getLengthMin() * 60;
		return addRecord(program.getChannel().getChannelId(), program.getStartTime(), duration);
	}
	
	/**
	 * Remove schedule record for give program
	 * 
	 * @param program
	 * @return true if record is finished successfully, otherwise false
	 */
	public boolean removeRecord(Program program)
	{
		int duration = program.getLengthMin() * 60;
		return removeRecord(program.getChannel().getChannelId(), program.getStartTime(), duration);
		
	}
	
	public boolean removeRecord(String channelID, Calendar start, int duration)
	{
		NavigableMap<String, RecordSched> navMap = _channelToRecordsNavigableMap.get(channelID);
		if (navMap != null)
		{
			String startTime = sdf.format(start.getTime());
			navMap.remove(startTime);
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
	public List<RecordSched> getRecordsByDate(int dateOffset)
	{
		
		List<RecordSched> ls = new ArrayList<RecordSched>();
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
		Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		now.add(Calendar.DATE, dateOffset);
		String strNow = df.format(now.getTime());
		
		for (NavigableMap<String, RecordSched> map : _channelToRecordsNavigableMap.values())
		{
			
			for (RecordSched entry : map.values())
			{
				
				String startTime = entry.get_startTime();
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
	 * Check if schedule record exists for given program
	 * 
	 * @param prog
	 */
	public boolean isProgramRecorded(Program prog)
	{
		
		NavigableMap<String, RecordSched> navMap = _channelToRecordsNavigableMap.get(prog.getChannel().getChannelId());
		if (navMap == null)
		{
			return false;
		}
		String progId = prog.getId();
		return navMap.containsKey(progId);
		
	}
	
	/**
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
	 * Check if schedule record expires
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
					continue;
				}
				
				NavigableMap<String, RecordSched> navigableMap = null;
				RecordSched rc = new RecordSched(chnId, startTime, duration);
				
				if (!_channelToRecordsNavigableMap.containsKey(chnId))
				{
					navigableMap = new TreeMap<String, RecordSched>();
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
	 * Stores records to data repository
	 */
	private void saveRecords()
	{
		
		StringBuilder buffer = new StringBuilder();
		Calendar calStartTime = Calendar.getInstance();
		
		for (NavigableMap<String, RecordSched> map : _channelToRecordsNavigableMap.values())
		{
			
			for (RecordSched entry : map.values())
			{
				try
				{
					calStartTime.setTime(sdf.parse(entry.get_startTime()));
					if (isExpireRecord(calStartTime))
					{
						continue;
					}
					buffer.append(entry.get_channelID());
					buffer.append(ITEM_DELIMITER);
					buffer.append(entry.get_startTime());
					buffer.append(ITEM_DELIMITER);
					buffer.append(Integer.toString(entry.get_duration()));
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
	 * Load records from data repository
	 */
	protected String loadRecordFromDataProvider()
	{
		if (_userPrefs.has(UserParam.SCHEDULE_RECORDS))
			return _userPrefs.getString(UserParam.SCHEDULE_RECORDS);
		return null;
		
	}
	
	/**
	 * Store records to data repository
	 */
	protected void saveRecordsToDataProvider(String serObject)
	{
		_userPrefs.put(UserParam.SCHEDULE_RECORDS, serObject);
	}
	
}

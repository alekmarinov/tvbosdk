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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.os.Bundle;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventReceiver;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.command.CommandHandler;
import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.epg.Program;
import com.aviq.tv.android.sdk.feature.epg.ProgramAttribute;
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
	
	public static enum Command
	{
		POST_RECORDING_SECHEDULER, GET_RECORDINGS_BY_DAY
	}
	
	public static enum CommandPostRecordingSchedulerExtras
	{
		PROGRAM_ID, CHANNEL_ID
	}
	
	/*public static enum CommandGetRecordingsByDay
	{
		DAY_OFFSET
	}*/
	
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
		require(FeatureName.Component.COMMAND);
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
		_feature.Component.COMMAND.addCommandHandler(new OnCommandPostRecordingscheduler());
		_feature.Component.COMMAND.addCommandHandler(new OnCommandGetRecordingsByDay());
		//_feature.Component.COMMAND.addCommandHandler(new OnCommandGetRecordingsDayOffsets());
		
		Environment.getInstance().getEventMessenger().register(new EventReceiver()
		{
			@Override
			public void onEvent(int msgId, Bundle bundle)
			{
				testCommands();
				Environment.getInstance().getEventMessenger().unregister(this, Environment.ON_LOADED);
			}
		}, Environment.ON_LOADED);
		
		// onSchedule(onFeatureInitialized);
	}
	
	private void testCommands()
	{
		OnResultReceived onResultReceived = new OnResultReceived()
		{
			@Override
			public void onReceiveResult(FeatureError error, Object object)
			{
				if (error.isError())
				{
					Log.e(TAG, error.getMessage(), error);
				}
				else
				{
					JSONObject jsonObj = (JSONObject) object;
					Log.i(TAG, "JSON object returned");
				}
			}
		};
		// after OnResultReceived we call the tests...
		Bundle bundle = new Bundle();
		Log.i(TAG, "CommandPostRecordsExtras:" + CommandPostRecordingSchedulerExtras.CHANNEL_ID.name()
		        + CommandPostRecordingSchedulerExtras.PROGRAM_ID.name());
		bundle.putString(CommandPostRecordingSchedulerExtras.PROGRAM_ID.name(), "20150723130000");
		bundle.putString(CommandPostRecordingSchedulerExtras.CHANNEL_ID.name(), "bnt1_hd");
		_feature.Component.COMMAND.execute(Command.POST_RECORDING_SECHEDULER.name(), bundle, onResultReceived);
		
		onResultReceived = new OnResultReceived()
		{
			@Override
			public void onReceiveResult(FeatureError error, Object object)
			{
				if (error.isError())
				{
					Log.e(TAG, error.getMessage(), error);
				}
				else
				{
					if (object != null)
					{
						try
						{
							JSONArray jsonArray = (JSONArray) object;
							Log.i(TAG, "JSON object returned GET_RECORDINGS_BY_DAY:");
							for (int i = 0; i < jsonArray.length(); i++)
							{
								Log.i(TAG, jsonArray.get(i).toString());
							}
						}
						catch (JSONException e)
						{
							Log.e(TAG, e.getMessage(), e);
						}
					}
				}
			}
		};
		// after OnResultReceived we call the tests...
		Log.e(TAG, "Records for dayOffset -3");
		bundle.clear();
		bundle = new Bundle();
		//bundle.putString(CommandGetRecordingsByDay.DAY_OFFSET.name(), "1");
		_feature.Component.COMMAND.execute(Command.GET_RECORDINGS_BY_DAY.name(), bundle, onResultReceived);
		bundle.clear();
		
		Log.e(TAG, "Records for dayOffset -2");
		bundle = new Bundle();
		//bundle.putString(CommandGetRecordingsByDay.DAY_OFFSET.name(), "0");
		_feature.Component.COMMAND.execute(Command.GET_RECORDINGS_BY_DAY.name(), bundle, onResultReceived);
		bundle.clear();
		
		/*
		Log.e(TAG, "Records for dayOffset -1");
		bundle = new Bundle();
		//bundle.putString(CommandGetRecordingsByDay.DAY_OFFSET.name(), "-1");
		_feature.Component.COMMAND.execute(Command.GET_RECORDINGS_BY_DAY.name(), bundle, onResultReceived);
		bundle.clear();
		
		onResultReceived = new OnResultReceived()
		{
			@Override
			public void onReceiveResult(FeatureError error, Object object)
			{
				if (error.isError())
				{
					Log.e(TAG, error.getMessage(), error);
				}
				else
				{
					if (object != null)
					{
						try
						{
							JSONArray jsonArray = (JSONArray) object;
							Log.i(TAG, "JSON object returned for records");
							for (int i = 0; i < jsonArray.length(); i++)
							{
								Log.i(TAG, "dayOffset:" + jsonArray.get(i).toString());
							}
						}
						catch (JSONException e)
						{
							Log.e(TAG, e.getMessage(), e);
						}
					}
				}
			}
		};
		// after OnResultReceived we call the tests...
		Log.e(TAG, "Records for dayOffset 0");
		_feature.Component.COMMAND.execute(Command.GET_RECORDINGS_DAY_OFFSETS.name(), bundle, onResultReceived);
		*/
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
		if (programs.size() > 0)
		{
			Log.d(TAG , "program from getRecordsByDate " + programs.get(0).getTitle());
		}
		else
		{
			Log.d(TAG , "no programs from getRecordsByDate with dateOffset = " + dateOffset);
		}
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
		if (!_userPrefs.has(UserParam.RECORDINGS) || _userPrefs.getString(UserParam.RECORDINGS).isEmpty())
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
						if (program == null)
							continue;
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
	
	private class OnCommandPostRecordingscheduler implements CommandHandler
	{
		public void execute(Bundle params, final OnResultReceived onResultReceived)
		{
			Log.i(TAG, ".OnCommandPostRECORDINGScheduler exec");
			// maybe not needed
			// loadRecordFromDataProvider(onResultReceived);
			
			String programId = params.getString(CommandPostRecordingSchedulerExtras.PROGRAM_ID.name());
			String channelId = params.getString(CommandPostRecordingSchedulerExtras.CHANNEL_ID.name());
			Log.i(TAG, ".OnCommandGetProgramDetails.execute: channelId = " + channelId.toString() + ", programId = "
			        + programId.toString());
			
			_feature.Component.EPG.getProgramDetails(channelId, programId, new OnResultReceived()
			{
				@Override
				public void onReceiveResult(FeatureError error, Object object)
				{
					if (error.isError())
					{
						Log.i(TAG, "error received from getProgramDetails in PostRecordingscheduler");
						onResultReceived.onReceiveResult(error, null);
					}
					else
					{
						Program program = (Program) object;
						
						if (!isProgramRecorded(program))
						{
							Log.i(TAG, "isProgram " + program.toString() + " recorded?" + isProgramRecorded(program));
							addRecord(program);
							Log.i(TAG, "program added");
							Log.i(TAG, "isProgram " + program.toString() + " recorded?" + isProgramRecorded(program));
							
							saveRecords();
							
							onResultReceived.onReceiveResult(FeatureError.OK(FeatureRecordingScheduler.this), null);
						}
						else
							onResultReceived.onReceiveResult(error, null);
						
					}
				}
			});
		}
		
		@Override
		public String getId()
		{
			Log.i(TAG, "Command.POST_RECORDING_SECHEDULER.name();");
			return Command.POST_RECORDING_SECHEDULER.name();
		}
	}
	
	private class OnCommandGetRecordingsByDay implements CommandHandler
	{
		public void execute(Bundle params, final OnResultReceived onResultReceived)
		{
			Log.i(TAG, ".OnCommandPostRECORDINGScheduler exec");
			Iterator<Integer> recordedDayoffset = getRecordedDayOffsets();
			Log.i(TAG, "recordedDayoffset:" + recordedDayoffset.toString());
			//int dayOffset = Integer.parseInt(params.getString(CommandGetRecordingsByDay.DAY_OFFSET.name()));
			
			
			final JSONArray jsonPrograms = new JSONArray();
			
			if(recordedDayoffset != null )
			while(recordedDayoffset.hasNext())
			{
				int dayOffset = recordedDayoffset.next();
				Log.i(TAG, ".OnCommandGetRecordingsByDay.execute: dayOffset = " + dayOffset);
				List<Program> programsInDayoffset = getRecordsByDate(dayOffset);
				Log.i(TAG, "programsInDayoffset size:" + programsInDayoffset.size());
				
				try
				{
					for (Program program : programsInDayoffset)
					{
							
						Log.i(TAG, "Program's title:" + program.getTitle());
						JSONObject jsonProgram = new JSONObject();
						jsonProgram.put("id", program.getId());
						jsonProgram.put("day_offset", dayOffset);
						jsonProgram.put("length", program.getLengthMin());
						jsonProgram.put("title", program.getTitle());
						String description = program.getDetailAttribute(ProgramAttribute.DESCRIPTION);
						if (description != null)
							jsonProgram.put("description", description);
						jsonProgram.put("image", program.getDetailAttribute(ProgramAttribute.IMAGE));
						Log.i(TAG, "jsonProgram:" + jsonProgram.toString());
						jsonPrograms.put(jsonProgram);
					}
				}
				catch (JSONException e)
				{
					Log.e(TAG, e.getMessage(), e);
					onResultReceived.onReceiveResult(new FeatureError(FeatureRecordingScheduler.this, e), null);
				}
			}
			onResultReceived.onReceiveResult(FeatureError.OK(FeatureRecordingScheduler.this), jsonPrograms);
		}
		
		@Override
		public String getId()
		{
			Log.i(TAG, "Command.GETRECORDINGSBYDAY.name();");
			return Command.GET_RECORDINGS_BY_DAY.name();
		}
	}
	
	/*
	private class OnCommandGetRecordingsDayOffsets implements CommandHandler
	{
		public void execute(Bundle params, final OnResultReceived onResultReceived)
		{
			try
			{
				JSONArray jsonPrograms = new JSONArray();
				
				
				if (recordedDayoffset == null || !recordedDayoffset.hasNext())
				{
					Log.i(TAG, "nothing to be displayed from records");
					onResultReceived.onReceiveResult(FeatureError.OK(FeatureRecordingScheduler.this), null);
				}
				else
				{
					while (recordedDayoffset.hasNext())
					{
						JSONObject jsonProgram = new JSONObject();
						jsonProgram.put("dayOffset", recordedDayoffset.next());
						jsonPrograms.put(jsonProgram);
					}
					onResultReceived.onReceiveResult(FeatureError.OK(FeatureRecordingScheduler.this),
					        jsonPrograms);
					
				}
			}
			catch (JSONException e)
			{
				Log.e(TAG, e.getMessage(), e);
				onResultReceived.onReceiveResult(new FeatureError(FeatureRecordingScheduler.this, e), null);
			}
		}
		
		@Override
		public String getId()
		{
			Log.i(TAG, "Command.GETRECORDINGSBYDAY.name();");
			return Command.GET_RECORDINGS_DAY_OFFSETS.name();
		}
	}*/
}

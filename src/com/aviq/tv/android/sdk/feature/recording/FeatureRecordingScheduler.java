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
import com.aviq.tv.android.sdk.feature.vod.FeatureVOD.Command;
import com.aviq.tv.android.sdk.feature.vod.FeatureVOD.CommandGetVodGroupsExtras;
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
		RECORD, GET_RECORDINGS, REMOVE_RECORDING, SET_RECORDING_WATCHED, GET_NUM_OF_NOT_WATCHED
	}

	public static enum CommandRecordExtras
	{
		PROGRAM_ID, CHANNEL_ID
	}
	
	public static enum CommandRemoveRecordingExtras
	{
		CHANNEL_ID, PROGRAM_ID
	}
	
	public static enum CommandSetRecordingWatched
	{
		CHANNEL_ID, PROGRAM_ID
	}
	
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
		_feature.Component.COMMAND.addCommandHandler(new OnCommandRecord());
		_feature.Component.COMMAND.addCommandHandler(new OnCommandGetRecordings());
		_feature.Component.COMMAND.addCommandHandler(new OnCommandRemoveRecording());
		_feature.Component.COMMAND.addCommandHandler(new OnCommandSetRecordingWatched());
		_feature.Component.COMMAND.addCommandHandler(new OnCommandGetNumOfNotWatched());
		
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
		String channelId = program.getChannel().getChannelId();
		NavigableMap<Calendar, Program> navMap = _channelToRecordsNavigableMap.get(channelId);
		if (navMap == null)
		{
			navMap = new TreeMap<Calendar, Program>();
			_channelToRecordsNavigableMap.put(channelId, navMap);
		}

		navMap.put(program.getStartTime(), program);
		int dayOffset = Calendars.getDayOffsetByDate(program.getStartTime());
		_dayOffsets.add(dayOffset);
		
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
	 * Set the parameter _isWatched of the program to true
	 * 
	 * @param program
	 * @return true if the record with its new value is saved to the repository
	 */
	public boolean setRecordWatched(Program program)
	{
		program.setIsWatched();
		
		
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

		return saveRecords();
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

	private class OnCommandRecord implements CommandHandler
	{
		@Override
        public void execute(Bundle params, final OnResultReceived onResultReceived)
		{
			Log.i(TAG, ".OnCommandRecord exec");

			String programId = params.getString(CommandRecordExtras.PROGRAM_ID.name());
			String channelId = params.getString(CommandRecordExtras.CHANNEL_ID.name());
			Log.i(TAG, ".OnCommandRecord.execute: channelId = " + channelId.toString() + ", programId = "
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
							addRecord(program);
						onResultReceived.onReceiveResult(FeatureError.OK(FeatureRecordingScheduler.this), null);
					}
				}
			});
		}

		@Override
		public String getId()
		{
			Log.i(TAG, "Command.POST_RECORDING_SECHEDULER.name();");
			return Command.RECORD.name();
		}
	}

	private class OnCommandGetRecordings implements CommandHandler
	{
		@Override
        public void execute(Bundle params, final OnResultReceived onResultReceived)
		{
			Log.i(TAG, ".OnCommandGetRecordings exec");
			final JSONArray jsonRecAllDayoffsets = new JSONArray();
			Iterator<Integer> recordedDayoffset = getRecordedDayOffsets();
			try
			{
				if (recordedDayoffset == null || !recordedDayoffset.hasNext())
				{
					Log.i(TAG, "nothing to be displayed from records");
					onResultReceived.onReceiveResult(FeatureError.OK(FeatureRecordingScheduler.this), null);
				}
				else
				{
					while (recordedDayoffset.hasNext())
					{
						final JSONObject jsonRecDayoffset = new JSONObject();
						int dayOffset = recordedDayoffset.next();
						JSONArray Programs = new JSONArray();

						Calendar shiftedDate = Calendars.getDateByDayOffset(dayOffset);
						SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
						String formatted = format.format(shiftedDate.getTime());
						System.out.println(formatted);
						jsonRecDayoffset.put("date", formatted);

						List<Program> programsInDayoffset = getRecordsByDate(dayOffset);
						for (Program program : programsInDayoffset)
						{
							JSONObject jsonProgram = new JSONObject();

							jsonProgram.put("program_id", program.getId());
							jsonProgram.put("channel_id", program.getChannel().getChannelId());
							jsonProgram.put("length", program.getLengthMin());
							jsonProgram.put("title", program.getTitle());
							jsonProgram.put("is_watched", program.getIsWatched());
							
							
							String description = program.getDetailAttribute(ProgramAttribute.DESCRIPTION);
							if (description != null)
								jsonProgram.put("description", description);
							jsonProgram.put("image", program.getDetailAttribute(ProgramAttribute.IMAGE));
							Programs.put(jsonProgram);
						}
						jsonRecDayoffset.put("programs", Programs);

						jsonRecAllDayoffsets.put(jsonRecDayoffset);
					}
					onResultReceived.onReceiveResult(FeatureError.OK(FeatureRecordingScheduler.this), jsonRecAllDayoffsets);

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
			Log.i(TAG, "Command.GET_RECORDINGS.name();");
			return Command.GET_RECORDINGS.name();
		}
	}
	
	
	private class OnCommandRemoveRecording implements CommandHandler
	{
		@Override
        public void execute(Bundle params, final OnResultReceived onResultReceived)
		{
			Log.i(TAG, ".OnCommandRemoveRecording exec");

			String channelId = params.getString(CommandRemoveRecordingExtras.CHANNEL_ID.name());
			String programId = params.getString(CommandRemoveRecordingExtras.PROGRAM_ID.name());
			
			_feature.Component.EPG.getProgramDetails(channelId, programId, new OnResultReceived()
			{
				@Override
				public void onReceiveResult(FeatureError error, Object object)
				{
					if (error.isError())
					{
						onResultReceived.onReceiveResult(error, null);
					}
					else
					{
						Program program = (Program) object;
						removeRecord(program);
						onResultReceived.onReceiveResult(FeatureError.OK(FeatureRecordingScheduler.this), null);
					}
				}
			});
		}

		@Override
		public String getId()
		{
			return Command.REMOVE_RECORDING.name();
		}
	}
	
	
	
	private class OnCommandSetRecordingWatched implements CommandHandler
	{
		@Override
        public void execute(Bundle params, final OnResultReceived onResultReceived)
		{
			Log.i(TAG, ".OnCommandSetRecordingWatched exec");

			String channelId = params.getString(CommandSetRecordingWatched.CHANNEL_ID.name());
			String programId = params.getString(CommandSetRecordingWatched.PROGRAM_ID.name());
			
			_feature.Component.EPG.getProgramDetails(channelId, programId, new OnResultReceived()
			{
				@Override
				public void onReceiveResult(FeatureError error, Object object)
				{
					try
					{
						Program program = (Program) object;
						setRecordWatched(program);
						onResultReceived.onReceiveResult(FeatureError.OK(FeatureRecordingScheduler.this), null);
					}
					catch(Error e)
					{
						Log.i(TAG , "Error:" + e);
						onResultReceived.onReceiveResult(error, null);
					}
				}
			});
		}

		@Override
		public String getId() 
		{
			return Command.SET_RECORDING_WATCHED.name();
		}
	}
	
	
	private class OnCommandGetNumOfNotWatched implements CommandHandler
	{
		@Override
        public void execute(Bundle params, final OnResultReceived onResultReceived)
		{
			Log.i(TAG, ".OnCommandGetNumOfNotWatched exec");
			Integer numOfNotWatched = 0;
			final JSONObject jsonNumOfUnwatched = new JSONObject();
			Iterator<Integer> recordedDayoffset = getRecordedDayOffsets();
			try
			{
				if (recordedDayoffset == null || !recordedDayoffset.hasNext())
				{
					Log.i(TAG, "nothing to be displayed from records");
					onResultReceived.onReceiveResult(FeatureError.OK(FeatureRecordingScheduler.this), null);
				}
				else
				{
					while (recordedDayoffset.hasNext())
					{
						int dayOffset = recordedDayoffset.next();

						List<Program> programsInDayoffset = getRecordsByDate(dayOffset);
						for (Program program : programsInDayoffset)
						{
							if(program.getIsWatched() == false)
								numOfNotWatched++;
						}
					}
					jsonNumOfUnwatched.put("numOfNotWatched",numOfNotWatched.toString());
					onResultReceived.onReceiveResult(FeatureError.OK(FeatureRecordingScheduler.this), jsonNumOfUnwatched);
				}
			}
			catch (JSONException e)
			{
				Log.e(TAG, e.getMessage(), e);
				onResultReceived.onReceiveResult(new FeatureError(FeatureRecordingScheduler.this,e), null);
			}
		}
		@Override
		public String getId() 
		{
			return Command.GET_NUM_OF_NOT_WATCHED.name();
		}
	}
				
}

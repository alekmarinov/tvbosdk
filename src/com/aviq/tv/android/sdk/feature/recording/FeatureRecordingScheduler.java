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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.os.Bundle;

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
import com.aviq.tv.android.sdk.feature.command.CommandHandler;
import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.epg.Program;
import com.aviq.tv.android.sdk.feature.epg.ProgramAttribute;
import com.aviq.tv.android.sdk.feature.epg.bulsat.ChannelBulsat;
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
	private Map<String, Boolean> _recordedPrograms = new HashMap<String, Boolean>();
	private Prefs _userPrefs;

	public static enum Command
	{
		RECORD, GET_RECORDINGS, REMOVE_RECORDING, GET_NUM_OF_NOT_WATCHED
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

	private class RecordingItem
	{
		String channelId;
		String programId;
		boolean watched;
	}

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

		if (!verifyRecordingsIntegrity())
		{
			Log.e(TAG, "Clear user recordings due to invalid integrity.");
			_userPrefs.remove(UserParam.RECORDINGS);
		}

		List<RecordingItem> recordings = getUserRecordings();
		for (RecordingItem recording : recordings)
		{
			_recordedPrograms.put(makeRecordingId(recording.channelId, recording.programId), recording.watched);
		}

		_feature.Component.COMMAND.addCommandHandler(new OnCommandRecord());
		_feature.Component.COMMAND.addCommandHandler(new OnCommandGetRecordings());
		_feature.Component.COMMAND.addCommandHandler(new OnCommandRemoveRecording());
		_feature.Component.COMMAND.addCommandHandler(new OnCommandGetNumOfNotWatched());

		super.initialize(onFeatureInitialized);
	}

	private boolean verifyRecordingsIntegrity()
	{
		if (!_userPrefs.has(UserParam.RECORDINGS))
		{
			return true;
		}
		String recordingSetting = _userPrefs.getString(UserParam.RECORDINGS);
		String[] recordings = recordingSetting.split(RECORD_DELIMITER);
		for (String recording : recordings)
		{
			String[] items = recording.split(ITEM_DELIMITER);
			if (items.length != 3)
			{
				Log.e(TAG,
				        "Invalid recording format! Expected channelId(String),programId(String),watched(true|false) + got "
				                + recording);
				return false;
			}

			String programId = items[1];

			if (programId.length() != 14)
			{
				Log.e(TAG, "Invalid recording format! Expected 14 chars for programId, got + " + programId.length());
				return false;
			}

			if (!programId.matches("[0-9]+"))
			{
				Log.e(TAG, "Invalid recording format! Expected only numbers for programId, got `" + programId + "'");
				return false;
			}

			String watched = items[2];
			if (!(watched.equals("true") || watched.equals("false")))
			{
				Log.e(TAG, "Invalid recording format! Expected true or false for watched, got `" + watched + "'");
				return false;
			}
		}
		return true;
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
	 */
	public void addRecord(Program program)
	{
		String channelId = program.getChannel().getChannelId();
		addRecord(channelId, program.getId());
	}

	/**
	 * Add new program recording
	 *
	 * @param channelId
	 * @param programId
	 */
	public void addRecord(String channelId, String programId)
	{
		_recordedPrograms.put(makeRecordingId(channelId, programId), Boolean.FALSE);
		saveRecordings();
	}

	private String serializeRecordings()
	{
		StringBuffer sb = new StringBuffer();
		for (String key : _recordedPrograms.keySet())
		{
			if (sb.length() > 0)
				sb.append(RECORD_DELIMITER);
			boolean watched = _recordedPrograms.get(key);
			String[] parts = key.split("_");
			sb.append(parts[0]).append(ITEM_DELIMITER).append(parts[1]).append(ITEM_DELIMITER).append(watched);
		}
		return sb.toString();
	}

	private void saveRecordings()
	{
		_userPrefs.put(UserParam.RECORDINGS, serializeRecordings());
	}

	/**
	 * Remove schedule record for given program
	 *
	 * @param program
	 */
	public void removeRecord(Program program)
	{
		removeRecord(program.getChannel().getChannelId(), program.getId());
	}

	/**
	 * Remove schedule record for given program
	 *
	 * @param channelId
	 * @param programId
	 */
	public void removeRecord(String channelId, String programId)
	{
		_recordedPrograms.remove(makeRecordingId(channelId, programId));
		saveRecordings();
	}

	/**
	 * Return all programs for recording by date
	 *
	 * @param dateOffset
	 *            - offset to current day, ex: 0 - current day, +1 (next day)
	 */
	/*
	 * public List<Program> getRecordsByDate(int dateOffset)
	 * {
	 * List<Program> programs = new ArrayList<Program>();
	 * Calendar date = Calendars.getDateByDayOffsetFrom(
	 * Calendar.getInstance(_feature.Component.TIMEZONE.getTimeZone()),
	 * dateOffset);
	 * // String strDate = String.format("%04d%02d%02d",
	 * // date.get(Calendar.YEAR), 1 + date.get(Calendar.MONTH),
	 * // date.get(Calendar.DAY_OF_MONTH));
	 * Log.d(TAG, ".getRecordsByDate: dateOffset = " + dateOffset + " -> " +
	 * Calendars.makeString(date));
	 * for (NavigableMap<Calendar, Program> map :
	 * _channelToRecordsNavigableMap.values())
	 * {
	 * for (Program program : map.values())
	 * {
	 * Calendar programTime = program.getStartTime();
	 * Log.d(TAG,
	 * "Comparing entry offset " + Calendars.makeString(programTime) + " - "
	 * + Calendars.getDayOffsetByDate(programTime) + " with dateOffset = " +
	 * dateOffset);
	 * if (Calendars.getDayOffsetByDate(programTime) == dateOffset)
	 * {
	 * programs.add(program);
	 * }
	 * }
	 * }
	 * Collections.sort(programs, _recordingSchedulerComparator);
	 * if (programs.size() > 0)
	 * {
	 * Log.d(TAG, "program from getRecordsByDate " +
	 * programs.get(0).getTitle());
	 * }
	 * else
	 * {
	 * Log.d(TAG, "no programs from getRecordsByDate with dateOffset = " +
	 * dateOffset);
	 * }
	 * return programs;
	 * }
	 */

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
		return _recordedPrograms.containsKey(makeRecordingId(program.getChannel().getChannelId(), program.getId()));
	}

	/**
	 * Set recording to status watched
	 *
	 * @param program
	 */
	public void setRecordWatched(Program program)
	{
		setRecordWatched(program.getChannel().getChannelId(), program.getId());
	}

	/**
	 * Set recording to status watched
	 *
	 * @param channelId
	 * @param programId
	 */
	public void setRecordWatched(String channelId, String programId)
	{
		_recordedPrograms.put(makeRecordingId(channelId, programId), Boolean.TRUE);
		saveRecordings();
	}

	/**
	 * @param program
	 * @return true if the specified program has been watched
	 */
	public boolean isRecordWatched(Program program)
	{
		Boolean watched = _recordedPrograms.get(makeRecordingId(program.getChannel().getChannelId(), program.getId()));
		return watched != null && watched.booleanValue();
	}

	private String makeRecordingId(String channelId, String programId)
	{
		return channelId + "_" + programId;
	}

	private List<RecordingItem> getUserRecordings()
	{
		List<RecordingItem> recordings = new ArrayList<RecordingItem>();
		if (_userPrefs.has(UserParam.RECORDINGS))
		{
			String records = _userPrefs.getString(UserParam.RECORDINGS);
			String[] recordParts = records.split(RECORD_DELIMITER);
			for (String record : recordParts)
			{
				RecordingItem recordingItem = new RecordingItem();
				String[] items = record.split(ITEM_DELIMITER);
				recordingItem.channelId = items[0];
				recordingItem.programId = items[1];
				recordingItem.watched = Boolean.parseBoolean(items[2]);
				recordings.add(recordingItem);
			}
		}
		return recordings;
	}

	public boolean haveRecordings()
	{
		return !_recordedPrograms.isEmpty();
	}

	/**
	 * Load all recorded programs
	 *
	 * @param onResultReceived
	 */
	public void loadRecordings(final OnResultReceived onResultReceived)
	{
		final List<Program> programs = new ArrayList<Program>();
		if (!haveRecordings())
		{
			onResultReceived.onReceiveResult(FeatureError.OK(this), programs);
			return;
		}

		List<String> channelIds = new ArrayList<String>();
		List<String> programIds = new ArrayList<String>();

		for (RecordingItem rec : getUserRecordings())
		{
			channelIds.add(rec.channelId);
			programIds.add(rec.programId);
		}

		_feature.Component.EPG.getMultiplePrograms(channelIds, programIds, onResultReceived);
	}

	/**
	 * Load all recorded programs
	 *
	 * @param onResultReceived
	 */
	public void loadRecordingsByDay(int dayOffset, final OnResultReceived onResultReceived)
	{
		final List<Program> programs = new ArrayList<Program>();
		if (!haveRecordings())
		{
			onResultReceived.onReceiveResult(FeatureError.OK(this), programs);
			return;
		}

		List<String> channelIds = new ArrayList<String>();
		List<String> programIds = new ArrayList<String>();

		for (RecordingItem rec : getUserRecordings())
		{
			Calendar startTime = Program.timeById(rec.programId);
			if (Calendars.getDayOffsetByDate(startTime) == dayOffset)
			{
				channelIds.add(rec.channelId);
				programIds.add(rec.programId);
			}
		}

		_feature.Component.EPG.getMultiplePrograms(channelIds, programIds, onResultReceived);
	}

	private class OnCommandRecord implements CommandHandler
	{
		@Override
		public void execute(Bundle params, final OnResultReceived onResultReceived)
		{
			Log.i(TAG, ".OnCommandRecord exec");

			String programId = params.getString(CommandRecordExtras.PROGRAM_ID.name());
			String channelId = params.getString(CommandRecordExtras.CHANNEL_ID.name());
			Log.i(TAG,
			        ".OnCommandRecord.execute: channelId = " + channelId.toString() + ", programId = "
			                + programId.toString());

			addRecord(channelId, programId);
		}

		@Override
		public String getId()
		{
			return Command.RECORD.name();
		}
	}

	private class OnCommandGetRecordings implements CommandHandler
	{
		private final String DATETIME_FORMAT = "yyyyMMddHHmmss";

		@Override
		public void execute(Bundle params, final OnResultReceived onResultReceived)
		{
			Log.i(TAG, ".OnCommandGetRecordings exec");

			loadRecordings(new OnResultReceived()
			{
				@Override
				public void onReceiveResult(FeatureError error, Object object)
				{
					if (!error.isError())
					{
						onResultReceived.onReceiveResult(error, null);
					}
					else
					{
						List<String> channelIds = new ArrayList<String>();
						List<String> programIds = new ArrayList<String>();
						for (RecordingItem rec : getUserRecordings())
						{
							channelIds.add(rec.channelId);
							programIds.add(rec.programId);
						}
						_feature.Component.EPG.getMultiplePrograms(channelIds, programIds, new OnResultReceived()
						{
							@Override
							public void onReceiveResult(FeatureError error, Object object)
							{
								@SuppressWarnings("unchecked")
								List<Program> programs = (List<Program>) object;

								Map<String, List<Program>> programsDateMap = new HashMap<String, List<Program>>();
								for (Program program : programs)
								{
									SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
									String formattedTime = format.format(program.getStartTime());
									List<Program> programsDate = programsDateMap.get(formattedTime);
									if (programsDate == null)
									{
										programsDate = new ArrayList<Program>();
										programsDateMap.put(formattedTime, programsDate);
									}
									programsDate.add(program);
								}
								try
								{
									final JSONArray jsonRecordingDates = new JSONArray();
									for (String formattedTime : programsDateMap.keySet())
									{
										JSONObject jsonRecordings = new JSONObject();
										jsonRecordings.put("date", formattedTime);
										JSONArray jsonPrograms = new JSONArray();
										for (Program program : programsDateMap.get(formattedTime))
										{
											JSONObject jsonProgram = new JSONObject();
											jsonProgram.put("program_id", program.getId());
											jsonProgram.put("channel_id", program.getChannel().getChannelId());

											Channel channel = program.getChannel();
											if (channel instanceof ChannelBulsat)
												jsonProgram.put(
												        "channel_thumbnail",
												        "data:image/png;base64,"
												                + channel
												                        .getChannelImageBase64(ChannelBulsat.LOGO_SELECTED));
											else
												jsonProgram.put(
												        "channel_thumbnail",
												        "data:image/png;base64,"
												                + channel.getChannelImageBase64(Channel.LOGO_NORMAL));

											jsonProgram.put("start",
											        Calendars.makeString(program.getStartTime(), DATETIME_FORMAT));

											jsonProgram.put("length", program.getLengthMin());
											jsonProgram.put("title", program.getTitle());
											jsonProgram.put("is_watched", isRecordWatched(program));

											String description = program
											        .getDetailAttribute(ProgramAttribute.DESCRIPTION);
											if (description != null)
												jsonProgram.put("description", description);
											jsonProgram.put("image", program.getDetailAttribute(ProgramAttribute.IMAGE));
											jsonPrograms.put(jsonProgram);
										}
										jsonRecordings.put("programs", jsonPrograms);
										jsonRecordingDates.put(jsonRecordings);
									}
									onResultReceived.onReceiveResult(FeatureError.OK(FeatureRecordingScheduler.this),
									        jsonRecordingDates);
								}
								catch (JSONException e)
								{
									onResultReceived.onReceiveResult(
									        new FeatureError(FeatureRecordingScheduler.this, e), null);
								}
							}
						});
					}
				}
			});
		}

		@Override
		public String getId()
		{
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

			removeRecord(channelId, programId);
		}

		@Override
		public String getId()
		{
			return Command.REMOVE_RECORDING.name();
		}
	}

	private class OnCommandGetNumOfNotWatched implements CommandHandler
	{
		@Override
		public void execute(Bundle params, final OnResultReceived onResultReceived)
		{
			Log.i(TAG, ".OnCommandGetNumOfNotWatched exec");
			int numOfNotWatched = 0;
			for (RecordingItem rec : getUserRecordings())
			{
				if (!rec.watched)
					numOfNotWatched++;
			}

			final JSONObject jsonNumOfUnwatched = new JSONObject();
			try
			{
				jsonNumOfUnwatched.put("numOfNotWatched", numOfNotWatched);
				onResultReceived.onReceiveResult(FeatureError.OK(FeatureRecordingScheduler.this), jsonNumOfUnwatched);
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
			return Command.GET_NUM_OF_NOT_WATCHED.name();
		}
	}
}

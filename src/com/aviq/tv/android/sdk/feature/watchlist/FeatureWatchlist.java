/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    Watchlist.java
 * Author:      alek
 * Date:        1 Dec 2013
 * Description: Component feature managing programs watchlist
 */

package com.aviq.tv.android.sdk.feature.watchlist;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.os.Bundle;
import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.feature.epg.EpgData;
import com.aviq.tv.android.sdk.feature.epg.Program;
import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Component feature managing programs watchlist
 */
public class FeatureWatchlist extends FeatureComponent
{
	public static final String TAG = FeatureWatchlist.class.getSimpleName();
	public static final int ON_PROGRAM_ADDED = EventMessenger.ID("ON_PROGRAM_ADDED");
	public static final int ON_PROGRAM_REMOVED = EventMessenger.ID("ON_PROGRAM_REMOVED");
	public static final int ON_PROGRAM_NOTIFY = EventMessenger.ID("ON_PROGRAM_NOTIFY");

	public enum UserParam
	{
		/**
		 * List of program identifier keys formatted as
		 * <channel_id>/<program_id>
		 */
		WATCHLIST
	}

	public enum Param
	{
		/**
		 * The time in seconds to notify before the program starts
		 */
		NOTIFY_EARLIER(300);

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.WATCHLIST).put(name(), value);
		}
	}

	private ArrayList<Program> _watchedPrograms = new ArrayList<Program>();
	private Prefs _userPrefs;
	private int _notifyEarlier;

	public FeatureWatchlist()
	{
		_dependencies.Schedulers.add(FeatureName.Scheduler.EPG);
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		_userPrefs = Environment.getInstance().getUserPrefs();
		_watchedPrograms = loadWatchlist(_feature.Scheduler.EPG.getEpgData());
		_notifyEarlier = getPrefs().getInt(Param.NOTIFY_EARLIER);
		updateProgramStartNotification();
		onFeatureInitialized.onInitialized(this, ResultCode.OK);
	}

	/**
	 * Add program to watchlist
	 *
	 * @param program
	 */
	public void addWatchlist(Program program)
	{
		if (!isWatched(program))
		{
			_watchedPrograms.add(program);

			Collections.sort(_watchedPrograms, new Comparator<Program>()
			{
				@Override
				public int compare(Program lhs, Program rhs)
				{
					return lhs.getStartTimeCalendar().compareTo(rhs.getStartTimeCalendar());
				}
			});
			updateProgramStartNotification();
			Bundle bundle = new Bundle();
			bundle.putString("PROGRAM", program.getId());
			bundle.putString("CHANNEL", program.getChannel().getChannelId());
			getEventMessenger().trigger(ON_PROGRAM_ADDED, bundle);
			saveWatchlist(_watchedPrograms);
		}
	}

	/**
	 * Remove program from the watchlist
	 *
	 * @param program
	 */
	public void removeWatchlist(Program program)
	{
		if (_watchedPrograms.remove(program))
		{
			Bundle bundle = new Bundle();
			bundle.putString("PROGRAM", program.getId());
			bundle.putString("CHANNEL", program.getChannel().getChannelId());
			getEventMessenger().trigger(ON_PROGRAM_REMOVED, bundle);
			updateProgramStartNotification();
			saveWatchlist(_watchedPrograms);
		}
	}

	/**
	 * Check if program is added to the watchlist
	 *
	 * @param program
	 * @return true if the program is watched
	 */
	public boolean isWatched(Program program)
	{
		for (Program watchedProgram : _watchedPrograms)
		{
			if (program.equals(watchedProgram))
				return true;
		}
		return false;
	}

	/**
	 * Check if program is expired and can't be watched
	 *
	 * @param program
	 * @return true if the program is expired
	 */
	public boolean isExpired(Program program)
	{
		return Calendar.getInstance().compareTo(program.getStartTimeCalendar()) > 0;
	}

	public void enableNotifications()
	{

	}

	/**
	 * @return list of programs whatchlist
	 */
	public ArrayList<Program> getWatchedPrograms()
	{
		return _watchedPrograms;
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.WATCHLIST;
	}

	// Saves programs list to watchlist settings
	private void saveWatchlist(List<Program> programs)
	{
		StringBuffer buffer = new StringBuffer();
		for (Program program : programs)
		{
			if (buffer.length() > 0)
				buffer.append(',');
			buffer.append(program.getChannel().getChannelId());
			buffer.append('/');
			buffer.append(program.getId());
		}
		_userPrefs.put(UserParam.WATCHLIST, buffer.toString());
	}

	// Loads programs list from watchlist settings
	private ArrayList<Program> loadWatchlist(EpgData epgData)
	{
		ArrayList<Program> programs = new ArrayList<Program>();
		if (epgData == null || !_userPrefs.has(UserParam.WATCHLIST))
			return programs;
		String buffer = _userPrefs.getString(UserParam.WATCHLIST);
		String[] programIds = buffer.split(",");
		for (String programId : programIds)
		{
			String[] idElements = programId.split("/");
			if (idElements.length != 2)
			{
				Log.w(TAG, "Invalid program ID saved in the watchlist: " + programId);
			}
			else
			{
				String chId = idElements[0];
				String prId = idElements[1];
				Program program = epgData.getProgram(chId, prId);
				if (program == null)
				{
					Log.w(TAG, "Program " + prId + " not found in channel " + chId);
					continue;
				}
				programs.add(program);
			}
		}
		return programs;
	}

	private void updateProgramStartNotification()
	{
		// remove expired programs
		List<Program> expiredPrograms = new ArrayList<Program>();
		for (Program program : _watchedPrograms)
		{
			if (isExpired(program))
			{
				// program expired
				expiredPrograms.add(program);
			}
		}
		for (Program program : expiredPrograms)
			removeWatchlist(program);

		if (_watchedPrograms.size() > 0)
			notifyProgram(_watchedPrograms.get(0));
		else
			Log.i(TAG, "No program for notification");
	}

	private void notifyProgram(Program program)
	{
		getEventMessenger().removeCallbacks(_onProgramNotification);
		_onProgramNotification.NotifyProgram = program;
		long now = Calendar.getInstance().getTimeInMillis();
		long delayToNotify = program.getStartTimeCalendar().getTimeInMillis() - now - 1000 * _notifyEarlier;
		if (delayToNotify < 0)
			delayToNotify = 5 * 1000;

		getEventMessenger().postDelayed(_onProgramNotification, delayToNotify);
		Log.i(TAG, "Schedule for notification program " + program.getChannel().getChannelId() + "/" + program.getId()
		        + " " + program.getTitle() + " in " + delayToNotify + " ms");
	}

	private ProgramNotifier _onProgramNotification = new ProgramNotifier();

	private class ProgramNotifier implements Runnable
	{
		Program NotifyProgram;

		@Override
		public void run()
		{
			Bundle bundle = new Bundle();
			bundle.putString("PROGRAM", NotifyProgram.getId());
			bundle.putString("CHANNEL", NotifyProgram.getChannel().getChannelId());
			Log.i(TAG, "Trigger ON_PROGRAM_NOTIFY: " + TextUtils.implodeBundle(bundle));
			getEventMessenger().trigger(ON_PROGRAM_NOTIFY, bundle);
			removeWatchlist(NotifyProgram);
			updateProgramStartNotification();
		}
	}
}

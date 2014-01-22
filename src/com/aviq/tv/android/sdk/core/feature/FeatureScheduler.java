/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureScheduler.java
 * Author:      alek
 * Date:        1 Dec 2013
 * Description: Defines the base class for scheduler feature type
 */

package com.aviq.tv.android.sdk.core.feature;

import java.util.Calendar;

import android.os.Bundle;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.ResultCode;

/**
 * Defines the base class for scheduler feature type
 *
 */
public abstract class FeatureScheduler implements IFeature
{
	private static final String TAG = FeatureScheduler.class.getSimpleName();

	public static final int ON_SCHEDULE = EventMessenger.ID();
	public static final int ON_SCHEDULE_FINISHED = EventMessenger.ID();

	protected FeatureSet _dependencies = new FeatureSet();
	private Calendar _scheduledTime;

	public FeatureScheduler()
	{
		getEventMessenger().register(this, ON_SCHEDULE);
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		onFeatureInitialized.onInitialized(this, ResultCode.OK);
	}

	protected void onSchedule(OnFeatureInitialized onFeatureInitialized)
	{
		onFeatureInitialized.onInitialized(this, ResultCode.OK);
	}

	@Override
	public FeatureSet dependencies()
	{
		return _dependencies;
	}

	@Override
	public Type getType()
	{
		return IFeature.Type.SCHEDULER;
	}

	@Override
    public String getName()
	{
		return getSchedulerName().toString();
	}

	@Override
    public Prefs getPrefs()
	{
		return Environment.getInstance().getFeaturePrefs(getSchedulerName());
	}

	/**
	 * @return an event messenger associated with this feature
	 */
	@Override
	public EventMessenger getEventMessenger()
	{
		return Environment.getInstance().getEventMessenger();
	}

	@Override
    public void onEvent(int msgId, Bundle bundle)
	{
		if (ON_SCHEDULE == msgId)
		{
			onSchedule(new OnFeatureInitialized()
			{
				@Override
				public void onInitialized(IFeature feature, int resultCode)
				{
					Log.i(TAG, ".onInitialized: feature = " + feature + ", resultCode = " + resultCode);
					getEventMessenger().trigger(ON_SCHEDULE_FINISHED);
				}

				@Override
				public void onInitializeProgress(IFeature feature, float progress)
				{
					Log.i(TAG, ".onInitializeProgress: feature = " + feature + ", progress = " + progress);
				}
			});
		}
	}

	public void scheduleDelayed(int delayMs)
	{
		_scheduledTime = Calendar.getInstance();
		_scheduledTime.setTimeInMillis(_scheduledTime.getTimeInMillis() + delayMs);
		getEventMessenger().trigger(ON_SCHEDULE, delayMs);
	}

	public Calendar getScheduledTime()
	{
		return _scheduledTime;
	}

	public abstract FeatureName.Scheduler getSchedulerName();
}

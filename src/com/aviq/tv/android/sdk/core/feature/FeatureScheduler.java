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
import com.aviq.tv.android.sdk.core.EventReceiver;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Defines the base class for scheduler feature type
 */
public abstract class FeatureScheduler implements IFeature, EventReceiver
{
	private static final String TAG = FeatureScheduler.class.getSimpleName();

	public static final int ON_SCHEDULE = EventMessenger.ID("ON_SCHEDULE");
	public static final int ON_SCHEDULE_FINISHED = EventMessenger.ID("ON_SCHEDULE_FINISHED");

	protected Features _feature;
	private FeatureSet _dependencies = new FeatureSet();
	private Calendar _scheduledTime;
	private EventMessenger _eventMessenger = new EventMessenger(getClass().getSimpleName());

	public FeatureScheduler()
	{
		getEventMessenger().register(this, ON_SCHEDULE);
	}

	/**
	 * Declare this feature depends on the specified component name
	 * @param featureName
	 * @throws FeatureNotFoundException
	 */
    protected final void require(FeatureName.Component featureName) throws FeatureNotFoundException
	{
		_dependencies.Components.add(featureName);
	}

	/**
	 * Declare this feature depends on the specified scheduler name
	 * @param featureName
	 * @throws FeatureNotFoundException
	 */
	protected final void require(FeatureName.Scheduler featureName) throws FeatureNotFoundException
	{
		_dependencies.Schedulers.add(featureName);
	}

	/**
	 * Declare this feature depends on the specified state name
	 * @param featureName
	 * @throws FeatureNotFoundException
	 */
    protected final void require(FeatureName.State featureName) throws FeatureNotFoundException
	{
		_dependencies.States.add(featureName);
	}

	/**
	 * Declare this feature depends on the specified special feature
	 * @param featureName
	 */
    protected final void require(Class<?> featureClass)
	{
		_dependencies.Specials.add(featureClass);
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		if (onFeatureInitialized != null)
			onFeatureInitialized.onInitialized(this, ResultCode.OK);
	}

	protected void onSchedule(OnFeatureInitialized onFeatureInitialized)
	{
		onFeatureInitialized.onInitialized(this, ResultCode.OK);
	}

	@Override
	public final FeatureSet dependencies()
	{
		return _dependencies;
	}

	@Override
	public final void setDependencyFeatures(Features features)
	{
		_feature = features;
	}

	@Override
	public final Type getType()
	{
		return IFeature.Type.SCHEDULER;
	}

	@Override
	public final String getName()
	{
		FeatureName.Scheduler name = getSchedulerName();
		if (FeatureName.Scheduler.SPECIAL.equals(name))
			return getClass().getName();
		else
			return name.toString();
	}

	@Override
	public String toString()
	{
		return getType() + " " + getName();
	}

	@Override
	public final Prefs getPrefs()
	{
		return Environment.getInstance().getFeaturePrefs(getSchedulerName());
	}

	/**
	 * @return an event messenger associated with this feature
	 */
	@Override
	public final EventMessenger getEventMessenger()
	{
		return _eventMessenger;
	}

	@Override
	public void onEvent(int msgId, Bundle bundle)
	{
		Log.i(getName(), ".onEvent: " + EventMessenger.idName(msgId) + TextUtils.implodeBundle(bundle));
		if (ON_SCHEDULE == msgId)
		{
			onSchedule(new OnFeatureInitialized()
			{
				@Override
				public void onInitialized(IFeature feature, int resultCode)
				{
					Log.i(TAG, "onSchedule.onInitialized: feature = " + feature + ", resultCode = " + resultCode);
					getEventMessenger().trigger(ON_SCHEDULE_FINISHED);
				}

				@Override
				public void onInitializeProgress(IFeature feature, float progress)
				{
					Log.i(TAG, "onSchedule.onInitializeProgress: feature = " + feature + ", progress = " + progress);
				}
			});
		}
	}

	public final void scheduleDelayed(int delayMs)
	{
		Log.i(getClass().getName(), ".scheduleDelayed: delayMs = " + delayMs);
		_scheduledTime = Calendar.getInstance();
		_scheduledTime.setTimeInMillis(_scheduledTime.getTimeInMillis() + delayMs);
		getEventMessenger().trigger(ON_SCHEDULE, delayMs);
	}

	public final Calendar getScheduledTime()
	{
		return _scheduledTime;
	}

	public abstract FeatureName.Scheduler getSchedulerName();
}

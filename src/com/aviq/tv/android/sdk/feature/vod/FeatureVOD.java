package com.aviq.tv.android.sdk.feature.vod;

import android.util.Log;

import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Scheduler;
import com.aviq.tv.android.sdk.core.feature.FeatureScheduler;

public abstract class FeatureVOD extends FeatureScheduler
{
	public static final String TAG = FeatureVOD.class.getSimpleName();
	
	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		super.initialize(onFeatureInitialized);
	}

	@Override
	protected void onSchedule(OnFeatureInitialized onFeatureInitialized)
	{
		super.onSchedule(onFeatureInitialized);
	}
	
	@Override
	public Scheduler getSchedulerName()
	{
		return FeatureName.Scheduler.VOD;
	}
	
	public abstract <T> T getVodData();
}

/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureVOD.java
 * Author:      zhelyazko
 * Date:        3 Feb 2014
 * Description: Feature providing VOD data
 */
package com.aviq.tv.android.sdk.feature.vod;

import java.util.List;

import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Scheduler;
import com.aviq.tv.android.sdk.core.feature.FeatureScheduler;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;
import com.aviq.tv.android.sdk.feature.vod.bulsat_v1.Vod;


/**
 * Feature providing VOD data
 */
@Author("zhelyazko")
public abstract class FeatureVODV1 extends FeatureScheduler
{
	public static final String TAG = FeatureVODV1.class.getSimpleName();

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

	public abstract <T> T getVodData(boolean removeEmptyElements);
	public abstract void loadVod(String id, OnVodLoaded onVodLoadedListener);
	public abstract void search(String term, OnVodSearchResult onVodSearchResult);

	public static interface OnVodLoaded
	{
		public void onVodLoaded(Vod vod);
		public void onVodError(Exception error);
	}

	public static interface OnVodSearchResult<E>
	{
		public void onVodSearchResult(List<E> resultList);
	}
}

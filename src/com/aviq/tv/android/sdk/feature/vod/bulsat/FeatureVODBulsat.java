/**
 * Copyright (c) 2007-2015, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureVODBulsat.java
 * Author:      alek
 * Date:        19 Jan 2015
 * Description: VOD feature scheduler for Bulsatcom provider
 */

package com.aviq.tv.android.sdk.feature.vod.bulsat;

import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.feature.vod.FeatureVOD;
import com.aviq.tv.android.sdk.feature.vod.VODGroup;
import com.aviq.tv.android.sdk.feature.vod.VODItem;

/**
 * VOD feature scheduler for Bulsatcom provider
 */
public class FeatureVODBulsat extends FeatureVOD
{

	public FeatureVODBulsat() throws FeatureNotFoundException
	{
	}

	@Override
	protected VODGroup createVodGroup(String id, String title, VODGroup parent)
	{
		return new VodGroupBulsat(id, title, parent);
	}

	@Override
	protected VODItem createVodItem(String id, String title, VODGroup parent, String poster)
	{
		return new VodItemBulsat(id, title, parent, poster);
	}

	@Override
	public <T> T getVodData(boolean removeEmptyElements)
	{
		return null;
	}

	@Override
	public void loadVod(String id, OnVodLoaded onVodLoadedListener)
	{
	}

	@Override
	public void search(String term, OnVodSearchResult onVodSearchResult)
	{
	}
}

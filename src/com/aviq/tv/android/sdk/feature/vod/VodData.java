/**
 * Copyright (c) 2007-2015, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    VodData.java
 * Author:      alek
 * Date:        19 Jan 2015
 * Description: VOD groups and items loaded in memory
 */

package com.aviq.tv.android.sdk.feature.vod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VOD groups and items loaded in memory
 */
public class VodData implements IVodData
{
	private List<VODGroup> _vodGroups;
	private Map<String, VODGroup> _vodGroupsMap;
	private Map<VODGroup, List<VODItem>> _vodGroupItems;

	public VodData(List<VODGroup> vodGroups)
	{
		_vodGroups = vodGroups;
		_vodGroupsMap = new HashMap<String, VODGroup>();
		for (VODGroup vodGroup : vodGroups)
			_vodGroupsMap.put(vodGroup.getId(), vodGroup);
	}

	public void setVodGroupItems(Map<VODGroup, List<VODItem>> vodGroupItems)
	{
		_vodGroupItems = vodGroupItems;
	}

	@Override
	public VODGroup getVodGroupById(String id)
	{
		return _vodGroupsMap.get(id);
	}

	@Override
	public List<VODGroup> getVodGroups(VODGroup vodGroup)
	{
		List<VODGroup> vodGroups = new ArrayList<VODGroup>();
		for (VODGroup group: _vodGroups)
		{
			if (group.getParent() == vodGroup)
				vodGroups.add(group);
		}
		return vodGroups;
	}

	@Override
	public List<VODItem> getVodItems(VODGroup vodGroup)
	{
		return _vodGroupItems.get(vodGroup);
	}
}

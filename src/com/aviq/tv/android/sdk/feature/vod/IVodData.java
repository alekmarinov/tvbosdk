/**
 * Copyright (c) 2007-2015, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    IVodData.java
 * Author:      alek
 * Date:        19 Jan 2015
 * Description: Vod groups and items access interface
 */

package com.aviq.tv.android.sdk.feature.vod;

import java.util.List;

/**
 * Vod groups and items access interface
 */
public interface IVodData
{
	VODGroup getVodGroupById(String id);
	VODItem getVodItemById(String id);
	List<VODGroup> getVodGroups(VODGroup vodGroup);
	List<VODItem> getVodItems(VODGroup vodGroup);
}

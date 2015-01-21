/**
 * Copyright (c) 2007-2015, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    VodItemBulsat.java
 * Author:      alek
 * Date:        19 Jan 2015
 * Description: Encapsulates VOD item attributes for Bulsatcom provider
 */

package com.aviq.tv.android.sdk.feature.vod.bulsat;

import com.aviq.tv.android.sdk.feature.vod.VODGroup;
import com.aviq.tv.android.sdk.feature.vod.VODItem;

/**
 * Encapsulates VOD item attributes for Bulsatcom provider
 */
public class VodItemBulsat extends VODItem
{
	public VodItemBulsat(String id, String title, VODGroup parent, String poster)
	{
		super(id, title, parent, poster);
	}

	@Override
    public void setAttributes(com.aviq.tv.android.sdk.feature.vod.VODGroup.MetaData metaData, String[] attributes)
    {
    }
}

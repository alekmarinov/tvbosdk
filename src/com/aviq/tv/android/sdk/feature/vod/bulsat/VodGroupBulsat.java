/**
 * Copyright (c) 2007-2015, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    VodGroupBulsat.java
 * Author:      alek
 * Date:        19 Jan 2015
 * Description: Encapsulates VOD group attributes for Bulsatcom provider
 */

package com.aviq.tv.android.sdk.feature.vod.bulsat;

import com.aviq.tv.android.sdk.feature.vod.VODGroup;

/**
 * Encapsulates VOD group attributes for Bulsatcom provider
 */
public class VodGroupBulsat extends VODGroup
{

	public VodGroupBulsat(String id, String title, VODGroup parent)
	{
		super(id, title, parent);
	}

	@Override
	public String toString()
	{
		return _title;
	}

	@Override
	public void setAttributes(MetaData metaData, String[] attributes)
	{
	}
}

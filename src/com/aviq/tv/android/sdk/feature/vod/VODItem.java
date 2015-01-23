/**
 * Copyright (c) 2007-2015, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    VODItem.java
 * Author:      alek
 * Date:        19 Jan 2015
 * Description: Encapsulates VOD item
 */

package com.aviq.tv.android.sdk.feature.vod;

/**
 * Encapsulates VOD item
 */
public abstract class VODItem extends VODGroup
{
	public static class MetaData extends VODGroup.MetaData
	{
	}

	public VODItem(String id, String title, VODGroup parent)
	{
		super(id, title, parent);
	}

	public abstract String getPoster();
	public abstract String getSourceUrl();
}

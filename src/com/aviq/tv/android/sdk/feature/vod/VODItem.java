/**
 * Copyright (c) 2007-2015, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    VODItem.java
 * Author:      alek
 * Date:        19 Jan 2015
 * Description: Encapsulates VOD item attributes
 */

package com.aviq.tv.android.sdk.feature.vod;

/**
 * Encapsulates VOD item attributes
 */
public abstract class VODItem extends VODGroup
{
	protected String _poster;

	public static class MetaData extends VODGroup.MetaData
	{
		public int metaVodItemPoster;
	}

	public VODItem(String id, String title, VODGroup parent, String poster)
	{
		super(id, title, parent);
		_poster = poster;
	}

    public String getPoster()
	{
		return _poster;
	}
}

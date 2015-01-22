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
	protected String _posterSmall;
	protected String _posterMedium;
	protected String _posterLarge;

	public static enum PosterSize
	{
		SMALL, MEDIUM, LARGE
	}

	public static class MetaData extends VODGroup.MetaData
	{
		public int metaVodItemPosterSmall;
		public int metaVodItemPosterMedium;
		public int metaVodItemPosterLarge;
	}

	public VODItem(String id, String title, VODGroup parent)
	{
		super(id, title, parent);
	}

	public void setPoster(PosterSize posterSize, String poster)
	{
		switch (posterSize)
		{
			case SMALL: _posterSmall = poster; break;
			case MEDIUM: _posterMedium = poster; break;
			case LARGE: _posterLarge = poster; break;
		}
	}

    public String getPoster(PosterSize posterSize)
	{
		switch (posterSize)
		{
			case SMALL: return _posterSmall;
			case MEDIUM: return _posterMedium;
			case LARGE: return _posterLarge;
		}
		return null;
	}
}

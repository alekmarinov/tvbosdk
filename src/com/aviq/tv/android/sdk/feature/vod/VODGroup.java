/**
 * Copyright (c) 2007-2015, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    VODGroup.java
 * Author:      alek
 * Date:        19 Jan 2015
 * Description: Encapsulates VOD group attributes
 */

package com.aviq.tv.android.sdk.feature.vod;


/**
 * Encapsulates VOD group
 */
public abstract class VODGroup
{
	protected String _id;
	protected String _title;
	protected VODGroup _parent;

	public static class MetaData
	{
		public int metaVodGroupId;
		public int metaVodGroupTitle;
		public int metaVodGroupParent;
	}

	public VODGroup(String id, String title, VODGroup parent)
	{
		_id = id;
		_title = title;
		_parent = parent;
	}

	public String getId()
	{
		return _id;
	}

	public String getTitle()
	{
		return _title;
	}

	public VODGroup getParent()
	{
		return _parent;
	}

	/**
	 * Sets provider's specific VodGroup attributes
	 *
	 * @param metaData
	 *            indexed meta data
	 * @param attributes
	 *            String array with the essential data positioned according the
	 *            meta data indices
	 */
	public abstract void setAttributes(MetaData metaData, String[] attributes);
}

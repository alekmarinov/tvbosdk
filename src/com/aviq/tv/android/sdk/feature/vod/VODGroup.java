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

import org.json.JSONException;
import org.json.JSONObject;

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

	/**
	 * @return group id
	 */
	public String getId()
	{
		return _id;
	}

	/**
	 * @return group title
	 */
	public String getTitle()
	{
		return _title;
	}

	/**
	 * @return parent group of this group
	 */
	public VODGroup getParent()
	{
		return _parent;
	}

	/**
	 * Sets VOD details in JSON format
	 *
	 * @param details
	 */
	public abstract void setDetails(JSONObject details) throws JSONException;

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

	/**
	 * Get's VOD attribute value by standardized attribute name
	 *
	 * @param attribute
	 *            VodAttribute enumeration
	 */
	public abstract String getAttribute(VodAttribute vodAttribute);
}

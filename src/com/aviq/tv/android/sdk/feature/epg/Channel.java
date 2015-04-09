/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    Channel.java
 * Author:      alek
 * Date:        19 Dec 2013
 * Description: Channel data holder class
 */
package com.aviq.tv.android.sdk.feature.epg;

import java.io.Serializable;

public abstract class Channel implements Serializable
{
	private static final long serialVersionUID = 6801678500699483646L;

	public static final int LOGO_NORMAL = 0;

	private int _index;
	private String _channelId;
	private String _title;
	private int _ndvr;
	private String _logo;

	public static class MetaData
	{
		public int metaChannelId;
		public int metaChannelTitle;
		public int metaChannelLogo;
	}

	/**
	 * No-arg constructor added for Kryo serialization. Do not use for anything
	 * else.
	 */
	public Channel()
	{
	}

	public Channel(int index)
	{
		_index = index;
	}

	@Override
	public String toString()
	{
		return "[Channel " + _channelId + "]";
	}

	public int getIndex()
	{
		return _index;
	}

	public String getChannelId()
	{
		return _channelId;
	}

	public void setChannelId(String channelId)
	{
		_channelId = channelId;
	}

	public String getTitle()
	{
		return _title;
	}

	public void setTitle(String title)
	{
		_title = title;
	}

	public void setNDVR(int ndvr)
	{
		_ndvr = ndvr;
	}

	public int getNDVR()
	{
		return _ndvr;
	}

	public String getLogo(int logoType)
	{
		if (logoType != LOGO_NORMAL)
			throw new IllegalArgumentException("Expected logoType " + LOGO_NORMAL + ", got " + logoType);
		return _logo;
	}

	public void setLogo(int logoType, String logo)
	{
		if (logoType != LOGO_NORMAL)
			throw new IllegalArgumentException("Expected logoType " + LOGO_NORMAL + ", got " + logoType);
		_logo = logo;
	}

	/**
	 * Sets provider's specific channel attributes
	 *
	 * @param metaData
	 *            indexed meta data
	 * @param attributes
	 *            String array with the essential data positioned according the
	 *            meta data indices
	 */
	public abstract void setAttributes(MetaData metaData, String[] attributes);
}


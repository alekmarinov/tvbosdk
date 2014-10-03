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

	private int _index;
	private String _channelId;
	private String _title;
	private String _thumbnail;
	private int _ndvr;

	public static class MetaData
	{
		public int metaChannelId;
		public int metaChannelTitle;
		public int metaChannelThumbnail;
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

	public String getThumbnail()
	{
		return _thumbnail;
	}

	public void setThumbnail(String thumbnail)
	{
		_thumbnail = thumbnail;
	}

	public void setNDVR(int ndvr)
	{
		_ndvr = ndvr;
	}

	public int getNDVR()
	{
		return _ndvr;
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


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

import android.graphics.Bitmap;

public abstract class Channel
{
	public static final int LOGO_NORMAL = 0;

	private int _index;
	private String _channelId;
	private String _title;
	private int _ndvr;
	private String _logoNormalUrl;
	private Bitmap _logoNormal;
	private String _logoNormalBase64;

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

	public String getChannelImageBase64(int imageType)
	{
		if (imageType != LOGO_NORMAL)
			throw new IllegalArgumentException("Expected imageType " + LOGO_NORMAL + ", got " + imageType);
		return _logoNormalBase64;
	}

	public void setChannelImageBase64(int imageType, String imageBase64)
	{
		if (imageType != LOGO_NORMAL)
			throw new IllegalArgumentException("Expected imageType " + LOGO_NORMAL + ", got " + imageType);
		_logoNormalBase64 = imageBase64;
	}

	public Bitmap getChannelImage(int imageType)
	{
		if (imageType != LOGO_NORMAL)
			throw new IllegalArgumentException("Expected imageType " + LOGO_NORMAL + ", got " + imageType);
		return _logoNormal;
	}

	public void setChannelImage(int imageType, Bitmap image)
	{
		if (imageType != LOGO_NORMAL)
			throw new IllegalArgumentException("Expected imageType " + LOGO_NORMAL + ", got " + imageType);
		_logoNormal = image;
	}

	public String getChannelImageUrl(int imageType)
	{
		if (imageType != LOGO_NORMAL)
			throw new IllegalArgumentException("Expected imageType " + LOGO_NORMAL + ", got " + imageType);
		return _logoNormalUrl;
	}

	public void setChannelImageUrl(int imageType, String imageUrl)
	{
		if (imageType != LOGO_NORMAL)
			throw new IllegalArgumentException("Expected imageType " + LOGO_NORMAL + ", got " + imageType);
		_logoNormalUrl = imageUrl;
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


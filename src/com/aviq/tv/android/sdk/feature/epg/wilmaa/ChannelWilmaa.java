/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    ChannelWilmaa.java
 * Author:      alek
 * Date:        19 Dec 2013
 * Description: Wilmaa specific channel data holder class
 */

package com.aviq.tv.android.sdk.feature.epg.wilmaa;

import java.io.Serializable;

import com.aviq.tv.android.sdk.feature.epg.Channel;

/**
 * Wilmaa specific channel data holder class
 */
public class ChannelWilmaa extends Channel implements Serializable
{
    private static final long serialVersionUID = 1863868397983235013L;

	private String _streamUrl;

	public ChannelWilmaa(int index)
    {
	    super(index);
    }

	public static class MetaData extends Channel.MetaData
	{
		public int metaChannelStreamUrl;
	}

	public void setStreamUrl(String streamUrl)
	{
		_streamUrl = streamUrl;
	}

	public String getStreamUrl()
	{
		return _streamUrl;
	}

	@Override
    public void setAttributes(Channel.MetaData channelMetaData, String[] attributes)
	{
		MetaData channelWilmaaMetaData = (MetaData)channelMetaData;
		setStreamUrl(attributes[channelWilmaaMetaData.metaChannelStreamUrl]);
	}
}

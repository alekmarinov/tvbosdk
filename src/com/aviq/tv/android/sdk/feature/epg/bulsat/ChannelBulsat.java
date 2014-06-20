/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    ChannelBulsat.java
 * Author:      alek
 * Date:        19 Dec 2013
 * Description: Bulsat specific channel data holder class
 */

package com.aviq.tv.android.sdk.feature.epg.bulsat;

import java.io.Serializable;

import com.aviq.tv.android.sdk.feature.epg.Channel;

/**
 * Bulsat specific channel data holder class
 */
public class ChannelBulsat extends Channel implements Serializable
{
    private static final long serialVersionUID = -8718850662391176233L;

	private String _streamUrl;

	public ChannelBulsat(int index)
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
		MetaData channelBulsatMetaData = (MetaData)channelMetaData;
		setStreamUrl(attributes[channelBulsatMetaData.metaChannelStreamUrl]);
	}
}

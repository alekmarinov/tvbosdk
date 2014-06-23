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

import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.feature.epg.Channel;

/**
 * Bulsat specific channel data holder class
 */
public class ChannelBulsat extends Channel implements Serializable
{
    private static final long serialVersionUID = -8718850662391176233L;

	private String _streamUrl;
	private int _channelNo;

    /**
	 * No-arg constructor added for Kryo serialization. Do not use for anything else.
	 */
	public ChannelBulsat()
	{
	}

	public ChannelBulsat(int index)
    {
	    super(index);
    }

	public static class MetaData extends Channel.MetaData
	{
		public int metaChannelStreamUrl;
		public int metaChannelChannelNo;
	}

	public void setStreamUrl(String streamUrl)
	{
		_streamUrl = streamUrl;
	}

	public String getStreamUrl()
	{
		return _streamUrl;
	}

	public void setChannelNo(int channelNo)
	{
		_channelNo = channelNo;
	}

	public int getChannelNo()
	{
		return _channelNo;
	}

	@Override
    public void setAttributes(Channel.MetaData channelMetaData, String[] attributes)
	{
		MetaData channelBulsatMetaData = (MetaData)channelMetaData;
		setStreamUrl(attributes[channelBulsatMetaData.metaChannelStreamUrl]);
		try {
			setChannelNo(Integer.parseInt(attributes[channelBulsatMetaData.metaChannelChannelNo]));
		}
		catch (NumberFormatException pe)
		{
			Log.e("ChannelBulsat", pe.getMessage(), pe);
		}
	}
}

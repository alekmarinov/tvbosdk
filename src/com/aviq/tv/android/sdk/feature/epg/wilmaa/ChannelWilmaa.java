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

import android.os.Parcel;
import android.os.Parcelable;

import com.aviq.tv.android.sdk.feature.epg.Channel;

/**
 * Wilmaa specific channel data holder class
 */
public class ChannelWilmaa extends Channel
{
	private String _streamUrl;

	public static final Parcelable.Creator<Channel> CREATOR = new Parcelable.Creator<Channel>()
	{
		@Override
		public Channel createFromParcel(Parcel in)
		{
			return new ChannelWilmaa(in);
		}

		@Override
		public Channel[] newArray(int size)
		{
			return new Channel[size];
		}
	};

	public static class MetaData extends Channel.MetaData
	{
		public int metaChannelStreamUrl;
	}

	public ChannelWilmaa()
	{
	}

	public ChannelWilmaa(Parcel in)
	{
		super(in);
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

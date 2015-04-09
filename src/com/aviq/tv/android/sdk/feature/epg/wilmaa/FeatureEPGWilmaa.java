/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureEPGWilmaa.java
 * Author:      alek
 * Date:        1 Dec 2013
 * Description: Wilmaa specific extension of EPG feature
 */

package com.aviq.tv.android.sdk.feature.epg.wilmaa;

import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.epg.FeatureEPGCompat;
import com.aviq.tv.android.sdk.feature.epg.Program;

/**
 * RayV specific extension of EPG feature
 */
public class FeatureEPGWilmaa extends FeatureEPGCompat
{
	public static final String TAG = FeatureEPGWilmaa.class.getSimpleName();

	public FeatureEPGWilmaa() throws FeatureNotFoundException
    {
    }

	/**
	 * @return wilmaa EPG provider name
	 */
	@Override
    protected FeatureEPGCompat.Provider getEPGProvider()
	{
		return FeatureEPGCompat.Provider.wilmaa;
	}

	@Override
    protected Channel.MetaData createChannelMetaData()
	{
		return new ChannelWilmaa.MetaData();
	}

	@Override
    protected void indexChannelMetaData(Channel.MetaData metaData, String[] meta)
	{
		ChannelWilmaa.MetaData wilmaaMetaData = (ChannelWilmaa.MetaData)metaData;
		super.indexChannelMetaData(wilmaaMetaData, meta);

		for (int j = 0; j < meta.length; j++)
		{
			String key = meta[j];
			if ("streams.1.url".equals(key))
				wilmaaMetaData.metaChannelStreamUrl = j;
		}
	}

	@Override
    protected String getChannelsUrl()
	{
		String url = super.getChannelsUrl();
		return url + "?attr=streams.1.url";
	}

	@Override
    protected Channel createChannel(int index)
    {
	    return new ChannelWilmaa(index);
    }

	@Override
	protected Program createProgram(String id, Channel channel)
    {
	    return new ProgramWilmaa(id, channel);
    }

	@Override
    public void getStreamUrl(Channel channel, long playTime, long playDuration, OnStreamURLReceived onStreamURLReceived)
    {
		ChannelWilmaa channelWilmaa = (ChannelWilmaa)channel;
		StringBuffer url = new StringBuffer(channelWilmaa.getStreamUrl());
		if (playTime > 0)
		{
			if (url.indexOf("?") > 0)
				url.append('&');
			else
				url.append('?');
			url.append("start=").append(playTime);
			if (playDuration > 0)
			{
				url.append('&');
				url.append("end=").append(playTime + playDuration);
			}
		}
		onStreamURLReceived.onStreamURL(FeatureError.OK, url.toString());
    }

	@Override
    public long getStreamBufferSize(Channel channel)
	{
		// FIXME: Retrieve Wilmaa channel npvr value to preset stream buffer size
		return 0;
	}
}

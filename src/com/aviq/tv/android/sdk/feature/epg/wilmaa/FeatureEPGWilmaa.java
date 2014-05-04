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

import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.epg.FeatureEPG;
import com.aviq.tv.android.sdk.feature.epg.Program;

/**
 * RayV specific extension of EPG feature
 */
public class FeatureEPGWilmaa extends FeatureEPG
{
	public static final String TAG = FeatureEPGWilmaa.class.getSimpleName();

	public FeatureEPGWilmaa() throws FeatureNotFoundException
    {
    }

	/**
	 * @return wilmaa EPG provider name
	 */
	@Override
    protected FeatureEPG.Provider getEPGProvider()
	{
		return FeatureEPG.Provider.wilmaa;
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

	/**
	 * Return stream url for specified channel
	 *
	 * @param channelIndex
	 * @return stream url
	 */
	@Override
    public String getChannelStreamId(int channelIndex)
	{
		// FIXME: Refactore to return stream id here, but provide the real url from new Bulsat streamer
		ChannelWilmaa channel = (ChannelWilmaa)getEpgData().getChannel(channelIndex);
		return channel.getStreamUrl();
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
}

/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureEPGBulsat.java
 * Author:      alek
 * Date:        1 Dec 2013
 * Description: Bulsat specific extension of EPG feature
 */

package com.aviq.tv.android.sdk.feature.epg.bulsat;

import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.epg.FeatureEPG;
import com.aviq.tv.android.sdk.feature.epg.Program;

/**
 * RayV specific extension of EPG feature
 */
public class FeatureEPGBulsat extends FeatureEPG
{
	public FeatureEPGBulsat() throws FeatureNotFoundException
	{
		super();
	}

	public static final String TAG = FeatureEPGBulsat.class.getSimpleName();

	/**
	 * @return Bulsat EPG provider name
	 */
	@Override
	protected FeatureEPG.Provider getEPGProvider()
	{
		return FeatureEPG.Provider.bulsat;
	}

	@Override
	protected Channel.MetaData createChannelMetaData()
	{
		return new ChannelBulsat.MetaData();
	}

	@Override
	protected void indexChannelMetaData(Channel.MetaData metaData, String[] meta)
	{
		ChannelBulsat.MetaData bulsatMetaData = (ChannelBulsat.MetaData) metaData;
		super.indexChannelMetaData(bulsatMetaData, meta);

		for (int j = 0; j < meta.length; j++)
		{
			String key = meta[j];
			if ("channel".equals(key))
				bulsatMetaData.metaChannelChannelNo = j;
			else if ("streams.1.url".equals(key))
				bulsatMetaData.metaChannelStreamUrl = j;
			else if ("genre".equals(key))
				bulsatMetaData.metaChannelGenre = j;
		}
	}

	@Override
	protected String getProgramsUrl(String channelId)
	{
		String url = super.getProgramsUrl(channelId);
		return url + "?attr=description";
	}

	@Override
	protected Channel createChannel(int index)
	{
		return new ChannelBulsat(index);
	}

	@Override
	protected String getChannelsUrl()
	{
		String url = super.getChannelsUrl();
		return url + "?attr=channel,streams.1.url,genre";
	}

	@Override
	protected Program createProgram(String id, Channel channel)
	{
		return new ProgramBulsat(id, channel);
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
		ChannelBulsat channel = (ChannelBulsat) getEpgData().getChannel(channelIndex);
		return channel.getStreamUrl();
	}

	@Override
	protected Program.MetaData createProgramMetaData()
	{
		return new ProgramBulsat.MetaData();
	}

	@Override
	protected void indexProgramMetaData(Program.MetaData metaData, String[] meta)
	{
		ProgramBulsat.MetaData bulsatMetaData = (ProgramBulsat.MetaData) metaData;
		super.indexProgramMetaData(metaData, meta);

		for (int j = 0; j < meta.length; j++)
		{
			String key = meta[j];
			if ("description".equals(key))
				bulsatMetaData.metaDescription = j;
		}
	}
}

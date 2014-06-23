/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureEPGZattoo.java
 * Author:      alek
 * Date:        1 Dec 2013
 * Description: Zattoo specific extension of EPG feature
 */

package com.aviq.tv.android.sdk.feature.epg.zattoo;

import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.epg.FeatureEPG;
import com.aviq.tv.android.sdk.feature.epg.Program;

/**
 * Zattoo specific extension of EPG feature
 */
public class FeatureEPGZattoo extends FeatureEPG
{
	public static final String TAG = FeatureEPGZattoo.class.getSimpleName();

	public FeatureEPGZattoo() throws FeatureNotFoundException
    {
    }

	/**
	 * @return rayv EPG provider name
	 */
	@Override
	protected FeatureEPG.Provider getEPGProvider()
	{
		return FeatureEPG.Provider.zattoo;
	}

	/**
	 * Return stream Id for specified channel
	 *
	 * @param channelIndex
	 * @return stream id
	 */
	@Override
	public String getChannelStreamId(int channelIndex)
	{
		return getEpgData().getChannel(channelIndex).getChannelId();
	}

	@Override
	protected Program createProgram(String id, Channel channel)
	{
		return new ProgramZattoo(id, channel);
	}

	@Override
	protected Channel createChannel(int index)
	{
		return new ChannelZattoo(index);
	}
}

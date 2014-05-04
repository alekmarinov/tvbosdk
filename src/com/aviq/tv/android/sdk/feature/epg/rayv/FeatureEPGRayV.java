/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureEPGRayV.java
 * Author:      alek
 * Date:        1 Dec 2013
 * Description: RayV specific extension of EPG feature
 */

package com.aviq.tv.android.sdk.feature.epg.rayv;

import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.epg.FeatureEPG;
import com.aviq.tv.android.sdk.feature.epg.Program;

/**
 * RayV specific extension of EPG feature
 */
public class FeatureEPGRayV extends FeatureEPG
{
	public static final String TAG = FeatureEPGRayV.class.getSimpleName();

	public FeatureEPGRayV() throws FeatureNotFoundException
    {
    }

	/**
	 * @return rayv EPG provider name
	 */
	@Override
	protected FeatureEPG.Provider getEPGProvider()
	{
		return FeatureEPG.Provider.rayv;
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
		return new ProgramRayV(id, channel);
	}

	@Override
	protected Channel createChannel(int index)
	{
		return new ChannelRayV(index);
	}
}

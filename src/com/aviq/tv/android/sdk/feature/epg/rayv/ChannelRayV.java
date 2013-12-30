/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    ChannelRayV.java
 * Author:      alek
 * Date:        19 Dec 2013
 * Description: RayV specific channel data holder class
 */

package com.aviq.tv.android.sdk.feature.epg.rayv;

import com.aviq.tv.android.sdk.feature.epg.Channel;

/**
 * RayV specific channel data holder class
 */
public class ChannelRayV extends Channel
{

	public ChannelRayV(int index)
    {
	    super(index);
    }

	@Override
    public void setAttributes(MetaData channelMetaData, String[] attributes)
	{
		// no RayV specific channel attributes
	}
}

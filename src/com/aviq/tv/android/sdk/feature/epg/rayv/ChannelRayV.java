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

import java.io.Serializable;

import com.aviq.tv.android.sdk.feature.epg.Channel;

/**
 * RayV specific channel data holder class
 */
public class ChannelRayV extends Channel implements Serializable
{
    private static final long serialVersionUID = -4615784090400813696L;

    /**
	 * No-arg constructor added for Kryo serialization. Do not use for anything else.
	 */
	public ChannelRayV()
	{
	}

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

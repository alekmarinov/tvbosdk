/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    ChannelZattoo.java
 * Author:      alek
 * Date:        19 Dec 2013
 * Description: Zattoo specific channel data holder class
 */

package com.aviq.tv.android.sdk.feature.epg.zattoo;

import com.aviq.tv.android.sdk.feature.epg.Channel;

/**
 * Zattoo specific channel data holder class
 */
public class ChannelZattoo extends Channel
{
    private static final long serialVersionUID = -8718850662391176233L;

	/**
	 * No-arg constructor added for Kryo serialization. Do not use for anything else.
	 */
	public ChannelZattoo()
	{
	}

	public ChannelZattoo(int index)
    {
	    super(index);
    }

	@Override
    public void setAttributes(MetaData channelMetaData, String[] attributes)
	{
		// no Zattoo specific channel attributes
	}
}

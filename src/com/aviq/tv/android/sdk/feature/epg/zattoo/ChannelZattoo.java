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

/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    ChannelBulsat.java
 * Author:      alek
 * Date:        19 Dec 2013
 * Description: Bulsat specific channel data holder class
 */

package com.aviq.tv.android.sdk.feature.epg.bulsat;

import com.aviq.tv.android.sdk.feature.epg.Channel;

/**
 * Bulsat specific channel data holder class
 */
public class ChannelBulsat extends Channel
{
	public ChannelBulsat(int index)
    {
	    super(index);
    }

	@Override
    public void setAttributes(Channel.MetaData channelMetaData, String[] attributes)
	{
	}
}

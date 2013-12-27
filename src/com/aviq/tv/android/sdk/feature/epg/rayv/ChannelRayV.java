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

import android.os.Parcel;
import android.os.Parcelable;

import com.aviq.tv.android.sdk.feature.epg.Channel;

/**
 * RayV specific channel data holder class
 */
public class ChannelRayV extends Channel
{

	public static final Parcelable.Creator<Channel> CREATOR = new Parcelable.Creator<Channel>()
	{
		@Override
		public Channel createFromParcel(Parcel in)
		{
			return new ChannelRayV(in);
		}

		@Override
		public Channel[] newArray(int size)
		{
			return new Channel[size];
		}
	};

	public ChannelRayV()
	{
	}

	public ChannelRayV(Parcel in)
	{
		super(in);
	}

	@Override
    public void setAttributes(MetaData channelMetaData, String[] attributes)
	{
		// no RayV specific channel attributes
	}
}

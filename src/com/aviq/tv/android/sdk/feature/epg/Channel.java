/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    Channel.java
 * Author:      alek
 * Date:        19 Dec 2013
 * Description: Channel data holder class
 */
package com.aviq.tv.android.sdk.feature.epg;

import android.os.Parcel;
import android.os.Parcelable;

public abstract class Channel implements Parcelable
{
	private String _channelId;
	private String _title;
	private String _thumbnail;

	public static class MetaData
	{
		public int metaChannelId;
		public int metaChannelTitle;
		public int metaChannelThumbnail;
	}

	public Channel()
	{
	}

	public Channel(Parcel in)
	{
		readFromParcel(in);
	}

	public void readFromParcel(Parcel in)
	{
		_channelId = in.readString();
		_title = in.readString();
		_thumbnail = in.readString();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeString(_channelId);
		dest.writeString(_title);
		dest.writeString(_thumbnail);
	}

	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof Channel))
			return false;

		Channel other = (Channel) obj;
		return (_channelId != null && _channelId.equals(other._channelId))
		        && (_title != null && _title.equals(other._title))
		        && (_thumbnail != null && _thumbnail.equals(other._thumbnail));
	}

	@Override
	public int hashCode()
	{
		int result = 0;
		result = 31 * result + (_channelId != null ? _channelId.hashCode() : 0);
		result = 31 * result + (_title != null ? _title.hashCode() : 0);
		result = 31 * result + (_thumbnail != null ? _thumbnail.hashCode() : 0);
		return result;
	}

	public String getChannelId()
	{
		return _channelId;
	}

	public void setChannelId(String channelId)
	{
		_channelId = channelId;
	}

	public String getTitle()
	{
		return _title;
	}

	public void setTitle(String title)
	{
		_title = title;
	}

	public String getThumbnail()
	{
		return _thumbnail;
	}

	public void setThumbnail(String thumbnail)
	{
		_thumbnail = thumbnail;
	}

	/**
	 * Sets provider's specific channel attributes
	 *
	 * @param metaData indexed meta data
	 * @param attributes String array with the essential data positioned according the meta data indices
	 */
    public abstract void setAttributes(MetaData metaData, String[] attributes);
}

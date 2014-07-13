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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.feature.epg.Channel;

/**
 * Bulsat specific channel data holder class
 */
public class ChannelBulsat extends Channel implements Serializable
{
	private static final String TAG = ChannelBulsat.class.getSimpleName();
	private static final long serialVersionUID = -8718850662391176233L;
	private String _streamUrl;
	private int _channelNo;
	private Genre _genre;

	public static enum Genre
	{
		NATIONAL, SPORT, SCIENCE, MOVIE, MUSIC, KIDS, OTHER, EROTIC, RADIO, VOD
	}

	private static Map<String, Genre> _genreMap = new HashMap<String, Genre>();

	static
	{
		_genreMap.put("Национални", Genre.NATIONAL);
		_genreMap.put("Спортни", Genre.SPORT);
		_genreMap.put("Научни", Genre.SCIENCE);
		_genreMap.put("Филмови", Genre.MOVIE);
		_genreMap.put("Музикални", Genre.MUSIC);
		_genreMap.put("Детски", Genre.KIDS);
		_genreMap.put("Политематични", Genre.OTHER);
		_genreMap.put("XXX", Genre.EROTIC);
		_genreMap.put("Радио", Genre.RADIO);
	}

	/**
	 * No-arg constructor added for Kryo serialization. Do not use for anything
	 * else.
	 */
	public ChannelBulsat()
	{
	}

	public ChannelBulsat(int index)
	{
		super(index);
	}

	public static class MetaData extends Channel.MetaData
	{
		public int metaChannelStreamUrl;
		public int metaChannelChannelNo;
		public int metaChannelGenre;
	}

	public void setStreamUrl(String streamUrl)
	{
		_streamUrl = streamUrl;
	}

	public String getStreamUrl()
	{
		return _streamUrl;
	}

	public void setChannelNo(int channelNo)
	{
		_channelNo = channelNo;
	}

	public int getChannelNo()
	{
		return _channelNo;
	}

	public void setGenre(Genre genre)
	{
		_genre = genre;
	}

	public Genre getGenre()
	{
		return _genre;
	}

	@Override
	public void setAttributes(Channel.MetaData channelMetaData, String[] attributes)
	{
		MetaData channelBulsatMetaData = (MetaData) channelMetaData;
		setStreamUrl(attributes[channelBulsatMetaData.metaChannelStreamUrl]);
		try
		{
			setChannelNo(Integer.parseInt(attributes[channelBulsatMetaData.metaChannelChannelNo]));
		}
		catch (NumberFormatException nfe)
		{
			Log.e("ChannelBulsat", nfe.getMessage(), nfe);
		}

		String genreTitle = attributes[channelBulsatMetaData.metaChannelGenre];
		Genre genre = _genreMap.get(genreTitle);
		if (genre == null)
		{
			Log.w(TAG, "Can't find genre mapping of " + genreTitle + ", mapping to " + Genre.OTHER);
			genre = Genre.OTHER;
		}
		setGenre(genre);
	}
}

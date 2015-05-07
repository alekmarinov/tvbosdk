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

import com.aviq.tv.android.sdk.R;
import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.epg.bulsat.ProgramBulsat.ImageSize;

/**
 * Bulsat specific channel data holder class
 */
public class ChannelBulsat extends Channel implements Serializable
{
	private static final String TAG = ChannelBulsat.class.getSimpleName();
	private static final long serialVersionUID = -8718850662391176233L;
	public static final int LOGO_SELECTED = LOGO_NORMAL + 1;
	public static final int LOGO_FAVORITE = LOGO_NORMAL + 2;
	private static final String PG18 = "PG18";
	private int _channelNo;
	private Genre _genre;
	private String _streamUrl;
	private String _seekUrl;
	private boolean _parentControl;
	private boolean _recordable;
	private String _logoSelected;
	private String _logoFavorite;
	private String _programImageMedium;
	private String _programImageLarge;

	public static enum Genre
	{
		FAVORITES, NATIONAL, SPORT, SCIENCE, MOVIE, MUSIC, KIDS, OTHER, EROTIC, RADIO, VOD
	}

	private static Map<String, Genre> _genreMap = new HashMap<String, Genre>();

	static
	{
		String[] categoryNames = Environment.getInstance().getResources().getStringArray(R.array.categories);
		Genre[] genreList = Genre.values();
		for (int index = 1; index < genreList.length - 1; index++)
		{
			_genreMap.put(categoryNames[index - 1], genreList[index]);
		}
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
		public int metaChannelChannelNo;
		public int metaChannelGenre;
		public int metaChannelNdvr;
		public int metaChannelStreamUrl;
		public int metaChannelSeekUrl;
		public int metaChannelPG;
		public int metaChannelRecordable;
		public int metaChannelLogoSelected;
		public int metaChannelLogoFavorite;
		public int metaChannelProgramImageMedium;
		public int metaChannelProgramImageLarge;
	}

	public void setStreamUrl(String streamUrl)
	{
		_streamUrl = streamUrl;
	}

	public String getStreamUrl()
	{
		return _streamUrl;
	}

	public void setSeekUrl(String seekUrl)
	{
		_seekUrl = seekUrl;
	}

	public String getSeekUrl()
	{
		return _seekUrl;
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

	public void setParentControl(boolean parentControl)
	{
		_parentControl = parentControl;
	}

	public boolean isParentControl()
	{
		return _parentControl;
	}

	public void setRecordable(boolean recordable)
	{
		_recordable = recordable;
	}

	public boolean isRecordable()
	{
		return _recordable;
	}

	@Override
	public String getLogo(int logoType)
	{
		switch (logoType)
		{
			case LOGO_SELECTED:
				return _logoSelected;
			case LOGO_FAVORITE:
				return _logoFavorite;
		}
		return super.getLogo(logoType);
	}

	@Override
	public void setLogo(int logoType, String logo)
	{
		switch (logoType)
		{
			case LOGO_SELECTED:
				_logoSelected = logo;
			break;
			case LOGO_FAVORITE:
				_logoFavorite = logo;
			break;
			default:
				super.setLogo(logoType, logo);
		}
	}

	public void setProgramImage(String programImage, ImageSize imageSize)
	{
		switch (imageSize)
		{
			case MEDIUM:
				_programImageMedium = programImage;
			break;
			case LARGE:
				_programImageLarge = programImage;
			break;
		}
	}

	public String getProgramImage(ImageSize imageSize)
	{
		switch (imageSize)
		{
			case MEDIUM:
				return _programImageMedium;
			case LARGE:
				return _programImageLarge;
		}
		return null;
	}

	@Override
	public void setAttributes(Channel.MetaData channelMetaData, String[] attributes)
	{
		MetaData channelBulsatMetaData = (MetaData) channelMetaData;
		try
		{
			setChannelNo(Integer.parseInt(attributes[channelBulsatMetaData.metaChannelChannelNo]));
		}
		catch (NumberFormatException nfe)
		{
			Log.w("ChannelBulsat", "Missing or invalid ChannelNo in channel " + getChannelId());
		}

		String genreTitle = attributes[channelBulsatMetaData.metaChannelGenre];
		Genre genre = _genreMap.get(genreTitle);
		if (genre == null)
		{
			if (Environment.getInstance().getResources().getString(R.string.channel_category_alias_national)
			        .equals(genreTitle))
			{
				Log.i(TAG, "Assigning " + genreTitle + " to its alias " + Genre.NATIONAL + " for channel "
				        + getChannelId());
				genre = Genre.NATIONAL;
			}
			else if (Environment.getInstance().getResources().getString(R.string.channel_category_alias_erotic)
			        .equals(genreTitle))
			{
				Log.i(TAG, "Assigning " + genreTitle + " to its alias " + Genre.EROTIC + " for channel "
				        + getChannelId());
				genre = Genre.EROTIC;
			}
			else if (Environment.getInstance().getResources().getString(R.string.channel_category_alias_radio)
			        .equals(genreTitle))
			{
				Log.i(TAG, "Assigning " + genreTitle + " to its alias " + Genre.RADIO + " for channel "
				        + getChannelId());
				genre = Genre.RADIO;
			}
			else if (Environment.getInstance().getResources().getString(R.string.channel_category_alias_other)
			        .equals(genreTitle))
			{
				Log.i(TAG, "Assigning " + genreTitle + " to its alias " + Genre.OTHER + " for channel "
				        + getChannelId());
				genre = Genre.OTHER;
			}
			else
			{
				Log.w(TAG, "Can't find genre mapping of " + genreTitle + ", mapping to " + Genre.OTHER + " in channel "
				        + getChannelId());
				genre = Genre.OTHER;
			}
		}
		setGenre(genre);

		try
		{
			setNDVR(Integer.parseInt(attributes[channelBulsatMetaData.metaChannelNdvr]));
		}
		catch (NumberFormatException nfe)
		{
			Log.w("ChannelBulsat", "Missing or invalid NDVR in channel " + getChannelId());
		}
		if (attributes[channelBulsatMetaData.metaChannelStreamUrl] != null)
			setStreamUrl(new String(attributes[channelBulsatMetaData.metaChannelStreamUrl]));
		if (attributes[channelBulsatMetaData.metaChannelSeekUrl] != null)
			setSeekUrl(new String(attributes[channelBulsatMetaData.metaChannelSeekUrl]));
		if (attributes[channelBulsatMetaData.metaChannelPG] != null)
			setParentControl(PG18.equals(attributes[channelBulsatMetaData.metaChannelPG]));
		if (attributes[channelBulsatMetaData.metaChannelRecordable] != null)
			setRecordable(!"0".equals(attributes[channelBulsatMetaData.metaChannelRecordable]));
		if (attributes[channelBulsatMetaData.metaChannelLogoSelected] != null)
			setLogo(LOGO_SELECTED, new String(attributes[channelBulsatMetaData.metaChannelLogoSelected]));
		if (attributes[channelBulsatMetaData.metaChannelLogoFavorite] != null)
			setLogo(LOGO_FAVORITE, new String(attributes[channelBulsatMetaData.metaChannelLogoFavorite]));
		if (attributes[channelBulsatMetaData.metaChannelProgramImageMedium] != null)
			setProgramImage(new String(attributes[channelBulsatMetaData.metaChannelProgramImageMedium]),
			        ImageSize.MEDIUM);
		if (attributes[channelBulsatMetaData.metaChannelProgramImageLarge] != null)
			setProgramImage(new String(attributes[channelBulsatMetaData.metaChannelProgramImageLarge]), ImageSize.LARGE);
	}
}

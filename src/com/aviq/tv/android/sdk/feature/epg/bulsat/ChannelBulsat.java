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

import java.util.HashMap;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.aviq.tv.android.sdk.R;
import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.epg.bulsat.ProgramBulsat.ImageSize;

/**
 * Bulsat specific channel data holder class
 */
public class ChannelBulsat extends Channel
{
	private static final String TAG = ChannelBulsat.class.getSimpleName();
	public static final int LOGO_SELECTED = LOGO_NORMAL + 1;
	public static final int LOGO_FAVORITE = LOGO_NORMAL + 2;
	private static final String PG18 = "PG18";
	private int _channelNo;
	private Genre _genre;
	private String _streamUrl;
	private String _seekUrl;
	private boolean _parentControl;
	private boolean _recordable;
	private Bitmap _logoSelected;
	private Bitmap _logoFavorite;
	private String _programImageMediumUrl;
	private String _programImageLargeUrl;

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
		public int metaChannelLogo; // base64 image
		public int metaChannelLogoSelected; // base64 image
		public int metaChannelLogoFavorite; // base64 image
		public int metaChannelProgramImageMedium; // image url
		public int metaChannelProgramImageLarge; // image url
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
	public Bitmap getChannelImage(int imageType)
	{
		switch (imageType)
		{
			case LOGO_SELECTED:
				return _logoSelected;
			case LOGO_FAVORITE:
				return _logoFavorite;
		}
		return super.getChannelImage(imageType);
	}

	@Override
	public void setChannelImage(int imageType, Bitmap image)
	{
		switch (imageType)
		{
			case LOGO_SELECTED:
				_logoSelected = image;
			break;
			case LOGO_FAVORITE:
				_logoFavorite = image;
			break;
			default:
				super.setChannelImage(imageType, image);
		}
	}

	public void setProgramImageUrl(String programImageUrl, ImageSize imageSize)
	{
		switch (imageSize)
		{
			case MEDIUM:
				_programImageMediumUrl = programImageUrl;
			break;
			case LARGE:
				_programImageLargeUrl = programImageUrl;
			break;
			default:
				throw new IllegalArgumentException("Image size " + imageSize + " is not supported");
		}
	}

	public String getProgramImageUrl(ImageSize imageSize)
	{
		switch (imageSize)
		{
			case MEDIUM:
				return _programImageMediumUrl;
			case LARGE:
				return _programImageLargeUrl;
		}
		throw new IllegalArgumentException("Image size " + imageSize + " is not supported");
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
		{
			byte[] decodedString = Base64.decode(attributes[channelBulsatMetaData.metaChannelLogoSelected], Base64.DEFAULT);
			setChannelImage(LOGO_SELECTED, BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
		}
		if (attributes[channelBulsatMetaData.metaChannelLogoFavorite] != null)
		{
			byte[] decodedString = Base64.decode(attributes[channelBulsatMetaData.metaChannelLogoFavorite], Base64.DEFAULT);
			setChannelImage(LOGO_FAVORITE, BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
		}
		if (attributes[channelBulsatMetaData.metaChannelProgramImageMedium] != null)
			setProgramImageUrl(new String(attributes[channelBulsatMetaData.metaChannelProgramImageMedium]),
			        ImageSize.MEDIUM);
		if (attributes[channelBulsatMetaData.metaChannelProgramImageLarge] != null)
			setProgramImageUrl(new String(attributes[channelBulsatMetaData.metaChannelProgramImageLarge]), ImageSize.LARGE);
	}
}

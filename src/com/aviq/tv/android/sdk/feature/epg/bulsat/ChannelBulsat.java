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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

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
	private boolean _playable;
	private boolean _recordable;
	private Bitmap _logoSelected;
	private Bitmap _logoFavorite;
	private String _logoSelectedBase64;
	private String _logoFavoriteBase64;
	private String _logoSelectedUrl;
	private String _logoFavoriteUrl;
	private String _programImageMediumUrl;
	private String _programImageLargeUrl;
	private boolean _radio;

	// public static enum Genre
	// {
	// FAVORITES, NATIONAL, SPORT, SCIENCE, MOVIE, MUSIC, KIDS, OTHER, EROTIC,
	// RADIO, VOD
	// }

	// private static Map<String, Genre> _genreMap = new HashMap<String,
	// Genre>();
	//
	// static
	// {
	// String[] categoryNames =
	// Environment.getInstance().getResources().getStringArray(R.array.categories);
	// Genre[] genreList = Genre.values();
	// for (int index = 1; index < genreList.length - 1; index++)
	// {
	// _genreMap.put(categoryNames[index - 1], genreList[index]);
	// }
	// }

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
		public int metaChannelRadio;
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

	public void setPlayable(boolean playable)
	{
		_playable = playable;
	}

	public boolean isPlayable()
	{
		return _playable;
	}

	public void setRadio(boolean radio)
	{
		_radio = radio;
	}

	public boolean isRadio()
	{
		return _radio;
	}

	@Override
	public String getChannelImageBase64(int imageType)
	{
		switch (imageType)
		{
			case LOGO_SELECTED:
				return _logoSelectedBase64;
			case LOGO_FAVORITE:
				return _logoFavoriteBase64;
		}
		return super.getChannelImageBase64(imageType);
	}

	@Override
	public void setChannelImageBase64(int imageType, String imageBase64)
	{
		switch (imageType)
		{
			case LOGO_SELECTED:
				_logoSelectedBase64 = imageBase64;
			break;
			case LOGO_FAVORITE:
				_logoFavoriteBase64 = imageBase64;
			break;
			default:
				super.setChannelImageBase64(imageType, imageBase64);
		}
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

	@Override
    public String getChannelImageUrl(int imageType)
	{
		switch (imageType)
		{
			case LOGO_SELECTED:
				return _logoSelectedUrl;
			case LOGO_FAVORITE:
				return _logoFavoriteUrl;
		}
		return super.getChannelImageUrl(imageType);
	}

	@Override
    public void setChannelImageUrl(int imageType, String imageUrl)
	{
		switch (imageType)
		{
			case LOGO_SELECTED:
				_logoSelectedUrl = imageUrl;
			break;
			case LOGO_FAVORITE:
				_logoFavoriteUrl = imageUrl;
			break;
			default:
				super.setChannelImageUrl(imageType, imageUrl);
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
    public void validateChannel()
	{
		super.validateChannel();
		String propName = null;
		if (_genre == null)
			propName = "genre";
		else if (_channelNo == 0)
			propName = "channelNo";
		else if (_logoSelectedUrl == null)
			propName = "logoSelectedUrl";
		else if (_logoFavoriteUrl == null)
			propName = "logoFavoriteUrl";
		else if (_programImageMediumUrl == null)
			propName = "programImageMediumUrl";
		else if (_programImageLargeUrl == null)
			propName = "programImageLargeUrl";

		if (propName != null)
			throw new IllegalArgumentException("Missing channel property " + propName);
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
		setGenre(Genres.getInstance().getGenreByTitle(genreTitle));
		if (getGenre() == null)
			throw new IllegalArgumentException("Genre " + genreTitle + " is not recognized");

		if (attributes[channelBulsatMetaData.metaChannelNdvr] != null)
		{
			try
			{
				int ndvr = Integer.parseInt(attributes[channelBulsatMetaData.metaChannelNdvr]);
				setNDVR(ndvr);
				_playable = ndvr > 0;
			}
			catch (NumberFormatException nfe)
			{
				Log.w("ChannelBulsat", "Missing or invalid NDVR in channel " + getChannelId());
			}
		}

		if (attributes[channelBulsatMetaData.metaChannelRecordable] != null)
		{
			try
			{
				int record = Integer.parseInt(attributes[channelBulsatMetaData.metaChannelRecordable]);
				_recordable = record > 0;
				if (getNDVR() == 0)
					setNDVR(record);
			}
			catch (NumberFormatException nfe)
			{
				Log.w("ChannelBulsat", "Missing or invalid recordable in channel " + getChannelId());
			}
		}

		if (attributes[channelBulsatMetaData.metaChannelStreamUrl] != null)
			setStreamUrl(new String(attributes[channelBulsatMetaData.metaChannelStreamUrl]));
		if (attributes[channelBulsatMetaData.metaChannelSeekUrl] != null)
			setSeekUrl(new String(attributes[channelBulsatMetaData.metaChannelSeekUrl]));
		if (attributes[channelBulsatMetaData.metaChannelPG] != null)
			setParentControl(PG18.equals(attributes[channelBulsatMetaData.metaChannelPG]));
		if (attributes[channelBulsatMetaData.metaChannelRadio] != null)
			setRadio("true".equals(attributes[channelBulsatMetaData.metaChannelRadio]));
		if (attributes[channelBulsatMetaData.metaChannelLogoSelected] != null)
		{
			setChannelImageBase64(LOGO_SELECTED, attributes[channelBulsatMetaData.metaChannelLogoSelected]);
			byte[] decodedString = Base64.decode(attributes[channelBulsatMetaData.metaChannelLogoSelected],
			        Base64.DEFAULT);
			setChannelImage(LOGO_SELECTED, BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
		}
		if (attributes[channelBulsatMetaData.metaChannelLogoFavorite] != null)
		{
			setChannelImageBase64(LOGO_FAVORITE, attributes[channelBulsatMetaData.metaChannelLogoFavorite]);
			byte[] decodedString = Base64.decode(attributes[channelBulsatMetaData.metaChannelLogoFavorite],
			        Base64.DEFAULT);
			setChannelImage(LOGO_FAVORITE, BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
		}
		if (attributes[channelBulsatMetaData.metaChannelProgramImageMedium] != null)
			setProgramImageUrl(new String(attributes[channelBulsatMetaData.metaChannelProgramImageMedium]),
			        ImageSize.MEDIUM);
		if (attributes[channelBulsatMetaData.metaChannelProgramImageLarge] != null)
			setProgramImageUrl(new String(attributes[channelBulsatMetaData.metaChannelProgramImageLarge]),
			        ImageSize.LARGE);
	}
}

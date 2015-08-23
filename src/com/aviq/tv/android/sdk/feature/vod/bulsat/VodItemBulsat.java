/**
 * Copyright (c) 2007-2015, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    VodItemBulsat.java
 * Author:      alek
 * Date:        19 Jan 2015
 * Description: Encapsulates VOD item attributes for Bulsatcom provider
 */

package com.aviq.tv.android.sdk.feature.vod.bulsat;

import org.json.JSONException;
import org.json.JSONObject;

import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.feature.vod.VODGroup;
import com.aviq.tv.android.sdk.feature.vod.VODItem;
import com.aviq.tv.android.sdk.feature.vod.VodAttribute;

/**
 * Encapsulates VOD item attributes for Bulsatcom provider
 */
public class VodItemBulsat extends VODItem
{
	private static final String TAG = VodItemBulsat.class.getSimpleName();

	// common attributes
	private String _posterSmall;
	private String _posterMedium;
	private String _posterLarge;
	private String _posterSmallBase64;
	private String _posterMediumBase64;
	private String _posterLargeBase64;
	private String _shortDescription;
	private String _release;
	private String _country;
	private int _duration;
	private float[] _ratings = new float[RatingSystem.values().length];
	private String _youTubeTrailerUrl;
	private String _director;

	// detail attributes
	private String _description;
	private String _source;

	public static class MetaData extends VODItem.MetaData
	{
		public int metaPosterSmall;
		public int metaPosterMedium;
		public int metaPosterLarge;
		public int metaPosterSmallBase64;
		public int metaPosterMediumBase64;
		public int metaPosterLargeBase64;
		public int metaDescription;
		public int metaShortDescription;
		public int metaRelease;
		public int metaCountry;
		public int metaRatingImdb;
		public int metaDuration;
		public int metaYouTubeTrailerUrl;
		public int metaDirector;
	}

	public static enum RatingSystem
	{
		IMDB, BULSATCOM
	}

	public static enum PosterSize
	{
		SMALL, MEDIUM, LARGE
	}

	public VodItemBulsat(String id, String title, VODGroup parent)
	{
		super(id, title, parent);
	}

	public String getPoster(PosterSize posterSize)
	{
		switch (posterSize)
		{
			case SMALL:
				return _posterSmall;
			case MEDIUM:
				return _posterMedium;
			case LARGE:
				return _posterLarge;
		}
		return null;
	}

	@Override
	public String getPoster()
	{
		return getPoster(PosterSize.LARGE);
	}

	@Override
    public String getSourceUrl()
    {
	    return _source;
    }

	private void setRating(RatingSystem ratingType, float ratingValue)
	{
		_ratings[ratingType.ordinal()] = ratingValue;
	}

	public float getRating(RatingSystem ratingType)
	{
		return _ratings[ratingType.ordinal()];
	}

	public String getShortDescription()
	{
		return _shortDescription;
	}

	public String getDescription()
	{
		return _description;
	}

	public String getRelease()
	{
		return _release;
	}

	public String getCountry()
	{
		return _country;
	}

	public int getDuration()
	{
		return _duration;
	}

	public String getYouTubeTrailerUrl()
	{
		return _youTubeTrailerUrl;
	}

	public String getDirector()
	{
		return _director;
	}

	@Override
    public void setDetails(JSONObject details) throws JSONException
    {
		_description = details.getString("description");
		_source = details.getString("source");
		_posterSmallBase64 = details.getString("poster_small_base64");
		_posterMediumBase64 = details.getString("poster_medium_base64");
		_posterLargeBase64 = details.getString("poster_large_base64");
    }

	@Override
	public void setAttributes(VODGroup.MetaData metaData, String[] attributes)
	{
		MetaData vodItemBulsatMetaData = (MetaData) metaData;

		if (attributes[vodItemBulsatMetaData.metaPosterSmall] != null)
			_posterSmall = new String(attributes[vodItemBulsatMetaData.metaPosterSmall]);
		if (attributes[vodItemBulsatMetaData.metaPosterMedium] != null)
			_posterMedium = new String(attributes[vodItemBulsatMetaData.metaPosterMedium]);
		if (attributes[vodItemBulsatMetaData.metaPosterLarge] != null)
			_posterLarge = new String(attributes[vodItemBulsatMetaData.metaPosterLarge]);

		if (attributes[vodItemBulsatMetaData.metaPosterSmallBase64] != null)
			_posterSmallBase64 = new String(attributes[vodItemBulsatMetaData.metaPosterSmallBase64]);

		if (attributes[vodItemBulsatMetaData.metaPosterMediumBase64] != null)
			_posterMediumBase64 = new String(attributes[vodItemBulsatMetaData.metaPosterMediumBase64]);
		if (attributes[vodItemBulsatMetaData.metaPosterLargeBase64] != null)
			_posterLargeBase64 = new String(attributes[vodItemBulsatMetaData.metaPosterLargeBase64]);

		if (attributes[vodItemBulsatMetaData.metaShortDescription] != null)
			_shortDescription = new String(attributes[vodItemBulsatMetaData.metaShortDescription]);

		if (attributes[vodItemBulsatMetaData.metaRelease] != null)
			_release = new String(attributes[vodItemBulsatMetaData.metaRelease]);
		if (attributes[vodItemBulsatMetaData.metaCountry] != null)
			_country = new String(attributes[vodItemBulsatMetaData.metaCountry]);

		try
		{
			if (attributes[vodItemBulsatMetaData.metaDuration] != null)
				_duration = Integer.parseInt(attributes[vodItemBulsatMetaData.metaDuration]);
		}
		catch (NumberFormatException nfe)
		{
			Log.w(TAG, nfe.getMessage(), nfe);
		}

		try
		{
			if (attributes[vodItemBulsatMetaData.metaRatingImdb] != null)
				setRating(RatingSystem.IMDB,
				        10 * Float.parseFloat(attributes[vodItemBulsatMetaData.metaRatingImdb]) / 1000);
		}
		catch (NumberFormatException nfe)
		{
			Log.w(TAG, nfe.getMessage(), nfe);
		}

		if (attributes[vodItemBulsatMetaData.metaYouTubeTrailerUrl] != null)
			_youTubeTrailerUrl = new String(attributes[vodItemBulsatMetaData.metaYouTubeTrailerUrl]);

		if (attributes[vodItemBulsatMetaData.metaDirector] != null)
			_director = new String(attributes[vodItemBulsatMetaData.metaDirector]);
	}

	@Override
	public String getAttribute(VodAttribute vodAttribute)
	{
		switch (vodAttribute)
		{
			case DESCRIPTION:
				return _description;
			case COUNTRY:
				return _country;
			case DIRECTOR:
				return _director;
			case RATING:
				return String.valueOf(_ratings[0]);
			case RELEASE:
				return _release;
			case SHORT_DESCRIPTION:
				return _shortDescription;
			case DURATION:
				return String.valueOf(_duration);
			case YOUTUBE_TRAILER_URL:
				return _youTubeTrailerUrl;
			case POSTER_SMALL_BASE64:
				return _posterSmallBase64;
			case POSTER_MEDIUM_BASE64:
				return _posterMediumBase64;
			case POSTER_LARGE_BASE64:
				return _posterLargeBase64;
			default:
		}
		return null;
	}
}

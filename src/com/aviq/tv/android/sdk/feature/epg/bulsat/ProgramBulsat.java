/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    ProgramBulsat.java
 * Author:      alek
 * Date:        19 Dec 2013
 * Description: Bulsat specific program data holder class
 */

package com.aviq.tv.android.sdk.feature.epg.bulsat;

import org.json.JSONException;
import org.json.JSONObject;

import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.epg.Program;
import com.aviq.tv.android.sdk.feature.epg.ProgramAttribute;

/**
 * Bulsat specific program data holder class
 */
public class ProgramBulsat extends Program
{
	public static final String TAG = ProgramBulsat.class.getSimpleName();
	public static final String NO_EPG_DATA = "NO_EPG_DATA";

	public static enum ImageSize
	{
		MEDIUM, LARGE
	}

	private String _description;
	private String _imageMedium;
	private String _imageLarge;

	public static class MetaData extends Program.MetaData
	{
		public int metaDescription;
		public int metaImageMedium;
		public int metaImageLarge;
	}

	/**
	 * No-arg constructor added for Kryo serialization. Do not use for anything
	 * else.
	 */
	public ProgramBulsat()
	{
	}

	public ProgramBulsat(String id, Channel channel)
	{
		super(id, channel);
	}

	public String getImage(ImageSize imageSize)
	{
		switch (imageSize)
		{
			case MEDIUM:
				return _imageMedium;
			case LARGE:
				return _imageLarge;
		}
		return null;
	}

	@Override
    public boolean hasDetails()
	{
		return _description != null;
	}

	@Override
	public void setDetails(JSONObject detailsResponse)
	{
		try
        {
			if (detailsResponse.has("description"))
				_description = detailsResponse.getString("description");
			else
				_description = "";
        }
        catch (JSONException e)
        {
        	Log.e(TAG, e.getMessage(), e);
        }
	}

	@Override
	public void setDetailAttributes(Program.MetaData programMetaData, String[] attributes)
	{
		MetaData programBulsatMetaData = (MetaData) programMetaData;
		if (attributes[programBulsatMetaData.metaDescription] != null)
			_description = new String(attributes[programBulsatMetaData.metaDescription]);
		if (attributes[programBulsatMetaData.metaImageMedium] != null)
			_imageMedium = new String(attributes[programBulsatMetaData.metaImageMedium]);
		if (attributes[programBulsatMetaData.metaImageLarge] != null)
			_imageLarge = new String(attributes[programBulsatMetaData.metaImageLarge]);
	}

	@Override
	public String getDetailAttribute(ProgramAttribute programAttribute)
	{
		switch (programAttribute)
		{
			case DESCRIPTION:
				return _description;
			case IMAGE:
				return _imageLarge;
		}
		return null;
	}
}

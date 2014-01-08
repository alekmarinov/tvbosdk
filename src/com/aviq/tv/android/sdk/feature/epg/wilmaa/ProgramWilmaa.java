/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    ProgramWilmaa.java
 * Author:      alek
 * Date:        19 Dec 2013
 * Description: Wilmaa specific program data holder class
 */

package com.aviq.tv.android.sdk.feature.epg.wilmaa;

import org.json.JSONException;
import org.json.JSONObject;

import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.epg.Program;
import com.aviq.tv.android.sdk.feature.epg.ProgramAttribute;

/**
 * RayV specific program data holder class
 */
public class ProgramWilmaa extends Program
{
	public static int MAX_SUMMARY_LENGTH = 100;
	public static int MIN_SUMMARY_LENGTH = 20;

	private static final String TAG = ProgramWilmaa.class.getSimpleName();
	private JSONObject _detailsResponse;

	public ProgramWilmaa(Channel channel)
	{
		super(channel);
	}

	@Override
	public boolean hasDetails()
	{
		return _detailsResponse != null;
	}

	@Override
	public void setDetails(JSONObject detailsResponse)
	{
		_detailsResponse = detailsResponse;
	}

	@Override
	public String getDetailAttribute(ProgramAttribute programAttribute)
	{
		if (_detailsResponse == null)
		{
			Log.w(TAG, "Requesting attribute " + programAttribute + " before setting program attributes");
			return null;
		}

		try
		{
			switch (programAttribute)
			{
				case SUBTITLE:
					return _detailsResponse.getString("subtitle");
				case SUMMARY:
					return _detailsResponse.getString("description");
				case DESCRIPTION:
					return _detailsResponse.getString("description");
				case GENRE:
				{
					return getAttributeList("category");
				}
				case IMAGE_URL:
				// not supported
				break;
				case SEASON:
				// not supported
				break;
				case SERIE:
				// not supported
				break;
				case DATE:
					return _detailsResponse.getString("date");
				case CREDITS_ACTORS:
					return getAttributeList("actor");
				case CREDITS_PRESENTER:
					return _detailsResponse.getString("credits.presenter");
				case CREDITS_DIRECTOR:
					return _detailsResponse.getString("credits.director");
				case ASPECT_RATIO:
					return _detailsResponse.getString("video.aspect");
			}
		}
		catch (JSONException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		return null;
	}

	private String getAttributeList(String key) throws JSONException
	{
		StringBuffer result = new StringBuffer();
		String element = _detailsResponse.getString(key + ".1");
		int index = 1;
		while (element != null)
		{
			if (index > 1)
				result.append(", ");
			result.append(element);
			index++;
			element = _detailsResponse.getString(key + "." + index);
		}
		return result.toString();
	}

	@Override
    public void setDetailAttributes(MetaData metaData, String[] attributes)
    {
		// no Wilmaa specific channel attributes
    }
}

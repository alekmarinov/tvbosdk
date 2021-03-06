/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    ProgramZattoo.java
 * Author:      alek
 * Date:        19 Dec 2013
 * Description: Zattoo specific program data holder class
 */

package com.aviq.tv.android.sdk.feature.epg.zattoo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.epg.Program;
import com.aviq.tv.android.sdk.feature.epg.ProgramAttribute;

/**
 * RayV specific program data holder class
 */
public class ProgramZattoo extends Program
{
	private static final long serialVersionUID = 6850910305560837142L;

	public static int MAX_SUMMARY_LENGTH = 100;
	public static int MIN_SUMMARY_LENGTH = 20;

	private static final String TAG = ProgramZattoo.class.getSimpleName();
	private JSONObject _detailsResponse;
	private String _zapiId;

	/**
	 * No-arg constructor added for Kryo serialization. Do not use for anything
	 * else.
	 */
	public ProgramZattoo()
	{
	}

	public ProgramZattoo(String id, Channel channel)
	{
		super(id, channel);
	}

	@Override
	public boolean hasDetails()
	{
		return _detailsResponse != null;
	}

	public void setZapiID(String zapiId)
	{
		_zapiId = zapiId;
	}

	public String getZapiID()
	{
		return _zapiId;
	}

	@Override
	public void setDetails(JSONObject detailsResponse)
	{
		_detailsResponse = detailsResponse;
	}

	private String getAnyString(JSONObject object, String field1, String field2)
	{
		String value = null;
		try
		{
			value = object.getString(field1);
		}
		catch (JSONException e)
		{
			try
			{
				value = object.getString(field2);
			}
			catch (JSONException e1)
			{
				Log.e(TAG, e1.getMessage(), e1);
			}
		}
		return value;
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
					return _detailsResponse.getString("episode_title");
				case SUMMARY:
					return _detailsResponse.getString("description");
				case DESCRIPTION:
					return _detailsResponse.getString("description");
				case GENRE:
				{
					return getAttributeList("categories");
				}
				case IMAGE:
				// not supported
				break;
				case SEASON:
				// not supported
				break;
				case SERIE:
				// not supported
				break;
				case DATE:
					return _detailsResponse.getString("year");
				case CREDITS_ACTORS:
					// not supported
				case CREDITS_PRESENTER:
					// not supported
				case CREDITS_DIRECTOR:
					// not supported
				case ASPECT_RATIO:
					// not supported
			}
		}
		catch (JSONException e)
		{
			Log.w(TAG, e.getMessage());
		}
		return null;
	}

	private String getAttributeList(String key) throws JSONException
	{
		StringBuffer result = new StringBuffer();
		try
		{
			// EPG GW format
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
		}
		catch (JSONException e)
		{
			// ZAPI format
			JSONArray list = _detailsResponse.getJSONArray(key);
			for (int i = 0; i < list.length(); i++)
			{
				if (result.length() > 0)
					result.append(", ");
				result.append(list.getString(i));
			}
		}
		return result.toString();
	}

	@Override
	public void setDetailAttributes(MetaData metaData, String[] attributes)
	{
		// no RayV specific channel attributes
	}
}

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

import java.io.Serializable;

import org.json.JSONObject;

import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.epg.Program;
import com.aviq.tv.android.sdk.feature.epg.ProgramAttribute;

/**
 * Bulsat specific program data holder class
 */
public class ProgramBulsat extends Program implements Serializable
{
	private static final long serialVersionUID = 5914858738230945780L;

	private String _description;
	private String _image;

	public static class MetaData extends Program.MetaData
	{
		public int metaDescription;
		public int metaImage;
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

	@Override
	public boolean hasDetails()
	{
		return true;
	}

	@Override
	public void setDetails(JSONObject detailsResponse)
	{
	}

	@Override
	public void setDetailAttributes(Program.MetaData programMetaData, String[] attributes)
	{
		MetaData programBulsatMetaData = (MetaData) programMetaData;
		if (attributes[programBulsatMetaData.metaDescription] != null)
			_description = new String(attributes[programBulsatMetaData.metaDescription]);
		if (attributes[programBulsatMetaData.metaImage] != null)
			_image = new String(attributes[programBulsatMetaData.metaImage]);
	}

	@Override
	public String getDetailAttribute(ProgramAttribute programAttribute)
	{
		switch (programAttribute)
		{
			case DESCRIPTION:
				return _description;
			case IMAGE:
				return _image;
		}
		return null;
	}
}

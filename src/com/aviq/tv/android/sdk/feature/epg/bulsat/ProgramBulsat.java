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

import org.json.JSONObject;

import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.epg.Program;
import com.aviq.tv.android.sdk.feature.epg.ProgramAttribute;

/**
 * Bulsat specific program data holder class
 */
public class ProgramBulsat extends Program
{
	private String _description;

	public static class MetaData extends Program.MetaData
	{
		public int metaDescription;
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
		MetaData programBulsatMetaData = (MetaData)programMetaData;
		_description = attributes[programBulsatMetaData.metaDescription];
    }

	@Override
	public String getDetailAttribute(ProgramAttribute programAttribute)
	{
		switch (programAttribute)
		{
			case DESCRIPTION:
				return _description;
		}
		return null;
	}
}

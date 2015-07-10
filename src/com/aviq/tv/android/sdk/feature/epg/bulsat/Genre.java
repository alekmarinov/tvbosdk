/**
 * Copyright (c) 2007-2015, Intelibo Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    Genre.java
 * Author:      alek
 * Date:        5 Jul 2015
 * Description: Bulsat specific genre data holder class
 */

package com.aviq.tv.android.sdk.feature.epg.bulsat;

import android.graphics.Bitmap;

/**
 * Bulsat specific genre data holder class
 */
public class Genre
{
	private String _id;
	private String _title;
	private Bitmap _logo;
	private Bitmap _logoSelected;
	private int _index;

	/**
	 * @return the _id
	 */
	public String getId()
	{
		return _id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(String id)
	{
		_id = id;
	}

	/**
	 * @return the title
	 */
	public String getTitle()
	{
		return _title;
	}

	/**
	 * @param title
	 *            the title to set
	 */
	public void setTitle(String title)
	{
		_title = title;
	}

	/**
	 * @return the logo bitmap
	 */
	public Bitmap getLogo()
	{
		return _logo;
	}

	/**
	 * @param logo
	 *            the logo bitmap to set
	 */
	public void setLogo(Bitmap logo)
	{
		_logo = logo;
	}

	/**
	 * @return the logoSelected bitmap
	 */
	public Bitmap getLogoSelected()
	{
		return _logoSelected;
	}

	/**
	 * @param logoSelected
	 *            the logoSelected bitmap to set
	 */
	public void setLogoSelected(Bitmap logoSelected)
	{
		_logoSelected = logoSelected;
	}

	/**
	 * @return the index
	 */
	public int getIndex()
	{
		return _index;
	}

	/**
	 * @param index
	 *            the index to set
	 */
	void setIndex(int index)
	{
		_index = index;
	}
}

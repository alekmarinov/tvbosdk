/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    VodGroup.java
 * Author:      elmira pavlova
 * Date:        29 Jul 2014
 * Description: Vod group data holder class
 */
package com.aviq.tv.android.sdk.feature.epg;

import java.util.ArrayList;
import java.util.List;

public class VodGroup
{
	private String _title;
	
	private List<VodGroup> _items;
	
	VodGroup()
	{
		_items = new ArrayList<VodGroup>();
	}
	
	public List<VodGroup> getItems()
	{
		// TODO Auto-generated method stub
		return _items;
	}
	public void addItem(VodGroup item)
	{
		// TODO Auto-generated method stub
		_items.add(item);
	}
	
	public boolean hasItems()
	{
		// TODO Auto-generated method stub
		return _items.size()>0;
	}
	
	public String getTitle()
	{
		return _title;
	}
	
	public void setTitle(String title)
	{
		this._title = title;
	}
	
}

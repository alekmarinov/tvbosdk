package com.aviq.tv.android.sdk.feature.vod.bulsat;

import java.util.ArrayList;
import java.util.List;

public class VodGroup
{
	private String _title;
	private String _shortName;
	private String _image;
	private String _subitems;
	private String _id;
	private List<Vod> _vodList = new ArrayList<Vod>();

	public String getTitle()
	{
		return _title;
	}

	public void setTitle(String title)
	{
		_title = title;
	}

	public String getShortName()
	{
		return _shortName;
	}
	
	public void setShortName(String shortName)
	{
		_shortName = shortName;
	}
	
	public String getImage()
	{
		return _image;
	}
	
	public void setImage(String image)
	{
		_image = image;
	}
	
	public String getSubitems()
	{
		return _subitems;
	}
	
	public void setSubitems(String subitems)
	{
		_subitems = subitems;
	}
	
	public String getId()
	{
		return _id;
	}
	
	public void setId(String id)
	{
		_id = id;
	}
	
	public List<Vod> getVodList()
	{
		return _vodList;
	}
	
	public void setVodList(List<Vod> vodList)
	{
		_vodList = vodList;
	}
	
	public void addVod(Vod vod)
	{
		_vodList.add(vod);
	}
}

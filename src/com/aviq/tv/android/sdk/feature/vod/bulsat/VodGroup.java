package com.aviq.tv.android.sdk.feature.vod.bulsat;

import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class VodGroup implements Parcelable
{
	private String _title;
	private String _shortName;
	private String _image;
	private String _subitems;
	private String _id;
	private List<Vod> _vodList = new ArrayList<Vod>();

	public VodGroup()
	{}
	
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
	
	@Override
	public String toString() 
	{
		return "VodGroup [id=" + _id + ", title=" + _title + "]";
	}

	@Override
	public int hashCode() 
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_id == null) ? 0 : _id.hashCode());
		result = prime * result
				+ ((_vodList == null) ? 0 : _vodList.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) 
	{
		if (this == obj)
			return true;
		
		if (obj == null)
			return false;
		
		if (getClass() != obj.getClass())
			return false;
		
		VodGroup other = (VodGroup) obj;
		
		if (_id == null) 
		{
			if (other._id != null)
				return false;
		} 
		else if (!_id.equals(other._id))
			return false;
		
		if (_vodList == null) 
		{
			if (other._vodList != null)
				return false;
		} 
		else if (!_vodList.equals(other._vodList))
			return false;
		
		return true;
	}
	
	// Parcelable contract
	
	public VodGroup(Parcel in)
	{
		_title = in.readString();
		_shortName = in.readString();
		_image = in.readString();
		_subitems = in.readString();
		_id = in.readString();
		_vodList = new ArrayList<Vod>();
		in.readList(_vodList, _vodList.getClass().getClassLoader());
	}
	
	@Override
    public int describeContents()
	{
        return 0;
    }
	
	@Override
    public void writeToParcel(Parcel dest, int flags) 
    {
		dest.writeString(_title);
		dest.writeString(_shortName);
		dest.writeString(_image);
		dest.writeString(_subitems);
		dest.writeString(_id);
		dest.writeList(_vodList);
    }
    
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() 
    {
        public VodGroup createFromParcel(Parcel in) 
        {
            return new VodGroup(in); 
        }

        public VodGroup[] newArray(int size) 
        {
            return new VodGroup[size];
        }
    };
}

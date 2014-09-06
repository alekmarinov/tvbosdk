package com.aviq.tv.android.sdk.feature.vod.bulsat;

import android.os.Parcel;
import android.os.Parcelable;


public class Vod implements Parcelable
{
	private String _id;
	private String _title;
	private String _poster;
	private String _sources;
	
	public Vod()
	{}
	
	public String getId()
	{
		return _id;
	}
	
	public void setId(String id)
	{
		_id = id;
	}
	
	public String getTitle()
	{
		return _title;
	}

	public void setTitle(String title)
	{
		_title = title;
	}

	public String getPoster()
	{
		return _poster;
	}
	
	public void setPoster(String poster)
	{
		_poster = poster;
	}
	
	public String getSources()
	{
		return _sources;
	}
	
	public void setSources(String sources)
	{
		_sources = sources;
	}
	
	@Override
	public String toString() 
	{
		return "Vod [id=" + _id + ", title=" + _title + "]";
	}

	@Override
	public int hashCode() 
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_id == null) ? 0 : _id.hashCode());
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
		
		Vod other = (Vod) obj;
		
		if (_id == null) 
		{
			if (other._id != null)
				return false;
		} 
		else if (!_id.equals(other._id))
			return false;
		
		return true;
	}
	
	// Parcellable contract
	
	public Vod(Parcel in)
	{
		_id = in.readString();
		_title = in.readString();
		_poster = in.readString();
		_sources = in.readString();
	}
	
	@Override
    public int describeContents()
	{
        return 0;
    }
	
	@Override
    public void writeToParcel(Parcel dest, int flags) 
    {
		dest.writeString(_id);
		dest.writeString(_title);
		dest.writeString(_poster);
		dest.writeString(_sources);
    }
    
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() 
    {
        public Vod createFromParcel(Parcel in) 
        {
            return new Vod(in); 
        }

        public Vod[] newArray(int size) 
        {
            return new Vod[size];
        }
    };
}

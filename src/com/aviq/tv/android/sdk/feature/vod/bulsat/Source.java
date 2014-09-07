package com.aviq.tv.android.sdk.feature.vod.bulsat;

import android.os.Parcel;
import android.os.Parcelable;

public class Source implements Parcelable
{
	private String _type;
	private String _url;
	
	public Source()
	{}
	
	public String getType() 
	{
		return _type;
	}

	public void setType(String type) 
	{
		_type = type;
	}

	public String getUrl() 
	{
		return _url;
	}

	public void setUrl(String url) 
	{
		_url = url;
	}
	
	// Parcelable contract
	
	public Source(Parcel in)
	{
		_type = in.readString();
		_url = in.readString();
	}
	
	@Override
    public int describeContents()
	{
        return 0;
    }
	
	@Override
    public void writeToParcel(Parcel dest, int flags) 
    {
		dest.writeString(_type);
		dest.writeString(_url);
    }
    
    public static final Parcelable.Creator<Source> CREATOR = new Parcelable.Creator<Source>() 
    {
        public Source createFromParcel(Parcel in) 
        {
            return new Source(in); 
        }

        public Source[] newArray(int size) 
        {
            return new Source[size];
        }
    };
}

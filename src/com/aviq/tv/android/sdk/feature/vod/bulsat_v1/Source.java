package com.aviq.tv.android.sdk.feature.vod.bulsat_v1;

import android.os.Parcel;
import android.os.Parcelable;

public class Source implements Parcelable
{
	private String _type;
	private String _url;
	private boolean _3d;
	private boolean _4k;
	
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
	
	public void set3D(boolean is3D)
	{
		_3d = is3D;
	}
	
	public boolean is3D()
	{
		return _3d;
	}
	
	public void set4K(boolean is4K)
	{
		_4k = is4K;
	}
	
	public boolean is4K()
	{
		return _4k;
	}
	
	// Parcelable contract
	
	public Source(Parcel in)
	{
		_type = in.readString();
		_url = in.readString();
		
		boolean[] bArr = new boolean[2];
		in.readBooleanArray(bArr);
		_3d = bArr[0];
		_4k = bArr[1];
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
		dest.writeBooleanArray(new boolean[] {_3d, _4k});
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

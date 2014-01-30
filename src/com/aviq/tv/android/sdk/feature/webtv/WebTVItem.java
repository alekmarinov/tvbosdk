package com.aviq.tv.android.sdk.feature.webtv;

import java.util.ArrayList;
import java.util.List;

public class WebTVItem
{
	private String _id;
	private String _name;
	private String _description;
	private List<String> _genres = new ArrayList<String>();
	private List<String> _languages = new ArrayList<String>();
	private String _country;
	private String _logo;
	private String _resolutions;
	private String _media;
	private String _uri;

	public String getId()
	{
		return _id;
	}
	public void setId(String id)
	{
		this._id = id;
	}
	public String getName()
	{
		return _name;
	}
	public void setName(String name)
	{
		this._name = name;
	}
	public String getDescription()
	{
		return _description;
	}
	public void setDescription(String description)
	{
		this._description = description;
	}
	public List<String> getGenres()
	{
		return _genres;
	}
	public void setGenres(List<String> genres)
	{
		this._genres = genres;
	}
	public List<String> getLanguages()
	{
		return _languages;
	}
	public void setLanguages(List<String> languages)
	{
		this._languages = languages;
	}
	public String getCountry()
	{
		return _country;
	}
	public void setCountry(String country)
	{
		this._country = country;
	}
	public String getLogo()
	{
		return _logo;
	}
	public void setLogo(String logo)
	{
		this._logo = logo;
	}
	public String getResolutions()
	{
		return _resolutions;
	}
	public void setResolutions(String resolutions)
	{
		this._resolutions = resolutions;
	}
	public String getMedia()
	{
		return _media;
	}
	public void setMedia(String media)
	{
		this._media = media;
	}
	public String getUri()
	{
		return _uri;
	}
	public void setUri(String uri)
	{
		this._uri = uri;
	}
}

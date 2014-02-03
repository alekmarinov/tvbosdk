/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    WebTVItem.java
 * Author:      zheliazko
 * Date:        30 Jan 2014
 * Description: WebTV item bean
 */
package com.aviq.tv.android.sdk.feature.webtv;

import java.util.ArrayList;
import java.util.List;

/**
 * WebTV item bean
 */
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
		_id = id;
	}

	public String getName()
	{
		return _name;
	}

	public void setName(String name)
	{
		_name = name;
	}

	public String getDescription()
	{
		return _description;
	}

	public void setDescription(String description)
	{
		_description = description;
	}

	public List<String> getGenres()
	{
		return _genres;
	}

	public void setGenres(List<String> genres)
	{
		_genres = genres;
	}

	public List<String> getLanguages()
	{
		return _languages;
	}

	public void setLanguages(List<String> languages)
	{
		_languages = languages;
	}

	public String getCountry()
	{
		return _country;
	}

	public void setCountry(String country)
	{
		_country = country;
	}

	public String getLogo()
	{
		return _logo;
	}

	public void setLogo(String logo)
	{
		_logo = logo;
	}

	public String getResolutions()
	{
		return _resolutions;
	}

	public void setResolutions(String resolutions)
	{
		_resolutions = resolutions;
	}

	public String getMedia()
	{
		return _media;
	}

	public void setMedia(String media)
	{
		_media = media;
	}

	public String getUri()
	{
		return _uri;
	}

	public void setUri(String uri)
	{
		_uri = uri;
	}
}

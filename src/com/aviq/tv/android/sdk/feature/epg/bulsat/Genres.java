/**
 * Copyright (c) 2007-2015, Intelibo Ltd
 *
 * Project:     TVBOSDK
 * Filename:    Genres.java
 * Author:      alek
 * Date:        5 Jul 2015
 * Description: Bulsatcom channel genres manager
 */

package com.aviq.tv.android.sdk.feature.epg.bulsat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aviq.tv.android.sdk.R;
import com.aviq.tv.android.sdk.core.Environment;

/**
 * Bulsatcom channel genres manager
 */
public class Genres
{
	public static final String TAG = Genres.class.getSimpleName();
	private static Genres _instance;
	private List<Genre> _genres = new ArrayList<Genre>();
	private Map<String, Genre> _genresTitlesMap = new HashMap<String, Genre>();
	private Genre _defaultGenre;
	private String _defaultGenreTitle;

	private Genres()
	{
		_defaultGenreTitle = Environment.getInstance().getResources().getString(R.string.channel_category_default);
	}

	public static synchronized Genres getInstance()
	{
		if (_instance == null)
			_instance = new Genres();
		return _instance;
	}

	public List<Genre> getGenres()
	{
		return _genres;
	}

	public Genre getGenreByTitle(String title)
	{
		Genre genre = _genresTitlesMap.get(title);
		if (genre == null)
			genre = _defaultGenre;
		return genre;
	}

	public Genre getDefaultGenre()
	{
		return _defaultGenre;
	}

	void addGenre(Genre genre)
	{
		genre.setIndex(_genres.size());
		_genres.add(genre);
		_genresTitlesMap.put(genre.getTitle(), genre);
		if (_defaultGenreTitle.equals(genre.getTitle()))
			_defaultGenre = genre;
	}
}

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
import com.aviq.tv.android.sdk.core.Log;

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

	public Genres()
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

	public void addGenre(Genre genre)
	{
		addGenre(-1, genre);
	}

	public void addGenre(int index, Genre genre)
	{
		if (index < 0)
		{
			genre.setIndex(_genres.size());
			_genres.add(genre);
		}
		else
		{
			genre.setIndex(index);
			_genres.add(index, genre);
			for (int i = index + 1; i < _genres.size(); i++)
			{
				_genres.get(i).setIndex(_genres.get(i).getIndex() + 1);
			}
		}
		_genresTitlesMap.put(genre.getTitle(), genre);
		if (_defaultGenreTitle.equals(genre.getTitle()))
			_defaultGenre = genre;
	}

	public boolean isEqualTo(Genres otherGenres)
	{
		List<Genre> thisGenreList = new ArrayList<Genre>();
		List<Genre> otherGenreList = new ArrayList<Genre>();

		for (Genre genre: otherGenres.getGenres())
		{
			if (!genre.isHidden())
				otherGenreList.add(genre);
		}

		for (Genre genre: getGenres())
		{
			if (!genre.isHidden())
				thisGenreList.add(genre);
		}

		if (thisGenreList.size() != otherGenreList.size())
		{
			Log.i(TAG, "isEqualTo: Genres differ by count");
			return false;
		}

		for (int i = 0; i < thisGenreList.size(); i++)
		{
			if (!thisGenreList.get(i).getTitle().equals(otherGenreList.get(i).getTitle()))
				return false;
		}
		return true;
	}

	public void addAll(Genres otherGenres)
	{
		for (Genre genre: otherGenres.getGenres())
		{
			addGenre(genre);
		}
	}

	public void clear()
	{
		_genres.clear();
		_genresTitlesMap.clear();
		_defaultGenreTitle = null;
	}

	public boolean isEmpty()
	{
		return _genres.size() == 0;
	}
}

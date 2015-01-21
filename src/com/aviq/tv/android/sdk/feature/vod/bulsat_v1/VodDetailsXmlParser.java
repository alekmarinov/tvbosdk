package com.aviq.tv.android.sdk.feature.vod.bulsat_v1;

/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    VodXmlParser.java
 * Author:      Zhelyazko Zhelev
 * Date:        7 Aug 2014
 * Description: XML parser for VOD data
 */

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.net.Uri;

class VodDetailsXmlParser
{
	private static final String TAG = VodDetailsXmlParser.class.getSimpleName();

	private static final String TAG_VODLISTS = "vodlists";
	private static final String TAG_VOD = "vod";
	private static final String TAG_ID = "id";
	private static final String TAG_TITLE = "title";
	private static final String TAG_TITLE_ORG = "title_org";
	private static final String TAG_SHORT_NAME = "short_name";
	private static final String TAG_POSTER = "poster";
	private static final String TAG_SHORT_DESCRIPTION = "short_description";
	private static final String TAG_DESCRIPTION = "description";
	private static final String TAG_SUBITEMS = "subitems";
	private static final String TAG_VALID_FROM = "valid_from";
	private static final String TAG_RELEASE = "release";
	private static final String TAG_DURATION = "duration";
	private static final String TAG_IMDB_ID = "imdb_id";
	private static final String TAG_RATING = "rating";
	private static final String TAG_AUDIO_LANG = "audio_lang";
	private static final String TAG_SUBTITLES = "subtitles";
	private static final String TAG_VIDEO_TYPE = "video_type";
	private static final String TAG_AUDIO_TYPE = "audio_type";
	private static final String TAG_TRAILER_LINK = "trailer_link";
	private static final String TAG_COUNTRY = "country";
	private static final String TAG_COUNTRY_ID = "country_id";
	private static final String TAG_PG_ID = "pg_id";
	private static final String TAG_PG = "pg";
	private static final String TAG_GENRE_ID = "genre_id";
	private static final String TAG_GENRE = "genre";
	private static final String TAG_GENRES_ALL = "genres_all";
	private static final String TAG_GENRE_NAME_ALL = "genre_name_all";
	private static final String TAG_SOURCE = "source";

	private static final String ATTR_SOURCE_TYPE = "type";
	private static final String ATTR_SOURCE_IS3D = "is3D";
	private static final String ATTR_SOURCE_IS4K = "is4K";
	
	private SAXParser _saxParser;
	private XmlVodHandler _handler;

	private Boolean _currentElement = false;
	private StringBuffer _currentValue;
	private Vod _vod = null;
	private boolean _inVod = false;
	private Source _source = null;

	public void initialize() throws ParserConfigurationException, SAXException
	{
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		_saxParser = saxParserFactory.newSAXParser();
		_handler = new XmlVodHandler();
		_currentValue = new StringBuffer();
	}

	public Vod fromXML(String inputString) throws SAXException, IOException
	{
		_saxParser.parse(new InputSource(new StringReader(inputString)), _handler);
		return _handler.getResult();
	}

	private class XmlVodHandler extends DefaultHandler
	{
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
		{
			_currentElement = true;
			_currentValue.setLength(0);

			if (TAG_VOD.equals(localName))
			{
				_inVod = true;
				_vod = new Vod();
			}
			else if (TAG_SOURCE.equals(localName))
			{
				String type = attributes.getValue(ATTR_SOURCE_TYPE);
				String is3D = attributes.getValue(ATTR_SOURCE_IS3D);
				String is4K = attributes.getValue(ATTR_SOURCE_IS4K);
				
				_source = new Source();
				_source.setType(type);
				_source.set3D(is3D == null || is3D.toLowerCase().equals("false") ? false : true);
				_source.set4K(is4K == null || is4K.toLowerCase().equals("false") ? false : true);
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException
		{
			_currentElement = false;

			if (TAG_VOD.equals(localName))
			{
				_inVod = false;
			}
			else if (TAG_ID.equals(localName))
			{
				_vod.setId(_currentValue.toString());
			}
			else if (TAG_TITLE.equals(localName))
			{
				_vod.setTitle(_currentValue.toString());
			}
			else if (TAG_TITLE_ORG.equals(localName))
			{
				_vod.setTitleOrg(_currentValue.toString());
			}
			else if (TAG_SHORT_NAME.equals(localName))
			{
				_vod.setShortName(_currentValue.toString());
			}
			else if (TAG_POSTER.equals(localName))
			{
				_vod.setPoster(_currentValue.toString());
			}
			else if (TAG_SHORT_DESCRIPTION.equals(localName))
			{
				_vod.setShortDescription(_currentValue.toString());
			}
			else if (TAG_DESCRIPTION.equals(localName))
			{
				_vod.setDescription(_currentValue.toString());
			}
			else if (TAG_SUBITEMS.equals(localName))
			{
				_vod.setSubitems(_currentValue.toString());
			}
			else if (TAG_VALID_FROM.equals(localName))
			{
				_vod.setValidFrom(_currentValue.toString());
			}
			else if (TAG_RELEASE.equals(localName))
			{
				_vod.setRelease(_currentValue.toString());
			}
			else if (TAG_DURATION.equals(localName))
			{
				_vod.setDuration(_currentValue.toString());
			}
			else if (TAG_IMDB_ID.equals(localName))
			{
				_vod.setImdbId(_currentValue.toString());
			}
			else if (TAG_RATING.equals(localName))
			{
				_vod.setRating(_currentValue.toString());
			}
			else if (TAG_AUDIO_LANG.equals(localName))
			{
				_vod.setAudioLang(_currentValue.toString());
			}
			else if (TAG_SUBTITLES.equals(localName))
			{
				_vod.setSubtitles(_currentValue.toString());
			}
			else if (TAG_VIDEO_TYPE.equals(localName))
			{
				_vod.setVideoType(_currentValue.toString());
			}
			else if (TAG_AUDIO_TYPE.equals(localName))
			{
				_vod.setAudioType(_currentValue.toString());
			}
			else if (TAG_TRAILER_LINK.equals(localName))
			{
				_vod.setTrailerLink(_currentValue.toString());
			}
			else if (TAG_COUNTRY.equals(localName))
			{
				_vod.setCountry(_currentValue.toString());
			}
			else if (TAG_COUNTRY_ID.equals(localName))
			{
				_vod.setCountryId(_currentValue.toString());
			}
			else if (TAG_PG_ID.equals(localName))
			{
				_vod.setPgId(_currentValue.toString());
			}
			else if (TAG_PG.equals(localName))
			{
				_vod.setPg(_currentValue.toString());
			}
			else if (TAG_GENRE_ID.equals(localName))
			{
				_vod.setGenreId(_currentValue.toString());
			}
			else if (TAG_GENRE.equals(localName))
			{
				_vod.setGenre(_currentValue.toString());
			}
			else if (TAG_GENRES_ALL.equals(localName))
			{
				_vod.setGenresAll(_currentValue.toString());
			}
			else if (TAG_GENRE_NAME_ALL.equals(localName))
			{
				_vod.setGenreNameAll(_currentValue.toString());
			}
			else if (TAG_SOURCE.equals(localName))
			{
				_source.setUrl(Uri.decode(_currentValue.toString()));
				_vod.addSource(_source);
			}
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException
		{
			if (_currentElement)
			{
				_currentValue.append(ch, start, length);
			}
		}

		public Vod getResult()
		{
			return _vod;
		}
	}
}


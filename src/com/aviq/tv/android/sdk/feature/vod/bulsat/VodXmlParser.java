package com.aviq.tv.android.sdk.feature.vod.bulsat;

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

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader.ImageContainer;
import com.android.volley.toolbox.ImageLoader.ImageListener;
import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;

class VodXmlParser
{
	private static final String TAG = VodXmlParser.class.getSimpleName();

	private static final String TAG_VODLISTS = "vodlists";

	private static final String TAG_VODGROUP = "vodgroup";
	private static final String TAG_VOD = "vod";
	private static final String TAG_ID = "id";
	private static final String TAG_TITLE = "title";
	private static final String TAG_POSTER = "poster";
	private static final String TAG_SOURCE = "source";

	private SAXParser _saxParser;
	private XmlVodHandler _handler;

	private Boolean _currentElement = false;
	private StringBuffer _currentValue;
	private VodGroup _currentVodGroup = null;
	private boolean _inVodGroup = false;
	private Vod _currentVod = null;
	private boolean _inVod = false;
	private VodTree.Node<VodGroup> _currentNode;
	private VodTree<VodGroup> _vodTree;

	public void initialize() throws ParserConfigurationException, SAXException
	{
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		_saxParser = saxParserFactory.newSAXParser();
		_handler = new XmlVodHandler();
		_currentValue = new StringBuffer();
	}

	public VodTree<VodGroup> fromXML(String inputString) throws SAXException, IOException
	{
		_currentNode = null;
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

			if (TAG_VODLISTS.equals(localName))
			{
				VodGroup rootData = new VodGroup();
				rootData.setTitle("VOD");

				_vodTree = new VodTree<VodGroup>(rootData);
				_currentNode = _vodTree.getRoot();
			}
			else if (TAG_VODGROUP.equals(localName))
			{
				_inVodGroup = true;

				_currentVodGroup = new VodGroup();
				_currentNode = _currentNode.add(_currentVodGroup);
			}
			else if (TAG_VOD.equals(localName))
			{
				_inVod = true;
				_currentVod = new Vod();
				_currentVodGroup.addVod(_currentVod);
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException
		{
			_currentElement = false;

			if (TAG_VODGROUP.equals(localName))
			{
				_inVodGroup = false;
				_currentNode = _currentNode.getParent();
			}
			else if (TAG_VOD.equals(localName))
			{
				_inVod = false;
			}
			else if (TAG_ID.equals(localName))
			{
				if (_inVod)
					_currentVod.setId(_currentValue.toString());
				else if (_inVodGroup)
					_currentVodGroup.setId(_currentValue.toString());
			}
			else if (TAG_TITLE.equals(localName))
			{
				//Log.e(TAG, (_inVod ? "VOD: " : "GRP: ") + "title = " + _currentValue.toString());

				if (_inVod)
					_currentVod.setTitle(_currentValue.toString());
				else if (_inVodGroup)
					_currentVodGroup.setTitle(_currentValue.toString());
			}
			else if (TAG_POSTER.equals(localName))
			{
				_currentVod.setPoster(_currentValue.toString());

				// Fetch the image and cache it
				// FIXME: Alek, this causes NullPtrException
				// fetchVodPoster(_currentVod.getTitle(), _currentVod.getPoster());
			}
			else if (TAG_SOURCE.equals(localName))
			{
				_currentVod.setSources(Uri.decode(_currentValue.toString()));
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

		public VodTree<VodGroup> getResult()
		{
			return _vodTree;
		}
	}

	private void fetchVodPoster(final String title, final String posterUrl)
	{
		// Fetch the image and cache it
		Environment.getInstance().runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				// ImageLoader must be invoked from the main thread
				Environment.getInstance().getImageLoader().get(_currentVod.getPoster(), new ImageListener()
				{
					@Override
					public void onErrorResponse(VolleyError error)
					{
						Log.e(TAG, "Could not download poster for VOD [" + title
								+ "] from [" + posterUrl + "]", error);
					}

					@Override
					public void onResponse(ImageContainer response, boolean isImmediate)
					{
						Log.v(TAG, "Successful download of poster for VOD [" + title
								+ "] from [" + posterUrl + "]");
					}
				});
			}
		});
	}
}

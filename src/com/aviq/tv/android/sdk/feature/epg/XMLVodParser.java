package com.aviq.tv.android.sdk.feature.epg;

/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    XMLVodParser.java
 * Author:      Elmira Pavlova
 * Date:        30 Jul 2014
 * Description: XML Parser of VOD Data
 */

import java.io.IOException;
import java.io.StringReader;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

class XMLVodParser
{
	
	private SAXParser _saxParser;
	private XmlVodHandler _handler;
	private Stack<VodGroup> _stack;
	private VodGroup _vodData;
	private StringBuilder _content;
	
	private static final String VOD_GROUP_TAG = "vodgroup";
	private static final String VOD_ITEM_TAG = "vod";
	private static final String VOD_GROUP_NAME_TAG = "name";
	private static final String VOD_ITEM_TITLE_TAG = "title";
	private static final String VOD_ITEM_LOGO_TAG = "logo";
	private static final String VOD_ITEM_SOURCE_TAG = "sources";
	
	public void initialize() throws ParserConfigurationException, SAXException
	{
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		_saxParser = saxParserFactory.newSAXParser();
		_handler = new XmlVodHandler();
		_stack = new Stack<VodGroup>();
		_content = new StringBuilder();
		
	}
	
	public VodGroup fromXMl(String inputString) throws SAXException, IOException
	{
		_stack.clear();
		_vodData = new VodGroup();
		_vodData.setTitle("Videoteka");
		_stack.push(_vodData);
		_saxParser.parse(new InputSource(new StringReader(inputString)), _handler);
		return _handler.getResult();
	}
	
	private class XmlVodHandler extends DefaultHandler
	{
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
		{
			
			if (qName.equalsIgnoreCase(VOD_GROUP_TAG))
			{
				VodGroup vodElement = new VodGroup();
				_stack.push(vodElement);
			}
			else if (qName.equalsIgnoreCase(VOD_ITEM_TAG))
			{
				VodItem vodElement = new VodItem();
				_stack.push(vodElement);
			}
			else if (qName.equalsIgnoreCase(VOD_GROUP_NAME_TAG) || qName.equalsIgnoreCase(VOD_ITEM_TITLE_TAG)
			        || qName.equalsIgnoreCase(VOD_ITEM_LOGO_TAG) || qName.equalsIgnoreCase(VOD_ITEM_SOURCE_TAG))
			{
				_content.setLength(0);
				
			}
			
		}
		
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException
		{
			if (qName.equalsIgnoreCase(VOD_GROUP_TAG) || qName.equalsIgnoreCase(VOD_ITEM_TAG))
			{
				
				VodGroup child = _stack.pop();
				if (!_stack.isEmpty())
				{
					VodGroup parent = _stack.peek();
					parent.addItem(child);
				}
				
			}
			else if (qName.equalsIgnoreCase(VOD_GROUP_NAME_TAG) || qName.equalsIgnoreCase(VOD_ITEM_TITLE_TAG))
			{
				VodGroup item = _stack.peek();
				item.setTitle(_content.toString());
				
			}
			else if (qName.equalsIgnoreCase(VOD_ITEM_LOGO_TAG))
			{
				VodItem item = (VodItem) _stack.peek();
				item.setLogo(_content.toString());
				
			}
			else if (qName.equalsIgnoreCase(VOD_ITEM_SOURCE_TAG))
			{
				VodItem item = (VodItem) _stack.peek();
				item.setSource(_content.toString());
				
			}
		}
		
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException
		{
			_content.append(ch, start, length);
		}
		
		public VodGroup getResult()
		{
			// TODO Auto-generated method stub
			return _vodData;
		}
	}
}

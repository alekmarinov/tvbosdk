/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureVODBulsat.java
 * Author:      zhelyazko
 * Date:        3 Feb 2014
 * Description: Feature providing VOD data from Bulsatcom
 */
package com.aviq.tv.android.sdk.feature.vod.bulsat_v1;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Scheduler;
import com.aviq.tv.android.sdk.feature.vod.FeatureVODV1;
import com.aviq.tv.android.sdk.utils.MapUtils;
import com.aviq.tv.android.sdk.utils.MapUtils.SortingOrder;
import com.google.gson.JsonSyntaxException;

/**
 * Feature providing VOD data from Bulsatcom
 */
public class FeatureVODBulsat extends FeatureVODV1
{
	public static final String TAG = FeatureVODBulsat.class.getSimpleName();

	private static final int SCORE_TITLE = 1000;
	private static final int SCORE_SHORT_DESC = 900;
	private static final int SCORE_DESC = 800;

	private OnFeatureInitialized _onFeatureInitialized;
	private VodTree<VodGroup> _vodData;
	private Locale mLocale = new Locale("bg", "BG");
	protected RequestQueue _httpQueue;

	public static enum Param
	{
		VOD_XML_URL("http://api.iptv.bulsat.com/?xml&vod"),

		/**
		 * Schedule interval
		 */
		VOD_UPDATE_INTERVAL(24 * 60 * 60 * 1000);

		Param(String value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Scheduler.VOD).put(name(), value);
		}

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Scheduler.VOD).put(name(), value);
		}
	}

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		_httpQueue = Environment.getInstance().getRequestQueue();
		onSchedule(onFeatureInitialized);
	}

	@Override
	public Scheduler getSchedulerName()
	{
		return FeatureName.Scheduler.VOD;
	}

	@Override
	protected void onSchedule(OnFeatureInitialized onFeatureInitialized)
	{
		_onFeatureInitialized = onFeatureInitialized;

		String vodServerURL = getPrefs().getString(Param.VOD_XML_URL);
		Log.i(TAG, "Retrieving VOD data from: " + vodServerURL);

		VodListResponseCallback responseCallback = new VodListResponseCallback();

		@SuppressWarnings("rawtypes")
		VodRequest<VodTree> vodRequest = new VodRequest<VodTree>(Request.Method.GET, vodServerURL,
				VodTree.class, responseCallback, responseCallback);

		_httpQueue.add(vodRequest);

		scheduleDelayed(getPrefs().getInt(Param.VOD_UPDATE_INTERVAL));
	}

	@Override
    @SuppressWarnings("unchecked")
	public VodTree<VodGroup> getVodData(boolean removeEmptyElements)
	{
		if (removeEmptyElements)
			removeEmptyNodes(_vodData.getRoot());

		return _vodData;
	}

	@SuppressWarnings("unchecked")
	public void removeEmptyNodes(VodTree.Node<VodGroup> node)
	{
		if (node.hasChildren())
		{
			// Use array to avoid ConcurrentModificationException
			for (VodTree.Node<VodGroup> child : node.getChildren().toArray(new VodTree.Node[0]))
				removeEmptyNodes(child);
		}
		else
		{
			// Remove a node when it has no children and no leaves
			if (node.getData().getVodList().isEmpty())
			{
				node.getParent().remove(node.getData());
			}
		}

		// Check the current node in case there are no children left
		if (!node.hasChildren() && node.getData().getVodList().isEmpty() && node.getParent() != null)
		{
			node.getParent().remove(node.getData());
		}
	}

	@Override
    public void loadVod(String id, OnVodLoaded onVodLoadedListener)
	{
		String vodServerURL = getPrefs().getString(Param.VOD_XML_URL) + "=" + id;
		Log.i(TAG, "Retrieving VOD data from: " + vodServerURL);

		VodResponseCallback callback = new VodResponseCallback(onVodLoadedListener);

		StringRequest request = new StringRequest(Request.Method.GET, vodServerURL, callback, callback)
		{
			@Override
			public Map<String, String> getHeaders() throws AuthFailureError
			{
				Map<String, String> headers = new HashMap<String, String>();
				headers.put("Connection", "close");
				return headers;
			}
		};
		_httpQueue.add(request);
	}

	@Override
    public void search(String term, OnVodSearchResult onVodSearchResult)
	{
		String[] terms = term.split("\\s");
		List<String> keywordList = new ArrayList<String>(terms.length);
		for (String s : terms)
		{
			if (s.length() < 3)
				continue;
			keywordList.add(s.trim());
		}
		String[] keywords = keywordList.toArray(new String[] {});

		Log.v(TAG, "Run search with keywords = " + Arrays.toString(keywords));

		Map<Vod, Integer> resultMap = new HashMap<Vod, Integer>();
		searchVodTree(_vodData.getRoot(), keywords, resultMap);

		LinkedHashMap<Vod, Integer> sortedResultMap = MapUtils.sortMapByValue(resultMap, SortingOrder.DESCENDING);

		List<Vod> resultList = new ArrayList<Vod>();
		for (Map.Entry<Vod, Integer> entry : sortedResultMap.entrySet())
			resultList.add(entry.getKey());

		onVodSearchResult.onVodSearchResult(resultList);
	}

	/**
	 * Search VOD tree for VOD items and score each result based on which
	 * property it is found in.
	 *
	 * TODO: maybe add keyword frequency in the scoring as well
	 *
	 * @param node
	 * @param keywords
	 * @param resultMap
	 */
	public void searchVodTree(VodTree.Node<VodGroup> node, String[] keywords, Map<Vod, Integer> resultMap)
	{
		// searching: title, shortDescription, description

		List<Vod> vodList = node.getData().getVodList();
		for (Vod vod : vodList)
		{
			boolean hasKeywords = true;
			boolean inTitle = true;
			boolean inShortDesc = true;
			boolean inDesc = true;
			int score = 0;

			for (String keyword : keywords)
			{
				inTitle = vod.getTitle() != null ? vod.getTitle()
						.toLowerCase(mLocale).indexOf(keyword) > -1 : false;
				inShortDesc = vod.getShortDescription() != null ? vod
						.getShortDescription().toLowerCase(mLocale)
						.indexOf(keyword) > -1 : false;
				inDesc = vod.getDescription() != null ? vod.getDescription()
						.toLowerCase(mLocale).indexOf(keyword) > -1 : false;

				hasKeywords = inTitle || inShortDesc || inDesc;

				if (inTitle)
					score += SCORE_TITLE;
				if (inShortDesc)
					score += SCORE_SHORT_DESC;
				if (inDesc)
					score += SCORE_DESC;
			}

			if (hasKeywords)
				resultMap.put(vod, score);
		}

		if (node.hasChildren())
		{
			for (VodTree.Node<VodGroup> child : node.getChildren())
				searchVodTree(child, keywords, resultMap);
		}
	}

	/**
	 * Find all VODs under a VOD group, including any child group's VOD as well.
	 */
	@SuppressWarnings("unchecked")
	public static List<Vod>[] findVodsInVodGroup(VodTree.Node<VodGroup> vodData)
	{
		Log.i(TAG, ".findVodsInVodGroup: vodData.size = " + (vodData != null ? vodData.getChildren().size() : "null"));

		if (vodData == null)
			return null;

		int numVodGroups = vodData != null ? vodData.getChildren().size() : 0;
		if (numVodGroups == 0)
		{
			List<Vod>[] vodGroupList = new ArrayList[1];
			vodGroupList[0] = vodData.getData().getVodList();
			return vodGroupList;
		}
		else
		{
			List<Vod>[] vodGroupList = new ArrayList[numVodGroups];
			for (int i = 0; i < numVodGroups; i++)
			{
				vodGroupList[i] = new ArrayList<Vod>();
				recurseVodsForGroup(vodData.getChildAt(i), vodGroupList[i]);
			}
			return vodGroupList;
		}
	}

	private static void recurseVodsForGroup(VodTree.Node<VodGroup> node, List<Vod> vodList)
	{
		// Add any VODs from this node to the provided list
		if (node.getData().getVodList().size() > 0)
		{
			vodList.addAll(node.getData().getVodList());
		}

		// Recurse down the tree to discover more VODs
		if (node.hasChildren())
		{
			for (VodTree.Node<VodGroup> child : node.getChildren())
				recurseVodsForGroup(child, vodList);
		}
	}

	public static VodGroup findVodGroupOfVod(VodTree.Node<VodGroup> startNode, Vod vod)
	{
		if (startNode == null)
			return null;

		if (startNode.getData().getVodList().contains(vod))
			return startNode.getData();

		VodGroup vodGroup = null;
		for (VodTree.Node<VodGroup> child : startNode.getChildren())
		{
			vodGroup = findVodGroupOfVod(child, vod);
			if (vodGroup != null)
				return vodGroup;
		}

		return null;
	}

	private class VodRequest<T> extends Request<T>
	{
		private final Class<T> mClazz;
		private final Listener<T> mListener;

		public VodRequest(int method, String url, Class<T> clazz, Listener<T> listener, ErrorListener errorListener)
		{
			super(Method.GET, url, errorListener);
			this.mClazz = clazz;
			this.mListener = listener;
		}

		@Override
		public Map<String, String> getHeaders() throws AuthFailureError
		{
			Map<String, String> headers = new HashMap<String, String>();
			headers.put("Connection", "close");
			return headers;
		}

		@Override
		protected void deliverResponse(T response)
		{
			mListener.onResponse(response);
		}

		@Override
		protected Response<T> parseNetworkResponse(NetworkResponse response)
		{
			try
			{
				String inputString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));

				VodXmlParser xmlParser = new VodXmlParser();
				try
				{
					xmlParser.initialize();
				}
				catch (ParserConfigurationException e)
				{
					Log.e(TAG, "Cannot configure SAX parser.", e);
					return Response.error(new VolleyError(e));
				}
				catch (SAXException e)
				{
					Log.e(TAG, "SAX parser error.", e);
					return Response.error(new VolleyError(e));
				}

				T xml = mClazz.cast(xmlParser.fromXML(inputString));
				return Response.success(xml, HttpHeaderParser.parseCacheHeaders(response));
			}
			catch (UnsupportedEncodingException e)
			{
				return Response.error(new ParseError(e));
			}
			catch (JsonSyntaxException e)
			{
				return Response.error(new ParseError(e));
			}
			catch (SAXException e)
			{
				return Response.error(new ParseError(e));
			}
			catch (IOException e)
			{
				return Response.error(new ParseError(e));
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private class VodListResponseCallback implements Response.Listener<VodTree>, Response.ErrorListener
	{
		@SuppressWarnings("unchecked")
		@Override
		public void onResponse(VodTree response)
		{
			_vodData = response;

			//print(_vodData.getRoot()); // Dump tree data to logcat

			_onFeatureInitialized.onInitialized(FeatureError.OK(FeatureVODBulsat.this));
		}

		@Override
		public void onErrorResponse(VolleyError error)
		{
			Log.e(TAG, "Error retrieving VOD data: " + error);
			_onFeatureInitialized.onInitialized(new FeatureError(FeatureVODBulsat.this, error));
		}
	}

	private class VodResponseCallback implements Response.Listener<String>, Response.ErrorListener
	{
		private OnVodLoaded _callback;

		public VodResponseCallback(OnVodLoaded callback)
		{
			_callback = callback;
		}

		@Override
		public void onResponse(String response)
		{
			try
			{
				VodDetailsXmlParser xmlParser = new VodDetailsXmlParser();
				xmlParser.initialize();

				Vod vod = xmlParser.fromXML(response);

				if (_callback != null)
					_callback.onVodLoaded(vod);
			}
			catch (ParserConfigurationException e)
			{
				Log.e(TAG, "Cannot configure SAX parser.", e);
				if (_callback != null)
					_callback.onVodError(e);
			}
			catch (SAXException e)
			{
				Log.e(TAG, "SAX parser error.", e);
				if (_callback != null)
					_callback.onVodError(e);
			}
			catch (IOException e)
			{
				Log.e(TAG, "Error parsing XML.", e);
				if (_callback != null)
					_callback.onVodError(e);
			}
		}

		@Override
		public void onErrorResponse(VolleyError error)
		{
			Log.e(TAG, "Error retrieving VOD data: " + error);
			_onFeatureInitialized.onInitialized(new FeatureError(FeatureVODBulsat.this, error));
		}
	}

	//---------------------------------------------------
	// DEBUGGING CODE TO DUMP THE VOD TREE RECURSIVELY
	//---------------------------------------------------

	private int _treeDepth;

	private void print(VodTree.Node<VodGroup> node)
	{
		if (node.equals(_vodData.getRoot()))
			_treeDepth = 0;

		Log.v(TAG, "depth = " + _treeDepth
				+ ", title = " + node.getData().getTitle()
				+ ", num VODs = " + node.getData().getVodList().size());

		_treeDepth++;
		if (node.hasChildren())
		{
			for (VodTree.Node<VodGroup> child : node.getChildren())
				print(child);
		}
		_treeDepth--;
	}
}

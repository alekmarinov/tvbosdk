/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureWebTV.java
 * Author:      zheliazko
 * Date:        30 Jan 2014
 * Description: Component feature managing WebTV streams
 */

package com.aviq.tv.android.sdk.feature.webtv;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;

/**
 * Component feature managing WebTV streams
 */
public class FeatureWebTV extends FeatureComponent
{
	public static final String TAG = FeatureWebTV.class.getSimpleName();

	public enum Param
	{
		/**
		 * URL of file with video streams.
		 */
		CONTENT_URL("http://aviq.bg/test/content.json");

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.WEBTV).put(name(), value);
		}

		Param(String value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.WEBTV).put(name(), value);
		}
	}

	private ArrayList<WebTVItem> _videoStreams = new ArrayList<WebTVItem>();

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		loadVideoStreams(onFeatureInitialized);
	}

	/**
	 * @return list of video streams
	 */
	public ArrayList<WebTVItem> getVideoStreams()
	{
		return _videoStreams;
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.WEBTV;
	}

	private void loadVideoStreams(OnFeatureInitialized onFeatureInitialized)
	{
		String url = getPrefs().getString(Param.CONTENT_URL);
		Log.i(TAG, "Retrieving WebTV content " + url);
		WebTVResponseCallback responseCallback = new WebTVResponseCallback(onFeatureInitialized);

		JsonArrayRequest webTvContentRequest = new JsonArrayRequest(url, responseCallback, responseCallback);
		Environment.getInstance().getRequestQueue().add(webTvContentRequest);
	}

	private void parseContent(JSONArray response, List<WebTVItem> _owner) throws JSONException
	{
		for (int i = 0; i < response.length(); i++)
		{
			JSONObject item = response.getJSONObject(i);

			WebTVItem bean = new WebTVItem();
			bean.setId(item.getString("id"));
			bean.setName(item.getString("name"));
			bean.setDescription(item.getString("description"));

			List<String> genres = new ArrayList<String>();
			JSONArray genresArr = item.getJSONArray("genres");
			for (int j = 0; j < genresArr.length(); j++)
				genres.add(genresArr.getString(j));
			bean.setGenres(genres);

			List<String> languages = new ArrayList<String>();
			JSONArray langArr = item.getJSONArray("languages");
			for (int j = 0; j < langArr.length(); j++)
				languages.add(langArr.getString(j));
			bean.setLanguages(languages);

			bean.setCountry(item.getString("country"));
			bean.setLogo(item.getString("logo"));
			bean.setResolutions(item.getString("resolutions"));
			bean.setMedia(item.getString("media"));
			bean.setUri(item.getString("uri"));

			_owner.add(bean);
		}
	}

	private class WebTVResponseCallback implements Response.Listener<JSONArray>, Response.ErrorListener
	{
		private OnFeatureInitialized _onFeatureInitialized;

		WebTVResponseCallback(OnFeatureInitialized onFeatureInitialized)
		{
			_onFeatureInitialized = onFeatureInitialized;
		}

		@Override
		public void onResponse(JSONArray response)
		{
			Log.i(TAG, ".onResponse: num WebTV streams = " + response.length());
			try
			{
				parseContent(response, _videoStreams);
				_onFeatureInitialized.onInitialized(FeatureWebTV.this, ResultCode.OK);
			}
			catch (JSONException e)
			{
				Log.e(TAG, "Error parsing WebTV video streams.", e);
				_onFeatureInitialized.onInitialized(FeatureWebTV.this, ResultCode.GENERAL_FAILURE);
			}
		}

		@Override
		public void onErrorResponse(VolleyError error)
		{
			int statusCode = error.networkResponse != null ? error.networkResponse.statusCode
			        : ResultCode.GENERAL_FAILURE;
			Log.e(TAG, "Error retrieving WebTV video streams with code " + statusCode + ": " + error);
			_onFeatureInitialized.onInitialized(FeatureWebTV.this, statusCode);
		}
	}

	/**
	 * A request for retrieving a {@link JSONArray} response body at a given
	 * URL.
	 */
	private class JsonArrayRequest extends JsonRequest<JSONArray>
	{

		/**
		 * Creates a new request.
		 *
		 * @param url
		 *            URL to fetch the JSON from
		 * @param listener
		 *            Listener to receive the JSON response
		 * @param errorListener
		 *            Error listener, or null to ignore errors.
		 */
		public JsonArrayRequest(String url, Listener<JSONArray> listener, ErrorListener errorListener)
		{
			super(Method.GET, url, null, listener, errorListener);
		}

		@Override
		protected Response<JSONArray> parseNetworkResponse(NetworkResponse response)
		{
			try
			{
				String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
				return Response.success(new JSONArray(jsonString), HttpHeaderParser.parseCacheHeaders(response));
			}
			catch (UnsupportedEncodingException e)
			{
				return Response.error(new ParseError(e));
			}
			catch (JSONException je)
			{
				return Response.error(new ParseError(je));
			}
		}
	}
}

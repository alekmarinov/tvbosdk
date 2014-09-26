/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 * 
 * Project:     Bulsatcom
 * Filename:    FeatureWeather.java
 * Author:      Elmira
 * Date:        26.09.2014
 * Description: Provide weather data information
 */

package com.aviq.tv.android.sdk.feature.weather;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Scheduler;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureScheduler;
import com.aviq.tv.android.sdk.feature.internet.FeatureInternet;
import com.aviq.tv.android.sdk.feature.internet.FeatureInternet.ResultExtras;
import com.aviq.tv.android.sdk.feature.vod.bulsat.FeatureVODBulsat;

/**
 * @author Elmira
 */
public class FeatureWeather extends FeatureComponent
{
	public static final String TAG = FeatureVODBulsat.class.getSimpleName();
	private OnFeatureInitialized _onFeatureInitialized;
	private WeatherData _data;
	
	public static enum Param
	{
		WEATHER_QUERY_URL(
		        "https://query.yahooapis.com/v1/public/yql?q=${YAHOO_QUERY}&format=json&env=store://datatables.org/alltableswithkeys"),
		
		YAHOO_QUERY(
		        "select item.condition.code, item.condition.temp from weather.forecast where u='c' and  woeid in (select woeid from geo.placefinder where text=\"${LATITUDE},${LONGTITUDE}\" and gflags=\"R\")"),
		
		IMAGE_URL("https://s.yimg.com/zz/combo?a/i/us/we/52/${IMAGE_NAME}.gif"),
		/**
		 * Schedule interval
		 */
		WEATHER_UPDATE_INTERVAL(1 * 60 * 1000);
		
		Param(String value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.WEATHER).put(name(), value);
		}
		
		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.WEATHER).put(name(), value);
		}
	}
	
	public static enum QUERY_TAGS
	{
		query, results, channel, item, condition, temp, code
	}
	
	public FeatureWeather() throws FeatureNotFoundException
	{
		require(FeatureName.Scheduler.INTERNET);
		_data = new WeatherData();
	}
	
	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		_feature.Scheduler.INTERNET.getEventMessenger().register(this, FeatureInternet.ON_CONNECTED);
		_onFeatureInitialized = onFeatureInitialized;
		super.initialize(onFeatureInitialized);
	}
	
	@Override
	public void onEvent(int msgId, Bundle bundle)
	{
		super.onEvent(msgId, bundle);
		if (FeatureInternet.ON_CONNECTED == msgId)
		{
			try
			{
				String latitude = null;
				String longitude = null;
				_data = null;
				String content = bundle.getString(FeatureInternet.ResultExtras.CONTENT.name());
				JSONObject obj;
				obj = new JSONObject(content);
				String latitudeName = ResultExtras.LATITUDE.name().toLowerCase();
				String longitudeName = ResultExtras.LONGITUDE.name().toLowerCase();
				if (!obj.isNull(latitudeName) && (!obj.isNull(longitudeName)))
				{
					latitude = Double.toString(obj.getDouble(latitudeName));
					longitude = Double.toString(obj.getDouble(longitudeName));
					
					WeatherResponseCallback responseCallback = new WeatherResponseCallback();
					Bundle queryParams = new Bundle();
					queryParams.putString("LONGTITUDE", longitude);
					queryParams.putString("LATITUDE", latitude);
					String query = getPrefs().getString(Param.YAHOO_QUERY, queryParams);					
					query = URLEncoder.encode(query, "UTF-8").replaceAll("\\+", "%20");
					Bundle queryUrlParams = new Bundle();
					queryUrlParams.putString("YAHOO_QUERY", query);
					String weatherServerURL = getPrefs().getString(Param.WEATHER_QUERY_URL, queryUrlParams);
					Log.i(TAG, "Retrieving weather data from: " + weatherServerURL);
					
					JsonObjectRequest weatherContentRequest = new JsonObjectRequest(Request.Method.GET,
					        weatherServerURL, null, responseCallback, responseCallback);
					Environment.getInstance().getRequestQueue().add(weatherContentRequest);
				}				
			}
			catch (JSONException e)
			{
				Log.e(TAG, e.getMessage());
			}
			catch (UnsupportedEncodingException e)
			{
				
				Log.e(TAG, e.getMessage());
			}
		}
	}
	
	private void parseContent(JSONObject response) throws JSONException
	{
		for (int id = 0; id < QUERY_TAGS.values().length - 2; id++)
		{
			if(response.isNull(QUERY_TAGS.values()[id].name()))
				return;
			response = response.getJSONObject(QUERY_TAGS.values()[id].name());
		}
		_data = new WeatherData();
		_data.setImgCode(response.getInt(QUERY_TAGS.code.name()));
		_data.setTemp(response.getInt(QUERY_TAGS.temp.name()));
	}
	
	private class WeatherResponseCallback implements Response.Listener<JSONObject>, Response.ErrorListener
	{
		@Override
		public void onResponse(JSONObject response)
		{
			try
			{
				parseContent(response);
				_onFeatureInitialized.onInitialized(FeatureError.OK(FeatureWeather.this));
			}
			catch (JSONException e)
			{
				Log.e(TAG, "Error parsing weather data", e);
				_onFeatureInitialized
				        .onInitialized(new FeatureError(FeatureWeather.this, ResultCode.PROTOCOL_ERROR, e));
			}
		}
		
		@Override
		public void onErrorResponse(VolleyError error)
		{
			Log.e(TAG, "Error retrieving weather data with code: " + error);
			_onFeatureInitialized.onInitialized(new FeatureError(FeatureWeather.this, error));
		}
	}
	
	public class WeatherData
	{
		private int _imgCode;
		private int _temp;
		
		public WeatherData(int imgCode, int temp)
		{
			_imgCode = imgCode;
			_temp = temp;
		}
		
		public WeatherData()
		{
			// TODO Auto-generated constructor stub
		}
		
		private void setImgCode(int i)
		{
			_imgCode = i;
		}
		
		private void setTemp(int temp)
		{
			_temp = temp;
		}
		
		public int getImgCode()
		{
			return _imgCode;
		}
		
		public String getImgURL()
		{
			Bundle queryParams = new Bundle();
			queryParams.putInt("IMAGE_NAME", _imgCode);
			String imgURL = getPrefs().getString(Param.IMAGE_URL, queryParams);
			return imgURL;
		}
		
		public int getTemp()
		{
			return _temp;
		}
		
	}
	
	public WeatherData getWeatherData()
	{
		return _data;
	}
	
	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.WEATHER;
	}
}

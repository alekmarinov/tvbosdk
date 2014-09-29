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
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.feature.internet.FeatureInternet;
import com.aviq.tv.android.sdk.feature.internet.FeatureInternet.ResultExtras;
import com.aviq.tv.android.sdk.feature.rcu.FeatureRCU;
import com.aviq.tv.android.sdk.feature.system.FeatureDevice.UserParam;
import com.aviq.tv.android.sdk.feature.vod.bulsat.FeatureVODBulsat;

/**
 * @author Elmira
 */
public class FeatureWeather extends FeatureComponent
{
	public static final String TAG = FeatureVODBulsat.class.getSimpleName();
	public static final int ON_WEATHER_CHANGE = EventMessenger.ID("ON_WEATHER_CHANGE");	
	private WeatherData _data = new WeatherData();
	private long _lastSendTime;
	private int _updateInterval;
	
	public static enum Param
	{
		WEATHER_QUERY_URL(
		        "https://query.yahooapis.com/v1/public/yql?q=${YAHOO_QUERY}&format=json&env=store://datatables.org/alltableswithkeys"),
		
		YAHOO_QUERY(
		        "select item.condition.code, item.condition.temp from weather.forecast where u='c' and  woeid in (select woeid from geo.placefinder where text=\"${LATITUDE},${LONGTITUDE}\" and gflags=\"R\")"),
		
		IMAGE_URL("https://s.yimg.com/zz/combo?a/i/us/we/52/${IMAGE_NAME}.gif"),
		/**
		 * Update interval in secunds
		 */
		WEATHER_UPDATE_INTERVAL(60);
		
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
		_lastSendTime = 0;
		_updateInterval = this.getPrefs().getInt(Param.WEATHER_UPDATE_INTERVAL);
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
				long deltaInSeconds = (System.currentTimeMillis() - _lastSendTime) / 1000;
				if (deltaInSeconds > _updateInterval)
				{
					_lastSendTime = System.currentTimeMillis();
					String latitude = null;
					String longitude = null;
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
			if (response.isNull(QUERY_TAGS.values()[id].name()))
			{
				Log.w(TAG, "Tag " + QUERY_TAGS.values()[id].name() + " missing");
				return;
			}
			response = response.getJSONObject(QUERY_TAGS.values()[id].name());
		}
		
		if (!response.isNull(QUERY_TAGS.code.name()) && (!response.isNull(QUERY_TAGS.temp.name())))
		{
			int code = response.getInt(QUERY_TAGS.code.name());
			int temp = response.getInt(QUERY_TAGS.temp.name());			
			_data.setImgCode(code);
			_data.setTemp(temp);
			if(_data.isChanged())
				getEventMessenger().trigger(ON_WEATHER_CHANGE);	
		}
		else
		{
			Log.w(TAG, "Tag code or temp missing");
		}
		
	}
	
	private class WeatherResponseCallback implements Response.Listener<JSONObject>, Response.ErrorListener
	{
		@Override
		public void onResponse(JSONObject response)
		{
			try
			{
				parseContent(response);
			}
			catch (JSONException e)
			{
				Log.e(TAG, "Error parsing weather data", e);
			}
		}
		
		@Override
		public void onErrorResponse(VolleyError error)
		{
			Log.e(TAG, "Error retrieving weather data with code: " + error);
		}
	}
	
	public class WeatherData
	{
		private int _imgCode;
		private int _temp;
		private boolean _isChanged;
		
		public WeatherData(int imgCode, int temp)
		{
			setTemp(temp);
			setImgCode(imgCode);
		}
		
		public WeatherData()
		{
			// TODO Auto-generated constructor stub
		}
		
		private void setImgCode(int i)
		{
			if (i != _imgCode) 
			{
				_isChanged = true;			
				_imgCode = i;
			}
		}
		
		private void setTemp(int temp)
		{
			if (temp != _temp) 
			{
				_isChanged = true;			
				_temp = temp;
			}
		}
		
		public int getImgCode()
		{
			_isChanged = false;
			return _imgCode;
		}

		public boolean isChanged()
		{
			return _isChanged;
		}
		
		public String getImgURL()
		{
			Bundle queryParams = new Bundle();
			queryParams.putInt("IMAGE_NAME", _imgCode);
			String imgURL = getPrefs().getString(Param.IMAGE_URL, queryParams);
			_isChanged = false;
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

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
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Scheduler;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.FeatureScheduler;
import com.aviq.tv.android.sdk.feature.internet.FeatureInternet;

/**
 * @author Elmira
 */
public class FeatureWeather extends FeatureScheduler
{
	public static final String TAG = FeatureWeather.class.getSimpleName();
	public static final int ON_WEATHER_CHANGE = EventMessenger.ID("ON_WEATHER_CHANGE");
	private WeatherData _weatherData;

	public static enum Param
	{
		/**
		 *  Weather update interval
		 */
		UPDATE_INTERVAL(60000),

		WEATHER_QUERY_URL(
		        "https://query.yahooapis.com/v1/public/yql?q=${YAHOO_QUERY}&format=json&env=store://datatables.org/alltableswithkeys"),

		YAHOO_QUERY(
		        "select item.condition.code, item.condition.temp from weather.forecast where u='c' and  woeid in (select woeid from geo.placefinder where text=\"${LATITUDE},${LONGTITUDE}\" and gflags=\"R\")"),

		IMAGE_URL("https://s.yimg.com/zz/combo?a/i/us/we/52/${IMAGE_NAME}.gif");

		Param(String value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Scheduler.WEATHER).put(name(), value);
		}

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Scheduler.WEATHER).put(name(), value);
		}
	}

	public static enum QUERY_TAGS
	{
		query, results, channel, item, condition, temp, code
	}

	public FeatureWeather() throws FeatureNotFoundException
	{
		require(FeatureName.Scheduler.INTERNET);
	}

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		_feature.Scheduler.INTERNET.getEventMessenger().register(this, FeatureInternet.ON_GEOIP);
		Environment.getInstance().getEventMessenger().register(this, Environment.ON_LOADED);
		super.initialize(onFeatureInitialized);
	}

	@Override
	public void onEvent(int msgId, Bundle bundle)
	{
		super.onEvent(msgId, bundle);
		if (FeatureInternet.ON_GEOIP == msgId || Environment.ON_LOADED == msgId)
		{
			getEventMessenger().trigger(ON_SCHEDULE);
		}
	}

	@Override
	public void onSchedule(final OnFeatureInitialized onFeatureInitialized)
	{
		checkWeather();
		scheduleDelayed(getPrefs().getInt(Param.UPDATE_INTERVAL));
	}

	public void checkWeather()
	{
		Bundle geoIp = _feature.Scheduler.INTERNET.getGeoIP();
		if (geoIp == null)
		{
			Log.d(TAG, "GeoIP is not available yet");
			return ;
		}
		WeatherResponseCallback responseCallback = new WeatherResponseCallback();
		double longitude = geoIp.getDouble(FeatureInternet.GeoIpExtras.longitude.name(), -1.0);
		double latitude = geoIp.getDouble(FeatureInternet.GeoIpExtras.latitude.name(), -1.0);
		Log.i(TAG, ".retrieveWeather: longitude = " + longitude + ", latitude = " + latitude);
		Bundle queryParams = new Bundle();
		queryParams.putString("LONGTITUDE", String.valueOf(longitude));
		queryParams.putString("LATITUDE", String.valueOf(latitude));
		String query = getPrefs().getString(Param.YAHOO_QUERY, queryParams);
		try
		{
			query = URLEncoder.encode(query, "UTF-8").replaceAll("\\+", "%20");
			Bundle queryUrlParams = new Bundle();
			queryUrlParams.putString("YAHOO_QUERY", query);
			String weatherServerURL = getPrefs().getString(Param.WEATHER_QUERY_URL, queryUrlParams);
			Log.i(TAG, "Retrieving weather data from: " + weatherServerURL);

			JsonObjectRequest weatherContentRequest = new JsonObjectRequest(Request.Method.GET, weatherServerURL,
			        null, responseCallback, responseCallback)
			{
				@Override
				public Map<String, String> getHeaders() throws AuthFailureError
				{
					Map<String, String> headers = new HashMap<String, String>();
					headers.put("Connection", "close");
					return headers;
				}
			};
			Environment.getInstance().getRequestQueue().add(weatherContentRequest);
		}
		catch (UnsupportedEncodingException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
	}

	public WeatherData getWeatherData()
	{
		return _weatherData;
	}

	@Override
	public Scheduler getSchedulerName()
	{
		return FeatureName.Scheduler.WEATHER;
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
			int temperature = response.getInt(QUERY_TAGS.temp.name());
			if (_weatherData == null)
				_weatherData = new WeatherData();
			_weatherData.setImgCode(code);
			_weatherData.setTemperature(temperature);
			getEventMessenger().trigger(ON_WEATHER_CHANGE);
		}
		else
		{
			Log.w(TAG, "Tag code or temperature missing");
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
		private int _temperature;

		private void setImgCode(int i)
		{
			_imgCode = i;
		}

		private void setTemperature(int temp)
		{
			_temperature = temp;
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

		public int getTemperature()
		{
			return _temperature;
		}
	}
}

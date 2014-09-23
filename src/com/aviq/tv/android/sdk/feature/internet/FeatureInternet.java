/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureInternet.java
 * Author:      alek
 * Date:        3 Dec 2013
 * Description: Feature managing internet connectivity
 */

package com.aviq.tv.android.sdk.feature.internet;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureScheduler;
import com.aviq.tv.android.sdk.core.service.ServiceController;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Feature managing Internet access
 */
public class FeatureInternet extends FeatureScheduler
{
	private static final String TAG = FeatureInternet.class.getSimpleName();

	public static final int ON_CONNECTED = EventMessenger.ID("ON_CONNECTED");
	public static final int ON_DISCONNECTED = EventMessenger.ID("ON_DISCONNECTED");

	public enum Param
	{
		/**
		 * Check URL interval in seconds
		 */
		CHECK_INTERVAL(60),

		/**
		 * Check URL attempts number
		 */
		CHECK_ATTEMPTS(6),

		/**
		 * Timeout in seconds to attempt checking
		 */
		CHECK_ATTEMPTS_TIMEOUT(30000),

		/**
		 * Delay between internet check attempts
		 */
		CHECK_ATTEMPT_DELAY(4000),

		/** URL to check against for the box's geoip information */
		GEOIP_URL("http://freegeoip.net/json"),

		/** Backup URL to check against for the box's geoip information */
		GEOIP_URL_BACKUP("http://www.telize.com/geoip");

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Scheduler.INTERNET).put(name(), value);
		}

		Param(String value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Scheduler.INTERNET).put(name(), value);
		}
	}

	public enum ResultExtras
	{
		URL, CONTENT, ERROR_MESSAGE, ERROR_CODE, PUBLIC_IP, LATITUDE, LONGITUDE, CITY, COUNTRY, REGION, ISP
	}

	public enum GeoIpExtras
	{
		PUBLIC_IP, LATITUDE, LONGITUDE, CITY, COUNTRY, REGION, ISP
	}

	// FIXME: the public ip must be obtained by ON_CONNECTED event
	@Deprecated
	private String _publicIP;

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
//		checkInternet(new OnResultReceived()
//		{
//			@Override
//			public void onReceiveResult(int resultCode, Bundle resultData)
//			{
//				Log.i(TAG,
//				        ".initialize:onReceiveResult: resultCode = " + resultCode + " ("
//				                + TextUtils.implodeBundle(resultData) + ")");
//				onFeatureInitialized.onInitialized(FeatureInternet.this, resultCode);
				getEventMessenger().trigger(ON_SCHEDULE);
//			}
//		});

		super.initialize(onFeatureInitialized);
	}

	@Override
	public void onSchedule(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".onSchedule");
		checkInternet(new OnResultReceived()
		{
			@Override
			public void onReceiveResult(int resultCode, Bundle resultData)
			{
				Log.i(TAG,
				        ".onSchedule:onReceiveResult: resultCode = " + resultCode + " ("
				                + TextUtils.implodeBundle(resultData) + ")");
				getEventMessenger().trigger(resultCode == ResultCode.OK ? ON_CONNECTED : ON_DISCONNECTED, resultData);
				scheduleDelayed(getPrefs().getInt(Param.CHECK_INTERVAL) * 1000);
			}
		});
	}

	@Override
	public FeatureName.Scheduler getSchedulerName()
	{
		return FeatureName.Scheduler.INTERNET;
	}

	/**
	 * Checks Internet access by attempt to retrieve the GeoIP information
	 *
	 * @param onResultReceived
	 *            result callback interface
	 */
	public void checkInternet(final OnResultReceived onResultReceived)
	{
		class InternetCheckResponse implements OnResultReceived, Runnable
		{
			private int _attemptsCounter = 0;
			private int _checkAttempts = getPrefs().getInt(Param.CHECK_ATTEMPTS);
			private long _checkStart = System.currentTimeMillis();

			@Override
			public void run()
			{
				if (_attemptsCounter == 0)
					getUrlContent(getPrefs().getString(Param.GEOIP_URL), this);
				else
					getUrlContent(getPrefs().getString(Param.GEOIP_URL_BACKUP), this);
			}

			@Override
			public void onReceiveResult(int resultCode, Bundle resultData)
			{
				if (resultCode != ResultCode.OK)
				{
					long timeElapsed = System.currentTimeMillis() - _checkStart;
					if (_attemptsCounter < _checkAttempts
					        || (timeElapsed < getPrefs().getInt(Param.CHECK_ATTEMPTS_TIMEOUT)))
					{
						_attemptsCounter++;
						Log.w(TAG, "Check internet failed. Trying " + (_checkAttempts - _attemptsCounter + 1)
						        + " more times");
						getEventMessenger().postDelayed(this, getPrefs().getInt(Param.CHECK_ATTEMPT_DELAY));
						return;
					}
				}
				else
				{
					String content = resultData.getString(ResultExtras.CONTENT.name());
					try
					{
						JSONObject obj = new JSONObject(content);
						if (!obj.isNull("ip"))
						{
							_publicIP = obj.getString("ip");
							resultData.putString(ResultExtras.PUBLIC_IP.name(), _publicIP);
						}
						if (!obj.isNull("latitude"))
							resultData.putDouble(ResultExtras.LATITUDE.name(), obj.getDouble("latitude"));
						if (!obj.isNull("longitude"))
							resultData.putDouble(ResultExtras.LONGITUDE.name(), obj.getDouble("longitude"));
						if (!obj.isNull("city"))
							resultData.putString(ResultExtras.CITY.name(), obj.getString("city"));
						if (!obj.isNull("country"))
							resultData.putString(ResultExtras.COUNTRY.name(), obj.getString("country"));
						else if (!obj.isNull("country_name"))
							resultData.putString(ResultExtras.COUNTRY.name(), obj.getString("country_name"));
						if (!obj.isNull("region"))
							resultData.putString(ResultExtras.REGION.name(), obj.getString("region"));
						else if (!obj.isNull("region_name"))
							resultData.putString(ResultExtras.REGION.name(), obj.getString("region_name"));
						if (!obj.isNull("isp"))
							resultData.putString(ResultExtras.ISP.name(), obj.getString("isp"));
					}
					catch (JSONException e)
					{
						Log.e(TAG, e.getMessage(), e);
					}
				}
				onResultReceived.onReceiveResult(resultCode, resultData);
			}
		}
		getEventMessenger().post(new InternetCheckResponse());
	}

	/**
	 * Get content from Url with custom headers
	 *
	 * @param url
	 *            the url to get content from
	 * @param headers
	 *            a hashmap with custom headers
	 * @param onResultReceived
	 *            result callback interface
	 */
	public void getUrlContent(String url, final Map<String, String> headers, OnResultReceived onResultReceived)
	{
		Log.i(TAG, ".getUrlContent: " + url);
		CheckResponse responseSuccess = new CheckResponse(url, onResultReceived);
		CheckResponse responseError = new CheckResponse(url, onResultReceived);
		StringRequest stringRequest = new StringRequest(url, responseSuccess, responseError)
		{
			@Override
			public Map<String, String> getHeaders() throws AuthFailureError
			{
				return headers;
			}

			@Override
			protected Response<String> parseNetworkResponse(NetworkResponse response)
			{
				return super.parseNetworkResponse(response);
			}
		};
		Environment.getInstance().getRequestQueue().add(stringRequest);
	}

	/**
	 * Get content from Url
	 *
	 * @param url
	 *            the url to get content from
	 * @param onResultReceived
	 *            result callback interface
	 */
	public void getUrlContent(String url, OnResultReceived onResultReceived)
	{
		getUrlContent(url, new HashMap<String, String>(), onResultReceived);
	}

	/**
	 * Get headers from Url
	 *
	 * @param url
	 *            the url to get headers from
	 * @param onResultReceived
	 *            result callback interface receiving bundle with all response
	 *            header fields
	 */
	public void getUrlHeaders(final String url, final OnResultReceived onResultReceived)
	{
		Log.i(TAG, ".getUrlHeaders: " + url);
		StringRequest stringRequest = new StringRequest(Request.Method.HEAD, url, null, null)
		{
			@Override
			protected Response<String> parseNetworkResponse(NetworkResponse response)
			{
				Bundle resultData = new Bundle();
				for (String key : response.headers.keySet())
				{
					resultData.putString(key, response.headers.get(key));
				}
				onResultReceived.onReceiveResult(response.statusCode, resultData);
				return super.parseNetworkResponse(response);
			}
		};
		Environment.getInstance().getRequestQueue().add(stringRequest);
	}

	/**
	 * Download file
	 *
	 * @param params
	 *            Bundle with various download options. See
	 *            DownloadService.Extras
	 * @param onResultReceived
	 */
	public void downloadFile(Bundle params, final OnResultReceived onResultReceived)
	{
		Log.i(TAG, ".downloadFile: " + TextUtils.implodeBundle(params));
		ServiceController serviceController = Environment.getInstance().getServiceController();
		serviceController.startService(DownloadService.class, params).then(onResultReceived);
	}

	/**
	 * Upload file
	 *
	 * @param params
	 *            Bundle with various upload options. See
	 *            UploadService.Extras
	 * @param onResultReceived
	 */
	public void uploadFile(Bundle params, final OnResultReceived onResultReceived)
	{
		Log.i(TAG, ".uploadFile: " + TextUtils.implodeBundle(params));
		ServiceController serviceController = Environment.getInstance().getServiceController();
		serviceController.startService(UploadService.class, params).then(onResultReceived);
	}

	public void stopFileDownload()
	{
		Log.i(TAG, ".stopFileDownload");
		DownloadService.stopIfRunning();
	}

	/**
	 * Url check response handler
	 */
	private class CheckResponse implements Listener<String>, ErrorListener
	{
		private OnResultReceived _onResultReceived;
		private String _url;

		CheckResponse(String url, OnResultReceived onResultReceived)
		{
			_url = url;
			_onResultReceived = onResultReceived;
		}

		/**
		 * Response success
		 */
		@Override
		public void onResponse(String response)
		{
			Log.i(TAG, _url + " -> " + response.substring(0, Math.min(10, response.length() - 1)));
			Bundle resultData = new Bundle();
			resultData.putString(ResultExtras.URL.name(), _url);
			resultData.putString(ResultExtras.CONTENT.name(), response);
			_onResultReceived.onReceiveResult(ResultCode.OK, resultData);
		}

		/**
		 * Response failure
		 */
		@Override
		public void onErrorResponse(VolleyError error)
		{
			int statusCode = ResultCode.GENERAL_FAILURE;
			StringBuffer headerInfo = new StringBuffer();
			if (error.networkResponse != null)
			{
				statusCode = error.networkResponse.statusCode;
				for (String key : error.networkResponse.headers.keySet())
				{
					String value = error.networkResponse.headers.get(key);
					headerInfo.append(key).append('=').append(value).append('\n');
				}
			}
			Log.e(TAG, _url + " -> " + statusCode + " ( " + error.getMessage() + "), header = {" + headerInfo + "}");
			Bundle resultData = new Bundle();
			resultData.putString(ResultExtras.URL.name(), _url);
			resultData.putString(ResultExtras.ERROR_MESSAGE.name(), error.getMessage());
			resultData.putInt(ResultExtras.ERROR_CODE.name(), statusCode);
			_onResultReceived.onReceiveResult(statusCode, resultData);
		}
	}

	// FIXME: the public ip must be obtained by ON_CONNECTED event
	@Deprecated
	public String getPublicIP()
	{
		return _publicIP;
	}
}

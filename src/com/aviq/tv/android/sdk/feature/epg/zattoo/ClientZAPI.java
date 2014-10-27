/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    ClientZAPI.java
 * Author:      alek
 * Date:        23 Jun 2014
 * Description: Zattoo API http client
 */

package com.aviq.tv.android.sdk.feature.epg.zattoo;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;

/**
 * Zattoo API http client
 */
public class ClientZAPI
{
	private static final String TAG = ClientZAPI.class.getSimpleName();
	public static final String EXTRA_ERROR = "ERROR";
	public static final String EXTRA_URL = "URL";
	public static final String EXTRA_STOP_URL = "STOP_URL";
	private String _baseUri;
	private String _cookie;
	private int _minRate;
	private int _initRate;
	private RequestQueue _requestQueue;

	public ClientZAPI(String baseUri, int minRate, int initRate)
	{
		_minRate = minRate;
		_initRate = initRate;
		_baseUri = baseUri;
		_requestQueue = Volley.newRequestQueue(Environment.getInstance(), new ExtHttpClientStack(new SslHttpClient()));
	}

	public void hello(final String appid, final String uuid, OnResultReceived onResultReceived)
	{
		ResponseCallback responseCallbackHello = new ResponseCallback(onResultReceived);
		StringRequest stringRequest = new StringRequest(Request.Method.POST, _baseUri + "/zapi/session/hello",
		        responseCallbackHello, responseCallbackHello)
		{
			@Override
			protected Map<String, String> getParams() throws AuthFailureError
			{
				Map<String, String> params = new HashMap<String, String>();
				params.put("app_tid", appid);
				params.put("uuid", uuid);
				params.put("lang", "en");
				params.put("format", "json");
				return params;
			}

			@Override
			protected Response<String> parseNetworkResponse(NetworkResponse response)
			{
				Map<String, String> responseHeaders = response.headers;
				_cookie = responseHeaders.get("set-cookie");
				Log.i(TAG, "hello::_cookie = " + _cookie);
				return super.parseNetworkResponse(response);
			}
		};
        try
        {
	       String body = new String(stringRequest.getPostBody());
			Log.i(TAG, "call " + stringRequest.getUrl() + " [" + body + "]");
			_requestQueue.add(stringRequest);
        }
        catch (AuthFailureError e)
        {
	        Log.e(TAG, e.getMessage(), e);
        }
	}

	public void login(final String username, final String password, OnResultReceived onResultReceived)
	{
		ResponseCallback responseCallback = new ResponseCallback(onResultReceived);
		StringRequest stringRequest = new StringRequest(Request.Method.POST, _baseUri + "/zapi/account/login",
		        responseCallback, responseCallback)
		{
			@Override
			protected Map<String, String> getParams() throws AuthFailureError
			{
				Map<String, String> params = new HashMap<String, String>();
				params.put("login", username);
				params.put("password", password);
				return params;
			}

			@Override
			protected Response<String> parseNetworkResponse(NetworkResponse response)
			{
				Map<String, String> responseHeaders = response.headers;
				_cookie = responseHeaders.get("set-cookie");
				Log.i(TAG, "login::_cookie = " + _cookie);
				return super.parseNetworkResponse(response);
			}
		};
        try
        {
	       String body = new String(stringRequest.getPostBody());
			Log.i(TAG, "call " + stringRequest.getUrl() + " [" + body + "]");
			_requestQueue.add(stringRequest);
        }
        catch (AuthFailureError e)
        {
	        Log.e(TAG, e.getMessage(), e);
        }
	}

	public void watch(final String channelId, final String streamType, OnResultReceived onResultReceived)
	{
		ResponseCallbackWatch responseCallback = new ResponseCallbackWatch(onResultReceived);
		StringRequest stringRequest = new StringRequest(Request.Method.POST, _baseUri + "/zapi/watch",
		        responseCallback, responseCallback)
		{
			@Override
			protected Map<String, String> getParams() throws AuthFailureError
			{
				Map<String, String> params = new HashMap<String, String>();
				params.put("cid", channelId);
				params.put("stream_type", streamType);
				if (_initRate > 0)
					params.put("initialrate", String.valueOf(_initRate));
				if (_minRate > 0)
					params.put("minrate", String.valueOf(_minRate));
				return params;
			}

			@Override
			protected Response<String> parseNetworkResponse(NetworkResponse response)
			{
				Map<String, String> responseHeaders = response.headers;
				_cookie = responseHeaders.get("set-cookie");
				Log.i(TAG, "login::_cookie = " + _cookie);
				return super.parseNetworkResponse(response);
			}
		};
        try
        {
	       String body = new String(stringRequest.getPostBody());
			Log.i(TAG, "call " + stringRequest.getUrl() + " [" + body + "]");
			_requestQueue.add(stringRequest);
        }
        catch (AuthFailureError e)
        {
	        Log.e(TAG, e.getMessage(), e);
        }
	}

	private class ResponseCallback implements Response.Listener<String>, Response.ErrorListener
	{
		protected OnResultReceived _onResultReceived;

		ResponseCallback(OnResultReceived onResultReceived)
		{
			_onResultReceived = onResultReceived;
		}

		@Override
		public void onResponse(String response)
		{
			Log.i(TAG, ".onResponse: response = " + response);
			_onResultReceived.onReceiveResult(FeatureError.OK);
		}

		@Override
		public void onErrorResponse(VolleyError error)
		{
			Log.i(TAG, ".onErrorResponse: " + error);
			_onResultReceived.onReceiveResult(new FeatureError(Environment.getInstance().getFeatureScheduler(
			        FeatureName.Scheduler.EPG), error));
		}
	}

	private class ResponseCallbackWatch extends ResponseCallback
	{
		ResponseCallbackWatch(OnResultReceived onResultReceived)
		{
			super(onResultReceived);
		}

		@Override
		public void onResponse(String response)
		{
			Log.i(TAG, ".onResponse: " + response);
			try
			{
				JSONObject json = new JSONObject(response);
				JSONObject stream = json.getJSONObject("stream");
				Bundle bundle = new Bundle();
				bundle.putString(EXTRA_URL, stream.getString("url"));
				bundle.putString(EXTRA_STOP_URL, stream.getString("stop_url"));
				_onResultReceived.onReceiveResult(new FeatureError(null, ResultCode.OK, bundle));
			}
			catch (JSONException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
		}

		@Override
		public void onErrorResponse(VolleyError error)
		{
			Log.i(TAG, ".onErrorResponse: " + error);
			_onResultReceived.onReceiveResult(new FeatureError(Environment.getInstance().getFeatureScheduler(
			        FeatureName.Scheduler.EPG), error));
		}
	}
}

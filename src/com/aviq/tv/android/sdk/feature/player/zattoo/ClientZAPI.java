/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    ClientZAPI.java
 * Author:      alek
 * Date:        23 Jun 2014
 * Description: Zattoo API http client
 */

package com.aviq.tv.android.sdk.feature.player.zattoo;

import java.util.HashMap;
import java.util.Map;

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
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;

/**
 * Zattoo API http client
 */
public class ClientZAPI
{
	private static final String TAG = ClientZAPI.class.getSimpleName();
	private String _baseUri;
	private String _cookie;
	public static final String EXTRA_ERROR = "ERROR";

	public ClientZAPI(String baseUri)
	{
		_baseUri = baseUri;
	}

	public void hello(final String appTid, final String uuid, OnResultReceived onResultReceived)
	{
		RequestQueue requestQueue = Volley.newRequestQueue(Environment.getInstance(), new ExtHttpClientStack(
		        new SslHttpClient()));

		ResponseCallbackHello responseCallbackHello = new ResponseCallbackHello(onResultReceived);
		StringRequest stringRequest = new StringRequest(Request.Method.POST, _baseUri + "/zapi/session/hello",
		        responseCallbackHello, responseCallbackHello)
		{
			@Override
			protected Map<String, String> getParams() throws AuthFailureError
			{
				Map<String, String> params = new HashMap<String, String>();
				params.put("app_tid", appTid);
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
				return super.parseNetworkResponse(response);
			}
		};
		requestQueue.add(stringRequest);
	}

	public void login(String username, String password, OnResultReceived onResultReceived)
	{

	}

	private class ResponseCallbackHello implements Response.Listener<String>, Response.ErrorListener
	{
		private OnResultReceived _onResultReceived;

		ResponseCallbackHello(OnResultReceived onResultReceived)
		{
			_onResultReceived = onResultReceived;
		}

		@Override
		public void onResponse(String response)
		{
			Log.i(TAG, ".onResponse: response = " + response);
			_onResultReceived.onReceiveResult(ResultCode.OK, new Bundle());
		}

		@Override
		public void onErrorResponse(VolleyError error)
		{
			Log.i(TAG, ".onErrorResponse: error = " + error);
			int statusCode = error.networkResponse != null ? error.networkResponse.statusCode
			        : ResultCode.GENERAL_FAILURE;
			Log.e(TAG, "Hello Error  " + statusCode + ": " + error);
			Bundle bundle = new Bundle();
			bundle.putString(EXTRA_ERROR, error.toString());
			_onResultReceived.onReceiveResult(statusCode, bundle);
		}
	}
}

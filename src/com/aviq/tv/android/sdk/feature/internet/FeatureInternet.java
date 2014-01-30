/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureSchedulerInternet.java
 * Author:      alek
 * Date:        3 Dec 2013
 * Description: Feature managing internet connectivity
 */

package com.aviq.tv.android.sdk.feature.internet;

import android.os.Bundle;

import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureScheduler;

/**
 * Feature managing internet connectivity
 */
public class FeatureInternet extends FeatureScheduler
{
	private static final String TAG = FeatureInternet.class.getSimpleName();

	/**
	 * Interface to be implemented when requested url returns response status
	 */
	public interface OnResultReceived
	{
		void onReceiveResult(int resultCode, Bundle resultData);
	}

	@Override
	public FeatureName.Scheduler getSchedulerName()
	{
		return FeatureName.Scheduler.INTERNET;
	}

	/**
	 * Adds url to be checked periodically. The url is requested immediately.
	 *
	 * @param url
	 *            to be checked periodically
	 * @param intervalSecs
	 *            the time interval in seconds
	 * @param onResultReceived
	 */
	public void addCheckUrl(String url, int intervalSecs, OnResultReceived onResultReceived)
	{
		CheckResponse responseSuccess = new CheckResponse(1000 * intervalSecs, onResultReceived);
		CheckResponse responseError = new CheckResponse(1000 * intervalSecs, onResultReceived);
		StringRequest stringRequest = new StringRequest(url, responseSuccess, responseError);
		responseSuccess.setRequest(stringRequest);
		responseError.setRequest(stringRequest);
		Environment.getInstance().getRequestQueue().add(stringRequest);
	}

	/**
	 * Url check response handler
	 */
	private class CheckResponse implements Listener<String>, ErrorListener, Runnable
	{
		private StringRequest _stringRequest;
		private int _intervalSecs;
		private OnResultReceived _onResultReceived;

		private CheckResponse(int intervalSecs, OnResultReceived onResultReceived)
		{
			_intervalSecs = intervalSecs;
			_onResultReceived = onResultReceived;
		}

		public void setRequest(StringRequest stringRequest)
		{
			_stringRequest = stringRequest;
		}

		/**
		 * Response success
		 */
		@Override
		public void onResponse(String response)
		{
			if (response.length() > 10)
				response = response.substring(0, 10);
			Log.i(TAG, _stringRequest.getUrl() + " -> " + response);
			if (_onResultReceived != null)
			{
				Bundle bundle = new Bundle();
				bundle.putString("URL", _stringRequest.getUrl());
				_onResultReceived.onReceiveResult(ResultCode.OK, bundle);
			}
			Log.i(TAG, "Post checking again in " + _intervalSecs + " ms");
			Environment.getInstance().getEventMessenger().postDelayed(this, _intervalSecs);
		}

		/**
		 * Response failure
		 */
		@Override
		public void onErrorResponse(VolleyError error)
		{
			int statusCode = ResultCode.GENERAL_FAILURE;
			if (error.networkResponse != null)
				statusCode = error.networkResponse.statusCode;
			Log.e(TAG, _stringRequest.getUrl() + " -> " + statusCode);
			if (_onResultReceived != null)
			{
				Bundle bundle = new Bundle();
				bundle.putString("URL", _stringRequest.getUrl());
				_onResultReceived.onReceiveResult(statusCode, bundle);
			}
			Log.i(TAG, "Post checking again in " + _intervalSecs + " ms");
			Environment.getInstance().getEventMessenger().postDelayed(this, _intervalSecs);
		}

		@Override
		public void run()
		{
			Log.i(TAG, "Calling url " + _stringRequest.getUrl());
			Environment.getInstance().getRequestQueue().add(_stringRequest);
		}
	}
}

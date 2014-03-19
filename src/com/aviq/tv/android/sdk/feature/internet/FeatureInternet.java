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
import com.aviq.tv.android.sdk.core.service.ServiceController;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Feature managing Internet access
 */
public class FeatureInternet extends FeatureScheduler
{
	private static final String TAG = FeatureInternet.class.getSimpleName();

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
		 * Delay between internet check attempts
		 */
		CHECK_ATTEMPT_DELAY(1000);

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
		URL, CONTENT, ERROR
	}

	private String _checkUrl;

	// FIXME: to be removed from this component
	private double _averageDownloadRateMbPerSec;

	@Override
	public void onSchedule(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".onSchedule");
		getUrlContent(_checkUrl, new OnResultReceived()
		{
			@Override
			public void onReceiveResult(int resultCode, Bundle resultData)
			{
				Log.i(TAG, ".onReceiveResult: resultCode = " + resultCode + " (" + TextUtils.implodeBundle(resultData)
				        + ")");
				onFeatureInitialized.onInitialized(FeatureInternet.this, resultCode);
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
	 * Start checking periodically this url
	 *
	 * @param url
	 *            to be checked periodically
	 */
	public void startCheckUrl(String url)
	{
		_checkUrl = url;
		getEventMessenger().trigger(ON_SCHEDULE);
	}

	/**
	 * Checks internet access
	 *
	 * @param onResultReceived
	 *            result callback interface
	 */
	public void checkInternet(final OnResultReceived onResultReceived)
	{
		class InternetCheckResponse implements OnResultReceived, Runnable
		{
			private int _attemptsCounter = 0;

			@Override
			public void run()
			{
				getUrlContent(_checkUrl, this);
			}

			@Override
			public void onReceiveResult(int resultCode, Bundle resultData)
			{
				if (resultCode != ResultCode.OK)
				{
					int checkAttempts = getPrefs().getInt(Param.CHECK_ATTEMPTS);
					if (_attemptsCounter < checkAttempts)
					{
						_attemptsCounter++;
						Log.w(TAG, "Check internet failed. Trying " + (checkAttempts - _attemptsCounter)
						        + " more times");
						getEventMessenger().postDelayed(this, getPrefs().getInt(Param.CHECK_ATTEMPT_DELAY));
						return;
					}
				}
				onResultReceived.onReceiveResult(resultCode, resultData);
			}
		}
		getEventMessenger().post(new InternetCheckResponse());
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
		Log.i(TAG, ".getUrlContent: " + url);
		CheckResponse responseSuccess = new CheckResponse(url, onResultReceived);
		CheckResponse responseError = new CheckResponse(url, onResultReceived);
		StringRequest stringRequest = new StringRequest(url, responseSuccess, responseError);
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
		serviceController.startService(DownloadService.class, params).then(new OnResultReceived()
		{
			@Override
			public void onReceiveResult(int resultCode, Bundle resultData)
			{
				if (resultData != null)
				{
					_averageDownloadRateMbPerSec = resultData
					        .getDouble(DownloadService.ResultExtras.DOWNLOAD_RATE_MB_PER_SEC.name());
				}

				onResultReceived.onReceiveResult(resultCode, resultData);
			}
		});
	}

	/**
	 * Return the download speed measured in MB/sec.
	 * FIXME: download rate must be obtained dynamically during download
	 * progress calls
	 */
	@Deprecated
	public double getAverageDownloadRate()
	{
		return _averageDownloadRateMbPerSec;
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
			if (error.networkResponse != null)
				statusCode = error.networkResponse.statusCode;
			Log.e(TAG, _url + " -> " + statusCode + " ( " + error.getMessage() + ")");
			Bundle resultData = new Bundle();
			resultData.putString(ResultExtras.URL.name(), _url);
			resultData.putString(ResultExtras.ERROR.name(), error.getMessage());
			_onResultReceived.onReceiveResult(statusCode, resultData);
		}
	}
}

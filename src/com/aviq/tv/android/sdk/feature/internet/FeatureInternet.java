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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.AsyncTask;
import android.os.Bundle;

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

		/** URL to check against for the box's public IP. */
		PUBLIC_IP_CHECK_URL("http://checkip.dyndns.org");

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
		URL, CONTENT, ERROR_MESSAGE, ERROR_CODE, HOST
	}

	private String _checkUrl;
	private String _publicIP;

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
				getEventMessenger().trigger(resultCode == ResultCode.OK ? ON_CONNECTED : ON_DISCONNECTED, resultData);
				scheduleDelayed(getPrefs().getInt(Param.CHECK_INTERVAL) * 1000);

				// Get public IP the first time
				if (_publicIP == null)
				{
					retrievePublicIPAsync();
				}
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
		Log.i(TAG, ".startCheckUrl: " + url);
		_checkUrl = url;
		getEventMessenger().removeMessages(ON_SCHEDULE);
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
			private long _checkStart = System.currentTimeMillis();

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
					long timeElapsed = System.currentTimeMillis() - _checkStart;
					if (_attemptsCounter < checkAttempts
					        || (timeElapsed < getPrefs().getInt(Param.CHECK_ATTEMPTS_TIMEOUT)))
					{
						_attemptsCounter++;
						Log.w(TAG, "Check internet failed. Trying " + (checkAttempts - _attemptsCounter + 1)
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
	 * Get headers from Url
	 *
	 * @param url
	 *            the url to get headers from
	 * @param onResultReceived
	 *            result callback interface
	 */
	public void getUrlHeaders(final String url, final OnResultReceived onResultReceived)
	{
		Log.i(TAG, ".getUrlHeaders: " + url);
		new AsyncTask<Void, Void, Map<String, List<String>>>()
		{
			@Override
			protected Map<String, List<String>> doInBackground(Void... params)
			{
				Map<String, List<String>> headers = null;

				HttpURLConnection connection = null;
				try
				{
					URL urlAddress = new URL(url);
					connection = (HttpURLConnection) urlAddress.openConnection();
					connection.setUseCaches(false);

					headers = connection.getHeaderFields();
				}
				catch (IOException e)
				{
					Log.e(TAG, e.getMessage(), e);
				}
				finally
				{
					if (connection != null)
						connection.disconnect();
				}

				return headers;
			}

			@Override
			protected void onPostExecute(Map<String, List<String>> result)
			{
				Bundle resultData = new Bundle();
				resultData.putString(ResultExtras.URL.name(), url);

				if (result != null)
				{
					for (Map.Entry<String, List<String>> entry : result.entrySet())
					{
						List<String> values = entry.getValue();
						if (values.size() > 0)
							resultData.putString(entry.getKey(), values.get(0));
					}
					onResultReceived.onReceiveResult(ResultCode.OK, resultData);
				}
				else
				{
					resultData.putString(ResultExtras.ERROR_MESSAGE.name(), "Cannot obtain headers from url = " + url);
					resultData.putInt(ResultExtras.ERROR_CODE.name(), ResultCode.GENERAL_FAILURE);

					onResultReceived.onReceiveResult(ResultCode.GENERAL_FAILURE, resultData);
				}
			}
		}.execute();
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

	public void stopFileDownload()
	{
		Log.i(TAG, ".stopFileDownload");
		DownloadService.stopIfRunning();
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

	private void retrievePublicIPAsync()
	{
		Log.i(TAG, ".retrievePublicIPAsync");
		String url = getPrefs().getString(Param.PUBLIC_IP_CHECK_URL);

		getUrlContent(url, new OnResultReceived()
		{
			@Override
			public void onReceiveResult(int resultCode, Bundle resultData)
			{
				if (resultCode == ResultCode.OK)
				{
					String content = resultData.getString(ResultExtras.CONTENT.name());

					String ip = null;

					// <html><head><title>Current IP
					// Check</title></head><body>Current IP Address:
					// 78.90.178.220</body></html>
					Pattern pattern = Pattern
					        .compile("\\<body\\>Current IP Address\\:\\s.*?(\\w+\\.\\w+.\\w+.\\w+)\\s*?\\<\\/body\\>");
					Matcher matcher = pattern.matcher(content);
					if (matcher.find())
					{
						ip = matcher.group(1).trim();
					}
					if (ip != null && !"".equals(ip))
						_publicIP = ip;

					Log.i(TAG, ".retrievePublicIPAsync: public IP = " + _publicIP);
				}
			}
		});
	}

	public String getPublicIP()
	{
		return _publicIP;
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
			resultData.putString(ResultExtras.ERROR_MESSAGE.name(), error.getMessage());
			resultData.putInt(ResultExtras.ERROR_CODE.name(), statusCode);
			_onResultReceived.onReceiveResult(statusCode, resultData);
		}
	}
}

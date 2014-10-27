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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureScheduler;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;
import com.aviq.tv.android.sdk.core.service.ServiceController;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Feature managing Internet access
 */
@Author("alek")
public class FeatureInternet extends FeatureScheduler
{
	private static final String TAG = FeatureInternet.class.getSimpleName();

	public static final int ON_CONNECTED = EventMessenger.ID("ON_CONNECTED");
	public static final int ON_DISCONNECTED = EventMessenger.ID("ON_DISCONNECTED");
	public static final int ON_GEOIP = EventMessenger.ID("ON_GEOIP");

	public static enum Param
	{
		/**
		 * Check URL interval in seconds
		 */
		CHECK_INTERVAL(60000),

		/**
		 * Timeout in seconds to attempt checking
		 */
		CHECK_TIMEOUT(30000),

		/**
		 * Check URL attempts number
		 */
		CHECK_ATTEMPTS(6),

		/**
		 * Delay between internet check attempts
		 */
		CHECK_ATTEMPT_DELAY(4000),

		/** Host to check route access */
		CHECK_HOST_ACCESS("www.google.com"),

		/** Backup host to check route access */
		CHECK_HOST_ACCESS_BACKUP("www.amazon.com"),

		/** URL to check against for the box's geoip information */
		GEOIP_URL("http://www.telize.com/geoip"),

		/** Backup URL to check against for the box's geoip information */
		GEOIP_URL_BACKUP("http://freegeoip.net/json");

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
		URL, CONTENT, ERROR_MESSAGE, ERROR_CODE
	}

	public enum GeoIpExtras
	{
		PUBLIC_IP, LATITUDE, LONGITUDE, CITY, COUNTRY, REGION, ISP
	}

	public enum OnConnectedExtras
	{
		/**
		 * @see android.net.NetworkInfo.getType NetworkInfo.getType()
		 */
		NETWORK_TYPE
	}

	private Bundle _geoIp;

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		getEventMessenger().trigger(ON_SCHEDULE);
		super.initialize(onFeatureInitialized);
	}

	@Override
	public void onSchedule(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".onSchedule");
		final int maxCheckAttempts = getPrefs().getInt(Param.CHECK_ATTEMPTS);
		checkInternet(new OnResultReceived()
		{
			private int _checkAttempts = 0;

			@Override
			public void onReceiveResult(FeatureError error)
			{
				Log.i(TAG, ".onSchedule:onReceiveResult: #" + _checkAttempts + ", error = " + error);
				final OnResultReceived _this = this;
				if (error.isError() && _checkAttempts < maxCheckAttempts)
				{
					_checkAttempts++;
					getEventMessenger().postDelayed(new Runnable()
					{
						@Override
						public void run()
						{
							checkInternet(_this);
						}
					}, getPrefs().getInt(Param.CHECK_ATTEMPT_DELAY));
				}
				else
				{
					getEventMessenger().trigger(error.isError() ? ON_DISCONNECTED : ON_CONNECTED, error.getBundle());
					scheduleDelayed(getPrefs().getInt(Param.CHECK_INTERVAL));
					_checkAttempts = 0;

					if (!error.isError() && _geoIp == null)
					{
						// retrieves GeoIP data
						retrieveGeoIP(new OnResultReceived()
						{
							@Override
							public void onReceiveResult(FeatureError error)
							{
								if (!error.isError())
								{
									_geoIp = new Bundle();
									Bundle bundle = error.getBundle();
									for (GeoIpExtras geoip : GeoIpExtras.values())
									{
										if (bundle.containsKey(geoip.name()))
										{
											Object value = bundle.get(geoip.name());
											Log.d(TAG, "GeoIP." + geoip.name() + " -> " + value);
											TextUtils.putBundleObject(_geoIp, geoip.name(), value);
										}
									}
									getEventMessenger().trigger(ON_GEOIP, _geoIp);
								}
							}
						});
					}
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
		OnContentResponse responseSuccess = new OnContentResponse(url, onResultReceived);
		OnContentResponse responseError = new OnContentResponse(url, onResultReceived);
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
	 * Get headers from Url. <br>
	 * <b>Caution:</b> Do not use this method against large content behind the
	 * url.
	 * The Volley library is allocating memory with size the content length
	 * returned by the request header,
	 * which may lead to OutOfMemoryError. <br>
	 * FIXME: Make head request issue workaround by providing custom HttpStack
	 * to the Environment's request queue.
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
				onResultReceived
				        .onReceiveResult(new FeatureError(FeatureInternet.this, response.statusCode, resultData));
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
	 * Returns bundle of GeoIP data
	 *
	 * @see GeoIpExtras
	 * @return Bundle
	 */
	public Bundle getGeoIP()
	{
		return _geoIp;
	}

	/**
	 * Url check response handler
	 */
	private class OnContentResponse implements Listener<String>, ErrorListener
	{
		private OnResultReceived _onResultReceived;
		private String _url;

		OnContentResponse(String url, OnResultReceived onResultReceived)
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
			_onResultReceived.onReceiveResult(new FeatureError(FeatureInternet.this, ResultCode.OK, resultData));
		}

		/**
		 * Response failure
		 */
		@Override
		public void onErrorResponse(VolleyError error)
		{
			_onResultReceived.onReceiveResult(new FeatureError(FeatureInternet.this, error));
		}
	}

	/**
	 * Checks for internet access by probing route to list of hosts
	 *
	 * @param onResultReceived
	 */
	public void checkInternet(final OnResultReceived onResultReceived)
	{
		final String[] hosts = new String[]
		{ getPrefs().getString(Param.CHECK_HOST_ACCESS), getPrefs().getString(Param.CHECK_HOST_ACCESS_BACKUP) };
		final int checkTimeout = getPrefs().getInt(Param.CHECK_TIMEOUT);
		final Object mutex = new Object();

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				final boolean status[] = new boolean[1];
				final int completed[] = new int[1];
				completed[0] = 0;
				Thread threads[] = new Thread[hosts.length];
				FeatureError error = new FeatureError(FeatureInternet.this, ResultCode.OK);
				final Bundle bundle = new Bundle();
				final ConnectivityManager connectivityManager = (ConnectivityManager) Environment.getInstance()
				        .getSystemService(Context.CONNECTIVITY_SERVICE);
				final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
				if (networkInfo != null)
				{
					bundle.putInt(OnConnectedExtras.NETWORK_TYPE.name(), networkInfo.getType());

					StringBuffer hostNames = new StringBuffer();
					for (int i = 0; i < hosts.length; i++)
					{
						if (i > 0)
							hostNames.append(',');
						hostNames.append(hosts[i]);
						threads[i] = new Thread(new Runnable()
						{
							@Override
							public void run()
							{
								boolean connected = false;
								String host = Thread.currentThread().getName();
								try
								{
									Log.i(TAG, "Resolving IP for host " + host);
									InetAddress inetAddress = InetAddress.getByName(host);
									if (inetAddress != null)
									{
										byte[] ip = inetAddress.getAddress();
										int addr = ((ip[3] & 0xff) << 24) | ((ip[2] & 0xff) << 16)
										        | ((ip[1] & 0xff) << 8) | (ip[0] & 0xff);

										connected = connectivityManager.requestRouteToHost(networkInfo.getType(), addr);
										bundle.putBoolean(host, connected);
									}
									Log.i(TAG, host + " - " + (connected ? "OK" : "FAIL"));
								}
								catch (UnknownHostException e)
								{
									Log.w(TAG, e.getMessage(), e);
								}
								completed[0]++;

								if (connected || completed[0] == hosts.length)
								{
									status[0] = status[0] || connected;
									synchronized (mutex)
									{
										mutex.notify();
									}
								}
							}
						}, hosts[i]);
					}

					for (int i = 0; i < hosts.length; i++)
					{
						threads[i].start();
					}

					synchronized (mutex)
					{
						try
						{
							mutex.wait(checkTimeout);

							if (!status[0])
							{
								error = new FeatureError(FeatureInternet.this, ResultCode.IO_ERROR, bundle,
								        "No route to hosts " + hostNames);
							}
						}
						catch (InterruptedException e)
						{
							if (!status[0])
							{
								error = new FeatureError(FeatureInternet.this, ResultCode.GENERAL_FAILURE, bundle,
								        "Internet check unexpectedly interrupted");
							}
						}
					}
				}
				else
				{
					error = new FeatureError(FeatureInternet.this, ResultCode.IO_ERROR, bundle,
					        "No active network available");
				}

				final FeatureError result = error;
				getEventMessenger().post(new Runnable()
				{
					@Override
					public void run()
					{
						onResultReceived.onReceiveResult(result);
					}
				});
			}
		}).start();
	}

	/**
	 * Retrieves GeoIP data
	 *
	 * @param onResultReceived
	 *            result callback interface
	 */
	private void retrieveGeoIP(final OnResultReceived onResultReceived)
	{
		class GeoIPCheckResponse implements OnResultReceived, Runnable
		{
			private int _attemptsCounter = 0;

			@Override
			public void run()
			{
				if (_attemptsCounter == 0)
					getUrlContent(getPrefs().getString(Param.GEOIP_URL), this);
				else
					getUrlContent(getPrefs().getString(Param.GEOIP_URL_BACKUP), this);
			}

			@Override
			public void onReceiveResult(FeatureError result)
			{
				Bundle resultData = result.getBundle();
				if (result.isError())
				{
					if (_attemptsCounter < 1)
					{
						_attemptsCounter++;
						getEventMessenger().post(this);
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
							resultData.putString(GeoIpExtras.PUBLIC_IP.name(), obj.getString("ip"));
						if (!obj.isNull("latitude"))
							resultData.putDouble(GeoIpExtras.LATITUDE.name(), obj.getDouble("latitude"));
						if (!obj.isNull("longitude"))
							resultData.putDouble(GeoIpExtras.LONGITUDE.name(), obj.getDouble("longitude"));
						if (!obj.isNull("city"))
							resultData.putString(GeoIpExtras.CITY.name(), obj.getString("city"));
						if (!obj.isNull("country"))
							resultData.putString(GeoIpExtras.COUNTRY.name(), obj.getString("country"));
						else if (!obj.isNull("country_name"))
							resultData.putString(GeoIpExtras.COUNTRY.name(), obj.getString("country_name"));
						if (!obj.isNull("region"))
							resultData.putString(GeoIpExtras.REGION.name(), obj.getString("region"));
						else if (!obj.isNull("region_name"))
							resultData.putString(GeoIpExtras.REGION.name(), obj.getString("region_name"));
						if (!obj.isNull("isp"))
							resultData.putString(GeoIpExtras.ISP.name(), obj.getString("isp"));
					}
					catch (JSONException e)
					{
						Log.e(TAG, e.getMessage(), e);
					}
				}
				onResultReceived.onReceiveResult(result);
			}
		}
		getEventMessenger().post(new GeoIPCheckResponse());
	}

	// FIXME: the public ip must be obtained by ON_CONNECTED event
	@Deprecated
	public String getPublicIP()
	{
		if (_geoIp != null)
			return _geoIp.getString(GeoIpExtras.PUBLIC_IP.name());
		return null;
	}
}

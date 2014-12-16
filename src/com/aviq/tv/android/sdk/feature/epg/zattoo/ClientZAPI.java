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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TimeZone;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.Bundle;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.IFeature;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.epg.EpgData;
import com.aviq.tv.android.sdk.feature.epg.Program;
import com.aviq.tv.android.sdk.feature.system.FeatureTimeZone;
import com.aviq.tv.android.sdk.utils.Calendars;

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
	private String _pghash;
	private int _minRateEth;
	private int _maxRateEth;
	private int _initRateEth;
	private int _minRateWifi;
	private int _maxRateWifi;
	private int _initRateWifi;
	private RequestQueue _requestQueue;
	private IFeature _ownerFeature;
	private Map<String, Channel> _channelsMap = new HashMap<String, Channel>();
	private EpgData _epgData;
	private int _countChannelLogos;
	private int _maxChannels;

	public ClientZAPI(IFeature ownerFeature, String baseUri, int minRateEth, int maxRateEth, int initRateEth,
	        int minRateWifi, int maxRateWifi, int initRateWifi, int maxChannels)
	{
		_minRateEth = minRateEth;
		_maxRateEth = maxRateEth;
		_initRateEth = initRateEth;
		_minRateWifi = minRateWifi;
		_maxRateWifi = maxRateWifi;
		_initRateWifi = initRateWifi;
		_baseUri = baseUri;
		_requestQueue = Volley.newRequestQueue(Environment.getInstance(), new ExtHttpClientStack(new SslHttpClient()));
		_ownerFeature = ownerFeature;
		_maxChannels = maxChannels;
	}

	public EpgData getEpgData()
	{
		return _epgData;
	}

	public void hello(final String appid, final String uuid, OnResultReceived onResultReceived)
	{
		HelloResponseCallback responseCallback = new HelloResponseCallback(onResultReceived);
		StringRequest stringRequest = new StringRequest(Request.Method.POST, _baseUri + "/zapi/session/hello",
		        responseCallback, responseCallback)
		{
			@Override
			public Map<String, String> getHeaders() throws AuthFailureError
			{
				Map<String, String> headers = new HashMap<String, String>();
				headers.put("Connection", "close");
				return headers;
			}

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

	private class HelloResponseCallback implements Response.Listener<String>, Response.ErrorListener
	{
		protected OnResultReceived _onResultReceived;

		HelloResponseCallback(OnResultReceived onResultReceived)
		{
			_onResultReceived = onResultReceived;
		}

		@Override
		public void onResponse(String response)
		{
			Log.i(TAG, ".onResponse: response = " + response);

			try
			{
				JSONObject jsonObj = new JSONObject(response);
				JSONObject jsonSesstion = jsonObj.getJSONObject("session");
				_pghash = (String) jsonSesstion.get("power_guide_hash");
				_onResultReceived.onReceiveResult(FeatureError.OK(_ownerFeature));
			}
			catch (JSONException e)
			{
				Log.e(TAG, e.getMessage(), e);
				_onResultReceived.onReceiveResult(new FeatureError(_ownerFeature, ResultCode.PROTOCOL_ERROR, e));
			}
		}

		@Override
		public void onErrorResponse(VolleyError error)
		{
			Log.i(TAG, ".onErrorResponse: " + error);
			if (error.networkResponse != null)
				Log.e(TAG, ".onErrorResponse: data = " + new String(error.networkResponse.data));
			_onResultReceived.onReceiveResult(new FeatureError(_ownerFeature, error));
		}
	}

	public void login(final String username, final String password, OnResultReceived onResultReceived)
	{
		ResponseCallback responseCallback = new ResponseCallback(onResultReceived);
		StringRequest stringRequest = new StringRequest(Request.Method.POST, _baseUri + "/zapi/account/login",
		        responseCallback, responseCallback)
		{
			@Override
			public Map<String, String> getHeaders() throws AuthFailureError
			{
				Map<String, String> header = new HashMap<String, String>();
				header.put("cookie", _cookie);
				header.put("Connection", "close");
				return header;
			}

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

	public void retrieveChannels(OnResultReceived onResultReceived)
	{
		ChannelsResponseCallback responseCallback = new ChannelsResponseCallback(onResultReceived);
		StringRequest stringRequest = new StringRequest(Request.Method.GET, _baseUri + "/zapi/channels",
		        responseCallback, responseCallback)
		{
			@Override
			public Map<String, String> getHeaders() throws AuthFailureError
			{
				Map<String, String> header = new HashMap<String, String>();
				header.put("cookie", _cookie);
				header.put("Connection", "close");
				return header;
			}

			@Override
			protected Response<String> parseNetworkResponse(NetworkResponse response)
			{
				Map<String, String> responseHeaders = response.headers;
				_cookie = responseHeaders.get("set-cookie");
				return super.parseNetworkResponse(response);
			}
		};

		Log.i(TAG, "call " + stringRequest.getUrl());
		_requestQueue.add(stringRequest);
	}

	private class ChannelsResponseCallback implements Response.Listener<String>, Response.ErrorListener
	{
		protected OnResultReceived _onResultReceived;

		ChannelsResponseCallback(OnResultReceived onResultReceived)
		{
			_onResultReceived = onResultReceived;
		}

		@Override
		public void onResponse(String response)
		{
			Log.i(TAG, ".onResponse: response = " + response);
			try
			{
				JSONObject jsonObj = new JSONObject(response);
				JSONArray jsonChannels = jsonObj.getJSONArray("channels");
				final List<Channel> channels = new ArrayList<Channel>();
				int maxChannels = jsonChannels.length();
				if (_maxChannels > 0)
					maxChannels = Math.min(jsonChannels.length(), _maxChannels);
				for (int i = 0; i < maxChannels; i++)
				{
					ChannelZattoo channel = new ChannelZattoo(i);
					JSONObject jsonChannel = (JSONObject) jsonChannels.get(i);
					channel.setChannelId((String) jsonChannel.get("cid"));
					channel.setThumbnail((String) jsonChannel.get("logo_84"));
					channel.setTitle((String) jsonChannel.get("title"));
					channels.add(channel);
					_channelsMap.put(channel.getChannelId(), channel);
				}
				_epgData = new EpgData(channels);
				retrieveChannelLogos(_epgData, _onResultReceived);
			}
			catch (JSONException e)
			{
				Log.e(TAG, e.getMessage(), e);
				_onResultReceived.onReceiveResult(new FeatureError(_ownerFeature, ResultCode.PROTOCOL_ERROR, e));
			}
		}

		@Override
		public void onErrorResponse(VolleyError error)
		{
			Log.i(TAG, ".onErrorResponse: " + error);
			if (error.networkResponse != null)
				Log.e(TAG, ".onErrorResponse: data = " + new String(error.networkResponse.data));
			_onResultReceived.onReceiveResult(new FeatureError(Environment.getInstance().getFeatureScheduler(
			        FeatureName.Scheduler.EPG), error));
		}
	}

	public void retrieveChannelLogos(final EpgData epgData, final OnResultReceived onResultReceived)
	{
		// retrieve channel logos
		_countChannelLogos = 0;
		for (final Channel channel : epgData.getChannels())
		{
			ImageRequest imageRequest = new ImageRequest(channel.getThumbnail(), new Response.Listener<Bitmap>()
			{
				@Override
				public void onResponse(Bitmap bitmap)
				{
					epgData.setChannelLogo(channel.getIndex(), bitmap);
					_countChannelLogos++;
					if (_countChannelLogos == epgData.getChannels().size())
						onResultReceived.onReceiveResult(FeatureError.OK(_ownerFeature));
				}
			}, 0, 0, Config.ARGB_8888, new Response.ErrorListener()
			{
				@Override
				public void onErrorResponse(VolleyError volleyError)
				{
					FeatureError error = new FeatureError(_ownerFeature, volleyError);
					Log.e(TAG, error.getMessage(), error);
					_countChannelLogos++;
					if (_countChannelLogos == epgData.getChannels().size())
						onResultReceived.onReceiveResult(FeatureError.OK(_ownerFeature));
				}
			})
			{
				@Override
				public Map<String, String> getHeaders() throws AuthFailureError
				{
					Map<String, String> headers = new HashMap<String, String>();
					headers.put("Connection", "close");
					return headers;
				}
			};
			_requestQueue.add(imageRequest);
		}
	}

	public void retrievePrograms(final OnResultReceived onResultReceived)
	{
		final int dayFrom = -4;
		int dayTo = 4;
		final int daysTotal = dayTo - dayFrom;
		final ProgramsResponseCallback[] programResponses = new ProgramsResponseCallback[daysTotal];

		OnResultReceived programsResultReceived = new OnResultReceived()
		{
			private int _ndays = 0;
			private Map<String, List<Program>[]> _channelToProgramLists = new HashMap<String, List<Program>[]>();

			@SuppressWarnings("unchecked")
			@Override
			public void onReceiveResult(FeatureError error)
			{
				_ndays++;
				if (!error.isError())
				{
					int day = error.getBundle().getInt("day");
					Log.i(TAG, "Got programs response for day " + day + " of " + daysTotal);
					Map<String, List<Program>> channelsToProgram = programResponses[day - dayFrom]
					        .getChannelsToPrograms();

					for (Map.Entry<String, List<Program>> entry : channelsToProgram.entrySet())
					{
						String channelId = entry.getKey();
						List<Program> programList = entry.getValue();
						List<Program>[] programLists = _channelToProgramLists.get(channelId);
						if (programLists == null)
						{
							programLists = (List<Program>[]) new ArrayList<?>[daysTotal];
							_channelToProgramLists.put(channelId, programLists);
						}
						programLists[day - dayFrom] = programList;
					}
				}
				if (_ndays == daysTotal)
				{
					for (Map.Entry<String, List<Program>[]> entry : _channelToProgramLists.entrySet())
					{
						String channelId = entry.getKey();
						List<Program>[] programLists = entry.getValue();
						List<Program> allProgramsList = new ArrayList<Program>();
						NavigableMap<Calendar, Integer> programMap = new TreeMap<Calendar, Integer>();
						// FIXME: resolve border programs between days
						for (int day = 0; day < daysTotal; day++)
						{
							List<Program> programList = programLists[day];
							if (programList != null)
							{
								for (Program program : programList)
								{
									allProgramsList.add(program);
									programMap.put(program.getStartTime(), allProgramsList.size() - 1);
								}
							}
						}
						_epgData.addProgramData(channelId, programMap, allProgramsList);
					}
					onResultReceived.onReceiveResult(error);
				}
			}
		};

		for (int day = dayFrom; day < dayTo; day++)
		{
			programResponses[day - dayFrom] = new ProgramsResponseCallback(day, programsResultReceived);
			Calendar startTime = Calendars.getDateByDayOffset(day);
			Calendar endTime = Calendars.getDateByDayOffset(day + 1);
			endTime.setTimeInMillis(endTime.getTimeInMillis() - 1000);
			Log.i(TAG,
			        "day " + day + " is time between " + Calendars.makeString(startTime) + " and "
			                + Calendars.makeString(endTime) + " is "
			                + ((endTime.getTimeInMillis() - startTime.getTimeInMillis()) / 1000));
			String startTimeParam = String.valueOf(startTime.getTimeInMillis() / 1000); // 1415012400
			String endTimeParam = String.valueOf(endTime.getTimeInMillis() / 1000); // 1415098800
			StringRequest stringRequest = new StringRequest(Request.Method.GET, _baseUri
			        + "/zapi/v2/cached/program/power_guide/" + _pghash + "?start=" + startTimeParam + "&end="
			        + endTimeParam, programResponses[day - dayFrom], programResponses[day - dayFrom])
			{
				@Override
				protected Map<String, String> getParams() throws AuthFailureError
				{
					Map<String, String> params = new HashMap<String, String>();
					return params;
				}

				@Override
				public Map<String, String> getHeaders() throws AuthFailureError
				{
					Map<String, String> header = new HashMap<String, String>();
					header.put("cookie", _cookie);
					header.put("Connection", "close");
					return header;
				}

				@Override
				protected Response<String> parseNetworkResponse(NetworkResponse response)
				{
					Map<String, String> responseHeaders = response.headers;
					_cookie = responseHeaders.get("set-cookie");
					return super.parseNetworkResponse(response);
				}
			};
			Log.i(TAG, "call " + stringRequest.getUrl());
			_requestQueue.add(stringRequest);
		}
	}

	private class ProgramsResponseCallback implements Response.Listener<String>, Response.ErrorListener
	{
		protected OnResultReceived _onResultReceived;
		public static final String FORMAT_PROGRAM_ID = "yyyyMMddHHmmss";
		private SimpleDateFormat _dfUtc;
		private FeatureTimeZone _featureTimeZone;
		private Map<String, List<Program>> _channelsToPrograms = new HashMap<String, List<Program>>();
		private int _day;

		ProgramsResponseCallback(int day, OnResultReceived onResultReceived)
		{
			_day = day;
			_onResultReceived = onResultReceived;
			_dfUtc = new SimpleDateFormat(FORMAT_PROGRAM_ID, Locale.getDefault());
			_dfUtc.setTimeZone(TimeZone.getTimeZone("UTC"));
			_featureTimeZone = (FeatureTimeZone) Environment.getInstance().getFeatureComponent(
			        FeatureName.Component.TIMEZONE);
		}

		@Override
		public void onResponse(String response)
		{
			Log.i(TAG, ".onResponse: response = " + response);
			try
			{
				JSONObject jsonObj = new JSONObject(response);
				JSONArray jsonChannels = jsonObj.getJSONArray("channels");
				for (int i = 0; i < jsonChannels.length(); i++)
				{
					JSONObject jsonChannel = (JSONObject) jsonChannels.get(i);

					Channel channel = _channelsMap.get(jsonChannel.get("cid"));
					if (channel == null)
					{
						Log.w(TAG, "Can't find channel " + jsonChannel.get("cid") + " in the initial map");
					}
					else
					{
						JSONArray jsonPrograms = (JSONArray) jsonChannel.get("programs");
						Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
						List<Program> programList = new ArrayList<Program>(jsonPrograms.length());
						for (int j = 0; j < jsonPrograms.length(); j++)
						{
							JSONObject jsonProgram = (JSONObject) jsonPrograms.get(j);

							// creates zattoo program instance
							ProgramZattoo program = new ProgramZattoo(_dfUtc.format(cal.getTime()), channel);
							program.setZapiID(jsonProgram.getString("id"));

							// set program start time
							cal.setTimeInMillis(jsonProgram.getLong("s") * 1000);
							Calendar startTime = new GregorianCalendar(_featureTimeZone.getTimeZone());
							startTime.setTimeInMillis(cal.getTimeInMillis());
							program.setStartTime(startTime);

							// set program end time
							cal.setTimeInMillis(jsonProgram.getLong("e") * 1000);
							Calendar endTime = new GregorianCalendar(_featureTimeZone.getTimeZone());
							endTime.setTimeInMillis(cal.getTimeInMillis());
							program.setStopTime(endTime);

							// set program title
							program.setTitle(jsonProgram.getString("t"));

							// add program to list and calendar map
							programList.add(program);
						}
						_channelsToPrograms.put(channel.getChannelId(), programList);
					}
				}
				Bundle bundle = new Bundle();
				bundle.putInt("day", _day);
				_onResultReceived.onReceiveResult(new FeatureError(_ownerFeature, ResultCode.OK, bundle));
			}
			catch (JSONException e)
			{
				Log.e(TAG, e.getMessage(), e);
				_onResultReceived.onReceiveResult(new FeatureError(_ownerFeature, ResultCode.PROTOCOL_ERROR, e));
			}
		}

		@Override
		public void onErrorResponse(VolleyError error)
		{
			Log.i(TAG, ".onErrorResponse: " + error);
			if (error.networkResponse != null)
				Log.e(TAG, ".onErrorResponse: data = " + new String(error.networkResponse.data));
			_onResultReceived.onReceiveResult(new FeatureError(_ownerFeature, error));
		}

		public Map<String, List<Program>> getChannelsToPrograms()
		{
			return _channelsToPrograms;
		}
	}

	public void watch(final String channelId, final String streamType, final boolean isEthernet,
	        OnResultReceived onResultReceived)
	{
		ResponseCallbackWatch responseCallback = new ResponseCallbackWatch(onResultReceived);
		StringRequest stringRequest = new StringRequest(Request.Method.POST, _baseUri + "/zapi/watch",
		        responseCallback, responseCallback)
		{
			@Override
			public Map<String, String> getHeaders() throws AuthFailureError
			{
				Map<String, String> headers = new HashMap<String, String>();
				headers.put("Connection", "close");
				return headers;
			}

			@Override
			protected Map<String, String> getParams() throws AuthFailureError
			{
				Map<String, String> params = new HashMap<String, String>();
				params.put("cid", channelId);
				params.put("stream_type", streamType);
				if (isEthernet)
				{
					if (_initRateEth > 0)
						params.put("initialrate", String.valueOf(_initRateEth));
					if (_minRateEth > 0)
						params.put("minrate", String.valueOf(_minRateEth));
					if (_maxRateEth > 0)
						params.put("maxrate", String.valueOf(_maxRateEth));
				}
				else
				{
					if (_initRateWifi > 0)
						params.put("initialrate", String.valueOf(_initRateWifi));
					if (_minRateWifi > 0)
						params.put("minrate", String.valueOf(_minRateWifi));
					if (_maxRateWifi > 0)
						params.put("maxrate", String.valueOf(_maxRateWifi));
				}
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
			_onResultReceived.onReceiveResult(FeatureError.OK(_ownerFeature));
		}

		@Override
		public void onErrorResponse(VolleyError error)
		{
			Log.i(TAG, ".onErrorResponse: " + error);
			if (error.networkResponse != null)
				Log.e(TAG, ".onErrorResponse: data = " + new String(error.networkResponse.data));
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
			if (error.networkResponse != null)
				Log.e(TAG, ".onErrorResponse: data = " + new String(error.networkResponse.data));
			_onResultReceived.onReceiveResult(new FeatureError(_ownerFeature, error));
		}
	}

	public void retrieveProgramDetails(final ProgramZattoo program, OnResultReceived onResultReceived)
	{
		ResponseCallbackDetails responseCallback = new ResponseCallbackDetails(program, onResultReceived);
		StringRequest stringRequest = new StringRequest(Request.Method.GET, _baseUri
		        + "/zapi/program/details?program_id=" + program.getZapiID(), responseCallback, responseCallback)
		{
			@Override
			public Map<String, String> getHeaders() throws AuthFailureError
			{
				Map<String, String> headers = new HashMap<String, String>();
				headers.put("Connection", "close");
				return headers;
			}

			@Override
			protected Response<String> parseNetworkResponse(NetworkResponse response)
			{
				Map<String, String> responseHeaders = response.headers;
				_cookie = responseHeaders.get("set-cookie");
				return super.parseNetworkResponse(response);
			}
		};
		Log.i(TAG, "call " + stringRequest.getUrl());
		_requestQueue.add(stringRequest);
	}

	private class ResponseCallbackDetails extends ResponseCallback
	{
		private Program _program;

		ResponseCallbackDetails(Program program, OnResultReceived onResultReceived)
		{
			super(onResultReceived);
			_program = program;
		}

		@Override
		public void onResponse(String response)
		{
			Log.i(TAG, ".onResponse: " + response);
			try
			{
				JSONObject json = new JSONObject(response);
				JSONObject jsonProgram = json.getJSONObject("program");
				_program.setDetails(jsonProgram);
				super.onResponse(response);
			}
			catch (JSONException e)
			{
				Log.e(TAG, e.getMessage(), e);
				_onResultReceived.onReceiveResult(new FeatureError(_ownerFeature, e));
			}
		}

		@Override
		public void onErrorResponse(VolleyError error)
		{
			Log.i(TAG, ".onErrorResponse: " + error);
			if (error.networkResponse != null)
				Log.e(TAG, ".onErrorResponse: data = " + new String(error.networkResponse.data));
			_onResultReceived.onReceiveResult(new FeatureError(_ownerFeature, error));
		}
	}

}

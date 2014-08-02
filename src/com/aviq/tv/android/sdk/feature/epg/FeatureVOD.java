package com.aviq.tv.android.sdk.feature.epg;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;
import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Scheduler;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureScheduler;
import com.google.gson.JsonSyntaxException;

public class FeatureVOD extends FeatureScheduler
{
	
	public static final String TAG = FeatureEPG.class.getSimpleName();
	
	private String _vodServerURL;
	private RequestQueue _httpQueue;
	private OnFeatureInitialized _onFeatureInitialized;
	private XMLVodParser _xmlParser;
	private VodGroup _vodData;
	
	public enum Param
	{
		
		/**
		 * VOD XML URL
		 */
		VOD_XML_URL("http://185.4.83.193/?xml&vod"),
		
		/**
		 * Schedule interval
		 */
		VOD_UPDATE_INTERVAL(24 * 60 * 60 * 1000);
		
		Param(String value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Scheduler.VOD).put(name(), value);
		}
		
		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Scheduler.VOD).put(name(), value);
		}
	}
	
	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		_vodServerURL = getPrefs().getString(Param.VOD_XML_URL);
		_httpQueue = Environment.getInstance().getRequestQueue();
		_xmlParser = new XMLVodParser();

		try
		{
			_xmlParser.initialize();
			onSchedule(onFeatureInitialized);
		}
		catch (ParserConfigurationException e)
		{
			Log.e(TAG, "Error during XML SAX Parser initialization " + e.getMessage());
			onFeatureInitialized.onInitialized(FeatureVOD.this, ResultCode.GENERAL_FAILURE);
		}
		catch (SAXException e)
		{
			Log.e(TAG, "Error during XML SAX Parser initialization " + e.getMessage());
			onFeatureInitialized.onInitialized(FeatureVOD.this, ResultCode.PROTOCOL_ERROR);
		}
	}
	
	public VodGroup getVodData()
	{
		return _vodData;
	}

	@Override
	protected void onSchedule(OnFeatureInitialized onFeatureInitialized)
	{
		_onFeatureInitialized = onFeatureInitialized;
		
		Log.i(TAG, "Retrieving VOD data from " + _vodServerURL);
		
		VodListResponseCallback responseCallback = new VodListResponseCallback();
		
		XmlRequest<VodGroup> channelListRequest = new XmlRequest<VodGroup>(Request.Method.GET, _vodServerURL,
		        VodGroup.class, responseCallback, responseCallback);
		
		_httpQueue.add(channelListRequest);
		
		scheduleDelayed(getPrefs().getInt(Param.VOD_UPDATE_INTERVAL));
	}
	
	@Override
	public Scheduler getSchedulerName()
	{
		// TODO Auto-generated method stub
		return FeatureName.Scheduler.VOD;
	}
	
	// XML volley request
	private class XmlRequest<T> extends Request<T>
	{
		
		private final Class<T> mClazz;
		private final Listener<T> mListener;
		
		public XmlRequest(int method, String url, Class<T> clazz, Listener<T> listener, ErrorListener errorListener)
		{
			super(Method.GET, url, errorListener);
			this.mClazz = clazz;
			this.mListener = listener;
			
		}
		
		@Override
		protected void deliverResponse(T response)
		{
			mListener.onResponse(response);
		}
		
		@Override
		protected Response<T> parseNetworkResponse(NetworkResponse response)
		{
			try
			{
				String inputString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
				T xml = mClazz.cast(_xmlParser.fromXMl(inputString));
				return Response.success(xml, HttpHeaderParser.parseCacheHeaders(response));
			}
			catch (UnsupportedEncodingException e)
			{
				return Response.error(new ParseError(e));
			}
			catch (JsonSyntaxException e)
			{
				return Response.error(new ParseError(e));
			}
			catch (SAXException e)
			{
				// TODO Auto-generated catch block
				return Response.error(new ParseError(e));
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				return Response.error(new ParseError(e));
			}
		}
	}
	
	private class VodListResponseCallback implements Response.Listener<VodGroup>, Response.ErrorListener
	{
		@Override
		public void onResponse(VodGroup response)
		{
			_vodData = response;			
			_onFeatureInitialized.onInitialized(FeatureVOD.this, ResultCode.OK);
		}
		
		@Override
		public void onErrorResponse(VolleyError error)
		{
			int statusCode = error.networkResponse != null ? error.networkResponse.statusCode
			        : ResultCode.GENERAL_FAILURE;
			Log.e(TAG, "Error retrieving vod data with code " + statusCode + ": " + error);
			_onFeatureInitialized.onInitialized(FeatureVOD.this, statusCode);
		}
		
	}
	
}

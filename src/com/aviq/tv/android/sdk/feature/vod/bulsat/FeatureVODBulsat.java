package com.aviq.tv.android.sdk.feature.vod.bulsat;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Scheduler;
import com.aviq.tv.android.sdk.core.feature.IFeature.OnFeatureInitialized;
import com.aviq.tv.android.sdk.feature.vod.FeatureVOD;
import com.google.gson.JsonSyntaxException;

public class FeatureVODBulsat extends FeatureVOD
{
	public static final String TAG = FeatureVODBulsat.class.getSimpleName();

	private OnFeatureInitialized _onFeatureInitialized;
	private VodTree<VodGroup> _vodData;

	public enum Param
	{
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
		onSchedule(onFeatureInitialized);
	}

	@Override
	public Scheduler getSchedulerName()
	{
		return FeatureName.Scheduler.VOD;
	}

	@Override
	protected void onSchedule(OnFeatureInitialized onFeatureInitialized)
	{
		_onFeatureInitialized = onFeatureInitialized;

		String vodServerURL = getPrefs().getString(Param.VOD_XML_URL);
		Log.i(TAG, "Retrieving VOD data from: " + vodServerURL);

		VodListResponseCallback responseCallback = new VodListResponseCallback();

		@SuppressWarnings("rawtypes")
		VodRequest<VodTree> vodRequest = new VodRequest<VodTree>(Request.Method.GET, vodServerURL,
				VodTree.class, responseCallback, responseCallback);
		Environment.getInstance().getRequestQueue().add(vodRequest);

		scheduleDelayed(getPrefs().getInt(Param.VOD_UPDATE_INTERVAL));
	}

	public VodTree<VodGroup> getVodData()
	{
		return _vodData;
	}

	private class VodRequest<T> extends Request<T>
	{
		private final Class<T> mClazz;
		private final Listener<T> mListener;

		public VodRequest(int method, String url, Class<T> clazz, Listener<T> listener, ErrorListener errorListener)
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

				VodXmlParser xmlParser = new VodXmlParser();
				try
				{
					xmlParser.initialize();
				}
				catch (ParserConfigurationException e)
				{
					Log.e(TAG, "Cannot configure SAX parser.", e);
					return Response.error(new VolleyError(e));
				}
				catch (SAXException e)
				{
					Log.e(TAG, "SAX parser error.", e);
					return Response.error(new VolleyError(e));
				}

				T xml = mClazz.cast(xmlParser.fromXML(inputString));
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
				return Response.error(new ParseError(e));
			}
			catch (IOException e)
			{
				return Response.error(new ParseError(e));
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private class VodListResponseCallback implements Response.Listener<VodTree>, Response.ErrorListener
	{
		@SuppressWarnings("unchecked")
		@Override
		public void onResponse(VodTree response)
		{
			_vodData = response;

			//print(_vodData.getRoot()); // Dump tree data to logcat

			_onFeatureInitialized.onInitialized(FeatureVODBulsat.this, ResultCode.OK);
		}

		@Override
		public void onErrorResponse(VolleyError error)
		{
			int statusCode = error.networkResponse != null ? error.networkResponse.statusCode
			        : ResultCode.GENERAL_FAILURE;
			Log.e(TAG, "Error retrieving VOD data: code " + statusCode + ": " + error);
			_onFeatureInitialized.onInitialized(FeatureVODBulsat.this, statusCode);
		}

	}

	//---------------------------------------------------
	// DEBUGGING CODE TO DUMP THE VOD TREE RECURSIVELY
	//---------------------------------------------------

	private int _treeDepth;

	private void print(VodTree.Node<VodGroup> node)
	{
		if (node.equals(_vodData.getRoot()))
			_treeDepth = 0;

		Log.v(TAG, "depth = " + _treeDepth
				+ ", title = " + node.getData().getTitle()
				+ ", num VODs = " + node.getData().getVodList().size());

		_treeDepth++;
		if (node.hasChildren())
		{
			for (VodTree.Node<VodGroup> child : node.getChildren())
				print(child);
		}
		_treeDepth--;
	}
}

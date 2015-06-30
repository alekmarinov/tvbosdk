/**
\ * Copyright (c) 2007-2015, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureVOD.java
 * Author:      alek
 * Date:        19 Jan 2015
 * Description: Feature providing VOD data
 */
package com.aviq.tv.android.sdk.feature.vod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.os.Bundle;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.aviq.tv.android.sdk.R;
import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.EventReceiver;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Scheduler;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.FeatureScheduler;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.command.CommandHandler;
import com.aviq.tv.android.sdk.feature.system.FeatureDevice.DeviceAttribute;

/**
 * Feature providing VOD data
 */
@Author("alek")
public abstract class FeatureVOD extends FeatureScheduler
{
	public static final String TAG = FeatureVOD.class.getSimpleName();
	public static final int ON_VOD_UPDATED = EventMessenger.ID("ON_VOD_UPDATED");
	public static final int ON_VOD_SEARCH = EventMessenger.ID("ON_VOD_SEARCH");
	
	public static enum Command
	{
		GET_VODGROUPS, GET_VODITEMS, GET_VODDETAILS
	}
	
	public static enum CommandGetVodGroupsExtras
	{
		VOD_GROUP_ID
	}
	
	public static enum CommandGetVodItemsExtras
	{
		VOD_GROUP_ID
	}
	
	public static enum CommandGetVodDetailsExtras
	{
		VOD_ITEM_ID
	}
	
	public enum OnVodSearchExtra
	{
		TEXT
	}
	
	public static enum Param
	{
		/**
		 * The main url to the VOD server
		 */
		VOD_SERVER("http://avtv.intelibo.com"),
		
		/**
		 * The VOD service version
		 */
		VOD_VERSION(1),
		
		/**
		 * The VOD provider
		 */
		VOD_PROVIDER("bulsat"),
		
		/**
		 * Schedule interval
		 */
		UPDATE_INTERVAL(24 * 60 * 60 * 1000),
		
		/**
		 * VOD groups url format
		 */
		VOD_GROUPS_URL("${SERVER}/v${VERSION}/vod/${PROVIDER}"),
		
		/**
		 * VOD items url format
		 */
		VOD_ITEMS_URL("${SERVER}/v${VERSION}/vod/${PROVIDER}/*"),
		
		/**
		 * VOD details url format
		 */
		VOD_DETAILS_URL("${SERVER}/v${VERSION}/vod/${PROVIDER}/${GROUP}/${ITEM}"),
		
		/**
		 * VOD image url format
		 */
		VOD_IMAGE_URL("${SERVER}/static/${PROVIDER}/vod/${IMAGE}"),
		
		/**
		 * VOD search url format
		 */
		VOD_SEARCH_URL("${SERVER}/v${VERSION}/search/vod/${PROVIDER}?text=${TEXT}"),
		
		/**
		 * VOD rate url format
		 */
		VOD_RATE_URL("${SERVER}/v${VERSION}/rate/vod/${PROVIDER}/${BOXID}/${ITEM}"),
		
		/**
		 * VOD recommend url format
		 */
		VOD_RECOMMEND_URL("${SERVER}/v${VERSION}/recommend/vod/${PROVIDER}/${BOXID}?max=${MAX_RECOMMENDED}"),
		
		/**
		 * The maximum recommended VOD items
		 */
		MAX_RECOMMENDED(30);
		
		Param(boolean value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Scheduler.VOD).put(name(), value);
		}
		
		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Scheduler.VOD).put(name(), value);
		}
		
		Param(String value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Scheduler.VOD).put(name(), value);
		}
	}
	
	protected RequestQueue _requestQueue;
	private OnFeatureInitialized _onFeatureInitialized;
	private int _vodVersion;
	private String _vodServer;
	private String _vodProvider;
	protected VodData _vodData;
	protected VodData _vodDataBeingLoaded;
	private int _maxRecommended;
	private String _lastSearchTerm;
	private List<VODItem> _lastSearchResults = new ArrayList<VODItem>();
	
	private static String CYRILLIC_CHARS = null;
	private static String LATIN_CHARS = null;
	
	public FeatureVOD() throws FeatureNotFoundException
	{
		require(FeatureName.Scheduler.INTERNET);
		require(FeatureName.Component.DEVICE);
		require(FeatureName.State.NETWORK_WIZARD);
		require(FeatureName.Component.COMMAND);
	}
	
	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		
		_vodProvider = getPrefs().getString(Param.VOD_PROVIDER);
		_vodVersion = getPrefs().getInt(Param.VOD_VERSION);
		_vodServer = getPrefs().getString(Param.VOD_SERVER);
		_maxRecommended = getPrefs().getInt(Param.MAX_RECOMMENDED);
		_requestQueue = Environment.getInstance().getRequestQueue();
		
		CYRILLIC_CHARS = Environment.getInstance().getResources().getString(R.string.cyrillic_chars);
		LATIN_CHARS = Environment.getInstance().getResources().getString(R.string.latin_chars);
		
		_feature.Component.COMMAND.addCommandHandler(new OnCommandGetVODGroups());
		_feature.Component.COMMAND.addCommandHandler(new OnCommandGetVODItems());
		_feature.Component.COMMAND.addCommandHandler(new OnCommandGetVODDetails());
		
		Environment.getInstance().getEventMessenger().register(new EventReceiver()
		{
			@Override
			public void onEvent(int msgId, Bundle bundle)
			{
				testCommands();
				Environment.getInstance().getEventMessenger().unregister(this, Environment.ON_LOADED);
			}
		}, Environment.ON_LOADED);
		
		onSchedule(onFeatureInitialized);
	}
	
	private void testCommands()
	{
		OnResultReceived onResultReceived = new OnResultReceived()
		{
			@Override
			public void onReceiveResult(FeatureError error, Object object)
			{
				if (error.isError())
				{
					Log.e(TAG, error.getMessage(), error);
				}
				else
				{
					JSONArray jsonArr = (JSONArray) object;
					try
					{
						Log.i(TAG, "== " + jsonArr.length() + " objects returned");
						for (int i = 0; i < jsonArr.length(); i++)
						{
							JSONObject jsonObj = jsonArr.getJSONObject(i);
							Log.i(TAG, jsonObj.toString());
						}
					}
					catch (JSONException e)
					{
						Log.e(TAG, e.getMessage(), e);
					}
				}
			}
		};
		/*
		 * _feature.Component.COMMAND.execute(Command.GET_VODGROUPS.name(), new
		 * Bundle(), onResultReceived);
		 * bundle = new Bundle();
		 * /*We must insert something in the bundle, cannot do the test with
		 * null
		 * bundle
		 * id: "ict_8", title: "Комедия/ Семеен", group: false
		 * bundle.putString(CommandGetVodItemsExtras.VOD_GROUP_ID.name(),
		 * "ict_8");
		 * _feature.Component.COMMAND.execute(Command.GET_VODITEMS.name(),
		 * bundle, onResultReceived);
		 */
		
		onResultReceived = new OnResultReceived()
		{
			@Override
			public void onReceiveResult(FeatureError error, Object object)
			{
				if (error.isError())
				{
					Log.e(TAG, error.getMessage(), error);
				}
				else
				{
					JSONObject jsonObj = (JSONObject) object;
					Log.i(TAG, "JSON object returned");
					
					Log.i(TAG, jsonObj.toString());
				}
			}
		};
		// after OnResultReceived we call the tests...
		Bundle bundle = new Bundle();
		Log.i(TAG, "CommandGetVodItemsExtras.VOD_GROUP_ID.name():" + CommandGetVodItemsExtras.VOD_GROUP_ID.name());
		bundle.putString(CommandGetVodDetailsExtras.VOD_ITEM_ID.name(), "ivd_867");
		_feature.Component.COMMAND.execute(Command.GET_VODDETAILS.name(), bundle, onResultReceived);
	}
	
	@Override
	protected void onSchedule(OnFeatureInitialized onFeatureInitialized)
	{
		_onFeatureInitialized = onFeatureInitialized;
		
		// Load VOD groups from server
		String vodGroupsUrl = getVodGroupsUrl();
		
		Log.i(TAG, "Retrieving VOD groups from " + vodGroupsUrl);
		VodGroupResponseCallback responseCallback = new VodGroupResponseCallback();
		JsonObjectRequest vodGroupsRequest = new JsonObjectRequest(vodGroupsUrl, null, responseCallback,
		        responseCallback);
		_requestQueue.add(vodGroupsRequest);
		
		// schedule update later
		scheduleDelayed(getPrefs().getInt(Param.UPDATE_INTERVAL));
	}
	
	private class VodGroupResponseCallback implements Response.Listener<JSONObject>, Response.ErrorListener
	{
		@Override
		public void onResponse(JSONObject response)
		{
			try
			{
				VODGroup.MetaData metaData = createVodGroupMetaData();
				JSONArray jsonArr = response.getJSONArray("meta");
				String[] meta = new String[jsonArr.length()];
				for (int i = 0; i < jsonArr.length(); i++)
					meta[i] = jsonArr.get(i).toString();
				indexVodGroupMetaData(metaData, meta);
				_vodDataBeingLoaded = parseVodGroupData(metaData, response.getJSONArray("data"));
				
				// Load VOD items from server
				String vodItemsUrl = getVodItemsUrl();
				Log.i(TAG, "Retrieving VOD items from " + vodItemsUrl);
				VodItemsResponseCallback responseCallback = new VodItemsResponseCallback();
				JsonObjectRequest vodItemsRequest = new JsonObjectRequest(vodItemsUrl, null, responseCallback,
				        responseCallback);
				_requestQueue.add(vodItemsRequest);
				
			}
			catch (JSONException e)
			{
				// Vod group load failed, notify error
				Log.e(TAG, e.getMessage(), e);
				_onFeatureInitialized.onInitialized(new FeatureError(FeatureVOD.this, ResultCode.PROTOCOL_ERROR, e));
			}
		}
		
		@Override
		public void onErrorResponse(VolleyError error)
		{
			// FIXME: This error occurs when the VOD server is down. Show
			// appropriate message
			int statusCode = error.networkResponse != null ? error.networkResponse.statusCode
			        : ResultCode.GENERAL_FAILURE;
			Log.e(TAG, "Error retrieving VOD groups with code " + statusCode + ": " + error);
			_onFeatureInitialized.onInitialized(new FeatureError(FeatureVOD.this, statusCode, error));
		}
	}
	
	private class VodItemsResponseCallback implements Response.Listener<JSONObject>, Response.ErrorListener
	{
		@Override
		public void onResponse(JSONObject response)
		{
			try
			{
				VODItem.MetaData metaData = createVodItemMetaData();
				JSONArray jsonArr = response.getJSONArray("meta");
				String[] meta = new String[jsonArr.length()];
				for (int i = 0; i < jsonArr.length(); i++)
					meta[i] = jsonArr.get(i).toString();
				
				// the meta data is the same as for vod group
				indexVodItemMetaData(metaData, meta);
				parseVodItemsData(_vodDataBeingLoaded, metaData, response.getJSONArray("data"));
				
				_vodData = _vodDataBeingLoaded;
				_vodDataBeingLoaded = null;
				getEventMessenger().trigger(ON_VOD_UPDATED);
				FeatureVOD.super.initialize(_onFeatureInitialized);
			}
			catch (JSONException e)
			{
				// Vod items load failed, notify error
				Log.e(TAG, e.getMessage(), e);
				_onFeatureInitialized.onInitialized(new FeatureError(FeatureVOD.this, ResultCode.PROTOCOL_ERROR, e));
			}
		}
		
		@Override
		public void onErrorResponse(VolleyError error)
		{
			// FIXME: This error occurs when the VOD server is down. Show
			// appropriate message
			int statusCode = error.networkResponse != null ? error.networkResponse.statusCode
			        : ResultCode.GENERAL_FAILURE;
			Log.e(TAG, "Error retrieving VOD groups with code " + statusCode + ": " + error);
			_onFeatureInitialized.onInitialized(new FeatureError(FeatureVOD.this, statusCode, error));
		}
	}
	
	private class VodDetailsResponseCallback implements Response.Listener<JSONObject>, Response.ErrorListener
	{
		private VODItem _vodItem;
		private OnResultReceived _onResultReceived;
		
		public VodDetailsResponseCallback(VODItem vodItem, OnResultReceived onResultReceived)
		{
			_vodItem = vodItem;
			_onResultReceived = onResultReceived;
		}
		
		@Override
		public void onResponse(JSONObject response)
		{
			try
			{
				_vodItem.setDetails(response);
				_onResultReceived.onReceiveResult(FeatureError.OK, null);
			}
			catch (JSONException e)
			{
				// Vod details load failed, notify error
				Log.e(TAG, e.getMessage(), e);
				_onResultReceived.onReceiveResult(new FeatureError(FeatureVOD.this, e), null);
			}
		}
		
		@Override
		public void onErrorResponse(VolleyError error)
		{
			int statusCode = error.networkResponse != null ? error.networkResponse.statusCode
			        : ResultCode.GENERAL_FAILURE;
			Log.e(TAG, "Error retrieving VOD details with code " + statusCode + ": " + error);
			_onResultReceived.onReceiveResult(new FeatureError(FeatureVOD.this, statusCode, error), null);
		}
	}
	
	protected VODGroup.MetaData createVodGroupMetaData()
	{
		return new VODGroup.MetaData();
	}
	
	protected VODItem.MetaData createVodItemMetaData()
	{
		return new VODItem.MetaData();
	}
	
	protected void indexVodGroupMetaData(VODGroup.MetaData metaData, String[] meta)
	{
		for (int j = 0; j < meta.length; j++)
		{
			String key = meta[j];
			
			if ("id".equals(key))
				metaData.metaVodGroupId = j;
			else if ("title".equals(key))
				metaData.metaVodGroupTitle = j;
			else if ("parent".equals(key))
				metaData.metaVodGroupParent = j;
		}
	}
	
	protected void indexVodItemMetaData(VODItem.MetaData metaData, String[] meta)
	{
		indexVodGroupMetaData(metaData, meta);
	}
	
	private VodData parseVodGroupData(VODGroup.MetaData metaData, JSONArray data) throws JSONException
	{
		List<VODGroup> newVodGroupList = new ArrayList<VODGroup>();
		Map<String, VODGroup> vodMap = new HashMap<String, VODGroup>();
		
		for (int i = 0; i < data.length(); i++)
		{
			JSONArray jsonArr = data.getJSONArray(i);
			String[] values = new String[jsonArr.length()];
			for (int j = 0; j < jsonArr.length(); j++)
			{
				if (jsonArr.get(j) != null)
				{
					if (!jsonArr.isNull(j))
						values[j] = jsonArr.get(j).toString();
				}
			}
			
			VODGroup parent = null;
			String parentId = values[metaData.metaVodGroupParent];
			if (parentId != null)
				parent = vodMap.get(parentId);
			
			String vodGroupId = values[metaData.metaVodGroupId];
			String vodGroupTitle = values[metaData.metaVodGroupTitle];
			VODGroup vodGroup = createVodGroup(vodGroupId, vodGroupTitle, parent);
			vodMap.put(vodGroup.getId(), vodGroup);
			
			vodGroup.setAttributes(metaData, values);
			
			newVodGroupList.add(vodGroup);
		}
		
		return new VodData(newVodGroupList);
	}
	
	private void parseVodItemsData(VodData vodData, VODItem.MetaData metaData, JSONArray data) throws JSONException
	{
		Map<VODGroup, List<VODItem>> newVodGroupItemsMap = new HashMap<VODGroup, List<VODItem>>();
		
		for (int i = 0; i < data.length(); i++)
		{
			JSONArray jsonArr = data.getJSONArray(i);
			String[] values = new String[jsonArr.length()];
			for (int j = 0; j < jsonArr.length(); j++)
			{
				if (!jsonArr.isNull(j))
				{
					if (jsonArr.get(j) != null)
						values[j] = jsonArr.get(j).toString();
				}
			}
			
			String vodItemId = values[metaData.metaVodGroupId];
			String parentId = values[metaData.metaVodGroupParent];
			if (parentId != null)
			{
				VODGroup parent = vodData.getVodGroupById(parentId);
				String vodItemTitle = values[metaData.metaVodGroupTitle];
				VODItem vodItem = createVodItem(vodItemId, vodItemTitle, parent);
				vodItem.setAttributes(metaData, values);
				
				List<VODItem> vodItems = newVodGroupItemsMap.get(parent);
				if (vodItems == null)
				{
					vodItems = new ArrayList<VODItem>();
					newVodGroupItemsMap.put(parent, vodItems);
				}
				vodItems.add(vodItem);
			}
			else
			{
				Log.w(TAG, "Parent id is missing in vod item " + vodItemId);
			}
		}
		
		vodData.setVodGroupItems(newVodGroupItemsMap);
	}
	
	/**
	 * @param id
	 *            the VOD group id
	 * @param title
	 *            the VOD group title
	 * @param parent
	 *            the parent group of this VOD group
	 * @return new VodGroup instance
	 */
	protected abstract VODGroup createVodGroup(String id, String title, VODGroup parent);
	
	/**
	 * @param id
	 *            the VOD item id
	 * @param title
	 *            the VOD item title
	 * @param parent
	 *            the parent group of this VOD item
	 * @return new VodItem instance
	 */
	protected abstract VODItem createVodItem(String id, String title, VODGroup parent);
	
	protected String getVodGroupsUrl()
	{
		Bundle bundle = new Bundle();
		bundle.putString("SERVER", _vodServer);
		bundle.putInt("VERSION", _vodVersion);
		bundle.putString("PROVIDER", _vodProvider);
		
		return getPrefs().getString(Param.VOD_GROUPS_URL, bundle);
	}
	
	protected String getVodItemsUrl()
	{
		Bundle bundle = new Bundle();
		bundle.putString("SERVER", _vodServer);
		bundle.putInt("VERSION", _vodVersion);
		bundle.putString("PROVIDER", _vodProvider);
		
		return getPrefs().getString(Param.VOD_ITEMS_URL, bundle);
	}
	
	public String getVodImageUrl(String imageName)
	{
		Bundle bundle = new Bundle();
		bundle.putString("SERVER", _vodServer);
		bundle.putString("PROVIDER", _vodProvider);
		bundle.putString("IMAGE", imageName);
		return getPrefs().getString(Param.VOD_IMAGE_URL, bundle);
	}
	
	protected String getVodDetailsUrl(String vodGroupId, String vodItemId)
	{
		Bundle bundle = new Bundle();
		bundle.putString("SERVER", _vodServer);
		bundle.putInt("VERSION", _vodVersion);
		bundle.putString("PROVIDER", _vodProvider);
		bundle.putString("GROUP", vodGroupId);
		bundle.putString("ITEM", vodItemId);
		return getPrefs().getString(Param.VOD_DETAILS_URL, bundle);
	}
	
	protected String getVodSearchUrl(String text)
	{
		Bundle bundle = new Bundle();
		bundle.putString("SERVER", _vodServer);
		bundle.putInt("VERSION", _vodVersion);
		bundle.putString("PROVIDER", _vodProvider);
		bundle.putString("TEXT", Uri.encode(text));
		
		return getPrefs().getString(Param.VOD_SEARCH_URL, bundle);
	}
	
	protected String getRateUrl(long boxId, long vodItemId)
	{
		Bundle bundle = new Bundle();
		bundle.putString("SERVER", _vodServer);
		bundle.putInt("VERSION", _vodVersion);
		bundle.putString("PROVIDER", _vodProvider);
		bundle.putLong("BOXID", boxId);
		bundle.putLong("ITEM", vodItemId);
		return getPrefs().getString(Param.VOD_RATE_URL, bundle);
	}
	
	protected String getRecommendUrl(long boxId)
	{
		Bundle bundle = new Bundle();
		bundle.putString("SERVER", _vodServer);
		bundle.putInt("VERSION", _vodVersion);
		bundle.putString("PROVIDER", _vodProvider);
		bundle.putLong("BOXID", boxId);
		bundle.putInt("MAX_RECOMMENDED", _maxRecommended);
		return getPrefs().getString(Param.VOD_RECOMMEND_URL, bundle);
	}
	
	@Override
	public Scheduler getSchedulerName()
	{
		return FeatureName.Scheduler.VOD;
	}
	
	/**
	 * Gets VODGroup by ID
	 *
	 * @param id
	 * @return VODGroup
	 */
	public VODGroup getVodGroupById(String id)
	{
		return _vodData.getVodGroupById(id);
	}
	
	/**
	 * Gets VODItem by ID
	 *
	 * @param id
	 * @return VODItem
	 */
	public VODItem getVodItemById(String id)
	{
		return _vodData.getVodItemById(id);
	}
	
	/**
	 * @param group
	 * @param parent
	 * @return true if parent is equal to group ot its direct or indirect parent
	 */
	public boolean isParentToGroup(VODGroup parent, VODGroup group)
	{
		if (group == parent)
			return true;
		else
			return group != null && isParentToGroup(parent, group.getParent());
	}
	
	/**
	 * Loads VOD groups which direct parent is the specified vodGroupId
	 *
	 * @param vodGroupId
	 *            the id of the parent VOD group
	 * @param vodGroups
	 *            out list with child VOD groups
	 * @param onResultReceived
	 */
	public void loadVodGroups(String vodGroupId, List<VODGroup> vodGroups, OnResultReceived onResultReceived)
	{
		VODGroup vodGroup = _vodData.getVodGroupById(vodGroupId);
		vodGroups.addAll(_vodData.getVodGroups(vodGroup));
		if (onResultReceived != null)
			onResultReceived.onReceiveResult(FeatureError.OK, null);
	}
	
	/**
	 * Returns long representation of vod item id
	 *
	 * @param vodId
	 * @return vod item id
	 */
	abstract protected long convertVodIdToLong(String vodId);
	
	/**
	 * Returns long representation of box id
	 *
	 * @param boxId
	 * @return box item id
	 */
	abstract protected long convertBoxIdToLong(String boxId);
	
	/**
	 * Rate vod item
	 *
	 * @param vodItem
	 * @param rating
	 * @param onResultReceived
	 */
	public void rateVodItem(final VODItem vodItem, final int rating, final OnResultReceived onResultReceived)
	{
		long boxId = convertBoxIdToLong(_feature.Component.DEVICE.getDeviceAttribute(DeviceAttribute.MAC));
		long vodId = convertVodIdToLong(vodItem.getId());
		RateResponseCallback responseCallback = new RateResponseCallback(onResultReceived);
		StringRequest rateRequest = new StringRequest(Request.Method.POST, getRateUrl(boxId, vodId), responseCallback,
		        responseCallback)
		{
			@Override
			protected Map<String, String> getParams()
			{
				Map<String, String> params = new HashMap<String, String>();
				params.put("rating", String.valueOf(rating));
				return params;
			}
		};
		_requestQueue.add(rateRequest);
	}
	
	/**
	 * Get vod item rating
	 *
	 * @param vodItem
	 * @param onResultReceived
	 */
	public void getVodItemRating(final VODItem vodItem, final OnResultReceived onResultReceived)
	{
		long boxId = convertBoxIdToLong(_feature.Component.DEVICE.getDeviceAttribute(DeviceAttribute.MAC));
		long vodId = convertVodIdToLong(vodItem.getId());
		GetRateResponseCallback responseCallback = new GetRateResponseCallback(onResultReceived);
		JsonObjectRequest getRateRequest = new JsonObjectRequest(getRateUrl(boxId, vodId), null, responseCallback,
		        responseCallback);
		_requestQueue.add(getRateRequest);
		
	}
	
	private class GetRateResponseCallback implements Response.Listener<JSONObject>, Response.ErrorListener
	{
		private OnResultReceived _onResultReceived;
		
		public GetRateResponseCallback(OnResultReceived onResultReceived)
		{
			_onResultReceived = onResultReceived;
		}
		
		@Override
		public void onErrorResponse(VolleyError error)
		{
			int statusCode = error.networkResponse != null ? error.networkResponse.statusCode
			        : ResultCode.GENERAL_FAILURE;
			Log.e(TAG, "Error retrieving VOD rate with code " + statusCode + ": " + error);
			_onResultReceived.onReceiveResult(new FeatureError(FeatureVOD.this, statusCode, error), null);
		}
		
		@Override
		public void onResponse(JSONObject response)
		{
			try
			{
				FeatureError result = FeatureError.OK(FeatureVOD.this);
				if (!response.isNull("rating"))
				{
					
					result.setCode(response.getInt("rating"));
				}
				else
				{
					result.setCode(0);
				}
				_onResultReceived.onReceiveResult(result, null);
			}
			catch (JSONException e)
			{
				Log.e(TAG, e.getMessage(), e);
				_onResultReceived
				        .onReceiveResult(new FeatureError(FeatureVOD.this, ResultCode.PROTOCOL_ERROR, e), null);
			}
		}
	}
	
	private class RateResponseCallback implements Response.Listener<String>, Response.ErrorListener
	{
		private OnResultReceived _onResultReceived;
		
		public RateResponseCallback(OnResultReceived onResultReceived)
		{
			_onResultReceived = onResultReceived;
		}
		
		@Override
		public void onErrorResponse(VolleyError error)
		{
			int statusCode = error.networkResponse != null ? error.networkResponse.statusCode
			        : ResultCode.GENERAL_FAILURE;
			Log.e(TAG, "Error retrieving VOD details with code " + statusCode + ": " + error);
			_onResultReceived.onReceiveResult(new FeatureError(FeatureVOD.this, statusCode, error), null);
		}
		
		@Override
		public void onResponse(String response)
		{
			_onResultReceived.onReceiveResult(FeatureError.OK, null);
		}
	}
	
	/**
	 * Loads recommended VOD items corresponding to specific VOD item
	 *
	 * @param vodItem
	 * @param onResultReceived
	 */
	public void loadRecommendedVodItems(final VODItem vodItem, final List<VODItem> vodItems,
	        final OnResultReceived onResultReceived)
	{
		long vodId = convertVodIdToLong(vodItem.getId());
		RecomendationResponseCallback responseCallback = new RecomendationResponseCallback(onResultReceived, vodItems,
		        vodItem);
		String strQuery = getRecommendUrl(vodId);
		JsonArrayRequest getRateRequest = new JsonArrayRequest(strQuery, responseCallback, responseCallback);
		_requestQueue.add(getRateRequest);
	}
	
	private class RecomendationResponseCallback implements Response.Listener<JSONArray>, Response.ErrorListener
	{
		private OnResultReceived _onResultReceived;
		private List<VODItem> _vodItems;
		private VODItem _vodItem;
		
		public RecomendationResponseCallback(OnResultReceived onResultReceived, List<VODItem> vodItems, VODItem vodItem)
		{
			_onResultReceived = onResultReceived;
			_vodItems = vodItems;
			_vodItem = vodItem;
		}
		
		@Override
		public void onErrorResponse(VolleyError error)
		{
			int statusCode = error.networkResponse != null ? error.networkResponse.statusCode
			        : ResultCode.GENERAL_FAILURE;
			Log.e(TAG, "Error retrieving VOD details with code " + statusCode + ": " + error);
			loadNoRecommendedItems();
			// _onResultReceived.onReceiveResult(new
			// FeatureError(FeatureVOD.this, statusCode, error));
		}
		
		@Override
		public void onResponse(JSONArray data)
		{
			try
			{
				if (data.length() > 0)
				{
					for (int idIdx = 0; idIdx < data.length(); idIdx++)
					{
						long vodId = data.getLong(idIdx);
						String strVodID = String.format("ivd_%s", vodId);
						VODItem vodItem = getVodItemById(strVodID);
						_vodItems.add(vodItem);
					}
				}
				else
				{
					loadNoRecommendedItems();
				}
				_onResultReceived.onReceiveResult(FeatureError.OK, null);
			}
			catch (JSONException e)
			{
				Log.e(TAG, e.getMessage(), e);
				// _onResultReceived.onReceiveResult(new
				// FeatureError(FeatureVOD.this, ResultCode.PROTOCOL_ERROR, e));
				loadNoRecommendedItems();
			}
		}
		
		private void loadNoRecommendedItems()
		{
			loadVodItems(_vodItem.getParent(), _vodItems, new OnResultReceived()
			{
				@Override
				public void onReceiveResult(FeatureError error, Object object)
				{
					if (!error.isError())
					{
						_vodItems.remove(_vodItem);
						_onResultReceived.onReceiveResult(FeatureError.OK, object);
					}
					else
					{
						Log.e(TAG, error.getMessage(), error);
						_onResultReceived.onReceiveResult(error, object);
					}
				}
			});
		}
	}
	
	/**
	 * Loads VOD item details
	 *
	 * @param vodItem
	 *            VodItem reference to be filled up with additional detail
	 *            attributes
	 * @param onResultReceived
	 *            response callback reference
	 */
	public void loadVodItemDetails(VODItem vodItem, OnResultReceived onResultReceived)
	{
		Log.i(TAG, "item id:" + vodItem.toString());
		String vodDetailsUrl = getVodDetailsUrl(vodItem.getParent().getId(), vodItem.getId());
		Log.i(TAG, "Retrieving VOD details for item " + vodItem.toString());
		Log.i(TAG, "vodDetailsUrl:" + vodDetailsUrl);
		VodDetailsResponseCallback responseCallback = new VodDetailsResponseCallback(vodItem, onResultReceived);
		JsonObjectRequest vodDetailsRequest = new JsonObjectRequest(vodDetailsUrl, null, responseCallback,
		        responseCallback);
		_requestQueue.add(vodDetailsRequest);
	}
	
	/**
	 * Loads all VOD items which direct or indirect parent is in the specified
	 * VOD group list
	 *
	 * @param vodGroups
	 *            list of VOD groups
	 * @param vodGroupItems
	 *            out map of VODGroup to VODItem list
	 * @param maxItems
	 *            maximum number of VOD items per VOD Group, 0 - unlimited
	 * @param onResultReceived
	 */
	public void loadVodItemsIndirect(List<VODGroup> vodGroups, Map<VODGroup, List<VODItem>> vodGroupItems,
	        int maxItems, OnResultReceived onResultReceived)
	{
		if (vodGroupItems.keySet().size() == 0)
		{
			for (VODGroup vodGroup : vodGroups)
				vodGroupItems.put(vodGroup, new ArrayList<VODItem>());
		}
		
		List<VODGroup> vodSubGroups = new ArrayList<VODGroup>();
		for (VODGroup vodGroup : vodGroups)
		{
			loadVodGroups(vodGroup.getId(), vodSubGroups, null);
			List<VODItem> vodItems = null;
			for (VODGroup parentGroup : vodGroupItems.keySet())
			{
				if (isParentToGroup(parentGroup, vodGroup))
				{
					vodItems = vodGroupItems.get(parentGroup);
					break;
				}
			}
			assert (vodItems != null);
			List<VODItem> items = _vodData.getVodItems(vodGroup);
			if (items != null && items.size() > 0)
			{
				for (VODItem item : items)
				{
					if (vodItems.indexOf(item) < 0)
					{
						vodItems.add(item);
					}
				}
			}
			// recurse into the sub-groups
			if (vodSubGroups.size() > 0)
				loadVodItemsIndirect(vodSubGroups, vodGroupItems, maxItems, null);
		}
		
		// reduce the number of items per group up to maxItems
		if (maxItems > 0)
		{
			for (List<VODItem> vodItems : vodGroupItems.values())
			{
				while (vodItems.size() > maxItems)
				{
					VODItem lastItem = null;
					List<VODItem> removeItems = new ArrayList<VODItem>();
					for (VODItem item : vodItems)
					{
						if (lastItem != null && lastItem.getParent() == item.getParent())
						{
							removeItems.add(item);
						}
						lastItem = item;
						if (vodItems.size() - removeItems.size() == maxItems)
							break;
					}
					if (removeItems.size() > 0)
					{
						for (VODItem item : removeItems)
							vodItems.remove(item);
					}
					else
					{
						while (vodItems.size() > maxItems)
							vodItems.remove(vodItems.size() - 1);
					}
				}
			}
		}
		
		if (onResultReceived != null)
			onResultReceived.onReceiveResult(FeatureError.OK, null);
	}
	
	/**
	 * Loads all VOD items directly owned by the specified VodGroup
	 *
	 * @param vodGroup
	 *            the parent VOD group of the requested items
	 * @param vodItems
	 *            out VODItem list
	 * @param onResultReceived
	 */
	public void loadVodItems(VODGroup vodGroup, List<VODItem> vodItems, OnResultReceived onResultReceived)
	{
		List<VODItem> items = _vodData.getVodItems(vodGroup);
		if (items != null)
			vodItems.addAll(items);
		
		if (onResultReceived != null)
			onResultReceived.onReceiveResult(FeatureError.OK, null);
	}
	
	/**
	 * Provide full text search in VOD items
	 *
	 * @param text
	 *            the text to be searched in the VOD items
	 * @param vodItems
	 *            the result VOD items list
	 * @param onResultReceived
	 */
	public void search(final String text, final List<VODItem> vodItems, final OnResultReceived onResultReceived)
	{
		Log.i(TAG, ".search: text = " + text);
		if (text == null)
			throw new NullPointerException("null text argument is not allowed");
		
		if (text.equals(_lastSearchTerm))
		{
			// returns the last search results immediately
			vodItems.clear();
			vodItems.addAll(_lastSearchResults);
			onResultReceived.onReceiveResult(FeatureError.OK(FeatureVOD.this), null);
			return;
		}
		
		String vodSearchUrl = getVodSearchUrl(text);
		
		Response.Listener<JSONObject> responseCallback = new Response.Listener<JSONObject>()
		{
			@Override
			public void onResponse(JSONObject response)
			{
				try
				{
					vodItems.clear();
					JSONArray arr = response.getJSONArray("meta");
					// get the index of vod id in data sub-arrays
					int idIdx = 0;
					for (int i = 0; i < arr.length(); i++)
					{
						if ("id".equalsIgnoreCase(arr.getString(i)))
						{
							idIdx = i;
							break;
						}
					}
					arr = response.getJSONArray("data");
					for (int i = 0; i < arr.length(); i++)
					{
						String vodId = arr.getJSONArray(i).getString(idIdx);
						VODItem vodItem = getVodItemById(vodId);
						if (vodItem == null)
						{
							Log.e(TAG, "Unknown VOD item received by the server " + vodItem);
						}
						else
						{
							vodItems.add(vodItem);
						}
					}
					onResultReceived.onReceiveResult(FeatureError.OK(FeatureVOD.this), null);
					
					_lastSearchTerm = text;
					_lastSearchResults.clear();
					_lastSearchResults.addAll(vodItems);
				}
				catch (JSONException e)
				{
					Log.e(TAG, e.getMessage(), e);
					onResultReceived.onReceiveResult(new FeatureError(FeatureVOD.this, e), null);
				}
			}
		};
		
		Response.ErrorListener errorCallback = new Response.ErrorListener()
		{
			@Override
			public void onErrorResponse(VolleyError err)
			{
				onResultReceived.onReceiveResult(new FeatureError(FeatureVOD.this, err), null);
			}
		};
		
		JsonObjectRequest vodSearchRequest = new JsonObjectRequest(vodSearchUrl, null, responseCallback, errorCallback);
		
		Log.i(TAG, ".search: " + vodSearchUrl);
		_requestQueue.add(vodSearchRequest);
		
		Bundle bundle = new Bundle();
		bundle.putString(OnVodSearchExtra.TEXT.name(), textToLatin(text));
		getEventMessenger().trigger(ON_VOD_SEARCH, bundle);
	}
	
	private String textToLatin(String text)
	{
		StringBuilder latin = new StringBuilder();
		for (int i = 0; i < text.length(); i++)
		{
			int idx = CYRILLIC_CHARS.indexOf(text.substring(i, i + 1));
			if (idx > -1)
				latin.append(LATIN_CHARS.charAt(idx));
			else
				latin.append(text.substring(i, i + 1));
		}
		return latin.toString();
	}
	
	// Command handlers
	
	/**
	 * Returns a JSON Array for the VOD groups
	 * Params:
	 * VOD_GROUP_ID - the parent of the VOD groups
	 */
	private class OnCommandGetVODGroups implements CommandHandler
	{
		@Override
		public void execute(Bundle params, final OnResultReceived onResultReceived)
		{
			String vodGroupId = params.getString(CommandGetVodGroupsExtras.VOD_GROUP_ID.name());
			final List<VODGroup> vodGroups = new ArrayList<VODGroup>();
			loadVodGroups(vodGroupId, vodGroups, new OnResultReceived()
			{
				
				@Override
				public void onReceiveResult(FeatureError error, Object object)
				{
					if (error.isError())
					{
						onResultReceived.onReceiveResult(error, null);
					}
					else
					{
						@SuppressWarnings("unchecked")
						JSONArray jsonVODgroups = new JSONArray();
						try
						{
							for (VODGroup vodGroup : vodGroups)
							{
								JSONObject jsonVODgroup = new JSONObject();
								jsonVODgroup.put("id", vodGroup.getId());
								jsonVODgroup.put("title", vodGroup.getTitle());
								jsonVODgroup.put("parent", vodGroup.getParent());
								jsonVODgroups.put(jsonVODgroup);
							}
							onResultReceived.onReceiveResult(FeatureError.OK(FeatureVOD.this), jsonVODgroups);
						}
						catch (JSONException e)
						{
							onResultReceived.onReceiveResult(new FeatureError(FeatureVOD.this, e), null);
						}
					}
				}
			});
		}
		
		@Override
		public String getId()
		{
			return Command.GET_VODGROUPS.name();
		}
	}
	
	/**
	 * Returns a JSON Array for the VOD items
	 */
	private class OnCommandGetVODItems implements CommandHandler
	{
		@Override
		public void execute(Bundle params, final OnResultReceived onResultReceived)
		{
			String vodGroupId = params.getString(CommandGetVodItemsExtras.VOD_GROUP_ID.name());
			final List<VODItem> vodItems = new ArrayList<VODItem>();
			VODGroup vodGroup = getVodGroupById(vodGroupId); // parent group
			
			// vodItems will be filled when we call loadVodItems()
			loadVodItems(vodGroup, vodItems, new OnResultReceived()
			{
				
				@Override
				public void onReceiveResult(FeatureError error, Object object)
				{
					if (error.isError())
					{
						onResultReceived.onReceiveResult(error, null);
					}
					else
					{
						@SuppressWarnings("unchecked")
						JSONArray jsonVODitems = new JSONArray();
						try
						{
							
							for (VODItem vodItem : vodItems)
							{
								JSONObject jsonVODitem = new JSONObject();
								jsonVODitem.put("id", vodItem.getId());
								
								jsonVODitem.put("title", vodItem.getTitle());
								
								jsonVODitem.put("poster", vodItem.getPoster());
								
								jsonVODitems.put(jsonVODitem);
							}
							onResultReceived.onReceiveResult(FeatureError.OK(FeatureVOD.this), jsonVODitems);
						}
						catch (JSONException e)
						{
							onResultReceived.onReceiveResult(new FeatureError(FeatureVOD.this, e), null);
						}
					}
				}
			});
		}
		
		@Override
		public String getId()
		{
			return Command.GET_VODITEMS.name();
		}
	}
	
	private class OnCommandGetVODDetails implements CommandHandler
	{
		@Override
		public void execute(Bundle params, final OnResultReceived onResultReceived)
		{
			final String vodItemId = params.getString(CommandGetVodDetailsExtras.VOD_ITEM_ID.name());
			final VODItem vodItem = getVodItemById(vodItemId);
			loadVodItemDetails(vodItem, new OnResultReceived()
			{
				@Override
				public void onReceiveResult(FeatureError error, Object object)
				{
					if (error.isError())
					{
						onResultReceived.onReceiveResult(error, null);
					}
					else
					{
						JSONObject jsonVODdetails = new JSONObject();
						try
						{
							jsonVODdetails.put("id", vodItem.getId());
							jsonVODdetails.put("title", vodItem.getTitle());
							jsonVODdetails.put("rating_imdb", vodItem.getAttribute(VodAttribute.RATING));
							jsonVODdetails.put("director", vodItem.getAttribute(VodAttribute.DIRECTOR));
							jsonVODdetails.put("description", vodItem.getAttribute(VodAttribute.DESCRIPTION));
							jsonVODdetails.put("actors", vodItem.getAttribute(VodAttribute.ACTORS));
							jsonVODdetails.put("youtube_trailer_code",
							        parseyoutubeUrlFull(vodItem.getAttribute(VodAttribute.YOUTUBE_TRAILER_URL)));
							jsonVODdetails.put("duration", vodItem.getAttribute(VodAttribute.DURATION));
							jsonVODdetails.put("poster_large", vodItem.getPoster());
							onResultReceived.onReceiveResult(FeatureError.OK(FeatureVOD.this), jsonVODdetails);
						}
						catch (JSONException e)
						{
							onResultReceived.onReceiveResult(new FeatureError(FeatureVOD.this, e), null);
						}
					}
				}
			});
		}
		
		String parseyoutubeUrlFull(String youtubeUrlFull)
		{
			int i = 0;
			boolean t = false;
			while (youtubeUrlFull.charAt(i) != '=' && i < youtubeUrlFull.length())
			{
				if (youtubeUrlFull.charAt(i) == '\\') // erase the character '\'
													  // since it is wrong
				                                      // and must not increment
				                                      // var i since the string
													  // shrinks
				{
					youtubeUrlFull = youtubeUrlFull.substring(0, i)
					        + youtubeUrlFull.substring(i + 1, youtubeUrlFull.length());
					continue;
				}
				i++;
			}
			if (i != youtubeUrlFull.length())
				t = true;
			
			i++; // position after '='
			if (!youtubeUrlFull.substring(0, i).equals("http://www.youtube.com/watch?v=") || t == false)
				return "not a youtube link";
			
			return youtubeUrlFull.substring(i, youtubeUrlFull.length());
		}
		
		@Override
		public String getId()
		{
			return Command.GET_VODDETAILS.name();
		}
	}
	
}

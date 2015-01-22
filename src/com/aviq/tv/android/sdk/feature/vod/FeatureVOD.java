/**
 * Copyright (c) 2007-2015, AVIQ Bulgaria Ltd
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

import android.os.Bundle;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Scheduler;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.FeatureScheduler;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.vod.VODItem.PosterSize;
import com.aviq.tv.android.sdk.feature.vod.bulsat_v1.Vod;

/**
 * Feature providing VOD data
 */
@Author("alek")
public abstract class FeatureVOD extends FeatureScheduler
{
	public static final String TAG = FeatureVOD.class.getSimpleName();
	public static final int ON_VOD_UPDATED = EventMessenger.ID("ON_VOD_UPDATED");

	public static enum Param
	{
		/**
		 * The main url to the VOD server
		 */
		VOD_SERVER("http://bulsat.aviq.bg"),

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
		VOD_ITEMS_URL("${SERVER}/v${VERSION}/vod/${PROVIDER}/*?attr=poster_small,poster_medium,poster_large"),

		/**
		 * VOD image url format
		 */
		VOD_IMAGE_URL("${SERVER}/static/${PROVIDER}/vod/${IMAGE}");

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

	public FeatureVOD() throws FeatureNotFoundException
	{
		require(FeatureName.Scheduler.INTERNET);
		require(FeatureName.State.NETWORK_WIZARD);
	}

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");

		_vodProvider = getPrefs().getString(Param.VOD_PROVIDER);
		_vodVersion = getPrefs().getInt(Param.VOD_VERSION);
		_vodServer = getPrefs().getString(Param.VOD_SERVER);
		_requestQueue = Environment.getInstance().getRequestQueue();
		onSchedule(onFeatureInitialized);
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

		for (int j = 0; j < meta.length; j++)
		{
			String key = meta[j];

			if ("poster_small".equals(key))
				metaData.metaVodItemPosterSmall = j;
			else if ("poster_medium".equals(key))
				metaData.metaVodItemPosterMedium = j;
			else if ("poster_large".equals(key))
				metaData.metaVodItemPosterLarge = j;
		}
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
					values[j] = jsonArr.get(j).toString();
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
				if (jsonArr.get(j) != null)
					values[j] = jsonArr.get(j).toString();
			}

			String vodItemId = values[metaData.metaVodGroupId];
			String parentId = values[metaData.metaVodGroupParent];
			if (parentId != null)
			{
				VODGroup parent = vodData.getVodGroupById(parentId);
				String vodItemTitle = values[metaData.metaVodGroupTitle];
				VODItem vodItem = createVodItem(vodItemId, vodItemTitle, parent);
				vodItem.setPoster(PosterSize.SMALL, values[metaData.metaVodItemPosterSmall]);
				vodItem.setPoster(PosterSize.MEDIUM, values[metaData.metaVodItemPosterMedium]);
				vodItem.setPoster(PosterSize.LARGE, values[metaData.metaVodItemPosterLarge]);

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
	 * @param poster
	 *            the poster image of this VOD item
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
			onResultReceived.onReceiveResult(FeatureError.OK);
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
	public void loadVodItems(List<VODGroup> vodGroups, Map<VODGroup, List<VODItem>> vodGroupItems, int maxItems,
	        OnResultReceived onResultReceived)
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
			if (maxItems == 0 || vodItems.size() < maxItems)
			{
				List<VODItem> items = _vodData.getVodItems(vodGroup);
				if (items != null)
					for (VODItem item : items)
					{
						vodItems.add(item);
						Log.d(TAG, "Add " + item.getTitle() + " in " + vodGroup.getTitle());
						if (maxItems > 0 && vodItems.size() == maxItems)
							break;
					}
			}
			// recurse into the sub-groups
			if (vodSubGroups.size() > 0)
				loadVodItems(vodSubGroups, vodGroupItems, maxItems, null);
		}
		if (onResultReceived != null)
			onResultReceived.onReceiveResult(FeatureError.OK);
	}

	/**
	 * Provide full text search in VOD items
	 *
	 * @param text
	 *            the text to be searched in the VOD items
	 * @param vodItems
	 *            the result VOD items list
	 * @param onResultReceived
	 *            FIXME: the current implementation is not doing real search but
	 *            returns some first 10 vod items
	 */
	public void search(String text, List<VODItem> vodItems, OnResultReceived onResultReceived)
	{
		final List<VODGroup> vodGroups = new ArrayList<VODGroup>();
		loadVodGroups(null, vodGroups, new OnResultReceived()
		{
			@Override
			public void onReceiveResult(FeatureError error)
			{
				if (!error.isError())
				{
					Map<VODGroup, List<VODItem>> vodGroupItems = new HashMap<VODGroup, List<VODItem>>();
					loadVodItems(vodGroups, vodGroupItems, 10, new OnResultReceived()
					{
						@Override
						public void onReceiveResult(FeatureError error)
						{
							if (!error.isError())
							{
							}
						}
					});
				}
			}
		});
	}

	@Deprecated
	public abstract <T> T getVodData(boolean removeEmptyElements);

	@Deprecated
	public abstract void loadVod(String id, OnVodLoaded onVodLoadedListener);

	@Deprecated
	public abstract void search(String term, OnVodSearchResult onVodSearchResult);

	@Deprecated
	public static interface OnVodLoaded
	{
		public void onVodLoaded(Vod vod);

		public void onVodError(Exception error);
	}

	@Deprecated
	public static interface OnVodSearchResult<E>
	{
		public void onVodSearchResult(List<E> resultList);
	}
}

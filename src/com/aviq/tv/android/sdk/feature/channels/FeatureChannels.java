/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureChannels.java
 * Author:      alek
 * Date:        28 Dec 2013
 * Description: Component feature managing favorite channels
 */

package com.aviq.tv.android.sdk.feature.channels;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.epg.EpgData;
import com.aviq.tv.android.sdk.feature.epg.FeatureEPG;

/**
 * Component feature managing favorite channels
 */
public class FeatureChannels extends FeatureComponent
{
	public static final String TAG = FeatureChannels.class.getSimpleName();

	public enum Param
	{
		/**
		 * List of channel identifier keys formatted as
		 * <channel_id>,<channel_id>,...
		 */
		CHANNELS
	}

	private Prefs _userPrefs;
	private EpgData _epgData;

	// List of favorite channels
	private List<Channel> _channels;

	public FeatureChannels()
	{
		_dependencies.Components.add(FeatureName.Component.EPG);
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		try
		{
			_userPrefs = Environment.getInstance().getUserPrefs();
			FeatureEPG featureEPG = (FeatureEPG) Environment.getInstance().getFeatureComponent(
			        FeatureName.Component.EPG);
			_epgData = featureEPG.getEpgData();
			_channels = loadFavoriteChannels(_epgData);
			onFeatureInitialized.onInitialized(this, ResultCode.OK);
		}
		catch (FeatureNotFoundException e)
		{
			Log.e(TAG, e.getMessage(), e);
			onFeatureInitialized.onInitialized(this, ResultCode.GENERAL_FAILURE);
		}
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.CHANNELS;
	}

	/**
	 * Add/remove channel to/from favorites list
	 *
	 * @param channel
	 * @param isFavorite
	 */
	public void setChannelFavorite(Channel channel, boolean isFavorite)
	{
		boolean isModified = false;
		if (isFavorite)
		{
			if (!isChannelFavorite(channel))
			{
				_channels.add(channel);
				isModified = true;
				Log.i(TAG, "Channel " + channel.getChannelId() + " added to favorites");
			}
		}
		else
		{
			if (_channels.remove(channel))
			{
				isModified = true;
				Log.i(TAG, "Channel " + channel.getChannelId() + " removed from favorites");
			}
		}
		if (isModified)
			saveFavoriteChannels(_epgData, _channels);
	}

	/**
	 * Verify if channel belongs to favorites list
	 */
	public boolean isChannelFavorite(Channel channel)
	{
		return _channels.indexOf(channel) >= 0;
	}

	/**
	 * @return list of user favorite channels
	 */
	public List<Channel> getFavoriteChannels()
	{
		return _channels;
	}

	private List<Channel> loadFavoriteChannels(EpgData epgData)
	{
		List<Channel> channels = new ArrayList<Channel>();
		boolean isEmpty = !_userPrefs.has(Param.CHANNELS) || _userPrefs.getString(Param.CHANNELS).length() == 0;
		if (isEmpty)
		{
			// Add all channels as favorites
			channels.addAll(epgData.getChannels());
		}
		else
		{
			String buffer = _userPrefs.getString(Param.CHANNELS);
			String[] channelIds = buffer.split(",");
			for (String channelId : channelIds)
			{
				channels.add(epgData.getChannel(channelId));
			}
		}
		Log.i(TAG, "Loaded " + channels.size() + " favorite channels");
		return channels;
	}

	private void saveFavoriteChannels(EpgData epgData, List<Channel> channels)
	{
		StringBuffer buffer = new StringBuffer();
		for (Channel channel : channels)
		{
			if (buffer.length() > 0)
				buffer.append(',');
			buffer.append(channel.getChannelId());
		}
		_userPrefs.put(Param.CHANNELS, buffer.toString());
		Log.i(TAG, "Updated favorites list: " + buffer);
	}
}

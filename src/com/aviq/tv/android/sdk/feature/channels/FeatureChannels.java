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
import java.util.Collections;
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
	private boolean _isModified = false;

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
			Log.i(TAG, "_epgData -> " + _epgData);
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
		if (isFavorite)
		{
			if (!isChannelFavorite(channel))
			{
				_channels.add(channel);
				_isModified = true;
				Log.i(TAG, "Channel " + channel.getChannelId() + " added to favorites");
			}
		}
		else
		{
			if (_channels.remove(channel))
			{
				_isModified = true;
				Log.i(TAG, "Channel " + channel.getChannelId() + " removed from favorites");
			}
		}
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
		if (isEverChanged())
			return _channels;
		return _epgData.getChannels();
	}

	/**
	 * Swap channel positions
	 *
	 */
	public void swapChannelPositions(int position1, int position2)
	{
		if (position1 != position2)
		{
			Collections.swap(_channels, position1, position2);
			_isModified = true;
		}
	}

	/**
	 * Save favorite channels to persistent storage
	 */
	public void save()
	{
		saveFavoriteChannels(_epgData, _channels);
		_isModified = false;
	}

	/**
	 * Returns true if the favorite channel list has been modified
	 */
	public boolean isModified()
	{
		return _isModified;
	}

	/**
	 * Returns true if the favorite channel list has been ever changed
	 */
	public boolean isEverChanged()
	{
		return _channels.size() > 0;
	}

	private List<Channel> loadFavoriteChannels(EpgData epgData)
	{
		List<Channel> channels = new ArrayList<Channel>();
		boolean isEmpty = !_userPrefs.has(Param.CHANNELS) || _userPrefs.getString(Param.CHANNELS).length() == 0;
		if (!isEmpty)
		{
			String buffer = _userPrefs.getString(Param.CHANNELS);
			String[] channelIds = buffer.split(",");
			for (String channelId : channelIds)
			{
				Log.i(TAG, "epgData -> " + epgData + ", channels -> " + channels);
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

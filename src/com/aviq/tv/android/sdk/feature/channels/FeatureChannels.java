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
import com.aviq.tv.android.sdk.feature.player.FeaturePlayer;

/**
 * Component feature managing favorite channels
 */
public class FeatureChannels extends FeatureComponent
{
	public static final String TAG = FeatureChannels.class.getSimpleName();

	public enum Param
	{
		/**
		 * Automatically start playing last played channel
		 */
		AUTOPLAY(true);

		Param(boolean value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.CHANNELS).put(name(), value);
		}
	}

	public enum UserParam
	{
		/**
		 * List of channel identifier keys formatted as
		 * <channel_id>,<channel_id>,...
		 */
		CHANNELS,

		/**
		 * Last played channel id
		 */
		LAST_CHANNEL_ID,

		/**
		 * Previous played channel id
		 */
		PREV_CHANNEL_ID,

		/**
		 * Active channels are from the favorites list
		 */
		USE_FAVORITES,

		/**
		 * LAST EPG Synchronized channels
		 */
		SYNCED_CHANNELS
	}

	private Prefs _userPrefs;
	private FeatureEPG _featureEPG;
	private FeaturePlayer _featurePlayer;
	private boolean _isModified = false;

	// List of favorite channels
	private List<Channel> _channels;

	public FeatureChannels()
	{
		_dependencies.Schedulers.add(FeatureName.Scheduler.EPG);
		_dependencies.Components.add(FeatureName.Component.PLAYER);
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		try
		{
			_userPrefs = Environment.getInstance().getUserPrefs();
			_featureEPG = (FeatureEPG) Environment.getInstance().getFeatureScheduler(FeatureName.Scheduler.EPG);
			_featurePlayer = (FeaturePlayer) Environment.getInstance()
			        .getFeatureComponent(FeatureName.Component.PLAYER);
			_channels = loadFavoriteChannels();

			if (getPrefs().getBool(Param.AUTOPLAY))
			{
				playLast();
			}
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
	 * Verify if channel belongs to favorites list
	 */
	public Channel getFavoriteChannel(String channelId)
	{
		int index = findChannelIndex(channelId);
		if (index > -1)
			return _channels.get(index);

		return null;
	}

	/**
	 * @return list of user favorite channels
	 */
	public List<Channel> getUserChannels()
	{
		return _channels;
	}

	/**
	 * @return list of channels depending on user settings
	 */
	public List<Channel> getActiveChannels()
	{
		if (isUseFavorites())
			return _channels;

		if (_featureEPG.getEpgData() == null)
		{
			Log.e(TAG, "No EPG data exists.");
			return new ArrayList<Channel>();
		}

		return _featureEPG.getEpgData().getChannels();
	}

	/**
	 * Swap channel positions
	 */
	public void swapChannelPositions(int position1, int position2)
	{
		if (position1 != position2 && position1 < _channels.size() && position2 < _channels.size())
		{
			Collections.swap(_channels, position1, position2);
			_isModified = true;
		}
	}

	/**
	 * Moves channel from position to top of list
	 */
	public void moveChannelToTop(int position)
	{
		if (position > 0)
		{
			Channel channel = _channels.get(position);
			_channels.remove(position);
			_channels.add(position, channel);
			_isModified = true;
		}
	}

	/**
	 * Save favorite channels to persistent storage
	 */
	public void save()
	{
		saveFavoriteChannels(_channels);
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
	 * Start playing channel at specified index in the channels favorite list
	 *
	 * @param index
	 */
	public void play(int index)
	{
		List<Channel> channels = getActiveChannels();
		if (channels.size() == 0)
			return;
		if (index < 0 || index >= channels.size())
			index = 0;
		Channel channel = channels.get(index);

		if (hasLastChannel())
		{
			String lastChannelId = _userPrefs.getString(UserParam.LAST_CHANNEL_ID);
			Log.i(TAG, ".play: last channel = " + lastChannelId + ", new channel = " + channel.getChannelId());
			if (_featurePlayer.isPlaying() && channel.getChannelId().equals(lastChannelId))
			{
				Log.d(TAG, ".play: already playing");
				return;
			}
			else
			{
				_userPrefs.put(UserParam.PREV_CHANNEL_ID, lastChannelId);
			}
		}
		setLastChannelId(channel.getChannelId());
		Log.d(TAG, ".play: start playing " + channel.getChannelId());
		int globalIndex = channel.getIndex();
		String streamUrl = _featureEPG.getChannelStreamUrl(globalIndex);
		_featurePlayer.play(streamUrl);
	}

	/**
	 * Start playing channel with specified channel id
	 */
	public void play(String channelId)
	{
		play(findChannelIndex(channelId));
	}

	/**
	 * Start playing last played channel
	 */
	public void playLast()
	{
		if (hasLastChannel())
		{
			play(_userPrefs.getString(UserParam.LAST_CHANNEL_ID));
		}
	}

	/**
	 * Returns true if last channel has been set
	 */
	public boolean hasLastChannel()
	{
		return _userPrefs.has(UserParam.LAST_CHANNEL_ID);
	}

	/**
	 * Returns last played channel id
	 */
	public String getLastChannelId()
	{
		if (hasLastChannel())
		{
			return _userPrefs.getString(UserParam.LAST_CHANNEL_ID);
		}
		return null;
	}

	/**
	 * Set last played channel id
	 */
	public void setLastChannelId(String lastChannelId)
	{
		_userPrefs.put(UserParam.LAST_CHANNEL_ID, lastChannelId);
	}

	/**
	 * Return previously played channel id
	 */
	public String getPreviousChannelId()
	{
		if (_userPrefs.has(UserParam.PREV_CHANNEL_ID))
		{
			return _userPrefs.getString(UserParam.PREV_CHANNEL_ID);
		}

		return null;
	}

	/**
	 * Returns last played channel index
	 */
	public int getLastChannelIndex()
	{
		if (hasLastChannel())
		{
			return findChannelIndex(_userPrefs.getString(UserParam.LAST_CHANNEL_ID));
		}
		return 0;
	}

	/**
	 * Tells to use the favorite channels or the original channels list by
	 * getFavoriteChannels
	 *
	 * @param isUseFavorites
	 */
	public void setUseFavorites(boolean isUseFavorites)
	{
		_userPrefs.put(UserParam.USE_FAVORITES, isUseFavorites);
	}

	/**
	 * @return true if the favorite channels or the original channels list will
	 *         be returned by getFavoriteChannels
	 */
	public boolean isUseFavorites()
	{
		return _userPrefs.has(UserParam.USE_FAVORITES) ? _userPrefs.getBool(UserParam.USE_FAVORITES) : false;
	}

	/**
	 * Remove current CHANNELS setup and restore defaults
	 */
	public void resetFavorites()
	{
		_userPrefs.remove(UserParam.CHANNELS);
		_channels = loadFavoriteChannels();
	}

	/**
	 * Return the list of previous EPG downloaded channels
	 */
	public String getSyncedChannels()
	{
		return _userPrefs.has(UserParam.SYNCED_CHANNELS) ? _userPrefs.getString(UserParam.SYNCED_CHANNELS) : null;
	}

	public void saveSyncedChannel()
	{
		if (_featureEPG.getEpgData() == null)
		{
			Log.e(TAG, "No EPG data exists.");
			return;
		}

		StringBuffer buffer = new StringBuffer();
		for (Channel channel : _featureEPG.getEpgData().getChannels())
		{
			if (buffer.length() > 0)
				buffer.append(',');
			buffer.append(channel.getChannelId());
		}
		_userPrefs.put(UserParam.SYNCED_CHANNELS, buffer.toString());
		Log.i(TAG, "Updated synched channels list: " + buffer);
	}

	private List<Channel> loadFavoriteChannels()
	{
		List<Channel> channels = new ArrayList<Channel>();

		EpgData epgData = _featureEPG.getEpgData();
		if (epgData == null)
		{
			Log.e(TAG, "No EPG data exists.");
			return channels;
		}

		boolean isEmpty = !_userPrefs.has(UserParam.CHANNELS) || _userPrefs.getString(UserParam.CHANNELS).length() == 0;
		if (isEmpty)
		{
			channels.addAll(epgData.getChannels());
			return channels;
		}
		else
		{
			String buffer = _userPrefs.getString(UserParam.CHANNELS);
			String[] channelIds = buffer.split(",");
			for (String channelId : channelIds)
			{
				Log.i(TAG, "epgData -> " + epgData + ", channels -> " + channels);
				Channel channel = epgData.getChannel(channelId);
				if (channel != null)
					channels.add(channel);
			}
		}
		Log.i(TAG, "Loaded " + channels.size() + " favorite channels");
		return channels;
	}

	private void saveFavoriteChannels(List<Channel> channels)
	{
		StringBuffer buffer = new StringBuffer();
		for (Channel channel : channels)
		{
			if (buffer.length() > 0)
				buffer.append(',');
			buffer.append(channel.getChannelId());
		}
		_userPrefs.put(UserParam.CHANNELS, buffer.toString());
		Log.i(TAG, "Updated favorites list: " + buffer);
	}

	private int findChannelIndex(String channelId)
	{
		if (channelId != null)
		{
			List<Channel> favoriteChannels = getActiveChannels();
			for (int i = 0; i < favoriteChannels.size(); i++)
			{
				Channel channel = favoriteChannels.get(i);
				if (channel.getChannelId().equals(channelId))
					return i;
			}
		}
		return -1;
	}
}

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

import android.os.Bundle;
import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventReceiver;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.epg.EpgData;
import com.aviq.tv.android.sdk.feature.epg.FeatureEPG;
import com.aviq.tv.android.sdk.feature.player.FeatureStreamer.OnStreamURLReceived;
import com.aviq.tv.android.sdk.feature.player.FeatureStreamerBulsat;
import com.aviq.tv.android.sdk.feature.player.FeatureTimeshift;

/**
 * Component feature managing favorite channels
 */
public class FeatureChannels extends FeatureComponent implements EventReceiver
{
	public static final String TAG = FeatureChannels.class.getSimpleName();

	public enum Param
	{
		/**
		 * Automatically start playing last played channel
		 */
		AUTOPLAY(true),

		/**
		 * Default channel depending on currently selected language
		 */
		DEFAULT_CHANNEL_DE, DEFAULT_CHANNEL_FR, DEFAULT_CHANNEL_EN;

		Param()
		{
		}

		Param(boolean value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.CHANNELS).put(name(), value);
		}

		Param(String value)
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
	private boolean _isModified = false;
	// reference to optional timeshift feature
	private FeatureTimeshift _featureTimeshift;

	// List of favorite channels
	private List<Channel> _channels;

	public FeatureChannels() throws FeatureNotFoundException
	{
		require(FeatureName.Scheduler.EPG);
		require(FeatureName.Component.LANGUAGE);
		require(FeatureName.Component.PLAYER);
		require(FeatureName.Component.STREAMER);
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		_userPrefs = Environment.getInstance().getUserPrefs();
		if (!_userPrefs.has(UserParam.LAST_CHANNEL_ID) && getPrefs().has(Param.DEFAULT_CHANNEL_EN))
		{
			String lastChannelId = getPrefs().getString(Param.DEFAULT_CHANNEL_EN);

			switch (_feature.Component.LANGUAGE.getLanguage())
			{
				case DE:
					lastChannelId = getPrefs().getString(Param.DEFAULT_CHANNEL_DE);
				break;
				case FR:
					lastChannelId = getPrefs().getString(Param.DEFAULT_CHANNEL_FR);
				break;
				default:
				break;
			}
			_userPrefs.put(UserParam.LAST_CHANNEL_ID, lastChannelId);
		}
		_feature.Scheduler.EPG.getEventMessenger().register(this, FeatureEPG.ON_EPG_UPDATED);
		_channels = loadFavoriteChannels();

		_featureTimeshift = (FeatureTimeshift) Environment.getInstance().getFeatureComponent(
		        FeatureName.Component.TIMESHIFT);
		_featureTimeshift.getEventMessenger().register(this, FeatureTimeshift.ON_SEEK);

		Environment.getInstance().getEventMessenger().register(this, Environment.ON_RESUME);
		Environment.getInstance().getEventMessenger().register(this, Environment.ON_PAUSE);

		if (getPrefs().getBool(Param.AUTOPLAY))
		{
			playLast();
		}
		onFeatureInitialized.onInitialized(this, ResultCode.OK);
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

		if (_feature.Scheduler.EPG.getEpgData() == null)
		{
			Log.e(TAG, "No EPG data exists.");
			return new ArrayList<Channel>();
		}

		return _feature.Scheduler.EPG.getEpgData().getChannels();
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
			_channels.add(0, channel);
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
	 * Start playing channel at specified index in the active channels list
	 *
	 * @param index
	 */
	public void play(int index)
	{
		play(index, 0);
	}

	/**
	 * Start playing channel at specified index and offset in time in the active
	 * channels list
	 *
	 * @param playTimeDelta
	 *            offset in seconds from real time, > 0 in the past, < 0 in the
	 *            future
	 * @param index
	 */
	public void play(int index, final long playTimeDelta)
	{
		Log.i(TAG, ".play: index = " + index + ", playTimeDelta = " + playTimeDelta);
		List<Channel> channels = getActiveChannels();
		if (channels.size() == 0)
			return;
		if (index < 0 || index >= channels.size())
			index = 0;
		final Channel channel = channels.get(index);
		final boolean[] isResetTimeshift = new boolean[]
		{ true };

		if (hasLastChannel())
		{
			String lastChannelId = _userPrefs.getString(UserParam.LAST_CHANNEL_ID);
			Log.i(TAG, ".play: last channel = " + lastChannelId + ", new channel = " + channel.getChannelId());

			if (!channel.getChannelId().equals(lastChannelId))
			{
				_userPrefs.put(UserParam.PREV_CHANNEL_ID, lastChannelId);
			}
			else
			{
				isResetTimeshift[0] = false;
			}
		}
		setLastChannelId(channel.getChannelId());
		int globalIndex = channel.getIndex();
		final String streamId = _feature.Scheduler.EPG.getChannelStreamId(globalIndex);
//		_feature.Component.STREAMER.getUrlByStreamId(streamId, playTimeDelta, new OnStreamURLReceived()
//		{
//			@Override
//			public void onStreamURL(final String streamUrl)
//			{
//				if (streamUrl == null)
//				{
//					Log.e(TAG, channel + " has no stream to play!");
//				}
//				else
//				{
//					// play stream
//					_feature.Component.PLAYER.play(streamUrl);
//					Log.d(TAG, ".play:onStreamURL: play `" + streamUrl + "'");
//
//					if (isResetTimeshift[0] && _featureTimeshift != null)
//					{
//						// reset timeshift to live
//						Log.d(TAG, ".play:onStreamURL: timeshift reset");
//						_featureTimeshift.reset();
//					}
//				}
//			}
//		});
		// FIXME: HORRIBLE HACK! USE IT ONLY FOR THE QUICK DEMO!
		FeatureStreamerBulsat featureStreamerBulsat = (FeatureStreamerBulsat) _feature.Component.STREAMER;
		featureStreamerBulsat.getUrlByStreamTestId(channel, playTimeDelta, new OnStreamURLReceived()
		{
			@Override
			public void onStreamURL(final String streamUrl)
			{
				if (streamUrl == null)
				{
					Log.e(TAG, channel + " has no stream to play!");
				}
				else
				{
					// play stream
					_feature.Component.PLAYER.play(streamUrl);
					Log.d(TAG, ".play:onStreamURL: play `" + streamUrl + "'");

					if (isResetTimeshift[0] && _featureTimeshift != null)
					{
						// reset timeshift to live
						Log.d(TAG, ".play:onStreamURL: timeshift reset");
						_featureTimeshift.reset();
					}
				}
			}
		});
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
		if (_feature.Scheduler.EPG.getEpgData() == null)
		{
			Log.e(TAG, "No EPG data exists.");
			return;
		}

		StringBuffer buffer = new StringBuffer();
		for (Channel channel : _feature.Scheduler.EPG.getEpgData().getChannels())
		{
			if (buffer.length() > 0)
				buffer.append(',');
			buffer.append(channel.getChannelId());
		}
		_userPrefs.put(UserParam.SYNCED_CHANNELS, buffer.toString());
		Log.i(TAG, "Updated synched channels list: " + buffer);
	}

	@Override
	public void onEvent(int msgId, Bundle bundle)
	{
		if (FeatureEPG.ON_EPG_UPDATED == msgId)
		{
			_channels = loadFavoriteChannels();
		}
		else if (Environment.ON_RESUME == msgId)
		{
			if (Environment.getInstance().isInitialized())
			{
				// restart playing on app resume
				playLast();
			}
		}
		else if (Environment.ON_PAUSE == msgId)
		{
			if (Environment.getInstance().isInitialized())
			{
				// stop playing on app pause
				_feature.Component.PLAYER.stop();
			}
		}
		else if (FeatureTimeshift.ON_SEEK == msgId)
		{
			long playTimeDelta = bundle.getLong(FeatureTimeshift.EXTRA_PLAY_TIME_DELTA, 0);
			play(getLastChannelIndex(), playTimeDelta);
		}
	}

	private List<Channel> loadFavoriteChannels()
	{
		List<Channel> channels = new ArrayList<Channel>();

		EpgData epgData = _feature.Scheduler.EPG.getEpgData();
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

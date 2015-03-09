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

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.EventReceiver;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;
import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.epg.FeatureEPG;
import com.aviq.tv.android.sdk.feature.epg.FeatureEPG.OnStreamURLReceived;
import com.aviq.tv.android.sdk.feature.epg.IEpgDataProvider;
import com.aviq.tv.android.sdk.feature.player.FeaturePlayer;
import com.aviq.tv.android.sdk.feature.player.FeatureTimeshift;
import com.aviq.tv.android.sdk.feature.system.FeatureDevice.IStatusFieldGetter;
import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Component feature managing favorite channels
 */
@Author("alek")
public class FeatureChannels extends FeatureComponent implements EventReceiver
{
	public static final String TAG = FeatureChannels.class.getSimpleName();

	public static final int ON_SWITCH_CHANNEL = EventMessenger.ID("ON_SWITCH_CHANNEL");
	public static final int ON_GET_STREAM_ERROR = EventMessenger.ID("ON_GET_STREAM_ERROR");
	public static final String EXTRA_GET_STREAM_ERROR_CODE = "GET_STREAM_ERROR_CODE";

	public enum OnSwitchChannelExtras
	{
		FROM_CHANNEL, TO_CHANNEL, SWITCH_DURATION
	}

	public static enum Param
	{
		/**
		 * Automatically start playing last played channel
		 */
		AUTOPLAY(true),

		/**
		 * Set all channels as favorites if there are none favorite channels
		 */
		DEFAULT_ALL_CHANNELS_FAVORITE(true),

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
	private String _channelId;
	private PlayEventsReceiver _playEventsReceiver = new PlayEventsReceiver();

	private class PlayEventsReceiver implements EventReceiver
	{
		private Channel _channel;

		@Override
		public void onEvent(int msgId, Bundle bundle)
		{
			Log.i(TAG, ".PLAYER:onEvent: " + EventMessenger.idName(msgId) + TextUtils.implodeBundle(bundle));

			if (msgId == FeaturePlayer.ON_PLAY_STARTED)
			{
				if (_channelId == null || !_channelId.equals(_channel.getChannelId()))
				{
					// switching to new channel
					Bundle switchBundle = new Bundle();
					if (_channelId != null)
						switchBundle.putString(OnSwitchChannelExtras.FROM_CHANNEL.name(), _channelId);
					switchBundle.putString(OnSwitchChannelExtras.TO_CHANNEL.name(), _channel.getChannelId());
					switchBundle.putLong(OnSwitchChannelExtras.SWITCH_DURATION.name(),
					        bundle.getLong(FeaturePlayer.Extras.TIME_ELAPSED.name()));
					getEventMessenger().trigger(ON_SWITCH_CHANNEL, switchBundle);
				}
				_channelId = _channel.getChannelId();
			}
			else if (msgId == FeaturePlayer.ON_PLAY_PAUSE)
			{
				if (_featureTimeshift != null)
				{
					if (_feature.Component.PLAYER.isPaused())
						_featureTimeshift.pause();
					else
						_featureTimeshift.resume();
				}
			}
			else if (msgId == FeaturePlayer.ON_PLAY_STOP)
			{
				if (_featureTimeshift != null)
					_featureTimeshift.seekLive();
			}
			else if (msgId == FeaturePlayer.ON_PLAY_ERROR || msgId == FeaturePlayer.ON_PLAY_STOP
			        || msgId == FeaturePlayer.ON_PLAY_TIMEOUT)
			{
				_feature.Component.PLAYER.getEventMessenger().unregister(this, EventMessenger.ON_ANY);
			}
		}
	}

	public FeatureChannels() throws FeatureNotFoundException
	{
		require(FeatureName.Scheduler.EPG);
		require(FeatureName.Component.LANGUAGE);
		require(FeatureName.Component.PLAYER);
		require(FeatureName.Component.DEVICE);
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		_userPrefs = Environment.getInstance().getUserPrefs();

		// FIXME: remove the logic bellow to project specific feature
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
		if (_featureTimeshift != null)
		{
			_featureTimeshift.getEventMessenger().register(this, FeatureTimeshift.ON_SEEK);
			int lastChannelIndex = getLastChannelIndex();
			if (lastChannelIndex < 0)
				// can't find channel index, default to 1st channel
				lastChannelIndex = 0;
			Channel lastChannel = getActiveChannels().get(lastChannelIndex);
			long bufferSize = _feature.Scheduler.EPG.getStreamBufferSize(lastChannel);
			_featureTimeshift.setTimeshiftDuration(bufferSize);
		}
		else
		{
			Log.i(TAG, "Timeshift support is disabled");
		}

		Environment.getInstance().getEventMessenger().register(this, Environment.ON_RESUME);
		Environment.getInstance().getEventMessenger().register(this, Environment.ON_PAUSE);

		if (getPrefs().getBool(Param.AUTOPLAY))
		{
			playLast();
		}

		_feature.Component.DEVICE.addStatusField("channel", new IStatusFieldGetter()
		{
			@Override
			public String getStatusField()
			{
				// FIXME: This value is not always currently playing channel
				return getLastChannelId();
			}
		});
		super.initialize(onFeatureInitialized);
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
			Log.e(TAG, "getActiveChannels: EPG not loaded!");
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
		play(index, System.currentTimeMillis() / 1000, 0);
	}

	/**
	 * Start playing channel at specified position and duration, set playTime to
	 * 0 for live stream
	 *
	 * @param playTime
	 *            timestamp to start channel from
	 * @param playDuration
	 *            stream duration in seconds
	 * @param index
	 */
	public void play(int index, long playTime, final long playDuration)
	{
		Log.i(TAG, ".play: index = " + index + ", playTime = " + playTime + ", playDuration = " + playDuration);
		List<Channel> channels = getActiveChannels();
		if (channels.size() == 0)
			return;
		if (index < 0 || index >= channels.size())
		{
			Log.w(TAG, ".play: channel index exceeds limits [0:" + channels.size() + ")");
			index = 0;
		}
		int lastChannelIndex = -1;
		final Channel channel = channels.get(index);
		if (hasLastChannel())
		{
			_userPrefs.put(UserParam.PREV_CHANNEL_ID, getLastChannelId());
			lastChannelIndex = getLastChannelIndex();
		}
		setLastChannelId(channel.getChannelId());

		// set timeshift buffer size
		if (_featureTimeshift != null)
		{
			Log.i(TAG, "set timeshift buffer size " + index + "/" + lastChannelIndex);
			// will not modify timeshift parameters unless the channel is
			// changed
			if (index != lastChannelIndex)
			{
				long bufferSize = _feature.Scheduler.EPG.getStreamBufferSize(channel);
				Log.i(TAG, "set timeshift buffer size of " + index + "/" + lastChannelIndex + " to " + bufferSize);
				if (bufferSize > 0)
				{
					// timeshift buffer is defined by stream provider
					_featureTimeshift.setTimeshiftDuration(bufferSize);
				}
				else
				{
					// start timeshift buffer recording
					_featureTimeshift.startTimeshift();
				}
			}
			playTime = _featureTimeshift.seekAt(playTime);
		}
		else
		{
			Log.d(TAG, "No timeshift support");
		}

		// start playing retrieved channel stream
		_feature.Scheduler.EPG.getStreamUrl(channel, playTime, playDuration, new OnStreamURLReceived()
		{
			@Override
			public void onStreamURL(FeatureError error, final String streamUrl)
			{
				if (error.isError())
				{
					Bundle bundle = new Bundle();
					bundle.putInt(EXTRA_GET_STREAM_ERROR_CODE, error.getCode());
					getEventMessenger().trigger(ON_GET_STREAM_ERROR, bundle);
				}
				else
				{
					if (streamUrl != null)
					{
						// play stream
						_feature.Component.PLAYER.play(streamUrl);

						// avoid register leaks
						_playEventsReceiver._channel = channel;
						_feature.Component.PLAYER.getEventMessenger().unregister(_playEventsReceiver,
						        EventMessenger.ON_ANY);
						_feature.Component.PLAYER.getEventMessenger().register(_playEventsReceiver,
						        EventMessenger.ON_ANY);
					}
					else
					{
						Log.e(TAG, channel + " has no stream to play!");
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
			Log.e(TAG, "saveSyncedChannel: EPG not loaded!");
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
	}

	private List<Channel> loadFavoriteChannels()
	{
		List<Channel> channels = new ArrayList<Channel>();

		IEpgDataProvider epgData = _feature.Scheduler.EPG.getEpgData();
		if (epgData == null)
		{
			Log.e(TAG, "loadFavoriteChannels: EPG not loaded!");
			return channels;
		}

		boolean isEmpty = !_userPrefs.has(UserParam.CHANNELS) || _userPrefs.getString(UserParam.CHANNELS).length() == 0;
		if (isEmpty)
		{
			if (getPrefs().getBool(Param.DEFAULT_ALL_CHANNELS_FAVORITE))
				channels.addAll(epgData.getChannels());
		}
		else
		{
			String buffer = _userPrefs.getString(UserParam.CHANNELS);
			String[] channelIds = buffer.split(",");
			for (String channelId : channelIds)
			{
				Channel channel = epgData.getChannel(channelId);
				if (channel != null)
					channels.add(channel);
			}

			if (channels.size() == 0 && isUseFavorites())
			{
				Log.w(TAG, "No favorite channels after update. Adding all channels as favorites");
				channels.addAll(epgData.getChannels());
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
			List<Channel> activeChannels = getActiveChannels();
			for (int i = 0; i < activeChannels.size(); i++)
			{
				Channel channel = activeChannels.get(i);
				if (channel.getChannelId().equals(channelId))
					return i;
			}
		}
		Log.w(TAG, ".findChannelIndex: Can't find channel index of `" + channelId + "'");
		return -1;
	}
}

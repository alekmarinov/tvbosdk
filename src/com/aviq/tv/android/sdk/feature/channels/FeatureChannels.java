/**
 * Copyright (c) 2007-2015, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureChannels.java
 * Author:      alek
 * Date:        07 May 2015
 * Description: Component feature managing TV channel lists
 */

package com.aviq.tv.android.sdk.feature.channels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.command.CommandHandler;
import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.epg.bulsat.ChannelBulsat;
//import com.aviq.tv.android.sdk.feature.epg.FeatureEPG.OnCommandGetPrograms;
import com.aviq.tv.android.sdk.feature.player.FeaturePlayer;
import com.aviq.tv.android.sdk.feature.player.FeaturePlayer.MediaType;
import com.aviq.tv.android.sdk.feature.player.FeatureTimeshift;
import com.aviq.tv.android.sdk.feature.system.FeatureDevice.IStatusFieldGetter;
import com.aviq.tv.android.sdk.utils.Calendars;

/**
 * Component feature managing TV channel lists
 */
@Author("alek")
public class FeatureChannels extends FeatureComponent implements EventReceiver
{
	public static final String TAG = FeatureChannels.class.getSimpleName();

	public static final int ON_SWITCH_CHANNEL = EventMessenger.ID("ON_SWITCH_CHANNEL");
	public static final int ON_GET_STREAM_ERROR = EventMessenger.ID("ON_GET_STREAM_ERROR");
	public static final String EXTRA_GET_STREAM_ERROR_CODE = "GET_STREAM_ERROR_CODE";

	public static enum Command
	{
		GET_CHANNELS, ADD_REMOVE_FAVORITES
	}

	public static enum CommandAddRemoveFavoriteExtras
	{
		CHANNEL_ID, IS_FAVORITE
	}

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
		 * Last synchronized channels, used to detect new channels coming from
		 * the EPG provider
		 * formatted as <channel_id>,<channel_id>,...
		 */
		SYNCED_CHANNELS
	}

	private Prefs _userPrefs;

	// True if the favorite list has been modified
	private boolean _isFavoritesModified = false;

	// Reference to optional timeshift feature
	private FeatureTimeshift _featureTimeshift;

	// List of favorite channels
	private List<Channel> _favoriteChannels = new ArrayList<Channel>();

	// The last played channel
	private Channel _lastPlayChannel;

	public FeatureChannels() throws FeatureNotFoundException
	{
		require(FeatureName.Component.EPG);
		require(FeatureName.Component.LANGUAGE);
		require(FeatureName.Component.PLAYER);
		require(FeatureName.Component.DEVICE);
		require(FeatureName.Component.COMMAND);
	}

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		_userPrefs = Environment.getInstance().getUserPrefs();

		List<Channel> channels = _feature.Component.EPG.getChannels();
		// set last channel to the first channel if not set
		if (channels.size() > 0 && !_userPrefs.has(UserParam.LAST_CHANNEL_ID))
		{
			_userPrefs.put(UserParam.LAST_CHANNEL_ID, channels.get(0).getChannelId());
		}

		loadFavoriteChannels();

		_featureTimeshift = (FeatureTimeshift) Environment.getInstance().getFeatureComponent(
		        FeatureName.Component.TIMESHIFT);

		if (_featureTimeshift != null)
		{
			Channel lastChannel = getLastChannel();
			if (lastChannel != null)
			{
				long bufferSize = _feature.Component.EPG.getStreamBufferSize(lastChannel);
				_featureTimeshift.setTimeshiftDuration(bufferSize);
			}
			else
			{
				Log.w(TAG, "No last channel while initiailizing FeatureChannels");
			}
		}
		else
		{
			Log.i(TAG, "Timeshift support is disabled");
		}

		Environment.getInstance().getEventMessenger().register(FeatureChannels.this, Environment.ON_RESUME);
		Environment.getInstance().getEventMessenger().register(FeatureChannels.this, Environment.ON_PAUSE);

		// register on player events
		_feature.Component.PLAYER.getEventMessenger().register(FeatureChannels.this, FeaturePlayer.ON_PLAY_ERROR);
		_feature.Component.PLAYER.getEventMessenger().register(FeatureChannels.this, FeaturePlayer.ON_PLAY_PAUSE);
		_feature.Component.PLAYER.getEventMessenger().register(FeatureChannels.this, FeaturePlayer.ON_PLAY_STOP);
		_feature.Component.PLAYER.getEventMessenger().register(FeatureChannels.this, FeaturePlayer.ON_PLAY_TIMEOUT);
		_feature.Component.PLAYER.getEventMessenger().register(FeatureChannels.this, FeaturePlayer.ON_PLAY_STARTED);

		if (getPrefs().getBool(Param.AUTOPLAY))
		{
			playLast();
		}

		_feature.Component.DEVICE.addStatusField("channel", new IStatusFieldGetter()
		{
			@Override
			public String getStatusField()
			{
				if (MediaType.TV.equals(_feature.Component.PLAYER.getMediaType())
				        && _feature.Component.PLAYER.isPlaying())
					return getLastChannelId();
				return null;
			}
		});

		// add GET_CHANNELS command
		_feature.Component.COMMAND.addCommandHandler(new OnCommandGetChannels());
		_feature.Component.COMMAND.addCommandHandler(new OnCommandAddRemoveFavorite());

		FeatureChannels.super.initialize(onFeatureInitialized);
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
				_favoriteChannels.add(channel);
				_isFavoritesModified = true;
				Log.i(TAG, "Channel " + channel.getChannelId() + " added to favorites");
			}
		}
		else
		{
			if (_favoriteChannels.remove(channel))
			{
				_isFavoritesModified = true;
				Log.i(TAG, "Channel " + channel.getChannelId() + " removed from favorites");
			}
		}
	}

	/**
	 * Verify if channel belongs to favorites list
	 */
	public boolean isChannelFavorite(Channel channel)
	{
		return _favoriteChannels.indexOf(channel) >= 0;
	}

	/**
	 * Verify if channel belongs to favorites list
	 */
	public Channel getFavoriteChannel(String channelId)
	{
		Channel channel = _feature.Component.EPG.getChannelById(channelId);
		if (channel != null && _favoriteChannels.indexOf(channel) >= 0)
		{
			return channel;
		}
		return null;
	}

	/**
	 * @return list of user favorite channels
	 */
	public List<Channel> getFavoriteChannels()
	{
		return _favoriteChannels;
	}

	/**
	 * @return list of channels depending on user settings
	 */
	public List<Channel> getActiveChannels()
	{
		if (isUseFavorites())
		{
			return _favoriteChannels;
		}
		else
		{
			return _feature.Component.EPG.getChannels();
		}
	}

	/**
	 * Swap channel positions
	 */
	public void swapChannelPositions(int position1, int position2)
	{
		if (position1 != position2 && position1 < _favoriteChannels.size() && position2 < _favoriteChannels.size())
		{
			Log.i(TAG, ".swapChannelPositions: " + position1 + " <-> " + position2);
			int delta = position1 < position2 ? 1 : -1;
			while (position1 != position2)
			{
				Collections.swap(_favoriteChannels, position1, position1 + delta);
				position1 += delta;
			}

			_isFavoritesModified = true;
		}
	}

	/**
	 * Moves channel from position to top of list
	 */
	public void moveChannelToTop(int position)
	{
		if (position > 0)
		{
			swapChannelPositions(position, 0);
		}
	}

	/**
	 * Save favorite channels to persistent storage
	 */
	public void save()
	{
		saveFavoriteChannels();
		_isFavoritesModified = false;
	}

	/**
	 * Returns true if the favorite channel list has been modified
	 */
	public boolean isModified()
	{
		return _isFavoritesModified;
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
	 * @param index
	 *            the channel index
	 * @param playTime
	 *            timestamp in seconds to start channel from
	 * @param playDuration
	 *            stream duration in seconds
	 */
	public void play(final int index, final long playTime, final long playDuration)
	{
		Log.i(TAG, ".play: index = " + index + ", playTime = " + Calendars.makeString((int) playTime)
		        + ", playDuration = " + playDuration);
		int nChannels = _feature.Component.EPG.getChannels().size();
		if (nChannels == 0)
		{
			Log.w(TAG, ".play: channels list is empty");
			return;
		}
		int playIndex = 0;
		if (index < 0 || index >= nChannels)
		{
			Log.w(TAG, ".play: channel index exceeds limits [0:" + nChannels + "), setting the index to 0");
		}
		else
		{
			playIndex = index;
		}
		final Channel channel = _feature.Component.EPG.getChannels().get(playIndex);
		Channel lastChannel = getLastChannel();
		if (lastChannel != null && lastChannel.getIndex() != channel.getIndex())
		{
			_userPrefs.put(UserParam.PREV_CHANNEL_ID, lastChannel.getChannelId());
		}
		_userPrefs.put(UserParam.LAST_CHANNEL_ID, channel.getChannelId());

		long seekTime = playTime;

		// set timeshift buffer size
		if (_featureTimeshift != null)
		{
			// will not modify timeshift parameters unless the
			// channel is changed
			if (lastChannel == null || lastChannel.getIndex() != channel.getIndex())
			{
				long bufferSize = _feature.Component.EPG.getStreamBufferSize(channel);
				Log.i(TAG, "Set timeshift buffer size of " + channel + " to " + bufferSize);
				if (bufferSize > 0)
				{
					// timeshift buffer is defined by stream
					// provider
					_featureTimeshift.setTimeshiftDuration(bufferSize);
				}
				else
				{
					// start timeshift buffer recording
					_featureTimeshift.startTimeshift();
				}
			}
			else
			{
				Log.i(TAG, "Keep timeshift buffer when the channel is not changed");
			}
			seekTime = _featureTimeshift.seekAt(playTime);
		}
		else
		{
			Log.d(TAG, "No timeshift support");
		}

		_lastPlayChannel = channel;

		// start playing retrieved channel stream
		_feature.Component.EPG.getStreamUrl(channel, seekTime, playDuration, new OnResultReceived()
		{
			@Override
			public void onReceiveResult(FeatureError error, final Object object)
			{
				if (error.isError())
				{
					Bundle bundle = new Bundle();
					bundle.putInt(EXTRA_GET_STREAM_ERROR_CODE, error.getCode());
					getEventMessenger().trigger(ON_GET_STREAM_ERROR, bundle);
				}
				else
				{
					String streamUrl = (String) object;
					if (streamUrl != null)
					{
						// play stream
						_feature.Component.PLAYER.play(streamUrl, MediaType.TV);
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
		Channel channel = _feature.Component.EPG.getChannelById(channelId);
		if (channel != null)
			play(channel.getIndex());
		else
			Log.w(TAG, "Channel id = " + channelId + " is not found");
	}

	/**
	 * Start playing last played channel
	 */
	public void playLast()
	{
		String lastChannelId = getLastChannelId();
		if (lastChannelId != null)
		{
			play(lastChannelId);
		}
	}

	/**
	 * Returns last played channel id
	 */
	public String getLastChannelId()
	{
		if (_userPrefs.has(UserParam.LAST_CHANNEL_ID))
		{
			return _userPrefs.getString(UserParam.LAST_CHANNEL_ID);
		}
		return null;
	}

	/**
	 * Set last channel id
	 */
	public void setLastChannelId(String channelId)
	{
		_userPrefs.put(UserParam.LAST_CHANNEL_ID, channelId);
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
	 * Set previous channel id
	 */
	public void setPreviousChannelId(String channelId)
	{
		_userPrefs.put(UserParam.PREV_CHANNEL_ID, channelId);
	}

	/**
	 * Returns last played channel
	 */
	public Channel getLastChannel()
	{
		String lastChannelId = getLastChannelId();
		if (lastChannelId != null)
		{
			return _feature.Component.EPG.getChannelById(lastChannelId);
		}
		return null;
	}

	/**
	 * Returns the previous played channel
	 */
	public Channel getPreviousChannel()
	{
		String previousChannelId = getPreviousChannelId();
		if (previousChannelId != null)
		{
			return _feature.Component.EPG.getChannelById(previousChannelId);
		}
		return null;
	}

	/**
	 * Set to use the favorite channels or the original channels list by
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
		loadFavoriteChannels();
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
		StringBuffer buffer = new StringBuffer();
		for (Channel channel : _feature.Component.EPG.getChannels())
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
		if (Environment.ON_RESUME == msgId)
		{
			if (!MediaType.TV.equals(_feature.Component.PLAYER.getMediaType()))
				// avoid handling non TV player events
				return;

			if (Environment.getInstance().isInitialized())
			{
				// restore last channel on app resume
				playLast();
			}
		}
		else if (Environment.ON_PAUSE == msgId)
		{
			if (!MediaType.TV.equals(_feature.Component.PLAYER.getMediaType()))
				// avoid handling non TV player events
				return;

			if (Environment.getInstance().isInitialized())
			{
				// stop tv playing on app pause
				_feature.Component.PLAYER.stop();
			}
		}
		else if (msgId == FeaturePlayer.ON_PLAY_STARTED)
		{
			if (!MediaType.TV.equals(_feature.Component.PLAYER.getMediaType()))
				// avoid handling non TV player events
				return;

			Channel prevChannel = getPreviousChannel();
			if (prevChannel == null || prevChannel.getIndex() != _lastPlayChannel.getIndex())
			{
				// trigger switching to new channel event
				Bundle switchBundle = new Bundle();
				if (prevChannel != null)
					switchBundle.putString(OnSwitchChannelExtras.FROM_CHANNEL.name(), prevChannel.getChannelId());
				switchBundle.putString(OnSwitchChannelExtras.TO_CHANNEL.name(), _lastPlayChannel.getChannelId());
				switchBundle.putLong(OnSwitchChannelExtras.SWITCH_DURATION.name(),
				        bundle.getLong(FeaturePlayer.Extras.TIME_ELAPSED.name()));
				getEventMessenger().trigger(ON_SWITCH_CHANNEL, switchBundle);
			}
		}
		else if (msgId == FeaturePlayer.ON_PLAY_PAUSE)
		{
			if (!MediaType.TV.equals(_feature.Component.PLAYER.getMediaType()))
				// avoid handling non TV player events
				return;

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
			if (!MediaType.TV.equals(_feature.Component.PLAYER.getMediaType()))
				// avoid handling non TV player events
				return;

			if (_featureTimeshift != null)
				_featureTimeshift.seekLive();
		}
	}

	private void loadFavoriteChannels()
	{
		_favoriteChannels.clear();
		if (_userPrefs.has(UserParam.CHANNELS))
		{
			String buffer = _userPrefs.getString(UserParam.CHANNELS);
			String[] channelIds = buffer.split(",");
			for (String channelId : channelIds)
			{
				Channel channel = _feature.Component.EPG.getChannelById(channelId);
				if (channel != null)
					_favoriteChannels.add(channel);
			}
		}
		if (_favoriteChannels.size() == 0 && isUseFavorites()
		        && getPrefs().getBool(Param.DEFAULT_ALL_CHANNELS_FAVORITE))
		{
			Log.i(TAG, "No favorite channels after update. Adding all channels as favorites");
			_favoriteChannels.addAll(_feature.Component.EPG.getChannels());
		}

		Log.i(TAG, "Loaded " + _favoriteChannels.size() + " favorite channels");
	}

	private void saveFavoriteChannels()
	{
		StringBuffer buffer = new StringBuffer();
		for (Channel channel : _favoriteChannels)
		{
			if (buffer.length() > 0)
				buffer.append(',');
			buffer.append(channel.getChannelId());
		}
		_userPrefs.put(UserParam.CHANNELS, buffer.toString());
		Log.i(TAG, "Updated favorites list: " + buffer);
	}

	// Command handlers
	private class OnCommandGetChannels implements CommandHandler
	{
		@Override
		public String getId()
		{
			return Command.GET_CHANNELS.name();
		}

		@Override
		public void execute(Bundle params, final OnResultReceived onResultReceived)
		{
			Log.i(TAG, ".OnCommandGetChannels.execute");
			FeatureChannels featureChannels = (FeatureChannels) Environment.getInstance().getFeatureComponent(
			        FeatureName.Component.CHANNELS);
			List<Channel> channels = featureChannels.getActiveChannels();
			JSONArray jsonChannels = new JSONArray();
			try
			{
				for (Channel channel : channels)
				{
					JSONObject jsonChannel = new JSONObject();
					jsonChannel.put("id", channel.getChannelId());
					if (channel instanceof ChannelBulsat)
						jsonChannel.put("thumbnail",
						        "data:image/png;base64," + channel.getChannelImageBase64(ChannelBulsat.LOGO_SELECTED));
					else
						jsonChannel.put("thumbnail",
						        "data:image/png;base64," + channel.getChannelImageBase64(Channel.LOGO_NORMAL));
					jsonChannel.put("title", channel.getTitle());
					jsonChannel.put("is_favorite", isChannelFavorite(channel));
					jsonChannels.put(jsonChannel);
				}
				onResultReceived.onReceiveResult(FeatureError.OK(FeatureChannels.this), jsonChannels);
			}
			catch (JSONException e)
			{
				onResultReceived.onReceiveResult(new FeatureError(FeatureChannels.this, e), null);
			}
		}
	}

	private class OnCommandAddRemoveFavorite implements CommandHandler
	{
		@Override
		public void execute(Bundle params, final OnResultReceived onResultReceived)
		{
			Log.i(TAG, ".OnCommandAddFavorite exec");

			String channelId = params.getString(CommandAddRemoveFavoriteExtras.CHANNEL_ID.name());
			String isFavorite = params.getString(CommandAddRemoveFavoriteExtras.IS_FAVORITE.name());

			Log.i(TAG, ".OnCommandAddFavorite.execute: channelId = " + channelId + ", isFavorite = " + isFavorite);

			try
			{
				if (channelId == null || isFavorite == null)
				{
					Log.i(TAG, "nothing to be displayed from records");
					onResultReceived.onReceiveResult(FeatureError.OK(FeatureChannels.this), null);
				}
				else
				{
					Channel channel = _feature.Component.EPG.getChannelById(channelId);

					setChannelFavorite(channel, Boolean.parseBoolean(isFavorite));

					onResultReceived.onReceiveResult(FeatureError.OK(FeatureChannels.this), null);
				}
			}
			catch (Exception e)
			{
				Log.e(TAG, e.getMessage(), e);
				onResultReceived.onReceiveResult(new FeatureError(FeatureChannels.this, e), null);
			}
		}

		@Override
		public String getId()
		{
			return Command.ADD_REMOVE_FAVORITES.name();
		}
	}
}

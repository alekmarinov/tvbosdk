/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureEPGZattooDirect.java
 * Author:      alek
 * Date:        1 Dec 2013
 * Description: Zattoo specific extension of EPG feature obtaining data directly from Zattoo servers
 */

package com.aviq.tv.android.sdk.feature.epg.zattoo;

import android.text.TextUtils;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.epg.FeatureEPGCompat;
import com.aviq.tv.android.sdk.feature.epg.Program;
import com.aviq.tv.android.sdk.feature.system.FeatureDevice.DeviceAttribute;

/**
 * Zattoo specific extension of EPG feature
 */
public class FeatureEPGZattooDirect extends FeatureEPGCompat
{
	public static final String TAG = FeatureEPGZattooDirect.class.getSimpleName();

	private ClientZAPI _clientZAPI;
	private OnFeatureInitialized _onFeatureInitialized;

	public static enum Param
	{
		/**
		 * Zattoo base URL
		 */
		ZATTOO_BASE_URL("https://zapi.zattoo.com"),

		/**
		 * Zattoo application ID
		 */
		ZATTOO_APP_TID("3c03cab5-cf36-49ad-88fa-2d25ea24042e"),
		/* a48d93cd-0247-4225-8063-301d540f3553 */

		/**
		 * Zattoo UUID
		 */
		ZATTOO_UUID("c7fb5bb2-c201-4b3a-9b76-7c25d5090cad"),

		/**
		 * requested minimum bitrate of zattoo stream
		 */
		ZATTOO_STREAM_MINRATE_ETH(1500),

		/**
		 * requested maximum bitrate of zattoo stream
		 */
		ZATTOO_STREAM_MAXRATE_ETH(5000),

		/**
		 * requested initial bitrate of zattoo stream
		 */
		ZATTOO_STREAM_INITRATE_ETH(0),

		/**
		 * requested minimum bitrate of zattoo stream
		 */
		ZATTOO_STREAM_MINRATE_WIFI(900),

		/**
		 * requested maximum bitrate of zattoo stream
		 */
		ZATTOO_STREAM_MAXRATE_WIFI(3000),

		/**
		 * requested initial bitrate of zattoo stream
		 */
		ZATTOO_STREAM_INITRATE_WIFI(0),

		/**
		 * force using this username for zattoo
		 */
		ZATTOO_FORCE_USER(""),

		/**
		 * force using this password for zattoo
		 */
		ZATTOO_FORCE_PASS("");

		Param(String value)
		{
			if (value != null)
				Environment.getInstance().getFeaturePrefs(FeatureName.Scheduler.EPG).put(name(), value);
		}

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Scheduler.EPG).put(name(), value);
		}
	}

	enum UserParam
	{
		/**
		 * Registered Zattoo user account name
		 */
		ZATTOO_USER,

		/**
		 * Registered Zattoo account password
		 */
		ZATTOO_PASS
	}

	public FeatureEPGZattooDirect() throws FeatureNotFoundException
	{
		require(FeatureName.State.NETWORK_WIZARD);
		require(FeatureName.Component.DEVICE);
	}

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		_onFeatureInitialized = onFeatureInitialized;
		_clientZAPI = new ClientZAPI(this, getPrefs().getString(Param.ZATTOO_BASE_URL), getPrefs().getInt(
		        Param.ZATTOO_STREAM_MINRATE_ETH), getPrefs().getInt(Param.ZATTOO_STREAM_MAXRATE_ETH), getPrefs()
		        .getInt(Param.ZATTOO_STREAM_INITRATE_ETH), getPrefs().getInt(Param.ZATTOO_STREAM_MINRATE_WIFI),
		        getPrefs().getInt(Param.ZATTOO_STREAM_MAXRATE_WIFI), getPrefs().getInt(
		                Param.ZATTOO_STREAM_INITRATE_WIFI), getPrefs().getInt(FeatureEPGCompat.Param.MAX_CHANNELS));
		Prefs userPrefs = Environment.getInstance().getUserPrefs();
		String mac = _feature.Component.DEVICE.getDeviceAttribute(DeviceAttribute.MAC);
		mac = mac.replace(":", "");
		if (!userPrefs.has(UserParam.ZATTOO_USER))
		{
			String forceUser = getPrefs().getString(Param.ZATTOO_FORCE_USER);
			if (TextUtils.isEmpty(forceUser))
				userPrefs.put(UserParam.ZATTOO_USER, mac);
			else
				userPrefs.put(UserParam.ZATTOO_USER, forceUser);
		}
		if (!userPrefs.has(UserParam.ZATTOO_PASS))
		{
			String forcePass = getPrefs().getString(Param.ZATTOO_FORCE_PASS);
			if (TextUtils.isEmpty(forcePass))
				userPrefs.put(UserParam.ZATTOO_PASS, mac);
			else
				userPrefs.put(UserParam.ZATTOO_PASS, forcePass);
		}
		final String username = userPrefs.getString(UserParam.ZATTOO_USER);
		final String password = userPrefs.getString(UserParam.ZATTOO_PASS);
		_clientZAPI.hello(getPrefs().getString(Param.ZATTOO_APP_TID), getPrefs().getString(Param.ZATTOO_UUID),
		        new OnResultReceived()
		        {
			        @Override
			        public void onReceiveResult(FeatureError error, Object object)
			        {
				        if (!error.isError())
				        {
					        // hello zattoo, loging in...
					        _clientZAPI.login(username, password, new OnResultReceived()
					        {
						        @Override
						        public void onReceiveResult(FeatureError error, Object object)
						        {
							        Log.i(TAG, "login response: " + error);

							        if (!error.isError())
							        {
								        if (!uncacheEpgData())
								        {
									        _clientZAPI.retrieveChannels(new OnResultReceived()
									        {
										        @Override
										        public void onReceiveResult(FeatureError error, Object object)
										        {
											        if (!error.isError())
											        {
												        _clientZAPI.retrievePrograms(new OnResultReceived()
												        {
													        @Override
													        public void onReceiveResult(FeatureError error, Object object)
													        {
														        Log.i(TAG, "Updating EPG finished with status " + error);
														        _epgData = _clientZAPI.getEpgData();
														        cacheEpgData();
														        onEPGLoadFinished(error);
													        }
												        });
											        }
											        else
											        {
												        onEPGLoadFinished(error);
											        }
										        }
									        });
								        }
								        else
								        {
									        // load channel logos
									        _clientZAPI.retrieveChannelLogos(_epgData, new OnResultReceived()
									        {
										        @Override
										        public void onReceiveResult(FeatureError error, Object object)
										        {
											        onEPGLoadFinished(error);
										        }
									        });
								        }
							        }
							        else
							        {
								        onEPGLoadFinished(error);
							        }
						        }
					        });
				        }
				        else
				        {
					        onEPGLoadFinished(error);
				        }
			        }
		        });
	}

	@Override
	protected void onSchedule(OnFeatureInitialized onFeatureInitialized)
	{
		// FIXME: implement scheduled EPG data retrieval
	}

	/**
	 * @return rayv EPG provider name
	 */
	@Override
	protected FeatureEPGCompat.Provider getEPGProvider()
	{
		return FeatureEPGCompat.Provider.zattoo;
	}

	@Override
	protected Program createProgram(String id, Channel channel)
	{
		return new ProgramZattoo(id, channel);
	}

	@Override
	protected Channel createChannel(int index)
	{
		return new ChannelZattoo(index);
	}

	@Override
	public void getStreamUrl(Channel channel, long playTime, long playDuration,
	        final OnStreamURLReceived onStreamURLReceived)
	{
		boolean isEthernet = "ETHERNET".equals(_feature.Component.DEVICE.getDeviceAttribute(DeviceAttribute.NETWORK));
		_clientZAPI.watch(channel.getChannelId(), "hls", isEthernet, new OnResultReceived()
		{
			@Override
			public void onReceiveResult(FeatureError error, Object object)
			{
				String url = null;
				if (!error.isError())
					url = error.getBundle().getString(ClientZAPI.EXTRA_URL);
				onStreamURLReceived.onStreamURL(error, url);
			}
		});
	}

	@Override
	public void getProgramDetails(String channelId, final Program program, final IOnProgramDetails onProgramDetails)
	{
		_clientZAPI.retrieveProgramDetails((ProgramZattoo) program, new OnResultReceived()
		{
			@Override
			public void onReceiveResult(FeatureError error, Object object)
			{
				onProgramDetails.onProgramDetails(error, program);
			}
		});
	}

	@Override
	public long getStreamBufferSize(Channel channel)
	{
		return 0;
	}

	private void onEPGLoadFinished(FeatureError error)
	{
		Log.i(TAG, ".onEPGLoadFinished: error = " + error);
		if (!error.isError())
		{
			Log.i(TAG, ".onEPGLoadFinished: _onFeatureInitialized = " + _onFeatureInitialized);
			_onFeatureInitialized.onInitialized(error);
		}
		else if (error.getCode() == 402)
		{
			// translates HTTP 402 error code to SUBSCRIPTION_ERROR
			error.setCode(ResultCode.SUBSCRIPTION_ERROR);
			_onFeatureInitialized.onInitialized(error);
		}
		else
		{
			// FIXME: show continue dialog
			_onFeatureInitialized.onInitialized(error);
		}
	}
}

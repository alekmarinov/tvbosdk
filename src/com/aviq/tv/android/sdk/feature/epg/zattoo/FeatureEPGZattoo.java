/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureEPGZattoo.java
 * Author:      alek
 * Date:        1 Dec 2013
 * Description: Zattoo specific extension of EPG feature
 */

package com.aviq.tv.android.sdk.feature.epg.zattoo;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.epg.Channel;
import com.aviq.tv.android.sdk.feature.epg.FeatureEPG;
import com.aviq.tv.android.sdk.feature.epg.Program;

/**
 * Zattoo specific extension of EPG feature
 */
public class FeatureEPGZattoo extends FeatureEPG
{
	public static final String TAG = FeatureEPGZattoo.class.getSimpleName();

	private ClientZAPI _clientZAPI;

	public static enum Param
	{
		/**
		 * Registered Zattoo user account name
		 */
		ZATTOO_USER("aviq@zattoo.com"),

		/**
		 * Registered Zattoo account password
		 */
		ZATTOO_PASS("avZat14"),

		/**
		 * Zattoo base URL
		 */
		ZATTOO_BASE_URL("https://zapi.zattoo.com"),

		/**
		 * Zattoo application ID
		 */
		ZATTOO_APP_TID("a48d93cd-0247-4225-8063-301d540f3553"),

		/**
		 * Zattoo UUID
		 */
		ZATTOO_UUID("c7fb5bb2-c201-4b3a-9b76-7c25d5090cad"),

		/**
		 * requested minimum bitrate of zattoo stream
		 */
		ZATTOO_STREAM_MINRATE(1100),

		/**
		 * requested initial bitrate of zattoo stream
		 */
		ZATTOO_STREAM_INITRATE(0);

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

	public FeatureEPGZattoo() throws FeatureNotFoundException
	{
		require(FeatureName.State.NETWORK_WIZARD);
	}

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		_clientZAPI = new ClientZAPI(getPrefs().getString(Param.ZATTOO_BASE_URL), getPrefs().getInt(
		        Param.ZATTOO_STREAM_MINRATE), getPrefs().getInt(Param.ZATTOO_STREAM_INITRATE));
		_clientZAPI.hello(getPrefs().getString(Param.ZATTOO_APP_TID), getPrefs().getString(Param.ZATTOO_UUID),
		        new OnResultReceived()
		        {
			        @Override
			        public void onReceiveResult(FeatureError result)
			        {
				        if (!result.isError())
				        {
					        // hello zattoo, loging in...
					        _clientZAPI.login(getPrefs().getString(Param.ZATTOO_USER),
					                getPrefs().getString(Param.ZATTOO_PASS), new OnResultReceived()
					                {
						                @Override
						                public void onReceiveResult(FeatureError result)
						                {
							                Log.i(TAG, "login response: " + result);
							                FeatureEPGZattoo.super.initialize(onFeatureInitialized);
						                }
					                });
				        }
				        else
				        {
					        onFeatureInitialized.onInitialized(result);
				        }
			        }
		        });
	}

	/**
	 * @return rayv EPG provider name
	 */
	@Override
	protected FeatureEPG.Provider getEPGProvider()
	{
		return FeatureEPG.Provider.zattoo;
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
		_clientZAPI.watch(channel.getChannelId(), "hls", new OnResultReceived()
		{
			@Override
			public void onReceiveResult(FeatureError result)
			{
				String url = null;
				if (!result.isError())
					url = result.getBundle().getString(ClientZAPI.EXTRA_URL);
				onStreamURLReceived.onStreamURL(url);
			}
		});
	}

	@Override
	public long getStreamBufferSize(Channel channel)
	{
		return 0;
	}
}

/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeaturePlayerZattoo.java
 * Author:      alek
 * Date:        1 Dec 2013
 * Description: Component feature providing Zattoo player
 */

package com.aviq.tv.android.sdk.feature.player.zattoo;

import android.os.Bundle;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.player.FeatureStreamer;

/**
 * Component feature providing RayV streamer
 */
public class FeatureStreamerZattoo extends FeatureStreamer
{
	public static final String TAG = FeatureStreamerZattoo.class.getSimpleName();

	public enum Param
	{
		/**
		 * Registered Zattoo user account name
		 */
		ZATTOO_USER("samtest@zattoo.com"),

		/**
		 * Registered Zattoo account password
		 */
		ZATTOO_PASS("12345"),

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
		ZATTOO_UUID("c7fb5bb2-c201-4b3a-9b76-7c25d5090cad");

		Param(String value)
		{
			if (value != null)
				Environment.getInstance().getFeaturePrefs(FeatureName.Component.STREAMER).put(name(), value);
		}

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.STREAMER).put(name(), value);
		}
	}

	private ClientZAPI _clientZAPI;

	public FeatureStreamerZattoo() throws FeatureNotFoundException
	{
		require(FeatureName.State.NETWORK_WIZARD);
	}

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		_clientZAPI = new ClientZAPI(getPrefs().getString(Param.ZATTOO_BASE_URL));
		_clientZAPI.hello(getPrefs().getString(Param.ZATTOO_APP_TID), getPrefs().getString(Param.ZATTOO_UUID),
		        new OnResultReceived()
		        {
			        @Override
			        public void onReceiveResult(int resultCode, Bundle resultData)
			        {
				        if (ResultCode.OK == resultCode)
				        {
					        // hello zattoo, loging in...
					        _clientZAPI.login(getPrefs().getString(Param.ZATTOO_USER),
					                getPrefs().getString(Param.ZATTOO_PASS), new OnResultReceived()
					                {
						                @Override
						                public void onReceiveResult(int resultCode, Bundle resultData)
						                {
						                	Log.i(TAG, "login response: " + resultCode);
							                onFeatureInitialized.onInitialized(FeatureStreamerZattoo.this, resultCode);
						                }
					                });
				        }
				        else
				        {
					        onFeatureInitialized.onInitialized(FeatureStreamerZattoo.this, resultCode);
				        }
			        }
		        });
	}

	/**
	 * Fetch stream url by specified streamId using ZAPI watch method
	 *
	 * @param streamId
	 * @param onStreamURLReceived callback invoked with corresponding stream url to specified streamId
	 */
	@Override
    public void getUrlByStreamId(String streamId, long playTimeDelta, final OnStreamURLReceived onStreamURLReceived)
	{
		_clientZAPI.watch(streamId, "hls", new OnResultReceived()
		{
			@Override
			public void onReceiveResult(int resultCode, Bundle resultData)
			{
				onStreamURLReceived.onStreamURL(resultData.getString(ClientZAPI.EXTRA_URL));
			}
		});
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.STREAMER;
	}
}

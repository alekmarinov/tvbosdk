/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureRegister.java
 * Author:      alek
 * Date:        3 Dec 2013
 * Description: Feature registering box to ABMP
 */

package com.aviq.tv.android.sdk.feature.register;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Base64;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventReceiver;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;
import com.aviq.tv.android.sdk.core.feature.annotation.Priority;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.internet.FeatureInternet;
import com.aviq.tv.android.sdk.feature.system.FeatureDevice.DeviceAttribute;

/**
 * Feature registering box to ABMP
 */
@SuppressLint("DefaultLocale")
@Priority
@Author("alek")
public class FeatureRegister extends FeatureComponent
{
	private static final String TAG = FeatureRegister.class.getSimpleName();

	public static enum Param
	{
		/**
		 * Box ID
		 */
		BOX_ID(""),

		/**
		 * ABMP Server address
		 */
		ABMP_SERVER(""),

		/**
		 * ABMP registration URL format
		 */
		ABMP_REGISTER_URL(
		        "${SERVER}/Box/Register.aspx?boxID=${BOX_ID}&version=${VERSION}&brand=${BRAND}&network=${NETWORK}"),

		/**
		 * Ping ABMP interval in seconds
		 */
		ABMP_REGISTER_INTERVAL(60);

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.REGISTER).put(name(), value);
		}

		Param(String value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.REGISTER).put(name(), value);
		}
	}

	private String _userToken;
	private String _version;

	/**
	 * @throws FeatureNotFoundException
	 */
	public FeatureRegister() throws FeatureNotFoundException
	{
		require(FeatureName.Scheduler.INTERNET);
		require(FeatureName.Component.DEVICE);
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		final String brand = _feature.Component.DEVICE.getDeviceAttribute(DeviceAttribute.BRAND);
		final String boxId = _feature.Component.DEVICE.getDeviceAttribute(DeviceAttribute.MAC);
		_userToken = createUserToken(boxId);
		_version = Environment.getInstance().getBuildVersion();

		_feature.Scheduler.INTERNET.getEventMessenger().register(new EventReceiver()
		{
			@Override
			public void onEvent(int msgId, Bundle bundle)
			{
				String registrationUrl = getRegistrationUrl(brand, boxId, _version);
				_feature.Scheduler.INTERNET.getUrlContent(registrationUrl, new OnResultReceived()
				{
					@Override
					public void onReceiveResult(FeatureError error, Object object)
					{
						Log.d(TAG, ".initialize:onReceiveResult: " + error);
					}
				});
			}
		}, FeatureInternet.ON_CONNECTED);

		super.initialize(onFeatureInitialized);
	}

	@Override
	public FeatureName.Component getComponentName()
	{
		return FeatureName.Component.REGISTER;
	}

	/**
	 * @return the user token for this box
	 */
	public String getUserToken()
	{
		return _userToken;
	}

	private String getActiveNetworkType()
	{
		final ConnectivityManager connectivityManager = (ConnectivityManager) Environment.getInstance()
		        .getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
		return (netInfo != null) ? netInfo.getTypeName().toLowerCase() : "unknown";
	}

	private String base64(byte[] bytes)
	{
		return android.util.Base64.encodeToString(bytes, Base64.DEFAULT);
	}

	private String createUserToken(String mac)
	{
		try
		{
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			digest.update(mac.getBytes());
			return base64(digest.digest());
		}
		catch (NoSuchAlgorithmException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		return null;
	}

	private String getRegistrationUrl(String brand, String boxId, String version)
	{
		Bundle bundle = new Bundle();
		bundle.putString("SERVER", getPrefs().getString(Param.ABMP_SERVER));
		bundle.putString("BOX_ID", boxId);
		bundle.putString("VERSION", version);
		bundle.putString("BRAND", brand);
		bundle.putString("NETWORK", getActiveNetworkType());

		String registrationUrl = getPrefs().getString(Param.ABMP_REGISTER_URL, bundle);
		return registrationUrl;
	}
}

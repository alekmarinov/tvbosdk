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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Base64;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.feature.internet.FeatureInternet;
import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Feature registering box to ABMP
 */
@SuppressLint("DefaultLocale")
public class FeatureRegister extends FeatureComponent
{
	private static final String TAG = FeatureRegister.class.getSimpleName();

	public enum Param
	{
		/**
		 * ABMP Server address
		 */
		ABMP_SERVER(""),

		/**
		 * Brand name to represent the box on ABMP
		 */
		BRAND(""),

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

	private static final String MAC_ADDRESS_FILE = "/sys/class/net/eth0/address";
	private String _boxId;
	private String _userToken;
	private String _version;
	private String _registrationUrl;
	private String _brand;

	/**
	 * @param environment
	 */
	public FeatureRegister()
	{
		_dependencies.Schedulers.add(FeatureName.Scheduler.INTERNET);
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		try
		{
			_brand = getPrefs().getString(Param.BRAND);
			_boxId = readMacAddress();
			_userToken = createUserToken(_boxId);
			_version = Environment.getInstance().getBuildVersion();

			Bundle bundle = new Bundle();
			bundle.putString("SERVER", getPrefs().getString(Param.ABMP_SERVER));
			bundle.putString("BOX_ID", _boxId);
			bundle.putString("VERSION", _version);
			bundle.putString("BRAND", _brand);
			bundle.putString("NETWORK", getActiveNetworkType());

			_registrationUrl = getPrefs().getString(Param.ABMP_REGISTER_URL, bundle);

			FeatureInternet featureInternet = (FeatureInternet) Environment.getInstance().getFeatureScheduler(
			        FeatureName.Scheduler.INTERNET);
			featureInternet.startCheckUrl(_registrationUrl);

			super.initialize(onFeatureInitialized);
		}
		catch (FeatureNotFoundException e)
		{
			Log.e(TAG, e.getMessage(), e);
			onFeatureInitialized.onInitialized(this, ResultCode.GENERAL_FAILURE);
		}
		catch (FileNotFoundException e)
		{
			Log.e(TAG, e.getMessage(), e);
			onFeatureInitialized.onInitialized(this, ResultCode.GENERAL_FAILURE);
		}
	}

	@Override
	public FeatureName.Component getComponentName()
	{
		return FeatureName.Component.REGISTER;
	}

	/**
	 * @return the box ID (MAC address)
	 */
	public String getBoxId()
	{
		// if (true) return "902B34F69D99"; //TODO used to test FW download
		return _boxId;
	}

	/**
	 * @return the url to ABMP
	 */
	public String getRegistrationUrl()
	{
		return _registrationUrl;
	}

	/**
	 * @return the user token for this box
	 */
	public String getUserToken()
	{
		return _userToken;
	}

	public String getBrand()
	{
		return _brand;
	}

	private String readMacAddress() throws FileNotFoundException
	{
		FileInputStream fis = new FileInputStream(MAC_ADDRESS_FILE);
		String macAddress = TextUtils.inputStreamToString(fis);
		macAddress = macAddress.substring(0, 17);
		return macAddress.replace(":", "").toUpperCase();
	}

	private String getActiveNetworkType()
	{
		final ConnectivityManager connectivityManager = (ConnectivityManager) Environment.getInstance()
		        .getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
		return (netInfo != null) ? netInfo.getTypeName().toLowerCase() : "";
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
}

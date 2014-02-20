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

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

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
		ABMP_SERVER("http://aviq.dyndns.org:984"),

		/**
		 * Brand name to represent the box on ABMP
		 */
		BRAND("zixi"),

		/**
		 * ABMP registration URL format
		 */
		ABMP_REGISTER_URL("${SERVER}/Box/Register.aspx?boxID=${BOX_ID}&version=${VERSION}&brand=${BRAND}&network=${NETWORK}"),

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
	private String _version;

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
			_boxId = readMacAddress();
			_version = Environment.getInstance().getBuildVersion();

			Bundle bundle = new Bundle();
			bundle.putString("SERVER", getPrefs().getString(Param.ABMP_SERVER));
			bundle.putString("BOX_ID", _boxId);
			bundle.putString("VERSION", _version);
			bundle.putString("BRAND", getPrefs().getString(Param.BRAND));
			bundle.putString("NETWORK", getActiveNetworkType());

			String abmpRegUrl = getPrefs().getString(Param.ABMP_REGISTER_URL, bundle);
			int registerInterval = getPrefs().getInt(Param.ABMP_REGISTER_INTERVAL);

			FeatureInternet featureInternet = (FeatureInternet) Environment.getInstance()
			        .getFeatureScheduler(FeatureName.Scheduler.INTERNET);
			featureInternet.addCheckUrl(abmpRegUrl, registerInterval, new FeatureInternet.OnResultReceived()
			{
				@Override
				public void onReceiveResult(int resultCode, Bundle resultData)
				{
					Log.i(TAG,
					        ".onReceiveResult: resultCode = " + resultCode + ", url = " + resultData.getString("URL"));
					if (resultCode != ResultCode.OK)
					{
						// TODO: Take decision what to do if the box is disabled
						// or no access to ABMP services
					}
				}
			});
			onFeatureInitialized.onInitialized(this, ResultCode.OK);
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

	public String getBoxId()
	{
		// if (true) return "902B34F69D99"; //TODO used to test FW download
		return _boxId;
	}

	private String readMacAddress() throws FileNotFoundException
	{
		FileInputStream fis = new FileInputStream(MAC_ADDRESS_FILE);
		String macAddress = TextUtils.inputSteamToString(fis);
		macAddress = macAddress.substring(0, 17);
		return macAddress.replace(":", "").toUpperCase();
	}

	private String getActiveNetworkType()
	{
		final ConnectivityManager connectivityManager = (ConnectivityManager) Environment.getInstance().getActivity()
		        .getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
		return (netInfo != null) ? netInfo.getTypeName().toLowerCase() : "";
	}
}

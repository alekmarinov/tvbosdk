/**
 * Copyright (c) 2007-2015, Intelibo Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureNetworkTime.java
 * Author:      alek
 * Date:        23 Jul 2015
 * Description: Feature providing network time
 */

package com.aviq.tv.android.sdk.feature.internet;

import java.net.InetAddress;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.commons.net.ntp.TimeStamp;

import android.app.AlarmManager;
import android.content.Context;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.utils.Calendars;

/**
 * Feature providing network time
 */
public class FeatureNetworkTime extends FeatureComponent
{
	private static final String TAG = FeatureNetworkTime.class.getSimpleName();
	private String _ntpServer;

	public static enum Param
	{
		/**
		 * Enable or disable time correction via NTP
		 */
		ENABLE_NTP(false),

		/**
		 * Network client timeout
		 */
		TIMEOUT(10000),

		/**
		 * NTP server address
		 */
		NTP_SERVER("bg.pool.ntp.org");

		Param(boolean value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.NETWORK_TIME).put(name(), value);
		}

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.NETWORK_TIME).put(name(), value);
		}

		Param(String value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.NETWORK_TIME).put(name(), value);
		}
	}

	/**
	 * @throws FeatureNotFoundException
	 */
	public FeatureNetworkTime() throws FeatureNotFoundException
	{
		require(FeatureName.State.NETWORK_WIZARD);
	}

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		if (!getPrefs().getBool(Param.ENABLE_NTP))
		{
			Log.i(TAG, "Time update via NTP is disabled");
			super.initialize(onFeatureInitialized);
		}
		else
		{
			_ntpServer = getPrefs().getString(Param.NTP_SERVER);
			Log.i(TAG, ".initialize: Updating time from NTP server " + _ntpServer);

			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					NTPUDPClient client = new NTPUDPClient();
					client.setDefaultTimeout(getPrefs().getInt(Param.TIMEOUT));
					try
					{
						client.open();
						InetAddress hostAddr = InetAddress.getByName(_ntpServer);
						Log.i(TAG, _ntpServer + " resolved to " + hostAddr.getHostAddress());
						TimeInfo timeInfo = client.getTime(hostAddr);
						TimeStamp receivedTimeStamp = timeInfo.getMessage().getReceiveTimeStamp();
						Log.i(TAG, "Destination time: " + Calendars.makeString((int) (receivedTimeStamp.getTime() / 1000)));
						Calendar dateTime = new GregorianCalendar();
						dateTime.setTimeInMillis(receivedTimeStamp.getTime());
						setDateTime(dateTime);
						client.close();

						Environment.getInstance().runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								FeatureNetworkTime.super.initialize(onFeatureInitialized);
							}
						});
					}
					catch (final Exception e)
					{
						Log.e(TAG, e.getMessage(), e);
						Environment.getInstance().runOnUiThread(new Runnable()
						{
							@Override
							public void run()
							{
								onFeatureInitialized.onInitialized(new FeatureError(FeatureNetworkTime.this, e));
							}
						});
					}
				}
			}).start();
		}
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.NETWORK_TIME;
	}

	protected void setDateTime(Calendar dateTime)
	{
		try
		{
			Log.i(TAG,
			        ".setDateTime: " + Calendars.makeString(dateTime) + " over current time = "
			                + Calendars.makeString(new GregorianCalendar()));
			AlarmManager am = (AlarmManager) Environment.getInstance().getSystemService(Context.ALARM_SERVICE);
			am.setTime(dateTime.getTimeInMillis());
		}
		catch (SecurityException se)
		{
			Log.w(TAG, se.getMessage(), se);
		}
	}
}

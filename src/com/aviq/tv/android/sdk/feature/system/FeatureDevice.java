/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureDevice.java
 * Author:      elmira
 * Date:        4 Sep 2014
 * Description: Defines device parameters
 */
package com.aviq.tv.android.sdk.feature.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import android.os.Bundle;
import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.utils.Files;

/**
 * Defines device parameters
 */
public class FeatureDevice extends FeatureComponent
{
	public static final String TAG = FeatureDevice.class.getSimpleName();
	private static final int ON_STATUS = EventMessenger.ID("ON_STATUS");
	private static String STAT_CMD = "vmstat -d %d";
	private static final String PARAM_CPU_IDLE = "cpuidle";
	private static final String PARAM_FREE_MEM = "freemem";

	private long _freeMemTotal;
	private long _freeMemSamplesCount;
	private long _cpuIdleTotal;
	private long _cpuIdleSamplesCount;
	private long _cpuIdleMin;
	private long _cpuIdleMax;
	private long _currentTimeInMs;
	private String _vmCmd;

	private final HashMap<String, IStatusFieldGetter> _fieldGetters = new HashMap<String, IStatusFieldGetter>();

	public enum Param
	{
		/**
		 * Box customer
		 */
		CUSTOMER(""),

		/**
		 * Application brand name
		 */
		BRAND("generic"),

		/**
		 * Release or development build kind
		 */
		BUILD("devel"),

		/**
		 * Box mac address
		 */
		MAC("00:00:00:00:00:00"),

		/**
		 * vmstat -d delay
		 */
		VMSTAT_DELAY(1),

		/**
		 * The interval to trigger ON_STATUS event
		 */
		STATUS_INTERVAL(60);

		Param(int value)
		{
			Environment.getInstance().getFeature(FeatureDevice.class).getPrefs().put(name(), value);
		}

		Param(String value)
		{
			Environment.getInstance().getFeature(FeatureDevice.class).getPrefs().put(name(), value);
		}
	}

	public enum DeviceAttribute
	{
		CUSTOMER, BRAND, BUILD, VERSION, MAC
	}

	public FeatureDevice() throws FeatureNotFoundException
	{
		require(Component.TIMEZONE);
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.DEVICE;
	}

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		_currentTimeInMs = _feature.Component.TIMEZONE.getCurrentTime().getTimeInMillis();
		long vmStatDelay = getPrefs().getInt(Param.VMSTAT_DELAY);
		_vmCmd = String.format(STAT_CMD, vmStatDelay);
		Log.w(TAG, "VMCmd = " + _vmCmd);

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				InputStream is = null;
				try
				{
					Process proc = Runtime.getRuntime().exec(_vmCmd);
					is = proc.getInputStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(is));
					String line = reader.readLine();
					while (line != null)
					{
						line = reader.readLine().trim();
						String[] pieces = line.split("\\s+");
						if (pieces.length == 15)
						{
							try
							{
								if (!"free".equals(pieces[2]))
								{
									long freemem = Long.parseLong(pieces[2]);
									long cpuidle = Long.parseLong(pieces[12]);
									_freeMemTotal += freemem;
									_freeMemSamplesCount += 1;
									_cpuIdleTotal += cpuidle;
									_cpuIdleSamplesCount += 1;
									long timeInMs = _feature.Component.TIMEZONE.getCurrentTime().getTimeInMillis();
									long deltaInSeconds = (timeInMs - _currentTimeInMs) / 1000;
									long statusInterval = getPrefs().getInt(Param.STATUS_INTERVAL);
									if (cpuidle < _cpuIdleMin)
									{
										_cpuIdleMin = cpuidle;
									}
									if (cpuidle > _cpuIdleMax)
									{
										_cpuIdleMax = cpuidle;
									}
									if (deltaInSeconds > statusInterval)
									{
										sendStatus();

									}

								}

							}
							catch (NumberFormatException e)
							{
								Log.w(TAG, e.getMessage());
							}
						}
						else
						{
							if (pieces.length != 4)
							{
								Log.e(TAG, "Expected number of columns 4 or 15 got " + pieces.length);
							}
						}
					}
				}
				catch (IOException e)
				{
					Log.e(TAG, e.getMessage(), e);
					Files.closeQuietly(is, TAG);
				}
			}
		}).start();

		super.initialize(onFeatureInitialized);
	}

	private void sendStatus()
	{
		Bundle bundle = new Bundle();
		long cpuMean = _cpuIdleTotal / _cpuIdleSamplesCount;
		long memMean = _freeMemTotal / _freeMemSamplesCount;
		bundle.putLong(PARAM_CPU_IDLE, cpuMean);
		bundle.putLong(PARAM_FREE_MEM, memMean);
		for (String paramName : _fieldGetters.keySet())
		{
			IStatusFieldGetter getter = _fieldGetters.get(paramName);
			bundle.putString(paramName, getter.getStatusField());
			Log.i(TAG, paramName + " = " + getter.getStatusField());
		}
		Log.i(TAG, "Send status PARAM_CPU_IDLE = " + cpuMean + " PARAM_FREE_MEM = " + memMean);

		getEventMessenger().trigger(ON_STATUS, bundle);

		reset();
	}

	private void reset()
	{
		_cpuIdleMin = Long.MIN_VALUE;
		_cpuIdleMax = Long.MAX_VALUE;
		_freeMemTotal = 0;
		_freeMemSamplesCount = 0;
		_cpuIdleTotal = 0;
		_cpuIdleSamplesCount = 0;
		_currentTimeInMs = _feature.Component.TIMEZONE.getCurrentTime().getTimeInMillis();
	}

	/**
	 * Gets device attribute
	 *
	 * @param deviceAttribute the name of the device attribute
	 * @return device attribute value
	 */
	public String getDeviceAttribute(DeviceAttribute deviceAttribute)
	{
		switch (deviceAttribute)
		{
			case CUSTOMER:
				return getPrefs().getString(Param.CUSTOMER);

			case BRAND:
				return getPrefs().getString(Param.BRAND);

			case VERSION:
				return Environment.getInstance().getBuildVersion();

			case BUILD:
				return getPrefs().getString(Param.BUILD);

			case MAC:
			break;
		}
		return null;
	}

	/**
	 * Provide field status accessing interface
	 *
	 * @param fieldName
	 *            - field name
	 * @IStatusFieldGetter - field value providing interface
	 */
	public void addStatusFieldGetter(String fieldName, IStatusFieldGetter getter)
	{
		_fieldGetters.put(fieldName, getter);
	}

	public static interface IStatusFieldGetter
	{
		String getStatusField();
	}
}

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

import android.content.pm.PackageManager.NameNotFoundException;
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
	private final String _vmCmd;

	private final HashMap<String, IStatusFieldGetter> _params;

	public enum Param
	{
		CUSTOMER(""),

		BRAND(""),

		RELEASE("devel"),

		VMSTAT_DELAY(1),

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

	FeatureDevice() throws FeatureNotFoundException
	{
		super();
		require(Component.TIMEZONE);
		_currentTimeInMs = _feature.Component.TIMEZONE.getCurrentTime().getTimeInMillis();
		_params = new HashMap<String, IStatusFieldGetter>();
		long vmStatDelay = getPrefs().getInt(Param.VMSTAT_DELAY);
		_vmCmd = String.format(STAT_CMD, vmStatDelay);
		Log.w(TAG, "VMCmd = " + _vmCmd);
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
		for (String paramName : _params.keySet())
		{
			IStatusFieldGetter getter = _params.get(paramName);
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
	 * Parses the version string from the manifest.
	 *
	 * @return build version
	 * @throws NameNotFoundException
	 */
	public String getBuildVersion()
	{
		return Environment.getInstance().getBuildVersion();
	}

	/**
	 * Get customer
	 *
	 * @return customer
	 */
	public String getCustomer()
	{
		return getPrefs().getString(Param.CUSTOMER);
	}

	/**
	 * Get device brand
	 *
	 * @return device brand
	 */
	public String getBrand()
	{
		return getPrefs().getString(Param.BRAND);
	}

	/**
	 * Get device build
	 *
	 * @return build
	 */
	public String getBuildKind()
	{
		return getPrefs().getString(Param.RELEASE);
	}

	/**
	 * Provide field status accessing interface
	 *
	 * @param param
	 *            - field name
	 * @IStatusFieldGetter - field value providing interface
	 * @return void
	 */
	public void addStatusFieldGetter(String param, IStatusFieldGetter getter)
	{
		_params.put(param, getter);
	}

	public static interface IStatusFieldGetter
	{
		String getStatusField();

	}
}

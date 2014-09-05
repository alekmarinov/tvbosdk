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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.StatFs;
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
	private static int KB = 1024;
	private static String STAT_CMD = "vmstat -n 1 -d %d";

	public enum OnStatusExtra
	{
		cpuidle, uplink, downlink, memfree, hddintfree, hddextfree, network
	}

	public enum DeviceAttribute
	{
		CUSTOMER, BRAND, BUILD, VERSION, MAC
	}

	public enum HDD_MEM_TYPE
	{
		hdd_int, hdd_ext
	}

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
		STATUS_INTERVAL(4),

		/**
		 * HDD Memory units: 0 - B, 1 - KB, 2 - MB, 3 - GB
		 */
		HDD_UNIT(0);

		Param(int value)
		{
			Environment.getInstance().getFeature(FeatureDevice.class).getPrefs().put(name(), value);
		}

		Param(String value)
		{
			Environment.getInstance().getFeature(FeatureDevice.class).getPrefs().put(name(), value);
		}
	}

	private long _memFreeTotal;
	private long _cpuIdleTotal;
	private long _vmstatSamplesCount;
	private long _cpuIdleMin;
	private long _cpuIdleMax;
	private long _lastSendTime;
	private String _vmCmd;
	private long _bytesRcvd;
	private long _bytesSent;

	private final HashMap<String, IStatusFieldGetter> _fieldGetters = new HashMap<String, IStatusFieldGetter>();

	public FeatureDevice() throws FeatureNotFoundException
	{
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
		reset();
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
					while (true)
					{
						Process proc = Runtime.getRuntime().exec(_vmCmd);
						is = proc.getInputStream();
						BufferedReader reader = new BufferedReader(new InputStreamReader(is));
						String line = reader.readLine();
						while (line != null)
						{
							line = line.trim();
							String[] parts = line.split("\\s+");
							if (parts.length == 15)
							{
								try
								{
									if (!("free".equals(parts[2]) || "procs".equals(parts[0])))
									{
										long memfree = Long.parseLong(parts[2]);
										long cpuidle = Long.parseLong(parts[12]);
										_memFreeTotal += memfree;
										_cpuIdleTotal += cpuidle;
										_vmstatSamplesCount++;

										if (cpuidle < _cpuIdleMin)
										{
											_cpuIdleMin = cpuidle;
										}
										if (cpuidle > _cpuIdleMax)
										{
											_cpuIdleMax = cpuidle;
										}
										long deltaInSeconds = (System.currentTimeMillis() - _lastSendTime) / 1000;
										if (deltaInSeconds > getPrefs().getInt(Param.STATUS_INTERVAL))
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
								if (parts.length != 4)
								{
									Log.e(TAG, "Expected number of columns 4 or 15 got " + parts.length);
								}
							}
							line = reader.readLine();
						}
						Files.closeQuietly(is, TAG);
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
		long cpuMean = _cpuIdleTotal / _vmstatSamplesCount;
		long memMean = _memFreeTotal / _vmstatSamplesCount;

		long sendPeriod = (System.currentTimeMillis() - _lastSendTime) / 1000;
		Log.i(TAG, "Send period = " + sendPeriod);
		double rcvdBytesPerSec = (TrafficStats.getTotalRxBytes() - _bytesRcvd) / (double) sendPeriod;
		double sntBytesPerSec = (TrafficStats.getTotalTxBytes() - _bytesSent) / (double) sendPeriod;

		bundle.putString(OnStatusExtra.cpuidle.name(), String.valueOf(cpuMean));
		bundle.putString(OnStatusExtra.memfree.name(), String.valueOf(memMean));
		bundle.putString(OnStatusExtra.uplink.name(), String.valueOf(sntBytesPerSec));
		bundle.putString(OnStatusExtra.downlink.name(), String.valueOf(rcvdBytesPerSec));

		bundle.putString(OnStatusExtra.hddextfree.name(), String.valueOf(getHddFreeMemory(HDD_MEM_TYPE.hdd_int)));
		bundle.putString(OnStatusExtra.hddintfree.name(), String.valueOf(getHddFreeMemory(HDD_MEM_TYPE.hdd_ext)));

		bundle.putString(OnStatusExtra.network.name(), getNetwork());

		for (String paramName : _fieldGetters.keySet())
		{
			IStatusFieldGetter getter = _fieldGetters.get(paramName);
			bundle.putString(paramName, getter.getStatusField());
			Log.i(TAG, paramName + " = " + getter.getStatusField());
		}
		getEventMessenger().trigger(ON_STATUS, bundle);

		reset();
	}

	private void reset()
	{
		_cpuIdleMin = Long.MAX_VALUE;
		_cpuIdleMax = Long.MIN_VALUE;
		_memFreeTotal = 0;
		_cpuIdleTotal = 0;
		_vmstatSamplesCount = 0;
		_lastSendTime = System.currentTimeMillis();
		_bytesRcvd = TrafficStats.getTotalRxBytes();
		_bytesSent = TrafficStats.getTotalTxBytes();
	}

	/**
	 * Gets device attribute
	 *
	 * @param deviceAttribute
	 *            the name of the device attribute
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

	private long getHddFreeMemory(HDD_MEM_TYPE hddType)
	{

		StatFs statFs = null;
		if (HDD_MEM_TYPE.hdd_int == hddType)
		{
			statFs = new StatFs(android.os.Environment.getDataDirectory().getPath());
		}
		else
		{
			statFs = new StatFs(android.os.Environment.getExternalStorageDirectory().getPath());
		}

		long availableBlocks = statFs.getAvailableBlocks();
		long blockSize = statFs.getBlockSize();
		long freeBytes = ((availableBlocks * blockSize) / (long) Math.pow(KB, getPrefs().getInt(Param.HDD_UNIT)));

		return freeBytes;

	}

	private String getNetwork()
	{
		String type = "";
		ConnectivityManager connMgr = (ConnectivityManager) Environment.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if ((networkInfo != null) && networkInfo.isConnected())
		{
			type = networkInfo.getTypeName();
		}
		return type;
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

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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.StatFs;
import android.os.SystemClock;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.FeatureState;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;
import com.aviq.tv.android.sdk.core.state.StateManager;
import com.aviq.tv.android.sdk.utils.Files;
import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Defines device parameters
 */
@Author("elmira")
public class FeatureDevice extends FeatureComponent
{
	public static final String TAG = FeatureDevice.class.getSimpleName();
	public static final int ON_STATUS = EventMessenger.ID("ON_STATUS");
	private static int KB = 1024;
	private static String CMD_STAT = "vmstat -n 1 -d %d";
	private static String CMD_LOGCAT = "logcat -v time -t 6000 -d";

	public enum OnStatusExtra
	{
		cpuidle, uplink, downlink, memfree, hddfree, network, section
	}

	public enum DeviceAttribute
	{
		PACKAGE, CUSTOMER, BRAND, BUILD, VERSION, MAC, UPTIME, REALTIME
	}

	public enum StartReason
	{
		NORMAL, SUICIDE, UNKONWN
	}

	public static interface IStatusFieldGetter
	{
		Object getStatusField();
	}

	public static enum UserParam
	{
		SUICIDE_REASON
	}

	public static enum Param
	{
		/**
		 * Application package
		 */
		PACKAGE(""),

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
		MAC(""),

		/**
		 * File with MAC address
		 */
		MAC_ADDRESS_FILE("/sys/class/net/eth0/address"),

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
		HDD_UNIT(1);

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
	private String _deviceMac;
	protected long _statusInterval;
	private StartReason _startReason = StartReason.UNKONWN;
	private String _suicideReason;
	private final HashMap<String, IStatusFieldGetter> _fieldGetters = new HashMap<String, IStatusFieldGetter>();

	public FeatureDevice() throws FeatureNotFoundException
	{
		Environment.getInstance().registerReceiver(new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				Log.i(TAG, ".onReceive: action: " + intent.getAction());
				_startReason = StartReason.NORMAL;
				Environment.getInstance().unregisterReceiver(this);
			}
		}, new IntentFilter(Intent.ACTION_BOOT_COMPLETED));
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

		Prefs userPrefs = Environment.getInstance().getUserPrefs();
		if (userPrefs.has(UserParam.SUICIDE_REASON))
		{
			_startReason = StartReason.SUICIDE;
			_suicideReason = userPrefs.getString(UserParam.SUICIDE_REASON);
			userPrefs.remove(UserParam.SUICIDE_REASON);
		}

		resetStats();
		long vmStatDelay = getPrefs().getInt(Param.VMSTAT_DELAY);
		_statusInterval = getPrefs().getInt(Param.STATUS_INTERVAL);
		_vmCmd = String.format(CMD_STAT, vmStatDelay);

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
										if (deltaInSeconds > _statusInterval)
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
			case PACKAGE:
				return Environment.getInstance().getPackageName();

			case CUSTOMER:
				return getPrefs().getString(Param.CUSTOMER);

			case BRAND:
				return getPrefs().getString(Param.BRAND);

			case VERSION:
				return Environment.getInstance().getBuildVersion();

			case BUILD:
				return getPrefs().getString(Param.BUILD);

			case MAC:
				_deviceMac = getPrefs().getString(Param.MAC);
				if (_deviceMac.length() == 0)
					try
					{
						_deviceMac = readMacAddress();
					}
					catch (FileNotFoundException e)
					{
						Log.e(TAG, e.getMessage(), e);
					}
				return _deviceMac;

			case REALTIME:
				return String.valueOf(SystemClock.elapsedRealtime() / 1000);

			case UPTIME:
				return String.valueOf(SystemClock.uptimeMillis() / 1000);
		}
		return null;
	}

	/**
	 * Provide field status accessing interface
	 *
	 * @param fieldName
	 *            the name of the field
	 * @param getter
	 *            IStatusFieldGetter callback interface
	 */
	public void addStatusField(String fieldName, IStatusFieldGetter getter)
	{
		_fieldGetters.put(fieldName, getter);
	}

	/**
	 * Kills this process eventually causing auto-restart
	 */
	public void suicide(String reason)
	{
		Log.i(TAG, ".suicide: reason = " + reason);
		Environment.getInstance().getUserPrefs().put(UserParam.SUICIDE_REASON, reason);
		Environment.getInstance().finish();
	}

	/**
	 * @return the reason for device start
	 * @see StartReason
	 */
	public StartReason getStartReason()
	{
		return _startReason;
	}

	/**
	 * @return the reason for application being suicide
	 */
	public String getSuicideReason()
	{
		return _suicideReason;
	}

	/**
	 * @return logcat InputStream
	 */
	public InputStream getLogcatInputStream() throws IOException
	{
		Process process = Runtime.getRuntime().exec(CMD_LOGCAT);
		return process.getInputStream();
	}

	private void sendStatus()
	{
		long cpuMean = _cpuIdleTotal / _vmstatSamplesCount;
		long memMean = _memFreeTotal / _vmstatSamplesCount;

		long sendPeriod = (System.currentTimeMillis() - _lastSendTime) / 1000;
		Log.i(TAG, "Send period = " + sendPeriod);
		long rcvdBytesPerSec = (TrafficStats.getTotalRxBytes() - _bytesRcvd) / sendPeriod;
		long sntBytesPerSec = (TrafficStats.getTotalTxBytes() - _bytesSent) / sendPeriod;

		Bundle bundle = new Bundle();
		bundle.putLong(OnStatusExtra.cpuidle.name(), cpuMean);
		bundle.putLong(OnStatusExtra.memfree.name(), memMean);
		bundle.putLong(OnStatusExtra.uplink.name(), sntBytesPerSec);
		bundle.putLong(OnStatusExtra.downlink.name(), rcvdBytesPerSec);
		bundle.putLong(OnStatusExtra.hddfree.name(), getHddFreeMemory());
		bundle.putString(OnStatusExtra.network.name(), getNetwork());

		// Do NULL checks as this method is called from different threads.
		Environment environment = Environment.getInstance();
		StateManager stateManager = null;
		if (environment != null)
			stateManager = environment.getStateManager();
		FeatureState mainState = null;
		if (stateManager != null)
			mainState = (FeatureState) stateManager.getMainState();
		if (mainState != null)
			bundle.putString(OnStatusExtra.section.name(), mainState.getStateName().name());

		for (String paramName : _fieldGetters.keySet())
		{
			IStatusFieldGetter getter = _fieldGetters.get(paramName);
			Object fieldValue = getter.getStatusField();
			if (fieldValue != null)
				TextUtils.putBundleObject(bundle, paramName, fieldValue);
			else
				Log.d(TAG, "Param " + paramName + " has null value!");
		}
		getEventMessenger().trigger(ON_STATUS, bundle);

		resetStats();
	}

	private void resetStats()
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

	private long getHddFreeMemory()
	{
		String drive = android.os.Environment.getDataDirectory().getPath();
		StatFs statFs = new StatFs(drive);
		long availableBlocks = statFs.getAvailableBlocks();
		long blockSize = statFs.getBlockSize();
		long freeBytes = ((availableBlocks * blockSize) / (long) Math.pow(KB, getPrefs().getInt(Param.HDD_UNIT)));
		return freeBytes;
	}

	private String getNetwork()
	{
		String type = "UNKNOWN";
		ConnectivityManager connMgr = (ConnectivityManager) Environment.getInstance().getSystemService(
		        Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo != null)
		{
			type = networkInfo.getTypeName().toUpperCase();
		}
		return type;
	}

	private String readMacAddress() throws FileNotFoundException
	{
		FileInputStream fis = new FileInputStream(getPrefs().getString(Param.MAC_ADDRESS_FILE));
		String macAddress = TextUtils.inputStreamToString(fis);
		macAddress = macAddress.substring(0, 17);
		return macAddress.replace(":", "").toUpperCase();
	}
}

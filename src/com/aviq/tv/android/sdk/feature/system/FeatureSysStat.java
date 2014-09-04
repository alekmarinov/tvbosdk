/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureSysStat.java
 * Author:      elmira
 * Date:        03 Sep 2014
 * Description: Feature collecting various system statistics
 */
package com.aviq.tv.android.sdk.feature.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.os.Bundle;
import android.util.Log;

import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.utils.Files;

/**
 * Feature collecting various system statistics
 */
public class FeatureSysStat extends FeatureComponent
{
	public static final String TAG = FeatureSysStat.class.getSimpleName();
	private static final int ON_STATUS = EventMessenger.ID("ON_STATUS");
	private static final String STAT_CMD = "vmstat -d 1";
	private static final String PARAM_CPU_IDLE = "cpuidle";
	private static final String PARAM_FREE_MEM = "freemem";

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.SYSTEM_STAT;
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
					Process proc = Runtime.getRuntime().exec(STAT_CMD);
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
									Bundle bundle = new Bundle();
									bundle.putLong(PARAM_CPU_IDLE, cpuidle);
									bundle.putLong(PARAM_FREE_MEM, freemem);
									Log.i(TAG, " PARAM_CPU_IDLE = " + cpuidle + " PARAM_FREE_MEM = " + freemem);
									getEventMessenger().trigger(ON_STATUS, bundle);
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
}

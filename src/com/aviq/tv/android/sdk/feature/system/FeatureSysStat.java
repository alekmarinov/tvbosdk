package com.aviq.tv.android.sdk.feature.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import android.os.Bundle;
import android.util.Log;

import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;

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
		// TODO Auto-generated method stub
		return FeatureName.Component.SYSTEM_STAT;
	}
	
	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		
		new Thread(new Runnable()
		{
			public void run()
			{
				BufferedReader reader = null;
				try
				{
					Process proc = Runtime.getRuntime().exec(STAT_CMD);
					reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
					String line = reader.readLine();
					while (line != null)
					{
						String[] pieces = line.split(" ");
						long freemem = Long.parseLong(pieces[2]);
						long cpuidle = Long.parseLong(pieces[12]);
						
						Bundle bundle = new Bundle();
						bundle.putLong(PARAM_CPU_IDLE, cpuidle);
						bundle.putLong(PARAM_FREE_MEM, freemem);
						getEventMessenger().trigger(ON_STATUS, bundle);
						line = reader.readLine();
					}
				}
				catch (IOException error)
				{
					Log.e(TAG, error.getMessage());
					if (reader != null)
					{
						try
						{
							reader.close();
						}
						catch (IOException e)
						{
							Log.e(TAG, e.getMessage());
						}
					}
				}
			}
		}).start();
		
		FeatureSysStat.super.initialize(onFeatureInitialized);
		
	}
	
}

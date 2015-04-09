/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureTimeZone.java
 * Author:      alek
 * Date:        14 Apr 2014
 * Description: Manage system timezone
 */

package com.aviq.tv.android.sdk.feature.system;

import java.util.Calendar;
import java.util.TimeZone;

import android.app.AlarmManager;
import android.content.Context;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;

/**
 * Manage system timezone
 */
@Author("alek")
public class FeatureTimeZone extends FeatureComponent
{
	public static final String TAG = FeatureTimeZone.class.getSimpleName();

	public static enum Param
	{
		/**
		 * Current timezone
		 */
		TIMEZONE("GMT");

		Param(String value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.TIMEZONE).put(name(), value);
		}
	}

	private TimeZone _timeZone;

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		_timeZone = TimeZone.getTimeZone(getPrefs().getString(Param.TIMEZONE));

		// Setting time zone from configuration
		AlarmManager alarm = (AlarmManager) Environment.getInstance().getSystemService(Context.ALARM_SERVICE);
		alarm.setTimeZone(_timeZone.getID());
		Log.i(TAG, "Time zone set to " + _timeZone.getID());
		super.initialize(onFeatureInitialized);
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.TIMEZONE;
	}

	/**
	 * @return the timezone configured for this app
	 */
	public TimeZone getTimeZone()
	{
		return _timeZone;
	}

	/**
	 * @return the current time according to this timezone
	 */
	public Calendar getCurrentTime()
	{
		return Calendar.getInstance(_timeZone);
	}

	/**
	 * Converts local time to GMT
	 */
	public Calendar getGMTTime(Calendar localTime)
	{
		Calendar gmt = Calendar.getInstance();
		gmt.setTimeInMillis(localTime.getTimeInMillis() - localTime.getTimeZone().getRawOffset());
		return gmt;
	}

	/**
	 * Converts GMT to local time
	 */
	public Calendar getLocalTime(Calendar gmtTime)
	{
		Calendar local = Calendar.getInstance();
		local.setTimeInMillis(gmtTime.getTimeInMillis() + _timeZone.getRawOffset());
		return local;
	}
}

/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureLanguage.java
 * Author:      alek
 * Date:        28 Dec 2013
 * Description: Component feature language
 */

package com.aviq.tv.android.sdk.feature.language;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;

/**
 * Component feature language
 */
public class FeatureLanguage extends FeatureComponent
{
	public static final String TAG = FeatureLanguage.class.getSimpleName();

	public enum UserParam
	{
		/**
		 * Currently selected language, e.g. EN, DE, FR, IT, etc.
		 */
		LANGUAGE
	}

	public enum Code
	{
		EN, FR, DE
	}

	@Override
    public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		// update application language
		setLanguage(getLanguage());
		super.initialize(onFeatureInitialized);
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.LANGUAGE;
	}

	/**
	 * Set system language
	 *
	 * @param language
	 *            code
	 *            is as EN, DE, FR, IT, etc.
	 */
	public void setLanguage(Code code)
	{
		Locale locale = null;

		switch (code)
		{
			case EN:
				locale = Locale.UK;
			break;
			case FR:
				locale = Locale.FRENCH;
			break;
			case DE:
				locale = Locale.GERMAN;
			break;
			default:
				locale = Locale.UK;
			break;
		}

		Environment.getInstance().getUserPrefs().put(UserParam.LANGUAGE, locale.getLanguage());
		setSystemLanguage(locale);
	}

	/**
	 * Get system language
	 *
	 * @return the current system language e.g. EN, DE, FR, IT, etc.
	 */
	public Code getLanguage()
	{
		Prefs userPrefs = Environment.getInstance().getUserPrefs();
		return userPrefs.has(UserParam.LANGUAGE) ? Code.valueOf(userPrefs.getString(UserParam.LANGUAGE).toUpperCase())
		        : Code.EN;
	}

	public boolean hasLanguage()
	{
		Prefs userPrefs = Environment.getInstance().getUserPrefs();
		return userPrefs.has(UserParam.LANGUAGE);
	}

	private void setSystemLanguage(Locale locale)
	{
		Configuration config = new Configuration();
		config.locale = locale;
		Resources res = Environment.getInstance().getResources();
		res.updateConfiguration(config, res.getDisplayMetrics());
	}

	private void setSystemLanguage2(Locale locale)
	{
		try
		{
			Class<?> amnClass = Class.forName("android.app.ActivityManagerNative");
			Object amn = null;
			Configuration config = null;

			// amn = ActivityManagerNative.getDefault();
			Method methodGetDefault = amnClass.getMethod("getDefault");
			methodGetDefault.setAccessible(true);
			amn = methodGetDefault.invoke(amnClass);

			// config = amn.getConfiguration();
			Method methodGetConfiguration = amnClass.getMethod("getConfiguration");
			methodGetConfiguration.setAccessible(true);
			config = (Configuration) methodGetConfiguration.invoke(amn);

			// config.userSetLocale = true;
			Class<? extends Configuration> configClass = config.getClass();
			Field f = configClass.getField("userSetLocale");
			f.setBoolean(config, true);

			// set the locale to the new value
			config.locale = locale;

			// amn.updateConfiguration(config);
			Method methodUpdateConfiguration = amnClass.getMethod("updateConfiguration", Configuration.class);
			methodUpdateConfiguration.setAccessible(true);
			methodUpdateConfiguration.invoke(amn, config);
		}
		catch (Exception e)
		{
			Log.e(TAG, "Error changing locale: " + e.getMessage(), e);
		}
	}
}

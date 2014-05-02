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

import java.util.Locale;

import android.content.res.Configuration;
import android.content.res.Resources;

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

	public enum Param
	{
		/**
		 * The default language
		 */
		DEFAULT_LANGUAGE("EN");

		Param(String value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.LANGUAGE).put(name(), value);
		}
	}

	public enum Code
	{
		BG, EN, FR, DE
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
		Environment.getInstance().getUserPrefs().put(UserParam.LANGUAGE, code.name());
		setSystemLanguage(getLocale());
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
		        : Code.valueOf(getPrefs().getString(Param.DEFAULT_LANGUAGE));
	}

	public Locale getLocale()
	{
		Locale locale = null;

		switch (getLanguage())
		{
			case BG:
				locale = new Locale("bg_BG");
			break;
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
		return locale;
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
}

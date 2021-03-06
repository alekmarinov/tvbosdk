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
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;

/**
 * Component feature language
 */
@Author("alek")
public class FeatureLanguage extends FeatureComponent
{
	public static final String TAG = FeatureLanguage.class.getSimpleName();

	public enum Code
	{
		BG, EN, FR, DE
	}

	public static enum Param
	{
		/**
		 * language parameter
		 */
		CODE(null);

		Param(String value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.State.TV).put(name(), value);
		}
	}

	private Code _langCode;

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		if (getPrefs().has(Param.CODE))
		{
			setLanguage(Code.valueOf(getPrefs().getString(Param.CODE)));
		}
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
		Log.i(TAG, ".setLanguage: code = " + code);
		_langCode = code;
		setSystemLanguage(getLocale(code));
	}

	/**
	 * Get system language
	 *
	 * @return the current system language e.g. EN, DE, FR, IT, etc.
	 */
	public Code getLanguage()
	{
		if (_langCode != null)
			return _langCode;

		String langCode = Locale.getDefault().getLanguage().toUpperCase();
		Log.d(TAG, ".getLanguage: -> " + langCode);
		return Code.valueOf(langCode);
	}

	private Locale getLocale(Code code)
	{
		Locale locale = Locale.getDefault();

		if (code != null)
		{
			switch (code)
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
			}
		}

		return locale;
	}

	public Locale getLocale()
	{
		return getLocale(_langCode);
	}

	private void setSystemLanguage(Locale locale)
	{
		Configuration config = new Configuration();
		config.locale = locale;
		Resources res = Environment.getInstance().getResources();
		res.updateConfiguration(config, res.getDisplayMetrics());
	}
}

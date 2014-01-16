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

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.LANGUAGE;
	}

	/**
	 * Set system language
	 *
	 * @param language
	 *            is as EN, DE, FR, IT, etc.
	 */
	public void setLanguage(String language)
	{
		Environment.getInstance().getUserPrefs().put(UserParam.LANGUAGE, language);
	}

	/**
	 * Get system language
	 *
	 * @return the current system language e.g. EN, DE, FR, IT, etc.
	 */
	public String getLanguage()
	{
		Prefs userPrefs = Environment.getInstance().getUserPrefs();
		return userPrefs.has(UserParam.LANGUAGE) ? userPrefs.getString(UserParam.LANGUAGE) : null;
	}
}

/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    IFeature.java
 * Author:      alek
 * Date:        1 Dec 2013
 * Description: Feature interface defining one functional element
 */

package com.aviq.tv.android.sdk.core.feature;

import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.EventReceiver;
import com.aviq.tv.android.sdk.core.Prefs;

/**
 * Feature interface defining one functional element
 */
public interface IFeature extends EventReceiver
{
	enum Type
	{
		COMPONENT, SCHEDULER, STATE
	}

	public interface OnFeatureInitialized
	{
		public void onInitialized(FeatureError error);

		public void onInitializeProgress(IFeature feature, float progress);
	}

	/**
	 * Sets Features instance referencing all initialized dependency features
	 * @param features
	 */
	void setDependencyFeatures(Features features);

	/**
	 * Method to be invoked to initialize this feature
	 */
	void initialize(OnFeatureInitialized onFeatureInitialized) throws FeatureError;

	/**
	 * Define the other features this feature is depending on
	 *
	 * @return FeatureSet
	 */
	FeatureSet dependencies();

	/**
	 * @return feature name
	 */
	String getName();

	/**
	 * @return feature type
	 */
	Type getType();

	/**
	 * @return feature preferences
	 */
	Prefs getPrefs();

	/**
	 * @return an event messenger associated with this feature
	 */
	EventMessenger getEventMessenger();
}

/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    IFeatureFactory.java
 * Author:      alek
 * Date:        27 Dec 2013
 * Description: Feature factory interface
 */

package com.aviq.tv.android.sdk.core.feature;

/**
 * Feature factory interface
 */
public interface IFeatureFactory
{
	/**
	 * Creates feature component instance by Feature Name
	 * @param featureId
	 * @return FeatureComponent instance
	 * @throws FeatureNotFoundException
	 */
	public FeatureComponent createComponent(FeatureName.Component featureId) throws FeatureNotFoundException;

	/**
	 * Creates feature scheduler instance by Feature Name
	 * @param featureId
	 * @return FeatureScheduler instance
	 * @throws FeatureNotFoundException
	 */
	public FeatureScheduler createScheduler(FeatureName.Scheduler featureId) throws FeatureNotFoundException;

	/**
	 * Creates feature state instance by Feature Name
	 * @param featureId
	 * @return FeatureState instance
	 * @throws FeatureNotFoundException
	 */
	public FeatureState createState(FeatureName.State featureId) throws FeatureNotFoundException;
}

/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureComponent.java
 * Author:      alek
 * Date:        1 Dec 2013
 * Description: Defines the base class for component feature type
 */

package com.aviq.tv.android.sdk.core.feature;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.ResultCode;

/**
 * Defines the base class for component feature type
 */
public abstract class FeatureComponent implements IFeature
{
	protected Features _feature;
	private FeatureSet _dependencies = new FeatureSet();
	private EventMessenger _eventMessenger = new EventMessenger(getClass().getSimpleName());

	public FeatureComponent()
	{
	}

	/**
	 * Declare this feature depends on the specified component name
	 * @param featureName
	 * @throws FeatureNotFoundException
	 */
    protected final void require(FeatureName.Component featureName) throws FeatureNotFoundException
	{
		_dependencies.Components.add(featureName);
	}

	/**
	 * Declare this feature depends on the specified scheduler name
	 * @param featureName
	 * @throws FeatureNotFoundException
	 */
	protected final void require(FeatureName.Scheduler featureName) throws FeatureNotFoundException
	{
		_dependencies.Schedulers.add(featureName);
	}

	/**
	 * Declare this feature depends on the specified state name
	 * @param featureName
	 * @throws FeatureNotFoundException
	 */
    protected final void require(FeatureName.State featureName) throws FeatureNotFoundException
	{
		_dependencies.States.add(featureName);
	}

	/**
	 * Declare this feature depends on the specified special feature
	 * @param featureName
	 */
    protected final void require(Class<?> featureClass)
	{
		_dependencies.Specials.add(featureClass);
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		if (onFeatureInitialized != null)
			onFeatureInitialized.onInitialized(this, ResultCode.OK);
	}

	@Override
	public final FeatureSet dependencies()
	{
		return _dependencies;
	}

	@Override
	public final void setDependencyFeatures(Features features)
	{
		_feature = features;
	}

	@Override
	public final Type getType()
	{
		return IFeature.Type.COMPONENT;
	}

	@Override
	public final String getName()
	{
		FeatureName.Component name = getComponentName();
		if (FeatureName.Component.SPECIAL.equals(name))
			return getClass().getName();
		else
			return name.toString();
	}

	@Override
	public String toString()
	{
		return getType() + " " + getName();
	}

	@Override
	public final Prefs getPrefs()
	{
		return Environment.getInstance().getFeaturePrefs(getComponentName());
	}

	/**
	 * @return an event messenger associated with this feature
	 */
	@Override
	public final EventMessenger getEventMessenger()
	{
		return _eventMessenger;
	}

	public abstract FeatureName.Component getComponentName();
}

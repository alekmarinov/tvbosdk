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
	protected FeatureSet _dependencies = new FeatureSet();
	protected Feature _feature;
	private EventMessenger _eventMessenger = new EventMessenger();

	public FeatureComponent()
	{
	}

	@Override
	public void initializeDependencies()
	{
		_feature = new Feature();
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		if (onFeatureInitialized != null)
			onFeatureInitialized.onInitialized(this, ResultCode.OK);
	}

	@Override
	public FeatureSet dependencies()
	{
		return _dependencies;
	}

	@Override
	public Type getType()
	{
		return IFeature.Type.COMPONENT;
	}

	@Override
	public String getName()
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
	public Prefs getPrefs()
	{
		return Environment.getInstance().getFeaturePrefs(getComponentName());
	}

	/**
	 * @return an event messenger associated with this feature
	 */
	@Override
	public EventMessenger getEventMessenger()
	{
		return _eventMessenger;
	}

	public abstract FeatureName.Component getComponentName();
}

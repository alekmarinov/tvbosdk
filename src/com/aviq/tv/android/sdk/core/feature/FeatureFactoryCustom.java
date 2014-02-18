/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureFactoryCustom.java
 * Author:      alek
 * Date:        17 Feb 2014
 * Description: Feature factory with custom defined features
 */

package com.aviq.tv.android.sdk.core.feature;

import java.util.HashMap;

import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Scheduler;
import com.aviq.tv.android.sdk.core.feature.FeatureName.State;

/**
 * Feature factory with custom defined features
 */
public class FeatureFactoryCustom implements IFeatureFactory
{
	public static final String TAG = FeatureFactoryCustom.class.getSimpleName();

	private HashMap<Component, IFeature> _components = new HashMap<Component, IFeature>();
	private HashMap<Scheduler, IFeature> _schedulers = new HashMap<Scheduler, IFeature>();
	private HashMap<State, IFeature> _states = new HashMap<State, IFeature>();
	private IFeatureFactory _featureFactory;

	/**
	 * Register feature to be created later by this factory
	 *
	 * @param feature
	 */
	public void registerFeature(IFeature feature)
	{
		IFeature.Type featureType = feature.getType();
		switch (featureType)
		{
			case COMPONENT:
				_components.put(((FeatureComponent) feature).getComponentName(), feature);
			break;
			case SCHEDULER:
				_schedulers.put(((FeatureScheduler) feature).getSchedulerName(), feature);
			break;
			case STATE:
				_states.put(((FeatureState) feature).getStateName(), feature);
			break;
		}
	}

	/**
	 * Set default feature factory to be used for any non registered feature
	 * name
	 *
	 * @param featureFactory
	 */
	public void setDefaultFactory(IFeatureFactory featureFactory)
	{
		_featureFactory = featureFactory;
	}

	@Override
	public FeatureComponent createComponent(Component featureId) throws FeatureNotFoundException
	{
		FeatureComponent component = (FeatureComponent) _components.get(featureId);
		if (component == null)
			if (_featureFactory != null)
				component = _featureFactory.createComponent(featureId);
			else
				throw new FeatureNotFoundException(featureId);
		return component;
	}

	@Override
	public FeatureScheduler createScheduler(Scheduler featureId) throws FeatureNotFoundException
	{
		FeatureScheduler scheduler = (FeatureScheduler) _schedulers.get(featureId);
		if (scheduler == null)
			if (_featureFactory != null)
				scheduler = _featureFactory.createScheduler(featureId);
			else
				throw new FeatureNotFoundException(featureId);
		return scheduler;
	}

	@Override
	public FeatureState createState(State featureId) throws FeatureNotFoundException
	{
		FeatureState state = (FeatureState) _states.get(featureId);
		if (state == null)
			if (_featureFactory != null)
				state = _featureFactory.createState(featureId);
			else
				throw new FeatureNotFoundException(featureId);
		return state;
	}
}

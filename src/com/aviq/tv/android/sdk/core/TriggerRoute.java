/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    TriggerRoute.java
 * Author:      alek
 * Date:        3 Sep 2014
 * Description: Event trigger route holder class
 */

package com.aviq.tv.android.sdk.core;

import android.os.Bundle;

import com.aviq.tv.android.sdk.core.feature.IFeature;

/**
 * Event trigger route holder class
 */
public class TriggerRoute
{
	private int _eventId;
	private IFeature _target;
	private Bundle _params;

	/**
	 * Creates TriggerRoute instance
	 *
	 * @param eventName the name of the event to trigger
	 * @param target the feature target to route the event to
	 */
	public TriggerRoute(int eventId, IFeature target)
	{
		_eventId = eventId;
		_target = target;
	}

	/**
	 * @return the event id
	 */
	public int getEventId()
	{
		return _eventId;
	}

	/**
	 * @return feature target to route the event to
	 */
	public IFeature getTarget()
	{
		return _target;
	}

	/**
	 * Set event params
	 *
	 * @param params Bundle with all event params
	 */
	public void setParams(Bundle params)
	{
		_params = params;
	}

	/**
	 * Get event params
	 *
	 * @return Bundle with all event params
	 */
	public Bundle getParams()
	{
		return _params;
	}
}

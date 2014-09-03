/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    Action.java
 * Author:      alek
 * Date:        3 Sep 2014
 * Description: Describes action triggered on hooked events
 */

package com.aviq.tv.android.sdk.core;

import android.os.Bundle;

import com.aviq.tv.android.sdk.core.feature.IFeature;

/**
 * Describes action triggered on hooked events
 */
public class Action
{
	private String _name;
	private IFeature _target;
	private Bundle _params;

	/**
	 * Creates Action instance
	 *
	 * @param name the name of the action
	 * @param target the feature target
	 */
	public Action(String name, IFeature target)
	{
		_name = name;
		_target = target;
	}

	/**
	 * @return the action name
	 */
	public String getName()
	{
		return _name;
	}

	/**
	 * @return description of the feature target
	 */
	public IFeature getTarget()
	{
		return _target;
	}

	/**
	 * Set action params
	 *
	 * @param params Bundle with all action params
	 */
	public void setParams(Bundle params)
	{
		_params = params;
	}

	/**
	 * Get action params
	 *
	 * @return Bundle with all action params
	 */
	public Bundle getParams()
	{
		return _params;
	}
}

/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureNotFoundException.java
 * Author:      alek
 * Date:        1 Dec 2013
 * Description: Throw when requested feature is not defined
 */

package com.aviq.tv.android.sdk.core.feature;

/**
 * Throw when requested feature is not found
 *
 */
@SuppressWarnings("serial")
public class FeatureNotFoundException extends Exception
{
	public FeatureNotFoundException()
	{
	}

	/**
	 * @param FeatureName.Component
	 */
	public FeatureNotFoundException(FeatureName.Component featureId)
	{
		super("Component feature " + featureId + " is not found");
	}

	/**
	 * @param FeatureName.Scheduler
	 */
	public FeatureNotFoundException(FeatureName.Scheduler featureId)
	{
		super("Scheduler feature " + featureId + " is not found");
	}

	/**
	 * @param FeatureName.State
	 */
	public FeatureNotFoundException(FeatureName.State featureId)
	{
		super("State feature " + featureId + " is not found");
	}

	/**
	 * @param detailMessage
	 */
	public FeatureNotFoundException(String detailMessage)
	{
		super(detailMessage);
	}

	/**
	 * @param throwable
	 */
	public FeatureNotFoundException(Throwable throwable)
	{
		super(throwable);
	}

	/**
	 * @param detailMessage
	 * @param throwable
	 */
	public FeatureNotFoundException(String detailMessage, Throwable throwable)
	{
		super(detailMessage, throwable);
	}
}

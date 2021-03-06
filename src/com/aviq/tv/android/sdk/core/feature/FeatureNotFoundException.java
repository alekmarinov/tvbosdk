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

import com.aviq.tv.android.sdk.core.ResultCode;

/**
 * Throw when requested feature is not found
 *
 */
@SuppressWarnings("serial")
public class FeatureNotFoundException extends FeatureError
{
	public FeatureNotFoundException()
	{
		super(null, ResultCode.FEATURE_NOT_FOUND);
	}

	/**
	 * @param FeatureName.Component
	 */
	public FeatureNotFoundException(FeatureName.Component featureId)
	{
		super(null, ResultCode.FEATURE_NOT_FOUND, "Component feature " + featureId + " is not found");
	}

	/**
	 * @param FeatureName.Scheduler
	 */
	public FeatureNotFoundException(FeatureName.Scheduler featureId)
	{
		super(null, ResultCode.FEATURE_NOT_FOUND, "Scheduler feature " + featureId + " is not found");
	}

	/**
	 * @param FeatureName.State
	 */
	public FeatureNotFoundException(FeatureName.State featureId)
	{
		super(null, ResultCode.FEATURE_NOT_FOUND, "State feature " + featureId + " is not found");
	}

	/**
	 * @param detailMessage
	 */
	public FeatureNotFoundException(String detailMessage)
	{
		super(null, ResultCode.FEATURE_NOT_FOUND, detailMessage);
	}

	/**
	 * @param throwable
	 */
	public FeatureNotFoundException(Throwable throwable)
	{
		super(null, ResultCode.FEATURE_NOT_FOUND, throwable);
	}

	/**
	 * @param detailMessage
	 * @param throwable
	 */
	public FeatureNotFoundException(String detailMessage, Throwable throwable)
	{
		super(null, ResultCode.FEATURE_NOT_FOUND, detailMessage, throwable);
	}
}

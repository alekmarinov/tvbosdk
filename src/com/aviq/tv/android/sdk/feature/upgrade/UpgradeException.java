/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    UpgradeException.java
 * Author:      alek
 * Date:        26 Feb 2014
 * Description: Exception caused by FeatureUpgrade
 */

package com.aviq.tv.android.sdk.feature.upgrade;

/**
 * Exception caused by FeatureUpgrade
 */
public class UpgradeException extends Exception
{
    private static final long serialVersionUID = 1L;

	public UpgradeException()
	{
	}

	/**
	 * @param detailMessage
	 */
	public UpgradeException(String detailMessage)
	{
		super(detailMessage);
	}

	/**
	 * @param throwable
	 */
	public UpgradeException(Throwable throwable)
	{
		super(throwable);
	}

	/**
	 * @param detailMessage
	 * @param throwable
	 */
	public UpgradeException(String detailMessage, Throwable throwable)
	{
		super(detailMessage, throwable);
	}

}

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

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.IFeature;

/**
 * Exception caused by FeatureUpgrade
 */
public class UpgradeException extends FeatureError
{
	private static final long serialVersionUID = 1L;

	public UpgradeException(IFeature feature, Throwable e)
	{
		super(e);
		_feature = feature;
		if (ParserConfigurationException.class.isInstance(e) || SAXException.class.isInstance(e)
		        || NumberFormatException.class.isInstance(e))
			_resCode = ResultCode.PROTOCOL_ERROR;

	}

	public UpgradeException(IFeature feature, int errCode, String detailedMessage)
    {
		super(feature, errCode, detailedMessage);
    }

	public UpgradeException(IFeature feature, int errCode, String detailedMessage, Throwable e)
    {
		super(feature, errCode, detailedMessage, e);
    }
}

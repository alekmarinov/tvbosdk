/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureError.java
 * Author:      alek
 * Date:        22 Sep 2014
 * Description: Encapsulates feature error
 */

package com.aviq.tv.android.sdk.core.feature;

import java.io.IOException;
import java.net.UnknownHostException;

import android.os.Bundle;

import com.android.volley.NoConnectionError;
import com.android.volley.VolleyError;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Encapsulates feature error
 */
public class FeatureError extends Exception
{
	private static final long serialVersionUID = 1L;
	protected IFeature _feature;
	protected int _resCode;
	protected Bundle _resData;

	public static FeatureError OK = OK(null);

	public static FeatureError OK(IFeature feature)
	{
		return new FeatureError(feature, ResultCode.OK);
	}

	/**
	 * @param throwable
	 *            optional exception object
	 */
	public FeatureError(Throwable e)
	{
		this(null, e);
	}

	/**
	 * @param feature
	 *            the feature related to this error
	 * @param throwable
	 *            optional exception object
	 */
	public FeatureError(IFeature feature, Throwable e)
	{
		this(feature, ResultCode.GENERAL_FAILURE, e);

		// attempt to detect error code
		if (FeatureNotFoundException.class.isInstance(e))
		{
			_resCode = ResultCode.FEATURE_NOT_FOUND;
		}
		else if (IOException.class.isInstance(e))
		{
			_resCode = ResultCode.IO_ERROR;
		}
		else if (UnknownHostException.class.isInstance(e) || NoConnectionError.class.isInstance(e))
		{
			_resCode = ResultCode.COMMUNICATION_ERROR;
		}
		else if (VolleyError.class.isInstance(e))
		{
			VolleyError ve = (VolleyError) e;
			if (ve.networkResponse != null)
			{
				_resCode = -ve.networkResponse.statusCode;
				_resData = new Bundle();

				for (String key : ve.networkResponse.headers.keySet())
				{
					String value = ve.networkResponse.headers.get(key);
					_resData.putString(key, value);
				}
			}
		}
	}

	/**
	 * @param feature
	 *            the feature related to this error
	 * @param errCode
	 *            the code of the error
	 */
	public FeatureError(IFeature feature, int errCode)
	{
		this(feature, errCode, ResultCode.text(errCode), null);
	}

	/**
	 * @param feature
	 *            the feature related to this error
	 * @param errCode
	 *            the code of the error
	 * @param errData
	 *            bundle with extra error data
	 */
	public FeatureError(IFeature feature, int errCode, Bundle errData)
	{
		this(feature, errCode, errData, ResultCode.text(errCode), null);
	}

	/**
	 * @param feature
	 *            the feature related to this error
	 * @param errCode
	 *            the code of the error
	 * @param detailMessage
	 *            message describing the error
	 */
	public FeatureError(IFeature feature, int errCode, String detailMessage)
	{
		this(feature, errCode, detailMessage, null);
	}

	/**
	 * @param feature
	 *            the feature related to this error
	 * @param errCode
	 *            the code of the error
	 * @param throwable
	 *            optional exception object
	 */
	public FeatureError(IFeature feature, int errCode, Throwable throwable)
	{
		this(feature, errCode, throwable.getMessage(), throwable);
	}

	/**
	 * @param feature
	 *            the feature related to this error
	 * @param errCode
	 *            the code of the error
	 * @param detailMessage
	 *            message describing the error
	 * @param throwable
	 *            optional exception object
	 */
	public FeatureError(IFeature feature, int errCode, String detailMessage, Throwable throwable)
	{
		this(feature, errCode, null, detailMessage, throwable);
	}

	/**
	 * @param feature
	 *            the feature related to this error
	 * @param errCode
	 *            the code of the error
	 * @param errData
	 *            bundle with extra error data
	 * @param detailMessage
	 *            message describing the error
	 */
	public FeatureError(IFeature feature, int errCode, Bundle errData, String detailMessage)
	{
		this(feature, errCode, errData, detailMessage, null);
	}

	/**
	 * @param feature
	 *            the feature related to this error
	 * @param errCode
	 *            the code of the error
	 * @param errData
	 *            bundle with extra error data
	 * @param detailMessage
	 *            message describing the error
	 * @param throwable
	 *            optional exception object
	 */
	public FeatureError(IFeature feature, int errCode, Bundle errData, String detailMessage, Throwable throwable)
	{
		super(detailMessage, throwable);
		_feature = feature;
		_resCode = errCode;
		_resData = errData;
	}

	/**
	 * @return the feature causing this exception
	 */
	public IFeature getFeature()
	{
		return _feature;
	}

	/**
	 * @param feature the feature causing this exception
	 */
	public void getFeature(IFeature feature)
	{
		_feature = feature;
	}

	/**
	 * @return result code describing the feature failure
	 */
	public int getCode()
	{
		return _resCode;
	}

	/**
	 * @param set result code
	 */
	public void setCode(int code)
	{
		_resCode = code;
	}

	/**
	 * @return extra result data
	 */
	public Bundle getBundle()
	{
		return _resData;
	}

	/**
	 * @param errData Bundle with additional error data
	 */
	public void setBundle(Bundle errData)
	{
		_resData = errData;
	}

	/**
	 * @return true is there is described error or false if the error code is OK
	 */
	public boolean isError()
	{
		return _resCode < 0;
	}

	/**
	 * String representation of this feature error
	 */
	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		if (_feature != null)
		{
			sb.append(_feature.getName()).append(' ');
		}
		sb.append("error ").append(_resCode);
		if (getMessage() != null)
			sb.append(": ").append(getMessage());

		if (_resData != null)
		{
			sb.append(' ').append(TextUtils.implodeBundle(_resData));
		}
		return sb.toString();
	}
}

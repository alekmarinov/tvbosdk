package com.aviq.tv.android.sdk.utils;

import java.io.IOException;

public class HttpException extends IOException
{
	private static final long serialVersionUID = 1L;
	private int _responseCode;
	private int _redirectsCount;

	/**
	 * @param message
	 * @param cause
	 */
	public HttpException(int responseCode)
	{
		super("HTTP Error: " + responseCode);
		this._responseCode = responseCode;
	}

	/**
	 * @return the responseCode
	 */
	public int getResponseCode()
	{
		return _responseCode;
	}

	/**
	 * @param redirectsCount
	 *            sets HTTP redirecting count
	 */
	public void setRedirectsCount(int redirectsCount)
	{
		_redirectsCount = redirectsCount;
	}

	/**
	 * @return redirecting count
	 */
	public int getRedirectsCount()
	{
		return _redirectsCount;
	}

	@Override
	public String getMessage()
	{
		StringBuffer msg = new StringBuffer("HttpException: responseCode = ").append(_responseCode);
		if (_redirectsCount > 0)
			msg.append(", redirectsCount = ").append(_redirectsCount);
		return msg.toString();
	}
}


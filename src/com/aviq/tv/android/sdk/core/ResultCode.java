/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    ResultCode.java
 * Author:      alek
 * Date:        1 Dec 2013
 * Description: Defines various results codes
 */

package com.aviq.tv.android.sdk.core;

/**
 * Defines various results codes
 */
public class ResultCode
{
	public static final int OK = 0;
	public static final int NOT_FOUND_404 = 404;

	/**
	 * Subscription error
	 */
	public static final int SUBSCRIPTION_ERROR = -10;

	/**
	 * I/O error
	 */
	public static final int IO_ERROR = -9;

	/**
	 * Remote communication error
	 */
	public static final int COMMUNICATION_ERROR = -8;

	/**
	 * Required feature is missing
	 */
	public static final int FEATURE_NOT_FOUND = -7;

	/**
	 * Timeout error
	 */
	public static final int TIMEOUT = -6;

	/**
	 * Error which happened while a feature was initializing.
	 */
	public static final int INIT_ERROR = -5;

	/**
	 * Error which should not happens
	 */
	public static final int INTERNAL_ERROR = -4;

	/**
	 * Mismatched protocol error
	 */
	public static final int PROTOCOL_ERROR = -3;

	/**
	 * Requested functionality is not supported
	 */
	public static final int NOT_SUPPORTED = -2;

	/**
	 * Not specified error
	 */
	public static final int GENERAL_FAILURE = -1;

	public static String text(int resCode)
	{
		String txt = "http ";
		switch (resCode)
		{
			case OK:
				txt = "success";
			break;
			case IO_ERROR:
				txt = "i/o error";
			break;
			case COMMUNICATION_ERROR:
				txt = "communication error";
			break;
			case FEATURE_NOT_FOUND:
				txt = "feature not found";
			break;
			case TIMEOUT:
				txt = "timeout error";
			break;
			case INIT_ERROR:
				txt = "init error";
			break;
			case INTERNAL_ERROR:
				txt = "internal error";
			break;
			case PROTOCOL_ERROR:
				txt = "protocol error";
			break;
			case NOT_SUPPORTED:
				txt = "not supported";
			break;
			case GENERAL_FAILURE:
				txt = "general failure";
			break;
		}
		return txt;
	}
}

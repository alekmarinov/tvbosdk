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
}

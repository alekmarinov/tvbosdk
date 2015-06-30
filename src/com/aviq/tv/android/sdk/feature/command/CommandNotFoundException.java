/**
 * Copyright (c) 2007-2015, Intelibo Ltd
 *
 * Project:     TVBOSDK
 * Filename:    CommandNotFoundException.java
 * Author:      Hari
 * Date:        24 June 2015
 * Description: Throw when requested command is not defined
 */

package com.aviq.tv.android.sdk.feature.command;

import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureError;

/**
 * Throw when requested command is not defined
 *
 */
@SuppressWarnings("serial")
public class CommandNotFoundException extends FeatureError
{
	public CommandNotFoundException()
	{
		super(null, ResultCode.NOT_SUPPORTED);
	}

	public CommandNotFoundException(String cmdId)
	{
		super(null, ResultCode.NOT_SUPPORTED, "Command " + cmdId + " is not supported");
	}

	/**
	 * @param detailMessage
	 */
	public CommandNotFoundException(String cmdId, String detailMessage)
	{
		super(null, ResultCode.NOT_SUPPORTED, cmdId + ": " + detailMessage);
	}

	/**
	 * @param throwable
	 */
	public CommandNotFoundException(Throwable throwable)
	{
		super(null, ResultCode.NOT_SUPPORTED, throwable);
	}

	/**
	 * @param detailMessage
	 * @param throwable
	 */
	public CommandNotFoundException(String detailMessage, Throwable throwable)
	{
		super(null, ResultCode.NOT_SUPPORTED, detailMessage, throwable);
	}
}

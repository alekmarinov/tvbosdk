/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTV
 * Filename:    StateException.java
 * Author:      alek
 * Date:        16 Jul 2013
 * Description: Exception class triggered when error occurs inside a State
 */

package com.aviq.tv.android.sdk.core.state;

/**
 * Exception class triggered when error occurs inside a State
 */
@SuppressWarnings("serial")
public class StateException extends Exception
{
	/**
	 * Default constructor
	 */
	public StateException(BaseState stateOwner)
	{
		super();
	}

	/**
	 * Constructor with message
	 *
	 * @param detailMessage
	 *            the exception message
	 */
	public StateException(BaseState stateOwner, String detailMessage)
	{
		super(detailMessage);
	}

	/**
	 * Constructor with throwable
	 *
	 * @param throwable
	 *            the catched throwable when this exception was triggered
	 */
	public StateException(BaseState stateOwner, Throwable throwable)
	{
		super(throwable);
	}

	/**
	 * Constructor with message and throwable
	 *
	 * @param detailMessage the exception message
	 * @param throwable the catched throwable when this exception was triggered
	 */
	public StateException(String detailMessage, Throwable throwable)
	{
		super(detailMessage, throwable);
	}
}

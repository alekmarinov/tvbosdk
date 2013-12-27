/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    Log.java
 * Author:      Zheliazko
 * Date:        16 Jul 2013
 *
 * Description: Logging wrapper
 */

package com.aviq.tv.android.sdk.core;

public class Log
{
	public static final int NONE = -1;
	public static final int VERBOSE = 2;
	public static final int DEBUG = 3;
	public static final int INFO = 4;
	public static final int WARN = 5;
	public static final int ERROR = 6;
	public static final int ASSERT = 7;

	private static int logLevel = NONE;

	private Log()
	{
	}

	public static void setLogLevel(int level)
	{
		logLevel = level;
	}

	public static int v(String tag, String msg)
	{
		return VERBOSE >= logLevel ? android.util.Log.v(tag, msg) : 0;
	}

	public static int v(String tag, String msg, Throwable tr)
	{
		return VERBOSE >= logLevel ? android.util.Log.v(tag, msg, tr) : 0;
	}

	public static int d(String tag, String msg)
	{
		return DEBUG >= logLevel ? android.util.Log.d(tag, msg) : 0;
	}

	public static int d(String tag, String msg, Throwable tr)
	{
		return DEBUG >= logLevel ? android.util.Log.d(tag, msg, tr) : 0;
	}

	public static int i(String tag, String msg)
	{
		return INFO >= logLevel ? android.util.Log.i(tag, msg) : 0;
	}

	public static int i(String tag, String msg, Throwable tr)
	{
		return INFO >= logLevel ? android.util.Log.i(tag, msg, tr) : 0;
	}

	public static int w(String tag, String msg)
	{
		return WARN >= logLevel ? android.util.Log.w(tag, msg) : 0;
	}

	public static int w(String tag, String msg, Throwable tr)
	{
		return WARN >= logLevel ? android.util.Log.w(tag, msg, tr) : 0;
	}

	public static int w(String tag, Throwable tr)
	{
		return WARN >= logLevel ? android.util.Log.w(tag, tr) : 0;
	}

	public static int e(String tag, String msg)
	{
		return ERROR >= logLevel ? android.util.Log.e(tag, msg) : 0;
	}

	public static int e(String tag, String msg, Throwable tr)
	{
		return ERROR >= logLevel ? android.util.Log.e(tag, msg, tr) : 0;
	}

	public static String getStackTraceString(Throwable tr)
	{
		return android.util.Log.getStackTraceString(tr);
	}

	public static int println(int priority, String tag, String msg)
	{
		return logLevel >= priority ? android.util.Log.println(priority, tag, msg) : 0;
	}
}

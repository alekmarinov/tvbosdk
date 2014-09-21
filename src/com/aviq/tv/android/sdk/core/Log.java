/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    Log.java
 * Author:      zhelyazko
 * Date:        16 Jul 2013
 *
 * Description: Logging wrapper
 */

package com.aviq.tv.android.sdk.core;

import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.feature.crashlog.FeatureCrashLog;

public class Log
{
	private static final String TAG = Log.class.getSimpleName();
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
		handleWarnsAndErrors(WARN, tag, msg, null);
		return WARN >= logLevel ? android.util.Log.w(tag, msg) : 0;
	}

	public static int w(String tag, String msg, Throwable tr)
	{
		handleWarnsAndErrors(WARN, tag, msg, tr);
		return WARN >= logLevel ? android.util.Log.w(tag, msg, tr) : 0;
	}

	public static int w(String tag, Throwable tr)
	{
		handleWarnsAndErrors(WARN, tag, tr.getMessage(), tr);
		return WARN >= logLevel ? android.util.Log.w(tag, tr) : 0;
	}

	public static int e(String tag, String msg)
	{
		handleWarnsAndErrors(ERROR, tag, msg, null);
		return ERROR >= logLevel ? android.util.Log.e(tag, msg) : 0;
	}

	public static int e(String tag, String msg, Throwable tr)
	{
		handleWarnsAndErrors(ERROR, tag, msg, tr);
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

	private static void handleWarnsAndErrors(final int level, final String tag, final String msg, final Throwable ex)
	{
		EventMessenger messenger = Environment.getInstance().getEventMessenger();
		if (messenger != null)
		{
			messenger.post(new Runnable()
			{
				@Override
				public void run()
				{
					FeatureCrashLog crashLog = (FeatureCrashLog) Environment.getInstance().getFeatureComponent(
					        FeatureName.Component.CRASHLOG);
					if (crashLog != null)
					{
						if (level == WARN)
							crashLog.alert(tag, msg, ex);
						else if (level == ERROR)
							crashLog.error(tag, msg, ex);
						else
						{
							android.util.Log.w(TAG, "Only warn and errors will handle the crash log");
						}
					}
					else
					{
						android.util.Log.w(TAG, "CRASHLOG is not ready yet");
					}
				}
			});
		}
		else
		{
			android.util.Log.w(TAG, "Environment is not ready yet");
		}
	}
}

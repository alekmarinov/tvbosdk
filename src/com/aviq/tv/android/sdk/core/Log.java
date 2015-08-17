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

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUtils;
import org.apache.commons.collections.buffer.CircularFifoBuffer;

import com.aviq.tv.android.sdk.feature.crashlog.FeatureCrashLog;

public class Log
{
	private static final String TAG = Log.class.getSimpleName();
	public static final int ANY = -1;
	public static final int VERBOSE = 2;
	public static final int DEBUG = 3;
	public static final int INFO = 4;
	public static final int WARN = 5;
	public static final int ERROR = 6;
	public static final int ASSERT = 7;
	private static Buffer _ringBuf = BufferUtils.synchronizedBuffer(new CircularFifoBuffer(10000));
	private static AtomicLong _counter = new AtomicLong(0);
	private static SimpleFormatter _formatter = new SimpleFormatter();
	private static boolean _enableRingBuffer = true;

	private static int _logLevel = VERBOSE;

	private Log()
	{
	}

	public static void setLogLevel(int level)
	{
		_logLevel = level;
	}

	public static int v(String tag, String msg)
	{
		return doLog(VERBOSE, tag, msg, null);
	}

	public static int v(String tag, String msg, Throwable ex)
	{
		return doLog(VERBOSE, tag, msg, ex);
	}

	public static int d(String tag, String msg)
	{
		return doLog(DEBUG, tag, msg, null);
	}

	public static int d(String tag, String msg, Throwable ex)
	{
		return doLog(DEBUG, tag, msg, ex);
	}

	public static int i(String tag, String msg)
	{
		return doLog(INFO, tag, msg, null);
	}

	public static int i(String tag, String msg, Throwable ex)
	{
		return doLog(INFO, tag, msg, ex);
	}

	public static int w(String tag, String msg)
	{
		return doLog(WARN, tag, msg, null);
	}

	public static int w(String tag, String msg, Throwable ex)
	{
		return doLog(WARN, tag, msg, ex);
	}

	public static int e(String tag, String msg)
	{
		return doLog(ERROR, tag, msg, null);
	}

	public static int e(String tag, String msg, Throwable ex)
	{
		return doLog(ERROR, tag, msg, ex);
	}

	@SuppressWarnings("unchecked")
	private static int doLog(int level, String tag, String msg, Throwable ex)
	{
		if (level == WARN || level == ERROR)
			handleWarnsAndErrors(level, tag, msg, ex);

		if (_enableRingBuffer)
		{
			Level logLevel;
			switch (level)
			{
				case VERBOSE:
					logLevel = Level.FINER;
				break;
				case DEBUG:
					logLevel = Level.FINE;
				break;
				case INFO:
					logLevel = Level.INFO;
				break;
				case WARN:
					logLevel = Level.WARNING;
				break;
				case ERROR:
				case ASSERT:
					logLevel = Level.SEVERE;
				break;
				default:
					logLevel = Level.FINEST;
				break;
			}
			LogRecord logRecord = new LogRecord(logLevel, tag + " " + msg);
			logRecord.setThrown(ex);
			logRecord.setThreadID((int) Thread.currentThread().getId());

			String logLine = _formatter.format(logRecord);
			logLine = logLine.replaceFirst(" null", "");
			logLine = logLine.replaceFirst(" null", "");
			logLine = logLine.replaceFirst("\n", "");
			_ringBuf.add(_counter.incrementAndGet() + ": " + logLine);
		}

		if (level >= _logLevel)
		{
			switch (level)
			{
				case DEBUG:
					return ex != null ? android.util.Log.d(tag, msg, ex) : android.util.Log.d(tag, msg);
				case INFO:
					return ex != null ? android.util.Log.i(tag, msg, ex) : android.util.Log.i(tag, msg);
				case WARN:
					return ex != null ? android.util.Log.w(tag, msg, ex) : android.util.Log.w(tag, msg);
				case ERROR:
					return ex != null ? android.util.Log.e(tag, msg, ex) : android.util.Log.e(tag, msg);
				default:
					return ex != null ? android.util.Log.v(tag, msg, ex) : android.util.Log.v(tag, msg);
			}
		}
		return 0;
	}

	public static void enableRingBuffer(boolean enableRingBuffer)
	{
		_enableRingBuffer = enableRingBuffer;
	}

	public static Buffer getRingBuffer()
	{
		return _ringBuf;
	}

	public static String getStackTraceString(Throwable tr)
	{
		return android.util.Log.getStackTraceString(tr);
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
					FeatureCrashLog crashLog = (FeatureCrashLog) Environment.getInstance().getFeature(
					        FeatureCrashLog.class);
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

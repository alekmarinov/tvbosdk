/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureCrashLog.java
 * Author:      alek
 * Date:        18 Sep 2014
 * Description: Feature handling application errors and crashes
 */

package com.aviq.tv.android.sdk.feature.crashlog;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import android.os.Bundle;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureManager;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.IFeature;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;

/**
 * Feature handling application errors and crashes
 */
@Author("alek")
public class FeatureCrashLog extends FeatureComponent implements Thread.UncaughtExceptionHandler
{
	public static final String TAG = FeatureCrashLog.class.getSimpleName();
	public static final int ON_CRASH_ERROR = EventMessenger.ID("ON_CRASH_ERROR");

	public static enum Extras
	{
		SEVERITY, TAG, MESSAGE, TRACEBACK, FEATURE, AUTHOR
	}

	public FeatureCrashLog()
	{
	}

	public enum Severity
	{
		INFO, ALERT, ERROR, FATAL;
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		Thread.currentThread().setUncaughtExceptionHandler(this);
		super.initialize(onFeatureInitialized);
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.CRASHLOG;
	}

	public void logError(Severity severity, String tag, String message, Throwable ex)
	{
		Log.e(TAG, message, ex);

		Bundle bundle = new Bundle();
		bundle.putString(Extras.SEVERITY.name(), severity.name());
		bundle.putString(Extras.TAG.name(), tag);
		bundle.putString(Extras.MESSAGE.name(), message);

		if (ex != null)
		{
			Writer traceBuffer = new StringWriter();
			ex.printStackTrace(new PrintWriter(traceBuffer));
			bundle.putString(Extras.TRACEBACK.name(), traceBuffer.toString());

			FeatureManager featureManager = Environment.getInstance().getFeatureManager();
			for (StackTraceElement ste: ex.getStackTrace())
			{
				String className = ste.getClassName();
				try
                {
	                Class<?> featureClass = Class.forName(className);
	                if (featureManager.isFeature(featureClass))
	                {
	                	// got feature in exception back trace
	                	IFeature feature = featureManager.getFeature(featureClass);
	            		bundle.putString(Extras.FEATURE.name(), feature.getName());
	            		Author author = featureClass.getAnnotation(Author.class);
	            		if (author != null)
	            		{
		            		bundle.putString(Extras.AUTHOR.name(), author.value());
	            		}
	                	break;
	                }
                }
                catch (ClassNotFoundException e)
                {
                	Log.e(TAG, e.getMessage(), e);
                }
                catch (FeatureNotFoundException e)
                {
                	Log.e(TAG, e.getMessage(), e);
                }
			}
		}

		getEventMessenger().trigger(ON_CRASH_ERROR, bundle);
	}

	public void logCrash(String TAG, String message, Throwable ex)
	{

	}

	@Override
	public void uncaughtException(Thread thread, Throwable ex)
	{
		logCrash(TAG, "uncaught exception:" + ex.getMessage(), ex);
	}
}

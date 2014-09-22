/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    ServiceManager.java
 * Author:      alek
 * Date:        15 Oct 2013
 * Description: Controls services starting and result handling
 */

package com.aviq.tv.android.sdk.core.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureError;

/**
 * Controls service starting and result handling
 */
public class ServiceController
{
	private static final String TAG = ServiceController.class.getSimpleName();
	private final Context _context;
	private Handler _handler = new Handler();

	/**
	 * Interface to be implemented by service caller when the service execution
	 * finishes
	 */
	public interface OnResultReceived
	{
		void onReceiveResult(FeatureError error);
	}

	/**
	 * Implements the asynchronous control of IntentService call
	 */
	public class Promise extends ResultReceiver
	{
		private OnResultReceived _then;
		private Intent _intentService;

		/**
		 * Creates the promise object
		 *
		 * @param intentService
		 *            the service this promise is responsible to control
		 * @param handler
		 *            the looper shared by the ResultReceiver and target
		 *            intentService
		 */
		public Promise(Intent intentService, Handler handler)
		{
			super(handler);
			_intentService = intentService;
		}

		/**
		 * Starts the intentService and sets a callback handling the result when
		 * the last finishes execution
		 *
		 * @param then
		 *            the callback handling the result returned by the
		 *            intentService
		 */
		public void then(OnResultReceived then)
		{
			_then = then;
			_context.startService(_intentService);
		}

		/**
		 * Starts the intentService periodically every intervalSecs seconds
		 * delayed with delaySecs seconds and sets a callback handling the
		 * result when the last finishes execution
		 *
		 * @param delaySecs
		 *            the number of seconds before starting the intentService
		 *            periodically
		 * @param intervalSecs
		 *            the time interval in seconds
		 * @param then
		 *            the callback handling the result returned by the
		 *            intentService
		 */
		public void every(int delaySecs, int intervalSecs, OnResultReceived then)
		{
			_then = then;
			PendingIntent pintent = PendingIntent.getService(_context, 0, _intentService,
			        PendingIntent.FLAG_UPDATE_CURRENT);
			AlarmManager alarm = (AlarmManager) _context.getSystemService(Context.ALARM_SERVICE);
			alarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delaySecs * 1000,
			        intervalSecs * 1000, pintent);
		}

		/**
		 * Starts the intentService periodically every intervalSecs seconds
		 * delayed with delaySecs seconds
		 *
		 * @param delaySecs
		 *            the number of seconds before starting the intentService
		 *            periodically
		 * @param intervalSecs
		 *            the time interval in seconds
		 */
		public void every(int delaySecs, int intervalSecs)
		{
			every(delaySecs, intervalSecs, null);
		}

		/**
		 * Starts the intentService periodically every intervalSecs seconds
		 * and sets a callback handling the result when the last finishes
		 * execution
		 *
		 * @param intervalSecs
		 *            the time interval in seconds
		 * @param then
		 *            the callback handling the result returned by the
		 *            intentService
		 */
		public void every(int intervalSecs, OnResultReceived then)
		{
			every(0, intervalSecs, then);
		}

		/**
		 * Starts the intentService periodically every intervalSecs seconds
		 *
		 * @param intervalSecs
		 *            the time interval in seconds
		 */
		public void every(int intervalSecs)
		{
			every(0, intervalSecs, null);
		}

		@Override
		protected void onReceiveResult(int resultCode, Bundle resultData)
		{
			if (_then != null)
			{
				_then.onReceiveResult(new FeatureError(null, resultCode, resultData));
			}
		}
	}

	/**
	 * Initialize ServiceManager instance.
	 *
	 * @param Context
	 *            The owner application context of this ServiceManager
	 */
	public ServiceController(Context context)
	{
		_context = context;
		Log.i(TAG, "ServiceController created");
	}

	/**
	 * Starts an intent service specified by its class
	 *
	 * @param serviceClass
	 *            the class of the IntentService
	 * @param params
	 *            Bundle
	 * @return Promise
	 */
	public Promise startService(Class<?> serviceClass, Bundle params)
	{
		Intent intent = new Intent(_context, serviceClass);
		Promise onServiceComplete = new Promise(intent, _handler);
		intent.putExtra(BaseService.EXTRA_RESULT_RECEIVER, onServiceComplete);
		if (params != null)
			intent.putExtras(params);
		return onServiceComplete;
	}

	/**
	 * Starts an intent service specified by its class
	 *
	 * @param serviceClass
	 *            the class of the IntentService
	 * @return Promise
	 */
	public Promise startService(Class<?> serviceClass)
	{
		return startService(serviceClass, null);
	}
}

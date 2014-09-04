/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureEventCollector.java
 * Author:      zhelyazko
 * Date:        19 Mar 2014
 * Description: Scheduler feature collecting events and sending them to a remote location.
 */

package com.aviq.tv.android.sdk.feature.eventcollector;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.Time;
import android.util.JsonWriter;
import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Scheduler;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.FeatureScheduler;
import com.aviq.tv.android.sdk.core.feature.PriorityFeature;
import com.aviq.tv.android.sdk.feature.internet.UploadService;
import com.aviq.tv.android.sdk.feature.system.FeatureDevice.DeviceAttribute;

/**
 * Scheduler feature collecting events and sending them to a remote location.
 */
@PriorityFeature
public class FeatureEventCollectorBase extends FeatureScheduler
{
	public static final String TAG = FeatureEventCollectorBase.class.getSimpleName();

	public enum Param
	{
		/** Schedule interval. */
		SEND_EVENTS_INTERVAL(60 * 1000),

		/** Report URL */
		EVENTS_SERVER_URL("https://services.aviq.com:30227/upload/logs/"),

		/** Username for report URL */
		EVENTS_SERVER_USERNAME(""),

		/** Password for report URL */
		EVENTS_SERVER_PASSWORD(""),

		/** Report name template */
		REPORT_FILENAME_TEMPLATE("${BUILD}-${CUSTOMER}-${BRAND}-${MAC}-${DATETIME}-${RANDOM}.eventlog"),

		/** Path to the CA certificate relative to the assets folder */
		EVENTS_SERVER_CA_CERT_PATH("");

		Param(int value)
		{
			Environment.getInstance().getFeature(FeatureEventCollectorBase.class).getPrefs().put(name(), value);
		}

		Param(String value)
		{
			Environment.getInstance().getFeature(FeatureEventCollectorBase.class).getPrefs().put(name(), value);
		}
	}

	private List<Bundle> _eventList = Collections.synchronizedList(new ArrayList<Bundle>());

	public FeatureEventCollectorBase() throws FeatureNotFoundException
	{
		require(FeatureName.Component.DEVICE);
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		onSchedule(onFeatureInitialized);
	}

	@Override
	protected void onSchedule(OnFeatureInitialized onFeatureInitialized)
	{
		// process events
		Log.i(TAG, "Processing collected events.");
		processCollectedEvents();

		scheduleDelayed(getPrefs().getInt(Param.SEND_EVENTS_INTERVAL));
		onFeatureInitialized.onInitialized(this, ResultCode.OK);
	}

	protected void processCollectedEvents()
	{
		try
		{
			Log.v(TAG, "Process " + _eventList.size() + " events.");
			String data = parseEventListToString(_eventList);
			_eventList.clear();

			if (data != null && data.length() > 0)
			{
				Environment env = Environment.getInstance();

				String reportName = getReportName();
				Intent uploadService = new Intent(env, UploadService.class);
				uploadService.putExtra(UploadService.EXTRA_CA_CERT_PATH,
				        getPrefs().getString(Param.EVENTS_SERVER_CA_CERT_PATH));
				uploadService.putExtra(UploadService.EXTRA_REPORT_URL,
				        getPrefs().getString(Param.EVENTS_SERVER_URL));
				uploadService.putExtra(UploadService.EXTRA_REPORT_URL_USER,
				        getPrefs().getString(Param.EVENTS_SERVER_USERNAME));
				uploadService.putExtra(UploadService.EXTRA_REPORT_URL_PASS,
				        getPrefs().getString(Param.EVENTS_SERVER_PASSWORD));
				uploadService.putExtra(UploadService.EXTRA_REPORT_NAME, reportName);
				uploadService.putExtra(UploadService.EXTRA_REPORT_DATA, data);
				env.startService(uploadService);
			}
		}
		catch (IOException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
	}

	protected String getReportName()
	{
		Time time = new Time();
		time.set(System.currentTimeMillis());
		String eventDateTime = time.format("%Y.%m.%d_%H.%M.%S");

		Random rnd = new Random();
		int randomNum = rnd.nextInt(1000);

		Bundle substitute = new Bundle();

		substitute.putString("BUILD", _feature.Component.DEVICE.getDeviceAttribute(DeviceAttribute.BUILD));
		substitute.putString("CUSTOMER", _feature.Component.DEVICE.getDeviceAttribute(DeviceAttribute.CUSTOMER));
		substitute.putString("BRAND", _feature.Component.DEVICE.getDeviceAttribute(DeviceAttribute.BRAND));
		substitute.putString("MAC", _feature.Component.DEVICE.getDeviceAttribute(DeviceAttribute.MAC));
		substitute.putString("DATETIME", eventDateTime);
		substitute.putString("RANDOM", String.valueOf(randomNum));
		return getPrefs().getString(Param.REPORT_FILENAME_TEMPLATE, substitute);
	}

	protected void addEvent(Bundle eventParams)
	{
		_eventList.add(eventParams);
	}

	protected void writeEvent(JsonWriter writer, Bundle event) throws IOException
	{
		writer.beginObject();

		// Iterate over the elements of each event
		Set<String> keys = event.keySet();
		for (String key : keys)
		{
			Object o = event.get(key);

			if (o == null)
			{
				writer.name(key).value((String) o);
			}
			else if (o.getClass().getName().contentEquals("java.lang.Byte"))
			{
				writer.name(key).value((Byte) o);
			}
			else if (o.getClass().getName().contentEquals("java.lang.Short"))
			{
				writer.name(key).value((Short) o);
			}
			else if (o.getClass().getName().contentEquals("java.lang.Integer"))
			{
				writer.name(key).value((Integer) o);
			}
			else if (o.getClass().getName().contentEquals("java.lang.Long"))
			{
				writer.name(key).value((Long) o);
			}
			else if (o.getClass().getName().contentEquals("java.lang.Boolean"))
			{
				writer.name(key).value((Boolean) o);
			}
			else if (o.getClass().getName().contentEquals("java.lang.Double"))
			{
				writer.name(key).value((Double) o);
			}
			else if (o.getClass().getName().contentEquals("java.lang.Float"))
			{
				writer.name(key).value((Float) o);
			}
			else if (o.getClass().getName().contentEquals("java.lang.String"))
			{
				writer.name(key).value((String) o);
			}
			else if (o.getClass().getName().contentEquals("java.lang.Character"))
			{
				writer.name(key).value(Character.toString((Character) o));
			}
			else if (o.getClass().getName().contentEquals("android.os.Bundle"))
			{
				// A complex object encountered, recurse through it
				writer.name(key);
				writeEvent(writer, (Bundle) o);
			}
			else
			{
				// etc.
				Log.w(TAG, "Unknown: " + o + " for type: " + o.getClass() + ", toString() = " + o.toString());
			}
		}
		writer.endObject();
	}

	private String parseEventListToString(List<Bundle> eventList) throws IOException
	{
		StringBuffer data = new StringBuffer();

		for (Bundle event : eventList)
		{
			StringWriter out = new StringWriter();
			JsonWriter writer = new JsonWriter(out);

			writeEvent(writer, event);

			StringBuffer line = out.getBuffer();
			data.append(line).append('\n');

			writer.close();
			out.close();
		}
		return data.toString();
	}

	@Override
	public Scheduler getSchedulerName()
	{
		return FeatureName.Scheduler.EVENT_COLLECTOR;
	}
}

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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;

import android.os.Bundle;
import android.text.format.Time;
import android.util.JsonWriter;
import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Scheduler;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.FeatureScheduler;
import com.aviq.tv.android.sdk.core.feature.PriorityFeature;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.internet.UploadService;
import com.aviq.tv.android.sdk.feature.system.FeatureDevice;
import com.aviq.tv.android.sdk.feature.system.FeatureDevice.DeviceAttribute;

/**
 * Scheduler feature collecting events and sending them to a remote location.
 */
@PriorityFeature
public class FeatureEventCollectorBase extends FeatureScheduler
{
	public static final String TAG = FeatureEventCollectorBase.class.getSimpleName();
	public static final int ON_TRACK = EventMessenger.ID("ON_TRACK");

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

	/**
	 * Extra params associated with ON_TRACK event
	 */
	public enum OnTrackExtras
	{
		EVENT, SOURCE, TAG
	}

	/**
	 * keep all collected events until uploading to the tracking server
	 */
	private List<Bundle> _eventList = Collections.synchronizedList(new ArrayList<Bundle>());

	public FeatureEventCollectorBase() throws FeatureNotFoundException
	{
		require(FeatureName.Component.DEVICE);
		require(FeatureName.Scheduler.INTERNET);
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		getEventMessenger().register(this, ON_TRACK);
		onSchedule(onFeatureInitialized);
	}

	@Override
	public Scheduler getSchedulerName()
	{
		return FeatureName.Scheduler.EVENT_COLLECTOR;
	}

	@Override
	public void onEvent(int msgId, Bundle bundle)
	{
		super.onEvent(msgId, bundle);
		if (ON_TRACK == msgId)
		{
			String eventName = bundle.getString(OnTrackExtras.EVENT.name().toLowerCase());
			String eventSource = bundle.getString(OnTrackExtras.SOURCE.name().toLowerCase());
			Bundle eventParams = new Bundle();
			eventParams.putBundle("device", createDeviceAttributes());
			eventParams.putBundle("event", createEventAttributes(eventName, eventSource));

			Bundle customAttributes = new Bundle();
			for (String key : bundle.keySet())
			{
				if (OnTrackExtras.valueOf(key.toUpperCase()) == null)
				{
					String value = bundle.getString(key);
					customAttributes.putString(key, value);
				}
			}
			eventParams.putBundle(bundle.getString(OnTrackExtras.TAG.name().toLowerCase()), customAttributes);
			addEvent(eventParams);
		}
	}

	/**
	 * Create device attributes to attach to each event
	 *
	 * @return Bundle
	 */
	protected Bundle createDeviceAttributes()
	{
		Bundle deviceParams = new Bundle();
		for (DeviceAttribute deviceAttribute : FeatureDevice.DeviceAttribute.values())
		{
			String attrValue = _feature.Component.DEVICE.getDeviceAttribute(deviceAttribute);
			deviceParams.putString(deviceAttribute.name().toLowerCase(), attrValue);
		}
		return deviceParams;
	}

	/**
	 * Create event attributes to attach to each event
	 *
	 * @return Bundle
	 */
	protected Bundle createEventAttributes(String name, String source)
	{
		TimeZone tzUTC = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ", Locale.US);
		df.setTimeZone(tzUTC);
		String timestamp = df.format(System.currentTimeMillis());
		Bundle eventParams = new Bundle();
		eventParams.putString("name", name);
		eventParams.putString("source", source);
		eventParams.putString("timestamp", timestamp);
		return eventParams;
	}

	@Override
	protected void onSchedule(OnFeatureInitialized onFeatureInitialized)
	{
		// process events
		processCollectedEvents();
		scheduleDelayed(getPrefs().getInt(Param.SEND_EVENTS_INTERVAL));
		onFeatureInitialized.onInitialized(this, ResultCode.OK);
	}

	/**
	 * Upload all collected events to the tracking server
	 */
	protected void processCollectedEvents()
	{
		Log.v(TAG, "Process " + _eventList.size() + " collected events");
		String data = parseEventListToString(_eventList);
		_eventList.clear();

		if (data != null && data.length() > 0)
		{
			String reportName = getReportName();
			String url = getPrefs().getString(Param.EVENTS_SERVER_URL);
			if (url.indexOf(url.length() - 1) != '/')
				url += '/';
			url += reportName;
			Bundle uploadParams = new Bundle();
			uploadParams.putString(UploadService.Extras.URL.name(), url);
			uploadParams.putString(UploadService.Extras.CA_CERT_PATH.name(),
			        getPrefs().getString(Param.EVENTS_SERVER_CA_CERT_PATH));
			uploadParams.putString(UploadService.Extras.USERNAME.name(),
			        getPrefs().getString(Param.EVENTS_SERVER_USERNAME));
			uploadParams.putString(UploadService.Extras.PASSWORD.name(),
			        getPrefs().getString(Param.EVENTS_SERVER_PASSWORD));
			uploadParams.putString(UploadService.Extras.BUFFER.name(), data);

			_feature.Scheduler.INTERNET.uploadFile(uploadParams, new OnResultReceived()
			{
				@Override
				public void onReceiveResult(int resultCode, Bundle resultData)
				{
					Log.i(TAG, ".uploadFile:onReceiveResult: resultCode = " + resultCode);
				}
			});
		}
	}

	/**
	 * Gets tracking report file name
	 *
	 * @return file name to upload on the server
	 */
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

	/**
	 * Adds event to collection
	 */
	protected void addEvent(Bundle eventParams)
	{
		_eventList.add(eventParams);
	}

	/**
	 * Writes bundle to JsonWriter
	 *
	 * @param writer
	 *            a JsonWriter
	 * @param bundle
	 *            bundle to write
	 * @throws IOException
	 */
	private void writeBundle(JsonWriter writer, Bundle bundle) throws IOException
	{
		writer.beginObject();

		// Iterate over the elements of each event
		Set<String> keys = bundle.keySet();
		for (String key : keys)
		{
			Object o = bundle.get(key);

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
				writeBundle(writer, (Bundle) o);
			}
			else
			{
				// etc.
				Log.w(TAG, "Unknown: " + o + " for type: " + o.getClass() + ", toString() = " + o.toString());
			}
		}
		writer.endObject();
	}

	/**
	 * Creates string buffer from events list
	 * Concatenate all events represented as JSON strings and separated with new
	 * line
	 *
	 * @param eventList
	 * @return String with all events
	 */
	private String parseEventListToString(List<Bundle> eventList)
	{
		StringBuffer data = new StringBuffer();
		try
		{
			for (Bundle event : eventList)
			{
				StringWriter out = new StringWriter();
				JsonWriter writer = new JsonWriter(out);

				writeBundle(writer, event);

				StringBuffer line = out.getBuffer();
				data.append(line).append('\n');

				writer.close();
				out.close();
			}
		}
		catch (IOException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}

		return data.toString();
	}
}

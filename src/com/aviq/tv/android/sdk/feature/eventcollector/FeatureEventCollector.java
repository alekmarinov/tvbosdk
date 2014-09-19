/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureEventCollector.java
 * Author:      zhelyazko
 * Date:        19 Mar 2014
 * Description: Scheduler feature sending collected events periodically to remote event tracking system
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
import com.aviq.tv.android.sdk.core.feature.annotation.Priority;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.FeatureScheduler;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.internet.FeatureInternet;
import com.aviq.tv.android.sdk.feature.internet.FeatureInternet.GeoIpExtras;
import com.aviq.tv.android.sdk.feature.internet.UploadService;
import com.aviq.tv.android.sdk.feature.system.FeatureDevice;
import com.aviq.tv.android.sdk.feature.system.FeatureDevice.DeviceAttribute;
import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Scheduler feature sending collected events periodically to remote event tracking system
 */
@Priority
public class FeatureEventCollector extends FeatureScheduler
{
	public static final String TAG = FeatureEventCollector.class.getSimpleName();
	public static final int ON_TRACK = EventMessenger.ID("ON_TRACK");

	public enum Param
	{
		/** Schedule interval. */
		SEND_EVENTS_INTERVAL(16 * 1000),

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
			Environment.getInstance().getFeature(FeatureEventCollector.class).getPrefs().put(name(), value);
		}

		Param(String value)
		{
			Environment.getInstance().getFeature(FeatureEventCollector.class).getPrefs().put(name(), value);
		}
	}

	/**
	 * Extra params associated with ON_TRACK event
	 */
	public enum OnTrackExtra
	{
		EVENT, SOURCE
	}

	/**
	 * keep all collected events until uploading to the tracking server
	 */
	private List<Bundle> _eventList = Collections.synchronizedList(new ArrayList<Bundle>());
	private Bundle _geoIp = new Bundle();

	public FeatureEventCollector() throws FeatureNotFoundException
	{
		require(FeatureName.Component.DEVICE);
		require(FeatureName.Scheduler.INTERNET);
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		getEventMessenger().register(this, ON_TRACK);
		_feature.Scheduler.INTERNET.getEventMessenger().register(this, FeatureInternet.ON_CONNECTED);
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
			String eventName = bundle.getString(OnTrackExtra.EVENT.name().toLowerCase());
			if (eventName == null)
			{
				Log.e(TAG, "attribute `" + OnTrackExtra.EVENT.name().toLowerCase()
				        + "' is required but missing in event " + TextUtils.implodeBundle(bundle));
				return;
			}
			String eventSource = bundle.getString(OnTrackExtra.SOURCE.name().toLowerCase());
			if (eventSource == null)
			{
				Log.e(TAG, "attribute `" + OnTrackExtra.SOURCE.name().toLowerCase()
				        + "' is required but missing in event " + TextUtils.implodeBundle(bundle));
				return;
			}
			Bundle eventParams = new Bundle();
			eventParams.putBundle("device", createDeviceAttributes());
			eventParams.putBundle("geoip", createGeoIPAttributes());
			eventParams.putBundle("event", createEventAttributes(eventName, eventSource));

			Bundle customAttributes = new Bundle();
			for (String key : bundle.keySet())
			{
				// verify if the custom param is not one of the OnTrackExtra
				boolean skipParam = false;
				for (OnTrackExtra extra: OnTrackExtra.values())
				{
					if (extra.name().equals(key.toUpperCase()))
					{
						skipParam = true;
						break;
					}
				}

				if (!skipParam)
				{
					Object value = bundle.get(key);
					TextUtils.putBundleObject(customAttributes, key, value);
				}
			}
			eventParams.putBundle(eventName, customAttributes);
			addEvent(eventParams);
		}
		else if (FeatureInternet.ON_CONNECTED == msgId)
		{
			for (GeoIpExtras geoip: FeatureInternet.GeoIpExtras.values())
			{
				if ( bundle.containsKey(geoip.name()))
					TextUtils.putBundleObject(_geoIp, geoip.name().toLowerCase(), bundle.get(geoip.name()));
			}
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
	 * Create geoip attributes to attach to each event
	 *
	 * @return Bundle
	 */
	protected Bundle createGeoIPAttributes()
	{
		return _geoIp;
	}
	/**
	 * Create event attributes to attach to each event
	 *
	 * @return Bundle
	 */
	protected Bundle createEventAttributes(String name, String source)
	{
		TimeZone tzUTC = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
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

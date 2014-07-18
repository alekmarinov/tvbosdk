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
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import android.util.SparseArray;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.EventReceiver;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Scheduler;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.FeatureScheduler;
import com.aviq.tv.android.sdk.core.feature.PriorityFeature;
import com.aviq.tv.android.sdk.feature.internet.FeatureInternet;
import com.aviq.tv.android.sdk.feature.internet.FeatureInternet.ResultExtras;
import com.aviq.tv.android.sdk.feature.internet.UploadService;
import com.aviq.tv.android.sdk.feature.network.FeatureEthernet;
import com.aviq.tv.android.sdk.feature.player.FeaturePlayer;
import com.aviq.tv.android.sdk.feature.register.FeatureRegister;
import com.aviq.tv.android.sdk.feature.upgrade.FeatureUpgrade;
import com.aviq.tv.android.sdk.player.BasePlayer;
import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Scheduler feature collecting events and sending them to a remote location.
 * Requirements to use it:
 * 1. Project has to include the relative path to the root certificate on the
 * server if it is a self-generated certificate.
 * 2. Project's AndroidManifest.xml has to reference FeatureInternet's
 * UploadService class.
 */
@PriorityFeature
public class FeatureEventCollector extends FeatureScheduler
{
	public static final String TAG = FeatureEventCollector.class.getSimpleName();

	protected static final String REPORT_PREFIX = "aviqv2-";
	protected static final String NO_VALUE = "n/a";

	public enum Param
	{
		/** Initial delay */
		EVENT_COLLECTOR_INITIAL_DELAY(1 * 1000),

		/** Schedule interval. */
		EVENT_COLLECTOR_SCHEDULE_INTERVAL(4 * 1000),

		/** Report URL */
		EVENT_COLLECTOR_REPORT_URL("https://services.aviq.com:30227/upload/logs/"),

		/** Username for report URL */
		EVENT_COLLECTOR_REPORT_URL_USERNAME(""),

		/** Password for report URL */
		EVENT_COLLECTOR_REPORT_URL_PASSWORD(""),

		/** Report name template based on String.format parameters */
		EVENT_COLLECTOR_REPORT_NAME_TEMPLATE("${BUILD}-${CUSTOMER}-${BRAND}-${MAC}-${DATETIME}-${RANDOM}.eventlog"),

		/** Path to the CA certificate relative to the assets folder */
		EVENT_COLLECTOR_CA_CERT_PATH(""),

		/**
		 * Some events may come too fast, e.g. events from some progress
		 * tracking like a download. Such events are marked in the code.
		 * Setting this value will effectively ignore events of the same
		 * type that come too often, i.e. if the last event was X milliseconds
		 * from the current or less, then ignore the current event.
		 * Note: events must be explicitly marked in order for this value
		 * to matter.
		 */
		EVENT_COLLECTOR_ANTI_FLOOD_INTERVAL(4000),

		EVENT_COLLECTOR_CUSTOMER(""),
		EVENT_COLLECTOR_BRAND("");

		Param(int value)
		{
			try
			{
				Environment.getInstance().getFeatureManager().getFeature(FeatureEventCollector.class).getPrefs()
				        .put(name(), value);
			}
			catch (FeatureNotFoundException e)
			{
			}
		}

		Param(String value)
		{
			try
			{
				Environment.getInstance().getFeatureManager().getFeature(FeatureEventCollector.class).getPrefs()
				        .put(name(), value);
			}
			catch (FeatureNotFoundException e)
			{
			}
		}
	}

	public static enum Severity
	{
		INFO, ALERT, ERROR, FATAL;
	}

	private FeatureInternet _featureInternet;
	private FeatureRegister _featureRegister;
	private FeatureEthernet _featureEthernet;
	private List<Bundle> _eventList = Collections.synchronizedList(new ArrayList<Bundle>());
	private SparseArray<Long> _antiFloodEvents = new SparseArray<Long>();
	private int _antiFloodInterval;

	public FeatureEventCollector() throws FeatureNotFoundException
	{
		require(FeatureName.Component.REGISTER);
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		try
		{
			Environment env = Environment.getInstance();

			// Load required preferences
			_antiFloodInterval = getPrefs().getInt(Param.EVENT_COLLECTOR_ANTI_FLOOD_INTERVAL);

			// Load required features
			_featureInternet = (FeatureInternet) env.getFeatureScheduler(FeatureName.Scheduler.INTERNET);
			_featureRegister = (FeatureRegister) env.getFeatureComponent(FeatureName.Component.REGISTER);
			_featureEthernet = (FeatureEthernet) env.getFeatureComponent(FeatureName.Component.ETHERNET);

			// Internet
			_featureInternet = (FeatureInternet) env.getFeatureScheduler(FeatureName.Scheduler.INTERNET);
			_featureInternet.getEventMessenger().register(this, FeatureInternet.ON_CONNECTED);

			onSchedule(onFeatureInitialized);
			super.initialize(onFeatureInitialized);
		}
		catch (Exception /*FeatureNotFoundException*/ e)
		{
			Log.e(TAG, e.getMessage(), e);
			onFeatureInitialized.onInitialized(this, ResultCode.GENERAL_FAILURE);
		}
	}

	@Override
	protected void onSchedule(OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, "Processing collected events.");

		// FIXME Consider converting this Scheduler to a Component. Currently
		// events are send when FeatureInternet sends "ON_CONNECTED" events.
		// However, if converted to a Component, it won't be possible to control
		// the sending of events.
		// processCollectedEvents();

		// Schedule another pass
		int delay = getPrefs().has(Param.EVENT_COLLECTOR_SCHEDULE_INTERVAL) ? getPrefs().getInt(
		        Param.EVENT_COLLECTOR_SCHEDULE_INTERVAL) : 0;
		scheduleDelayed(delay);
	}

	@Override
	public Scheduler getSchedulerName()
	{
		return FeatureName.Scheduler.SPECIAL;
	}

	@Override
	public void onEvent(int msgId, Bundle bundle)
	{
		super.onEvent(msgId, bundle);
		Log.i(TAG, ".onEvent: " + EventMessenger.idName(msgId) + TextUtils.implodeBundle(bundle));

		Long lastEventTime = _antiFloodEvents.get(msgId);
		if (lastEventTime != null)
		{
			long now = System.currentTimeMillis();
			if (lastEventTime > 0)
			{
				long delta = now - lastEventTime;
				if (delta <= _antiFloodInterval)
				{
					Log.i(TAG, "Anti-flood protection for event " + msgId + ". Event dropped; time delta = " + delta);
					return;
				}
			}

			// Set the time for the last received event
			_antiFloodEvents.put(msgId, now);
		}

		if (FeatureInternet.ON_CONNECTED == msgId)
		{
			processCollectedEvents();
		}
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

				Time time = new Time();
				time.set(System.currentTimeMillis());
				String eventDateTime = time.format("%Y.%m.%d_%H.%M.%S");

				Random rnd = new Random();
				int randomNum = rnd.nextInt(1000);

				String customer = getPrefs().getString(Param.EVENT_COLLECTOR_CUSTOMER);
				String release = env.getPrefs().getString(Environment.Param.RELEASE);
				String brand = getPrefs().getString(Param.EVENT_COLLECTOR_BRAND);
				String mac = _feature.Component.REGISTER != null ? _feature.Component.REGISTER.getBoxId() : "null";

				String reportName = REPORT_PREFIX + getPrefs().getString(Param.EVENT_COLLECTOR_REPORT_NAME_TEMPLATE);
				reportName = reportName.replace("${BUILD}", release);
				reportName = reportName.replace("${CUSTOMER}", customer);
				reportName = reportName.replace("${BRAND}", brand);
				reportName = reportName.replace("${MAC}", mac);
				reportName = reportName.replace("${DATETIME}", eventDateTime);
				reportName = reportName.replace("${RANDOM}", "" + randomNum);

				Intent uploadService = new Intent(env, UploadService.class);
				uploadService.putExtra(UploadService.EXTRA_CA_CERT_PATH,
				        getPrefs().getString(Param.EVENT_COLLECTOR_CA_CERT_PATH));
				uploadService.putExtra(UploadService.EXTRA_REPORT_URL,
				        getPrefs().getString(Param.EVENT_COLLECTOR_REPORT_URL));
				uploadService.putExtra(UploadService.EXTRA_REPORT_URL_USER,
				        getPrefs().getString(Param.EVENT_COLLECTOR_REPORT_URL_USERNAME));
				uploadService.putExtra(UploadService.EXTRA_REPORT_URL_PASS,
				        getPrefs().getString(Param.EVENT_COLLECTOR_REPORT_URL_PASSWORD));
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

	private Bundle prepareEventObject(Bundle eventBundle)
	{
		Bundle bundle = new Bundle();

		eventBundle.putString(Key.TIMESTAMP, getTimeAsISO(System.currentTimeMillis()));
		bundle.putBundle(Key.EVENT, eventBundle);

		Bundle deviceBundle = new Bundle();

		String mac = _featureRegister != null ? _featureRegister.getBoxId() : NO_VALUE;
		if (mac == null || mac.trim().length() == 0)
			mac = NO_VALUE;
		deviceBundle.putString(Key.MAC, mac);

		String localIP = _featureEthernet != null ? _featureEthernet.getNetworkConfig().Addr : NO_VALUE;
		if (localIP == null || localIP.trim().length() == 0)
			localIP = NO_VALUE;
		deviceBundle.putString(Key.IP, localIP);

		String publicIP = _featureInternet != null ? _featureInternet.getPublicIP() : NO_VALUE;
		if (publicIP == null || publicIP.trim().length() == 0)
			publicIP = NO_VALUE;
		deviceBundle.putString(Key.PUBLIC_IP, publicIP);

		bundle.putBundle(Key.DEVICE, deviceBundle);

		Bundle swBundle = new Bundle();
		swBundle.putString(Key.VERSION, Environment.getInstance().getBuildVersion());
		swBundle
		        .putString(Key.KIND, Environment.getInstance().getPrefs().getString(Environment.Param.RELEASE));
		swBundle.putString(Key.CUSTOMER, getCustomer()); // _featureRegister.getBrand()
		swBundle.putString(Key.BRAND, getBrand()); // _featureRegister.getBrand()
		deviceBundle.putBundle(Key.SW, swBundle);

		return bundle;
	}

	protected void addEvent(Bundle eventParams)
	{
		Bundle event = prepareEventObject(eventParams);
		_eventList.add(event);
	}

	private String getCustomer()
	{
		return getPrefs().getString(Param.EVENT_COLLECTOR_CUSTOMER);
	}

	private String getBrand()
	{
		return getPrefs().getString(Param.EVENT_COLLECTOR_BRAND);
	}

	public static String getTimeAsISO(long timestampMillis)
	{
		// TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ");
		// df.setTimeZone(tz);
		String timeAsISO = df.format(timestampMillis);
		return timeAsISO;
	}

	public static long getTimeAsMillis(String isoDateTime) throws ParseException
	{
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ");
		long millis = df.parse(isoDateTime).getTime();
		return millis;
	}

	private void writeEvent(JsonWriter writer, Bundle event) throws IOException
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

	protected Bundle prepareParams(String source, String item, String name, String context, Severity severity)
	{
		Bundle params = new Bundle();

		params.putString(Key.SOURCE, source);

		if (item.length() > 0)
			params.putString(Key.ITEM, item);

		params.putString(Key.NAME, name);
		params.putString(Key.CONTEXT, context);
		params.putString(Key.SEVERITY, severity.name().toLowerCase());

		return params;
	}

	public void registerForOnFeatureInitErrorEvent()
	{
		Environment.getInstance().getEventMessenger().register(new EventReceiver()
		{
			@Override
	        public void onEvent(int msgId, Bundle bundle)
	        {
				String context = Environment.class.getSimpleName() + "::" + bundle.getString(Environment.EXTRA_FEATURE_NAME)
		                + ": error code = " + bundle.getInt(Environment.EXTRA_ERROR_CODE);

				Bundle params = prepareParams("system", "environment", Key.ERROR, context, Severity.FATAL);

				Bundle errorBundle = new Bundle();
				errorBundle.putString(Key.CODE, "n/a");
				errorBundle.putString(Key.MESSAGE, "Cannot complete feature initialization due to an error.");
				params.putBundle(Key.ERROR, errorBundle);

				addEvent(params);
	        }
		}, Environment.ON_FEATURE_INIT_ERROR);
	}

	public void registerForOnPlayerErrorEvent()
	{
		Environment.getInstance().getEventMessenger().register(new EventReceiver()
		{
			@Override
	        public void onEvent(int msgId, Bundle bundle)
	        {
				String context = BasePlayer.class.getSimpleName() + ": "
				        + (bundle != null ? bundle.getString(BasePlayer.PARAM_ERROR) : "");

				Bundle params = prepareParams("player", "", Key.ERROR, context, Severity.ERROR);

				String url = Environment.getInstance().getUserPrefs().getString(FeaturePlayer.UserParam.LAST_URL);
				params.putString(Key.URL, url);

				Bundle errorBundle = new Bundle();
				errorBundle.putString(Key.CODE, bundle != null ? "" + bundle.getInt(BasePlayer.PARAM_WHAT) : "n/a");
				errorBundle.putString(Key.MESSAGE, bundle != null ? "" + bundle.getString(BasePlayer.PARAM_ERROR) : "n/a");
				params.putBundle(Key.ERROR, errorBundle);

				addEvent(params);
	        }
		}, BasePlayer.ON_ERROR);
	}

	public void registerForOnPlayerCompletionEvent()
	{
		Environment.getInstance().getEventMessenger().register(new EventReceiver()
		{
			@Override
	        public void onEvent(int msgId, Bundle bundle)
	        {
				Bundle params = prepareParams(")", "", "completed", BasePlayer.class.getSimpleName(),
				        Severity.INFO);
				addEvent(params);
	        }
		}, BasePlayer.ON_COMPLETION);
	}

	public void registerForOnPlayTimeoutEvent()
	{
		Environment.getInstance().getEventMessenger().register(new EventReceiver()
		{
			@Override
	        public void onEvent(int msgId, Bundle bundle)
	        {
				Bundle params = prepareParams("system", Key.CONNECTION, Key.ERROR, FeatureInternet.class.getSimpleName(),
				        Severity.ERROR);
				addEvent(params);
	        }
		}, FeaturePlayer.ON_PLAY_TIMEOUT);
	}

	public void registerForOnPlayStartedEvent()
	{
		Environment.getInstance().getEventMessenger().register(new EventReceiver()
		{
			@Override
	        public void onEvent(int msgId, Bundle bundle)
	        {
				Bundle params = prepareParams("player", "", "started", FeaturePlayer.class.getSimpleName(),
				        Severity.INFO);

				String url = Environment.getInstance().getUserPrefs().getString(FeaturePlayer.UserParam.LAST_URL);
				params.putString(Key.URL, url);

				addEvent(params);
	        }
		}, FeaturePlayer.ON_PLAY_STARTED);
	}

	public void registerForOnStartFromUpdateEvent()
	{
		Environment.getInstance().getEventMessenger().register(new EventReceiver()
		{
			@Override
	        public void onEvent(int msgId, Bundle bundle)
	        {
				Bundle params = prepareParams("system", Key.UPGRADE, "completed", FeatureUpgrade.class.getSimpleName(),
				        Severity.INFO);

				Bundle swBundle = new Bundle();
				swBundle.putString(Key.PREV_VERSION, bundle.getString(FeatureUpgrade.EXTRA_VERSION_PREV));
				swBundle.putLong(Key.DURATION, bundle.getLong(FeatureUpgrade.EXTRA_UPGRADE_DURATION));

				Bundle upgradeBundle = new Bundle();
				upgradeBundle.putBundle(Key.SW, swBundle);

				params.putBundle(Key.UPGRADE, upgradeBundle);

				addEvent(params);
	        }
		}, FeatureUpgrade.ON_START_FROM_UPDATE);
	}

	public void registerForOnStartUpdateEvent()
	{
		Environment.getInstance().getEventMessenger().register(new EventReceiver()
		{
			@Override
	        public void onEvent(int msgId, Bundle bundle)
	        {
				Bundle params = prepareParams("system", Key.UPGRADE, "started", FeatureUpgrade.class.getSimpleName(),
				        Severity.INFO);

				Bundle swBundle = new Bundle();
				swBundle.putString(Key.VERSION, bundle.getString(FeatureUpgrade.EXTRA_VERSION));

				Bundle upgradeBundle = new Bundle();
				upgradeBundle.putBundle(Key.SW, swBundle);

				params.putBundle(Key.UPGRADE, upgradeBundle);

				addEvent(params);
	        }
		}, FeatureUpgrade.ON_START_UPDATE);
	}

	public void registerForOnDisconnectedEvent()
	{
		Environment.getInstance().getEventMessenger().register(new EventReceiver()
		{
			@Override
	        public void onEvent(int msgId, Bundle bundle)
	        {
				Bundle params = prepareParams("system", Key.CONNECTION, Key.ERROR, FeatureInternet.class.getSimpleName(),
				        Severity.ERROR);

				String errorMsg = "Disconnected from Internet. Reported reason: "
				        + bundle.getString(ResultExtras.ERROR_MESSAGE.name());

				Bundle errorBundle = new Bundle();
				errorBundle.putInt(Key.CODE, bundle.getInt(ResultExtras.ERROR_CODE.name()));
				errorBundle.putString(Key.MESSAGE, errorMsg);
				params.putBundle(Key.ERROR, errorBundle);

				addEvent(params);
	        }
		}, FeatureInternet.ON_DISCONNECTED);
	}

	protected static interface Key
	{
		String DEVICE = "device";
		String MAC = "mac";
		String IP = "ip";
		String PUBLIC_IP = "public_ip";
		String SW = "sw";
		String VERSION = "version";
		String PREV_VERSION = "prev_version";
		String KIND = "kind";
		String CUSTOMER = "customer";
		String BRAND = "brand";
		String EVENT = "event";
		String TIMESTAMP = "timestamp";
		String SOURCE = "source";
		String ITEM = "item";
		String NAME = "name";
		String URL = "url";
		String CODE = "code";
		String MESSAGE = "message";
		String ERROR = "error";
		String UPGRADE = "upgrade";
		String BITRATE = "bitrate";
		String DURATION = "duration";
		String CONTEXT = "context";
		String SEVERITY = "severity";
		String FROM_CHANNEL = "from_channel";
		String TO_CHANNEL = "to_channel";
		String CONNECTION = "connection";
		String INTERVAL = "interval";
		String AVG = "avg";
		String MIN = "min";
		String MAX = "max";
		String DOWNLINK = "downlink";
		String UPLINK = "uplink";
		String PROGRESS = "progress";
		String SIZE = "size";
		String RECEIVED = "received";
		String HOST = "host";
		String DOWNLOAD = "download";
		String TEXT = "text";
		String MODE = "mode";
		String BACKEND = "backend";
	}
}

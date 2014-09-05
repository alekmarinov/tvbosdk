/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureCrashLog.java
 * Author:      zhelyazko
 * Date:        1 Apr 2014
 * Description: Handle unhandled exceptions.
 */

package com.aviq.tv.android.sdk.feature.crashlog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.acra.ACRA;
import org.acra.ACRAConfiguration;
import org.acra.ACRAConfigurationException;
import org.acra.ErrorReporter;
import org.acra.ReportingInteractionMode;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Application;
import android.os.Bundle;
import android.widget.Toast;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.EventReceiver;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.PriorityFeature;
import com.aviq.tv.android.sdk.feature.easteregg.FeatureEasterEgg;
import com.aviq.tv.android.sdk.feature.eventcollector.FeatureEventCollector;
import com.aviq.tv.android.sdk.feature.internet.FeatureInternet;
import com.aviq.tv.android.sdk.feature.network.FeatureNetwork;
import com.aviq.tv.android.sdk.feature.register.FeatureRegister;

/**
 * Handle unhandled exceptions.
 */
@PriorityFeature
public class FeatureCrashLog extends FeatureComponent implements EventReceiver
{
	public static final String TAG = FeatureCrashLog.class.getSimpleName();

	public static final int ON_RUNTIME_EXCEPTION = EventMessenger.ID("ON_RUNTIME_EXCEPTION");
	public static final int ON_APP_STARTED = EventMessenger.ID("ON_APP_STARTED");

	public static final String EXTRA_RUNTIME_EXCEPTION_MESSAGE = "EXTRA_RUNTIME_EXCEPTION_MESSAGE";
	public static final String EXTRA_RUNTIME_EXCEPTION_IS_SILENT = "EXTRA_RUNTIME_EXCEPTION_IS_SILENT";

	/**
	 * Add this to any calls to ACRA.handleSilentException when sending logcat
	 * to the server.
	 */
	public static final String EXCEPTION_TAG = "[SILENT-EXCEPTION-ID]";
	public static final String NO_VALUE = "n/a";

	public enum Param
	{
		/** Server location where crashlogs are uploaded. */
		REMOTE_SERVER("https://services.aviq.com:30227/upload/logs/"),

		/** Username for remote server */
		REMOTE_SERVER_USERNAME(""),

		/** Password for remote server */
		REMOTE_SERVER_PASSWORD(""),

		/** Socket timeout in milliseconds */
		SOCKET_TIMEOUT_MILLIS(20000),

		CRASHLOG_CUSTOMER(""),
		CRASHLOG_BRAND(""),

		COLD_BOOT_FILE("/cache/update/coldboot.txt"),

		/**
		 * This file (in the "files" directory) contains the following data:
		 * 0 = app started after cold boot
		 * 1 = app started after standby's suicide
		 * 2 = app started after ACRA handled some uncaught exception
		 */
		APP_START_REASON_FILE("app_start_reason.txt");

		/*
		 * TODO
		 * config.setLogcatArguments(new String[] {"-t", "1000", "-v", "time"});
		 */

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.CRASHLOG).put(name(), value);
		}

		Param(String value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.CRASHLOG).put(name(), value);
		}
	}

	public FeatureCrashLog() throws FeatureNotFoundException
	{
		require(FeatureName.Scheduler.INTERNET);
		require(FeatureName.Component.EASTER_EGG);
		require(FeatureName.Scheduler.EVENT_COLLECTOR);
	}

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		initAcra();
		_feature.Component.EASTER_EGG.getEventMessenger().register(this, FeatureEasterEgg.ON_KEY_SEQUENCE);

		/**
		 * The purpose of this is to be able to catch requests for crash events
		 * triggered from outside the app.
		 */
		getEventMessenger().register(this, ON_RUNTIME_EXCEPTION);

		// Detect the reason for the app start
		sendAppStartEvent();

		super.initialize(onFeatureInitialized);
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.CRASHLOG;
	}

	@Override
	public void onEvent(int msgId, Bundle bundle)
	{
		if (msgId == FeatureInternet.ON_CONNECTED)
		{
			String publicIP = bundle.getString(FeatureInternet.ResultExtras.PUBLIC_IP.name()); //  _feature.Scheduler.INTERNET.getPublicIP();
			if (publicIP != null)
			{
				// Got the public IP, no need to check for it anymore
				_feature.Scheduler.INTERNET.getEventMessenger().unregister(this, FeatureInternet.ON_CONNECTED);
				ACRA.getErrorReporter().putCustomData("PUBLIC_IP", publicIP);

				// Ensure this is in the data as well
				String localIP = FeatureNetwork.getLocalIP();
				if (localIP == null || localIP.trim().length() == 0)
					localIP = NO_VALUE;
				ACRA.getErrorReporter().putCustomData("LOCAL_IP", localIP);

				// Only reset it if already there
				if (ACRA.getErrorReporter().getCustomData(Key.DEVICE) != null)
					ACRA.getErrorReporter().putCustomData(Key.DEVICE, prepareDeviceObject());
			}
		}
		else if (FeatureEasterEgg.ON_KEY_SEQUENCE == msgId)
		{
			String keySeq = bundle.getString(FeatureEasterEgg.EXTRA_KEY_SEQUENCE);
			if (FeatureEasterEgg.KEY_SEQ_LOG.equals(keySeq))
			{
				ACRA.getErrorReporter().handleSilentException(
				        new Exception(EXCEPTION_TAG + " Sending logcat from user activity."));

				Toast.makeText(Environment.getInstance().getApplicationContext(),
				        "Log has been captured and sent for processing. Thank you!", Toast.LENGTH_LONG).show();
			}
		}
		else if (ON_RUNTIME_EXCEPTION == msgId)
		{
			String msg = "User-triggered runtime error.";
			boolean isSilent = false;

			if (bundle != null)
			{
				if (bundle.containsKey(EXTRA_RUNTIME_EXCEPTION_MESSAGE))
					msg = bundle.getString(EXTRA_RUNTIME_EXCEPTION_MESSAGE);

				if (bundle.containsKey(EXTRA_RUNTIME_EXCEPTION_IS_SILENT))
					isSilent = bundle.getBoolean(EXTRA_RUNTIME_EXCEPTION_IS_SILENT);
			}

			if (isSilent)
			{
				ACRA.getErrorReporter().handleSilentException(new RuntimeException(msg));
			}
			else
			{
				throw new RuntimeException(msg);
			}
		}
	}

	private void initAcra()
	{
		Log.i(TAG, ".initAcra");

		Environment env = Environment.getInstance();
		Application app = env.getApplication();

		String serverUri = getPrefs().getString(Param.REMOTE_SERVER);
		String username = getPrefs().getString(Param.REMOTE_SERVER_USERNAME);
		String password = getPrefs().getString(Param.REMOTE_SERVER_PASSWORD);

		ACRAConfiguration config = ACRA.getNewDefaultConfig(app);
		config.setFormUri(serverUri);
		config.setFormUriBasicAuthLogin(username);
		config.setFormUriBasicAuthPassword(password);
		config.setDisableSSLCertValidation(true);
		config.setHttpMethod(org.acra.sender.HttpSender.Method.PUT);
		// config.setReportType(org.acra.sender.HttpSender.Type.JSON);
		config.setReportType(org.acra.sender.HttpSender.Type.FORM);
		config.setSocketTimeout(20000);
		config.setLogcatArguments(new String[]
		{ "-t", "6000", "-v", "time" });
		config.setApplicationLogFile(null);
		config.setApplicationLogFileLines(0);
		config.setAdditionalSharedPreferences(null);

		try
		{
			config.setMode(ReportingInteractionMode.SILENT);
		}
		catch (ACRAConfigurationException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		ACRA.setConfig(config);

		ACRA.init(app);

		// Set a custom sender; always right after ACRA.init().
		// CrashLogJsonReportSender crashLogSender = new
		// CrashLogJsonReportSender(app);
		CrashLogTextReportSender crashLogSender = new CrashLogTextReportSender(app);
		ACRA.getErrorReporter().setReportSender(crashLogSender);

		// Add a new sender; keep the previous senders
		// ACRA.getErrorReporter().addReportSender(yourSender);

		ErrorReporter errorReporter = ACRA.getErrorReporter();

		// Add custom report data

		if (CrashLogJsonReportSender.class.equals(crashLogSender.getClass()))
		{
			errorReporter.putCustomData(Key.DEVICE, prepareDeviceObject());
			errorReporter.putCustomData(Key.EVENT, prepareEventObject());
		}

		String boxId = "00:00:00:00:00:00";
		FeatureRegister featureRegister = (FeatureRegister) Environment.getInstance().getFeatureComponent(FeatureName.Component.REGISTER);
		if (featureRegister != null)
		{
			boxId = featureRegister.getBoxId();
		}
		errorReporter.putCustomData("BOX_ID", boxId);
		errorReporter.putCustomData("BRAND", getBrand());
		errorReporter.putCustomData("CUSTOMER", getCustomer());
		errorReporter.putCustomData("SW_VERSION", Environment.getInstance().getBuildVersion());

		// FIXME: Take from FeatureEthernet when implemented.
		errorReporter.putCustomData("ETHERNET_MAC", boxId);

		String localIP = FeatureNetwork.getLocalIP();
		if (localIP == null || localIP.trim().length() == 0)
			localIP = NO_VALUE;
		errorReporter.putCustomData("LOCAL_IP", localIP);

		String publicIP = _feature.Scheduler.INTERNET.getPublicIP();
		if (publicIP == null || publicIP.trim().length() == 0)
			publicIP = NO_VALUE;
		errorReporter.putCustomData("PUBLIC_IP", publicIP);

		// If the public IP is null, wait for Internet to show up and recheck
		if (NO_VALUE.equals(publicIP))
			_feature.Scheduler.INTERNET.getEventMessenger().register(this, FeatureInternet.ON_CONNECTED);
	}

	private String prepareDeviceObject()
	{
		JSONObject device = new JSONObject();
		try
		{

			String boxId = "00:00:00:00:00:00";
			FeatureRegister featureRegister = (FeatureRegister) Environment.getInstance().getFeatureComponent(FeatureName.Component.REGISTER);
			if (featureRegister != null)
			{
				boxId = featureRegister.getBoxId();
			}

			device.accumulate(Key.MAC, boxId);

			String localIP = FeatureNetwork.getLocalIP();
			if (localIP == null || localIP.trim().length() == 0)
				localIP = NO_VALUE;
			device.accumulate(Key.IP, localIP);

			String publicIP = _feature.Scheduler.INTERNET.getPublicIP();
			if (publicIP == null || publicIP.trim().length() == 0)
				publicIP = NO_VALUE;
			device.accumulate(Key.PUBLIC_IP, publicIP);

			JSONObject sw = new JSONObject();
			sw.accumulate(Key.VERSION, Environment.getInstance().getBuildVersion());
			sw.accumulate(Key.KIND, Environment.getInstance().getPrefs().getString(Environment.Param.RELEASE));
			sw.accumulate(Key.CUSTOMER, getCustomer()); // _featureRegister.getBrand()
			sw.accumulate(Key.BRAND, getBrand()); // _featureRegister.getBrand()

			device.accumulate(Key.SW, sw);
		}
		catch (JSONException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		return device.toString();
	}

	private String prepareEventObject()
	{
		JSONObject event = new JSONObject();
		try
		{
			event.accumulate(Key.TIMESTAMP, "{{TIMESTAMP}}");
			event.accumulate(Key.SOURCE, "system");
			event.accumulate(Key.ITEM, "crash");
		}
		catch (JSONException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		return event.toString();
	}

	private String getCustomer()
	{
		return getPrefs().getString(Param.CRASHLOG_CUSTOMER);
	}

	private String getBrand()
	{
		return getPrefs().getString(Param.CRASHLOG_BRAND);
	}

	private void sendAppStartEvent()
	{
		Log.i(TAG, ".sendAppStartEvent");

		boolean isColdBood = isColdBoot();
		Log.i(TAG, "isColdBoot = " + isColdBood);

		File f = new File(Environment.getInstance().getFilesDir(), getPrefs().getString(Param.APP_START_REASON_FILE));
		if (isColdBood)
		{
			// Cold boot => OK, delete reason file
			f.delete();

			Bundle params = new Bundle();
			params.putString(FeatureEventCollector.Key.REASON, AppRestartReasonType.COLDBOOT.getName());
			params.putString(FeatureEventCollector.Key.SEVERITY, AppRestartReasonType.COLDBOOT.getSeverity());
			getEventMessenger().trigger(ON_APP_STARTED, params);
		}
		else
		{
			// Reason file does not exists - unknown reason for app restart
			if (!f.exists())
			{
				new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						// Pattern for Signal 7 = SIGBUS, Incorrect access to
						// memory (data misalignment)
						//
						// Pattern for Signal 11 = SIGSEGV, Incorrect access to
						// memory (write to inaccessible memory)

						Pattern patternSignal711 = Pattern.compile(".*?Fatal signal (7|11).*?", Pattern.MULTILINE
						        | Pattern.CASE_INSENSITIVE);

						// Pattern for Signal 15 = SIGTERM, Termination request.
						// JVM will exit normally.
						// Sample: Process 5035 terminated by signal (15)

						Pattern patternSignal15 = Pattern.compile(
								".*?Process\\s.*?\\sterminated by signal \\(15\\).*?", Pattern.MULTILINE
						                | Pattern.CASE_INSENSITIVE);

						// Pattern for Signal 9 = App kill
						// Sample: Sending signal. PID: 6364 SIG: 9
						Pattern patternSignal9 = Pattern.compile(
						        ".*?Sending signal\\.\\sPID:\\s.*?\\sSIG:\\s9.*?", Pattern.MULTILINE
						                | Pattern.CASE_INSENSITIVE);

						boolean foundMatch = false;

						List<String> logcat = readLogcat();
						for (int i = logcat.size() - 1; i > -1; i--)
						{
							String line = logcat.get(i);

							// Search for crashes resulting from signals 9 and 15
							Matcher matcher = patternSignal9.matcher(line);
							if (matcher.find())
							{
								Bundle params = new Bundle();
								params.putString(FeatureEventCollector.Key.REASON,
								        AppRestartReasonType.APP_KILLED.getName());
								params.putString(FeatureEventCollector.Key.SEVERITY,
								        AppRestartReasonType.APP_KILLED.getSeverity());
								getEventMessenger().trigger(ON_APP_STARTED, params);

								ACRA.getErrorReporter()
								        .handleSilentException(
								                new RuntimeException(
								                        "Generated exception due to unexpected application restart: signal 9 - app killed via shell command."));

								foundMatch = true;
								break;
							}

							matcher = patternSignal15.matcher(line);
							if (matcher.find())
							{
								Bundle params = new Bundle();
								params.putString(FeatureEventCollector.Key.REASON,
								        AppRestartReasonType.APP_KILLED.getName());
								params.putString(FeatureEventCollector.Key.SEVERITY,
								        AppRestartReasonType.APP_KILLED.getSeverity());
								getEventMessenger().trigger(ON_APP_STARTED, params);

								ACRA.getErrorReporter()
								        .handleSilentException(
								                new RuntimeException(
								                        "Generated exception due to unexpected application restart: signal 15 - VM terminated the application."));

								foundMatch = true;
								break;
							}

							matcher = patternSignal711.matcher(line);
							if (matcher.find())
							{
								Bundle params = new Bundle();
								params.putString(FeatureEventCollector.Key.REASON,
								        AppRestartReasonType.SIGNAL_CRASH.getName());
								params.putString(FeatureEventCollector.Key.SEVERITY,
								        AppRestartReasonType.SIGNAL_CRASH.getSeverity());
								getEventMessenger().trigger(ON_APP_STARTED, params);

								ACRA.getErrorReporter()
								        .handleSilentException(
								                new RuntimeException(
								                        "Generated exception due to unexpected application restart: signal 7 (data misalignment) or signal 11 (write to inaccessible memory)."));

								foundMatch = true;
								break;
							}
						}

						if (!foundMatch)
						{
							Bundle params = new Bundle();
							params.putString(FeatureEventCollector.Key.REASON, AppRestartReasonType.UNKNOWN.getName());
							params.putString(FeatureEventCollector.Key.SEVERITY,
							        AppRestartReasonType.UNKNOWN.getSeverity());
							getEventMessenger().trigger(ON_APP_STARTED, params);

							ACRA.getErrorReporter()
							        .handleSilentException(
							                new RuntimeException(
							                        "Generated exception due to unexpected application restart: unknown reason."));
						}
					}
				}).start();
			}
			else
			{
				int reason = getAppRestartReason();
				if (AppRestartReasonType.STANDBY_WAKEUP.getReason() == reason)
				{
					Bundle params = new Bundle();
					params.putString(FeatureEventCollector.Key.REASON, AppRestartReasonType.STANDBY_WAKEUP.getName());
					params.putString(FeatureEventCollector.Key.SEVERITY,
					        AppRestartReasonType.STANDBY_WAKEUP.getSeverity());
					getEventMessenger().trigger(ON_APP_STARTED, params);
				}
				else if (AppRestartReasonType.UNHANDLED_CRASH.getReason() == reason)
				{
					Bundle params = new Bundle();
					params.putString(FeatureEventCollector.Key.REASON, AppRestartReasonType.UNHANDLED_CRASH.getName());
					params.putString(FeatureEventCollector.Key.SEVERITY,
					        AppRestartReasonType.UNHANDLED_CRASH.getSeverity());
					getEventMessenger().trigger(ON_APP_STARTED, params);
				}
				else
				{
					Bundle params = new Bundle();
					params.putString(FeatureEventCollector.Key.REASON, AppRestartReasonType.UNKNOWN.getName());
					params.putString(FeatureEventCollector.Key.SEVERITY, AppRestartReasonType.UNKNOWN.getSeverity());
					getEventMessenger().trigger(ON_APP_STARTED, params);

					ACRA.getErrorReporter().handleSilentException(
					        new RuntimeException(
					                "Generated exception due to unexpected application restart."));
				}
			}

			// Delete the file, don't need it anymore
			f.delete();
		}
	}

	private boolean isColdBoot()
	{
		File file = new File(getPrefs().getString(Param.COLD_BOOT_FILE));
		if (file.exists())
		{
			// Don't delete this or app features that use it may fail
			// FIXME: Need to work around this
			//file.delete();
			return true;
		}
		return false;
	}

	public static enum AppRestartReasonType
	{
		COLDBOOT(-1, "system_start", FeatureEventCollector.Severity.INFO.name()),
		STANDBY_WAKEUP(1, "standby_wakeup", FeatureEventCollector.Severity.INFO.name()),
		UNHANDLED_CRASH(2, "app_crash", FeatureEventCollector.Severity.ERROR.name()),
		SIGNAL_CRASH(3, "system_crash", FeatureEventCollector.Severity.FATAL.name()),
		APP_KILLED(4, "app_killed", FeatureEventCollector.Severity.FATAL.name()),
		UNKNOWN(5, "unknown", FeatureEventCollector.Severity.FATAL.name());

		private int _reason;
		private String _name;
		private String _severity;

		private AppRestartReasonType(int id, String name, String severity)
		{
			_reason = id;
			_name = name;
			_severity = severity;
		}

		public int getReason()
		{
			return _reason;
		}

		public String getName()
		{
			return _name;
		}

		public String getSeverity()
		{
			return _severity;
		}
	}

	public void setAppRestartReason(AppRestartReasonType reason)
	{
		FileOutputStream fos = null;
	    try
	    {
			File f = new File(Environment.getInstance().getFilesDir(), getPrefs()
			        .getString(Param.APP_START_REASON_FILE));
	        fos = new FileOutputStream(f);
	        fos.write(new byte[] { (byte) reason.getReason() });
	    }
	    catch (IOException e)
	    {
	        Log.e(TAG, e.getMessage(), e);
	    }
	    finally
	    {
	        if (fos != null)
	        {
	            try
                {
                    fos.close();
                }
                catch (IOException e)
                {
                	Log.e(TAG, e.getMessage(), e);
                }
	        }
	    }
	}

	private int getAppRestartReason()
	{
		int reason = AppRestartReasonType.COLDBOOT.getReason();
		FileInputStream fis = null;
		try
		{
			File f = new File(Environment.getInstance().getFilesDir(), getPrefs()
			        .getString(Param.APP_START_REASON_FILE));
			fis = new FileInputStream(f);
			reason = fis.read();
		}
		catch (IOException e)
	    {
	        Log.e(TAG, e.getMessage(), e);
	    }
	    finally
	    {
	        if (fis != null)
	        {
	            try
                {
	            	fis.close();
                }
                catch (IOException e)
                {
                	Log.e(TAG, e.getMessage(), e);
                }
	        }
	    }
		return reason;
	}

	private List<String> readLogcat()
	{
		List<String> log = new ArrayList<String>(10000);
		try
		{
			Process process = Runtime.getRuntime().exec("logcat -v time -t 6000 -d");
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			String line = "";
			while ((line = bufferedReader.readLine()) != null)
			{
				log.add(line);
			}
		}
		catch (IOException e)
		{
			Log.e(TAG, "Cannot retrieve logcat.", e);
		}

		return log;
	}

	private static interface Key
	{
		String DEVICE = "device";
		String MAC = "mac";
		String IP = "ip";
		String PUBLIC_IP = "public_ip";
		String SW = "sw";
		String VERSION = "version";
		String KIND = "kind";
		String CUSTOMER = "customer";
		String BRAND = "brand";
		String EVENT = "event";
		String TIMESTAMP = "timestamp";
		String SOURCE = "source";
		String ITEM = "item";
		String NAME = "name";
	}
}

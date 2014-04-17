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

import org.acra.ACRA;
import org.acra.ACRAConfiguration;
import org.acra.ACRAConfigurationException;
import org.acra.ErrorReporter;
import org.acra.ReportingInteractionMode;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventReceiver;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.feature.internet.FeatureInternet;
import com.aviq.tv.android.sdk.feature.network.FeatureEthernet;
import com.aviq.tv.android.sdk.feature.register.FeatureRegister;

/**
 * Handle unhandled exceptions.
 */
public class FeatureCrashLog extends FeatureComponent implements EventReceiver
{
	public static final String TAG = FeatureCrashLog.class.getSimpleName();

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
		CRASHLOG_BRAND("");

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

	private FeatureInternet _featureInternet;
	private FeatureRegister _featureRegister;
	private FeatureEthernet _featureEthernet;

	public FeatureCrashLog()
	{
		_dependencies.Schedulers.add(FeatureName.Scheduler.INTERNET);
		_dependencies.Components.add(FeatureName.Component.REGISTER);
		_dependencies.Components.add(FeatureName.Component.ETHERNET);
	}

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");

		Environment env = Environment.getInstance();
		try
		{
			_featureInternet = (FeatureInternet) env.getFeatureScheduler(FeatureName.Scheduler.INTERNET);
			_featureRegister = (FeatureRegister) env.getFeatureComponent(FeatureName.Component.REGISTER);
			_featureEthernet = (FeatureEthernet) env.getFeatureComponent(FeatureName.Component.ETHERNET);
		}
		catch (FeatureNotFoundException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}

		initAcra();

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
			String publicIP = _featureInternet.getPublicIP();
			if (publicIP != null)
			{
				// Got the public IP, no need to check for it anymore
				_featureInternet.getEventMessenger().unregister(this, FeatureInternet.ON_CONNECTED);
				ACRA.getErrorReporter().putCustomData("PUBLIC IP", publicIP);
				ACRA.getErrorReporter().putCustomData(Key.DEVICE, prepareDeviceObject());
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
		config.setReportType(org.acra.sender.HttpSender.Type.JSON);
		config.setSocketTimeout(20000);
		config.setLogcatArguments(new String[]
		{ "-t", "2000", "-v", "time" });
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
		CrashLogJsonReportSender crashLogSender = new CrashLogJsonReportSender(app);
		ACRA.getErrorReporter().setReportSender(crashLogSender);

		// Add a new sender; keep the previous senders
		// ACRA.getErrorReporter().addReportSender(yourSender);

		ErrorReporter errorReporter = ACRA.getErrorReporter();

		// Add custom report data

		errorReporter.putCustomData(Key.DEVICE, prepareDeviceObject());
		errorReporter.putCustomData(Key.EVENT, prepareEventObject());

		errorReporter.putCustomData("BOX_ID", _featureRegister.getBoxId());
		errorReporter.putCustomData("BRAND", getBrand());
		errorReporter.putCustomData("CUSTOMER", getCustomer());

		String publicIP = _featureInternet.getPublicIP();
		errorReporter.putCustomData("PUBLIC IP", publicIP);

		// If the public IP is null, wait for Internet to show up and recheck
		if (publicIP == null)
			_featureInternet.getEventMessenger().register(this, FeatureInternet.ON_CONNECTED);
	}

	private String prepareDeviceObject()
	{
		JSONObject device = new JSONObject();
		try
		{
			device.accumulate(Key.MAC, _featureRegister.getBoxId());
			device.accumulate(Key.IP, _featureEthernet.getNetworkConfig().Addr);
			device.accumulate(Key.PUBLIC_IP, _featureInternet.getPublicIP());

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

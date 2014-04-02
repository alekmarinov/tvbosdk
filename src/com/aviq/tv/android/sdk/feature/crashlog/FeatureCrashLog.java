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

import android.app.Application;
import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.feature.internet.FeatureInternet;
import com.aviq.tv.android.sdk.feature.register.FeatureRegister;

/**
 * Handle unhandled exceptions.
 */
public class FeatureCrashLog extends FeatureComponent
{
	public static final String TAG = FeatureCrashLog.class.getSimpleName();

	public enum Param
	{
		/** Server location where crashlogs are uploaded. */
		REMOTE_SERVER("https://services.aviq.com:30227/upload/logs/"),

		/** Username for remote server */
		REMOTE_SERVER_USERNAME("zixiStb"),

		/** Password for remote server */
		REMOTE_SERVER_PASSWORD("datQrsP1_pH3247ttee"),

		/** Socket timeout in milliseconds */
		SOCKET_TIMEOUT_MILLIS(20000);

		/* TODO
		config.setLogcatArguments(new String[] {"-t", "1000", "-v", "time"});
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

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");

		initAcra();

		super.initialize(onFeatureInitialized);
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.CRASHLOG;
	}

	private void initAcra()
	{
		Log.i(TAG, ".initAcra");

		Application app = Environment.getInstance().getActivity().getApplication();

		String serverUri = getPrefs().getString(Param.REMOTE_SERVER);
		String username = getPrefs().getString(Param.REMOTE_SERVER_USERNAME);
		String password = getPrefs().getString(Param.REMOTE_SERVER_PASSWORD);

		ACRAConfiguration config = ACRA.getNewDefaultConfig(app);
		config.setFormUri(serverUri);
		config.setFormUriBasicAuthLogin(username);
		config.setFormUriBasicAuthPassword(password);
		config.setDisableSSLCertValidation(true);
		config.setHttpMethod(org.acra.sender.HttpSender.Method.PUT);
		config.setReportType(org.acra.sender.HttpSender.Type.FORM);
		config.setSocketTimeout(20000);
		config.setLogcatArguments(new String[] {"-t", "1000", "-v", "time"});
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
		CrashLogReportSender crashLogSender = new CrashLogReportSender(app);
        ACRA.getErrorReporter().setReportSender(crashLogSender);

		// Add a new sender; keep the previous senders
		//ACRA.getErrorReporter().addReportSender(yourSender);

        try
        {
        	ErrorReporter errorReporter = ACRA.getErrorReporter();

        	// Add custom report data from FeatureRegister
        	Environment env = Environment.getInstance();
	        env.use(FeatureName.Component.REGISTER);
	        FeatureRegister featureRegister = (FeatureRegister) env.getFeatureComponent(FeatureName.Component.REGISTER);

	        env.use(FeatureName.Scheduler.INTERNET);
	        FeatureInternet featureInternet = (FeatureInternet) env.getFeatureScheduler(FeatureName.Scheduler.INTERNET);

	        errorReporter.putCustomData("BOX_ID", featureRegister.getBoxId());
	        errorReporter.putCustomData("BRAND", featureRegister.getBrand());

	        errorReporter.putCustomData("PUBLIC IP", featureInternet.getPublicIP());
	        //errorReporter.putCustomData("CUSTOMER", Globals.CUSTOMER);
        }
        catch (FeatureNotFoundException e)
        {
	        Log.e(TAG, "Cannot add custom data for crashlogs from FeatureRegister.", e);
        }
	}
}

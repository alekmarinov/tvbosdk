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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.EventReceiver;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureManager;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.IFeature;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;
import com.aviq.tv.android.sdk.core.feature.annotation.Priority;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.easteregg.FeatureEasterEgg;
import com.aviq.tv.android.sdk.feature.eventcollector.FeatureEventCollector;
import com.aviq.tv.android.sdk.feature.internet.FeatureInternet;
import com.aviq.tv.android.sdk.feature.internet.UploadService;
import com.aviq.tv.android.sdk.feature.system.FeatureDevice;
import com.aviq.tv.android.sdk.feature.system.FeatureDevice.DeviceAttribute;
import com.aviq.tv.android.sdk.utils.Files;

/**
 * Feature handling application errors and crashes
 */
@Author("alek")
@Priority
public class FeatureCrashLog extends FeatureComponent implements Thread.UncaughtExceptionHandler
{
	public static final String TAG = FeatureCrashLog.class.getSimpleName();
	public static final int ON_CRASH_ERROR = EventMessenger.ID("ON_CRASH_ERROR");

	public static enum Extras
	{
		SEVERITY, TAG, MESSAGE, LOGCAT_URL, TRACEBACK, FILENAME, METHOD, LINE_NUMBER, FEATURE, PARAMS, AUTHOR
	}

	public FeatureCrashLog() throws FeatureNotFoundException
	{
		require(FeatureName.Component.DEVICE);
	}

	public enum Severity
	{
		INFO, ALERT, ERROR, FATAL;
	}

	public static enum Param
	{
		/** Crash Log URL */
		CRASHLOG_SERVER_URL(""),

		/** Username for report URL */
		CRASHLOG_SERVER_USERNAME(""),

		/** Password for report URL */
		CRASHLOG_SERVER_PASSWORD(""),

		/** Logcat name template */
		LOGCAT_FILENAME_TEMPLATE("${BUILD}-${CUSTOMER}-${BRAND}-${MAC}-${DATETIME}-${RANDOM}.logcat"),

		/** Path to the CA certificate relative to the assets folder */
		CRASHLOG_SERVER_CA_CERT_PATH(""),

		/** Directory where to store logcat before upload */
		LOGCAT_DIRECTORY("logcat");

		Param(String value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.CRASHLOG).put(name(), value);
		}
	}

	private String _logcatDir;

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		Thread.currentThread().setUncaughtExceptionHandler(this);

		FeatureEasterEgg featureEasterEgg = (FeatureEasterEgg)Environment.getInstance().getFeatureComponent(FeatureName.Component.EASTER_EGG);
		if (featureEasterEgg != null)
			featureEasterEgg.getEventMessenger().register(this, FeatureEasterEgg.ON_KEY_SEQUENCE);

		Environment.getInstance().getEventMessenger().register(new EventReceiver()
		{
			@Override
			public void onEvent(int msgId, Bundle bundle)
			{
				String featureName = bundle.getString(Environment.ExtraInitError.FEATURE_NAME.name());
				String featureClassName = bundle.getString(Environment.ExtraInitError.FEATURE_CLASS.name());
				Class<?> featureClass;
				IFeature feature = null;
				try
				{
					featureClass = Class.forName(featureClassName);
					feature = Environment.getInstance().getFeature(featureClass);
				}
				catch (ClassNotFoundException e)
				{
					Log.e(TAG, e.getMessage(), e);
				}
				int errCode = bundle.getInt(Environment.ExtraInitError.ERROR_CODE.name());
				Bundle errData = bundle.getBundle(Environment.ExtraInitError.ERROR_DATA.name());
				FeatureError error = new FeatureError(feature, errCode, errData);
				fatal(featureName, "feature init failed: " + error, error);
			}
		}, Environment.ON_FEATURE_INIT_ERROR);

		FeatureDevice.StartReason startReason = _feature.Component.DEVICE.getStartReason();

		// initialize logcat file directory
		File filesDir = Environment.getInstance().getFilesDir();
		_logcatDir = filesDir.getAbsolutePath() + File.separator + getPrefs().getString(Param.LOGCAT_DIRECTORY);
		filesDir = new File(_logcatDir);
		if (!filesDir.exists())
			filesDir.mkdirs();

		// delete previously stored logcat files
		deleteLogcats(_logcatDir);

		if (!FeatureDevice.StartReason.NORMAL.equals(startReason))
		{
			if (FeatureDevice.StartReason.SUICIDE.equals(startReason))
			{
				alert(TAG, "Suicide restart: " + _feature.Component.DEVICE.getSuicideReason(), null);
				super.initialize(onFeatureInitialized);
			}
			else
			{
				new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						String reason = null;
						Bundle bundleEx = new Bundle();
						String logcatFileName = null;
						try
						{
							// saves logcat and search logcat for signal
							logcatFileName = newLogcatName();
							String logcatFilePath = _logcatDir + File.separator + logcatFileName;
							reason = saveLogcatAndDetectSignalReason(logcatFilePath);

							// upload logcat on server
							String logcatUrl = uploadLogcatFile(logcatFilePath);
							bundleEx.putString(Extras.LOGCAT_URL.name(), logcatUrl);
						}
						catch (IOException e)
						{
							Log.e(TAG, e.getMessage(), e);
						}
						if (reason == null)
							reason = "Restart without reason. Check logcat " + logcatFileName;

						fatal(TAG, reason, null, bundleEx);
					}
				}).start();
				FeatureCrashLog.super.initialize(onFeatureInitialized);
			}
		}
		else
		{
			super.initialize(onFeatureInitialized);
		}
	}

	@Override
	public void onEvent(int msgId, Bundle bundle)
	{
		if (FeatureEasterEgg.ON_KEY_SEQUENCE == msgId)
		{
			String keySeq = bundle.getString(FeatureEasterEgg.EXTRA_KEY_SEQUENCE);
			if (FeatureEasterEgg.KEY_SEQ_LOG.equals(keySeq))
			{
				throw new RuntimeException("Test Fatal Exception");
			}
		}
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.CRASHLOG;
	}

	/**
	 * Sends logcat to server and returns logcat URL on the server where it can
	 * be located if the sending succeeds
	 *
	 * @throws IOException
	 */
	public void sendLogcat() throws IOException
	{
		// saves logcat and search logcat for signal
		String logcatFileName = newLogcatName();
		String logcatFilePath = _logcatDir + File.separator + logcatFileName;
		saveLogcat(logcatFilePath);
		String logcatUrl = uploadLogcatFile(logcatFilePath);
		Bundle bundle = new Bundle();
		bundle.putString(Extras.LOGCAT_URL.name(), logcatUrl);
		log(Severity.INFO, TAG, "Event with logcat attached", null, bundle);
	}

	/**
	 * Log info message
	 *
	 * @param tag
	 *            Used to identify the source of a log message
	 * @param message
	 *            The message you would like logged.
	 * @param ex
	 *            An exception to log
	 */
	public void info(String tag, String message, Throwable ex)
	{
		log(Severity.INFO, tag, message, ex);
	}

	/**
	 * Log alert message
	 *
	 * @param tag
	 *            Used to identify the source of a log message
	 * @param message
	 *            The message you would like logged.
	 * @param ex
	 *            An exception to log
	 */
	public void alert(String tag, String message, Throwable ex)
	{
		log(Severity.ALERT, tag, message, ex);
	}

	/**
	 * Log error message
	 *
	 * @param tag
	 *            Used to identify the source of a log message
	 * @param message
	 *            The message you would like logged.
	 * @param ex
	 *            An exception to log
	 */
	public void error(String tag, String message, Throwable ex)
	{
		log(Severity.ERROR, tag, message, ex);
	}

	/**
	 * Log fatal error
	 *
	 * @param tag
	 *            Used to identify the source of a log message
	 * @param message
	 *            The message you would like logged.
	 * @param ex
	 *            An exception to log
	 */
	public void fatal(String tag, String message, Throwable ex)
	{
		log(Severity.FATAL, tag, message, ex);
	}

	/**
	 * Log fatal error
	 *
	 * @param tag
	 *            Used to identify the source of a log message
	 * @param message
	 *            The message you would like logged.
	 * @param ex
	 *            An exception to log
	 * @param extra
	 *            Additional data related to the fatal error
	 */
	public void fatal(String tag, String message, Throwable ex, Bundle extra)
	{
		log(Severity.FATAL, tag, message, ex, extra);
	}

	@Override
	public void uncaughtException(Thread thread, final Throwable ex)
	{
		Log.e(TAG, ex.getMessage(), ex);
		fatal(TAG, "uncaught exception: " + ex.getMessage(), ex);
	}

	private void log(Severity severity, String tag, String message, Throwable ex)
	{
		log(severity, tag, message, ex, null);
	}

	private void log(Severity severity, String tag, String message, Throwable ex, Bundle extra)
	{
		Bundle bundle = new Bundle();
		if (extra != null)
			bundle.putAll(extra);
		bundle.putString(Extras.SEVERITY.name(), severity.name());
		bundle.putString(Extras.TAG.name(), tag);
		bundle.putString(Extras.MESSAGE.name(), tag + ": " + message);

		if (ex != null)
		{
			bundle.putString(Extras.TRACEBACK.name(), android.util.Log.getStackTraceString(ex));

			FeatureManager featureManager = Environment.getInstance().getFeatureManager();
			StackTraceElement steLog = null;
			for (StackTraceElement ste : ex.getStackTrace())
			{
				// log first point in exception stack
				if (steLog == null)
					steLog = ste;
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
						else
						{
							bundle.putString(Extras.AUTHOR.name(), "unknown");
						}

						bundle.putString(Extras.PARAMS.name(), collectFeatureParams(feature));
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
			bundle.putString(Extras.FILENAME.name(), steLog.getFileName());
			bundle.putString(Extras.METHOD.name(), steLog.getMethodName());
			bundle.putInt(Extras.LINE_NUMBER.name(), steLog.getLineNumber());
		}

		if (Severity.FATAL.equals(severity))
		{
			bundle.putBoolean(FeatureEventCollector.OnTrackExtra.IMMEDIATE.name(), Boolean.TRUE);
			getEventMessenger().triggerDirect(ON_CRASH_ERROR, bundle);
		}
		else
			getEventMessenger().trigger(ON_CRASH_ERROR, bundle);
	}

	private String collectFeatureParams(IFeature feature)
	{
		StringBuffer sb = new StringBuffer();
		try
		{
			Prefs prefs = feature.getPrefs();
			Class<?> paramClass = Class.forName(feature.getClass().getName() + "$Param");
			boolean isFirst = true;
			for (Object paramName : paramClass.getEnumConstants())
			{
				if (isFirst)
					isFirst = false;
				else
					sb.append(',');

				String paramValue = null;
				try
				{
					paramValue = prefs.getString(paramName);
				}
				catch (ClassCastException cce)
				{
					paramValue = String.valueOf(prefs.getInt(paramName));
				}
				sb.append(paramName).append('=').append(paramValue);
			}
		}
		catch (ClassNotFoundException e)
		{
			Log.d(TAG, "Feature " + feature + " have no params");
		}
		return sb.toString();
	}

	private void deleteLogcats(String logcatsDir)
	{
		Log.d(TAG, ".deleteLogcats: logcatsDir = " + logcatsDir);
		String[] files = new File(logcatsDir).list();
		if (files == null)
		{
			Log.w(TAG, "Directory " + logcatsDir + " is missing");
			return;
		}
		String expExt = Files.ext(getPrefs().getString(Param.LOGCAT_FILENAME_TEMPLATE));
		for (String fileName : files)
		{
			if (expExt.equals(Files.ext(fileName)))
			{
				Log.i(TAG, "Deleting " + fileName + " in " + logcatsDir);
				new File(logcatsDir, fileName).delete();
			}
		}
	}

	private String uploadLogcatFile(String logcatFilePath)
	{
		String url = getPrefs().getString(Param.CRASHLOG_SERVER_URL);
		final Bundle uploadParams = new Bundle();
		uploadParams.putString(UploadService.Extras.URL.name(), url);
		uploadParams.putString(UploadService.Extras.CA_CERT_PATH.name(),
		        getPrefs().getString(Param.CRASHLOG_SERVER_CA_CERT_PATH));
		uploadParams.putString(UploadService.Extras.USERNAME.name(),
		        getPrefs().getString(Param.CRASHLOG_SERVER_USERNAME));
		uploadParams.putString(UploadService.Extras.PASSWORD.name(),
		        getPrefs().getString(Param.CRASHLOG_SERVER_PASSWORD));
		uploadParams.putString(UploadService.Extras.LOCAL_FILE.name(), logcatFilePath);

		final FeatureInternet featureInternet = (FeatureInternet) Environment.getInstance().getFeatureScheduler(
		        FeatureName.Scheduler.INTERNET);
		if (featureInternet != null)
		{
			featureInternet.getEventMessenger().register(new EventReceiver()
			{
				@Override
				public void onEvent(int msgId, Bundle bundle)
				{
					final EventReceiver _this = this;
					featureInternet.uploadFile(uploadParams, new OnResultReceived()
					{
						@Override
						public void onReceiveResult(FeatureError result, Object object)
						{
							if (result.isError())
								Log.e(TAG, ".uploadFile:onReceiveResult: " + result);
							featureInternet.getEventMessenger().unregister(_this, FeatureInternet.ON_CONNECTED);
						}
					});
				}
			}, FeatureInternet.ON_CONNECTED);
		}
		else
		{
			Log.w(TAG, "FeatureInternet is required to send logcats on server");
		}
		if (url.charAt(url.length() - 1) != '/')
			url = url + '/';
		return url + Files.baseName(logcatFilePath);
	}

	private Stack<String> saveLogcat(String logcatFileName) throws IOException
	{
		BufferedReader logcatReader = new BufferedReader(new InputStreamReader(
		        _feature.Component.DEVICE.getLogcatInputStream()));
		Stack<String> logcatStack = new Stack<String>();
		String line;
		FileOutputStream fileOut = new FileOutputStream(logcatFileName);
		while ((line = logcatReader.readLine()) != null)
		{
			fileOut.write((line + "\n").getBytes());
			logcatStack.push(line);
		}
		fileOut.close();
		return logcatStack;
	}

	private String saveLogcatAndDetectSignalReason(String logcatFileName) throws IOException
	{
		// Pattern for Signal 7 = SIGBUS, Incorrect access to
		// memory (data misalignment)
		Pattern patternSignal7 = Pattern.compile(".*?Fatal signal 7.*?", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

		// Pattern for Signal 11 = SIGSEGV, Incorrect access to
		// memory (write to inaccessible memory)
		Pattern patternSignal11 = Pattern
		        .compile(".*?Fatal signal 11.*?", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

		// Pattern for Signal 15 = SIGTERM, Termination request.
		// JVM will exit normally.
		// Sample: Process 5035 terminated by signal (15)
		Pattern patternSignal15 = Pattern.compile(".*?Process\\s.*?\\sterminated by signal \\(15\\).*?",
		        Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

		// Pattern for Signal 9 = App kill
		// Sample: Sending signal. PID: 6364 SIG: 9
		Pattern patternSignal9 = Pattern.compile(".*?Sending signal\\.\\sPID:\\s.*?\\sSIG:\\s9.*?", Pattern.MULTILINE
		        | Pattern.CASE_INSENSITIVE);

		Stack<String> logcatStack = saveLogcat(logcatFileName);
		while (!logcatStack.isEmpty())
		{
			String line = logcatStack.pop();

			// Search for crashes resulting from signals 9 and 15
			Matcher matcher = patternSignal9.matcher(line);
			if (matcher.find())
			{
				return "signal 9: kill from shell";
			}

			matcher = patternSignal15.matcher(line);
			if (matcher.find())
			{
				return "signal 15: VM terminated the application";
			}

			matcher = patternSignal7.matcher(line);
			if (matcher.find())
			{
				return "signal 7: data misalignment";
			}

			matcher = patternSignal11.matcher(line);
			if (matcher.find())
			{
				return "signal 11: write to inaccessible memory";
			}
		}
		return null;
	}

	/**
	 * Composes new logcat file name
	 *
	 * @return logcat file name to upload on the server
	 */
	private String newLogcatName()
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
		return getPrefs().getString(Param.LOGCAT_FILENAME_TEMPLATE, substitute);
	}
}

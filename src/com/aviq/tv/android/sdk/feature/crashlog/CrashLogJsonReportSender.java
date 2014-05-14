package com.aviq.tv.android.sdk.feature.crashlog;

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.acra.ACRA;
import org.acra.ACRAConfiguration;
import org.acra.ReportField;
import org.acra.collector.CrashReportData;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;
import org.acra.util.JSONReportBuilder.JSONReportException;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Environment.Param;
import com.aviq.tv.android.sdk.core.Log;

public class CrashLogJsonReportSender implements ReportSender
{
	private static final String TAG = CrashLogJsonReportSender.class.getSimpleName();

	public static final String REPORT_NAME_TEMPLATE = "%s-%s-%s-%s-%s-%d.crashlog";
	public static final String LOGCAT_NAME_TEMPLATE = "%s-%s-%s-%s-%s-%d.log";

	private final String mReportNameTemplate;
	private final String mLogcatNameTemplate;
	private final Context mContext;
	private String _logcat;
	private String _logcatFileName;

	public CrashLogJsonReportSender(Context context)
	{
		mContext = context;
		mReportNameTemplate = REPORT_NAME_TEMPLATE;
		mLogcatNameTemplate = LOGCAT_NAME_TEMPLATE;

		String pkg = context.getPackageName();
		int pos = pkg.lastIndexOf('.');
	}

	@Override
	public void send(CrashReportData report) throws ReportSenderException
	{
		String boxId = "";
//		String appVersionCode = "";
		String userCrashDate = "";
		String brandName = "";
		String customer = "";
		Random rnd = new Random();
		int randomNum = rnd.nextInt(1000);

		Set<Entry<ReportField, String>> entrySet = report.entrySet();
		for (Entry<ReportField, String> entry : entrySet)
		{
			ReportField reportField = entry.getKey();
			String value = entry.getValue();

			// Appends some extra stuff to the "CUSTOM_DATA" field

//			Does not work when using ACRA json generation later
//			if (ReportField.CUSTOM_DATA.equals(reportField))
//			{
//				ACRA.getErrorReporter().putCustomData("TOTAL_MEMORY", "" + Runtime.getRuntime().maxMemory());
//				ACRA.getErrorReporter().putCustomData("AVAILABLE_MEMORY", "" + Runtime.getRuntime().freeMemory());
//			}

			// Get some values needed for the file name of the log

//			if (ReportField.APP_VERSION_CODE.equals(reportField))
//			{
//				appVersionCode = value;
//			}
//			else
			if (ReportField.USER_CRASH_DATE.equals(reportField))
			{
				// Expected format: 2013-02-04T16:02:32.000+01:00
				userCrashDate = value.replaceAll("-", ".").replaceAll(":", ".").replace('T', '_');
				int pos = userCrashDate.indexOf('+');
				if (pos > -1)
				{
					userCrashDate = userCrashDate.substring(0, pos);
				}
				if (userCrashDate.equals(""))
				{
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd_kk.mm.ss.S", Locale.GERMAN);
					userCrashDate = sdf.format(new Date(System.currentTimeMillis()));
				}
			}
			else if (ReportField.CUSTOM_DATA.equals(reportField))
			{
				// Find (w/o the quotes): "BOX_ID = 902B34F69D99\n"
				Pattern patternBoxId = Pattern.compile("BOX_ID\\s.*?=\\s.*?(\\w+)\\s.*?");
				Matcher matcherBoxId = patternBoxId.matcher(value);
				if (matcherBoxId.find())
				{
					boxId = matcherBoxId.group(1).trim();
				}
				if (boxId == null || boxId.equals("") || boxId.equalsIgnoreCase("null"))
					boxId = "000000000000";

				// Find (w/o the quotes): "CUSTOMER = some_customer\n"
				Pattern patternCustomer = Pattern.compile("CUSTOMER\\s.*?=\\s.*?(\\w+)\\s.*?");
				Matcher matcherCustomer = patternCustomer.matcher(value);
				if (matcherCustomer.find())
				{
					customer = matcherCustomer.group(1).trim();
				}
				if (customer == null || customer.equals("") || customer.equalsIgnoreCase("null"))
					customer = "customer";

				// Find (w/o the quotes): "BRAND = some_brand\n"
				Pattern patternBrand = Pattern.compile("BRAND\\s.*?=\\s.*?(\\w+)\\s.*?");
				Matcher matcherBrand = patternBrand.matcher(value);
				if (matcherBrand.find())
				{
					brandName = matcherBrand.group(1).trim();
				}
				if (brandName == null || brandName.equals("") || brandName.equalsIgnoreCase("null"))
					brandName = "brand";
			}
		}

		// Generate the report's file name.
		Environment env = Environment.getInstance();
		String buildType = env.getPrefs().getString(Param.RELEASE);

		String reportFileName = String.format(mReportNameTemplate, buildType, customer, brandName, boxId,
		        userCrashDate, randomNum);

		_logcatFileName = String.format(mLogcatNameTemplate, buildType, customer, brandName, boxId, userCrashDate,
		        randomNum);

		try
		{
			String data = getDataString(report);

			// Send the logcat to the server
			sendData(_logcatFileName, _logcat, "text/plain");

			// Send the report to the server
			String contentType = ACRA.getConfig().reportType().getContentType();
			sendData(reportFileName, data, contentType);
		}
		catch (JSONReportException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
        catch (JSONException e)
        {
        	Log.e(TAG, e.getMessage(), e);
        }

		System.gc();
	}

	public static void sendData(String reportFileName, String reportAsString, String contentType)
	{
		try
		{
			URL reportUrl = new URL(ACRA.getConfig().formUri());
			Log.d(TAG, "Connect to " + reportUrl.toString());

			final String login = ACRAConfiguration.isNull(ACRA.getConfig().formUriBasicAuthLogin()) ? null : ACRA
			        .getConfig().formUriBasicAuthLogin();
			final String password = ACRAConfiguration.isNull(ACRA.getConfig().formUriBasicAuthPassword()) ? null : ACRA
			        .getConfig().formUriBasicAuthPassword();

			String method = ACRA.getConfig().httpMethod().name();

			final HttpRequest request = new HttpRequest();
			request.setConnectionTimeOut(ACRA.getConfig().connectionTimeout());
			request.setSocketTimeOut(ACRA.getConfig().socketTimeout());
			request.setMaxNrRetries(ACRA.getConfig().maxNumberOfRequestRetries());
			request.setLogin(login);
			request.setPassword(password);
			request.setHeaders(ACRA.getConfig().getHttpHeaders());

			if (ACRA.getConfig().disableSSLCertValidation())
				request.disableSSLCertValidation();
			else
				request.enableSSLCertValidation();

			if ("PUT".equalsIgnoreCase(method))
				reportUrl = new URL(reportUrl.toString() + '/' + reportFileName);

			request.send(reportUrl, method, reportAsString, contentType);

//try
//{
//	FileOutputStream fos = new FileOutputStream(Environment.getInstance().getFilesDir() + "/" + reportFileName);
//	fos.write(reportAsString.getBytes());
//	fos.close();
//}
//catch (Exception e)
//{
//	Log.e(TAG, e.getMessage(), e);
//}
		}
		catch (IOException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
	}

	public static String getTimeAsISO(long timestampMillis)
	{
		// TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ");
		// df.setTimeZone(tz);
		String timeAsISO = df.format(timestampMillis);
		return timeAsISO;
	}

	private String getDataString(CrashReportData report) throws JSONReportException, JSONException
	{
		JSONObject dataObject = report.toJSON();

		// Move elements to another location in dataObject

		JSONObject device = new JSONObject(dataObject.getJSONObject("CUSTOM_DATA").remove("device").toString());
		dataObject.put("device", device);

		JSONObject event = new JSONObject(dataObject.getJSONObject("CUSTOM_DATA").remove("event").toString());
		event.put("timestamp", getTimeAsISO(System.currentTimeMillis()));

		JSONObject logcat = new JSONObject();
		logcat.put("file", _logcatFileName);
		event.put("logcat", logcat);

		dataObject.put("event", event);

		// Remove logcat. It will be handled separately.
		_logcat = dataObject.remove("LOGCAT").toString();

		// Remove unwanted elements

		dataObject.remove("ENVIRONMENT");
		dataObject.remove("INITIAL_CONFIGURATION");
		dataObject.remove("USER_EMAIL");
		dataObject.remove("DEVICE_FEATURES");
		dataObject.remove("SETTINGS_SECURE");
		dataObject.remove("SETTINGS_SYSTEM");
		dataObject.remove("CRASH_CONFIGURATION");
		dataObject.remove("BUILD");
		dataObject.remove("DISPLAY");

		return dataObject.toString();
	}
}

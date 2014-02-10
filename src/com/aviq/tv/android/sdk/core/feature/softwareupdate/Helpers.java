package com.aviq.tv.android.sdk.core.feature.softwareupdate;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import android.content.pm.PackageManager.NameNotFoundException;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.utils.HttpException;
import com.aviq.tv.android.sdk.utils.TextUtils;

class Helpers
{
	private static final String TAG = Helpers.class.getSimpleName();

	private static final String MAC_ADDRESS_FILE = "/sys/class/net/eth0/address";
	private static int CONNECT_TIMEOUT = 60 * 1000;
	private static int READ_TIMEOUT = 60 * 1000;
	private static final int MAX_REDIRECTS = 10;

	public static String readMacAddress() throws FileNotFoundException
	{
		FileInputStream fis = new FileInputStream(MAC_ADDRESS_FILE);
		String macAddress = TextUtils.inputSteamToString(fis);
		macAddress = macAddress.substring(0, 17);
		return macAddress.replace(":", "").toUpperCase();
	}

	public static String parseAppVersion() throws NameNotFoundException
	{
		String version = Environment.getInstance().getContext().getPackageManager()
		        .getPackageInfo(Environment.getInstance().getContext().getPackageName(), 0).versionName;
		int dotIdx = version.lastIndexOf('.');
		if (dotIdx >= 0)
		{
			version = version.substring(dotIdx + 1);
		}
		return version;
	}

	// Quick solution to bypass SSL verification
	public static void trustAllHosts()
	{
		// create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[]
		{ new X509TrustManager()
		{
			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers()
			{
				return new java.security.cert.X509Certificate[]
				{};
			}

			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
			{
			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
			{
			}
		} };

		// install the all-trusting trust manager
		try
		{
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static HttpURLConnection openHttpConnection(URL url, RequestMethod method, Proxy proxy,
	        RedirectCallback redirectCallback, int recurseCounter) throws IOException
	{
		HttpURLConnection conn;

		if (url.getProtocol().toLowerCase().equals("https"))
		{
			trustAllHosts();

			HttpsURLConnection https = null;
			if (proxy != null)
				https = (HttpsURLConnection) url.openConnection(proxy);
			else
				https = (HttpsURLConnection) url.openConnection();

			https.setHostnameVerifier(new HostnameVerifier()
			{
				@Override
				public boolean verify(String hostname, SSLSession session)
				{
					return true;
				}
			});
			conn = https;
		}
		else
		{
			if (proxy != null)
				conn = (HttpURLConnection) url.openConnection(proxy);
			else
				conn = (HttpURLConnection) url.openConnection();
		}

		/*
		 * iss: this fixes exception java.util.zip.GZIPInputStream and probably
		 * doesn't break anything else ...
		 */
		conn.setRequestProperty("Accept-Encoding", "");

		conn.setRequestMethod(method._name);
		conn.setConnectTimeout(CONNECT_TIMEOUT);
		conn.setReadTimeout(READ_TIMEOUT);

		// Do not follow redirects in case callback is not set to handle it
		// manually.
		conn.setInstanceFollowRedirects(redirectCallback == null);
		conn.connect();
		int responseCode = conn.getResponseCode();
		Log.i(TAG, "`" + url + "' -> HTTP " + responseCode);

		if (redirectCallback != null && (responseCode >= 301 && responseCode <= 303))
		{
			// notify callback with the redirect location and status code
			String location = conn.getHeaderField("Location");
			if (responseCode == 301 && !url.toString().startsWith("http://aviq-abmp-wilmaa.dyndns.org"))
			{
				// notify caller for persistent redirect
				Log.i(TAG, "Notify persistent redirect to " + location);
				redirectCallback.onRedirect(location);
			}
			conn.disconnect();

			if (recurseCounter < MAX_REDIRECTS)
			{
				// redirect to the new location
				Log.i(TAG, "Recurse redirect " + (recurseCounter + 1) + " times");
				return openHttpConnection(new URL(location), method, proxy, redirectCallback, recurseCounter + 1);
			}
			else
			{
				HttpException httpExp = new HttpException(responseCode);
				httpExp.setRedirectsCount(recurseCounter);
				throw httpExp;
			}
		}
		else if (responseCode != HttpURLConnection.HTTP_OK)
		{
			throw new HttpException(responseCode);
		}
		return conn;
	}

	public static enum RequestMethod
	{
		GET("GET"), POST("POST"), HEAD("HEAD"), OPTIONS("OPTIONS"), PUT("PUT"), DELETE("DELETE"), TRACE("TRACE"), DEFAULT(
		        "GET");

		private final String _name;

		private RequestMethod(String name)
		{
			_name = name;
		}
	}

	public static interface RedirectCallback
	{
		/**
		 * Connection redirect callback
		 *
		 * @param location
		 *            to the redirected location
		 */
		public void onRedirect(String location);
	}
}

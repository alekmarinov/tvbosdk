/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    DownloadService.java
 * Author:      alek
 * Date:        3 Dec 2013
 * Description: IntentService performing file download
 */
package com.aviq.tv.android.sdk.feature.internet;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.os.StatFs;

import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.service.BaseService;
import com.aviq.tv.android.sdk.utils.Files;

/**
 * IntentService performing file download
 */
public class DownloadService extends BaseService
{
	private static final String TAG = DownloadService.class.getSimpleName();
	private static final int CONNECT_TIMEOUT = 60 * 1000;
	private static final int READ_TIMEOUT = 60 * 1000;
	private static final int BUFFER_SIZE = 10 * 8192;
	private static final int ONE_MEGABYTE = 1024 * 1024;

	public static final int DOWNLOAD_PROGRESS = EventMessenger.ID("DOWNLOAD_PROGRESS");
	public static final int DOWNLOAD_SUCCESS = EventMessenger.ID("DOWNLOAD_SUCCESS");
	public static final int DOWNLOAD_FAILED = EventMessenger.ID("DOWNLOAD_FAILED");
	public static final int DOWNLOAD_CANCELLED = EventMessenger.ID("DOWNLOAD_CANCELLED");
	public static final int DOWNLOAD_OUT_OF_FREE_SPACE = EventMessenger.ID("DOWNLOAD_OUT_OF_FREE_SPACE");

	/**
	 * Service extras
	 */
	public enum Extras
	{
		URL, LOCAL_FILE, IS_COMPUTE_MD5, PROXY_HOST, PROXY_PORT, CONNECT_TIMEOUT, READ_TIMEOUT, BUFFER_SIZE, USERNAME, PASSWORD,
		/** time in milliseconds to sleep after saving each downloaded buffer */
		DOWNLOAD_DELAY
	}

	/**
	 * Service result extras
	 */
	public enum ResultExtras
	{
		PROGRESS, MD5, BYTES_DOWNLOADED, BYTES_TOTAL, EXCEPTION, DOWNLOAD_RATE_MB_PER_SEC, TOTAL_TIME, FREE_SPACE, REQUIRED_SPACE, REASON
	}

	private static boolean _cancelService = false;

	public DownloadService()
	{
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent intent, ResultReceiver resultReceiver)
	{
		Log.i(TAG, ".onHandleIntent");

		_cancelService = false;

		BufferedInputStream inputStream = null;
		HttpURLConnection conn = null;
		FileOutputStream outputStream = null;
		int result = DOWNLOAD_FAILED;
		Bundle resultData = new Bundle();
		try
		{
			final String username = intent.getStringExtra(Extras.USERNAME.name());
			final String password = intent.getStringExtra(Extras.PASSWORD.name());

			if (username != null && password != null)
			{
				Authenticator.setDefault(new Authenticator()
				{
					@Override
					protected PasswordAuthentication getPasswordAuthentication()
					{
						return new PasswordAuthentication(username, password.toCharArray());
					}
				});
			}

			String fileUrl = intent.getStringExtra(Extras.URL.name());
			String localFile = intent.getStringExtra(Extras.LOCAL_FILE.name());
			URL url = new URL(fileUrl);
			Log.i(TAG, "Downloading " + fileUrl + " to " + localFile);
			long downloadDelay = intent.getLongExtra(Extras.DOWNLOAD_DELAY.name(), 0);

			// Bypass SSL verification
			trustAllHosts();

			// prepare proxy settings
			String proxyHost = intent.getStringExtra(Extras.PROXY_HOST.name());
			int proxyPort = intent.getIntExtra(Extras.PROXY_PORT.name(), 0);
			if (proxyPort != 0)
			{
				InetSocketAddress proxySockAddr = new InetSocketAddress(proxyHost, proxyPort);
				Proxy proxy = new Proxy(Proxy.Type.HTTP, proxySockAddr);
				Log.i(TAG, "Using proxy " + proxyHost + ":" + proxyPort);

				// open connection with proxy
				conn = (HttpURLConnection) url.openConnection(proxy);
			}
			else
			{
				// open connection
				conn = (HttpURLConnection) url.openConnection();
			}

			// setup connection
			conn.setRequestProperty("Accept-Encoding", ""); // fixes exception
			                                                // java.util.zip.GZIPInputStream

			conn.setRequestMethod("GET");
			conn.setConnectTimeout(intent.getIntExtra(Extras.CONNECT_TIMEOUT.name(), CONNECT_TIMEOUT));
			conn.setReadTimeout(intent.getIntExtra(Extras.READ_TIMEOUT.name(), READ_TIMEOUT));

			// do connect
			conn.connect();

			int responseCode = conn.getResponseCode();
			Log.i(TAG, "`" + url + "' -> HTTP " + responseCode);

			int contentLength = conn.getHeaderFieldInt("Content-Length", -1);

			// prepare md5 digest
			boolean isComputeMd5 = intent.getBooleanExtra(Extras.IS_COMPUTE_MD5.name(), false);
			MessageDigest md5 = null;
			if (isComputeMd5)
			{
				// FIXME: Extend with ability to request various check sum
				// algorithms, e.g. SHA
				Log.i(TAG, "Requested md5 check sum calculation");
				md5 = MessageDigest.getInstance("MD5");
			}
			int bufSize = intent.getIntExtra(Extras.BUFFER_SIZE.name(), BUFFER_SIZE);

			// prepare input stream
			inputStream = new BufferedInputStream(conn.getInputStream(), bufSize);

			// prepare destination directory
			String dirName = Files.dirName(this, localFile);
			File dir = new File(dirName);
			if (!dir.exists())
				dir.mkdirs();

			localFile = Files.filePath(this, localFile);

			// create output stream
			File partFile = new File(localFile + ".part");
			File targetFile = new File(localFile);
			outputStream = new FileOutputStream(partFile);

			long downloadStart = System.currentTimeMillis();
			long duration = 0;
			double downloadRateMbPerSec = 0.0;
			int total = conn.getContentLength();
			byte data[] = new byte[bufSize];
			int count;
			int bytesWritten = 0;
			Bundle progressData = new Bundle();
			// downloading
			long lastIterTime = System.currentTimeMillis();
			while ((count = inputStream.read(data, 0, bufSize)) != -1)
			{
				if (_cancelService)
					break;

				if (md5 != null)
					md5.update(data, 0, count);
				outputStream.write(data, 0, count);
				if (count > 0)
				{
					try
                    {
	                    Thread.sleep(downloadDelay);
                    }
                    catch (InterruptedException e)
                    {
                    	Log.e(TAG, e.getMessage(), e);
                    }
					bytesWritten += count;
				}

				duration = System.currentTimeMillis() - downloadStart;
				downloadRateMbPerSec = (bytesWritten / ((double) duration / 1000)) / ONE_MEGABYTE;

				if (System.currentTimeMillis() - lastIterTime > 300)
				{
					// send progress events back to receiver
					progressData.putFloat(ResultExtras.PROGRESS.name(), (float) bytesWritten / total);
					progressData.putFloat(ResultExtras.BYTES_DOWNLOADED.name(), bytesWritten);
					progressData.putFloat(ResultExtras.BYTES_TOTAL.name(), total);
					progressData.putDouble(ResultExtras.DOWNLOAD_RATE_MB_PER_SEC.name(), downloadRateMbPerSec);
					progressData.putLong(ResultExtras.TOTAL_TIME.name(), duration);
					resultReceiver.send(DOWNLOAD_PROGRESS, progressData);
					lastIterTime = System.currentTimeMillis();
				}

				if (contentLength > 0)
				{
					long freeSpace = getFreeSpace(partFile.getParent());
					long requiredSpace = contentLength - bytesWritten;
					if (freeSpace < requiredSpace)
					{
						Log.w(TAG, "Running out of free space... free space left = " + freeSpace + ", require space = "
						        + requiredSpace);

						Bundle outOfSpaceData = new Bundle();
						outOfSpaceData.putLong(ResultExtras.FREE_SPACE.name(), freeSpace);
						outOfSpaceData.putLong(ResultExtras.REQUIRED_SPACE.name(), requiredSpace);
						resultReceiver.send(DOWNLOAD_OUT_OF_FREE_SPACE, outOfSpaceData);
					}
				}
			}

			if (!_cancelService)
			{
				// Service completed OK

				duration = System.currentTimeMillis() - downloadStart;

				// send full progress event to fill up the progress bars
				progressData.putFloat(ResultExtras.PROGRESS.name(), 1.0f);
				progressData.putFloat(ResultExtras.BYTES_DOWNLOADED.name(), total);
				progressData.putFloat(ResultExtras.BYTES_TOTAL.name(), total);
				progressData.putDouble(ResultExtras.DOWNLOAD_RATE_MB_PER_SEC.name(), downloadRateMbPerSec);
				progressData.putLong(ResultExtras.TOTAL_TIME.name(), duration);
				resultReceiver.send(DOWNLOAD_PROGRESS, progressData);

				// resultData.putDouble(ResultExtras.DOWNLOAD_RATE_MB_PER_SEC.name(),
				// downloadRateMbPerSec);
				resultData.putAll(progressData);
				Log.i(TAG, bytesWritten + " bytes in " + duration + " ms downloaded from " + url + ", download rate = "
				        + downloadRateMbPerSec + " MB/sec");

				if (md5 != null)
				{
					// format md5 string
					byte[] digest = md5.digest();
					StringBuffer downloadedMd5 = new StringBuffer();
					for (int i = 0; i < digest.length; i++)
					{
						downloadedMd5.append(Integer.toString((digest[i] & 0xFF) + 0x100, 16).substring(1));
					}
					Log.i(TAG, "Computed MD5 sum: " + downloadedMd5);
					resultData.putString(ResultExtras.MD5.name(), downloadedMd5.toString());
				}

				// delete if file with the same name already exists
				if (targetFile.exists())
				{
					targetFile.delete();
					Log.i(TAG, "Deleting previous file: " + targetFile.getAbsolutePath());
				}

				// rename file to requested name
				partFile.renameTo(targetFile);

				// finish success
				Log.i(TAG, "Download success: " + targetFile);
				result = DOWNLOAD_SUCCESS;
			}
			else
			{
				// Service is cancelled

				result = DOWNLOAD_CANCELLED;

				boolean isDeleted = partFile.delete();
				Log.i(TAG, "Deleting temporary file: " + partFile.getAbsolutePath() + " --> "
				        + (isDeleted ? "succeeded" : "failed"));
			}
		}
		catch (MalformedURLException e)
		{
			Log.e(TAG, e.getMessage(), e);
			resultData.putSerializable(ResultExtras.EXCEPTION.name(), e);
		}
		catch (IOException e)
		{
			Log.e(TAG, e.getMessage(), e);
			resultData.putSerializable(ResultExtras.EXCEPTION.name(), e);
		}
		catch (NoSuchAlgorithmException e)
		{
			Log.e(TAG, e.getMessage(), e);
			resultData.putSerializable(ResultExtras.EXCEPTION.name(), e);
		}
		finally
		{
			if (conn != null)
			{
				conn.disconnect();
			}
			Files.closeQuietly(inputStream, TAG);
			Files.closeQuietly(outputStream, TAG);

			// send download status to result receiver
			resultReceiver.send(result, resultData);
			Log.i(TAG, ".onHandleIntent: finished");
		}
	}

	private long getFreeSpace(String path)
	{
		StatFs statFs = new StatFs(path);
		long free = ((long) statFs.getAvailableBlocks() * (long) statFs.getBlockSize());
		return free;
	}

	// quick solution to bypass SSL verification
	private static void trustAllHosts()
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

	public static void stopIfRunning()
	{
		_cancelService = true;
	}
}

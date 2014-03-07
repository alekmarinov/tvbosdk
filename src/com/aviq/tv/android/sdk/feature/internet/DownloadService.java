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
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;

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
	private static final int BUFFER_SIZE = 10 * 100 * 8192;

	public static final int DOWNLOAD_PROGRESS = EventMessenger.ID();
	public static final int DOWNLOAD_SUCCESS = EventMessenger.ID();
	public static final int DOWNLOAD_FAILED = EventMessenger.ID();

	/**
	 * Service extras
	 */
	public enum Extras
	{
		URL, LOCAL_FILE, IS_COMPUTE_MD5, PROXY_HOST, PROXY_PORT, CONNECT_TIMEOUT, READ_TIMEOUT, BUFFER_SIZE
	}

	/**
	 * Service result extras
	 */
	public enum ResultExtras
	{
		PROGRESS, MD5, BYTES_DOWNLOADED, BYTES_TOTAL, EXCEPTION
	}

	public DownloadService()
	{
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent intent, ResultReceiver resultReceiver)
	{
		BufferedInputStream inputStream = null;
		HttpURLConnection conn = null;
		FileOutputStream outputStream = null;
		int result = DOWNLOAD_FAILED;
		Bundle resultData = new Bundle();
		try
		{
			String fileUrl = intent.getStringExtra(Extras.URL.name());
			String localFile = intent.getStringExtra(Extras.LOCAL_FILE.name());
			URL url = new URL(fileUrl);
			Log.i(TAG, "Downloading " + fileUrl + " to " + localFile);

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

			// prepare md5 digest
			boolean isComputeMd5 = intent.getBooleanExtra(Extras.IS_COMPUTE_MD5.name(), false);
			MessageDigest md5 = null;
			if (isComputeMd5)
			{
				// FIXME: Extend with ability to request various check sum algorithms, e.g. SHA
				Log.i(TAG, "Requested md5 check sum calculation");
				md5 = MessageDigest.getInstance("MD5");
			}
			int bufSize = intent.getIntExtra(Extras.BUFFER_SIZE.name(), BUFFER_SIZE);

			long downloadStart = System.currentTimeMillis();
			// prepare input stream
			inputStream = new BufferedInputStream(conn.getInputStream(), bufSize);

			// prepare destination directory
			String dirName = Files.dirName(localFile);
			if (!TextUtils.isEmpty(dirName))
			{
				File dir = new File(getFilesDir(), dirName);
				if (!dir.exists())
					dir.mkdir();
			}

			// create output stream
			File partFile = new File(getFilesDir(), localFile + ".part");
			outputStream = new FileOutputStream(partFile);

			int total = conn.getContentLength();
			byte data[] = new byte[bufSize];
			int count;
			int bytesWritten = 0;
			Bundle progressData = new Bundle();
			// downloading
			while ((count = inputStream.read(data, 0, bufSize)) != -1)
			{
				if (md5 != null)
					md5.update(data, 0, count);
				outputStream.write(data, 0, count);
				bytesWritten += count;

				// send progress events back to receiver
				progressData.putFloat(ResultExtras.PROGRESS.name(), (float) bytesWritten / total);
				progressData.putFloat(ResultExtras.BYTES_DOWNLOADED.name(), bytesWritten);
				progressData.putFloat(ResultExtras.BYTES_TOTAL.name(), total);
				resultReceiver.send(DOWNLOAD_PROGRESS, progressData);
			}
			long duration = System.currentTimeMillis() - downloadStart;
			Log.i(TAG, bytesWritten + " bytes in " + duration + " ms downloaded from " + url);

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

			File downloadedFile = new File(getFilesDir(), localFile);
			// delete if file with the same name already exists
			if (downloadedFile.exists())
				downloadedFile.delete();

			// rename file to requested name
			partFile.renameTo(downloadedFile);

			// finish success
			Log.i(TAG, "Download success: " + downloadedFile);
			result = DOWNLOAD_SUCCESS;
		}
		catch (MalformedURLException e)
		{
			resultData.putSerializable(ResultExtras.EXCEPTION.name(), e);
		}
		catch (IOException e)
		{
			resultData.putSerializable(ResultExtras.EXCEPTION.name(), e);
		}
		catch (NoSuchAlgorithmException e)
		{
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
		}
	}
}
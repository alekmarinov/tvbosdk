package com.aviq.tv.android.sdk.feature.softwareupdate;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.service.BaseService;
import com.aviq.tv.android.sdk.feature.softwareupdate.Helpers.RedirectCallback;
import com.aviq.tv.android.sdk.feature.softwareupdate.Helpers.RequestMethod;
import com.aviq.tv.android.sdk.utils.Files;
import com.aviq.tv.android.sdk.utils.HttpException;

public class DownloadUpdateService extends IntentService
{
	private static final String TAG = DownloadUpdateService.class.getSimpleName();

	public static final String PARAM_SERVER_URL = "SERVER_URL";
	public static final String PARAM_FILENAME = "FILENAME";
	public static final String PARAM_FILESIZE = "FILESIZE";

	public static final int DOWNLOAD_BUF_SIZE = 10 * 100 * 8192;
	public static int CONNECT_TIMEOUT = 60 * 1000;
	public static int READ_TIMEOUT = 60 * 1000;
	public static final int MAX_REDIRECTS = 10;

	private static final String UPDATE_DIR = "update";

	private ResultReceiver _resultReceiver;
	private String _abmpURL;
	private String _fileName;
	private long _fileSize;

	public DownloadUpdateService()
    {
	    super(TAG);
    }

	@Override
    protected void onHandleIntent(Intent intent)
    {
		String action = intent.getAction();
		Log.i(TAG, ".onHandleIntent: action = " + action);

		Bundle extras = intent.getExtras();
		if (extras != null)
		{
			_resultReceiver = (ResultReceiver) extras.get(BaseService.EXTRA_RESULT_RECEIVER);
			_resultReceiver.send(FeatureSoftwareUpdate.ON_UPDATE_DOWNLOAD_STARTED, null);

			_fileName = extras.getString(PARAM_FILENAME);
			_fileSize = extras.getLong(PARAM_FILESIZE);
			_abmpURL = extras.getString(PARAM_SERVER_URL);
		}

		try
		{
			// New software version available for download
			String downloadURL = _abmpURL;
			String md5URL = downloadURL + ".md5";

			Log.i(TAG, "New software version available, downloading from " + downloadURL);

			File dir = new File(getFilesDir(), UPDATE_DIR);
			if (!dir.exists())
				dir.mkdir();

			try
			{
				// Get MD5 file
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				downloadFile(new URL(md5URL), null, baos, null, null, null);
				String md5Expected = new String(baos.toByteArray(), 0, 32);

				_fileName = Files.baseName(_fileName);
				String filePath = UPDATE_DIR + "/" + _fileName;
				File file = new File(getFilesDir(), filePath);

				// Check if file is already downloaded
				if (!existsInDir(UPDATE_DIR, _fileName))
				{
					Log.i(TAG, filePath + " doesn't exists. Downloading...");

					Bundle params = new Bundle();
					params.putString(FeatureSoftwareUpdate.PARAM_DOWNLOAD_URL, downloadURL);

					if (_resultReceiver != null)
						_resultReceiver.send(FeatureSoftwareUpdate.ON_UPDATE_DOWNLOAD_STARTED, params);

					deletePreviousDownloads();

					// Downloading file to .part

					String partFilePath = filePath + ".part";
					FileOutputStream fout = new FileOutputStream(new File(getFilesDir(), partFilePath));
					MessageDigest md5 = MessageDigest.getInstance("MD5");

					final int[] lastProgress = new int[1];
					lastProgress[0] = -1;

					downloadFile(new URL(downloadURL), null, fout, new IProgressable()
					{
						@Override
						public void onProgress(float progress) throws InterruptedException
						{
							int nProgress = (int) (1000 * progress);
							if (nProgress > lastProgress[0])
							{
								Log.i(TAG, "Download progress " + (nProgress / 10) + "%");

								Bundle params = new Bundle();
								params.putFloat(FeatureSoftwareUpdate.PARAM_PROGRESS_AMOUNT, Float.valueOf(progress));

								if (_resultReceiver != null)
									_resultReceiver.send(FeatureSoftwareUpdate.ON_UPDATE_DOWNLOAD_PROGRESS, params);

								lastProgress[0] = nProgress;
							}
						}
					}, new Helpers.RedirectCallback()
					{
						@Override
						public void onRedirect(String location)
						{
							int boxPos = location.indexOf("/Box/");
							if (boxPos > 0)
							{
								location = location.substring(0, boxPos);
								_abmpURL = location;

								Bundle resultData = new Bundle();
								resultData.putString(FeatureSoftwareUpdate.PARAM_NEW_SERVER_CONFIG, location);
								_resultReceiver.send(FeatureSoftwareUpdate.ON_NEW_SERVER_CONFIG, resultData);
							}
							else
							{
								Log.e(TAG, "Redirected location `" + location + "' doesn't match */Box/* pattern");
							}
						}
					}, md5);
					fout.close();

					// Convert MD5 digest to string
					byte[] digest = md5.digest();
					StringBuffer md5Downloaded = new StringBuffer();
					for (int i = 0; i < digest.length; i++)
					{
						md5Downloaded.append(Integer.toString((digest[i] & 0xFF) + 0x100, 16).substring(1));
					}

					// Test if MD5 is valid
					Log.i(TAG, "Verifying MD5 sums " + md5Expected + " ?= " + md5Downloaded.toString());
					if (md5Expected.equalsIgnoreCase(md5Downloaded.toString()))
					{
						Log.i(TAG, "MD5 check ok");
					}
					else
					{
						throw new IOException("Invalid firmware checksum: " + "expected " + md5Expected + ", got "
						        + md5Downloaded);
					}

					// Check if downloaded file is valid.
					File partFile = new File(getFilesDir(), partFilePath);
					if (partFile.length() != _fileSize)
					{
						// Unexpected file size, removing it.
						String errMsg = "Invalid downloaded file size " + partFile.length() + ", expected " + _fileSize;
						Log.i(TAG, errMsg + ". Removing " + partFile.getAbsolutePath());

						partFile.delete();

						throw new IOException(errMsg);
					}
					else
					{
						// Rename downloaded file to omit .part suffix
						partFile.renameTo(file);

						Log.i(TAG, "Download finished. File `" + file.getAbsolutePath() + "' is ready for update");

						params.clear();
						params.putString(FeatureSoftwareUpdate.PARAM_DOWNLOAD_FILE, file.getAbsolutePath());

						if (_resultReceiver != null)
							_resultReceiver.send(FeatureSoftwareUpdate.ON_UPDATE_DOWNLOAD_FINISHED, params);
					}
				}
				else
				{
					Bundle params = new Bundle();
					params.putString(FeatureSoftwareUpdate.PARAM_DOWNLOAD_FILE, file.getAbsolutePath());

					if (_resultReceiver != null)
						_resultReceiver.send(FeatureSoftwareUpdate.ON_UPDATE_DOWNLOAD_FINISHED, params);
				}
			}
			catch (InterruptedException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
			catch (NoSuchAlgorithmException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
		}
		catch (Exception e)
		{
			Log.e(TAG, e.getMessage(), e);

			Bundle resultData = new Bundle();
			resultData.putString(FeatureSoftwareUpdate.PARAM_ERROR, e.getMessage());

			if (e instanceof HttpException && ((HttpException) e).getResponseCode() == 403)
			{
				resultData.putString(FeatureSoftwareUpdate.PARAM_ERROR_DETAILS, FeatureSoftwareUpdate.ERROR_HTTP_403);
			}

			if (_resultReceiver != null)
				_resultReceiver.send(FeatureSoftwareUpdate.ON_UPDATE_ERROR, intent.getExtras());
		}
    }

	private static void downloadFile(URL url, Proxy proxy, OutputStream fos, IProgressable progressable,
	        RedirectCallback redirectCallback, MessageDigest diggest) throws IOException, InterruptedException
	{
		Log.i(TAG, "Downloading " + url.toString());
		BufferedInputStream inputStream = null;
		InputStream is = null;

		HttpURLConnection conn = null;
		if (progressable != null)
			progressable.onProgress(0);
		try
		{
			long start = System.currentTimeMillis();
			conn = Helpers.openHttpConnection(url, RequestMethod.DEFAULT, proxy, redirectCallback, 0);
			is = conn.getInputStream();
			inputStream = new BufferedInputStream(is, DOWNLOAD_BUF_SIZE);
			int total = conn.getContentLength();
			byte data[] = new byte[DOWNLOAD_BUF_SIZE];
			int count;
			int totalWritten = 0;
			while ((count = inputStream.read(data, 0, DOWNLOAD_BUF_SIZE)) != -1)
			{
				if (diggest != null)
					diggest.update(data, 0, count);
				fos.write(data, 0, count);
				totalWritten += count;
				if (progressable != null)
					progressable.onProgress((float) totalWritten / total);

				// NOTE: Decomment the next line if download takes too much CPU.
				// Thread.sleep(10);
				if ((System.currentTimeMillis() - start) % 500 == 0)
					Log.d(TAG, "Downloaded " + totalWritten + " bytes");
			}
			long duration = System.currentTimeMillis() - start;
			Log.i(TAG, totalWritten + " bytes in " + duration + " ms downloaded from " + url);
		}
		finally
		{
			if (progressable != null)
				progressable.onProgress(1);
			if (conn != null)
			{
				conn.disconnect();
			}
			Files.closeQuietly(inputStream, TAG);
			Files.closeQuietly(is, TAG);
		}
	}

	private boolean existsInDir(String dirName, String fileName)
	{
		// collect existing files
		File dir = new File(getFilesDir(), dirName);
		String[] files = dir.list();
		if (files != null)
		{
			Log.e(TAG, "*** Checks if file " + fileName + " exists in " + dirName + " directory with " + files.length
			        + " files");
			for (String file : files)
			{
				Log.e(TAG, "*** " + file + " <?> " + fileName);
				if (file.equals(fileName))
					return true;
			}
		}
		else
		{
			Log.w(TAG, dir.getAbsolutePath() + " is not directory");
		}
		return false;
	}

	private void deletePreviousDownloads()
	{
		// Remove all existing files before the download
		String[] files = new File(getFilesDir(), UPDATE_DIR).list();
		if (files != null)
		{
			for (String delFileName : files)
			{
				boolean success = new File(getFilesDir(), UPDATE_DIR + "/" + delFileName).delete();
				Log.i(TAG, "Deleting file `" + UPDATE_DIR + "/" + delFileName
				        + (success ? "' succeeded" : "' failed"));
			}
		}
	}

	public interface IProgressable
	{
		/**
		 * Callback for tracking current work progress
		 *
		 * @param progress
		 *            the progress amount as a fraction
		 */
		void onProgress(float progress) throws InterruptedException;
	}
}

/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeaturePlayerRayV.java
 * Author:      zhelyazko
 * Date:        3 Feb 2014
 * Description: Component feature providing ticker widget's data
 */

package com.aviq.tv.android.sdk.feature.softwareupdate;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Scheduler;
import com.aviq.tv.android.sdk.core.feature.FeatureScheduler;
import com.aviq.tv.android.sdk.core.service.ServiceController;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;

/**
 * Component feature providing ticker widget's data
 */
public class FeatureSoftwareUpdate extends FeatureScheduler
{
	public static final String TAG = FeatureSoftwareUpdate.class.getSimpleName();

	public static final int ON_SOFTWARE_UPDATE_FOUND = EventMessenger.ID();

	public static final int CODE_NEW_SERVER_CONFIG = EventMessenger.ID();
	public static final String PARAM_NEW_SERVER_CONFIG = "NEW_SERVER_CONFIG";

	public static final int CODE_UPDATE_CHECK_RESULTS = EventMessenger.ID();
	public static final String PARAM_CURRENT_VERSION = "CURRENT_VERSION";
	public static final String PARAM_SERVER_VERSION = "SERVER_VERSION";
	public static final String PARAM_FILENAME = "FILENAME";
	public static final String PARAM_FILESIZE = "FILESIZE";
	public static final String PARAM_SOFTWARE_TYPE = "SOFTWARE_TYPE";
	public static final String PARAM_ISFORCED = "ISFORCED";
	public static final String PARAM_SERVER_BRAND = "SERVER_BRAND";

	public static final int CODE_UPDATE_ERROR = EventMessenger.ID();
	public static final String PARAM_ERROR = "ERROR";
	public static final String PARAM_ERROR_DETAILS = "ERROR_DETAILS";

	public static final int CODE_UPDATE_DOWNLOAD_STARTED = EventMessenger.ID();
	public static final String PARAM_DOWNLOAD_URL = "DOWNLOAD_URL";

	public static final int CODE_UPDATE_DOWNLOAD_FINISHED = EventMessenger.ID();
	public static final String PARAM_DOWNLOAD_FILE = "DOWNLOAD_FILE";

	public static final int CODE_UPDATE_DOWNLOAD_PROGRESS = EventMessenger.ID();
	public static final String PARAM_PROGRESS_AMOUNT = "PROGRESS_AMOUNT";
	public static final String PARAM_PROGRESS_SERVER_VERSION = "PROGRESS_SERVER_VERSION";

	public static final String ERROR_HTTP_403 = "HTTP_403";

	public enum Param
	{
		/**
		 * Registration brand of the device. It should be filled in by the client project.
		 */
		REGISTRATION_BRAND("zixi"),

		/**
		 * Schedule interval
		 */
		UPDATE_CHECK_INTERVAL(5 * 60 * 1000),

		/**
		 * URL for software updates. It should be set by the client application.
		 */
		ABMP_URL("http://aviq.dyndns.org:983");

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Scheduler.SOFTWARE_UPDATE).put(name(), value);
		}

		Param(String value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Scheduler.SOFTWARE_UPDATE).put(name(), value);
		}
	}

	public enum UserParam
	{
		/**
		 * Box category type (read-only purposes)
		 */
		UPGRADE_BOX_CATEGORY,

		/**
		 * File name of the downloaded software update.
		 */
		UPGRADE_FILE,

		/**
		 * Version of the software update downloaded.
		 */
		UPGRADE_VERSION,

		/**
		 * Brand for which a software update has been downloaded.
		 */
		UPGRADE_BRAND;
	}

	private OnFeatureInitialized _onFeatureInitialized;
	private boolean _hasUpdate;
	private boolean _updateDownloadStarted;
	private boolean _updateDownloadFinished;
	private boolean _initializing = true;
	private Bundle _updateCheckResultsBundle;

	public FeatureSoftwareUpdate()
	{
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		//super.initialize(onFeatureInitialized);
		_onFeatureInitialized = onFeatureInitialized;
		onSchedule(onFeatureInitialized);
	}

	@Override
	protected void onSchedule(OnFeatureInitialized onFeatureInitialized)
	{
		// Check for software updates
		Log.i(TAG, "Checking for software updates.");

		// Contact the server for a software update check
		checkForUpdate();

		// Schedule another update
		scheduleDelayed(getPrefs().getInt(Param.UPDATE_CHECK_INTERVAL));
	}

	@Override
	public Scheduler getSchedulerName()
	{
		return FeatureName.Scheduler.SOFTWARE_UPDATE;
	}

	public boolean hasUpdate()
	{
		// FIXME: remove when done testing
//		_hasUpdate = true;

		Log.i(TAG, ".hasUpdate: " + _hasUpdate);
		return _hasUpdate;
	}

	public boolean isUpdateDownloadStarted()
	{
		Log.i(TAG, ".isUpdateDownloadStarted: " + _updateDownloadStarted);
		// FIXME: remove when done testing
		return _updateDownloadStarted;
	}

	public boolean isUpdateDownloadFinished()
	{
		Log.i(TAG, ".isUpdateDownloadFinished: " + _updateDownloadFinished);
		// FIXME: remove when done testing
		return _updateDownloadFinished;
	}

	private void checkForUpdate()
	{
		Log.i(TAG, ".checkForUpdate");

		ServiceController serviceController = Environment.getInstance().getServiceController();

		Bundle params = new Bundle();

		serviceController.startService(UpdateCheckService.class, params).then(new OnResultReceived()
		{
			@Override
			public void onReceiveResult(int resultCode, Bundle resultData)
			{
				if (CODE_UPDATE_CHECK_RESULTS == resultCode)
				{
					_updateCheckResultsBundle = resultData;

					String currentVersion = resultData.getString(PARAM_CURRENT_VERSION);
					String serverVersion = resultData.getString(PARAM_SERVER_VERSION);
					String filename = resultData.getString(PARAM_FILENAME);
					long filesize = resultData.getLong(PARAM_FILESIZE);
					String softwareType = resultData.getString(PARAM_SOFTWARE_TYPE);
					boolean isForced = resultData.getBoolean(PARAM_ISFORCED);
					String brand = resultData.getString(PARAM_SERVER_BRAND);

					Log.i(TAG, ".checkForUpdate result: currentVersion = " + currentVersion + ", serverVersion = "
					        + serverVersion + ", filename = " + filename + ", filesize = " + filesize
					        + " softwareType = " + softwareType + ", isForced = " + isForced + ", brand = " + brand);

					// store box category type - for read only purposes
					Environment.getInstance().getUserPrefs().put(UserParam.UPGRADE_BOX_CATEGORY, softwareType);

					_hasUpdate = hasNewVersion(currentVersion, serverVersion, brand);
					if (_hasUpdate)
					{
						// Notify that new update exists
						getEventMessenger().trigger(ON_SOFTWARE_UPDATE_FOUND, resultData);

						// Start to download it in the background
						downloadUpdate();
					}

					if (_initializing)
					{
						_initializing = false;
//						_onFeatureInitialized.onInitialized(FeatureSoftwareUpdate.this, ResultCode.OK);

						// TODO try to delay in order to demo the splash; remove later
						getEventMessenger().postDelayed(new Runnable()
						{
							@Override
							public void run()
							{
								_hasUpdate = true;
								_onFeatureInitialized.onInitialized(FeatureSoftwareUpdate.this, ResultCode.OK);
							}
						}, 7000);
					}
				}
				else if (CODE_UPDATE_ERROR == resultCode)
				{
					// TODO
					Log.e(TAG, ".checkForUpdate failed due to an error");

					if (_initializing)
					{
						_initializing = false;

						// TODO try to delay in order to demo the splash; remove later
						getEventMessenger().postDelayed(new Runnable()
						{
							@Override
							public void run()
							{
								_hasUpdate = true;
								_onFeatureInitialized.onInitialized(FeatureSoftwareUpdate.this, ResultCode.GENERAL_FAILURE);
							}
						}, 7000);
					}
				}
				else if (CODE_NEW_SERVER_CONFIG == resultCode)
				{
					// TODO: store in prefs?
					Log.i(TAG, ".checkForUpdate: new server configuration found");
				}
			}
		});
	}

	public void downloadUpdate()
	{
		Log.i(TAG, ".downloadUpdate");

		ServiceController serviceController = Environment.getInstance().getServiceController();

		serviceController.startService(DownloadUpdateService.class, _updateCheckResultsBundle).then(
		        new OnResultReceived()
		        {
			        @Override
			        public void onReceiveResult(int resultCode, Bundle resultData)
			        {
				        if (CODE_UPDATE_DOWNLOAD_STARTED == resultCode)
				        {
					        // Download started
					        _updateDownloadStarted = true;
					        _updateDownloadFinished = false;
					        return;
				        }
				        else if (CODE_UPDATE_DOWNLOAD_FINISHED == resultCode)
				        {
					        // Download finished
					        _updateDownloadStarted = false;
					        _updateDownloadFinished = true;

					        // Update user preferences with necessary data

					        String updateFile = resultData.getString(PARAM_DOWNLOAD_FILE);
					        String updateVersion = _updateCheckResultsBundle.getString(PARAM_SERVER_VERSION);
					        String brand = _updateCheckResultsBundle.getString(PARAM_SERVER_BRAND);

					        Prefs userPrefs = Environment.getInstance().getUserPrefs();
					        userPrefs.put(UserParam.UPGRADE_FILE, updateFile);
					        userPrefs.put(UserParam.UPGRADE_VERSION, updateVersion);
					        userPrefs.put(UserParam.UPGRADE_BRAND, brand);
				        }
				        else if (CODE_UPDATE_DOWNLOAD_PROGRESS == resultCode)
				        {
					        // TODO
				        }
				        else if (CODE_UPDATE_ERROR == resultCode)
				        {
					        // TODO
				        	Log.e(TAG, ".downloadUpdate failed due to an error");
				        }
				        else if (CODE_NEW_SERVER_CONFIG == resultCode)
				        {
					        // TODO: store in prefs?
				        	Log.i(TAG, ".downloadUpdate: new server configuration found");
				        }
			        }
		        });
	}

	private boolean hasNewVersion(String currentVersion, String otherVersion, String brand)
	{
		if (currentVersion == null || otherVersion == null)
			throw new IllegalArgumentException("Arguments cannot be null");

		// check if BoxID is reassigned to new brand to allow FW update
		boolean isNewBrand = !(TextUtils.isEmpty(brand) || getPrefs().getString(Param.REGISTRATION_BRAND)
		        .equalsIgnoreCase(brand));
		if (isNewBrand)
			return true;

		int left = 0;
		int right = otherVersion.length();

		if (right == 0)
			return true;

		while (left < right && !Character.isDigit(otherVersion.charAt(0)))
			left++;
		while (right > left && !Character.isDigit(otherVersion.charAt(right - 1)))
			right--;

		if (left == right)
			right++;

		if (left == otherVersion.length())
			return true;

		otherVersion = otherVersion.substring(left, right);

		return Integer.valueOf(otherVersion) > Integer.valueOf(currentVersion);
	}
}

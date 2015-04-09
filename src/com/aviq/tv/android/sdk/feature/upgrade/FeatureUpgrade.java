/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureUpgrade.java
 * Author:      zhelyazko
 * Date:        3 Feb 2014
 * Description: Software upgrade scheduler feature
 */

package com.aviq.tv.android.sdk.feature.upgrade;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.os.Bundle;
import android.os.RecoverySystem;
import android.text.TextUtils;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Scheduler;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.FeatureScheduler;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;
import com.aviq.tv.android.sdk.core.service.ServiceController;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.internet.DownloadService;
import com.aviq.tv.android.sdk.feature.internet.FeatureInternet;
import com.aviq.tv.android.sdk.feature.internet.FeatureInternet.ResultExtras;
import com.aviq.tv.android.sdk.feature.register.FeatureRegister;
import com.aviq.tv.android.sdk.feature.system.FeatureDevice.DeviceAttribute;
import com.aviq.tv.android.sdk.feature.system.FeatureStandBy;
import com.aviq.tv.android.sdk.utils.Files;

/**
 * Software upgrade scheduler feature
 */
@Author("zhelyazko")
public class FeatureUpgrade extends FeatureScheduler
{
	public static final String TAG = FeatureUpgrade.class.getSimpleName();
	public static final int ON_STATUS_CHANGED = EventMessenger.ID("ON_STATUS_CHANGED");
	public static final int ON_UPDATE_CHECKED = EventMessenger.ID("ON_UPDATE_CHECKED");
	public static final int ON_UPDATE_FOUND = EventMessenger.ID("ON_UPDATE_FOUND");
	public static final int ON_UPDATE_PROGRESS = DownloadService.DOWNLOAD_PROGRESS;
	public static final int ON_START_UPDATE = EventMessenger.ID("ON_START_UPDATE");
	public static final int ON_START_FROM_UPDATE = EventMessenger.ID("ON_START_FROM_UPDATE");
	public static final int ON_UPDATE_FAILED = EventMessenger.ID("ON_UPDATE_FAILED");

	public static final String EXTRA_VERSION = "VERSION";
	public static final String EXTRA_VERSION_PREV = "VERSION_PREV";
	public static final String EXTRA_UPGRADE_DURATION = "UPGRADE_DURATION";

	public static enum Param
	{
		/**
		 * Schedule interval
		 */
		UPDATE_CHECK_INTERVAL(10 * 60 * 1000),

		/**
		 * ABMP update check URL format
		 */
		ABMP_UPDATE_CHECK_URL("${SERVER}/Box/SWVersion.ashx?boxID=${BOX_ID}&version=${VERSION}"),

		/**
		 * ABMP update download URL format
		 */
		ABMP_UPDATE_DOWNLOAD_URL("${SERVER}/Box/SWupdate.ashx?boxID=${BOX_ID}&service=swupdate&file=${FILE_NAME}"),

		/**
		 * Local updates directory relative to /files
		 */
		UPDATES_DIR("/cache/update"),

		/**
		 * Milliseconds to wait before rebooting the box from calling
		 * rebootToInstall
		 */
		UPGRADE_REBOOT_DELAY(5000),

		/**
		 * Milliseconds to wait after saving every downloaded buffer
		 */
		DOWNLOAD_DELAY(20),

		/**
		 * Download software update automatically when new version is detected
		 */
		AUTO_DOWNLOAD(true);

		Param(boolean value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Scheduler.UPGRADE).put(name(), value);
		}

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Scheduler.UPGRADE).put(name(), value);
		}

		Param(String value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Scheduler.UPGRADE).put(name(), value);
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
		UPGRADE_BRAND,

		/**
		 * Whether the system has started after upgrade.
		 */
		IS_AFTER_UPGRADE,

		/**
		 * Keeps the version from which we upgraded from.
		 */
		UPGRADE_VERSION_PREV,

		/**
		 * Keeps when the upgrade was started in order to measure the upgrade
		 * duration.
		 */
		UPGRADE_START_TIME_MILLIS;
	}

	public enum Status
	{
		IDLE, ERROR, CHECKING, DOWNLOADING
	}

	public enum ErrorReason
	{
		NO_ERROR, EXCEPTION, CONNECTION_ERROR, MD5_CHECK_FAILED, CANCELLED, OUT_OF_FREE_SPACE
	}

	public class UpdateInfo
	{
		public String Version;
		public String FileName;
		public long FileSize;
		public String Brand;
		public String SoftwareType;
		public boolean IsForced;
	}

	private boolean _hasUpdate;
	private UpdateInfo _updateInfo;
	private Status _status = Status.IDLE;
	private ErrorReason _errorReason = ErrorReason.NO_ERROR;
	private int _errorCode = ResultCode.OK;
	private UpgradeException _exception;
	private Prefs _userPrefs;
	private boolean _standingBy = false;

	public FeatureUpgrade() throws FeatureNotFoundException
	{
		require(FeatureName.Component.DEVICE);
		require(FeatureName.Component.REGISTER);
		require(FeatureName.Scheduler.INTERNET);
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		_userPrefs = Environment.getInstance().getUserPrefs();
		Environment.getInstance().getEventMessenger().register(this, Environment.ON_LOADED);

		FeatureStandBy featureStandBy = (FeatureStandBy) Environment.getInstance().getFeatureComponent(
		        FeatureName.Component.STANDBY);
		if (featureStandBy != null)
		{
			featureStandBy.getEventMessenger().register(this, FeatureStandBy.ON_STANDBY_ENTER);
			featureStandBy.getEventMessenger().register(this, FeatureStandBy.ON_STANDBY_LEAVE);
		}

		super.initialize(onFeatureInitialized);
	}

	@Override
	protected void onSchedule(OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".onSchedule");
		super.onSchedule(onFeatureInitialized);

		// check for new software update. This method will run only if the
		// current status is IDLE or ERROR
		checkForUpdate(getPrefs().getBool(Param.AUTO_DOWNLOAD));

		// Schedule another update
		scheduleDelayed(getPrefs().getInt(Param.UPDATE_CHECK_INTERVAL));
	}

	@Override
	public Scheduler getSchedulerName()
	{
		return FeatureName.Scheduler.UPGRADE;
	}

	/**
	 * @return true if new software is ready for upgrade by rebootToInstall
	 */
	public boolean isUpgradeReady()
	{
		if (!_userPrefs.has(UserParam.UPGRADE_VERSION))
		{
			Log.i(TAG, ".isUpgradeReady: no");
			return false;
		}
		String version = _userPrefs.getString(UserParam.UPGRADE_VERSION);
		String brand = _userPrefs.getString(UserParam.UPGRADE_BRAND);
		File upgradeFile = getUpgradeFile();
		boolean isNewVersion = isNewVersion(version, brand);
		boolean isFileExists = upgradeFile != null && upgradeFile.exists();
		boolean isReady = isFileExists && isNewVersion;
		StringBuffer log = new StringBuffer();
		log.append("version=").append(version);
		log.append(", brand=").append(brand);
		log.append(", upgradeFile=").append(upgradeFile);
		log.append(", isFileExists=").append(isFileExists);
		log.append(", isNewVersion=").append(isNewVersion);
		log.append(", isReady=").append(isReady);
		Log.e(TAG, ".isUpgradeReady: " + log.toString());
		return isReady;
	}

	/**
	 * @return true if new software is available for download
	 */
	public boolean isUpgradeAvailable()
	{
		return _hasUpdate;
	}

	/**
	 * @return last update information
	 */
	public UpdateInfo getUpdateInfo()
	{
		Log.i(TAG, ".getUpdateInfo: _updateInfo = " + _updateInfo);
		return _updateInfo;
	}

	/**
	 * Reboot to install new software upgrade
	 */
	public void rebootToInstall() throws UpgradeException
	{
		if (!isUpgradeReady())
		{
			throw new UpgradeException(this, ResultCode.GENERAL_FAILURE,
			        "rebootToInstall: Not ready for software upgrade");
		}

		int delay = getPrefs().getInt(Param.UPGRADE_REBOOT_DELAY);

		_userPrefs.put(UserParam.IS_AFTER_UPGRADE, true);
		_userPrefs.put(UserParam.UPGRADE_VERSION_PREV, Environment.getInstance().getBuildVersion());
		_userPrefs.put(UserParam.UPGRADE_START_TIME_MILLIS, System.currentTimeMillis() + delay);

		Bundle params = null;
		if (_updateInfo != null)
		{
			params = new Bundle();
			params.putString(EXTRA_VERSION, _updateInfo.Version);
		}
		getEventMessenger().trigger(ON_START_UPDATE, params);

		getEventMessenger().postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					Environment.getInstance().resetPreferences();
					RecoverySystem.installPackage(Environment.getInstance(), getUpgradeFile());
				}
				catch (IOException e)
				{
					setStatus(new UpgradeException(FeatureUpgrade.this, ResultCode.IO_ERROR, "rebootToInstall: failed",
					        e), null);
				}
			}
		}, delay);
	}

	/**
	 * @return Status - the current execution status of the upgrade feature
	 */
	public Status getStatus()
	{
		return _status;
	}

	/**
	 * @return ErrorReason the high level reason in case of Error status
	 */
	public ErrorReason getErrorReason()
	{
		return _errorReason;
	}

	/**
	 * @return ResultCode integer describing the error occurred
	 */
	public int getErrorCode()
	{
		return _errorCode;
	}

	/**
	 * @return Exception occurred in case of Error status
	 */
	public UpgradeException getException()
	{
		return _exception;
	}

	/**
	 * Checks for new software update. This method will run only if the current
	 * status is IDLE or ERROR
	 *
	 * @return true if check for upgrade started, false means the upgrade
	 *         process is in the middle of checking or downloading
	 */
	public boolean checkForUpdate()
	{
		return checkForUpdate(false);
	}

	/**
	 * Checks for new software update and download if new update is found.
	 * This method will run only if the current status is IDLE or ERROR
	 *
	 * @return true if check for upgrade started, false means the upgrade
	 *         process is in the middle of checking or downloading
	 */
	public boolean checkForUpdateAndDownload()
	{
		Log.i(TAG, ".checkForUpdateAndDownload");
		return checkForUpdate(true);
	}

	@Override
	public void onEvent(int msgId, Bundle bundle)
	{
		super.onEvent(msgId, bundle);
		if (msgId == Environment.ON_LOADED)
		{
			Prefs userPrefs = Environment.getInstance().getUserPrefs();
			if (userPrefs.has(UserParam.IS_AFTER_UPGRADE) && userPrefs.getBool(UserParam.IS_AFTER_UPGRADE))
			{
				Bundle params = new Bundle();

				String oldVersion = userPrefs.has(UserParam.UPGRADE_VERSION_PREV) ? userPrefs
				        .getString(UserParam.UPGRADE_VERSION_PREV) : null;
				params.putString(EXTRA_VERSION_PREV, oldVersion);

				long duration = userPrefs.has(UserParam.UPGRADE_START_TIME_MILLIS) ? (System.currentTimeMillis() - userPrefs
				        .getLong(UserParam.UPGRADE_START_TIME_MILLIS)) / 1000 : -1;
				params.putLong(EXTRA_UPGRADE_DURATION, duration);

				String currentBrand = _feature.Component.DEVICE.getDeviceAttribute(DeviceAttribute.BRAND);
				if (!isNewVersion(oldVersion, currentBrand))
				{
					// Old version = current version, i.e. the upgrade failed

					String expectedVersion = userPrefs.has(UserParam.UPGRADE_VERSION) ? userPrefs
					        .getString(UserParam.UPGRADE_VERSION) : null;
					params.putString(EXTRA_VERSION, expectedVersion);

					getEventMessenger().trigger(ON_UPDATE_FAILED, params);
				}
				else
				{
					// Update succeeded
					getEventMessenger().trigger(ON_START_FROM_UPDATE, params);
				}

				userPrefs.put(UserParam.IS_AFTER_UPGRADE, false);
			}

			// Start upgrade scheduling after the app is fully loaded
			getEventMessenger().trigger(FeatureScheduler.ON_SCHEDULE, getPrefs().getInt(Param.UPDATE_CHECK_INTERVAL));
		}
		else if (msgId == FeatureStandBy.ON_STANDBY_ENTER)
		{
			_standingBy = true;
		}
		else if (msgId == FeatureStandBy.ON_STANDBY_LEAVE)
		{
			_standingBy = false;
		}
	}

	private boolean checkForUpdate(final boolean isDownload)
	{
		Log.i(TAG, ".checkForUpdate: isDownload = " + isDownload);
		// Contact the server for a software update check
		if (!(_status.equals(Status.IDLE) || _status.equals(Status.ERROR)))
		{
			Log.w(TAG, ".checkForUpdate: already in status " + _status);
			return false;
		}

		// Check for software updates
		Log.i(TAG, "Checking for new software update");
		setStatus(Status.CHECKING, ErrorReason.NO_ERROR, ResultCode.OK, null);

		Bundle updateCheckUrlParams = new Bundle();
		updateCheckUrlParams.putString("SERVER",
		        _feature.Component.REGISTER.getPrefs().getString(FeatureRegister.Param.ABMP_SERVER));
		updateCheckUrlParams.putString("BOX_ID", _feature.Component.DEVICE.getDeviceAttribute(DeviceAttribute.MAC));
		updateCheckUrlParams.putString("VERSION", Environment.getInstance().getBuildVersion());

		String abmpUpdateCheckUrl = getPrefs().getString(Param.ABMP_UPDATE_CHECK_URL, updateCheckUrlParams);
		Log.i(TAG, ".checkForUpdate: " + abmpUpdateCheckUrl);

		_hasUpdate = false;
		_feature.Scheduler.INTERNET.getUrlContent(abmpUpdateCheckUrl, new OnResultReceived()
		{
			@Override
			public void onReceiveResult(FeatureError result, Object object)
			{
				if (!result.isError())
				{
					try
					{
						String content = result.getBundle().getString(FeatureInternet.ResultExtras.CONTENT.name());
						DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
						DocumentBuilder db = dbf.newDocumentBuilder();
						InputSource xmlSource = new InputSource(new StringReader(content));
						Document doc = db.parse(xmlSource);

						XPathFactory factory = XPathFactory.newInstance();
						XPath xPath = factory.newXPath();
						Element docElement = doc.getDocumentElement();

						Node versionNode = ((NodeList) xPath
						        .evaluate("/SW/version", docElement, XPathConstants.NODESET)).item(0);
						UpdateInfo updateData = new UpdateInfo();
						updateData.Version = xPath.evaluate("@name", versionNode);
						updateData.FileName = xPath.evaluate("@url", versionNode);
						updateData.SoftwareType = xPath.evaluate("@type", versionNode);
						updateData.Brand = xPath.evaluate("@brand", versionNode);
						updateData.IsForced = Boolean.parseBoolean(xPath.evaluate("@forced", versionNode));
						updateData.FileSize = Long.parseLong(xPath.evaluate("@filesize", versionNode));

						Log.i(TAG, "Reported software version=" + updateData.Version + ", brand=" + updateData.Brand
						        + ", fileName=`" + updateData.FileName + "', fileSize=" + updateData.FileSize
						        + ", softwareType=" + updateData.SoftwareType + ", forced=" + updateData.IsForced);
						_updateInfo = updateData;

						// Store box category type - for read only purposes
						_userPrefs.put(UserParam.UPGRADE_BOX_CATEGORY, updateData.SoftwareType);

						_hasUpdate = isNewVersion(updateData.Version, updateData.Brand);
						if (_hasUpdate)
						{
							getEventMessenger().trigger(ON_UPDATE_FOUND);

							if (isDownload)
							{
								if (isUpgradeReady())
								{
									if (isVersionsDiffer(_userPrefs.getString(UserParam.UPGRADE_VERSION),
									        _userPrefs.getString(UserParam.UPGRADE_BRAND), updateData.Version,
									        updateData.Brand))
									{
										_userPrefs.remove(UserParam.UPGRADE_VERSION);
										_userPrefs.remove(UserParam.UPGRADE_BRAND);

										// The old upgrade file will be deleted
										// before new downloading starts
										_userPrefs.remove(UserParam.UPGRADE_FILE);
									}
								}

								// Proceed with downloading new software
								downloadUpdate();
							}
						}

						if (!(_hasUpdate && isDownload))
						{
							// set ok status if not downloading
							setOkStatus(result.getBundle());
						}
					}
					catch (Exception e)
					{
						setStatus(new UpgradeException(FeatureUpgrade.this, e), result.getBundle());
					}
				}
				else
				{
					setStatus(Status.ERROR, ErrorReason.CONNECTION_ERROR, result.getCode(), result.getBundle());
				}
				getEventMessenger().trigger(ON_UPDATE_CHECKED);
			}
		});

		return true;
	}

	private void setStatus(Status status, ErrorReason errorReason, int errorCode, Bundle extraData)
	{
		if (!status.equals(_status))
		{
			_status = status;
			_errorReason = errorReason;
			_errorCode = errorCode;

			Log.i(TAG, ".setStatus: trigger = " + ON_STATUS_CHANGED + ", _status = " + _status + ", _errorReason = "
			        + _errorReason + ", _errorCode = " + _errorCode);
			getEventMessenger().trigger(ON_STATUS_CHANGED, extraData);
		}
	}

	private void setOkStatus(Bundle extraData)
	{
		setStatus(Status.IDLE, ErrorReason.NO_ERROR, ResultCode.OK, extraData);
	}

	private void setStatus(UpgradeException exception, Bundle extraData)
	{
		if (_standingBy)
			Log.w(TAG, exception.toString());
		else
			Log.e(TAG, exception.toString(), exception);
		_exception = exception;
		setStatus(Status.ERROR, ErrorReason.EXCEPTION, exception.getCode(), extraData);
	}

	// returns the last downloaded and verified with checksum firmware file
	private File getUpgradeFile()
	{
		if (!_userPrefs.has(UserParam.UPGRADE_FILE))
			return null;

		String updateFile = _userPrefs.getString(UserParam.UPGRADE_FILE);
		return new File(Files.filePath(Environment.getInstance(), updateFile));
	}

	private void downloadUpdate()
	{
		Log.i(TAG, ".downloadUpdate");
		if (isUpgradeReady())
		{
			Log.i(TAG, ".downloadUpdate: upgrade already downloaded");
			setOkStatus(null);
			return;
		}

		// change status to downloading
		setStatus(Status.DOWNLOADING, ErrorReason.NO_ERROR, ResultCode.OK, null);

		// format download url
		Bundle updateUrlParams = new Bundle();
		updateUrlParams.putString("SERVER",
		        _feature.Component.REGISTER.getPrefs().getString(FeatureRegister.Param.ABMP_SERVER));
		updateUrlParams.putString("BOX_ID", _feature.Component.DEVICE.getDeviceAttribute(DeviceAttribute.MAC));
		updateUrlParams.putString("FILE_NAME", _updateInfo.FileName);
		final String updateUrl = getPrefs().getString(Param.ABMP_UPDATE_DOWNLOAD_URL, updateUrlParams);

		// download md5 file
		final String updateUrlMd5 = updateUrl + ".md5";
		_feature.Scheduler.INTERNET.getUrlContent(updateUrlMd5, new ServiceController.OnResultReceived()
		{
			@Override
			public void onReceiveResult(FeatureError result, Object object)
			{
				if (!result.isError())
				{
					String md5Content = result.getBundle().getString(ResultExtras.CONTENT.name());
					String[] md5Parts = md5Content.split(" ");
					if (md5Parts.length > 0)
						md5Content = md5Parts[0];
					final String md5 = md5Content;

					final String updateFile = getPrefs().getString(Param.UPDATES_DIR) + File.separator
					        + Files.baseName(_updateInfo.FileName);
					Bundle downloadParams = new Bundle();
					downloadParams.putString(DownloadService.Extras.URL.name(), updateUrl);
					downloadParams.putString(DownloadService.Extras.LOCAL_FILE.name(), updateFile);
					downloadParams.putBoolean(DownloadService.Extras.IS_COMPUTE_MD5.name(), true);
					downloadParams.putLong(DownloadService.Extras.DOWNLOAD_DELAY.name(),
					        getPrefs().getInt(Param.DOWNLOAD_DELAY));
					// FIXME: Add other params like proxy, connection timeouts
					// etc.

					// clean up the update directory before starting new
					// download
					File updatesDir = new File(Files.normalizeName(Environment.getInstance(),
					        getPrefs().getString(Param.UPDATES_DIR)));
					if (!updatesDir.exists())
						updatesDir.mkdirs();
					String[] files = updatesDir.list();

					// remove all collected *.part and *.zip files
					if (files != null)
					{
						for (String delFileName : files)
						{
							if (delFileName.toLowerCase().endsWith(".part")
							        || delFileName.toLowerCase().endsWith(".zip"))
							{
								delFileName = updatesDir + "/" + delFileName;
								boolean success = new File(delFileName).delete();
								if (!success)
									Log.e(TAG, "Failed to delete file `" + delFileName + "'");
								else
									Log.i(TAG, "Deleted file `" + delFileName + "'");
							}
						}
					}

					// start download
					_feature.Scheduler.INTERNET.downloadFile(downloadParams, new OnResultReceived()
					{
						@Override
						public void onReceiveResult(FeatureError result, Object object)
						{
							if (DownloadService.DOWNLOAD_PROGRESS == result.getCode())
							{
								float progress = result.getBundle().getFloat(
								        DownloadService.ResultExtras.PROGRESS.name());
								Log.v(TAG, "File download progress " + progress);
								getEventMessenger().trigger(ON_UPDATE_PROGRESS, result.getBundle());
							}
							else if (DownloadService.DOWNLOAD_SUCCESS == result.getCode())
							{
								Log.i(TAG, ".downloadUpdate: download success");
								// Download finished, checking md5
								String downloadedMd5 = result.getBundle().getString(
								        DownloadService.ResultExtras.MD5.name());
								if (!md5.equalsIgnoreCase(downloadedMd5))
								{
									// md5 check failed
									setStatus(Status.ERROR, ErrorReason.MD5_CHECK_FAILED, ResultCode.GENERAL_FAILURE,
									        result.getBundle());
								}
								else
								{
									Log.i(TAG, ".downloadUpdate: md5 verification pass: " + downloadedMd5);
									// download success
									_userPrefs.put(UserParam.UPGRADE_FILE, updateFile);
									_userPrefs.put(UserParam.UPGRADE_VERSION, _updateInfo.Version);
									_userPrefs.put(UserParam.UPGRADE_BRAND, _updateInfo.Brand);
									setOkStatus(result.getBundle());
								}
							}
							else if (DownloadService.DOWNLOAD_CANCELLED == result.getCode())
							{
								setStatus(Status.ERROR, ErrorReason.CANCELLED, ResultCode.IO_ERROR, result.getBundle());
							}
							else if (result.isError())
							{
								if (result.getCode() == ResultCode.IO_SPACE_ERROR)
								{
									setStatus(Status.ERROR, ErrorReason.OUT_OF_FREE_SPACE, ResultCode.IO_SPACE_ERROR,
									        result.getBundle());
								}
								else
								{
									Log.e(TAG, ".downloadUpdate: download failed");
									Throwable exception = (Throwable) result.getBundle().getSerializable(
									        DownloadService.ResultExtras.EXCEPTION.name());
									setStatus(new UpgradeException(FeatureUpgrade.this, exception), result.getBundle());
								}
							}
						}
					});
				}
				else
				{
					Log.e(TAG, "Error retrieving md5 file " + updateUrlMd5);
					setStatus(Status.ERROR, ErrorReason.CONNECTION_ERROR, result.getCode(), result.getBundle());
				}
			}
		});
	}

	private boolean isVersionsDiffer(String version1, String brand1, String version2, String brand2)
	{
		if (version1 == null || brand1 == null || version2 == null || brand2 == null)
			throw new IllegalArgumentException("Arguments cannot be null: " + version1 + ", " + brand1 + ", "
			        + version2 + ", " + brand2);

		boolean isNewBrand = !(TextUtils.isEmpty(brand2) || brand2.equalsIgnoreCase(brand1));
		if (isNewBrand)
			return true;

		String[] version1Parts = version1.split("\\.");
		String[] version2Parts = version2.split("\\.");
		if (version1Parts.length != version2Parts.length)
		{
			Log.i(TAG, ".isVersionsDiffer: No compatible versions formats current=" + version1 + ", other=" + version2
			        + ". Assuming new version!");
			return true;
		}

		try
		{
			for (int i = 0; i < version1Parts.length; i++)
			{
				int currentVer = Integer.parseInt(version1Parts[i]);
				int otherVer = Integer.parseInt(version2Parts[i]);
				if (currentVer > otherVer)
				{
					Log.i(TAG, ".isVersionsDiffer: current=" + version1 + " is newer than other=" + version2
					        + ". Assuming new version!");
					return true;
				}
				else if (currentVer < otherVer)
				{
					Log.i(TAG, ".isVersionsDiffer: current=" + version1 + " is older than other=" + version2
					        + ". New version!");
					return true;
				}
			}
		}
		catch (NumberFormatException e)
		{
			Log.i(TAG, ".isVersionsDiffer: Invalid version component in current=" + version1 + " or other=" + version2
			        + ". Assuming new version!");
			return true;
		}

		Log.i(TAG, ".isVersionsDiffer: Current version " + version1 + " is the same as the new reported " + version2
		        + ". No new software version!");
		return false;
	}

	private boolean isNewVersion(String otherVersion, String otherBrand)
	{
		String currentBrand = _feature.Component.DEVICE.getDeviceAttribute(DeviceAttribute.BRAND);
		return isVersionsDiffer(Environment.getInstance().getBuildVersion(), currentBrand, otherVersion, otherBrand);
	}
}

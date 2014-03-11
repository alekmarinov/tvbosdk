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
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.os.Bundle;
import android.os.RecoverySystem;
import android.text.TextUtils;
import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Scheduler;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.FeatureScheduler;
import com.aviq.tv.android.sdk.core.service.ServiceController;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.internet.DownloadService;
import com.aviq.tv.android.sdk.feature.internet.FeatureInternet;
import com.aviq.tv.android.sdk.feature.internet.FeatureInternet.ResultExtras;
import com.aviq.tv.android.sdk.feature.register.FeatureRegister;
import com.aviq.tv.android.sdk.utils.Files;

/**
 * Software upgrade scheduler feature
 */
public class FeatureUpgrade extends FeatureScheduler
{
	public static final String TAG = FeatureUpgrade.class.getSimpleName();
	public static final int ON_STATUS_CHANGED = EventMessenger.ID();
	public static final int ON_UPDATE_FOUND = EventMessenger.ID();
	public static final int ON_UPDATE_PROGRESS = DownloadService.DOWNLOAD_PROGRESS;

	public enum Param
	{
		/**
		 * Schedule interval
		 */
		UPDATE_CHECK_INTERVAL(30 * 60 * 1000),

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
		UPDATES_DIR("update");

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
		UPGRADE_BRAND;
	}

	public enum Status
	{
		IDLE, ERROR, CHECKING, DOWNLOADING
	}

	public enum ErrorReason
	{
		NO_ERROR, EXCEPTION, CONNECTION_ERROR, MD5_CHECK_FAILED
	}

	private class UpdateData
	{
		String Version;
		String FileName;
		long FileSize;
		String Brand;
		String SoftwareType;
		boolean IsForced;
	}

	private FeatureRegister _featureRegister;
	private FeatureInternet _featureInternet;
	private boolean _hasUpdate;
	private UpdateData _updateData;
	private Status _status = Status.IDLE;
	private ErrorReason _errorReason = ErrorReason.NO_ERROR;
	private int _errorCode = ResultCode.OK;
	private UpgradeException _exception;
	private Prefs _userPrefs;

	public FeatureUpgrade()
	{
		_dependencies.Components.add(FeatureName.Component.REGISTER);
		_dependencies.Schedulers.add(FeatureName.Scheduler.INTERNET);
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		try
		{
			_userPrefs = Environment.getInstance().getUserPrefs();

			_featureRegister = (FeatureRegister) Environment.getInstance().getFeatureComponent(
			        FeatureName.Component.REGISTER);
			_featureInternet = (FeatureInternet) Environment.getInstance().getFeatureScheduler(
			        FeatureName.Scheduler.INTERNET);

			super.initialize(onFeatureInitialized);
		}
		catch (FeatureNotFoundException e)
		{
			Log.e(TAG, e.getMessage(), e);
			onFeatureInitialized.onInitialized(this, ResultCode.GENERAL_FAILURE);
		}
	}

	@Override
	protected void onSchedule(OnFeatureInitialized onFeatureInitialized)
	{
		super.onSchedule(onFeatureInitialized);

		// check for new software update. This method will run only if the
		// current status is IDLE or ERROR
		checkForUpdateAndDownload();

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
		File upgradeFile = getUpgradeFile();
		if (!_userPrefs.has(UserParam.UPGRADE_VERSION))
			return false;
		String version = _userPrefs.getString(UserParam.UPGRADE_VERSION);
		String brand = _userPrefs.getString(UserParam.UPGRADE_BRAND);
		return upgradeFile != null && upgradeFile.exists() && isNewVersion(version, brand);
	}

	/**
	 * @return true if new software is available for download
	 */
	public boolean isUpgradeAvailable()
	{
		return _updateData != null && isNewVersion(_updateData.Version, _updateData.Brand);
	}

	/**
	 * Reboot to install new software upgrade
	 */
	public void rebootToInstall() throws UpgradeException
	{
		if (!isUpgradeReady())
		{
			throw new UpgradeException(ResultCode.GENERAL_FAILURE, "rebootToInstall: Not ready for software upgrade");
		}
		try
		{
			RecoverySystem.installPackage(Environment.getInstance().getActivity(), getUpgradeFile());
		}
		catch (Exception e)
		{
			throw new UpgradeException(ResultCode.GENERAL_FAILURE, "rebootToInstall: failed", e);
		}
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
		return checkForUpdate(true);
	}

	private boolean checkForUpdate(final boolean isDownload)
	{
		// Contact the server for a software update check
		if (!(_status.equals(Status.IDLE) || _status.equals(Status.ERROR)))
		{
			Log.w(TAG, ".checkForUpdate: already in status " + _status);
			return false;
		}

		// Check for software updates
		Log.i(TAG, "Checking for new software update");
		setStatus(Status.CHECKING, ErrorReason.NO_ERROR, ResultCode.OK);

		Bundle updateCheckUrlParams = new Bundle();
		updateCheckUrlParams.putString("SERVER",
		        _featureRegister.getPrefs().getString(FeatureRegister.Param.ABMP_SERVER));
		updateCheckUrlParams.putString("BOX_ID", _featureRegister.getBoxId());
		updateCheckUrlParams.putString("VERSION", Environment.getInstance().getBuildVersion());

		String abmpUpdateCheckUrl = getPrefs().getString(Param.ABMP_UPDATE_CHECK_URL, updateCheckUrlParams);
		Log.i(TAG, ".checkForUpdate: " + abmpUpdateCheckUrl);

		_featureInternet.getUrlContent(abmpUpdateCheckUrl, new OnResultReceived()
		{
			@Override
			public void onReceiveResult(int resultCode, Bundle resultData)
			{
				if (ResultCode.OK == resultCode)
				{
					try
					{
						String content = resultData.getString(FeatureInternet.ResultExtras.CONTENT.name());
						DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
						DocumentBuilder db = dbf.newDocumentBuilder();
						InputSource xmlSource = new InputSource(new StringReader(content));
						Document doc = db.parse(xmlSource);

						XPathFactory factory = XPathFactory.newInstance();
						XPath xPath = factory.newXPath();
						Element docElement = doc.getDocumentElement();

						Node versionNode = ((NodeList) xPath
						        .evaluate("/SW/version", docElement, XPathConstants.NODESET)).item(0);
						UpdateData updateData = new UpdateData();
						updateData.Version = xPath.evaluate("@name", versionNode);
						updateData.FileName = xPath.evaluate("@url", versionNode);
						updateData.SoftwareType = xPath.evaluate("@type", versionNode);
						updateData.Brand = xPath.evaluate("@brand", versionNode);
						updateData.IsForced = Boolean.parseBoolean(xPath.evaluate("@forced", versionNode));
						updateData.FileSize = Long.parseLong(xPath.evaluate("@filesize", versionNode));

						Log.i(TAG, "Reported software version=" + updateData.Version + ", brand=" + updateData.Brand
						        + ", fileName=`" + updateData.FileName + "', fileSize=" + updateData.FileSize
						        + ", softwareType=" + updateData.SoftwareType + ", forced=" + updateData.IsForced);
						_updateData = updateData;

						// Store box category type - for read only purposes
						_userPrefs.put(UserParam.UPGRADE_BOX_CATEGORY, updateData.SoftwareType);

						_hasUpdate = isNewVersion(updateData.Version, updateData.Brand);
						if (_hasUpdate && isDownload)
						{
							// Proceed with downloading new software
							downloadUpdate();
						}
						else
						{
							// No new software update
							setOkStatus();
						}
					}
					catch (ParserConfigurationException e)
					{
						setStatus(new UpgradeException(ResultCode.PROTOCOL_ERROR, e));
					}
					catch (SAXException e)
					{
						setStatus(new UpgradeException(ResultCode.PROTOCOL_ERROR, e));
					}
					catch (NumberFormatException e)
					{
						setStatus(new UpgradeException(ResultCode.PROTOCOL_ERROR, e));
					}
					catch (IOException e)
					{
						setStatus(new UpgradeException(ResultCode.INTERNAL_ERROR, e));
					}
					catch (XPathExpressionException e)
					{
						setStatus(new UpgradeException(ResultCode.INTERNAL_ERROR, e));
					}
				}
				else
				{
					setStatus(Status.ERROR, ErrorReason.CONNECTION_ERROR, resultCode);
				}
			}
		});

		return true;
	}

	private void setStatus(Status status, ErrorReason errorReason, int errorCode)
	{
		if (!status.equals(_status))
		{
			_status = status;
			_errorReason = errorReason;
			_errorCode = errorCode;

			Log.i(TAG, ".setStatus: trigger = " + ON_STATUS_CHANGED + ", _status = " + _status + ", _errorReason = "
			        + _errorReason + ", _errorCode = " + _errorCode);
			getEventMessenger().trigger(ON_STATUS_CHANGED);
		}
	}

	private void setOkStatus()
	{
		setStatus(Status.IDLE, ErrorReason.NO_ERROR, ResultCode.OK);
	}

	private void setStatus(UpgradeException exception)
	{
		_exception = exception;
		setStatus(Status.ERROR, ErrorReason.EXCEPTION, exception.getResultCode());
	}

	// returns the last downloaded and verified firmware file
	private File getUpgradeFile()
	{
		if (!_userPrefs.has(UserParam.UPGRADE_FILE))
			return null;

		String updateFile = _userPrefs.getString(UserParam.UPGRADE_FILE);
		File filesDir = Environment.getInstance().getActivity().getFilesDir();
		return new File(filesDir, updateFile);
	}

	private void downloadUpdate()
	{
		if (isUpgradeReady())
		{
			setOkStatus();
			return;
		}
		// notify update is found and start downloading
		getEventMessenger().trigger(ON_UPDATE_FOUND);

		// change status to downloading
		setStatus(Status.DOWNLOADING, ErrorReason.NO_ERROR, ResultCode.OK);

		// format download url
		Bundle updateUrlParams = new Bundle();
		updateUrlParams.putString("SERVER", _featureRegister.getPrefs().getString(FeatureRegister.Param.ABMP_SERVER));
		updateUrlParams.putString("BOX_ID", _featureRegister.getBoxId());
		updateUrlParams.putString("FILE_NAME", _updateData.FileName);
		final String updateUrl = getPrefs().getString(Param.ABMP_UPDATE_DOWNLOAD_URL, updateUrlParams);

		// download md5 file
		final String updateUrlMd5 = updateUrl + ".md5";
		_featureInternet.getUrlContent(updateUrlMd5, new ServiceController.OnResultReceived()
		{
			@Override
			public void onReceiveResult(int resultCode, Bundle resultData)
			{
				if (ResultCode.OK == resultCode)
				{
					String md5Content = resultData.getString(ResultExtras.CONTENT.name());
					String[] md5Parts = md5Content.split(" ");
					if (md5Parts.length > 0)
						md5Content = md5Parts[0];
					final String md5 = md5Content;

					final String updateFile = getPrefs().getString(Param.UPDATES_DIR) + "/"
					        + Files.baseName(_updateData.FileName);
					Bundle downloadParams = new Bundle();
					downloadParams.putString(DownloadService.Extras.URL.name(), updateUrl);
					downloadParams.putString(DownloadService.Extras.LOCAL_FILE.name(), updateFile);
					downloadParams.putBoolean(DownloadService.Extras.IS_COMPUTE_MD5.name(), true);
					// FIXME: Add other params like proxy, connection timeouts
					// etc.

					// start download
					_featureInternet.downloadFile(downloadParams, new OnResultReceived()
					{
						@Override
						public void onReceiveResult(int resultCode, Bundle resultData)
						{
							if (DownloadService.DOWNLOAD_PROGRESS == resultCode)
							{
								float progress = resultData.getFloat(DownloadService.ResultExtras.PROGRESS.name());
								Log.v(TAG, "File download progress " + progress);
								getEventMessenger().trigger(ON_UPDATE_PROGRESS, resultData);
							}
							else if (DownloadService.DOWNLOAD_SUCCESS == resultCode)
							{
								Log.i(TAG, ".downloadUpdate: download success");
								// Download finished, checking md5
								if (!md5.equalsIgnoreCase(resultData.getString(DownloadService.ResultExtras.MD5.name())))
								{
									// md5 check failed
									setStatus(Status.ERROR, ErrorReason.MD5_CHECK_FAILED, ResultCode.GENERAL_FAILURE);
								}
								else
								{
									// download success
									_userPrefs.put(UserParam.UPGRADE_FILE, updateFile);
									_userPrefs.put(UserParam.UPGRADE_VERSION, _updateData.Version);
									_userPrefs.put(UserParam.UPGRADE_BRAND, _updateData.Brand);
									setOkStatus();
								}
							}
							else if (DownloadService.DOWNLOAD_FAILED == resultCode)
							{
								Log.e(TAG, ".downloadUpdate: download failed");
								Throwable exception = (Throwable) resultData
								        .getSerializable(DownloadService.ResultExtras.EXCEPTION.name());
								setStatus(new UpgradeException(ResultCode.GENERAL_FAILURE, exception));
							}
						}
					});
				}
				else
				{
					Log.e(TAG, "Error retrieving md5 file " + updateUrlMd5);
					setStatus(Status.ERROR, ErrorReason.CONNECTION_ERROR, resultCode);
				}
			}
		});
	}

	private boolean isNewVersion(String otherVersion, String brand)
	{
		if (otherVersion == null)
			throw new IllegalArgumentException("Arguments cannot be null");
		String currentVersion = Environment.getInstance().getBuildVersion();

		// check if BoxID is reassigned to new brand to allow FW update
		boolean isNewBrand = !(TextUtils.isEmpty(brand) || _featureRegister.getPrefs()
		        .getString(FeatureRegister.Param.BRAND).equalsIgnoreCase(brand));
		if (isNewBrand)
			return true;

		String[] currentVersionParts = currentVersion.split("\\.");
		String[] otherVersionParts = otherVersion.split("\\.");
		if (currentVersionParts.length != otherVersionParts.length)
		{
			Log.i(TAG, ".hasNewVersion: No compatible versions formats current=" + currentVersion + ", other="
			        + otherVersion + ". Assuming new version!");
			return true;
		}

		try
		{
			for (int i = 0; i < currentVersionParts.length; i++)
			{
				int currentVer = Integer.parseInt(currentVersionParts[i]);
				int otherVer = Integer.parseInt(otherVersionParts[i]);
				if (currentVer > otherVer)
				{
					Log.i(TAG, ".hasNewVersion: current=" + currentVersion + " is newer than other=" + otherVersion
					        + ". Assuming new version!");
					return true;
				}
				else if (currentVer < otherVer)
				{
					Log.i(TAG, ".hasNewVersion: current=" + currentVersion + " is older than other=" + otherVersion
					        + ". New version!");
					return true;
				}
			}
		}
		catch (NumberFormatException e)
		{
			Log.i(TAG, ".hasNewVersion: Invalid version component in current=" + currentVersion + " or other="
			        + otherVersion + ". Assuming new version!");
			return true;
		}

		Log.i(TAG, ".hasNewVersion: Current version " + currentVersion + " is the same as the new reported "
		        + otherVersion + ". No new software version!");
		return false;
	}
}

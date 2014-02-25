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
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.internet.DownloadService;
import com.aviq.tv.android.sdk.feature.internet.FeatureInternet;
import com.aviq.tv.android.sdk.feature.register.FeatureRegister;
import com.aviq.tv.android.sdk.utils.Files;

/**
 * Software upgrade scheduler feature
 */
public class FeatureUpgrade extends FeatureScheduler
{
	public static final String TAG = FeatureUpgrade.class.getSimpleName();
	public static final int ON_SOFTWARE_UPDATE_FOUND = EventMessenger.ID();
	public static final int ON_UPDATE_PROGRESS = EventMessenger.ID();
	public static final int ON_UPDATE_ERROR = EventMessenger.ID();
	public static final int ON_UPDATE_DOWNLOAD_FINISHED = EventMessenger.ID();

	public enum Param
	{
		/**
		 * Schedule interval
		 */
		UPDATE_CHECK_INTERVAL(5 * 60 * 1000),

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

	public class UpdateData
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
	private boolean _updateDownloadStarted;
	private boolean _updateDownloadFinished;
	private UpdateData _updateData;

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
			_featureRegister = (FeatureRegister) Environment.getInstance().getFeatureComponent(
			        FeatureName.Component.REGISTER);
			_featureInternet = (FeatureInternet) Environment.getInstance().getFeatureScheduler(
			        FeatureName.Scheduler.INTERNET);

			onSchedule(onFeatureInitialized);
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

		// Contact the server for a software update check; skip check if we
		// already know there is an update or if the update is currently being
		// downloaded or has already been downloaded because the main has
		// been notify about the update and further checking is pointless.
		boolean performCheck = !(hasUpdate() && (isUpdateDownloadStarted() || isUpdateDownloadFinished()));
		Log.i(TAG, "performCheck = " + performCheck);
		if (performCheck)
		{
			checkForUpdate();
		}

		// Schedule another update
		scheduleDelayed(getPrefs().getInt(Param.UPDATE_CHECK_INTERVAL));
	}

	@Override
	public Scheduler getSchedulerName()
	{
		return FeatureName.Scheduler.UPGRADE;
	}

	public boolean hasUpdate()
	{
		Log.i(TAG, ".hasUpdate: " + _hasUpdate);
		return _hasUpdate;
	}

	public boolean isUpdateDownloadStarted()
	{
		Log.i(TAG, ".isUpdateDownloadStarted: " + _updateDownloadStarted);
		return _updateDownloadStarted;
	}

	public boolean isUpdateDownloadFinished()
	{
		Log.i(TAG, ".isUpdateDownloadFinished: " + _updateDownloadFinished);
		return _updateDownloadFinished;
	}

	public String getUpdateFileName()
	{
		if (_updateData == null)
			return null;
		return _updateData.FileName;
	}

	private void checkForUpdate()
	{
		// Check for software updates
		Log.i(TAG, "Checking for new software update");

		Bundle updateCheckUrlParams = new Bundle();
		updateCheckUrlParams.putString("SERVER", _featureRegister.getPrefs().getString(FeatureRegister.Param.ABMP_SERVER));
		updateCheckUrlParams.putString("BOX_ID", _featureRegister.getBoxId());
		updateCheckUrlParams.putString("VERSION", Environment.getInstance().getBuildVersion());

		String abmpUpdateCheckUrl = getPrefs().getString(Param.ABMP_UPDATE_CHECK_URL, updateCheckUrlParams);
		Log.i(TAG, ".checkForUpdate: " + abmpUpdateCheckUrl);

		_featureInternet.getUrlContent(abmpUpdateCheckUrl, new OnResultReceived()
		{
			@Override
			public void onReceiveResult(int resultCode, Bundle resultData)
			{
				switch (resultCode)
				{
					case ResultCode.OK:
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

							Node versionNode = ((NodeList) xPath.evaluate("/SW/version", docElement,
							        XPathConstants.NODESET)).item(0);
							UpdateData updateData = new UpdateData();
							updateData.Version = xPath.evaluate("@name", versionNode);
							updateData.FileName = xPath.evaluate("@url", versionNode);
							updateData.SoftwareType = xPath.evaluate("@type", versionNode);
							updateData.Brand = xPath.evaluate("@brand", versionNode);
							updateData.IsForced = Boolean.parseBoolean(xPath.evaluate("@forced", versionNode));
							updateData.FileSize = Long.parseLong(xPath.evaluate("@filesize", versionNode));
							updateCheckResult(updateData);
						}
						catch (ParserConfigurationException e)
						{
							Log.e(TAG, e.getMessage(), e);
						}
						catch (SAXException e)
						{
							Log.e(TAG, e.getMessage(), e);
						}
						catch (IOException e)
						{
							Log.e(TAG, e.getMessage(), e);
						}
						catch (XPathExpressionException e)
						{
							Log.e(TAG, e.getMessage(), e);
						}
						catch (NumberFormatException e)
						{
							Log.w(TAG, e.getMessage());
						}
					}
					break;
				}
			}
		});
	}

	private void updateCheckResult(UpdateData updateData)
	{
		Log.i(TAG, "Reported software version=" + updateData.Version + ", brand=" + updateData.Brand + ", fileName=`"
		        + updateData.FileName + "', fileSize=" + updateData.FileSize + ", softwareType="
		        + updateData.SoftwareType + ", forced=" + updateData.IsForced);
		_updateData = updateData;

		// Store box category type - for read only purposes
		Environment.getInstance().getUserPrefs().put(UserParam.UPGRADE_BOX_CATEGORY, updateData.SoftwareType);

		_hasUpdate = hasNewVersion(Environment.getInstance().getBuildVersion(), updateData.Version, updateData.Brand);
		if (_hasUpdate)
		{
			// Notify that new update exists
			getEventMessenger().trigger(ON_SOFTWARE_UPDATE_FOUND);

			// Start download in background
			downloadUpdate();
		}
	}

	private void downloadUpdate()
	{
		Bundle updateUrlParams = new Bundle();
		updateUrlParams.putString("SERVER", _featureRegister.getPrefs().getString(FeatureRegister.Param.ABMP_SERVER));
		updateUrlParams.putString("BOX_ID", _featureRegister.getBoxId());
		updateUrlParams.putString("FILE_NAME", _updateData.FileName);
		String updateUrl = getPrefs().getString(Param.ABMP_UPDATE_DOWNLOAD_URL, updateUrlParams);
		final String updateFile = getPrefs().getString(Param.UPDATES_DIR) + "/" + Files.baseName(_updateData.FileName);

		Bundle downloadParams = new Bundle();
		downloadParams.putString(DownloadService.Extras.URL.name(), updateUrl);
		downloadParams.putString(DownloadService.Extras.LOCAL_FILE.name(), updateFile);
		// FIXME: Add other params like md5, proxy, timeouts etc.

		// start download
		_updateDownloadStarted = true;
		_updateDownloadFinished = false;
		_featureInternet.downloadFile(downloadParams, new OnResultReceived()
		{
			@Override
			public void onReceiveResult(int resultCode, Bundle resultData)
			{
				if (DownloadService.RESULT_PROGRESS == resultCode)
				{
					float progress = resultData.getFloat(DownloadService.ResultExtras.PROGRESS.name());
					Log.i(TAG, "File download progress " + progress);
					getEventMessenger().trigger(ON_UPDATE_PROGRESS, resultData);
				}
				else if (DownloadService.DOWNLOAD_SUCCESS == resultCode)
				{
					Log.e(TAG, ".downloadUpdate: download success");
					// Download finished
					_updateDownloadStarted = false;
					_updateDownloadFinished = true;

					Prefs userPrefs = Environment.getInstance().getUserPrefs();
					userPrefs.put(UserParam.UPGRADE_FILE, updateFile);
					userPrefs.put(UserParam.UPGRADE_VERSION, _updateData.Version);
					userPrefs.put(UserParam.UPGRADE_BRAND, _updateData.Brand);
					getEventMessenger().trigger(ON_UPDATE_DOWNLOAD_FINISHED, resultData);
				}
				else if (DownloadService.DOWNLOAD_FAILED == resultCode)
				{
					Log.e(TAG, ".downloadUpdate: download failed");
					getEventMessenger().trigger(ON_UPDATE_ERROR);
				}
			}
		});
	}

	private boolean hasNewVersion(String currentVersion, String otherVersion, String brand)
	{
		if (currentVersion == null || otherVersion == null)
			throw new IllegalArgumentException("Arguments cannot be null");

		// check if BoxID is reassigned to new brand to allow FW update
		boolean isNewBrand = !(TextUtils.isEmpty(brand) || _featureRegister.getPrefs().getString(FeatureRegister.Param.BRAND)
		        .equalsIgnoreCase(brand));
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

		Log.i(TAG, ".hasNewVersion: Failed comparing versions current=" + currentVersion + " and other=" + otherVersion
		        + ". Assuming new version!");
		return true;
	}
}

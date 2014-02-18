package com.aviq.tv.android.sdk.feature.softwareupdate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

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
import org.xml.sax.SAXException;

import android.app.IntentService;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.ResultReceiver;

import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.service.BaseService;
import com.aviq.tv.android.sdk.utils.HttpException;

public class UpdateCheckService extends IntentService
{
	private static final String TAG = UpdateCheckService.class.getSimpleName();

	public static final String PARAM_SERVER_URL = "SERVER_URL";

	private String _abmpURL;
	private ResultReceiver _resultReceiver;

	public UpdateCheckService()
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
			_abmpURL = extras.getString(PARAM_SERVER_URL);
		}

		try
		{
			checkServerForUpdate();
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

	private void checkServerForUpdate() throws XPathExpressionException, ParserConfigurationException, IOException,
	        SAXException, NameNotFoundException
	{
		URL newVerURL = new URL(_abmpURL);
		Log.i(TAG, "Checking for new software version from " + newVerURL.toString());

		URLConnection conn = Helpers.openHttpConnection(newVerURL, Helpers.RequestMethod.DEFAULT, null, new Helpers.RedirectCallback()
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
		}, 0);
		InputStream is = conn.getInputStream();

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(is);

		XPathFactory factory = XPathFactory.newInstance();
		XPath xPath = factory.newXPath();
		Element docElement = doc.getDocumentElement();

		Node versionNode = ((NodeList) xPath.evaluate("/SW/version", docElement, XPathConstants.NODESET)).item(0);
		final String version = xPath.evaluate("@name", versionNode);

		String fileName = xPath.evaluate("@url", versionNode);
		String softwareType = xPath.evaluate("@type", versionNode);
		String brand = xPath.evaluate("@brand", versionNode);
		boolean isForced = false;
		try
		{
			isForced = Boolean.parseBoolean(xPath.evaluate("@forced", versionNode));
		}
		catch (XPathExpressionException e)
		{
			Log.w(TAG, e.getMessage());
		}

		long fileSize = 0;
		try
		{
			fileSize = Long.parseLong(xPath.evaluate("@filesize", versionNode));
		}
		catch (NumberFormatException e)
		{
			Log.w(TAG, e.getMessage());
		}

		Log.i(TAG, "Reported software version=" + version + ", fileName=`" + fileName
		        + "', fileSize=" + fileSize + ", softwareType=" + softwareType + ", forced=" + isForced);

		Bundle resultData = new Bundle();
		resultData.putString(FeatureSoftwareUpdate.PARAM_SERVER_VERSION, version);
		resultData.putString(FeatureSoftwareUpdate.PARAM_FILENAME, fileName);
		resultData.putLong(FeatureSoftwareUpdate.PARAM_FILESIZE, fileSize);
		resultData.putString(FeatureSoftwareUpdate.PARAM_SOFTWARE_TYPE, softwareType);
		resultData.putBoolean(FeatureSoftwareUpdate.PARAM_ISFORCED, isForced);
		resultData.putString(FeatureSoftwareUpdate.PARAM_SERVER_BRAND, brand);

		if (_resultReceiver != null)
			_resultReceiver.send(FeatureSoftwareUpdate.ON_UPDATE_CHECK_RESULTS, resultData);
	}
}

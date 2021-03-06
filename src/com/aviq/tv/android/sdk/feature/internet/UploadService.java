/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    UploadService.java
 * Author:      zheliazko
 * Date:        3 Dec 2013
 * Description: IntentService performing file upload
 */
package com.aviq.tv.android.sdk.feature.internet;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.service.BaseService;

public class UploadService extends BaseService
{
	public static final String TAG = UploadService.class.getSimpleName();
	public static final int CONNECT_TIMEOUT = DownloadService.CONNECT_TIMEOUT;
	public static final int WRITE_TIMEOUT = DownloadService.READ_TIMEOUT;

	private int _connTimeout;
	private int _writeTimeout;

	/**
	 * Service extras
	 */
	public enum Extras
	{
		URL, LOCAL_FILE, BUFFER, CA_CERT_PATH, USERNAME, PASSWORD, CONN_TIMEOUT, SOCK_TIMEOUT
	}

	/**
	 * Service result extras
	 */
	public enum ResultExtras
	{
		EXCEPTION
	}

	public UploadService()
	{
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent intent, ResultReceiver resultReceiver)
	{
		String url = intent.getExtras().getString(Extras.URL.name());
		String username = intent.getExtras().getString(Extras.USERNAME.name());
		String password = intent.getExtras().getString(Extras.PASSWORD.name());
		String localFile = intent.getExtras().getString(Extras.LOCAL_FILE.name());
		String buffer = intent.getExtras().getString(Extras.BUFFER.name());
		String caCertPath = intent.getExtras().getString(Extras.CA_CERT_PATH.name());
		_connTimeout = intent.getExtras().getInt(Extras.CONN_TIMEOUT.name(), CONNECT_TIMEOUT);
		_writeTimeout = intent.getExtras().getInt(Extras.SOCK_TIMEOUT.name(), WRITE_TIMEOUT);

		Log.i(TAG, ".onHandleIntent: url = " + url + ", caCertPath = " + caCertPath + ", username = " + username
		        + ", password = " + password + ", localFile = " + localFile + ", buffer size = "
		        + (buffer != null ? buffer.length() : -1) + ", conn timeout = " + _connTimeout + ", sock timeout = "
		        + _writeTimeout);

		if (localFile != null)
		{
			if (!TextUtils.isEmpty(buffer))
			{
				Log.w(TAG, "Sending buffer with size " + buffer.length() + " ignored when uploading local file");
			}
			sendFile(resultReceiver, new File(localFile), url, caCertPath, username, password);
		}
		else
		{
			sendString(resultReceiver, buffer, url, caCertPath, username, password);
		}
	}

	private HttpClient getHttpClient(HttpParams httpParams, String caCertPath) throws CertificateException,
	        IOException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException
	{

		if (TextUtils.isEmpty(caCertPath))
		{
			return new DefaultHttpClient(httpParams);
		}

		// Load CAs from an InputStream
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		InputStream is = getAssets().open(caCertPath);
		InputStream caInput = new BufferedInputStream(is);
		Certificate ca;
		try
		{
			ca = cf.generateCertificate(caInput);
			Log.d(TAG, ".getHttpClient: ca = " + ((X509Certificate) ca).getSubjectDN());
		}
		finally
		{
			caInput.close();
		}

		// Create a KeyStore containing our trusted CAs
		String keyStoreType = KeyStore.getDefaultType();
		KeyStore keyStore = KeyStore.getInstance(keyStoreType);
		keyStore.load(null, null);
		keyStore.setCertificateEntry("ca", ca);

		// Create a TrustManager that trusts the CAs in our KeyStore
		String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
		tmf.init(keyStore);

		// Create an SSLContext that uses our TrustManager
		final SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, tmf.getTrustManagers(), null);

		SSLSocketFactory sslSocketFactory = new SSLSocketFactory(keyStore)
		{
			@Override
			public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException,
			        UnknownHostException
			{
				return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
			}

			@Override
			public Socket createSocket() throws IOException
			{
				return sslContext.getSocketFactory().createSocket();
			}
		};
		Scheme sslTrustAllScheme = new Scheme("https", sslSocketFactory, 30227);
		SchemeRegistry schReg = new SchemeRegistry();
		schReg.register(sslTrustAllScheme);

		ClientConnectionManager conMgr = new ThreadSafeClientConnManager(httpParams, schReg);

		HttpConnectionParams.setConnectionTimeout(httpParams, _connTimeout);
		HttpConnectionParams.setSoTimeout(httpParams, _writeTimeout);

		HttpClient httpClient = new DefaultHttpClient(conMgr, httpParams);
		return httpClient;
	}

	protected void sendData(ResultReceiver resultReceiver, HttpEntity entity, String url, String caCertPath,
	        String username, String password)
	{
		Bundle resultData = new Bundle();
		int resultCode;
		try
		{
			HttpPut httpPut = new HttpPut(url);
			httpPut.setEntity(entity);

			HttpClient httpClient = getHttpClient(httpPut.getParams(), caCertPath);

			Credentials creds = new UsernamePasswordCredentials(username, password);
			((AbstractHttpClient) httpClient).getCredentialsProvider().setCredentials(new AuthScope(null, -1), creds);

			HttpResponse response = httpClient.execute(httpPut);
			resultCode = response.getStatusLine().getStatusCode();
			if (resultCode == 201 || resultCode == 204)
				resultCode = ResultCode.OK;
		}
		catch (Exception e)
		{
			FeatureError fe = new FeatureError(e);
			Log.e(TAG, "Cannot send data to " + url, fe);
			resultCode = fe.getCode();
			resultData.putSerializable(ResultExtras.EXCEPTION.name(), fe);
		}
		if (resultReceiver != null)
			resultReceiver.send(resultCode, resultData);
	}

	protected void sendFile(ResultReceiver resultReceiver, File file, String url, String caCertPath, String username,
	        String password)
	{
		HttpEntity entity = new FileEntity(file, "application/octet-stream");

		// append trailing slash if needed
		if (url.charAt(url.length() - 1) != '/')
			url = url + '/';
		url += file.getName();
		sendData(resultReceiver, entity, url, caCertPath, username, password);
	}

	protected void sendString(ResultReceiver resultReceiver, String data, String url, String caCertPath,
	        String username, String password)
	{
		HttpEntity entity;
		try
		{
			entity = new StringEntity(data);
			sendData(resultReceiver, entity, url, caCertPath, username, password);
		}
		catch (UnsupportedEncodingException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
	}
}

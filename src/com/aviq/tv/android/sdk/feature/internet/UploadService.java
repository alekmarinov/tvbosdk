package com.aviq.tv.android.sdk.feature.internet;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import java.util.zip.GZIPOutputStream;

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
import org.apache.http.params.HttpParams;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

public class UploadService extends IntentService
{
	public static final String TAG = UploadService.class.getSimpleName();

	public static final String EXTRA_REPORT_URL = "REPORT_URL";
	public static final String EXTRA_REPORT_URL_USER = "REPORT_URL_USER";
	public static final String EXTRA_REPORT_URL_PASS = "REPORT_URL_PASS";
	public static final String EXTRA_REPORT_NAME = "REPORT_NAME";
	public static final String EXTRA_REPORT_DATA = "REPORT_DATA";
	public static final String EXTRA_CA_CERT_PATH = "CA_CERT_PATH";

	private String _reportUrl;
	private String _reportUrlUser;
	private String _reportUrlPass;
	private String _reportName;
	private String _reportData;
	private String _caCertPath;

	public UploadService()
	{
		super(TAG);
	}

	public UploadService(String name)
	{
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
		_reportUrl = intent.getExtras().getString(EXTRA_REPORT_URL);
		_reportUrlUser = intent.getExtras().getString(EXTRA_REPORT_URL_USER);
		_reportUrlPass = intent.getExtras().getString(EXTRA_REPORT_URL_PASS);
		_reportName = intent.getExtras().getString(EXTRA_REPORT_NAME);
		_reportData = intent.getExtras().getString(EXTRA_REPORT_DATA);
		_caCertPath = intent.getExtras().getString(EXTRA_CA_CERT_PATH);

		sendData(_reportData, _reportName);
	}

	private HttpClient getHttpClient(HttpParams httpParams) throws CertificateException, IOException,
	        KeyStoreException, NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException
	{

		if (TextUtils.isEmpty(_caCertPath))
		{
			return new DefaultHttpClient(httpParams);
		}

		// Load CAs from an InputStream
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		InputStream is = getAssets().open(_caCertPath);
		InputStream caInput = new BufferedInputStream(is);
		Certificate ca;
		try
		{
			ca = cf.generateCertificate(caInput);
			System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
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

		HttpClient httpClient = new DefaultHttpClient(conMgr, httpParams);
		return httpClient;
	}

	private void sendData(HttpEntity entity, String remoteFileName)
	{
		try
		{
			HttpPut httpPut = new HttpPut(_reportUrl + remoteFileName);
			httpPut.setEntity(entity);

			HttpClient httpClient = getHttpClient(httpPut.getParams());

			Credentials creds = new UsernamePasswordCredentials(_reportUrlUser, _reportUrlPass);
			((AbstractHttpClient) httpClient).getCredentialsProvider().setCredentials(new AuthScope(null, -1), creds);

			Log.i(TAG, ".sendData: _reportUrl = " + _reportUrl + remoteFileName + ", _reportUrlUser = "
			        + _reportUrlUser + ", _reportUrlPass = " + _reportUrlPass);
			HttpResponse response = httpClient.execute(httpPut);
		}
		catch (Exception e)
		{
			Log.e(TAG, "Cannot send event report.", e);
		}
	}

	protected void sendData(File file)
	{
		HttpEntity entity = new FileEntity(file, "application/octet-stream");
		sendData(entity, file.getName());
	}

	protected void sendData(File file, String remoteFileName)
	{
		HttpEntity entity = new FileEntity(file, "application/octet-stream");
		sendData(entity, remoteFileName);
	}

	protected void sendData(String data, String remoteFileName)
	{
		HttpEntity entity;
		try
		{
			entity = new StringEntity(data);
			sendData(entity, remoteFileName);
		}
		catch (UnsupportedEncodingException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
	}

	protected boolean zip(String srcFileName, String destFileName)
	{
		FileInputStream in = null;
		GZIPOutputStream gzipOut = null;
		try
		{
			in = new FileInputStream(srcFileName);
			gzipOut = new GZIPOutputStream(openFileOutput(destFileName, Context.MODE_PRIVATE));

			byte[] buffer = new byte[8192];

			int nread = in.read(buffer);
			while (nread > 0)
			{
				gzipOut.write(buffer, 0, nread);
				if (nread != buffer.length)
					break;
				nread = in.read(buffer);
			}
		}
		catch (FileNotFoundException e)
		{
			Log.e(TAG, "Source file not found", e);
			return false;
		}
		catch (IOException e)
		{
			Log.e(TAG, "IO error", e);
			return false;
		}
		finally
		{
			try
			{
				if (in != null)
					in.close();

				if (gzipOut != null)
					gzipOut.close();
			}
			catch (IOException e2)
			{
			}
		}
		return true;
	}
}

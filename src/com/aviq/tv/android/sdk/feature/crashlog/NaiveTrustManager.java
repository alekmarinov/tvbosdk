package com.aviq.tv.android.sdk.feature.crashlog;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

/**
 * Accepts any certificate, ideal for self-signed certificates.
 */
class NaiveTrustManager implements X509TrustManager
{
	@Override
	public X509Certificate[] getAcceptedIssuers()
	{
		return new X509Certificate[0];
	}

	@Override
	public void checkClientTrusted(X509Certificate[] x509CertificateArray, String string) throws CertificateException
	{
	}

	@Override
	public void checkServerTrusted(X509Certificate[] x509CertificateArray, String string) throws CertificateException
	{
	}
}

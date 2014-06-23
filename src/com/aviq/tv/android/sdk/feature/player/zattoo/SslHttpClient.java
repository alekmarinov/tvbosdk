/**
 * Copyright 2013 Ognyan Bankov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aviq.tv.android.sdk.feature.player.zattoo;

import khandroid.ext.apache.http.conn.ClientConnectionManager;
import khandroid.ext.apache.http.conn.scheme.PlainSocketFactory;
import khandroid.ext.apache.http.conn.scheme.Scheme;
import khandroid.ext.apache.http.conn.scheme.SchemeRegistry;
import khandroid.ext.apache.http.conn.ssl.SSLSocketFactory;
import khandroid.ext.apache.http.impl.client.DefaultHttpClient;
import khandroid.ext.apache.http.impl.conn.PoolingClientConnectionManager;

public class SslHttpClient extends DefaultHttpClient
{
	private static final int HTTP_DEFAULT_PORT = 80;
	private static final String HTTP_SCHEME = "http";
	private static final int HTTP_DEFAULT_HTTPS_PORT = 443;
	private static final String HTTP_SSL_SCHEME = "https";

	private int mHttpsPort = HTTP_DEFAULT_HTTPS_PORT;

	@Override
	protected ClientConnectionManager createClientConnectionManager()
	{
		SchemeRegistry registry = new SchemeRegistry();

		PlainSocketFactory pfs = PlainSocketFactory.getSocketFactory();
		Scheme s = new Scheme(HTTP_SCHEME, HTTP_DEFAULT_PORT, pfs);
		registry.register(s);
		registry.register(new Scheme(HTTP_SSL_SCHEME, mHttpsPort, SSLSocketFactory.getSocketFactory()));
		return new PoolingClientConnectionManager(registry);
	}

	public void setHttpsPort(int httpsPort)
	{
		mHttpsPort = httpsPort;
	}
}

package com.aviq.tv.android.sdk.feature.crashlog;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import com.aviq.tv.android.sdk.core.Log;

public final class HttpRequest
{
	private static final String TAG = HttpRequest.class.getSimpleName();

	/**
	 * Available HTTP methods to send data. Only POST and PUT are currently
	 * supported.
	 */
	public enum Method
	{
		POST, PUT
	}

	/**
	 * Type of report data encoding, currently supports Html Form encoding and
	 * JSON.
	 */
	public enum Type
	{
		/**
		 * Send data as a www form encoded list of key/values.
		 */
		FORM
		{
			@Override
			public String getContentType()
			{
				return "application/x-www-form-urlencoded";
			}
		},
		/**
		 * Send data as a structured JSON tree.
		 */
		JSON
		{
			@Override
			public String getContentType()
			{
				return "application/json";
			}
		};

		public abstract String getContentType();
	}

	private static class SocketTimeOutRetryHandler implements HttpRequestRetryHandler
	{
		private final HttpParams httpParams;
		private final int maxNrRetries;

		/**
		 * @param httpParams
		 *            HttpParams that will be used in the HttpRequest.
		 * @param maxNrRetries
		 *            Max number of times to retry Request on failure due to
		 *            SocketTimeOutException.
		 */
		private SocketTimeOutRetryHandler(HttpParams httpParams, int maxNrRetries)
		{
			this.httpParams = httpParams;
			this.maxNrRetries = maxNrRetries;
		}

		@Override
		public boolean retryRequest(IOException exception, int executionCount, HttpContext context)
		{
			if (exception instanceof SocketTimeoutException)
			{
				if (executionCount <= maxNrRetries)
				{
					if (httpParams != null)
					{
						final int newSocketTimeOut = HttpConnectionParams.getSoTimeout(httpParams) * 2;
						HttpConnectionParams.setSoTimeout(httpParams, newSocketTimeOut);
						Log.d(TAG, "SocketTimeOut - increasing time out to " + newSocketTimeOut
						        + " millis and trying again");
					}
					else
					{
						Log.d(TAG,
						        "SocketTimeOut - no HttpParams, cannot increase time out. Trying again with current settings");
					}

					return true;
				}

				Log.d(TAG, "SocketTimeOut but exceeded max number of retries: " + maxNrRetries);
			}

			return false;
		}
	}

	private String login;
	private String password;
	private int connectionTimeOut = 3000;
	private int socketTimeOut = 3000;
	private int maxNrRetries = 3;
	private Map<String, String> headers;
	private boolean sslCertValidation = true;
	private String mMethod;
	private String mContentType;

	public HttpRequest()
	{
	}

	public void setLogin(String login)
	{
		this.login = login;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	public void setConnectionTimeOut(int connectionTimeOut)
	{
		this.connectionTimeOut = connectionTimeOut;
	}

	public void setSocketTimeOut(int socketTimeOut)
	{
		this.socketTimeOut = socketTimeOut;
	}

	public void setHeaders(Map<String, String> headers)
	{
		this.headers = headers;
	}

	/**
	 * The default number of retries is 3.
	 *
	 * @param maxNrRetries
	 *            Max number of times to retry Request on failure due to
	 *            SocketTimeOutException.
	 */
	public void setMaxNrRetries(int maxNrRetries)
	{
		this.maxNrRetries = maxNrRetries;
	}

	public void enableSSLCertValidation()
	{
		sslCertValidation = true;
	}

	public void disableSSLCertValidation()
	{
		sslCertValidation = false;
	}

	/**
	 * Posts to a URL.
	 *
	 * @param url
	 *            URL to which to post.
	 * @param method
	 *            POST / PUT
	 * @param content
	 *            Map of parameters to post to a URL.
	 * @param contentType
	 *            HTTP content type
	 * @throws IOException
	 *             if the data cannot be posted.
	 */
	public void send(URL url, String method, String content, String contentType) throws IOException
	{
		final HttpClient httpClient = getHttpClient();
		final HttpEntityEnclosingRequestBase httpRequest = getHttpRequest(url, method, content, contentType);

		Log.d(TAG, "Sending request to " + url + ", method = " + method);

		HttpResponse response = null;
		try
		{
			response = httpClient.execute(httpRequest, new BasicHttpContext());
			if (response != null)
			{
				final StatusLine statusLine = response.getStatusLine();
				if (statusLine != null)
				{
					final String statusCode = Integer.toString(response.getStatusLine().getStatusCode());

					// 409 return code means that the report has been received
					// already. So we can discard it.

					// a 403 error code is an explicit data validation refusal
					// from the server. The request must not be repeated.
					// Discard it.

					if (!statusCode.equals("409") && !statusCode.equals("403")
					        && (statusCode.startsWith("4") || statusCode.startsWith("5")))
					{
						Log.d(TAG, "Could not send HttpPost: " + httpRequest);
						Log.d(TAG, "HttpResponse status: "
						        + (statusLine != null ? statusLine.getStatusCode() : "NoStatusLine#noCode"));
						final String respContent = EntityUtils.toString(response.getEntity());
						Log.d(TAG,
						        "HttpResponse content: "
						                + respContent.substring(0, Math.min(respContent.length(), 200)));
						throw new IOException("Host returned error code " + statusCode);
					}
				}

				Log.d(TAG, "HttpResponse status: "
				        + (statusLine != null ? statusLine.getStatusCode() : "NoStatusLine#noCode"));
				final String respContent = EntityUtils.toString(response.getEntity());
				Log.d(TAG, "HttpResponse content : " + respContent.substring(0, Math.min(respContent.length(), 200)));

			}
			else
			{
				Log.d(TAG, "No HTTP response!");
			}
		}
		finally
		{
			if (response != null)
			{
				response.getEntity().consumeContent();
			}
		}
	}

	/**
	 * @return HttpClient to use with this HttpRequest.
	 */
	private HttpClient getHttpClient()
	{
		final HttpParams httpParams = new BasicHttpParams();
		httpParams.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.RFC_2109);
		HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeOut);
		HttpConnectionParams.setSoTimeout(httpParams, socketTimeOut);
		HttpConnectionParams.setSocketBufferSize(httpParams, 8192);

		final SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", new PlainSocketFactory(), 80));

		if (!sslCertValidation)
		{
			registry.register(new Scheme("https", (new FakeSocketFactory()), 443));
		}
		else
		{
			registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
		}

		final ClientConnectionManager clientConnectionManager = new ThreadSafeClientConnManager(httpParams, registry);
		final DefaultHttpClient httpClient = new DefaultHttpClient(clientConnectionManager, httpParams);

		final HttpRequestRetryHandler retryHandler = new SocketTimeOutRetryHandler(httpParams, maxNrRetries);
		httpClient.setHttpRequestRetryHandler(retryHandler);

		return httpClient;
	}

	/**
	 * @return Credentials to use with this HttpRequest or null if no
	 *         credentials were supplied.
	 */
	private UsernamePasswordCredentials getCredentials()
	{
		if (login != null || password != null)
		{
			return new UsernamePasswordCredentials(login, password);
		}
		return null;
	}

	private HttpEntityEnclosingRequestBase getHttpRequest(URL url, String method, String content, String contentType)
	        throws UnsupportedEncodingException, UnsupportedOperationException
	{

		final HttpEntityEnclosingRequestBase httpRequest;
		if ("POST".equalsIgnoreCase(method))
			httpRequest = new HttpPost(url.toString());
		else if ("PUT".equalsIgnoreCase(method))
			httpRequest = new HttpPut(url.toString());
		else
			throw new UnsupportedOperationException("Unknown method: " + method);

		final UsernamePasswordCredentials creds = getCredentials();
		if (creds != null)
		{
			httpRequest.addHeader(BasicScheme.authenticate(creds, "UTF-8", false));
		}
		httpRequest.setHeader("User-Agent", "Android");
		httpRequest
		        .setHeader("Accept",
		                "text/html,application/xml,application/json,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
		httpRequest.setHeader("Content-Type", contentType);

		if (headers != null)
		{
			Iterator<String> headerIt = headers.keySet().iterator();
			while (headerIt.hasNext())
			{
				String header = headerIt.next();
				String value = headers.get(header);
				httpRequest.setHeader(header, value);
			}
		}

		httpRequest.setEntity(new StringEntity(content, "UTF-8"));

		return httpRequest;
	}

	/**
	 * Converts a Map of parameters into a URL encoded Sting.
	 *
	 * @param parameters
	 *            Map of parameters to convert.
	 * @return URL encoded String representing the parameters.
	 * @throws UnsupportedEncodingException
	 *             if one of the parameters couldn't be converted to UTF-8.
	 */
	public static String getParamsAsFormString(Map<?, ?> parameters) throws UnsupportedEncodingException
	{
		final StringBuilder dataBfr = new StringBuilder();
		for (final Object key : parameters.keySet())
		{
			if (dataBfr.length() != 0)
			{
				dataBfr.append('&');
			}
			final Object preliminaryValue = parameters.get(key);
			final Object value = (preliminaryValue == null) ? "" : preliminaryValue;
			dataBfr.append(URLEncoder.encode(key.toString(), "UTF-8"));
			dataBfr.append('=');
			dataBfr.append(URLEncoder.encode(value.toString(), "UTF-8"));
		}
		return dataBfr.toString();
	}
}

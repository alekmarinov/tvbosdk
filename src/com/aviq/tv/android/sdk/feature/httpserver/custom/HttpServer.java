/**
 * Copyright (c) 2007-2013, AVIQ Systems AG
 *
 * Project:     AVIQTV
 * Filename:    HttpServer.java
 * Author:      alek
 * Description: WebServer utility component hosting local site in either /assets or /files directory
 */

package com.aviq.tv.android.sdk.feature.httpserver.custom;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.StringTokenizer;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;

import android.content.Context;

import com.aviq.tv.android.sdk.core.Log;

public class HttpServer implements Runnable
{
	private static final String TAG = HttpServer.class.getName();

	public static final int SOCKET_TIMEOUT = 5000;

	// Web Server port (0 - autoselect)
	private int _port = 0;

	// true while server is running
	private boolean _isRunning = false;

	// app context
	private Context _context;

	// server socket
	private ServerSocket _socket;

	// web worker thread
	private Thread _thread;

	// custom request handler
	private RequestHandler _requestHandler;

	// true if web dir is located in /assets, otherwise in /files
	private boolean _isAssets = true;

	// local web path relative to /files or /assets depending on
	// isAssetsWebDir()
	private String _localPath;

	/**
	 * Request handler interface
	 */
	public interface RequestHandler
	{
		/**
		 * Callback for every client request.
		 *
		 * @param request
		 * @param response
		 * @param client
		 * @return true if request has been handled, false tells the web server
		 *         to return the request file directly (if exists)
		 */
		boolean handleRequest(HttpRequest request, HttpResponse response, Socket client);
	}

	/**
	 * Instantiate new AssetsHttpServer object
	 *
	 * @param context
	 */
	public HttpServer(Context context)
	{
		this(context, null);
	}

	/**
	 * Instantiate new AssetsHttpServer object
	 *
	 * @param context
	 * @param requestHandler
	 */
	public HttpServer(Context context, RequestHandler requestHandler)
	{
		this._context = context;
		_requestHandler = requestHandler;
	}

	/**
	 * Creates server socket and start listening
	 *
	 * @throws SocketException
	 */
	public void create() throws IOException
	{
		_socket = new ServerSocket(_port, 0, InetAddress.getByAddress(new byte[]
		{ 127, 0, 0, 1 }));
		_socket.setSoTimeout(SOCKET_TIMEOUT);
		_port = _socket.getLocalPort();
		Log.i(TAG, ".create: HTTP server listens on port " + _port);
	}

	/**
	 * Creates server socket and start listening
	 */
	public void destroy()
	{
		Log.i(TAG, ".destroy");
		if (_socket != null)
		{
			try
			{
				_socket.close();
			}
			catch (IOException e)
			{
				Log.e(TAG, "Error closing server socket", e);
			}
			_socket = null;
		}
	}

	/**
	 * Returns the port the WebServer is listening on
	 */
	public int getPort()
	{
		return _port;
	}

	/**
	 * Start accepting client connections
	 */
	public void start()
	{
		Log.i(TAG, ".start");
		if (_socket == null)
		{
			throw new IllegalStateException("Cannot start server; it has not been initialized.");
		}

		_thread = new Thread(this);
		_isRunning = true;
		_thread.start();
		Log.i(TAG, "Web server started listening for client connections");
	}

	/**
	 * Stop accepting client connections
	 */
	public void stop()
	{
		Log.i(TAG, ".stop");
		_isRunning = false;

		if (_thread == null)
		{
			throw new IllegalStateException("Cannot stop server; it has not been started.");
		}

		_thread.interrupt();
		try
		{
			_thread.join(5000);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		Log.i(TAG, "Web server stopped");
	}

	@Override
	public void run()
	{
		while (_isRunning && _socket != null)
		{
			try
			{
				Socket client = _socket.accept();
				Log.d(TAG, client + " connected");
				if (client == null)
				{
					continue;
				}
				HttpRequest request = readRequest(client);
				processRequest(request, client);
			}
			catch (SocketTimeoutException e)
			{
				// Do nothing
			}
			catch (IOException e)
			{
				Log.e(TAG, "I/O error", e);
			}
		}
		Log.d(TAG, "Server interrupted. Shutting down.");
	}

	private void processRequest(HttpRequest request, Socket client) throws IllegalStateException, IOException
	{
		if (request == null)
		{
			Log.e(TAG, "got null request");
			return;
		}

		String uri = request.getRequestLine().getUri();
		// strip leading '/'
		if (uri.charAt(0) == '/')
			uri = uri.substring(1);

		Log.d(TAG, ".processRequest: uri = " + uri);

		// handle custom requests
		HttpResponse response = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1), 200, null);
		if (_requestHandler == null || !_requestHandler.handleRequest(request, response, client))
		{
			// proceed as simple file handler
			InputStream is = null;

			int code = 200;
			try
			{
				int qi = uri.lastIndexOf('?');
				if (qi >= 0)
					uri = uri.substring(0, qi);

				if (_localPath != null)
					uri += _localPath + '/' + uri;
				if (_isAssets)
				{
					is = _context.getAssets().open(uri);
				}
				else
				{
					String filePath = _context.getFilesDir().getAbsolutePath() + '/' + uri;
					is = new FileInputStream(filePath);
				}
			}
			catch (IOException e)
			{
				code = 404;
			}
			response.setStatusCode(code);

			if (is != null)
			{
				response.setHeader("Content Type", "text/html");
				response.setHeader("Content Length", "" + is.available());
			}

			StringBuilder httpString = new StringBuilder();
			httpString.append(response.getStatusLine().toString());

			httpString.append("\n");
			for (Header h : response.getAllHeaders())
			{
				httpString.append(h.getName()).append(": ").append(h.getValue()).append("\n");
			}
			httpString.append("\n");

			int bytesTotal = 0;
			try
			{
				byte[] buffer = httpString.toString().getBytes();
				int readBytes = -1;
				client.getOutputStream().write(buffer, 0, buffer.length);

				if (is != null)
				{
					// Start streaming content.
					byte[] buff = new byte[1024 * 50];
					while (_isRunning && (readBytes = is.read(buff, 0, buff.length)) != -1)
					{
						client.getOutputStream().write(buff, 0, readBytes);
						bytesTotal += readBytes;
					}
				}
			}
			catch (Exception e)
			{
				Log.e("", e.getMessage(), e);
			}
			finally
			{
				if (is != null)
				{
					is.close();
				}
				client.close();
			}
			Log.d(TAG, ".processRequest: " + bytesTotal + " bytes returned with code " + code);
		}
	}

	private HttpRequest readRequest(Socket client)
	{
		HttpRequest request = null;
		InputStream is;
		String firstLine;
		try
		{
			is = client.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			firstLine = reader.readLine();
		}
		catch (IOException e)
		{
			Log.e(TAG, "Error parsing request", e);
			return request;
		}

		if (firstLine == null)
		{
			Log.i(TAG, "Client closed connection without a request.");
			return request;
		}

		StringTokenizer st = new StringTokenizer(firstLine);
		String method = st.nextToken();
		String uri = st.nextToken();
		String realUri = uri.substring(1);
		request = new BasicHttpRequest(method, realUri);
		return request;
	}

	/**
	 * Sets web server dir located in /files
	 */
	public void setFilesWebDir()
	{
		_isAssets = false;
	}

	/**
	 * Sets web server dir located in /assets
	 */
	public void setAssetsWebDir()
	{
		_isAssets = true;
	}

	/**
	 * @return true if the web dir is relative to /assets
	 */
	public boolean isAssetsWebDir()
	{
		return _isAssets;
	}

	/**
	 * Sets local web path relative to /files or /assets depending on
	 * isAssetsWebDir()
	 *
	 * @param localPath
	 */
	public void setLocalWebPath(String localPath)
	{
		_localPath = localPath;
	}
}

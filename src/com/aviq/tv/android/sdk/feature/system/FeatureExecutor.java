/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureExecutor.java
 * Author:      alek
 * Date:        3 Mar 2014
 * Description: Provides application's root access via the executor service
 */

package com.aviq.tv.android.sdk.feature.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.feature.annotation.Author;

/**
 * Provides application's root access via the executor service
 */
@Author("alek")
public class FeatureExecutor extends FeatureComponent
{
	public static final String TAG = FeatureExecutor.class.getSimpleName();
	public static final String DEFAULT_HOST = "localhost";
	public static final int DEFAULT_PORT = 6869;
	private BufferedReader _bufferedReader;
	private final BlockingQueue<String> _cmdQueue = new LinkedBlockingQueue<String>();
	private Socket _socket;
	private OutputStream _outputStream;
	private int _port;
	private String _host;

	public static enum Param
	{
		/**
		 * Executor port number
		 */
		PORT(DEFAULT_PORT),

		/**
		 * Executor host address
		 */
		HOST(DEFAULT_HOST);

		Param(int value)
		{
			if (Environment.getInstance() != null)
				Environment.getInstance().getFeaturePrefs(FeatureName.Component.SYSTEM).put(name(), value);
		}

		Param(String value)
		{
			if (Environment.getInstance() != null)
				Environment.getInstance().getFeaturePrefs(FeatureName.Component.SYSTEM).put(name(), value);
		}
	}

	public FeatureExecutor() throws FeatureNotFoundException
	{
	}

	public FeatureExecutor(String host, int port)
	{
		_host = host;
		_port = port;
	}

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		if (_host == null && _port == 0)
		{
			_host = getPrefs().getString(Param.HOST);
			_port = getPrefs().getInt(Param.PORT);
		}
		connect(new Runnable()
		{
			@Override
			public void run()
			{
				FeatureExecutor.super.initialize(onFeatureInitialized);
			}
		});
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.SYSTEM;
	}

	public void command(final String cmd)
	{
		Log.i(TAG, "Adding `" + cmd + "' to execution queue");
		_cmdQueue.add(cmd);
	}

	private Thread _readerThread = new Thread(new Runnable()
	{
		@Override
		public void run()
		{
			try
			{
				Log.i(TAG, "Reader thread started");
				while (true)
				{
					String res = _bufferedReader.readLine();
					Log.i(TAG, "got " + res);
				}
			}
			catch (IOException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
		}
	});

	private Thread _writerThread = new Thread(new Runnable()
	{
		@Override
		public void run()
		{
			try
			{
				Log.i(TAG, "Writer thread started");
				while (true)
				{
					String cmd;
					try
					{
						cmd = _cmdQueue.take();
						Log.i(TAG, "sending `" + cmd + "'");
						_outputStream.write((cmd + "\n").getBytes());
					}
					catch (InterruptedException e)
					{
						Log.e(TAG, e.getMessage(), e);
					}
				}
			}
			catch (IOException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
		}
	});

	private void connect(final Runnable callback)
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				disconnect();
				Log.i(TAG, "Connecting " + _host + ":" + _port);
				_socket = new Socket();
				SocketAddress sockaddr = new InetSocketAddress(_host, _port);
				try
				{
					_socket.connect(sockaddr, 5000);
					_outputStream = _socket.getOutputStream();
					InputStream is = _socket.getInputStream();
					_bufferedReader = new BufferedReader(new InputStreamReader(is));
					_readerThread.start();
					_writerThread.start();
				}
				catch (IOException e)
				{
					Log.e(TAG, e.getMessage(), e);
				}
				finally
				{
					if (Environment.getInstance() != null)
						Environment.getInstance().runOnUiThread(callback);
				}
			}
		}).start();
	}

	private void disconnect()
	{
		try
		{
			if (_socket != null)
			{
				Log.i(TAG, "disconnecting");
				_outputStream.close();
				_bufferedReader.close();
				_socket.close();
				_socket = null;
			}
		}
		catch (IOException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
	}
}

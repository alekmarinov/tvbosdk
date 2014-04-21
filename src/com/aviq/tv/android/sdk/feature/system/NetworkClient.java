/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    NetworkClient.java
 * Author:      alek
 * Date:        19 Apr 2014
 * Description: Network client utility
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

import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;

/**
 * Network client utility
 */
public class NetworkClient
{
	public static final String TAG = NetworkClient.class.getSimpleName();
	private final BlockingQueue<String> _cmdQueue = new LinkedBlockingQueue<String>();
	private Socket _socket;
	private BufferedReader _bufferedReader;
	private OutputStream _outputStream;
	private String _host;
	private int _port;
	private OnNetworkEvent _onNetworkEvent;

	public interface OnNetworkEvent
	{
		void onDataReceived(String data);

		void onConnected(boolean success);

		void onDisconnected();
	}

	public NetworkClient(String host, int port, OnNetworkEvent onNetworkEvent)
	{
		_host = host;
		_port = port;
		_onNetworkEvent = onNetworkEvent;
	}

	public void command(final String cmd)
	{
		Log.i(TAG, "Adding `" + cmd.trim() + "' to execution queue");
		_cmdQueue.add(cmd);
	}

	public void connect()
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
					_onNetworkEvent.onConnected(true);
				}
				catch (IOException e)
				{
					Log.e(TAG, e.getMessage(), e);
					_onNetworkEvent.onConnected(false);
				}
			}
		}).start();
	}

	public void disconnect()
	{
		try
		{
			if (_socket != null)
			{
				Log.i(TAG, "disconnecting");
				_outputStream.close();
				_bufferedReader.close();
				_socket.close();
				_onNetworkEvent.onDisconnected();
			}
		}
		catch (IOException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
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
					Thread.sleep(100);
					final String res = _bufferedReader.readLine();
					Log.v(TAG, "got " + res);
					Environment.getInstance().runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							_onNetworkEvent.onDataReceived(res);
						}
					});
				}
			}
			catch (IOException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
            catch (InterruptedException e)
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
						Log.i(TAG, "sending `" + cmd.trim() + "'");
						_outputStream.write((cmd + "\n").getBytes());
						_outputStream.flush();
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
}

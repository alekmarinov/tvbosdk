/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    LuaRPC.java
 * Author:      alek
 * Date:        7 Feb 2014
 * Description: Lua based RPC scripting service
 */

package com.aviq.tv.android.sdk.feature.rpc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.keplerproject.luajava.JavaFunction;
import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaState;
import org.keplerproject.luajava.LuaStateFactory;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;

/**
 * Lua based RPC scripting service
 */
public class FeatureLuaRPC extends FeatureComponent
{
	public static final String TAG = FeatureLuaRPC.class.getSimpleName();
	private static final String EOL = "-- End of Lua";

	public enum Param
	{
		/**
		 * RPC server port
		 */
		PORT(6768);

		Param(int value)
		{
			// Environment.getInstance().getFeaturePrefs(FeatureName.Component.RPC).put(name(),
			// value);
		}
	}

	private ServerThread _serverThread;

	public FeatureLuaRPC()
	{
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		try
		{
			// int port = getPrefs().getInt(Param.PORT);
			int port = 6768;
			ServerSocket serverSocket = new ServerSocket(port);
			_serverThread = new ServerThread(serverSocket);
			_serverThread.start();

			super.initialize(onFeatureInitialized);
		}
		catch (IOException e)
		{
			Log.e(TAG, e.getMessage(), e);
			if (onFeatureInitialized != null)
				onFeatureInitialized.onInitialized(this, ResultCode.GENERAL_FAILURE);
		}
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.RPC;
	}

	private static class ClientThread extends Thread
	{
		private Socket _clientSocket;
		private OutputStreamWriter _clientWriter;

		ClientThread(Socket clientSocket)
		{
			super("client: " + clientSocket);
			_clientSocket = clientSocket;
		}

		private static byte[] readAll(InputStream input) throws IOException
		{
			ByteArrayOutputStream output = new ByteArrayOutputStream(4096);
			byte[] buffer = new byte[4096];
			int n = 0;
			while (-1 != (n = input.read(buffer)))
			{
				output.write(buffer, 0, n);
				String line = new String(buffer, 0, n - 2);
				Log.i(TAG, "line = `" + line + "', EOL = `" + EOL + "' -> " + line.length() + "/" + EOL.length() + ":"
				        + (line.indexOf(EOL)));
				if (line.indexOf(EOL) >= 0)
					break;
			}
			return output.toByteArray();
		}

		@Override
		public void run()
		{
			try
			{
				final LuaState L = LuaStateFactory.newLuaState();
				L.openLibs();

				JavaFunction print = new JavaFunction(L)
				{
					@Override
					public int execute() throws LuaException
					{
						try
						{
							for (int i = 2; i <= L.getTop(); i++)
							{
								int type = L.type(i);
								String stype = L.typeName(type);
								String val = null;
								if (stype.equals("userdata"))
								{
									Object obj = L.toJavaObject(i);
									if (obj != null)
										val = obj.toString();
								}
								else if (stype.equals("boolean"))
								{
									val = L.toBoolean(i) ? "true" : "false";
								}
								else
								{
									val = L.toString(i);
								}
								if (val == null)
									val = stype;
								_clientWriter.append(val);

								Log.i(TAG, val);
							}
							_clientWriter.append("\t");
							_clientWriter.append("\n");
							_clientWriter.flush();
							return 0;
						}
						catch (IOException e)
						{
							throw new LuaException(e);
						}
					}
				};
				print.register("print");

				InputStream clientInput = _clientSocket.getInputStream();
				OutputStream clientOutput = _clientSocket.getOutputStream();
				_clientWriter = new OutputStreamWriter(clientOutput);
				_clientWriter.write("Lua output follows...\r\n");
				_clientWriter.flush();

				byte[] luaBuffer = readAll(clientInput);
				Log.i(TAG, new String(luaBuffer));
				L.setTop(0);
				final int ok = L.LloadBuffer(luaBuffer, "lua");
				Log.i(TAG, "loaded ..." + ok);
				if (ok == 0)
				{
					Environment.getInstance().getActivity().runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							Log.i(TAG, "starting lua...");
							L.getGlobal("debug");
							L.getField(-1, "traceback");
							L.remove(-2);
							L.insert(-2);
							int ok = L.pcall(0, 0, -2);
							try
							{
								_clientWriter.write("Lua output finished!\r\n");
								_clientWriter.flush();
								if (ok != 0)
									throw new LuaException(errorReason(ok) + ": " + L.toString(-1));
							}
							catch (IOException e)
							{
								Log.e(TAG, e.getMessage(), e);
							}
							catch (LuaException e)
							{
								Log.e(TAG, e.getMessage(), e);
							}
						}
					});
				}
				else
					throw new LuaException(errorReason(ok) + ": " + L.toString(-1));
			}
			catch (IOException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
			catch (LuaException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}

		}

		private String errorReason(int error)
		{
			switch (error)
			{
				case 4:
					return "Out of memory";
				case 3:
					return "Syntax error";
				case 2:
					return "Runtime error";
				case 1:
					return "Yield error";
			}
			return "Unknown error " + error;
		}
	}

	private static class ServerThread extends Thread
	{
		private ServerSocket _serverSocket;

		ServerThread(ServerSocket serverSocket)
		{
			super("LuaRPC server");
			_serverSocket = serverSocket;
		}

		@Override
		public void run()
		{
			while (true)
			{
				try
				{
					Socket clientSocket = _serverSocket.accept();
					new ClientThread(clientSocket).start();
				}
				catch (IOException e)
				{
					Log.e(TAG, e.getMessage(), e);
				}
			}
		}
	}
}

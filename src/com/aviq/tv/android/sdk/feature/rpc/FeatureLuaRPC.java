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
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.keplerproject.luajava.JavaFunction;
import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaState;
import org.keplerproject.luajava.LuaStateFactory;

import android.content.res.AssetManager;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureName;

/**
 * Lua based RPC scripting service
 */
public class FeatureLuaRPC extends FeatureRPC
{
	public static final String TAG = FeatureLuaRPC.class.getSimpleName();
	private static final int BUF_SIZE = 1024 * 100;
	private static final String EOL = "-- End of Lua";

	public enum Param
	{
		/**
		 * RPC host listen
		 */
		HOST("127.0.0.1"),

		/**
		 * RPC server port
		 */
		PORT(6768);

		Param(int value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.RPC).put(name(), value);
		}

		Param(String value)
		{
			Environment.getInstance().getFeaturePrefs(FeatureName.Component.RPC).put(name(), value);
		}
	}

	private int _port;
	private String _host;
	private ServerThread _serverThread;

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		try
		{
			_port = getPrefs().getInt(Param.PORT);
			_host = getPrefs().getString(Param.HOST);

			ServerSocket serverSocket;
			if (Environment.getInstance().isDevel())
				// listen to any host if under development environment
				serverSocket = new ServerSocket(_port);
			else
				// limit listening to parameterized host if under release environment
				serverSocket = new ServerSocket(_port, 0, InetAddress.getByName(_host));
			_serverThread = new ServerThread(this, serverSocket);
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

	/**
	 * Executes script as input stream synchronously. This method needs to be
	 * overwritten by specific RPC implementation
	 *
	 * @param inputStream
	 */
	@Override
	public void execute(InputStream inputStream)
	{
		try
		{
			Socket clientSocket = new Socket("localhost", _port);
			OutputStream outputStream = clientSocket.getOutputStream();
			byte[] buffer = new byte[4096];
			int n = 0;
			while (-1 != (n = inputStream.read(buffer)))
			{
				Log.i(TAG, new String(buffer));
				outputStream.write(buffer, 0, n);
			}
			inputStream.close();
			outputStream.close();
		}
		catch (UnknownHostException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IOException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
	}

	private static class ClientThread extends Thread
	{
		private Socket _clientSocket;
		private OutputStreamWriter _clientWriter;
		private FeatureLuaRPC _featureLuaRPC;

		ClientThread(FeatureLuaRPC featureLuaRPC, Socket clientSocket)
		{
			super("client: " + clientSocket);
			_featureLuaRPC = featureLuaRPC;
			_clientSocket = clientSocket;
		}

		private void writeToClient(String message)
		{
			if (_clientWriter != null)
			{
				try
				{
					_clientWriter.append(message);
					_clientWriter.flush();
				}
				catch (IOException e)
				{
					Log.w(TAG, e.getMessage());
					_clientWriter = null;
				}
			}
		}

		private static byte[] readAll(InputStream input) throws IOException
		{
			ByteArrayOutputStream output = new ByteArrayOutputStream(BUF_SIZE);
			byte[] buffer = new byte[BUF_SIZE];
			int n = 0;
			while (-1 != (n = input.read(buffer)))
			{
				output.write(buffer, 0, n);
				String line = new String(buffer, 0, n - 2);
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
							writeToClient(val);

							Log.i(TAG, val);
						}
						writeToClient("\t");
						writeToClient("\n");
						return 0;
					}
				};
				print.register("print");

				JavaFunction assetLoader = new JavaFunction(L)
				{
					@Override
					public int execute() throws LuaException
					{
						String name = L.toString(-1);

						AssetManager am = Environment.getInstance().getAssets();
						try
						{
							String scriptPath = "lua/" + name.replace('.', '/') + ".lua";
							Log.i(TAG, "Loading " + scriptPath);
							InputStream is = am.open(scriptPath);
							byte[] bytes = readAll(is);
							Log.i(TAG, new String(bytes));
							L.LloadBuffer(bytes, name);
							return 1;
						}
						catch (Exception e)
						{
							ByteArrayOutputStream os = new ByteArrayOutputStream();
							e.printStackTrace(new PrintStream(os));
							L.pushString("Cannot load module " + name + ":\n" + os.toString());
							return 1;
						}
					}
				};

				L.getGlobal("package");
				L.getField(-1, "loaders");
				int nLoaders = L.objLen(-1);

				L.pushJavaFunction(assetLoader);
				L.rawSetI(-2, nLoaders + 1);
				L.pop(1);

				InputStream clientInput = _clientSocket.getInputStream();
				OutputStream clientOutput = _clientSocket.getOutputStream();
				_clientWriter = new OutputStreamWriter(clientOutput);
				writeToClient("Lua output follows...\r\n");

				byte[] luaBuffer = readAll(clientInput);
				L.setTop(0);
				final int ok = L.LloadBuffer(luaBuffer, "lua");
				Log.i(TAG, "loaded ..." + new String(luaBuffer) + " -> " + ok);
				if (ok != 0)
					throw new LuaException(errorReason(ok) + ": " + L.toString(-1));

				Environment.getInstance().runOnUiThread(new Runnable()
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
						writeToClient("Lua output finished!\r\n");
						if (ok != 0)
						{
							Exception e = new LuaException(errorReason(ok) + ": " + L.toString(-1));
							Log.e(TAG, e.getMessage(), e);
						}
					}
				});
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
		private FeatureLuaRPC _featureLuaRPC;

		ServerThread(FeatureLuaRPC featureLuaRPC, ServerSocket serverSocket)
		{
			super("LuaRPC server");
			_featureLuaRPC = featureLuaRPC;
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
					new ClientThread(_featureLuaRPC, clientSocket).start();
				}
				catch (IOException e)
				{
					Log.e(TAG, e.getMessage(), e);
				}
			}
		}
	}
}

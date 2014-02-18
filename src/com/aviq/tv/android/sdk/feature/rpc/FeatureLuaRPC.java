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
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;

/**
 * Lua based RPC scripting service
 */
public class FeatureLuaRPC extends FeatureComponent
{
	public static final String TAG = FeatureLuaRPC.class.getSimpleName();
	public static final int RPC_SERVER_PORT = 6768;
	private static final String EOL = "-- End of Lua";
	private String _luaStub;
	private int _port;

	public enum Param
	{
		/**
		 * RPC server port
		 */
		PORT(RPC_SERVER_PORT);

		Param(int value)
		{
			if (Environment.getInstance().isInitialized())
				Environment.getInstance().getFeaturePrefs(FeatureName.Component.RPC).put(name(), value);
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
			_port = RPC_SERVER_PORT;
			if (Environment.getInstance().isInitialized())
				_port = getPrefs().getInt(Param.PORT);

			ServerSocket serverSocket = new ServerSocket(_port);
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

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.RPC;
	}

	public void setLuaStub(String luaStub)
	{
		_luaStub = luaStub;
	}

	public String getLuaStub()
	{
		return _luaStub;
	}

	public void execute(final InputStream inputStream)
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					Socket clientSocket = new Socket("localhost", _port);
					OutputStream outputStream = clientSocket.getOutputStream();
					byte[] buffer = new byte[4096];
					int n = 0;
					while (-1 != (n = inputStream.read(buffer)))
					{
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
		}).start();
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

		private static byte[] readAll(InputStream input) throws IOException
		{
			ByteArrayOutputStream output = new ByteArrayOutputStream(4096);
			byte[] buffer = new byte[4096];
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

				JavaFunction assetLoader = new JavaFunction(L)
				{
					@Override
					public int execute() throws LuaException
					{
						String name = L.toString(-1);

						AssetManager am = Environment.getInstance().getActivity().getAssets();
						try
						{
							String scriptPath = "lua/" + name.replace('.', '/') + ".lua";
							Log.i(TAG, "Loading " + scriptPath);
							InputStream is = am.open(scriptPath);
							byte[] bytes = readAll(is);
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
				_clientWriter.write("Lua output follows...\r\n");
				_clientWriter.flush();

				if (_featureLuaRPC.getLuaStub() != null)
				{
					int stubok = L.LloadBuffer(_featureLuaRPC.getLuaStub().getBytes(), "stub");
					if (stubok != 0)
						throw new LuaException(errorReason(stubok) + ": " + L.toString(-1));

					Environment.getInstance().getActivity().runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							try
							{
								Log.i(TAG, "starting stub...");
								L.getGlobal("debug");
								L.getField(-1, "traceback");
								L.remove(-2);
								L.insert(-2);
								int stubok = L.pcall(0, 0, -2);
								if (stubok != 0)
									throw new LuaException(errorReason(stubok) + ": " + L.toString(-1));
							}
							catch (LuaException e)
							{
								Log.e(TAG, e.getMessage(), e);
							}
						}
					});
				}

				byte[] luaBuffer = readAll(clientInput);
				L.setTop(0);
				final int ok = L.LloadBuffer(luaBuffer, "lua");
				Log.i(TAG, "loaded ..." + ok);
				if (ok != 0)
					throw new LuaException(errorReason(ok) + ": " + L.toString(-1));

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

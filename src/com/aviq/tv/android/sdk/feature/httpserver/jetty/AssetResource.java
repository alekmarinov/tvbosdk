/**
 * Copyright (c) 2007-2015, Intelibo Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    AssetResource.java
 * Author:      alek
 * Date:        7 Jul 2015
 * Description:
 */

package com.aviq.tv.android.sdk.feature.httpserver.jetty;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jetty.util.resource.Resource;

import android.content.Context;
import android.content.res.AssetFileDescriptor;

import com.aviq.tv.android.sdk.core.Log;

/**
 *
 */
public class AssetResource extends Resource
{
	public static final String TAG = AssetResource.class.getSimpleName();
	private AssetFileDescriptor _file;
	private String _fileName;
	private Context _context;

	public AssetResource(Context context, String fileName) throws IOException
	{
		_context = context;
		_fileName = fileName;
		_file = context.getAssets().openFd(fileName);
	}

	@Override
	public Resource addPath(String path) throws IOException, MalformedURLException
	{
		return new AssetResource(_context, _fileName + path);
	}

	@Override
	public boolean delete() throws SecurityException
	{
		return false;
	}

	@Override
	public boolean exists()
	{
		return true;
	}

	@Override
	public File getFile() throws IOException
	{
		return null;
	}

	@Override
	public InputStream getInputStream() throws IOException
	{
		return _file.createInputStream();
	}

	@Override
	public String getName()
	{
		return _fileName;
	}

	@Override
	public OutputStream getOutputStream() throws IOException, SecurityException
	{
		return null;
	}

	@Override
	public URL getURL()
	{
		return null;
	}

	@Override
	public boolean isContainedIn(Resource arg0) throws MalformedURLException
	{
		return false;
	}

	@Override
	public boolean isDirectory()
	{
		return false;
	}

	@Override
	public long lastModified()
	{
		return 0;
	}

	@Override
	public long length()
	{
		return 0;
	}

	@Override
	public String[] list()
	{
		return null;
	}

	@Override
	public void release()
	{
		try
        {
	        _file.close();
        }
        catch (IOException e)
        {
        	Log.e(TAG, e.getMessage(), e);
        }
	}

	@Override
	public boolean renameTo(Resource arg0) throws SecurityException
	{
		return false;
	}
}

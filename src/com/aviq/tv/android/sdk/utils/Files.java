/**
 * Copyright (c) 2003-2012, AVIQ Systems AG
 *
 * Project:     javiq
 * Filename:    FileName.java
 * Author:      alek
 * Description: Text utilities with file names
 */

package com.aviq.tv.android.sdk.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;

import com.aviq.tv.android.sdk.core.Log;

public class Files
{
	/**
	 * Return file name with path stripped
	 */
	public static String baseName(String fileName)
	{
		int sep = fileName.lastIndexOf('/');
		if (sep >= 0)
			return fileName.substring(sep + 1);
		return fileName;
	}

	/**
	 * Return file name extension
	 */
	public static String ext(String fileName)
	{
		int dot = fileName.lastIndexOf('.');
		if (dot >= 0)
			return fileName.substring(dot);
		return "";
	}

	/**
	 * Load file content to String
	 */
	public static String loadToString(String filePath) throws java.io.IOException
	{
		StringBuffer fileData = new StringBuffer(1000);
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		char[] buf = new char[1024];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1)
		{
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
		}
		reader.close();
		return fileData.toString();
	}

	public static void closeQuietly(OutputStream os, String TAG)
	{
		if (os != null)
		{
			try
			{
				os.close();
			}
			catch (IOException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
		}
	}

	public static void closeQuietly(InputStream is, String TAG)
	{
		if (is != null)
		{
			try
			{
				is.close();
			}
			catch (IOException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
		}
	}

	public static void closeQuietly(Reader reader, String TAG)
	{
		if (reader != null)
		{
			try
			{
				reader.close();
			}
			catch (IOException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}
		}
	}

	/**
	 * Deletes the directory including contetn
	 *
	 * @param dir
	 * @author Lukas
	 */
	public static boolean deleteDirectory(File path)
	{

		if (path.exists())
		{
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++)
			{
				if (files[i].isDirectory())
				{
					deleteDirectory(files[i]);
				}
				else
				{
					files[i].delete();
				}
			}
		}
		return (path.delete());

	}
}
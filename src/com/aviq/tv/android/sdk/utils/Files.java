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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.zip.GZIPOutputStream;

import android.content.Context;

import com.aviq.tv.android.sdk.core.Log;

public class Files
{
	public static final String TAG = Files.class.getSimpleName();

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
	 * Return absolute file path. If the fileName is given relative (without
	 * leading '/') then it is prefixed with the path to app files directory
	 */
	public static String normalizeName(Context context, String fileName)
	{
		if (!fileName.startsWith("/"))
		{
			// prefix with app files dir since the file name is given relative
			if (!fileName.isEmpty())
				fileName = '/' + fileName;
			fileName = context.getFilesDir().getAbsolutePath() + fileName;
		}
		return fileName;
	}

	/**
	 * Return file directory
	 */
	public static String dirName(String fileName)
	{
		int sep = fileName.lastIndexOf('/');
		if (sep >= 0)
			return fileName.substring(0, sep);
		return "";
	}

	/**
	 * Return file directory related to application files directory if the file
	 * name is give relative
	 */
	public static String dirName(Context context, String fileName)
	{
		return dirName(normalizeName(context, fileName));
	}

	/**
	 * Return absolute path to file related to application files directory if
	 * the file name is give relative
	 */
	public static String filePath(Context context, String fileName)
	{
		String dirName = dirName(fileName);
		if (!dirName.startsWith("/"))
		{
			// prefix with app files dir since the file name is given relative
			if (!dirName.isEmpty())
				dirName = '/' + dirName;
			dirName = context.getFilesDir().getAbsolutePath() + dirName;
		}
		if (!dirName.endsWith("/"))
			dirName = dirName + "/";
		return dirName + baseName(fileName);
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

	/**
	 * Save string to file
	 */
	public static void saveToFile(String text, String filePath) throws java.io.IOException
	{
		FileWriter writer = new FileWriter(filePath);
		writer.write(text);
		writer.close();
	}

	/**
	 * Closes output stream with shutting up the IO exceptions
	 *
	 * @param os
	 * @param TAG
	 */
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

	/**
	 * Closes output stream with shutting up the IO exceptions
	 *
	 * @param is
	 * @param TAG
	 */
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

	/**
	 * Closes reader with shutting up the IO exceptions
	 *
	 * @param reader
	 * @param TAG
	 */
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

	/**
	 * GZip file
	 *
	 * @param context an application context
	 * @param srcFileName the file to be zipped
	 * @param destFileName the destination zip file
	 * @return
	 */
	protected boolean gzip(Context context, String srcFileName, String destFileName)
	{
		FileInputStream in = null;
		GZIPOutputStream gzipOut = null;
		try
		{
			in = new FileInputStream(srcFileName);
			gzipOut = new GZIPOutputStream(context.openFileOutput(destFileName, Context.MODE_PRIVATE));

			byte[] buffer = new byte[8192];

			int nread = in.read(buffer);
			while (nread > 0)
			{
				gzipOut.write(buffer, 0, nread);
				if (nread != buffer.length)
					break;
				nread = in.read(buffer);
			}
		}
		catch (FileNotFoundException e)
		{
			Log.e(TAG, "Source file not found", e);
			return false;
		}
		catch (IOException e)
		{
			Log.e(TAG, "IO error", e);
			return false;
		}
		finally
		{
			Files.closeQuietly(in, TAG);
			Files.closeQuietly(gzipOut, TAG);
		}
		return true;
	}
}

/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    SystemProperties.java
 * Author:      alek
 * Date:        14 Apr 2014
 * Description: Provides access to android.os.SystemProperties with reflection
 */

package com.aviq.tv.android.sdk.feature.system;

import java.lang.reflect.Method;

import com.aviq.tv.android.sdk.core.Log;

public class SystemProperties
{
	private static final String TAG = SystemProperties.class.getSimpleName();

	/**
	 * This class cannot be instantiated
	 */
	private SystemProperties()
	{

	}

	/**
	 * Get the value for the given key.
	 *
	 * @return an empty string if the key isn't found
	 * @throws IllegalArgumentException
	 *             if the key exceeds 32 characters
	 */
	public static String get(String key) throws IllegalArgumentException
	{
		String ret = "";

		try
		{
			Class<?> SystemProperties = Class.forName("android.os.SystemProperties");
			Class<?>[] paramTypes = new Class<?>[1];
			paramTypes[0] = String.class;

			Method get = SystemProperties.getMethod("get", paramTypes);

			// Parameters
			Object[] params = new Object[1];
			params[0] = new String(key);

			ret = (String) get.invoke(SystemProperties, params);
		}
		catch (IllegalArgumentException iAE)
		{
			throw iAE;
		}
		catch (Exception e)
		{
			Log.e(TAG, e.getMessage(), e);
		}

		return ret;
	}

	/**
	 * Get the value for the given key, and return as an integer.
	 *
	 * @param key
	 *            the key to lookup
	 * @param def
	 *            a default value to return
	 * @return the key parsed as an integer, or def if the key isn't found or
	 *         cannot be parsed
	 * @throws IllegalArgumentException
	 *             if the key exceeds 32 characters
	 */
	public static Integer getInt(String key, int def) throws IllegalArgumentException
	{
		Integer ret = def;

		try
		{
			Class<?> SystemProperties = Class.forName("android.os.SystemProperties");

			// Parameters Types
			Class<?>[] paramTypes = new Class<?>[2];
			paramTypes[0] = String.class;
			paramTypes[1] = int.class;

			Method getInt = SystemProperties.getMethod("getInt", paramTypes);

			// Parameters
			Object[] params = new Object[2];
			params[0] = new String(key);
			params[1] = Integer.valueOf(def);

			ret = (Integer) getInt.invoke(SystemProperties, params);
		}
		catch (IllegalArgumentException iAE)
		{
			throw iAE;
		}
		catch (Exception e)
		{
			Log.e(TAG, e.getMessage(), e);
		}

		return ret;
	}

	/**
	 * Get the value for the given key, and return as a long.
	 *
	 * @param key
	 *            the key to lookup
	 * @param def
	 *            a default value to return
	 * @return the key parsed as a long, or def if the key isn't found or
	 *         cannot be parsed
	 * @throws IllegalArgumentException
	 *             if the key exceeds 32 characters
	 */
	public static Long getLong(String key, long def) throws IllegalArgumentException
	{

		Long ret = def;

		try
		{
			Class<?> SystemProperties = Class.forName("android.os.SystemProperties");

			// Parameters Types
			Class<?>[] paramTypes = new Class<?>[2];
			paramTypes[0] = String.class;
			paramTypes[1] = long.class;

			Method getLong = SystemProperties.getMethod("getLong", paramTypes);

			// Parameters
			Object[] params = new Object[2];
			params[0] = new String(key);
			params[1] = Long.valueOf(def);

			ret = (Long) getLong.invoke(SystemProperties, params);

		}
		catch (IllegalArgumentException iAE)
		{
			throw iAE;
		}
		catch (Exception e)
		{
			Log.e(TAG, e.getMessage(), e);
		}

		return ret;

	}

	/**
	 * Get the value for the given key, returned as a boolean.
	 * Values 'n', 'no', '0', 'false' or 'off' are considered false.
	 * Values 'y', 'yes', '1', 'true' or 'on' are considered true.
	 * (case insensitive).
	 * If the key does not exist, or has any other value, then the default
	 * result is returned.
	 *
	 * @param key
	 *            the key to lookup
	 * @param def
	 *            a default value to return
	 * @return the key parsed as a boolean, or def if the key isn't found or is
	 *         not able to be parsed as a boolean.
	 * @throws IllegalArgumentException
	 *             if the key exceeds 32 characters
	 */
	public static Boolean getBoolean(String key, boolean def) throws IllegalArgumentException
	{
		Boolean ret = def;

		try
		{
			Class<?> SystemProperties = Class.forName("android.os.SystemProperties");

			// Parameters Types
			Class<?>[] paramTypes = new Class<?>[2];
			paramTypes[0] = String.class;
			paramTypes[1] = boolean.class;

			Method getBoolean = SystemProperties.getMethod("getBoolean", paramTypes);

			// Parameters
			Object[] params = new Object[2];
			params[0] = new String(key);
			params[1] = Boolean.valueOf(def);

			ret = (Boolean) getBoolean.invoke(SystemProperties, params);
		}
		catch (IllegalArgumentException iAE)
		{
			throw iAE;
		}
		catch (Exception e)
		{
			Log.e(TAG, e.getMessage(), e);
		}

		return ret;
	}
}

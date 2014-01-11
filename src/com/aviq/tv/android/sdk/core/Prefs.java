/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    Prefs.java
 * Author:      alek
 * Date:        16 Jul 2013
 * Description: Android SharedPreferences wrapper
 */

package com.aviq.tv.android.sdk.core;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;

import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Android SharedPreferences wrapper
 */
public class Prefs
{
	private static final String TAG = Prefs.class.getSimpleName();
	private final SharedPreferences _prefs;
	private boolean _isOverwrite;

	/**
	 * Constructor
	 *
	 * @param prefs
	 *            is initialized Android SharedPreferences
	 * @param isOverwrite
	 *            set to true if the 2nd put method should overwrite the
	 *            previous put
	 */
	public Prefs(SharedPreferences prefs, boolean isOverwrite)
	{
		_prefs = prefs;
		_isOverwrite = isOverwrite;
	}

	/**
	 * Checks if key exists in the preferences
	 *
	 * @param key
	 *            parameter name
	 * @return true if the key is set in preferences
	 */
	public boolean has(Object key)
	{
		return _prefs.contains(key.toString());
	}

	/**
	 * Returns string parameter from preferences
	 *
	 * @param key
	 *            parameter name
	 * @return String value corresponding to the specified key
	 */
	public String getString(Object key)
	{
		if (!_prefs.contains(key.toString()))
			throw new RuntimeException("Parameter " + key + " is not set");
		return _prefs.getString(key.toString(), null);
	}

	/**
	 * Returns string parameter from preferences with applied substitutions
	 * provided by the <i>bundle</i>.
	 *
	 * @param key
	 *            parameter name
	 * @return String value corresponding to the specified key with applied
	 *         substitutions
	 */
	public String getString(Object key, Bundle bundle)
	{
		return TextUtils.substitute(getString(key), bundle);
	}

	/**
	 * Returns int parameter from preferences
	 *
	 * @param key
	 *            parameter name
	 * @return Int value corresponding to the specified key
	 */
	public int getInt(Object key)
	{
		if (!_prefs.contains(key.toString()))
			throw new RuntimeException("Parameter " + key + " is not set");
		return _prefs.getInt(key.toString(), 0);
	}

	/**
	 * Returns boolean parameter from preferences
	 *
	 * @param key
	 *            parameter name
	 * @return Boolean value corresponding to the specified key
	 */
	public boolean getBool(Object key)
	{
		if (!_prefs.contains(key.toString()))
			throw new RuntimeException("Parameter " + key + " is not set");
		return _prefs.getBoolean(key.toString(), false);
	}

	/**
	 * Sets string parameter in preferences
	 *
	 * @param key
	 *            parameter name
	 * @param String
	 *            value
	 */
	public void put(Object key, String value)
	{
		if (_isOverwrite || !_prefs.contains(key.toString()))
		{
			Log.d(TAG, "Set " + key + " = " + value);
			Editor edit = _prefs.edit();
			edit.putString(key.toString(), value);
			edit.commit();
		}
		else
		{
			Log.v(TAG, "Skip setting " + key + " = " + value + ", previous value = " + getString(key));
		}
	}

	/**
	 * Sets int parameter in preferences
	 *
	 * @param key
	 *            parameter name
	 * @param int
	 *        value
	 */
	public void put(Object key, int value)
	{
		if (_isOverwrite || !_prefs.contains(key.toString()))
		{
			Log.d(TAG, "Set " + key + " = " + value);
			Editor edit = _prefs.edit();
			edit.putInt(key.toString(), value);
			edit.commit();
		}
		else
		{
			Log.v(TAG, "Skip setting " + key + " = " + value + ", previous value = " + getInt(key));
		}
	}

	/**
	 * Sets boolean parameter in preferences
	 *
	 * @param key
	 *            parameter name
	 * @param boolean
	 *        value
	 */
	public void put(Object key, boolean value)
	{
		if (_isOverwrite || !_prefs.contains(key.toString()))
		{
			Log.d(TAG, "Set " + key + " = " + value);
			Editor edit = _prefs.edit();
			edit.putBoolean(key.toString(), value);
			edit.commit();
		}
		else
		{
			Log.v(TAG, "Skip setting " + key + " = " + value + ", previous value = " + getBool(key));
		}
	}

	/**
	 * Clean all preferences
	 */
	public void clear()
	{
		Editor edit = _prefs.edit();
		edit.clear();
		edit.commit();
	}
}

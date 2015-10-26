/**
 * Copyright (c) 2007-2015, Intelibo Ltd
 *
 * Project:     tvbosdk
 * Filename:    CommandGetPrefs.java
 * Author:      alek
 * Date:        30.06.2015 ã.
 * Description: Command returning application preferences
 */

package com.aviq.tv.android.sdk.feature.command.handlers;

import java.util.Map.Entry;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.IFeature;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.command.CommandHandler;

/**
 * Command returning application preferences
 */
public class CommandGetPrefs implements CommandHandler
{
	private static final String TAG = CommandGetPrefs.class.getSimpleName();
	public static final String ID = "GET_PREFS";

	public static enum Extras
	{
		TYPE
	}

	public static enum PrefType
	{
		USER, SYSTEM
	}

	@Override
	public void execute(Bundle params, final OnResultReceived onResultReceived)
	{
		FeatureComponent thisFeature = Environment.getInstance().getFeatureComponent(FeatureName.Component.COMMAND);
		String type = params.getString(Extras.TYPE.name());
		if (type == null)
		{
			onResultReceived.onReceiveResult(new FeatureError(thisFeature, ResultCode.PROTOCOL_ERROR,
			        "Expected parameter type"), null);
			return;
		}
		type = type.toUpperCase();
		Log.i(TAG, ".execute: type = " + type);
		JSONObject json = new JSONObject();
		Prefs prefs = null;

		if (PrefType.USER.name().equals(type.toUpperCase()))
		{
			prefs = Environment.getInstance().getUserPrefs();
		}
		else if (PrefType.SYSTEM.name().equals(type.toUpperCase()))
		{
			prefs = Environment.getInstance().getPrefs();
		}
		else
		{
			IFeature feature = null;
			String[] nameParts = type.split("\\.");
			if (nameParts.length != 2)
			{
				onResultReceived.onReceiveResult(new FeatureError(thisFeature, ResultCode.PROTOCOL_ERROR,
				        "Invalid feature name format. Expected [component|scheduler|state].name, got " + type), null);
				return;
			}
			IFeature.Type featureType = IFeature.Type.valueOf(nameParts[0]);
			if (featureType == null)
			{
				onResultReceived.onReceiveResult(new FeatureError(thisFeature, ResultCode.PROTOCOL_ERROR,
				        "Invalid feature type. Expected component|scheduler|state, got " + type), null);
				return;
			}
			try
			{
				switch (featureType)
				{
					case COMPONENT:
						FeatureName.Component componentName = FeatureName.Component.valueOf(nameParts[1]);
						feature = Environment.getInstance().getFeatureComponent(componentName);
					break;
					case SCHEDULER:
						FeatureName.Scheduler schedulerName = FeatureName.Scheduler.valueOf(nameParts[1]);
						feature = Environment.getInstance().getFeatureScheduler(schedulerName);
					break;
					case STATE:
						FeatureName.State stateName = FeatureName.State.valueOf(nameParts[1]);
						feature = Environment.getInstance().getFeatureState(stateName);
					break;
				}
			}
			catch (Exception e)
			{
				Log.e(TAG, e.getMessage());
			}
			if (feature == null)
			{
				onResultReceived.onReceiveResult(new FeatureError(thisFeature, ResultCode.FEATURE_NOT_FOUND,
				        "Unknown feature " + featureType + " with name " + type), null);
				return;
			}

			prefs = feature.getPrefs();
		}

		try
		{
			for (Entry<String, ?> entry : prefs.getAll().entrySet())
			{
				Object v = entry.getValue();
				String key = entry.getKey();
				if (v instanceof Boolean)
					json.put(key, ((Boolean) v).booleanValue());
				else if (v instanceof Float)
					json.put(key, ((Float) v).floatValue());
				else if (v instanceof Integer)
					json.put(key, ((Integer) v).intValue());
				else if (v instanceof Long)
					json.put(key, ((Long) v).longValue());
				else if (v instanceof String)
					json.put(key, v);
			}
		}
		catch (JSONException e)
		{
			onResultReceived.onReceiveResult(new FeatureError(thisFeature, e), null);
			return;
		}

		onResultReceived.onReceiveResult(FeatureError.OK, json);
	}

	@Override
	public String getId()
	{
		return ID;
	}
}

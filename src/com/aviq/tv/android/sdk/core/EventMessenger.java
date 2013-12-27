/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    EventMessanger.java
 * Author:      alek
 * Date:        14 Dec 2013
 * Description: Manage event messages
 */

package com.aviq.tv.android.sdk.core;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;

import com.aviq.tv.android.sdk.core.feature.IFeature;

/**
 * Manage event messages
 */
public class EventMessenger extends Handler
{
	public static final String TAG = EventMessenger.class.getSimpleName();
	private static int ID_GENERATOR = 0;
	private SparseArray<List<IFeature>> _listners = new SparseArray<List<IFeature>>();

	public static synchronized int ID()
	{
		return ++ID_GENERATOR;
	}

	/**
	 * Register feature to listen for messages with id msgId
	 *
	 * @param feature
	 *            the feature to be registered
	 * @param msgId
	 */
	public void register(IFeature feature, int msgId)
	{
		Log.i(TAG, ".register " + feature.getName() + " " + feature.getType() + " on " + msgId);
		List<IFeature> msgListeners = _listners.get(msgId);
		if (msgListeners == null)
		{
			msgListeners = new ArrayList<IFeature>();
			_listners.put(msgId, msgListeners);
		}
		msgListeners.add(feature);
	}

	/**
	 * Unregisters feature from listening to message msgId
	 *
	 * @param feature
	 *            the feature to be unregistered
	 * @param msgId
	 */
	public void unregister(IFeature feature, int msgId)
	{
		Log.i(TAG, ".unregister " + feature.getName() + " " + feature.getType() + " from " + msgId);
		List<IFeature> msgListeners = _listners.get(msgId);
		if (msgListeners != null)
			msgListeners.remove(feature);
	}

	/**
	 * Triggers event message to listeners for this feature
	 *
	 * @param msgId
	 *            the id of the message to trigger
	 */
	public void trigger(int msgId)
	{
		sendMessage(obtainMessage(msgId));
	}

	/**
	 * Triggers event message to listeners for this feature
	 *
	 * @param msgId
	 *            the id of the message to trigger
	 * @param Bundle
	 *            additional data to the triggered message
	 */
	public void trigger(int msgId, Bundle bundle)
	{
		sendMessage(obtainMessage(msgId, bundle));
	}

	/**
	 * Triggers event message to listeners for this feature
	 *
	 * @param msgId
	 *            the id of the message to trigger
	 * @param delayMs
	 *            delay in milliseconds before triggering the message
	 */
	public void trigger(int msgId, long delayMs)
	{
		sendMessageDelayed(obtainMessage(msgId), delayMs);
	}

	/**
	 * Triggers event message to listeners for this feature
	 *
	 * @param msgId
	 *            the id of the message to trigger
	 * @param Bundle
	 *            additional data to the triggered message
	 * @param delayMs
	 *            delay in milliseconds before triggering the message
	 */
	public void trigger(int msgId, Bundle bundle, long delayMs)
	{
		sendMessageDelayed(obtainMessage(msgId, bundle), delayMs);
	}

	@Override
	public void handleMessage(Message msg)
	{
		super.handleMessage(msg);
		List<IFeature> msgListeners = _listners.get(msg.what);
		if (msgListeners != null)
			for (IFeature feature : msgListeners)
				feature.onEvent(msg.what, (Bundle) msg.obj);
	}
}

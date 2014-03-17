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

import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Manage event messages
 */
public class EventMessenger extends Handler
{
	public static final String TAG = EventMessenger.class.getSimpleName();
	private static int ID_GENERATOR = 0;
	private SparseArray<List<EventReceiver>> _listeners = new SparseArray<List<EventReceiver>>();
	private List<RegisterCouple> _registerLater = new ArrayList<RegisterCouple>();
	private List<RegisterCouple> _unregisterLater = new ArrayList<RegisterCouple>();
	private boolean _inEventIteration = false;
	private static int _lastId = 0;

	public static synchronized int ID()
	{
		_lastId = ++ID_GENERATOR;
		return _lastId;
	}

	/**
	 * Register EventReceiver to listen for messages with id msgId
	 *
	 * @param EventReceiver
	 *            to be registered
	 * @param msgId
	 */
	public void register(EventReceiver eventReceiver, int msgId)
	{
		if (_inEventIteration)
		{
			_registerLater.add(new RegisterCouple(eventReceiver, msgId));
		}
		else
		{
			Log.d(TAG, ".register " + eventReceiver + " on " + msgId);
			List<EventReceiver> msgListeners = _listeners.get(msgId);
			if (msgListeners == null)
			{
				msgListeners = new ArrayList<EventReceiver>();
				_listeners.put(msgId, msgListeners);
			}
			msgListeners.add(eventReceiver);
		}
	}

	/**
	 * Unregisters EventReceiver from listening to message msgId
	 *
	 * @param EventReceiver
	 *            to be unregistered
	 * @param msgId
	 */
	public void unregister(EventReceiver eventReceiver, int msgId)
	{
		if (_inEventIteration)
		{
			_unregisterLater.add(new RegisterCouple(eventReceiver, msgId));
		}
		else
		{
			Log.d(TAG, ".unregister " + eventReceiver + " from " + ((msgId > 0) ? msgId : "all events"));
			int msgIdFirst = 1;
			int msgIdLast = _lastId;
			if (msgId > 0)
			{
				msgIdFirst = msgId;
				msgIdLast = msgId;
			}

			for (int id = msgIdFirst; id <= msgIdLast; id++)
			{
				List<EventReceiver> msgListeners = _listeners.get(id);
				if (msgListeners != null)
					msgListeners.remove(eventReceiver);
			}
		}
	}

	/**
	 * Triggers event message to listeners for this EventReceiver
	 *
	 * @param msgId
	 *            the id of the message to trigger
	 */
	public void trigger(int msgId)
	{
		Log.v(TAG, ".trigger: " + msgId);
		removeMessages(msgId);
		sendMessage(obtainMessage(msgId));
	}

	/**
	 * Triggers event message to listeners for this EventReceiver
	 *
	 * @param msgId
	 *            the id of the message to trigger
	 * @param Bundle
	 *            additional data to the triggered message
	 */
	public void trigger(int msgId, Bundle bundle)
	{
		Log.v(TAG, ".trigger: " + msgId + TextUtils.implodeBundle(bundle));
		removeMessages(msgId);
		sendMessage(obtainMessage(msgId, bundle));
	}

	/**
	 * Triggers event message to listeners for this EventReceiver
	 *
	 * @param msgId
	 *            the id of the message to trigger
	 * @param delayMs
	 *            delay in milliseconds before triggering the message
	 */
	public void trigger(int msgId, long delayMs)
	{
		Log.v(TAG, ".trigger: " + msgId + " in " + delayMs + " ms");
		removeMessages(msgId);
		sendMessageDelayed(obtainMessage(msgId), delayMs);
	}

	/**
	 * Triggers event message to listeners for this EventReceiver
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
		Log.v(TAG, ".trigger: " + msgId + TextUtils.implodeBundle(bundle) + " in " + delayMs + " ms");
		removeMessages(msgId);
		sendMessageDelayed(obtainMessage(msgId, bundle), delayMs);
	}

	@Override
	public void handleMessage(Message msg)
	{
		super.handleMessage(msg);
		_inEventIteration = true;
		List<EventReceiver> msgListeners = _listeners.get(msg.what);
		if (msgListeners != null)
		{
			Log.v(TAG, ".handleMessage: notifying " + msgListeners.size() + " listeners on " + msg.what);
			for (EventReceiver eventReceiver : msgListeners)
				eventReceiver.onEvent(msg.what, (Bundle) msg.obj);
		}
		_inEventIteration = false;
		for (RegisterCouple registerCouple : _registerLater)
		{
			register(registerCouple.Receiver, registerCouple.MsgId);
		}
		for (RegisterCouple registerCouple : _unregisterLater)
		{
			unregister(registerCouple.Receiver, registerCouple.MsgId);
		}
		_registerLater.clear();
		_unregisterLater.clear();
	}

	private class RegisterCouple
	{
		EventReceiver Receiver;
		int MsgId;

		RegisterCouple(EventReceiver receiver, int msgId)
		{
			Receiver = receiver;
			MsgId = msgId;
		}
	}
}

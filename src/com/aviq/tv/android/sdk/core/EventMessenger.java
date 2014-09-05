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
	/**
	 * Message ID identifying any message id
	 */
	public static final int ON_ANY = 0;

	private SparseArray<List<EventReceiver>> _listners = new SparseArray<List<EventReceiver>>();
	private List<RegisterCouple> _registerLater = new ArrayList<RegisterCouple>();
	private List<RegisterCouple> _unregisterLater = new ArrayList<RegisterCouple>();
	private boolean _inEventIteration = false;
	private static List<String> _messageNames = new ArrayList<String>();
	private static SparseArray<List<TriggerRoute>> _eventHooks = new SparseArray<List<TriggerRoute>>();
	private String _tag;

	public EventMessenger(String tag)
	{
		_tag = tag;
	}

	public static synchronized int ID(String msgName)
	{
		if (nameId(msgName) > 0)
			throw new RuntimeException("Message name " + msgName + " is already registered");
		_messageNames.add(msgName);
		return _messageNames.size();
	}

	public static synchronized String idName(int msgId)
	{
		if (msgId > 0)
			return _messageNames.get(msgId - 1);
		return "ANY";
	}

	public static synchronized int nameId(String msg)
	{
		int index = _messageNames.indexOf(msg);
		return index + 1;
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
			Log.d(_tag, ".register " + eventReceiver + " on " + idName(msgId) + " (" + msgId + ")");
			List<EventReceiver> msgListeners = _listners.get(msgId);
			if (msgListeners == null)
			{
				msgListeners = new ArrayList<EventReceiver>();
				_listners.put(msgId, msgListeners);
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
			Log.d(_tag, ".unregister " + eventReceiver + " from " + idName(msgId) + " (" + msgId + ")");
			int msgIdFirst = 1;
			int msgIdLast = _messageNames.size();
			if (msgId > 0)
			{
				msgIdFirst = msgId;
				msgIdLast = msgId;
			}

			for (int id = msgIdFirst; id <= msgIdLast; id++)
			{
				List<EventReceiver> msgListeners = _listners.get(id);
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
		Log.v(_tag, ".trigger: " + idName(msgId) + " (" + msgId + ")");
		removeMessages(msgId);
		sendMessage(obtainMessage(msgId));
	}

	/**
	 * Triggers event message to listeners for this EventReceiver directly
	 * bypassing events looper
	 *
	 * @param msgId
	 *            the id of the message to trigger
	 */
	public void triggerDirect(int msgId)
	{
		Log.v(_tag, ".triggerDirect: " + idName(msgId) + " (" + msgId + ")");
		handleMessage(obtainMessage(msgId));
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
		Log.v(_tag, ".trigger: " + idName(msgId) + " (" + msgId + ")" + TextUtils.implodeBundle(bundle));
		removeMessages(msgId);
		sendMessage(obtainMessage(msgId, bundle));
	}

	/**
	 * Triggers event message to listeners for this EventReceiver directly
	 * bypassing events looper
	 *
	 * @param msgId
	 *            the id of the message to trigger
	 * @param Bundle
	 *            additional data to the triggered message
	 */
	public void triggerDirect(int msgId, Bundle bundle)
	{
		Log.v(_tag, ".triggerDirect: " + idName(msgId) + " (" + msgId + ")" + TextUtils.implodeBundle(bundle));
		handleMessage(obtainMessage(msgId, bundle));
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
		Log.v(_tag, ".trigger: " + idName(msgId) + " (" + msgId + ")" + " in " + delayMs + " ms");
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
		Log.v(_tag, ".trigger: " + idName(msgId) + " (" + msgId + ")" + TextUtils.implodeBundle(bundle) + " in "
		        + delayMs + " ms");
		removeMessages(msgId);
		sendMessageDelayed(obtainMessage(msgId, bundle), delayMs);
	}

	@Override
	public void handleMessage(Message msg)
	{
		super.handleMessage(msg);
		_inEventIteration = true;
		List<EventReceiver> msgListeners = _listners.get(msg.what);
		if (msgListeners != null)
		{
			Log.v(_tag, ".handleMessage: notifying " + msgListeners.size() + " listeners on " + idName(msg.what) + " ("
			        + msg.what + ")");
			for (EventReceiver eventReceiver : msgListeners)
			{
				Log.d(_tag, eventReceiver + ".onEvent " + idName(msg.what));
				eventReceiver.onEvent(msg.what, (Bundle) msg.obj);
			}
		}

		// handle event hooks
		List<TriggerRoute> triggerRoutes = _eventHooks.get(msg.what);
		if (triggerRoutes != null)
		{
			Bundle eventParams = (Bundle) msg.obj;
			for (TriggerRoute triggerRoute : triggerRoutes)
			{
				// copy routed bundle and apply value substitution from source
				// event bundle
				Bundle bundle = new Bundle();
				Bundle routedEventParams = triggerRoute.getParams();
				if (routedEventParams != null)
				{
					for (String key : routedEventParams.keySet())
					{
						String value = routedEventParams.getString(key);
						if (value.charAt(0) == '{' && value.charAt(value.length() - 1) == '}')
						{
							// fetch value from event params
							String eventParamKey = value.substring(1, value.length() - 1);
							Object substVal = eventParams.get(eventParamKey);
							if (substVal != null)
							{
								// substitute value to the new bundle
								TextUtils.putBundleObject(bundle, key, substVal);
							}
							else
							{
								Log.w(_tag, "Event key `" + eventParamKey + "' has null value!");
							}
						}
						else
						{
							bundle.putString(key, value);
						}
					}
				}
				Log.d(_tag,
				        "Redirect event " + idName(msg.what) + TextUtils.implodeBundle(eventParams) + " to "
				                + triggerRoute.getTarget().getName() + ":" + idName(triggerRoute.getEventId())
				                + TextUtils.implodeBundle(bundle));
				triggerRoute.getTarget().getEventMessenger().trigger(triggerRoute.getEventId(), bundle);
			}
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

	public void addEventHook(int eventId, TriggerRoute action)
	{
		List<TriggerRoute> actions = _eventHooks.get(eventId);
		if (actions == null)
		{
			actions = new ArrayList<TriggerRoute>();
			_eventHooks.put(eventId, actions);
		}
		actions.add(action);
	}

	private static class RegisterCouple
	{
		EventReceiver Receiver;
		int MsgId;

		RegisterCouple(EventReceiver receiver, int msgId)
		{
			Receiver = receiver;
			MsgId = msgId;
		}
	}

	public static class RegisterCollector
	{
		public static final String TAG = RegisterCollector.class.getSimpleName();

		private List<RegisterTouple> _registrations = new ArrayList<RegisterTouple>();

		/**
		 * Register EventReceiver to listen for messages on arbitrary
		 * EventMessenger with id msgId
		 *
		 * @param EventReceiver
		 *            to be registered
		 * @param EventMessenger
		 *            the target EventMessenger registering to
		 * @param msgId
		 */
		public void register(EventReceiver receiver, EventMessenger eventMessenger, int msgId)
		{
			_registrations.add(new RegisterTouple(receiver, eventMessenger, msgId));
			eventMessenger.register(receiver, msgId);
		}

		/**
		 * Cleanup all registrations collected by this RegisterCollector
		 */
		public void cleanupRegistrations()
		{
			for (RegisterTouple registerTouple : _registrations)
			{
				registerTouple.EventMessenger.unregister(registerTouple.Receiver, registerTouple.MsgId);
			}
			Log.i(TAG, ".cleanupRegistrations: " + _registrations.size() + " cleaned up total");
			_registrations.clear();
		}

		private class RegisterTouple extends RegisterCouple
		{
			EventMessenger EventMessenger;

			RegisterTouple(EventReceiver receiver, EventMessenger eventMessenger, int msgId)
			{
				super(receiver, msgId);
				EventMessenger = eventMessenger;
			}
		}
	}
}

/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureState.java
 * Author:      alek
 * Date:        1 Dec 2013
 * Description: Defines the base class for state feature type
 */

package com.aviq.tv.android.sdk.core.feature;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import com.aviq.tv.android.sdk.core.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.state.BaseState;
import com.aviq.tv.android.sdk.core.state.StateException;
import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Defines the base class for state feature type
 */
public abstract class FeatureState extends BaseState implements IFeature
{
	public static final String TAG = FeatureState.class.getSimpleName();
	public static final int ON_SHOW = EventMessenger.ID("ON_SHOW");
	public static final String EXTRA_ON_SHOW_UNCOVER = "ON_SHOW_UNCOVER";
	public static final int ON_HIDE = EventMessenger.ID("ON_HIDE");
	public static final String EXTRA_ON_HIDE_COVER = "ON_HIDE_COVER";
	protected Features _feature;
	private FeatureSet _dependencies = new FeatureSet();
	private List<Subscription> _subscriptions = new ArrayList<Subscription>();
	private EventMessenger _eventMessenger = new EventMessenger(getClass().getSimpleName());

	public FeatureState()
	{
	}

	/**
	 * Declare this feature depends on the specified component name
	 * @param featureName
	 * @throws FeatureNotFoundException
	 */
    protected final void require(FeatureName.Component featureName) throws FeatureNotFoundException
	{
		_dependencies.Components.add(featureName);
	}

	/**
	 * Declare this feature depends on the specified scheduler name
	 * @param featureName
	 * @throws FeatureNotFoundException
	 */
	protected final void require(FeatureName.Scheduler featureName) throws FeatureNotFoundException
	{
		_dependencies.Schedulers.add(featureName);
	}

	/**
	 * Declare this feature depends on the specified state name
	 * @param featureName
	 * @throws FeatureNotFoundException
	 */
    protected final void require(FeatureName.State featureName) throws FeatureNotFoundException
	{
		_dependencies.States.add(featureName);
	}

	/**
	 * Declare this feature depends on the specified special feature
	 * @param featureName
	 */
    protected final void require(Class<?> featureClass)
	{
		_dependencies.Specials.add(featureClass);
	}

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		if (onFeatureInitialized != null)
			onFeatureInitialized.onInitialized(this, ResultCode.OK);
	}

	@Override
	public final FeatureSet dependencies()
	{
		return _dependencies;
	}

	@Override
	public final void setDependencyFeatures(Features features)
	{
		_feature = features;
	}

	@Override
	public final Type getType()
	{
		return IFeature.Type.STATE;
	}

	@Override
	public final String getName()
	{
		FeatureName.State name = getStateName();
		if (FeatureName.State.SPECIAL.equals(name))
			return getClass().getName();
		else
			return name.toString();
	}

	@Override
	public String toString()
	{
		return getType() + " " + getName();
	}

	@Override
	public final Prefs getPrefs()
	{
		return Environment.getInstance().getFeaturePrefs(getStateName());
	}

	/**
	 * @return an event messenger associated with this feature
	 */
	@Override
	public final EventMessenger getEventMessenger()
	{
		return _eventMessenger;
	}

	/**
	 * Subscribes this feature to event triggered by a given EventMessenger
	 *
	 * @param eventMessenger
	 *            the EventMessenger to subscribe to
	 * @param msgId
	 *            the id of the message to subscribe
	 */
	protected final void subscribe(EventMessenger eventMessenger, int msgId)
	{
		Log.i(TAG, getName() + ".subscribe: on " + EventMessenger.idName(msgId) + "(" + msgId + ")");
		if (isSubscribed(eventMessenger, msgId))
		{
			throw new RuntimeException(getName() + " attempts to subscribe on event " + EventMessenger.idName(msgId)
			        + "(" + msgId + ") more than once");
		}

		// add subscription for registration when this state is shown
		_subscriptions.add(new Subscription(eventMessenger, msgId));
		if (isAdded())
		{
			// register immediately if the state is already shown
			eventMessenger.register(this, msgId);
		}
	}

	/**
	 * Subscribes this feature to event triggered by a given feature
	 *
	 * @param feature
	 *            the IFeature to subscribe to
	 * @param msgId
	 *            the id of the message to subscribe
	 */
	protected final void subscribe(IFeature feature, int msgId)
	{
		subscribe(feature.getEventMessenger(), msgId);
	}

	/**
	 * Unsubscribes this feature from event triggered from EventMessenger
	 *
	 * @param eventMessenger
	 *            the EventMessenger to unsubscribe from
	 * @param msgId
	 *            the id of the message to unsubscribe
	 */
	protected final void unsubscribe(EventMessenger eventMessenger, int msgId)
	{
		Log.i(TAG, getName() + ".unsubscribe: from " + EventMessenger.idName(msgId) + "(" + msgId + ")");
		for (int i = 0; i < _subscriptions.size(); i++)
		{
			Subscription subscription = _subscriptions.get(i);
			if (subscription.EventMessenger == eventMessenger && subscription.MsgId == msgId)
			{
				_subscriptions.remove(i);
				if (isAdded())
				{
					// if already shown unregister immediately
					eventMessenger.unregister(this, msgId);
				}
				return;
			}
		}
		throw new RuntimeException(getName() + " attempts to unsubscribe from event " + EventMessenger.idName(msgId)
		        + "(" + msgId + ")"
		        + " without being subscribed. Verify if you are not unsubscribing it more than once");
	}

	/**
	 * Unsubscribes this feature from event triggered from a given feature
	 *
	 * @param feature
	 *            the IFeature to unsubscribe from
	 * @param msgId
	 *            the id of the message to unsubscribe
	 */
	protected final void unsubscribe(IFeature feature, int msgId)
	{
		unsubscribe(feature.getEventMessenger(), msgId);
	}

	/**
	 * @param eventMessenger
	 *            the EventMessenger to verify if subscribed to
	 * @param msgId
	 *            the id of the message to verify if subscribed to
	 * @return true if this feature is subscribed to EventMessenger with event
	 *         msgId
	 */
	protected final boolean isSubscribed(EventMessenger eventMessenger, int msgId)
	{
		for (int i = 0; i < _subscriptions.size(); i++)
		{
			Subscription subscription = _subscriptions.get(i);
			if (subscription.EventMessenger == eventMessenger && subscription.MsgId == msgId)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * @param feature
	 *            the IFeature to verify if subscribed to
	 * @param msgId
	 *            the id of the message to verify if subscribed to
	 * @return true if this feature is subscribed to IFeature with event
	 *         msgId
	 */
	protected final boolean isSubscribed(IFeature feature, int msgId)
	{
		return isSubscribed(feature.getEventMessenger(), msgId);
	}

	/**
	 * On showing this FeatureState
	 *
	 * @param params
	 *            The params set to this State when showing
	 * @param isViewUncovered
	 *            set to true if the view has been uncovered from overlay
	 * @throws StateException
	 */
	@Override
	protected void onShow(boolean isViewUncovered)
	{
		Log.i(TAG, getName() + ".onShow: isViewUncovered = " + isViewUncovered);
		if (!isViewUncovered)
		{
			for (Subscription subscription : _subscriptions)
			{
				Log.i(TAG, getName() + " registers on event " + EventMessenger.idName(subscription.MsgId) + "("
				        + subscription.MsgId + ")");
				subscription.EventMessenger.register(this, subscription.MsgId);
			}
		}
		Bundle bundle = new Bundle();
		bundle.putBoolean(EXTRA_ON_SHOW_UNCOVER, isViewUncovered);
		getEventMessenger().trigger(ON_SHOW, bundle);
	}

	/**
	 * On hiding this FeatureState
	 * isViewCovered true if the view has been covered by overlay
	 */
	@Override
	protected void onHide(boolean isViewCovered)
	{
		Log.i(TAG, getName() + ".onHide: isViewCovered = " + isViewCovered);
		if (!isViewCovered)
		{
			for (Subscription subscription : _subscriptions)
			{
				Log.i(TAG,
				        "Unregister " + getName() + " " + getType() + " from event "
				                + EventMessenger.idName(subscription.MsgId) + "(" + subscription.MsgId + ")");
				subscription.EventMessenger.unregister(this, subscription.MsgId);
			}
		}
		Bundle bundle = new Bundle();
		bundle.putBoolean(EXTRA_ON_HIDE_COVER, isViewCovered);
		getEventMessenger().trigger(ON_HIDE, bundle);
	}

	public abstract FeatureName.State getStateName();

	private class Subscription
	{
		EventMessenger EventMessenger;
		int MsgId;

		Subscription(EventMessenger eventMessenger, int msgId)
		{
			EventMessenger = eventMessenger;
			MsgId = msgId;
		}
	}

	@Override
	public void onEvent(int msgId, Bundle bundle)
	{
		Log.i(TAG,
		        this + ".onEvent: " + EventMessenger.idName(msgId) + "(" + msgId + ")"
		                + TextUtils.implodeBundle(bundle));
	}
}

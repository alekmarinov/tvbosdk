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
import android.util.Log;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.EventMessenger;
import com.aviq.tv.android.sdk.core.Prefs;
import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.state.BaseState;
import com.aviq.tv.android.sdk.core.state.StateException;

/**
 * Defines the base class for state feature type
 */
public abstract class FeatureState extends BaseState implements IFeature
{
	public static final String TAG = FeatureState.class.getSimpleName();
	protected FeatureSet _dependencies = new FeatureSet();
	private List<Subscription> _subscriptions = new ArrayList<Subscription>();

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		if (onFeatureInitialized != null)
			onFeatureInitialized.onInitialized(this, ResultCode.OK);
	}

	@Override
	public FeatureSet dependencies()
	{
		return _dependencies;
	}

	@Override
	public Type getType()
	{
		return IFeature.Type.STATE;
	}

	@Override
	public String getName()
	{
		FeatureName.State name = getStateName();
		if (FeatureName.Scheduler.SPECIAL.equals(name))
			return name.toString();
		else
			return getClass().getName();
	}

	@Override
	public Prefs getPrefs()
	{
		return Environment.getInstance().getFeaturePrefs(getStateName());
	}

	/**
	 * @return an event messenger associated with this feature
	 */
	@Override
	public EventMessenger getEventMessenger()
	{
		return Environment.getInstance().getEventMessenger();
	}

	@Override
	public void onEvent(int msgId, Bundle bundle)
	{
		Log.i(TAG, getName() + ".onEvent: msgId = " + msgId);
	}

	/**
	 * Subscribes this feature to event triggered from another feature
	 *
	 * @param Feature
	 *            the feature to subscribe to
	 * @param msgId
	 *            the id of the message to subscribe
	 */
	protected void subscribe(IFeature featureTo, int msgId)
	{
		Log.i(TAG, getName() + ".subscribe: for " + featureTo.getName() + " on " + msgId);
		if (isSubscribed(featureTo, msgId))
		{
			throw new RuntimeException(getName() + " attempts to subscribe to " + featureTo.getName() + " "
			        + featureTo.getType() + " on event id " + msgId + " more than once");
		}

		// add subscription for registration when this state is shown
		_subscriptions.add(new Subscription(featureTo, msgId));
		if (isCreated())
		{
			// register immediately if the state is already shown
			featureTo.getEventMessenger().register(this, msgId);
		}
	}

	/**
	 * Unsubscribes this feature from event triggered from another feature
	 *
	 * @param Feature
	 *            the feature to unsubscribe from
	 * @param msgId
	 *            the id of the message to unsubscribe
	 */
	protected void unsubscribe(IFeature featureFrom, int msgId)
	{
		Log.i(TAG, getName() + ".unsubscribe: from " + featureFrom.getName() + " on " + msgId);
		for (int i = 0; i < _subscriptions.size(); i++)
		{
			Subscription subscription = _subscriptions.get(i);
			if (subscription.Feature == featureFrom && subscription.MsgId == msgId)
			{
				_subscriptions.remove(i);
				if (isCreated())
				{
					// if already shown unregister immediately
					featureFrom.getEventMessenger().unregister(this, msgId);
				}
				return;
			}
		}
		throw new RuntimeException(getName() + " attempts to unsubscribe from " + featureFrom.getName() + " "
		        + featureFrom.getType() + " on event id " + msgId
		        + " without being subscribed. Verify if you are not unsubscribing it more than once");
	}

	/**
	 * @param featureTo
	 *            the feature to verify if subscribed to
	 * @param msgId
	 *            the id of the message to verify if subscribed to
	 * @return true if this feature is subscribed to featureTo with event msgId
	 */
	protected boolean isSubscribed(IFeature featureTo, int msgId)
	{
		for (int i = 0; i < _subscriptions.size(); i++)
		{
			Subscription subscription = _subscriptions.get(i);
			if (subscription.Feature == featureTo && subscription.MsgId == msgId)
			{
				return true;
			}
		}
		return false;
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
				Log.i(TAG,
				        getName() + " registers to " + subscription.Feature.getName() + " "
				                + subscription.Feature.getType() + " on event id = " + subscription.MsgId);
				subscription.Feature.getEventMessenger().register(this, subscription.MsgId);
			}
		}
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
				Log.i(TAG, "Unregister " + getName() + " " + getType() + " from " + subscription.Feature.getName()
				        + " " + subscription.Feature.getType() + " on event id = " + subscription.MsgId);
				subscription.Feature.getEventMessenger().unregister(this, subscription.MsgId);
			}
		}
	}

	public abstract FeatureName.State getStateName();

	private class Subscription
	{
		IFeature Feature;
		int MsgId;

		Subscription(IFeature feature, int msgId)
		{
			Feature = feature;
			MsgId = msgId;
		}
	}
}

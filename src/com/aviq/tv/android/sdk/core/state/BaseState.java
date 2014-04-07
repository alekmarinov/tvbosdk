/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTV
 * Filename:    BaseState.java
 * Author:      alek
 * Date:        Jul 16, 2013
 * Description: Base class of all application visible states
 */

package com.aviq.tv.android.sdk.core.state;

import android.app.Fragment;
import android.os.Bundle;
import android.view.View;

import com.aviq.tv.android.sdk.core.AVKeyEvent;
import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.state.StateManager.StateLayer;

/**
 * Base class of all application visible states.
 * A state may be set as background or overlay. A state is active if appears on
 * top of the States stack.
 */
public class BaseState extends Fragment
{
	private static final String TAG = BaseState.class.getSimpleName();

	/**
	 * Initialize State instance.
	 */
	public BaseState()
	{
		Log.i(TAG, this + " created");
	}

	/**
	 * Create state
	 *
	 * @param params
	 *            The params set to this State when showing
	 * @param isOverlay
	 *            set to true to show this state as Overlay
	 * @throws StateException
	 */
	public void create(Bundle params, boolean isOverlay) throws StateException
	{
		Log.i(getClass().getSimpleName(), ".create: isOverlay = " + isOverlay);
		Environment env = (Environment)getActivity();
		if (isOverlay)
			env.getStateManager().setStateOverlay(this, params);
		else
			env.getStateManager().setStateMain(this, params);
	}

	/**
	 * Create state on main layer of the screen
	 *
	 * @param params
	 *            The params set to this State when showing
	 * @throws StateException
	 */
	public void create(Bundle params) throws StateException
	{
		create(params, false);
	}

	/**
	 * Create state on overlay layer of the screen
	 *
	 * @param params
	 *            The params set to this State when showing
	 * @throws StateException
	 */
	public void createOverlay(Bundle params) throws StateException
	{
		create(params, true);
	}

	/**
	 * Destroy and remove state from screen
	 */
	public void close()
	{
		Log.i(getClass().getSimpleName(), ".close");
		Environment env = (Environment)getActivity();
		StateManager stateManager = env.getStateManager();
		StateLayer stateLayer = stateManager.getStateLayer(this);
		try
		{
			if (StateLayer.MAIN.equals(stateLayer))
			{
				stateManager.setStateMain(null, null);
			}
			else if (StateLayer.OVERLAY.equals(stateLayer))
			{
				stateManager.setStateOverlay(null, null);
			}
		}
		catch (StateException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
	}

	/**
	 * Show state view
	 */
	public void show()
	{
		Log.i(getClass().getSimpleName(), ".show");
		super.getView().setVisibility(View.VISIBLE);
	}

	/**
	 * Hide state view
	 */
	public void hide()
	{
		Log.i(getClass().getSimpleName(), ".hide");
		super.getView().setVisibility(View.INVISIBLE);
	}

	/**
	 * @return true if the this state is shown
	 */
	public boolean isShown()
	{
		return super.getView().getVisibility() == View.VISIBLE;
	}

	/**
	 * Called on showing this state. The method can be overwritten in order to
	 * initialize visualization.
	 *
	 * @param isViewUncovered
	 *            true if the view has been uncovered from overlay
	 */
	protected void onShow(boolean isViewUncovered)
	{
		Log.i(TAG, ".onShow: isViewUncovered = " + isViewUncovered);
	}

	/**
	 * Called on hiding this state.
	 *
	 * @param isViewCovered
	 *            true if the view has been covered by overlay
	 */
	protected void onHide(boolean isViewCovered)
	{
		Log.i(TAG, ".onHide: isViewCovered = " + isViewCovered);
	}

	/**
	 * Returns true if the current state is created
	 */
	public boolean isCreated()
	{
		return isAdded();
	}

	/**
	 * Method consuming key down event if the State is currently active
	 *
	 * @param event
	 *            the AVKeyEvent
	 * @return Return true to prevent this event from being propagated further,
	 *         or false to indicate that you have not handled this event and it
	 *         should continue to be propagated.
	 * @throws StateException
	 */
	public boolean onKeyDown(AVKeyEvent event)
	{
		return false;
	}

	/**
	 * Method consuming key up event if the State is currently active
	 *
	 * @param event
	 *            the AVKeyEvent
	 * @return Return true to prevent this event from being propagated further,
	 *         or false to indicate that you have not handled this event and it
	 *         should continue to be propagated.
	 * @throws StateException
	 */
	public boolean onKeyUp(AVKeyEvent event)
	{
		return false;
	}

	/**
	 * Method consuming long key press event if the State is currently active
	 *
	 * @param event
	 *            the AVKeyEvent
	 * @return Return true to prevent this event from being propagated further,
	 *         or false to indicate that you have not handled this event and it
	 *         should continue to be propagated.
	 * @throws StateException
	 */
	public boolean onKeyLongPress(AVKeyEvent event)
	{
		return false;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName();
	}
}

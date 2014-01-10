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

import com.aviq.tv.android.sdk.core.AVKeyEvent;
import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;

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
		Log.i(TAG, getClass().getSimpleName() + " created");
	}

	/**
	 * Shows State on screen.
	 *
	 * @param params
	 *            The params set to this State when showing
	 * @param isOverlay
	 *            set to true to show this state as Overlay
	 * @throws StateException
	 */
	public void show(Bundle params, boolean isOverlay) throws StateException
	{
		if (isOverlay)
			Environment.getInstance().getStateManager().setStateOverlay(this, params);
		else
			Environment.getInstance().getStateManager().setStateMain(this, params);
	}

	/**
	 * Shows State on Main layer of the screen
	 *
	 * @param params
	 *            The params set to this State when showing
	 * @throws StateException
	 */
	public void show(Bundle params) throws StateException
	{
		show(params, false);
	}

	/**
	 * Shows State on Overlay layer of the screen
	 *
	 * @param params
	 *            The params set to this State when showing
	 * @throws StateException
	 */
	public void showOverlay(Bundle params) throws StateException
	{
		show(params, true);
	}

	/**
	 * Hides State from screen
	 */
	public void hide()
	{
		Environment.getInstance().getStateManager().hideState(this);
	}

	/**
	 * Called on showing this state. The method can be overwritten in order to
	 * initialize visualization.
	 *
	 * @param isViewUncovered true if the view has been uncovered from overlay
	 */
	protected void onShow(boolean isViewUncovered)
	{
	}

	/**
	 * Called on hiding this state.
	 *
	 * @param isViewCovered true if the view has been covered by overlay
	 */
	protected void onHide(boolean isViewCovered)
	{
	}

	/**
	 * Returns true if the current state is visible on screen
	 */
	public boolean isShown()
	{
		return isAdded();
	}

	/**
	 * Method consuming key down event if the State is currently active
	 *
	 * @param event the AVKeyEvent
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
	 * @param event the AVKeyEvent
	 * @return Return true to prevent this event from being propagated further,
	 *         or false to indicate that you have not handled this event and it
	 *         should continue to be propagated.
	 * @throws StateException
	 */
	public boolean onKeyUp(AVKeyEvent event)
	{
		return false;
	}
}

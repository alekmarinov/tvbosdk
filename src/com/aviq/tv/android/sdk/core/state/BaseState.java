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

/**
 * Base class of all application visible states.
 * A state may be set as background or overlay. A state is active if appears on
 * top of the States stack.
 */
public class BaseState extends Fragment
{
	private static final String TAG = BaseState.class.getSimpleName();

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		// Recommended solution by commuinity to avoid
		// "Can not perform this action after onSaveInstanceState"
	}

	/**
	 * Initialize State instance.
	 */
	public BaseState()
	{
		Log.i(TAG, this + " created");
	}

	/**
	 * Destroy and remove state from screen
	 */
	public void close()
	{
		if (isAdded())
		{
			Log.i(getClass().getSimpleName(), ".close");
			Environment env = (Environment) getActivity();
			env.getStateManager().closeState(this);
		}
		else
		{
			Log.w(TAG, "Attempt to close already closed state");
		}
	}

	/**
	 * Show state view
	 */
	public void show()
	{
		Log.i(getClass().getSimpleName(), ".show");
		if (super.getView() != null)
			super.getView().setVisibility(View.VISIBLE);
	}

	/**
	 * Hide state view
	 */
	public void hide()
	{
		Log.i(getClass().getSimpleName(), ".hide");
		if (super.getView() != null)
			super.getView().setVisibility(View.INVISIBLE);
	}

	/**
	 * @return true if the this state is shown
	 */
	public boolean isShown()
	{
		return super.getView() != null && super.getView().getVisibility() == View.VISIBLE;
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

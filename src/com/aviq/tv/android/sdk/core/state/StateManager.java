/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTV
 * Filename:    StateManager.java
 * Author:      alek
 * Date:        Jul 16, 2013
 * Description: Control visibility of one or more States on the screen
 */

package com.aviq.tv.android.sdk.core.state;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;

import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Control visibility of one or two States on the screen and optional message
 * box state on the top. The current states are represented as a stack of size
 * limit 2 occupying layers MAIN and OVERLAY (StateLayer)
 */
public class StateManager
{
	private static final String TAG = StateManager.class.getSimpleName();
	private final Map<StateEnum, BaseState> _states = new HashMap<StateEnum, BaseState>();
	private final Stack<BaseState> _activeStates = new Stack<BaseState>();
	private final Activity _activity;
	private final Handler _handler = new Handler();
	private int _overlayBackgroundColor;
	private int _overlayBackgroundImage = 0;
	private int _mainFragmentId;
	private int _overlayFragmentId;
	private int _messageFragmentId;
	private BaseState _messageState;

	public enum StateLayer
	{
		MAIN, OVERLAY, MESSAGE
	}

	// Message stage types definition
	public enum MessageType
	{
		INFO, WARN, ERROR
	}
	public static final String PARAM_MESSAGE_TYPE = "PARAM_TYPE";
	public static final String PARAM_MESSAGE_TEXT_ID = "PARAM_TEXT_ID";

	/**
	 * Initialize StateManager instance.
	 *
	 * @param mainActivity
	 *            The owner MainActivity of this StateManager
	 */
	public StateManager(Activity activity)
	{
		_activity = activity;
		Log.i(TAG, "StateManager created");
	}

	/**
	 * Sets new State as active. If isOverlay is true than the new State appears
	 * over the current main State.
	 *
	 * @param state
	 *            The new State to activate
	 * @param params
	 *            Bundle holding params to be sent to the State when showing
	 * @param isOverlay
	 *            If this State overlays the current
	 */
	private void setState(BaseState newState, Bundle params, boolean isOverlay) throws StateException
	{
		StringBuffer logMsg = new StringBuffer();
		String stateName;
		if (newState != null)
			stateName = newState.getClass().getSimpleName();
		else
			stateName = "null";

		logMsg.append(".setState: ").append(stateName).append('(');
		TextUtils.implodeBundle(logMsg, params, '=', ',').append("), overlay=").append(isOverlay);
		Log.i(TAG, logMsg.toString());

		switch (_activeStates.size())
		{
			case 0:
				if (isOverlay)
				{
					throw new StateException(null, "Can't set overlay state `" + stateName
					        + "' without background State");
				}
				else
				{
					if (newState != null)
					{
						_activeStates.add(newState);
						showState(newState, StateLayer.MAIN, params);
					}
				}
			break;
			case 1:
				if (isOverlay)
				{
					if (newState != null)
					{
						_activeStates.add(newState);
						showState(newState, StateLayer.OVERLAY, params);
					}
				}
				else
				{
					hideState(_activeStates.pop());
					if (newState != null)
					{
						_activeStates.add(newState);
						showState(newState, StateLayer.MAIN, params);
					}
				}
			break;
			case 2:
				if (isOverlay)
				{
					hideState(_activeStates.pop());
					if (newState != null)
					{
						_activeStates.add(newState);
						showState(newState, StateLayer.OVERLAY, params);
					}
					else
					{
						_activeStates.get(0).onShow();
						// !!! This breaks the EPG grid
						// _activity.findViewById(R.id.main_fragment).requestFocus();
						// _activeStates.elementAt(_activeStates.size() -
						// 1).getView().requestFocus();
					}
				}
				else
				{
					hideState(_activeStates.pop());
					hideState(_activeStates.pop());
					if (newState != null)
					{
						_activeStates.add(newState);
						showState(newState, StateLayer.MAIN, params);
					}
				}
			break;
		}
	}

	/**
	 * Sets new main State as active.
	 *
	 * @param state
	 *            The new State to activate
	 * @param params
	 *            Bundle holding params to be sent to the State when showing
	 */
	public void setStateMain(BaseState state, Bundle params) throws StateException
	{
		setState(state, params, false);
	}

	/**
	 * Sets new State as active overlay.
	 *
	 * @param state
	 *            The new State to activate
	 * @param params
	 *            Bundle holding params to be sent to the State when showing
	 */
	public void setStateOverlay(BaseState state, Bundle params) throws StateException
	{
		setState(state, params, true);
	}

	/**
	 * Hides overlay state
	 */
	public void hideStateOverlay()
	{
		try
		{
			setState(null, null, true);
		}
		catch (StateException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
	}

	/**
	 * Displays state on screen at specified state layer (see StateLayer)
	 *
	 * @param state
	 *            to be shown
	 * @param stateLayer
	 *            the layer which this state will occupy
	 * @param params
	 *            Bundle with State params
	 */
	/* package */void showState(final BaseState state, final StateLayer stateLayer, final Bundle params)
	{
		StringBuffer logMsg = new StringBuffer();
		logMsg.append(".showState: ").append(state.getClass().getSimpleName()).append('(');
		TextUtils.implodeBundle(logMsg, params, '=', ',').append("), layer=").append(stateLayer.name());
		Log.i(TAG, logMsg.toString());

		// Workaround of setting fragment arguments when the fragment is already
		// added
		Runnable showFragmentChunk = new Runnable()
		{
			@Override
			public void run()
			{
				state.setArguments(params);
				FragmentTransaction ft = _activity.getFragmentManager().beginTransaction();
				int fragmentId = 0;
				switch (stateLayer)
				{
					case MAIN:
						fragmentId = _mainFragmentId;
					break;
					case OVERLAY:
						fragmentId = _overlayFragmentId;
					break;
					case MESSAGE:
						fragmentId = _messageFragmentId;
					break;
				}
				if (fragmentId == 0)
					throw new RuntimeException("Set fragment layer resource ids with method setFragmentLayerResources");

				ft.add(fragmentId, state);
				// FIXME: make transition effect depending on state's StateLayer
				ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
				ft.commit();

				_handler.post(new Runnable()
				{
					@Override
					public void run()
					{
						if (stateLayer.equals(StateLayer.OVERLAY))
						{
							if (_overlayBackgroundImage != 0)
								state.getView().setBackgroundResource(_overlayBackgroundImage);
							else
								state.getView().setBackgroundColor(_overlayBackgroundColor);

							_activeStates.get(0).onHide();
						}

						// notify state is shown
						state.onShow();
					}
				});
			}
		};
		if (state.isAdded())
		{
			hideState(state);
			_handler.post(showFragmentChunk);
		}
		else
		{
			showFragmentChunk.run();
		}
	}

	/**
	 * Gets State instance by enum
	 *
	 * @param stateEnum
	 * @return State instance corresponding to the specified enum
	 */
	public BaseState getState(StateEnum stateEnum)
	{
		return _states.get(stateEnum);
	}

	/**
	 * Gets current active main state instance
	 *
	 * @return current state instance
	 */
	public BaseState getMainState()
	{
		if (_activeStates.size() > 0)
			return _activeStates.get(0);
		return null;
	}

	/**
	 * Gets current active overlay state instance
	 *
	 * @return current overlay instance
	 */
	public BaseState getOverlayState()
	{
		if (_activeStates.size() > 1)
			return _activeStates.get(1);
		return null;
	}

	/**
	 * Delegates key down event to the current active state or overlay
	 *
	 * @param keyCode
	 *            The value in event.getKeyCode().
	 * @param event
	 *            Description of the key event.
	 * @return Return true to prevent this event from being propagated further,
	 *         or false to indicate that you have not handled this event and it
	 *         should continue to be propagated.
	 * @throws StateException
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		Log.i(TAG, ".onKeyDown: keyCode = " + keyCode + ", state = " + getMainState() + ", overlay = "
		        + getOverlayState());

		if (_activeStates.size() > 0)
			return _activeStates.get(_activeStates.size() - 1).onKeyDown(keyCode, event);

		return false;
	}

	/**
	 * Delegates key up event to the current active state or overlay
	 *
	 * @param keyCode
	 *            The value in event.getKeyCode().
	 * @param event
	 *            Description of the key event.
	 * @return Return true to prevent this event from being propagated further,
	 *         or false to indicate that you have not handled this event and it
	 *         should continue to be propagated.
	 * @throws StateException
	 */
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		Log.i(TAG, ".onKeyUp: keyCode = " + keyCode + ", state = " + getMainState() + ", overlay = "
		        + getOverlayState());
		if (_activeStates.size() > 0)
			return _activeStates.get(_activeStates.size() - 1).onKeyUp(keyCode, event);

		return false;
	}

	// TODO: Add onLongKeyDown() method
	// TODO: Add onLongKeyUp() method

	/**
	 * Show message box
	 *
	 * @param msgType
	 *            determine the kind of message (see MessageBox.Type)
	 * @param stringId
	 *            string resource identifier for the message text
	 */
	public void showMessage(MessageType msgType, int stringId)
	{
		final Bundle params = new Bundle();
		params.putString(PARAM_MESSAGE_TYPE, msgType.name());
		params.putInt(PARAM_MESSAGE_TEXT_ID, stringId);
		showState(_messageState, StateLayer.MESSAGE, params);
	}

	/**
	 * Hides message box
	 */
	public void hideMessage()
	{
		hideState(_messageState);
	}

	/**
	 * Removes state fragment from screen
	 *
	 * @param state
	 *            to be removed from screen
	 */
	/* package */void hideState(BaseState state)
	{
		Log.i(TAG, ".hideState: " + state.getClass().getSimpleName());

		if (state.isAdded())
		{
			FragmentTransaction ft = _activity.getFragmentManager().beginTransaction();
			ft.remove(state);
			ft.commit();

			// notify state is hidden
			state.onHide();
		}
	}

	public void setFragmentLayerResources(int mainFragmentId, int overlayFragmentId, int messageFragmentId)
	{
		_mainFragmentId = mainFragmentId;
		_overlayFragmentId = overlayFragmentId;
		_messageFragmentId = messageFragmentId;
	}

	public void setMessageState(BaseState messageState)
	{
		_messageState = messageState;
	}

	public void setOverlayBackgroundColor(int overlayBackgroundColor)
	{
		_overlayBackgroundColor = overlayBackgroundColor;
	}

	public void setOverlayBackgroundImage(int overlayBackgroundImage)
	{
		_overlayBackgroundImage = overlayBackgroundImage;
	}
}

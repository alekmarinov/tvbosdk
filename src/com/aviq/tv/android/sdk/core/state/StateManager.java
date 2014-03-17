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

import java.util.ArrayList;
import java.util.Stack;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.aviq.tv.android.sdk.core.AVKeyEvent;
import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.utils.TextUtils;

/**
 * Control visibility of one or two States on the screen and optional message
 * box state on the top. The current states are represented as a stack of size
 * limit 2 occupying layers MAIN and OVERLAY (StateLayer)
 */
public class StateManager
{
	private static final String TAG = StateManager.class.getSimpleName();
	private final Stack<BaseState> _activeStates = new Stack<BaseState>();
	private final Activity _activity;
	private final Handler _handler = new Handler();
	private int _overlayBackgroundColor = 0;
	private int _overlayBackgroundImage = 0;
	private int _mainFragmentId;
	private int _overlayFragmentId;
	private int _messageFragmentId;
	private BaseState _messageState;

	public enum StateLayer
	{
		MAIN, OVERLAY, MESSAGE
	}

	public static class MessageParams
	{
		public static final String PARAM_TYPE = "PARAM_TYPE";
		public static final String PARAM_TITLE = "PARAM_TITLE";
		public static final String PARAM_TEXT = "PARAM_TEXT";
		public static final String PARAM_SKIP_BTN_IMAGE = "PARAM_SKIP_BTN_IMAGE";
		public static final String PARAM_AUTO_HIDE_DELAY = "PARAM_AUTO_HIDE_DELAY";
		public static final String PARAM_AUTO_HIDE_DEFAULT_BUTTON = "PARAM_AUTO_HIDE_DEFAULT_BUTTON";
		public static final String PARAM_POSITIVE_BUTTON_LABEL = "PARAM_POSITIVE_BUTTON_LABEL";
		public static final String PARAM_NEGATIVE_BUTTON_LABEL = "PARAM_NEGATIVE_BUTTON_LABEL";
		public static final String PARAM_IMAGE_URL = "PARAM_IMAGE_URL";
		public static final String PARAM_CUSTOM_BUTTON_LABEL = "PARAM_CUSTOM_BUTTON_LABEL";
		public static final String PARAM_CUSTOM_BUTTON_ACTION_CODE = "PARAM_CUSTOM_BUTTON_ACTION_CODE";

		public enum Type
		{
			INFO, WARN, ERROR
		}

		public enum Button
		{
			OK, CANCEL, YES, NO, POSITIVE_BUTTON, NEGATIVE_BUTTON, CUSTOM
		}

		private Bundle _bundle = new Bundle();

		public MessageParams setType(Type type)
		{
			_bundle.putString(PARAM_TYPE, type.name());
			return this;
		}

		public MessageParams setTitle(String title)
		{
			_bundle.putString(PARAM_TITLE, title);
			return this;
		}

		public MessageParams setTitle(int titleId)
		{
			_bundle.putString(PARAM_TITLE, Environment.getInstance().getResources().getString(titleId));
			return this;
		}

		public MessageParams setText(String text)
		{
			_bundle.putString(PARAM_TEXT, text);
			return this;
		}

		public MessageParams setText(int textId)
		{
			_bundle.putString(PARAM_TEXT, Environment.getInstance().getResources().getString(textId));
			return this;
		}

		public MessageParams setImageUrl(String url)
		{
			_bundle.putString(PARAM_IMAGE_URL, url);
			return this;
		}

		public MessageParams enableButton(Button buttonName)
		{
			_bundle.putBoolean(buttonName.name(), true);
			return this;
		}

		public MessageParams enablePositiveButton(int labelResId)
		{
			_bundle.putBoolean(Button.POSITIVE_BUTTON.name(), true);
			_bundle.putInt(PARAM_POSITIVE_BUTTON_LABEL, labelResId);
			return this;
		}

		public MessageParams enableNegativeButton(int labelResId)
		{
			_bundle.putBoolean(Button.NEGATIVE_BUTTON.name(), true);
			_bundle.putInt(PARAM_NEGATIVE_BUTTON_LABEL, labelResId);
			return this;
		}

		public MessageParams addCustomButton(String label, int actionCode)
		{
			ArrayList<Bundle> customButtonList = _bundle.getParcelableArrayList(Button.CUSTOM.name());
			if (customButtonList == null)
			{
				customButtonList = new ArrayList<Bundle>();
				_bundle.putParcelableArrayList(Button.CUSTOM.name(), customButtonList);
			}

			Bundle bundle = new Bundle();
			bundle.putString(PARAM_CUSTOM_BUTTON_LABEL, label);
			bundle.putInt(PARAM_CUSTOM_BUTTON_ACTION_CODE, actionCode);

			customButtonList.add(bundle);
			return this;
		}

		public MessageParams skipButtonImage(boolean skipImage)
		{
			_bundle.putBoolean(PARAM_SKIP_BTN_IMAGE, skipImage);
			return this;
		}

		public MessageParams setAutoHideDelay(int delayMillis)
		{
			_bundle.putInt(PARAM_AUTO_HIDE_DELAY, delayMillis);
			return this;
		}

		public MessageParams setAutoHideButton(Button buttonName)
		{
			_bundle.putString(PARAM_AUTO_HIDE_DEFAULT_BUTTON, buttonName.name());
			return this;
		}

		public Bundle getParamsBundle()
		{
			return _bundle;
		}
	}

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
						createState(newState, StateLayer.MAIN, params);
					}
				}
			break;
			case 1:
				if (isOverlay)
				{
					if (newState != null)
					{
						_activeStates.add(newState);
						createState(newState, StateLayer.OVERLAY, params);
					}
				}
				else
				{
					closeState(_activeStates.pop());
					if (newState != null)
					{
						_activeStates.add(newState);
						createState(newState, StateLayer.MAIN, params);
					}
				}
			break;
			case 2:
				if (isOverlay)
				{
					closeState(_activeStates.pop());
					if (newState != null)
					{
						_activeStates.add(newState);
						createState(newState, StateLayer.OVERLAY, params);
					}
					else
					{
						// restore focus of the uncovered view
						_activeStates.get(0).onShow(true);
					}
				}
				else
				{
					closeState(_activeStates.pop());
					closeState(_activeStates.pop());
					if (newState != null)
					{
						_activeStates.add(newState);
						createState(newState, StateLayer.MAIN, params);
					}
				}
			break;
		}
	}

	public StateLayer getStateLayer(BaseState state)
	{
		switch (_activeStates.indexOf(state))
		{
			case 0:
				return StateLayer.MAIN;
			case 1:
				return StateLayer.OVERLAY;
		}
		return null;
	}

	/**
	 * Replace one state with another keeping the same layer
	 *
	 * @param currentState
	 *            The state to be replaced. It must occupy MAIN or OVERLAY layer
	 * @param newState
	 *            The new State to replace with
	 * @param params
	 *            Bundle holding params to be sent to the State when showing
	 */
	public void replaceState(BaseState currentState, BaseState newState, Bundle params) throws StateException
	{
		StateLayer stateLayer = getStateLayer(currentState);
		if (stateLayer == null)
		{
			throw new StateException(currentState, "State " + currentState.getClass().getSimpleName()
			        + " is not active to be replaced by " + newState.getClass().getSimpleName());
		}
		if (StateLayer.MAIN.equals(stateLayer))
			setStateMain(newState, params);
		else if (StateLayer.OVERLAY.equals(stateLayer))
			setStateOverlay(newState, params);
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
	/* package */void createState(final BaseState state, final StateLayer stateLayer, final Bundle params)
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
							View stateView = state.getView();
							if (stateView != null)
							{
								if (_overlayBackgroundImage != 0)
									state.getView().setBackgroundResource(_overlayBackgroundImage);
								else if (_overlayBackgroundColor != 0)
									state.getView().setBackgroundColor(_overlayBackgroundColor);
								else
								{
									Log.i(TAG, "Overlay background is not defined in StateManager");
								}
							}
							else
							{
								Log.e(TAG, "The view of overlay " + state.getClass().getName() + " is null!");
							}
							_activeStates.get(0).onHide(true);
						}

						// notify state is shown
						state.onShow(false);
					}
				});
			}
		};
		if (state.isAdded())
		{
			closeState(state);
			_handler.post(showFragmentChunk);
		}
		else
		{
			showFragmentChunk.run();
		}
	}

	/**
	 * FIXME: rename to getMainState for consistency with setStateMain
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
	 * FIXME: rename to getStateOverlay for consistency with setStateOverlay
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
	public boolean onKeyDown(AVKeyEvent keyEvent)
	{
		Log.i(TAG, ".onKeyDown: key = " + keyEvent + ", state = " + getMainState() + ", overlay = " + getOverlayState());

		if (_messageState.isAdded())
		{
			if (_messageState.onKeyDown(keyEvent))
				return true;
		}
		if (_activeStates.size() > 0)
		{
			return _activeStates.get(_activeStates.size() - 1).onKeyDown(keyEvent);
		}

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
	public boolean onKeyUp(AVKeyEvent keyEvent)
	{
		Log.i(TAG, ".onKeyUp: key = " + keyEvent + ", state = " + getMainState() + ", overlay = " + getOverlayState());

		if (_messageState.isAdded())
		{
			return _messageState.onKeyUp(keyEvent);
		}
		else if (_activeStates.size() > 0)
		{
			return _activeStates.get(_activeStates.size() - 1).onKeyUp(keyEvent);
		}

		return false;
	}

	/**
	 * Delegates long key press event to the current active state or overlay
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
	public boolean onKeyLongPress(AVKeyEvent keyEvent)
	{
		Log.i(TAG, ".onKeyLongPress: key = " + keyEvent + ", state = " + getMainState() + ", overlay = "
		        + getOverlayState());

		if (_messageState.isAdded())
		{
			return _messageState.onKeyLongPress(keyEvent);
		}
		else if (_activeStates.size() > 0)
		{
			return _activeStates.get(_activeStates.size() - 1).onKeyLongPress(keyEvent);
		}

		return false;
	}

	// TODO: Add onLongKeyDown() method
	// TODO: Add onLongKeyUp() method

	/**
	 * Show message box
	 *
	 * @param MessageParams
	 *            message box parameters
	 * @return BaseState used to display the message. Use this reference to
	 *         register for message box events.
	 */
	public BaseState showMessage(MessageParams messageParams)
	{
		createState(_messageState, StateLayer.MESSAGE, messageParams.getParamsBundle());
		return _messageState;
	}

	/**
	 * @return BaseState used to display the message
	 */
	public BaseState getMessageState()
	{
		return _messageState;
	}

	/**
	 * Hides message box
	 */
	public void hideMessage()
	{
		if (closeState(_messageState))
			if (_activeStates.size() > 0)
				_activeStates.get(_activeStates.size() - 1).onShow(true);
	}

	/**
	 * Removes state fragment from screen
	 *
	 * @param state
	 *            to be removed from screen
	 */
	/* package */boolean closeState(BaseState state)
	{
		Log.i(TAG, ".hideState: " + state.getClass().getSimpleName());

		if (state.isAdded())
		{
			FragmentTransaction ft = _activity.getFragmentManager().beginTransaction();
			ft.remove(state);
			ft.commit();

			// notify state is hidden
			state.onHide(false);
			return true;
		}
		return false;
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

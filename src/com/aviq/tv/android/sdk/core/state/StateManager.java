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
import java.util.List;
import java.util.Stack;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.aviq.tv.android.sdk.core.AVKeyEvent;
import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureState;
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
	private int _mainBackgroundColor = 0;
	private int _mainBackgroundImage = 0;
	private int _mainFragmentId;
	private int _messageFragmentId;
	private BaseState _messageState;
	private ViewGroup _contentView;
	private List<Integer> _overlayFragmentIds = new ArrayList<Integer>();
	private FrameLayout _mainFrame;
	private FrameLayout _overlayFrame;
	private FrameLayout _messageFrame;
	private RelativeLayout _overlayLayout;
	private int _viewLayerId = 0x00af0001;
	private boolean _inSetState = false;
	private boolean _inCreateState = false;
	private Stack<StateHistoryEntry> _stateHistory = new Stack<StateHistoryEntry>();

	private class StateHistoryEntry
	{
		BaseState state;
		Bundle params;

		StateHistoryEntry(BaseState state, Bundle params)
		{
			this.state = state;
			this.params = params;
		}
	}

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

	public StateManager(Activity activity)
	{
		_activity = activity;
		// Create enveloping activity layout
		RelativeLayout contentView = new RelativeLayout(_activity);
		contentView.setBackgroundColor(Environment.getInstance().getPrefs()
		        .getInt(Environment.Param.MAIN_BACKGROUND_COLOR));
		_contentView = contentView;
		_activity.setContentView(contentView);

		// Create frame layout for main state
		_mainFrame = new FrameLayout(_activity);

		// Create layout holder for arbitrary number of overlay states
		_overlayLayout = new RelativeLayout(_activity);

		// Create frame layout for one overlay state
		_overlayFrame = new FrameLayout(_activity);
		_overlayFrame.setId(_viewLayerId++);
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
		        RelativeLayout.LayoutParams.MATCH_PARENT);
		_overlayLayout.addView(_overlayFrame, lp);

		// Create frame layout for message state
		_messageFrame = new FrameLayout(_activity);

		// add main state frame layout
		addViewLayer(_mainFrame, false);
		addViewLayer(_overlayLayout, false);
		// addViewLayer(overlayFrame, false);
		addViewLayer(_messageFrame, false);

		_mainFragmentId = _mainFrame.getId();
		_overlayFragmentIds.add(_overlayFrame.getId());
		_messageFragmentId = _messageFrame.getId();

		Log.i(TAG, "StateManager created");
	}

	private ViewGroup createContentView(Context context)
	{
		return new RelativeLayout(context);
	}

	/**
	 * Add custom view to the main activity
	 *
	 * @param viewLayer
	 * @param isBottom
	 *            true if the view must be added at the bottom of the other
	 *            views
	 */
	public void addViewLayer(View viewLayer, boolean isBottom)
	{
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
		        RelativeLayout.LayoutParams.MATCH_PARENT);
		lp.addRule(RelativeLayout.CENTER_IN_PARENT);

		viewLayer.setId(_viewLayerId);
		if (isBottom)
			_contentView.addView(viewLayer, 0, lp);
		else
			_contentView.addView(viewLayer, lp);
		_viewLayerId++;
	}

	private View createFrameLayout()
	{
		return new FrameLayout(_activity);
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
	private void setState(BaseState newState, Bundle params, boolean isOverlay, boolean isOverlayAdd,
	        boolean replaceMainState) throws StateException
	{
		try
		{
			if (_inSetState)
			{
				throw new StateException(null, "setState is not re-entrant!");
			}
			_inSetState = true;
			StringBuffer logMsg = new StringBuffer();
			String stateName;
			if (newState != null)
				stateName = newState.getClass().getSimpleName();
			else
				stateName = "null";

			logMsg.append(".setState: ").append(stateName).append('(');
			TextUtils.implodeBundle(logMsg, params, '=', ',').append("), overlay=").append(isOverlay);
			Log.i(TAG, logMsg.toString());

			// FIXME: just for debugging
			// if ("FeatureStateVODDetails".equals(stateName) &&
			// params.getParcelableArrayList("VODGROUP_PATH") == null)
			// throw new RuntimeException("Who are you?");

			switch (_activeStates.size())
			{
				case 0:
					if (isOverlay)
					{
						throw new StateException(null, "Can't set overlay state `" + stateName + "' without main State");
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
							_activeStates.get(_activeStates.size() - 1).onHide(true);
							_activeStates.add(newState);
							createState(newState, StateLayer.OVERLAY, params);
						}
					}
					else
					{
						if (!replaceMainState)
							removeState(_activeStates.pop());
						else
							removeState(_activeStates.remove(0));
						if (newState != null)
						{
							if (!replaceMainState)
								_activeStates.add(newState);
							else
								_activeStates.add(0, newState);
							createState(newState, StateLayer.MAIN, params);
						}
					}
				break;
				default:
					if (isOverlay)
					{
						if (!isOverlayAdd)
						{
							while (_activeStates.size() > 1)
								removeState(_activeStates.pop());
						}
						if (newState != null)
						{
							_activeStates.get(_activeStates.size() - 1).onHide(true);
							_activeStates.add(newState);
							createState(newState, StateLayer.OVERLAY, params);
						}
						else
						{
							if (isOverlayAdd)
								removeState(_activeStates.pop());

							// restore focus of the uncovered view
							_activeStates.get(_activeStates.size() - 1).onShow(true);
						}
					}
					else
					{
						if (!replaceMainState)
						{
							while (_activeStates.size() > 0)
								removeState(_activeStates.pop());
						}
						else
						{
							removeState(_activeStates.remove(0));
						}
						if (newState != null)
						{
							if (!replaceMainState)
								_activeStates.add(newState);
							else
								_activeStates.add(0, newState);
							createState(newState, StateLayer.MAIN, params);
						}
					}
				break;
			}
		}
		finally
		{
			_inSetState = false;
		}
	}

	public StateLayer getStateLayer(BaseState state)
	{
		int stateIndex = _activeStates.indexOf(state);
		if (stateIndex == 0)
			return StateLayer.MAIN;
		else if (stateIndex > 0)
			return StateLayer.OVERLAY;
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
		setState(state, params, false, false, false);
	}

	/**
	 * Adds State to history stack
	 *
	 * @param state
	 *            State to add in the history stack
	 * @param params
	 *            Bundle holding params to be sent to the State when activated
	 */
	public void pushStateHistory(BaseState state, Bundle params)
	{
		StringBuffer logMsg = new StringBuffer();
		logMsg.append(".pushStateHistory: ").append(state.getClass().getSimpleName()).append('(');
		TextUtils.implodeBundle(logMsg, params, '=', ',').append(")");
		Log.i(TAG, logMsg.toString());
		_stateHistory.add(new StateHistoryEntry(state, params));
	}

	/**
	 * Sets new main state as active pop'ed from the top of history stack if not
	 * empty
	 */
	public void popStateHistory() throws StateException
	{
		if (_stateHistory.size() > 0)
		{
			StateHistoryEntry entry = _stateHistory.pop();
			StringBuffer logMsg = new StringBuffer();
			logMsg.append(".popStateHistory: ").append(entry.state.getClass().getSimpleName()).append('(');
			TextUtils.implodeBundle(logMsg, entry.params, '=', ',').append(")");
			Log.i(TAG, logMsg.toString());
			setStateMain(entry.state, entry.params);
		}
	}

	/**
	 * @return size of history with pushed main states
	 */
	public int getStateHistorySize()
	{
		return _stateHistory.size();
	}

	/**
	 * Clears the history of pushed Main states
	 */
	public void clearStateHistory()
	{
		_stateHistory.clear();
	}

	/**
	 * Replace the main State with keeping the above overlays
	 *
	 * @param state
	 *            The new State to activate replacing the current
	 * @param params
	 *            Bundle holding params to be sent to the State when showing
	 */
	public void replaceStateMain(BaseState state, Bundle params) throws StateException
	{
		setState(state, params, false, false, true);
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
		setState(state, params, true, false, false);
	}

	/**
	 * Add new State as active overlay.
	 *
	 * @param state
	 *            The new State to activate
	 * @param params
	 *            Bundle holding params to be sent to the State when showing
	 */
	public void addStateOverlay(BaseState state, Bundle params) throws StateException
	{
		setState(state, params, true, true, false);
	}

	/**
	 * Hides overlay state
	 */
	public void hideStateOverlay()
	{
		try
		{
			setState(null, null, true, false, false);
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
	 * @throws StateException
	 */
	/* package */void createState(final BaseState state, final StateLayer stateLayer, final Bundle params)
	        throws StateException
	{
		try
		{
			if (_inCreateState)
			{
				throw new StateException(null, "createState is not re-entrant!");
			}
			_inCreateState = true;
			StringBuffer logMsg = new StringBuffer();
			logMsg.append(".showState: ").append(state.getClass().getSimpleName()).append('(');
			TextUtils.implodeBundle(logMsg, params, '=', ',').append("), layer=").append(stateLayer.name());
			Log.i(TAG, logMsg.toString());

			// Workaround of setting fragment arguments when the fragment is
			// already
			// added
			Runnable showFragmentChunk = new Runnable()
			{
				@Override
				public void run()
				{
					state.setArguments(params);
					if (!state.isAdded())
					{
						FragmentTransaction ft = _activity.getFragmentManager().beginTransaction();
						int fragmentId = 0;
						switch (stateLayer)
						{
							case MAIN:
								fragmentId = _mainFragmentId;
								_mainFrame.requestFocus();
							break;
							case OVERLAY:
								int nOverlays = _activeStates.size() - 1;
								_overlayFrame.requestFocus();
								while (nOverlays > _overlayFragmentIds.size())
								{
									// add new overlay frame
									View overlayFrame = new FrameLayout(_activity);
									overlayFrame.setId(_viewLayerId++);
									RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
									        RelativeLayout.LayoutParams.MATCH_PARENT,
									        RelativeLayout.LayoutParams.MATCH_PARENT);
									_overlayLayout.addView(overlayFrame, lp);
									_overlayFragmentIds.add(overlayFrame.getId());
									overlayFrame.requestFocus();
								}
								fragmentId = _overlayFragmentIds.get(_overlayFragmentIds.size() - 1);
							break;
							case MESSAGE:
								fragmentId = _messageFragmentId;
								_messageFrame.requestFocus();
							break;
						}
						if (fragmentId == 0)
							throw new RuntimeException(
							        "Set fragment layer resource ids with method setFragmentLayerResources");

						ft.add(fragmentId, state);
						// FIXME: make transition effect depending on state's
						// StateLayer
						ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
						// ft.commit();
						ft.commitAllowingStateLoss();
					}
					_handler.post(new Runnable()
					{
						@Override
						public void run()
						{
							setStateBackground(stateLayer);
							// notify state is shown
							state.onShow(false);

							if (FeatureState.class.isAssignableFrom(state.getClass()))
							{
								FeatureState featureState = (FeatureState) state;
								Bundle bundle = new Bundle();
								bundle.putString(Environment.ExtraStateChanged.STATE_NAME.name(), featureState
								        .getStateName().name());
								Environment.getInstance().getEventMessenger()
								        .trigger(Environment.ON_STATE_CHANGED, bundle);
							}
						}

						private void setStateBackground(StateLayer layer)
						{
							int bgColor = 0;
							int bgImage = 0;
							if (StateLayer.OVERLAY == layer)
							{
								bgColor = _overlayBackgroundColor;
								bgImage = _overlayBackgroundImage;
							}
							// else if (StateLayer.MAIN == layer)
							// {
							// bgColor = _mainBackgroundColor;
							// bgImage = _mainBackgroundImage;
							// }
							View stateView = state.getView();
							if (stateView != null)
							{
								if (bgColor != 0)
									state.getView().setBackgroundColor(bgColor);
								else if (bgImage != 0)
									state.getView().setBackgroundResource(bgImage);
								else
								{
									Log.i(TAG, "Background is not defined in StateManager");
								}
							}
							else
							{
								Log.e(TAG, "The view of overlay " + state.getClass().getName() + " is null!");
							}
						}
					});
				}
			};
			if (state.isAdded())
			{
				removeState(state);
				_handler.post(showFragmentChunk);
			}
			else
			{
				showFragmentChunk.run();
			}
		}
		finally
		{
			_inCreateState = false;
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
		StringBuffer statesDump = new StringBuffer();
		for (int i = 0; i < _activeStates.size(); i++)
		{
			if (i > 0)
				statesDump.append(", ");
			statesDump.append(_activeStates.get(i));
		}
		Log.i(TAG, ".onKeyDown: key = " + keyEvent + ", states = " + statesDump);

		if (_messageState.isAdded())
		{
			if (_messageState.onKeyDown(keyEvent))
				return true;
		}
		if (_activeStates.size() > 0)
		{
			Log.i(TAG, ".onKeyDown: delegating " + keyEvent + " to " + _activeStates.get(_activeStates.size() - 1));
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
		StringBuffer statesDump = new StringBuffer();
		for (int i = 0; i < _activeStates.size(); i++)
		{
			if (i > 0)
				statesDump.append(", ");
			statesDump.append(_activeStates.get(i));
		}
		Log.i(TAG, ".onKeyUp: key = " + keyEvent + ", states = " + statesDump);

		if (_messageState.isAdded())
		{
			return _messageState.onKeyUp(keyEvent);
		}
		else if (_activeStates.size() > 0)
		{
			Log.i(TAG, ".onKeyUp: delegating " + keyEvent + " to " + _activeStates.get(_activeStates.size() - 1));
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
		StringBuffer statesDump = new StringBuffer();
		for (int i = 0; i < _activeStates.size(); i++)
		{
			if (i > 0)
				statesDump.append(", ");
			statesDump.append(_activeStates.get(i));
		}
		Log.i(TAG, ".onKeyLongPress: key = " + keyEvent + ", states = " + statesDump);

		if (_messageState.isAdded())
		{
			return _messageState.onKeyLongPress(keyEvent);
		}
		else if (_activeStates.size() > 0)
		{
			Log.i(TAG, ".onKeyLongPress: delegating " + keyEvent + " to " + _activeStates.get(_activeStates.size() - 1));
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
		try
		{
			createState(_messageState, StateLayer.MESSAGE, messageParams.getParamsBundle());
		}
		catch (StateException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
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
		if (removeState(_messageState))
			if (_activeStates.size() > 0)
				_activeStates.get(_activeStates.size() - 1).onShow(true);
	}

	public void closeState(BaseState state)
	{
		Log.i(TAG, ".closeState: state = " + state);
		StateLayer stateLayer = getStateLayer(state);
		try
		{
			if (StateLayer.MAIN.equals(stateLayer))
			{
				setStateMain(null, null);
			}
			else if (StateLayer.OVERLAY.equals(stateLayer))
			{
				setState(null, null, true, true, false);
			}
		}
		catch (StateException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
	}

	/**
	 * Removes state fragment from screen
	 *
	 * @param state
	 *            to be removed from screen
	 */
	private boolean removeState(BaseState state)
	{
		Log.i(TAG, ".hideState: " + state.getClass().getSimpleName());

		if (state.isAdded())
		{
			try
			{
				FragmentTransaction ft = _activity.getFragmentManager().beginTransaction();
				ft.remove(state);
				ft.commit();
			}
			catch (IllegalStateException e)
			{
				Log.e(TAG, e.getMessage(), e);
			}

			// notify state is hidden
			state.onHide(false);
			return true;
		}
		return false;
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

	public void setMainBackgroundColor(int overlayBackgroundColor)
	{
		_mainBackgroundColor = overlayBackgroundColor;
	}

	public void setMainBackgroundImage(int overlayBackgroundImage)
	{
		_mainBackgroundImage = overlayBackgroundImage;
	}
}

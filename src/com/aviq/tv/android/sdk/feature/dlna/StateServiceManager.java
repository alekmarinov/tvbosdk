/**
 * Copyright (c) 2007-2015, Intelibo Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    StateServiceManager.java
 * Author:      alek
 * Date:        25 Jun 2015
 * Description:
 */

package com.aviq.tv.android.sdk.feature.dlna;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.fourthline.cling.model.Command;
import org.fourthline.cling.model.ModelUtil;
import org.fourthline.cling.model.ServiceManager;
import org.fourthline.cling.model.meta.LocalService;
import org.fourthline.cling.model.meta.StateVariable;
import org.fourthline.cling.model.state.StateVariableAccessor;
import org.fourthline.cling.model.state.StateVariableValue;
import org.seamless.util.Exceptions;

import com.aviq.tv.android.sdk.core.Log;

/**
 *
 */
@SuppressWarnings(
{ "unchecked", "rawtypes" })
public class StateServiceManager implements ServiceManager<Object>
{
	private static final String TAG = StateServiceManager.class.getSimpleName();

	private LocalService<?> _stateService;
	private final ReentrantLock _lock = new ReentrantLock(true);
	private PropertyChangeSupport _propertyChangeSupport;

	// The monitor entry and exit methods
	protected void lock()
	{
		try
		{
			if (!_lock.tryLock(getLockTimeoutMillis(), TimeUnit.MILLISECONDS))
			{
				throw new RuntimeException("Failed to acquire lock in milliseconds: " + getLockTimeoutMillis());
			}
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException("Failed to acquire lock:" + e);
		}
	}

	protected void unlock()
	{
		_lock.unlock();
	}

	protected int getLockTimeoutMillis()
	{
		return 500;
	}

	StateServiceManager(LocalService<?> stateService)
	{
		_stateService = stateService;
	}

	@Override
	public void execute(Command cmd) throws Exception
	{
		Log.i(TAG, ".execute: cmd = " + cmd);
	}

	@Override
	public Collection getCurrentState() throws Exception
	{
		lock();
		Log.i(TAG, ".getCurrentState()");

		try
		{
			Collection<StateVariableValue> values = new ArrayList<StateVariableValue>();
			for (StateVariable stateVariable : getService().getStateVariables())
			{
				if (stateVariable.getEventDetails().isSendEvents())
				{
					StateVariableAccessor accessor = getService().getAccessor(stateVariable);
					if (accessor == null)
						throw new IllegalStateException("No accessor for evented state variable");
					values.add(accessor.read(stateVariable, getImplementation()));
				}
			}
			return values;
		}
		finally
		{
			unlock();
		}
	}

	private Collection<StateVariableValue> getCurrentState(String[] variableNames) throws Exception
	{
		lock();
		try
		{
			Log.i(TAG, ".getCurrentState: variableNames = " + variableNames);

			Collection<StateVariableValue> values = new ArrayList<StateVariableValue>();
			for (String variableName : variableNames)
			{
				variableName = variableName.trim();

				StateVariable stateVariable = getService().getStateVariable(variableName);
				if (stateVariable == null || !stateVariable.getEventDetails().isSendEvents())
				{
					Log.w(TAG, "Ignoring unknown or non-evented state variable: " + variableName);
					continue;
				}

				StateVariableAccessor accessor = getService().getAccessor(stateVariable);
				if (accessor == null)
				{
					Log.w(TAG, "Ignoring evented state variable without accessor: " + variableName);
					continue;
				}
				values.add(accessor.read(stateVariable, getImplementation()));
			}
			return values;
		}
		finally
		{
			unlock();
		}
	}

	@Override
	public Object getImplementation()
	{
		Log.i(TAG, ".getImplementation -> " + _stateService);
		return _stateService;
	}

	@Override
	public PropertyChangeSupport getPropertyChangeSupport()
	{
		lock();
		Log.i(TAG, ".getPropertyChangeSupport");
		try
		{
			if (_propertyChangeSupport == null)
			{
	            // How the implementation instance will tell us about property changes
				_propertyChangeSupport = new PropertyChangeSupport(_stateService);
				_propertyChangeSupport.addPropertyChangeListener(new StatePropertyChangeListener());
			}
			return _propertyChangeSupport;
		}
		finally
		{
			unlock();
		}
	}

	@Override
	public LocalService getService()
	{
		return _stateService;
	}

	protected class StatePropertyChangeListener implements PropertyChangeListener
	{
		@Override
		public void propertyChange(PropertyChangeEvent e)
		{
			Log.i(TAG, "Property change event on local service: " + e.getPropertyName());

			// Prevent recursion
			if (e.getPropertyName().equals(EVENTED_STATE_VARIABLES))
				return;

			String[] variableNames = ModelUtil.fromCommaSeparatedList(e.getPropertyName());
			Log.i(TAG, "Changed variable names: " + Arrays.toString(variableNames));

			try
			{
				Collection<StateVariableValue> currentValues = getCurrentState(variableNames);
				if (!currentValues.isEmpty())
				{
					getPropertyChangeSupport().firePropertyChange(EVENTED_STATE_VARIABLES, null, currentValues);
				}
			}
			catch (Exception ex)
			{
				// TODO: Is it OK to only log this error? It means we keep
				// running although we couldn't send events?
				Log.e(TAG,
				        "Error reading state of service after state variable update event: " + Exceptions.unwrap(ex),
				        ex);
			}
		}
	}
}

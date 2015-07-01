/**
 * Copyright (c) 2007-2015, Intelibo Ltd
 * 
 * Project:     tvbosdk
 * Filename:    FeatureCommand.java
 * Author:      Hari
 * Date:        24 June 2015
 * Description: Feature implementing common command interface
 */

package com.aviq.tv.android.sdk.feature.command;

import java.util.HashMap;
import java.util.Map;

import android.os.Bundle;

import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.command.handlers.CommandSendKey;

/**
 * Feature implementing common command interface
 */
public class FeatureCommand extends FeatureComponent
{
	public static final String TAG = FeatureCommand.class.getSimpleName();
	private Map<String, CommandHandler> _commandHandlers = new HashMap<String, CommandHandler>();

	public FeatureCommand() throws FeatureNotFoundException
	{
		require(FeatureName.Component.RCU);
	}

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		
		// add global command handlers
		CommandSendKey commandSendKey = new CommandSendKey(_feature.Component.RCU);
		addCommandHandler(commandSendKey);
		
		super.initialize(onFeatureInitialized);
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.COMMAND;
	}
	
	/**
	 * Registers new command handler
	 * 
	 * @param commandHandler
	 */
	public void addCommandHandler(CommandHandler commandHandler)
	{
		if (_commandHandlers.get(commandHandler.getId()) != null)
		{
			throw new RuntimeException("Command id " + commandHandler.getId() + " is already defined");
		}
		_commandHandlers.put(commandHandler.getId(), commandHandler);
	}

	/**
	 * Removes existing command handler
	 * 
	 * @param commandHandler
	 */
	public void removeCommandHandler(CommandHandler commandHandler)
	{
		_commandHandlers.remove(commandHandler.getId());
	}

	/**
	 * Executes command handler specified by cmdId
	 * 
	 * @param cmdId
	 *            command handler id
	 * @param params
	 *            Bundle with command params
	 * @param onResultReceived
	 *            callback on command execution finished
	 * @throws CommandNotFoundException
	 */
	public void execute(String cmdId, Bundle params, OnResultReceived onResultReceived)
	{
		CommandHandler commandHandler = _commandHandlers.get(cmdId);
		if (commandHandler == null)
		{
			onResultReceived.onReceiveResult(new CommandNotFoundException(cmdId), null);
		}
		else
		{
			commandHandler.execute(params, onResultReceived);
		}
	}
}

/**
 * Copyright (c) 2007-2015, Intelibo Ltd
 * 
 * Project:     tvbosdk
 * Filename:    CommandSendKey.java
 * Author:      Hari
 * Date:        30.06.2015 �.
 * Description: 
 */

package com.aviq.tv.android.sdk.feature.command.handlers;

import android.app.Instrumentation;
import android.os.Bundle;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Key;
import com.aviq.tv.android.sdk.core.feature.FeatureError;
import com.aviq.tv.android.sdk.core.service.ServiceController.OnResultReceived;
import com.aviq.tv.android.sdk.feature.command.CommandHandler;
import com.aviq.tv.android.sdk.feature.rcu.FeatureRCU;
import com.aviq.tv.android.sdk.feature.rcu.FeatureRCUKeyboard;

/**
 * 
 */



public class CommandSendKey implements CommandHandler
{
	
	public static final String ID = "SEND_KEY";
	
	public static enum Extras
	{
		KEY
	}
	
	private Instrumentation _instrumentation = new Instrumentation();
	private FeatureRCU _featureRCU;
	
	/**
	 * 
	 */
	public CommandSendKey(FeatureRCU featureRCU)
	{
		_featureRCU = featureRCU;
	}
	
	@Override
    public void execute(Bundle params, final OnResultReceived onResultReceived)
    {	
		String keyName = ""; // FIXME:
		keyName = params.getString(Extras.KEY.name());
		Key key = Key.valueOf(keyName);
		FeatureRCUKeyboard featureRCUKeyboard = new FeatureRCUKeyboard();
		final int keyCode = featureRCUKeyboard.getCode(key);
		
		
		// inject key event via Android instrumentation
		// ...	
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
		    	_instrumentation.sendKeyDownUpSync(keyCode);

		    	// call back with success
		    	Environment.getInstance().runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						onResultReceived.onReceiveResult(FeatureError.OK, null);						
					}
				});
			}
		}).start();
    }
	
	@Override
	public String getId()
	{
		return ID;
	}
}

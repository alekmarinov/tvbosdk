package com.aviq.tv.android.sdk.feature.display;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.util.Log;

public class MboxOutputModeManager
{
	public static final String TAG = MboxOutputModeManager.class.getSimpleName();
	
	private Object _mboxOutputManager;
	
	public MboxOutputModeManager(Object mboxOutputManager)
	{
		_mboxOutputManager = mboxOutputManager;
	}
	
	public int autoSwitchHdmiPassthough()
	{
		int res = 0;
		try
		{
			Method autoSwitchHdmiPassthough = _mboxOutputManager.getClass().getMethod("autoSwitchHdmiPassthough",new Class[] {});
			Integer iRes = (Integer) autoSwitchHdmiPassthough.invoke(_mboxOutputManager, new Object[]{});
			res = iRes.intValue();
		}
		catch (NoSuchMethodException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IllegalArgumentException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IllegalAccessException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (InvocationTargetException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		return res;
	}
	
	public void enableDobly_DRC(boolean parseBoolean)
	{
		
		try
		{
			Method enableDobly_DRC = _mboxOutputManager.getClass().getMethod("enableDobly_DRC", boolean.class);
			enableDobly_DRC.invoke(_mboxOutputManager, parseBoolean);
		}
		catch (NoSuchMethodException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IllegalArgumentException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IllegalAccessException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (InvocationTargetException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
	}
	
	public void setDTS_DownmixMode(String mode)
	{
		try
		{
			Method setDTS_DownmixMode = _mboxOutputManager.getClass().getMethod("setDTS_DownmixMode", String.class);
			setDTS_DownmixMode.invoke(_mboxOutputManager, mode);
		}
		catch (NoSuchMethodException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IllegalArgumentException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IllegalAccessException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (InvocationTargetException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		
	}
	
	public void setDoblyMode(String mode)
	{
		try
		{
			Method setDoblyMode = _mboxOutputManager.getClass().getMethod("setDoblyMode", String.class);
			setDoblyMode.invoke(_mboxOutputManager, mode);
		}
		catch (NoSuchMethodException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IllegalArgumentException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IllegalAccessException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (InvocationTargetException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		
		
	}
	
	public void enableDTS_DRC_scale_control(boolean parseBoolean)
	{
		try
		{
			Method enableDTS_DRC_scale_control = _mboxOutputManager.getClass().getMethod("enableDTS_DRC_scale_control", boolean.class);
			enableDTS_DRC_scale_control.invoke(_mboxOutputManager, parseBoolean);
		}
		catch (NoSuchMethodException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IllegalArgumentException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IllegalAccessException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (InvocationTargetException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		
	}
	
	public void enableDTS_Dial_Norm_control(boolean parseBoolean)
	{
		try
		{
			Method enableDTS_Dial_Norm_control = _mboxOutputManager.getClass().getMethod("enableDTS_Dial_Norm_control", boolean.class);
			enableDTS_Dial_Norm_control.invoke(_mboxOutputManager, parseBoolean);
		}
		catch (NoSuchMethodException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IllegalArgumentException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IllegalAccessException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (InvocationTargetException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		
	}

	public String getBestMatchResolution()
    {
		String res = null;
		try
		{
			
			Method getBestMatchResolution = _mboxOutputManager.getClass().getMethod("getBestMatchResolution",new Class[] {});			
			res = (String) getBestMatchResolution.invoke(_mboxOutputManager, new Object[]{});			
		}
		catch (NoSuchMethodException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IllegalArgumentException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IllegalAccessException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (InvocationTargetException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		return res;
    }

	public boolean ifModeIsSetting()
    {
		boolean res =  false;
		try
		{
			Method ifModeIsSetting = _mboxOutputManager.getClass().getMethod("ifModeIsSetting",new Class[] {});
			res = (Boolean) ifModeIsSetting.invoke(_mboxOutputManager, new Object[]{});			
		}
		catch (NoSuchMethodException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IllegalArgumentException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IllegalAccessException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (InvocationTargetException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		return res;
    }

	public boolean isHDMIPlugged()
    {
		boolean res =  false;
		try
		{
			Method isHDMIPlugged = _mboxOutputManager.getClass().getMethod("isHDMIPlugged",new Class[] {});
			res = (Boolean) isHDMIPlugged.invoke(_mboxOutputManager, new Object[]{});			
		}
		catch (NoSuchMethodException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IllegalArgumentException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IllegalAccessException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (InvocationTargetException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		return res;
    }

	public void setDigitalVoiceValue(String string)
    {
	
		try
		{
			Method setDigitalVoiceValue = _mboxOutputManager.getClass().getMethod("setDigitalVoiceValue",String.class);
			setDigitalVoiceValue.invoke(_mboxOutputManager, string);			
		}
		catch (NoSuchMethodException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IllegalArgumentException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IllegalAccessException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (InvocationTargetException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
	
	    
    }

	public void setHdmiUnPlugged()
    {		
		try
		{
			Method setHdmiUnPlugged = _mboxOutputManager.getClass().getMethod("setHdmiUnPlugged",new Class[] {});
			setHdmiUnPlugged.invoke(_mboxOutputManager, new Object[]{});			
		}
		catch (NoSuchMethodException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IllegalArgumentException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IllegalAccessException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (InvocationTargetException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}			    
    }

	public void setHdmiPlugged()
    {
		try
		{
			Method setHdmiPlugged = _mboxOutputManager.getClass().getMethod("setHdmiPlugged",new Class[] {});
			setHdmiPlugged.invoke(_mboxOutputManager, new Object[]{});			
		}
		catch (NoSuchMethodException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IllegalArgumentException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IllegalAccessException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (InvocationTargetException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}			    
    }

	public void setOutputMode(String mode)
    {
		try
		{
			Method setOutputMode = _mboxOutputManager.getClass().getMethod("setOutputMode",String.class);
			setOutputMode.invoke(_mboxOutputManager, mode);			
		}
		catch (NoSuchMethodException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IllegalArgumentException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (IllegalAccessException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
		catch (InvocationTargetException e)
		{
			Log.e(TAG, e.getMessage(), e);
		}			    
    }
}

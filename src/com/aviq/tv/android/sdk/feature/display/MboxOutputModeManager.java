package com.aviq.tv.android.sdk.feature.display;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.util.Log;

public class MboxOutputModeManager
{
	public static final String TAG = MboxOutputModeManager.class.getSimpleName();

	private Object _mboxOutputManager;
	private Method _autoSwitchHdmiPassthough;
	private Method _enableDobly_DRC;
	private Method _setDTS_DownmixMode;
	private Method _setDoblyMode;
	private Method _enableDTS_DRC_scale_control;
	private Method _enableDTS_Dial_Norm_control;
	private Method _getBestMatchResolution;
	private Method _ifModeIsSetting;
	private Method _isHDMIPlugged;
	private Method _setDigitalVoiceValue;
	private Method _setHdmiUnPlugged;
	private Method _setHdmiPlugged;
	private Method _setOutputMode;
	private boolean _isSupported;

	public MboxOutputModeManager(Object mboxOutputManager)
	{
		_mboxOutputManager = mboxOutputManager;

		try
        {
	        _autoSwitchHdmiPassthough = _mboxOutputManager.getClass().getMethod("autoSwitchHdmiPassthough",new Class[] {});
	        _enableDobly_DRC = _mboxOutputManager.getClass().getMethod("enableDobly_DRC", boolean.class);
	        _setDTS_DownmixMode = _mboxOutputManager.getClass().getMethod("setDTS_DownmixMode", String.class);
	        _setDoblyMode = _mboxOutputManager.getClass().getMethod("setDoblyMode", String.class);
	        _enableDTS_DRC_scale_control = _mboxOutputManager.getClass().getMethod("enableDTS_DRC_scale_control", boolean.class);
	        _enableDTS_Dial_Norm_control = _mboxOutputManager.getClass().getMethod("enableDTS_Dial_Norm_control", boolean.class);
	        _getBestMatchResolution = _mboxOutputManager.getClass().getMethod("getBestMatchResolution",new Class[] {});
	        _ifModeIsSetting = _mboxOutputManager.getClass().getMethod("ifModeIsSetting",new Class[] {});
	        _isHDMIPlugged = _mboxOutputManager.getClass().getMethod("isHDMIPlugged",new Class[] {});
	        _setDigitalVoiceValue = _mboxOutputManager.getClass().getMethod("setDigitalVoiceValue",String.class);
	        _setHdmiUnPlugged = _mboxOutputManager.getClass().getMethod("setHdmiUnPlugged",new Class[] {});
	        _setHdmiPlugged = _mboxOutputManager.getClass().getMethod("setHdmiPlugged",new Class[] {});
	        _setOutputMode = _mboxOutputManager.getClass().getMethod("setOutputMode",String.class);
        	_isSupported = true;
        }
        catch (NoSuchMethodException e)
        {
        	Log.w(TAG, e.getMessage(), e);
        }
	}

	public boolean isSupported()
	{
		return _isSupported;
	}

	public int autoSwitchHdmiPassthough()
	{
		int res = 0;
		try
		{
			Integer iRes = (Integer) _autoSwitchHdmiPassthough.invoke(_mboxOutputManager, new Object[]{});
			res = iRes.intValue();
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
			_enableDobly_DRC.invoke(_mboxOutputManager, parseBoolean);
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
			_setDTS_DownmixMode.invoke(_mboxOutputManager, mode);
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
			_setDoblyMode.invoke(_mboxOutputManager, mode);
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
			_enableDTS_DRC_scale_control.invoke(_mboxOutputManager, parseBoolean);
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
			_enableDTS_Dial_Norm_control.invoke(_mboxOutputManager, parseBoolean);
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
			res = (String) _getBestMatchResolution.invoke(_mboxOutputManager, new Object[]{});
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
			res = (Boolean) _ifModeIsSetting.invoke(_mboxOutputManager, new Object[]{});
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
			res = (Boolean) _isHDMIPlugged.invoke(_mboxOutputManager, new Object[]{});
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
			_setDigitalVoiceValue.invoke(_mboxOutputManager, string);
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
			_setHdmiUnPlugged.invoke(_mboxOutputManager, new Object[]{});
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
			_setHdmiPlugged.invoke(_mboxOutputManager, new Object[]{});
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
			_setOutputMode.invoke(_mboxOutputManager, mode);
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

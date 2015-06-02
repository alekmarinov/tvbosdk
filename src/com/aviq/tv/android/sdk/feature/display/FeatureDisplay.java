/**
 * Copyright (c) 2007-2015, Intelibo Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureDisplay.java
 * Author:      alek
 * Date:        4 May 2015
 * Description: Provides access to display settings
 */

package com.aviq.tv.android.sdk.feature.display;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.core.feature.FeatureNotFoundException;
import com.aviq.tv.android.sdk.feature.system.SystemProperties;
import com.aviq.tv.android.sdk.utils.Files;

/**
 * Provides access to display settings
 */
public class FeatureDisplay extends FeatureComponent
{
	private static final String TAG = FeatureDisplay.class.getSimpleName();
	private final String MBOX_OUTPUTMODE_SERVICE = "mbox_outputmode_service";

	private final static String DISPLAY_MODE_SYSFS = "/sys/class/display/mode";
	private static final String[] ALL_HDMI_MODE_VALUE_LIST =
	{ "1080p", "1080p50hz", "1080p24hz", "720p", "720p50hz", "1080i", "1080i50hz" };
	// "4k2k24hz", "4k2k25hz", "4k2k30hz", "4k2ksmpte"

	private MboxOutputModeManager _mboxOutputModeManager;

	public FeatureDisplay() throws FeatureNotFoundException
	{
	}

	@Override
	public void initialize(final OnFeatureInitialized onFeatureInitialized)
	{
		Log.i(TAG, ".initialize");
		List<VideoMode> videoModes = getVideoModes();
		int i = 0;
		for (VideoMode videoMode : videoModes)
		{
			Log.i(TAG, i + ". " + videoMode);
			i++;
		}

		_mboxOutputModeManager = new MboxOutputModeManager(Environment.getInstance().getApplicationContext()
		        .getSystemService(MBOX_OUTPUTMODE_SERVICE));

		super.initialize(onFeatureInitialized);
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.DISPLAY;
	}

	public List<VideoMode> getVideoModes()
	{
		List<VideoMode> videoModes = new ArrayList<VideoMode>();
		for (String modeId : ALL_HDMI_MODE_VALUE_LIST)
		{
			VideoMode videoMode = parseVideoModeId(modeId);
			if (videoMode != null)
				videoModes.add(videoMode);
		}
		return videoModes;
	}

	private VideoMode parseVideoModeId(String modeId)
	{
		try
		{
			VideoMode videoMode = new VideoMode();
			videoMode.modeId = modeId;
			if (modeId.startsWith("4k2k"))
			{
				videoMode.width = 4096;
				videoMode.height = 2160;
				// 4k video mode is progressive
				videoMode.isProgressive = true;
				modeId = modeId.substring(4);
				if (modeId.startsWith("smpte"))
				{
					videoMode.isSmpte = true;
				}
				else
				{
					StringBuffer hzStr = new StringBuffer();
					for (int c = 0; c < modeId.length() && Character.isDigit(modeId.charAt(c)); c++)
						hzStr.append(modeId.charAt(c));
					videoMode.hz = Integer.valueOf(hzStr.toString());
				}
			}
			else
			{
				StringBuffer heightStr = new StringBuffer();
				for (int c = 0; c < modeId.length() && Character.isDigit(modeId.charAt(c)); c++)
					heightStr.append(modeId.charAt(c));
				videoMode.height = Integer.valueOf(heightStr.toString());
				switch (videoMode.height)
				{
					case 1080:
						videoMode.width = 1920;
					break;
					case 720:
						videoMode.width = 1280;
					break;
					case 576:
						videoMode.width = 720;
					break;
					case 480:
						videoMode.width = 640;
					break;
				}
				modeId = modeId.substring(heightStr.length());
				videoMode.isProgressive = modeId.charAt(0) == 'p';
				modeId = modeId.substring(1);
				if (modeId.length() > 0)
				{
					StringBuffer hzStr = new StringBuffer();
					for (int c = 0; c < modeId.length() && Character.isDigit(modeId.charAt(c)); c++)
						hzStr.append(modeId.charAt(c));
					videoMode.hz = Integer.valueOf(hzStr.toString());
				}
				else
				{
					videoMode.hz = 60;
				}
			}
			return videoMode;
		}
		catch (Exception e)
		{
			Log.w(TAG, e.getMessage(), e);
			return null;
		}
	}

	public void setVideoMode(VideoMode videoMode)
	{
		_mboxOutputModeManager.setOutputMode(videoMode.modeId);
	}

	public VideoMode getVideoMode()
	{
		try
		{
			String modeId = Files.loadToString(DISPLAY_MODE_SYSFS);
			return parseVideoModeId(modeId.trim());
		}
		catch (IOException e)
		{
			Log.e(TAG, e.getMessage(), e);
			return null;
		}
	}

	public VideoMode getBestVideoMode()
	{
		return parseVideoModeId(_mboxOutputModeManager.getBestMatchResolution());
	}

	private void saveFile(String text, String fileName) throws IOException
	{
		Log.i(TAG, "save `" + text + "' -> " + fileName);
        Files.saveToFile(text, fileName);
	}

	private void setProp(String name, String value)
	{
		Log.i(TAG, "setprop " + name + "=" + value);
		SystemProperties.set(name, value);
	}

	public void setScreenPosition(ScreenPosition screenPosition)
	{
		int right = screenPosition.x + screenPosition.w - 1;
		int bottom = screenPosition.y + screenPosition.h - 1;

		try
        {
			saveFile(String.format("%d %d %d %d", screenPosition.x, screenPosition.y, right, bottom), "/sys/class/graphics/fb0/window_axis");
			saveFile("0x10001", "/sys/class/graphics/fb0/free_scale");
        }
        catch (IOException e)
        {
        	Log.e(TAG, e.getMessage(), e);
        }
	}

	public void saveScreenPosition(ScreenPosition screenPosition)
	{
		VideoMode videoMode = getVideoMode();

		setProp(String.format("ubootenv.var.%soutputx", videoMode.modeId), String.valueOf(screenPosition.x));
		setProp(String.format("ubootenv.var.%soutputy", videoMode.modeId), String.valueOf(screenPosition.y));
		setProp(String.format("ubootenv.var.%soutputwidth", videoMode.modeId), String.valueOf(screenPosition.w));
		setProp(String.format("ubootenv.var.%soutputheight", videoMode.modeId), String.valueOf(screenPosition.h));

		int right = screenPosition.x + screenPosition.w - 1;
		int bottom = screenPosition.y + screenPosition.h - 1;

		try
        {
			saveFile(String.format("%d %d %d %d %d %d 18 18", screenPosition.x, screenPosition.y, screenPosition.w, screenPosition.h, screenPosition.x, screenPosition.y), "/sys/class/display/axis");
			saveFile(String.format("%d %d %d %d", screenPosition.x, screenPosition.y, right, bottom), "/sys/class/video/axis");
        }
        catch (IOException e)
        {
        	Log.e(TAG, e.getMessage(), e);
        }
	}

	public ScreenPosition getScreenPosition()
	{
		ScreenPosition screenPosition = new ScreenPosition();
       	VideoMode videoMode = getVideoMode();
    	screenPosition.x = screenPosition.y = 0;
    	screenPosition.w = videoMode.width;
    	screenPosition.h = videoMode.height;
		try
        {
	        String displayAxis = Files.loadToString("/sys/class/display/axis");
	        String[] parts = displayAxis.split(" ");
	        if (parts.length >= 4)
	        {
	        	screenPosition.x = Integer.valueOf(parts[0]);
	        	screenPosition.y = Integer.valueOf(parts[1]);
	        	screenPosition.w = Integer.valueOf(parts[2]);
	        	screenPosition.h = Integer.valueOf(parts[3]);
	        }
        }
        catch (Exception e)
        {
        	Log.w(TAG, e.getMessage(), e);
        }
		return screenPosition;
	}

	public static class ScreenPosition
	{
		public int x;
		public int y;
		public int w;
		public int h;

		@Override
		public String toString()
		{
			return String.format("%d %d %d %d", x, y, w, h);
		}
	}

	public static class VideoMode
	{
		public int width;
		public int height;
		String modeId;
		boolean isProgressive;
		int hz;
		boolean isSmpte;

		@Override
		public String toString()
		{
			return String.format("%s%s-%s", width == 4096 ? "4k" : (width + "x"), height == 2160 ? "2k"
			        : (height + (isProgressive ? "p" : "i")), isSmpte ? "smpte" : (hz + "Hz"));
		}
	}
}

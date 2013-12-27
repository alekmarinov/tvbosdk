/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    Loader.java
 * Author:      alek
 * Date:        16 Jul 2013
 * Description: RayV Streaming Agent loader
 */

package com.rayv.StreamingAgent;

import android.util.Log;


public class Loader
{
	public static final String TAG = Loader.class.getSimpleName();
	public static native void startStreamer(String streamerIni);

	static
	{
		try
		{
			System.loadLibrary("crystax_shared");
			System.loadLibrary("gnustl_shared");
			System.loadLibrary("glib");
			System.loadLibrary("wx");
			System.loadLibrary("avutil");
			System.loadLibrary("avcodec");
			System.loadLibrary("avformat");
			System.loadLibrary("playercore");
			System.loadLibrary("streaming_agent");
		}
		catch (UnsatisfiedLinkError e)
		{
			Log.e(TAG, e.getMessage(), e);
		}
	}
}

/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeatureStreamer.java
 * Author:      alek
 * Date:        23 Apr 2014
 * Description: Base of Streamer component feature
 */

package com.aviq.tv.android.sdk.feature.player;

import com.aviq.tv.android.sdk.core.Log;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;

/**
 * Base of Streamer component feature
 */
public class FeatureStreamer extends FeatureComponent
{
	private static final String TAG = FeatureStreamer.class.getSimpleName();

	public interface OnStreamURLReceived
	{
		void onStreamURL(String streamUrl);
	}

	/**
	 * Default implementation returning streamerId as url.
	 * This method can be overridden in order to provide url depending on local
	 * streaming service.
	 *
	 * @param streamId
	 * @param playTimeDelta
	 *            offset in seconds from real time, > 0 in the past, < 0 in the
	 *            future
	 * @param onStreamURLReceived
	 *            callback invoked with corresponding stream url to specified
	 *            streamId
	 */
	public void getUrlByStreamId(String streamId, long playTimeDelta, OnStreamURLReceived onStreamURLReceived)
	{
		if (playTimeDelta > 0)
		{
			Log.w(TAG, "Can't handle positive playTimeDelta = " + playTimeDelta + " for streamId = `" + streamId + "'");
		}
		onStreamURLReceived.onStreamURL(streamId);
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.STREAMER;
	}
}

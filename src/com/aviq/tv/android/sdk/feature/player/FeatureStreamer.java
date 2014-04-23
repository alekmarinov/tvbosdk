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

import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;

/**
 * Base of Streamer component feature
 */
public class FeatureStreamer extends FeatureComponent
{
	/**
	 * Default implementation returning streamerId as url.
	 * This method can be overridden in order to provide url depending on local
	 * streaming service.
	 *
	 * @param streamId
	 * @return stream url
	 */
	public String getUrlByStreamId(String streamId)
	{
		return streamId;
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.STREAMER;
	}
}

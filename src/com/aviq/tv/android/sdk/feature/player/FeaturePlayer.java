/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    FeaturePlayer.java
 * Author:      alek
 * Date:        1 Dec 2013
 * Description: Component feature providing player
 */

package com.aviq.tv.android.sdk.feature.player;

import android.widget.VideoView;

import com.aviq.tv.android.sdk.core.ResultCode;
import com.aviq.tv.android.sdk.core.feature.FeatureComponent;
import com.aviq.tv.android.sdk.core.feature.FeatureName;
import com.aviq.tv.android.sdk.core.feature.FeatureName.Component;
import com.aviq.tv.android.sdk.player.AndroidPlayer;
import com.aviq.tv.android.sdk.player.IPlayer;

/**
 * Component feature providing player
 */
public class FeaturePlayer extends FeatureComponent
{
	public static final String TAG = FeaturePlayer.class.getSimpleName();
	protected AndroidPlayer _player;
	private VideoView _videoView;

	@Override
	public void initialize(OnFeatureInitialized onFeatureInitialized)
	{
		if (_videoView == null)
			throw new RuntimeException("Set VideoView first via method setVideoView");
		_player = new AndroidPlayer(_videoView);
		onFeatureInitialized.onInitialized(this, ResultCode.OK);
	}

	@Override
	public Component getComponentName()
	{
		return FeatureName.Component.PLAYER;
	}

	public IPlayer getPlayer()
	{
		return _player;
	}

	public void setVideoView(VideoView videoView)
	{
		_videoView = videoView;
	}
}

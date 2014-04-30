package com.aviq.tv.android.sdk.feature.player;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Key;
import com.aviq.tv.android.sdk.core.Log;

public class AviqMediaController extends MediaController
{
	private static final String TAG = AviqMediaController.class.getSimpleName();

	public AviqMediaController(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public AviqMediaController(Context context, boolean useFastForward)
	{
		super(context);
	}

	public AviqMediaController(Context context)
	{
		super(context);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event)
	{
		int keyCode = event.getKeyCode();
		Key key = Environment.getInstance().translateKeyCode(keyCode);

		Log.e(TAG, ".dispatchKeyEvent: keyCode = " + keyCode);

		ViewGroup rootLayout = (ViewGroup) getChildAt(0);
		ViewGroup buttonContainer = (ViewGroup) rootLayout.getChildAt(0);
		ViewGroup progressContainer = (ViewGroup) rootLayout.getChildAt(1);
		View seekBar = progressContainer.getChildAt(1);

		if (buttonContainer.hasFocus())
			seekBar.requestFocus();

		switch (key)
		{
			case PLAY_BACKWARD:
				event = new KeyEvent(event.getDownTime(), event.getEventTime(), event.getAction(),
				        KeyEvent.KEYCODE_DPAD_LEFT, event.getRepeatCount(), event.getMetaState(), event.getDeviceId(),
				        event.getScanCode(), event.getFlags(), event.getSource());
				break;

			case PLAY_FORWARD:
				event = new KeyEvent(event.getDownTime(), event.getEventTime(), event.getAction(),
				        KeyEvent.KEYCODE_DPAD_RIGHT, event.getRepeatCount(), event.getMetaState(), event.getDeviceId(),
				        event.getScanCode(), event.getFlags(), event.getSource());
				break;

			default:
				break;
		}

		return super.dispatchKeyEvent(event);
	}
}

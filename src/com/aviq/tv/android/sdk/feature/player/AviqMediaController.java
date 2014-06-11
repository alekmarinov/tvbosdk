package com.aviq.tv.android.sdk.feature.player;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.MediaController;

import com.aviq.tv.android.sdk.core.Environment;
import com.aviq.tv.android.sdk.core.Key;
import com.aviq.tv.android.sdk.core.Log;

public class AviqMediaController extends MediaController
{
	private static final String TAG = AviqMediaController.class.getSimpleName();

	private MediaController.MediaPlayerControl _player;
	private int _seekSmallStepMillis;
	private int _seekLargeStepMillis;

	public AviqMediaController(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init();
	}

	public AviqMediaController(Context context, boolean useFastForward)
	{
		super(context);
		init();
	}

	public AviqMediaController(Context context)
	{
		super(context);
		init();
	}

	public void init()
	{
		post(new Runnable()
		{
			@Override
            public void run()
            {
				ViewGroup rootLayout = (ViewGroup) getChildAt(0);
				ViewGroup buttonContainer = (ViewGroup) rootLayout.getChildAt(0);

				// Rewind button
				ImageButton rewButton = (ImageButton) buttonContainer.getChildAt(1);
				rewButton.setOnClickListener(new OnClickListener()
				{
					@Override
		            public void onClick(View v)
		            {
						_player.seekTo(_player.getCurrentPosition() - _seekLargeStepMillis);
		            }
				});

				// Fast forward button
				ImageButton ffwdButton = (ImageButton) buttonContainer.getChildAt(3);
				ffwdButton.setOnClickListener(new OnClickListener()
				{
					@Override
		            public void onClick(View v)
		            {
						_player.seekTo(_player.getCurrentPosition() + _seekLargeStepMillis);
		            }
				});
            }
		});
	}

	public void setSeekStepMillis(int smallStep, int largeStep)
	{
		_seekSmallStepMillis = smallStep;
		_seekLargeStepMillis = largeStep;
	}

	@Override
	public void setMediaPlayer (MediaController.MediaPlayerControl player)
	{
		_player = player;
		super.setMediaPlayer(player);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event)
	{
		int keyCode = event.getKeyCode();
		Key key = Environment.getInstance().translateKeyCode(keyCode);

		Log.e(TAG, ".dispatchKeyEvent: keyCode = " + keyCode);

		// This used to not give focus to the MediaController widget
		ViewGroup rootLayout = (ViewGroup) getChildAt(0);
		ViewGroup buttonContainer = (ViewGroup) rootLayout.getChildAt(0);
		//ViewGroup progressContainer = (ViewGroup) rootLayout.getChildAt(1);
		//View seekBar = progressContainer.getChildAt(1);

		//if (buttonContainer.hasFocus())
		//	seekBar.requestFocus();

		switch (key)
		{
			case LEFT:
				if (buttonContainer.hasFocus())
					break;

				if (event.getAction() == KeyEvent.ACTION_DOWN)
				{
					_player.seekTo(_player.getCurrentPosition() - _seekSmallStepMillis);
					return true;
				}
				break;

			case RIGHT:
				if (buttonContainer.hasFocus())
					break;

				if (event.getAction() == KeyEvent.ACTION_DOWN)
				{
					_player.seekTo(_player.getCurrentPosition() + _seekSmallStepMillis);
					return true;
				}
				break;

			case PLAY_BACKWARD:

				if (event.getAction() == KeyEvent.ACTION_DOWN)
				{
					_player.seekTo(_player.getCurrentPosition() - _seekSmallStepMillis);
					return true;
				}

				// Default handling by super class
				//event = new KeyEvent(event.getDownTime(), event.getEventTime(), event.getAction(),
				//        KeyEvent.KEYCODE_DPAD_LEFT, event.getRepeatCount(), event.getMetaState(), event.getDeviceId(),
				//        event.getScanCode(), event.getFlags(), event.getSource());
				break;

			case PLAY_FORWARD:

				if (event.getAction() == KeyEvent.ACTION_DOWN)
				{
					_player.seekTo(_player.getCurrentPosition() + _seekSmallStepMillis);
					return true;
				}

				// Default handling by parent class
				//event = new KeyEvent(event.getDownTime(), event.getEventTime(), event.getAction(),
				//        KeyEvent.KEYCODE_DPAD_RIGHT, event.getRepeatCount(), event.getMetaState(), event.getDeviceId(),
				//        event.getScanCode(), event.getFlags(), event.getSource());
				break;

			default:
				break;
		}

		return super.dispatchKeyEvent(event);
	}
}

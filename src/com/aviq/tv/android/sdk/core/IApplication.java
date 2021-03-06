/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    IApplication.java
 * Author:      alek
 * Date:        4 Dec 2013
 * Description: Defines main application interface
 */

package com.aviq.tv.android.sdk.core;

import android.app.Activity;

/**
 * Defines main application interface
 */
public interface IApplication
{
	/**
	 * Invoked on activity create
	 */
	void onActivityCreate(Activity activity);

	/**
	 * Invoked on activity destroy
	 */
	void onActivityDestroy();

	/**
	 * Invoked on activity resume
	 */
	void onActivityResume();

	/**
	 * Invoked on activity pause
	 */
	void onActivityPause();
}

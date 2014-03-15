/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    EventReceiver.java
 * Author:      alek
 * Date:        15 Mar 2014
 * Description: Events receiver interface
 */

package com.aviq.tv.android.sdk.core;

import android.os.Bundle;

/**
 * Events receiver interface
 */
public interface EventReceiver
{
	/**
	 * Message receiver method
	 *
	 * @param msgId
	 *            the id of the received message
	 * @param bundle
	 *            additional message data
	 */
	void onEvent(int msgId, Bundle bundle);
}

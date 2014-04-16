/**
 * Copyright (c) 2007-2013, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    IStateMenuItem.java
 * Author:      alek
 * Date:        18 Dec 2013
 * Description: Interface defining state supposed to integrate in the main application menu
 *
 */

package com.aviq.tv.android.sdk.core.state;


/**
 * Interface defining state supposed to integrate in the settings state
 */
public interface IStateSettingsItem extends IStateMenuItem
{
	/**
	 * @return the text describing current setting
	 */
	String getSettingsDescription();
}

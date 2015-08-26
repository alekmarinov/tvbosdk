/**
 * Copyright (c) 2007-2015, Intelibo Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    Name.java
 * Author:      alek
 * Date:        25 Jun 2015
 * Description: Static SDK name
 */

package com.aviq.tv.android.sdk;

import java.util.Calendar;

/**
 * Static SDK name
 */
public class Name
{
	public static final String COMPANY = "Intelibo";
	public static final String COPYRIGHT = String.format("Copyright (c) 2007-%d, %s", Calendar.getInstance().get(Calendar.YEAR), COMPANY);
	public static final String SDK = "tvbosdk";
}

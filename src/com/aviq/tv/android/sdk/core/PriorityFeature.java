/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    PriorityFeature.java
 * Author:      alek
 * Date:        8 Apr 2014
 * Description:
 */

package com.aviq.tv.android.sdk.core;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Attach to feature class to mark it as prioritized in initialization order
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface PriorityFeature
{
}

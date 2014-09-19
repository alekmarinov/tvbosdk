/**
 * Copyright (c) 2007-2014, AVIQ Bulgaria Ltd
 *
 * Project:     AVIQTVSDK
 * Filename:    PriorityFeature.java
 * Author:      alek
 * Date:        8 Apr 2014
 * Description:
 */

package com.aviq.tv.android.sdk.core.feature.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Attach to feature class to mark his author
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Author
{
	 String value();
}

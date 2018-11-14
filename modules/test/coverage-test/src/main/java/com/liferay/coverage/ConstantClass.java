/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.coverage;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

/**
 * @author Xiangyue Cai
 */
public class ConstantClass {

	public String toString(String testVariable) {
		return _TEST + _TEST_VARIABLE + _log.toString() + testVariable;
	}

	private static final String _TEST = new String("TEST");

	private static final String _TEST_VARIABLE = "TEST_VARIABLE";

	private static final Log _log = LogFactoryUtil.getLog(ConstantClass.class);

}
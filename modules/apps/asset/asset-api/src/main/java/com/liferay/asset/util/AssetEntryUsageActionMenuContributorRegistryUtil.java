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

package com.liferay.asset.util;

import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMap;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMapFactory;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * @author Pavel Savinov
 */
public class AssetEntryUsageActionMenuContributorRegistryUtil {

	public static AssetEntryUsageActionMenuContributor
		getAssetEntryUsageActionMenuContributor(String className) {

		return _instance._getAssetEntryUsageActionMenuContributor(className);
	}

	private AssetEntryUsageActionMenuContributorRegistryUtil() {
		Bundle bundle = FrameworkUtil.getBundle(
			AssetEntryUsageActionMenuContributorRegistryUtil.class);

		BundleContext bundleContext = bundle.getBundleContext();

		_serviceTrackerMap = ServiceTrackerMapFactory.openSingleValueMap(
			bundleContext, AssetEntryUsageActionMenuContributor.class,
			"model.class.name");
	}

	private AssetEntryUsageActionMenuContributor
		_getAssetEntryUsageActionMenuContributor(String className) {

		return _serviceTrackerMap.getService(className);
	}

	private static final AssetEntryUsageActionMenuContributorRegistryUtil
		_instance = new AssetEntryUsageActionMenuContributorRegistryUtil();

	private final
		ServiceTrackerMap<String, AssetEntryUsageActionMenuContributor>
			_serviceTrackerMap;

}
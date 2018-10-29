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

package com.liferay.portal.cache.ehcache.internal.configurator;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.PropsKeys;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import java.util.Properties;

/**
 * @author Leon Chi
 */
public class PropsInvocationHandler implements InvocationHandler {

	public PropsInvocationHandler(boolean clusterEnabled) {
		this(clusterEnabled, false, true, true);
	}

	public PropsInvocationHandler(
		boolean clusterEnabled, boolean bootstrapLoaderEnabled,
		boolean bootstrapLoaderPropertiesIsEmpty,
		boolean replicatorPropertiesIsEmpty) {

		_clusterEnabled = clusterEnabled;
		_bootstrapLoaderEnabled = bootstrapLoaderEnabled;
		_bootstrapLoaderPropertiesIsEmpty = bootstrapLoaderPropertiesIsEmpty;
		_replicatorPropertiesIsEmpty = replicatorPropertiesIsEmpty;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) {
		String methodName = method.getName();

		if ("get".equals(methodName)) {
			if (PropsKeys.EHCACHE_RMI_PEER_LISTENER_FACTORY_CLASS.equals(
					args[0])) {

				return "com.liferay.portal.cache.ehcache.internal.rmi." +
					"TestLiferayRMICacheManagerPeerListenerFactory";
			}

			if (PropsKeys.EHCACHE_RMI_PEER_PROVIDER_FACTORY_CLASS.equals(
					args[0])) {

				return "net.sf.ehcache.distribution." +
					"TestRMICacheManagerPeerProviderFactory";
			}

			if (PropsKeys.EHCACHE_BOOTSTRAP_CACHE_LOADER_ENABLED.equals(
					args[0])) {

				return String.valueOf(_bootstrapLoaderEnabled);
			}

			if (PropsKeys.CLUSTER_LINK_ENABLED.equals(args[0])) {
				return String.valueOf(_clusterEnabled);
			}
		}

		if ("getArray".equals(methodName)) {
			if ("portal.property.Key1".equals(args[0])) {
				return new String[0];
			}

			if ("portal.property.Key2".equals(args[0])) {
				return new String[] {"key=value"};
			}

			return new String[] {"key1=value1", "key2=value2"};
		}

		if ("getProperties".equals(methodName) && (args.length > 0)) {
			Properties properties = new Properties();

			if (args[0].equals(
					PropsKeys.EHCACHE_BOOTSTRAP_CACHE_LOADER_PROPERTIES +
						StringPool.PERIOD) &&
				!_bootstrapLoaderPropertiesIsEmpty) {

				properties.put("portalCacheName1", "key1=value1");
				properties.put("portalCacheName2X", "key2X=value2X");
			}

			if (args[0].equals(
					PropsKeys.EHCACHE_REPLICATOR_PROPERTIES +
						StringPool.PERIOD) &&
				!_replicatorPropertiesIsEmpty) {

				properties.put("portalCacheName1", "key1=value1");
				properties.put("portalCacheName2Y", "key2Y=value2Y");
			}

			return properties;
		}

		return null;
	}

	private final boolean _bootstrapLoaderEnabled;
	private final boolean _bootstrapLoaderPropertiesIsEmpty;
	private final boolean _clusterEnabled;
	private final boolean _replicatorPropertiesIsEmpty;

}
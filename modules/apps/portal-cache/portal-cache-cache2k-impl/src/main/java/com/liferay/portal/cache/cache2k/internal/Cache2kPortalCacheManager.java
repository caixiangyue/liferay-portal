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

package com.liferay.portal.cache.cache2k.internal;

import com.liferay.portal.cache.BasePortalCacheManager;
import com.liferay.portal.cache.configuration.PortalCacheConfiguration;
import com.liferay.portal.cache.configuration.PortalCacheManagerConfiguration;
import com.liferay.portal.kernel.cache.PortalCache;
import com.liferay.portal.kernel.util.Props;

import java.io.Serializable;

import java.net.URL;

import java.util.Collections;
import java.util.Iterator;
import java.util.Properties;

import javax.management.MBeanServer;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.cache2k.CacheManager;
import org.cache2k.spi.Cache2kCoreProvider;
import org.cache2k.spi.SingleProviderResolver;

/**
 * @author Xiangyue Cai
 */
public class Cache2kPortalCacheManager<K extends Serializable, V>
	extends BasePortalCacheManager<K, V> {

	@Override
	public void reconfigurePortalCaches(URL configurationURL) {
	}

	protected Cache<K, V> createCache2kCache(String portalCacheName) {
		Cache2kBuilder<K, V> cache2kBuilder = new Cache2kBuilder<K, V>() {};

		return cache2kBuilder.manager(
			CacheManager.getInstance(_cacheManager.getName())
		).name(
			portalCacheName
		).build();
	}

	@Override
	protected PortalCache<K, V> createPortalCache(
		PortalCacheConfiguration portalCacheConfiguration) {

		String portalCacheName = portalCacheConfiguration.getPortalCacheName();

		Cache<K, V> cache = null;

		synchronized (_cacheManager) {
			cache = _cacheManager.getCache(portalCacheName);

			if (cache == null) {
				cache = createCache2kCache(portalCacheName);
			}
		}

		return new Cache2kPortalCache<>(this, cache);
	}

	@Override
	protected void doClearAll() {
		if (_cacheManager != null) {
			_cacheManager.clear();
		}
	}

	@Override
	protected void doDestroy() {
		_cacheManager.close();
		_provider.close();
	}

	@Override
	protected void doRemovePortalCache(String portalCacheName) {
		Iterable<Cache> actionCaches = _cacheManager.getActiveCaches();

		Iterator<Cache> itr = actionCaches.iterator();

		while (itr.hasNext()) {
			Cache cache = itr.next();

			if (portalCacheName.equals(cache.getName())) {
				itr.remove();

				break;
			}
		}
	}

	@Override
	protected PortalCacheManagerConfiguration
		getPortalCacheManagerConfiguration() {

		return _portalCacheManagerConfiguration;
	}

	@Override
	protected void initPortalCacheManager() {
		_provider = SingleProviderResolver.resolveMandatory(
			Cache2kCoreProvider.class);

		_cacheManager = _provider.getManager(
			_provider.getDefaultClassLoader(), getPortalCacheManagerName());

		_portalCacheManagerConfiguration = new PortalCacheManagerConfiguration(
			null,
			new PortalCacheConfiguration(
				null, Collections.emptySet(), new Properties()),
			null);
	}

	@Override
	protected void removeConfigurableEhcachePortalCacheListeners(
		PortalCache<K, V> portalCache) {
	}

	protected MBeanServer mBeanServer;
	protected volatile Props props;

	private CacheManager _cacheManager;
	private PortalCacheManagerConfiguration _portalCacheManagerConfiguration;
	private Cache2kCoreProvider _provider;

}
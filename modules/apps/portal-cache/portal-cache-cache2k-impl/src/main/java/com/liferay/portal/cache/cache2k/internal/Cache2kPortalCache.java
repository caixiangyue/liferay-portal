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

import com.liferay.portal.cache.BasePortalCache;
import com.liferay.portal.kernel.cache.PortalCacheManager;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.List;

import org.cache2k.Cache;

/**
 * @author Xiangyue Cai
 */
public class Cache2kPortalCache<K extends Serializable, V>
	extends BasePortalCache<K, V> {

	public Cache2kPortalCache(
		PortalCacheManager<K, V> portalCacheManager, Cache cache) {

		super(portalCacheManager);

		this.cache = cache;
	}

	@Override
	public List<K> getKeys() {
		List<K> keys = new ArrayList<>();

		for (K key : cache.keys()) {
			keys.add(key);
		}

		return keys;
	}

	@Override
	public String getPortalCacheName() {
		return cache.getName();
	}

	@Override
	public void removeAll() {
		cache.clear();
	}

	@Override
	protected V doGet(K key) {
		return cache.get(key);
	}

	@Override
	protected void doPut(K key, V value, int timeToLive) {
		cache.put(key, value);
	}

	@Override
	protected V doPutIfAbsent(K key, V value, int timeToLive) {

		// TODO: check Cache2k JavaDoc for computeIfAbsent return value (V)

		// return cache.computeIfAbsent(key, () -> value);

		// TODO: check Cache2k JavaDoc for putIfAbsent return value (boolean)

		V oldValue = cache.get(key);

		boolean put = cache.putIfAbsent(key, value);

		if (put) {
			return null;
		}

		return oldValue;
	}

	@Override
	protected void doRemove(K key) {
		cache.remove(key);
	}

	@Override
	protected boolean doRemove(K key, V value) {

		// TODO: check Cache2k JavaDoc;
		// doRemove(K, V) may mean remove only when both key and value equals

		return cache.removeIfEquals(key, value);
	}

	@Override
	protected V doReplace(K key, V value, int timeToLive) {

		// TODO: check Cache2k JavaDoc for peekAndReplace return value

		return cache.peekAndReplace(key, value);
	}

	@Override
	protected boolean doReplace(K key, V oldValue, V newValue, int timeToLive) {

		// TODO: check Cache2k JavaDoc for replace return value (boolean)

		if (oldValue.equals(cache.get(key))) {
			return cache.replace(key, newValue);
		}

		return false;
	}

	protected volatile Cache<K, V> cache;

}
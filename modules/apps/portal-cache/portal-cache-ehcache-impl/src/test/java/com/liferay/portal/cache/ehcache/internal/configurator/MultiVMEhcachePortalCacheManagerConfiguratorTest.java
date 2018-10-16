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

import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.cache.PortalCacheReplicator;
import com.liferay.portal.cache.configuration.PortalCacheConfiguration;
import com.liferay.portal.cache.configuration.PortalCacheManagerConfiguration;
import com.liferay.portal.kernel.io.unsync.UnsyncStringReader;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.rule.CodeCoverageAssertor;
import com.liferay.portal.kernel.util.ObjectValuePair;
import com.liferay.portal.kernel.util.Props;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.ProxyUtil;
import com.liferay.portal.kernel.util.StringUtil;

import java.io.IOException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author Leon Chi
 */
public class MultiVMEhcachePortalCacheManagerConfiguratorTest {

	@ClassRule
	public static final CodeCoverageAssertor codeCoverageAssertor =
		CodeCoverageAssertor.INSTANCE;

	@Test
	public void testActivate() {
		MultiVMEhcachePortalCacheManagerConfigurator
			multiVMEhcachePortalCacheManagerConfigurator =
				_getMultiVMEhcachePortalCacheManagerConfigurator(
					true, true, true, true);

		Assert.assertTrue(
			"The _bootstrapLoaderEnabled should be true if props.get(" +
				"PropsKeys.EHCACHE_BOOTSTRAP_CACHE_LOADER_ENABLED) return true",
			(Boolean)ReflectionTestUtil.getFieldValue(
				multiVMEhcachePortalCacheManagerConfigurator,
				"_bootstrapLoaderEnabled"));
		Assert.assertTrue(
			"The clusterEnabled should be true if props.get(PropsKeys." +
				"CLUSTER_LINK_ENABLED) return true",
			(Boolean)ReflectionTestUtil.getFieldValue(
				multiVMEhcachePortalCacheManagerConfigurator,
				"clusterEnabled"));
		Assert.assertEquals(
			new Properties(),
			ReflectionTestUtil.getFieldValue(
				multiVMEhcachePortalCacheManagerConfigurator,
				"_bootstrapLoaderProperties"));
		Assert.assertEquals(
			"key1=value1,key2=value2",
			ReflectionTestUtil.getFieldValue(
				multiVMEhcachePortalCacheManagerConfigurator,
				"_defaultBootstrapLoaderPropertiesString"));
		Assert.assertEquals(
			"key1=value1,key2=value2",
			ReflectionTestUtil.getFieldValue(
				multiVMEhcachePortalCacheManagerConfigurator,
				"_defaultReplicatorPropertiesString"));
		Assert.assertEquals(
			new Properties(),
			ReflectionTestUtil.getFieldValue(
				multiVMEhcachePortalCacheManagerConfigurator,
				"_replicatorProperties"));
	}

	@Test
	public void testGetMergedPropertiesMap() {

		// Test 1: _bootstrapLoaderEnabled is true, _bootstrapLoaderProperties
		// and _replicatorProperties are empty

		Assert.assertEquals(
			Collections.emptyMap(),
			ReflectionTestUtil.invoke(
				_getMultiVMEhcachePortalCacheManagerConfigurator(
					true, true, true, true),
				"_getMergedPropertiesMap", null, null));

		// Test 2: _bootstrapLoaderEnabled is true, _bootstrapLoaderProperties
		// and _replicatorProperties are non-empty

		Map<String, ObjectValuePair<Properties, Properties>>
			mergedPropertiesMap1 = ReflectionTestUtil.invoke(
				_getMultiVMEhcachePortalCacheManagerConfigurator(
					true, true, false, false),
				"_getMergedPropertiesMap", null, null);

		Properties expectedProperties1 = _getProperties(
			"portalCacheName1=key1=value1,portalCacheName2X=key2X=value2X," +
				"portalCacheName2Y=key2Y=value2Y");

		Assert.assertEquals(
			expectedProperties1.keySet(), mergedPropertiesMap1.keySet());

		ObjectValuePair objectValuePair1 = mergedPropertiesMap1.get(
			"portalCacheName1");

		Assert.assertEquals(
			_getProperties("key1=value1"), objectValuePair1.getKey());

		Assert.assertEquals(
			_getProperties(
				new ObjectValuePair<Object, Object>("key1", "value1"),
				new ObjectValuePair<Object, Object>("replicator", true)),
			objectValuePair1.getValue());

		ObjectValuePair objectValuePair2 = mergedPropertiesMap1.get(
			"portalCacheName2X");

		Assert.assertEquals(
			_getProperties("key2X=value2X"), objectValuePair2.getKey());

		Assert.assertNull(objectValuePair2.getValue());

		ObjectValuePair objectValuePair3 = mergedPropertiesMap1.get(
			"portalCacheName2Y");

		Assert.assertNull(objectValuePair3.getKey());

		Assert.assertEquals(
			_getProperties(
				new ObjectValuePair<Object, Object>("key2Y", "value2Y"),
				new ObjectValuePair<Object, Object>("replicator", true)),
			objectValuePair3.getValue());

		// Test 3: _bootstrapLoaderEnabled is false, _bootstrapLoaderProperties
		// and _replicatorProperties are non-empty

		Map<String, ObjectValuePair<Properties, Properties>>
			mergedPropertiesMap2 = ReflectionTestUtil.invoke(
				_getMultiVMEhcachePortalCacheManagerConfigurator(
					true, false, false, false),
				"_getMergedPropertiesMap", null, null);

		Properties expectedProperties2 = _getProperties(
			"portalCacheName1=key1=value1,portalCacheName2Y=key2Y=value2Y");

		Assert.assertEquals(
			expectedProperties2.keySet(), mergedPropertiesMap2.keySet());

		ObjectValuePair objectValuePair4 = mergedPropertiesMap2.get(
			"portalCacheName1");

		Assert.assertNull(objectValuePair4.getKey());

		Assert.assertEquals(
			_getProperties(
				new ObjectValuePair<Object, Object>("key1", "value1"),
				new ObjectValuePair<Object, Object>("replicator", true)),
			objectValuePair4.getValue());

		ObjectValuePair objectValuePair5 = mergedPropertiesMap2.get(
			"portalCacheName2Y");

		Assert.assertNull(objectValuePair5.getKey());

		Assert.assertEquals(
			_getProperties(
				new ObjectValuePair<Object, Object>("key2Y", "value2Y"),
				new ObjectValuePair<Object, Object>("replicator", true)),
			objectValuePair5.getValue());
	}

	@Test
	public void testGetPortalPropertiesString() {
		MultiVMEhcachePortalCacheManagerConfigurator
			multiVMEhcachePortalCacheManagerConfigurator =
				_getMultiVMEhcachePortalCacheManagerConfigurator(
					true, true, true, true);

		Assert.assertNull(
			multiVMEhcachePortalCacheManagerConfigurator.
				getPortalPropertiesString("portal.property.Key1"));

		Assert.assertSame(
			"key=value",
			multiVMEhcachePortalCacheManagerConfigurator.
				getPortalPropertiesString("portal.property.Key2"));

		Assert.assertEquals(
			"key1=value1,key2=value2",
			multiVMEhcachePortalCacheManagerConfigurator.
				getPortalPropertiesString("portal.property.Key3"));
	}

	@Test
	public void testIsRequireSerialization() {
		MultiVMEhcachePortalCacheManagerConfigurator
			multiVMEhcachePortalCacheManagerConfigurator =
				_getMultiVMEhcachePortalCacheManagerConfigurator(
					true, true, true, true);

		CacheConfiguration cacheConfiguration = new CacheConfiguration();

		Assert.assertTrue(
			"The true value should be returned if clusterEnabled is true",
			multiVMEhcachePortalCacheManagerConfigurator.isRequireSerialization(
				cacheConfiguration));

		multiVMEhcachePortalCacheManagerConfigurator =
			_getMultiVMEhcachePortalCacheManagerConfigurator(
				false, true, true, true);

		Assert.assertFalse(
			"The false value should be returned if clusterEnabled is false " +
				"with empty cacheConfiguration",
			multiVMEhcachePortalCacheManagerConfigurator.isRequireSerialization(
				cacheConfiguration));
	}

	@Test
	public void testManageConfiguration() {
		MultiVMEhcachePortalCacheManagerConfigurator
			multiVMEhcachePortalCacheManagerConfigurator =
				_getMultiVMEhcachePortalCacheManagerConfigurator(
					false, true, true, true);

		// Test 1: clusterEnabled is false

		Configuration configuration = new Configuration();

		final AtomicBoolean calledGetDefaultPortalCacheConfiguration =
			new AtomicBoolean(false);

		multiVMEhcachePortalCacheManagerConfigurator.manageConfiguration(
			configuration,
			new PortalCacheManagerConfiguration(null, null, null) {

				@Override
				public PortalCacheConfiguration
					getDefaultPortalCacheConfiguration() {

					calledGetDefaultPortalCacheConfiguration.set(true);

					return null;
				}

			});

		Assert.assertFalse(
			"The method MultiVMEhcachePortalCacheManagerConfigurator." +
				"manageConfiguration(Configuration, PortalCacheManagerConfigu" +
					"ration) should be returned if clusterEnabled is false",
			calledGetDefaultPortalCacheConfiguration.get());

		// Test 2: clusterEnabled is true, _bootstrapLoaderProperties and
		// _replicatorProperties are non-empty

		multiVMEhcachePortalCacheManagerConfigurator =
			_getMultiVMEhcachePortalCacheManagerConfigurator(
				true, true, false, false);

		Set<Properties> portalCacheListenerPropertiesSet = new HashSet<>();

		portalCacheListenerPropertiesSet.add(
			_getProperties(
				new ObjectValuePair<Object, Object>(
					PortalCacheReplicator.REPLICATOR, true)));

		portalCacheListenerPropertiesSet.add(
			_getProperties(
				new ObjectValuePair<Object, Object>(
					PortalCacheReplicator.REPLICATOR, false)));

		PortalCacheManagerConfiguration portalCacheManagerConfiguration =
			new PortalCacheManagerConfiguration(
				null,
				new PortalCacheConfiguration(
					PortalCacheConfiguration.PORTAL_CACHE_NAME_DEFAULT,
					portalCacheListenerPropertiesSet, null),
				null);

		multiVMEhcachePortalCacheManagerConfigurator.manageConfiguration(
			configuration, portalCacheManagerConfiguration);

		Map<String, ObjectValuePair<Properties, Properties>>
			mergedPropertiesMap = ReflectionTestUtil.invoke(
				multiVMEhcachePortalCacheManagerConfigurator,
				"_getMergedPropertiesMap", null, null);

		PortalCacheConfiguration portalCacheConfiguration =
			portalCacheManagerConfiguration.getPortalCacheConfiguration(
				"portalCacheName1");

		ObjectValuePair<Properties, Properties> propertiesPair =
			mergedPropertiesMap.get("portalCacheName1");

		Assert.assertEquals(
			propertiesPair.getKey(),
			portalCacheConfiguration.getPortalCacheBootstrapLoaderProperties());

		portalCacheListenerPropertiesSet =
			portalCacheConfiguration.getPortalCacheListenerPropertiesSet();

		Assert.assertEquals(
			portalCacheListenerPropertiesSet.toString(), 2,
			portalCacheListenerPropertiesSet.size());
		Assert.assertFalse(
			portalCacheListenerPropertiesSet.toString(),
			portalCacheListenerPropertiesSet.contains(
				_getProperties(
					new ObjectValuePair<Object, Object>(
						PortalCacheReplicator.REPLICATOR, true))));
		Assert.assertTrue(
			portalCacheListenerPropertiesSet.toString(),
			portalCacheListenerPropertiesSet.contains(
				propertiesPair.getValue()));
		Assert.assertTrue(
			portalCacheListenerPropertiesSet.toString(),
			portalCacheListenerPropertiesSet.contains(
				_getProperties(
					new ObjectValuePair<Object, Object>(
						PortalCacheReplicator.REPLICATOR, false))));

		portalCacheConfiguration =
			portalCacheManagerConfiguration.getPortalCacheConfiguration(
				"portalCacheName2X");

		propertiesPair = mergedPropertiesMap.get("portalCacheName2X");

		Assert.assertEquals(
			propertiesPair.getKey(),
			portalCacheConfiguration.getPortalCacheBootstrapLoaderProperties());

		portalCacheListenerPropertiesSet =
			portalCacheConfiguration.getPortalCacheListenerPropertiesSet();

		Assert.assertEquals(
			portalCacheListenerPropertiesSet.toString(), 2,
			portalCacheListenerPropertiesSet.size());
		Assert.assertTrue(
			portalCacheListenerPropertiesSet.toString(),
			portalCacheListenerPropertiesSet.contains(
				_getProperties(
					new ObjectValuePair<Object, Object>(
						PortalCacheReplicator.REPLICATOR, true))));
		Assert.assertTrue(
			portalCacheListenerPropertiesSet.toString(),
			portalCacheListenerPropertiesSet.contains(
				_getProperties(
					new ObjectValuePair<Object, Object>(
						PortalCacheReplicator.REPLICATOR, false))));

		portalCacheConfiguration =
			portalCacheManagerConfiguration.getPortalCacheConfiguration(
				"portalCacheName2Y");

		propertiesPair = mergedPropertiesMap.get("portalCacheName2Y");

		Assert.assertNull(
			portalCacheConfiguration.getPortalCacheBootstrapLoaderProperties());

		portalCacheListenerPropertiesSet =
			portalCacheConfiguration.getPortalCacheListenerPropertiesSet();

		Assert.assertEquals(
			portalCacheListenerPropertiesSet.toString(), 2,
			portalCacheListenerPropertiesSet.size());
		Assert.assertFalse(
			portalCacheListenerPropertiesSet.toString(),
			portalCacheListenerPropertiesSet.contains(
				_getProperties(
					new ObjectValuePair<Object, Object>(
						PortalCacheReplicator.REPLICATOR, true))));
		Assert.assertTrue(
			portalCacheListenerPropertiesSet.toString(),
			portalCacheListenerPropertiesSet.contains(
				propertiesPair.getValue()));
		Assert.assertTrue(
			portalCacheListenerPropertiesSet.toString(),
			portalCacheListenerPropertiesSet.contains(
				_getProperties(
					new ObjectValuePair<Object, Object>(
						PortalCacheReplicator.REPLICATOR, false))));

		// Test 3: clusterEnabled and _bootstrapLoaderEnabled are true,
		// _bootstrapLoaderProperties is empty, _replicatorProperties is
		// non-empty

		multiVMEhcachePortalCacheManagerConfigurator =
			_getMultiVMEhcachePortalCacheManagerConfigurator(
				true, true, true, false);

		portalCacheManagerConfiguration = new PortalCacheManagerConfiguration(
			null,
			new PortalCacheConfiguration(
				PortalCacheConfiguration.PORTAL_CACHE_NAME_DEFAULT, null, null),
			null);

		multiVMEhcachePortalCacheManagerConfigurator.manageConfiguration(
			configuration, portalCacheManagerConfiguration);

		mergedPropertiesMap = ReflectionTestUtil.invoke(
			multiVMEhcachePortalCacheManagerConfigurator,
			"_getMergedPropertiesMap", null, null);

		for (Map.Entry<String, ObjectValuePair<Properties, Properties>> entry :
				mergedPropertiesMap.entrySet()) {

			portalCacheConfiguration =
				portalCacheManagerConfiguration.getPortalCacheConfiguration(
					entry.getKey());

			Assert.assertNull(
				portalCacheConfiguration.
					getPortalCacheBootstrapLoaderProperties());

			propertiesPair = entry.getValue();

			portalCacheListenerPropertiesSet =
				portalCacheConfiguration.getPortalCacheListenerPropertiesSet();

			Assert.assertTrue(
				portalCacheListenerPropertiesSet.contains(
					propertiesPair.getValue()));
		}

		// Test 4: clusterEnabled is true, _bootstrapLoaderEnabled is false,
		// _bootstrapLoaderProperties is empty, _replicatorProperties is
		// non-empty

		multiVMEhcachePortalCacheManagerConfigurator =
			_getMultiVMEhcachePortalCacheManagerConfigurator(
				true, false, true, false);

		portalCacheManagerConfiguration = new PortalCacheManagerConfiguration(
			null,
			new PortalCacheConfiguration(
				PortalCacheConfiguration.PORTAL_CACHE_NAME_DEFAULT, null, null),
			null);

		multiVMEhcachePortalCacheManagerConfigurator.manageConfiguration(
			configuration, portalCacheManagerConfiguration);

		mergedPropertiesMap = ReflectionTestUtil.invoke(
			multiVMEhcachePortalCacheManagerConfigurator,
			"_getMergedPropertiesMap", null, null);

		for (Map.Entry<String, ObjectValuePair<Properties, Properties>> entry :
				mergedPropertiesMap.entrySet()) {

			portalCacheConfiguration =
				portalCacheManagerConfiguration.getPortalCacheConfiguration(
					entry.getKey());

			Assert.assertNull(
				portalCacheConfiguration.
					getPortalCacheBootstrapLoaderProperties());

			propertiesPair = entry.getValue();

			portalCacheListenerPropertiesSet =
				portalCacheConfiguration.getPortalCacheListenerPropertiesSet();

			Assert.assertTrue(
				portalCacheListenerPropertiesSet.toString(),
				portalCacheListenerPropertiesSet.contains(
					propertiesPair.getValue()));
		}

		// Test 5: clusterEnabled is true, _bootstrapLoaderEnabled is true,
		// _bootstrapLoaderProperties is non-empty, _replicatorProperties is
		// empty

		multiVMEhcachePortalCacheManagerConfigurator =
			_getMultiVMEhcachePortalCacheManagerConfigurator(
				true, true, false, true);

		portalCacheManagerConfiguration = new PortalCacheManagerConfiguration(
			null,
			new PortalCacheConfiguration(
				PortalCacheConfiguration.PORTAL_CACHE_NAME_DEFAULT, null, null),
			null);

		multiVMEhcachePortalCacheManagerConfigurator.manageConfiguration(
			configuration, portalCacheManagerConfiguration);

		mergedPropertiesMap = ReflectionTestUtil.invoke(
			multiVMEhcachePortalCacheManagerConfigurator,
			"_getMergedPropertiesMap", null, null);

		for (Map.Entry<String, ObjectValuePair<Properties, Properties>> entry :
				mergedPropertiesMap.entrySet()) {

			portalCacheConfiguration =
				portalCacheManagerConfiguration.getPortalCacheConfiguration(
					entry.getKey());

			propertiesPair = entry.getValue();

			Assert.assertEquals(
				propertiesPair.getKey(),
				portalCacheConfiguration.
					getPortalCacheBootstrapLoaderProperties());

			portalCacheListenerPropertiesSet =
				portalCacheConfiguration.getPortalCacheListenerPropertiesSet();

			Assert.assertTrue(
				portalCacheListenerPropertiesSet.toString(),
				portalCacheListenerPropertiesSet.isEmpty());
		}

		// Test 6: clusterEnabled is true, _bootstrapLoaderEnabled is true,
		// _bootstrapLoaderProperties and _replicatorProperties are non-empty

		multiVMEhcachePortalCacheManagerConfigurator =
			_getMultiVMEhcachePortalCacheManagerConfigurator(
				true, true, false, false);

		final AtomicBoolean calledNewPortalCacheConfiguration =
			new AtomicBoolean(false);

		portalCacheListenerPropertiesSet = new HashSet<>();

		PortalCacheConfiguration defaultPortalCacheConfiguration =
			new PortalCacheConfiguration(
				PortalCacheConfiguration.PORTAL_CACHE_NAME_DEFAULT,
				portalCacheListenerPropertiesSet, null) {

				@Override
				public PortalCacheConfiguration newPortalCacheConfiguration(
					String portalCacheName) {

					calledNewPortalCacheConfiguration.set(true);

					return null;
				}

			};

		portalCacheManagerConfiguration = new PortalCacheManagerConfiguration(
			null, defaultPortalCacheConfiguration, null);

		mergedPropertiesMap = ReflectionTestUtil.invoke(
			multiVMEhcachePortalCacheManagerConfigurator,
			"_getMergedPropertiesMap", null, null);

		for (Map.Entry<String, ObjectValuePair<Properties, Properties>> entry :
				mergedPropertiesMap.entrySet()) {

			String portalCacheName = entry.getKey();

			portalCacheConfiguration = new PortalCacheConfiguration(
				portalCacheName, portalCacheListenerPropertiesSet, null);

			portalCacheManagerConfiguration.putPortalCacheConfiguration(
				portalCacheName, portalCacheConfiguration);
		}

		multiVMEhcachePortalCacheManagerConfigurator.manageConfiguration(
			configuration, portalCacheManagerConfiguration);

		Assert.assertFalse(
			"The portalCacheConfiguration do not need be created again it " +
				"already exists",
			calledNewPortalCacheConfiguration.get());
	}

	@Test
	public void testParseCacheListenerConfigurations() {

		// Test 1: clusterEnabled is false, _bootstrapLoaderEnabled is true,
		// _bootstrapLoaderProperties and _replicatorProperties are empty

		MultiVMEhcachePortalCacheManagerConfigurator
			multiVMEhcachePortalCacheManagerConfigurator1 =
				_getMultiVMEhcachePortalCacheManagerConfigurator(
					false, true, true, true);

		PortalCacheConfiguration portalCacheConfiguration1 =
			multiVMEhcachePortalCacheManagerConfigurator1.
				parseCacheListenerConfigurations(
					new CacheConfiguration("TestName", 0), true);

		Assert.assertEquals(
			"TestName", portalCacheConfiguration1.getPortalCacheName());
		Assert.assertNull(
			portalCacheConfiguration1.
				getPortalCacheBootstrapLoaderProperties());

		Assert.assertEquals(
			Collections.emptySet(),
			portalCacheConfiguration1.getPortalCacheListenerPropertiesSet());

		// Test 2: clusterEnabled and _bootstrapLoaderEnabled are true,
		// _bootstrapLoaderProperties and _replicatorProperties are empty

		MultiVMEhcachePortalCacheManagerConfigurator
			multiVMEhcachePortalCacheManagerConfigurator2 =
				_getMultiVMEhcachePortalCacheManagerConfigurator(
					true, true, true, true);

		PortalCacheConfiguration portalCacheConfiguration2 =
			multiVMEhcachePortalCacheManagerConfigurator2.
				parseCacheListenerConfigurations(
					new CacheConfiguration("TestName", 0), true);

		Assert.assertEquals(
			"TestName", portalCacheConfiguration2.getPortalCacheName());

		Assert.assertEquals(
			_getProperties("key1=value1,key2=value2"),
			portalCacheConfiguration2.
				getPortalCacheBootstrapLoaderProperties());

		Assert.assertEquals(
			Collections.singleton(
				_getProperties(
					new ObjectValuePair<Object, Object>("key1", "value1"),
					new ObjectValuePair<Object, Object>("key2", "value2"),
					new ObjectValuePair<Object, Object>("replicator", true))),
			portalCacheConfiguration2.getPortalCacheListenerPropertiesSet());

		// Test 3: clusterEnabled is true, _bootstrapLoaderEnabled is false,
		// _bootstrapLoaderProperties and _replicatorProperties are empty

		MultiVMEhcachePortalCacheManagerConfigurator
			multiVMEhcachePortalCacheManagerConfigurator3 =
				_getMultiVMEhcachePortalCacheManagerConfigurator(
					true, false, true, true);

		PortalCacheConfiguration portalCacheConfiguration3 =
			multiVMEhcachePortalCacheManagerConfigurator3.
				parseCacheListenerConfigurations(
					new CacheConfiguration("TestName", 0), true);

		Assert.assertEquals(
			"TestName", portalCacheConfiguration3.getPortalCacheName());

		Assert.assertNull(
			portalCacheConfiguration3.
				getPortalCacheBootstrapLoaderProperties());

		Assert.assertEquals(
			Collections.singleton(
				_getProperties(
					new ObjectValuePair<Object, Object>("key1", "value1"),
					new ObjectValuePair<Object, Object>("key2", "value2"),
					new ObjectValuePair<Object, Object>("replicator", true))),
			portalCacheConfiguration3.getPortalCacheListenerPropertiesSet());

		// Test 4: clusterEnabled and _bootstrapLoaderEnabled are true,
		// _bootstrapLoaderProperties and _replicatorProperties are non-empty

		MultiVMEhcachePortalCacheManagerConfigurator
			multiVMEhcachePortalCacheManagerConfigurator4 =
				_getMultiVMEhcachePortalCacheManagerConfigurator(
					true, true, false, false);

		PortalCacheConfiguration portalCacheConfiguration4 =
			multiVMEhcachePortalCacheManagerConfigurator4.
				parseCacheListenerConfigurations(
					new CacheConfiguration("portalCacheName1", 0), true);

		Properties bootstrapLoaderProperties = ReflectionTestUtil.getFieldValue(
			multiVMEhcachePortalCacheManagerConfigurator4,
			"_bootstrapLoaderProperties");
		Properties replicatorProperties = ReflectionTestUtil.getFieldValue(
			multiVMEhcachePortalCacheManagerConfigurator4,
			"_replicatorProperties");

		Assert.assertNull(bootstrapLoaderProperties.get("portalCacheName1"));
		Assert.assertNull(replicatorProperties.get("portalCacheName1"));
		Assert.assertEquals(
			"portalCacheName1", portalCacheConfiguration4.getPortalCacheName());

		Assert.assertEquals(
			Collections.singleton(
				_getProperties(
					new ObjectValuePair<Object, Object>("key1", "value1"),
					new ObjectValuePair<Object, Object>("replicator", true))),
			portalCacheConfiguration4.getPortalCacheListenerPropertiesSet());
	}

	@Test
	public void testSetProps() {
		Props props = (Props)ProxyUtil.newProxyInstance(
			MultiVMEhcachePortalCacheManagerConfiguratorTest.
				class.getClassLoader(),
			new Class<?>[] {Props.class},
			new PropsInvocationHandler(true, true, true, true));

		MultiVMEhcachePortalCacheManagerConfigurator
			multiVMEhcachePortalCacheManagerConfigurator =
				new RMIMultiVMEhcachePortalCacheManagerConfigurator();

		multiVMEhcachePortalCacheManagerConfigurator.setProps(props);

		Assert.assertSame(
			props,
			ReflectionTestUtil.getFieldValue(
				multiVMEhcachePortalCacheManagerConfigurator, "props"));
	}

	private MultiVMEhcachePortalCacheManagerConfigurator
		_getMultiVMEhcachePortalCacheManagerConfigurator(
			boolean clusterEnabled, boolean bootstrapLoaderEnabled,
			boolean bootstrapLoaderPropertiesIsEmpty,
			boolean replicatorPropertiesIsEmpty) {

		MultiVMEhcachePortalCacheManagerConfigurator
			multiVMEhcachePortalCacheManagerConfigurator =
				new MultiVMEhcachePortalCacheManagerConfigurator();

		multiVMEhcachePortalCacheManagerConfigurator.setProps(
			(Props)ProxyUtil.newProxyInstance(
				MultiVMEhcachePortalCacheManagerConfiguratorTest.
					class.getClassLoader(),
				new Class<?>[] {Props.class},
				new PropsInvocationHandler(
					clusterEnabled, bootstrapLoaderEnabled,
					bootstrapLoaderPropertiesIsEmpty,
					replicatorPropertiesIsEmpty)));

		multiVMEhcachePortalCacheManagerConfigurator.activate();

		return multiVMEhcachePortalCacheManagerConfigurator;
	}

	private Properties _getProperties(
		ObjectValuePair<Object, Object>... objectObjectValuePairs) {

		Properties properties = new Properties();

		for (ObjectValuePair<Object, Object> objectObjectValuePair :
				objectObjectValuePairs) {

			properties.put(
				objectObjectValuePair.getKey(),
				objectObjectValuePair.getValue());
		}

		return properties;
	}

	private Properties _getProperties(String propertiesString) {
		Properties properties = new Properties();

		if (propertiesString == null) {
			return properties;
		}

		try {
			properties.load(
				new UnsyncStringReader(
					StringUtil.replace(
						propertiesString, CharPool.COMMA,
						StringPool.NEW_LINE)));
		}
		catch (IOException ioe) {
			throw new RuntimeException(ioe);
		} return properties;
	}

	private class PropsInvocationHandler implements InvocationHandler {

		public PropsInvocationHandler(
			boolean clusterEnabled, boolean bootstrapLoaderEnabled,
			boolean bootstrapLoaderPropertiesIsEmpty,
			boolean replicatorPropertiesIsEmpty) {

			_clusterEnabled = clusterEnabled;
			_bootstrapLoaderEnabled = bootstrapLoaderEnabled;
			_bootstrapLoaderPropertiesIsEmpty =
				bootstrapLoaderPropertiesIsEmpty;
			_replicatorPropertiesIsEmpty = replicatorPropertiesIsEmpty;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) {
			String methodName = method.getName();

			if (methodName.equals("get")) {
				if (PropsKeys.CLUSTER_LINK_ENABLED.equals(args[0])) {
					return String.valueOf(_clusterEnabled);
				}

				if (PropsKeys.EHCACHE_BOOTSTRAP_CACHE_LOADER_ENABLED.equals(
						args[0])) {

					return String.valueOf(_bootstrapLoaderEnabled);
				}
			}

			if (methodName.equals("getArray")) {
				if ("portal.property.Key1".equals(args[0])) {
					return new String[0];
				}

				if ("portal.property.Key2".equals(args[0])) {
					return new String[] {"key=value"};
				}

				return new String[] {"key1=value1", "key2=value2"};
			}

			if (methodName.equals("getProperties")) {
				if (args[0].equals(
						PropsKeys.EHCACHE_BOOTSTRAP_CACHE_LOADER_PROPERTIES +
							StringPool.PERIOD) &&
					!_bootstrapLoaderPropertiesIsEmpty) {

					return _getProperties(
						"portalCacheName1=key1=value1," +
							"portalCacheName2X=key2X=value2X");
				}

				if (args[0].equals(
						PropsKeys.EHCACHE_REPLICATOR_PROPERTIES +
							StringPool.PERIOD) &&
					!_replicatorPropertiesIsEmpty) {

					return _getProperties(
						"portalCacheName1=key1=value1," +
							"portalCacheName2Y=key2Y=value2Y");
				}

				return _getProperties((String)null);
			}

			return null;
		}

		private final boolean _bootstrapLoaderEnabled;
		private final boolean _bootstrapLoaderPropertiesIsEmpty;
		private final boolean _clusterEnabled;
		private final boolean _replicatorPropertiesIsEmpty;

	}

}
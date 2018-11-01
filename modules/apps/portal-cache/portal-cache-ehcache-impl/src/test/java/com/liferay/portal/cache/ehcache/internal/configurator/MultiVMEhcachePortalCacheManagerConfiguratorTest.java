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
import com.liferay.portal.kernel.util.ProxyUtil;
import com.liferay.portal.kernel.util.StringUtil;

import java.io.IOException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author Leon Chi
 */
public class MultiVMEhcachePortalCacheManagerConfiguratorTest
	extends BaseEhcachePortalCacheManagerConfiguratorTestCase {

	@ClassRule
	public static final CodeCoverageAssertor codeCoverageAssertor =
		CodeCoverageAssertor.INSTANCE;

	@Before
	public void setUp() {
		baseEhcachePortalCacheManagerConfigurator =
			getMultiVMEhcachePortalCacheManagerConfigurator(
				false, true, true, true);
	}

	@Test
	public void testActivate() {
		MultiVMEhcachePortalCacheManagerConfigurator
			multiVMEhcachePortalCacheManagerConfigurator =
				getMultiVMEhcachePortalCacheManagerConfigurator(
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

		Map<String, ObjectValuePair<Properties, Properties>>
			mergedPropertiesMap1 = ReflectionTestUtil.invoke(
				getMultiVMEhcachePortalCacheManagerConfigurator(
					true, true, true, true),
				"_getMergedPropertiesMap", null, null);

		Assert.assertTrue(
			"The mergedPropertiesMap should be empty if " +
				"bootstrapLoaderProperties and replicatorProperties are empty",
			mergedPropertiesMap1.isEmpty());

		// Test 2: _bootstrapLoaderEnabled is true, _bootstrapLoaderProperties
		// and _replicatorProperties are non-empty

		Map<String, ObjectValuePair<Properties, Properties>>
			mergedPropertiesMap2 = ReflectionTestUtil.invoke(
				getMultiVMEhcachePortalCacheManagerConfigurator(
					true, true, false, false),
				"_getMergedPropertiesMap", null, null);

		Assert.assertEquals(
			mergedPropertiesMap2.toString(), 3, mergedPropertiesMap2.size());

		_assertMergedPropertiesMap(
			_getProperties("key1=value1"),
			_getProperties(
				new ObjectValuePair<Object, Object>("key1", "value1"),
				new ObjectValuePair<Object, Object>("replicator", true)),
			mergedPropertiesMap2, "portalCacheName1");
		_assertMergedPropertiesMap(
			_getProperties("key2X=value2X"), null, mergedPropertiesMap2,
			"portalCacheName2X");
		_assertMergedPropertiesMap(
			null,
			_getProperties(
				new ObjectValuePair<Object, Object>("key2Y", "value2Y"),
				new ObjectValuePair<Object, Object>("replicator", true)),
			mergedPropertiesMap2, "portalCacheName2Y");

		// Test 3: _bootstrapLoaderEnabled is false, _bootstrapLoaderProperties
		// and _replicatorProperties are non-empty

		Map<String, ObjectValuePair<Properties, Properties>>
			mergedPropertiesMap3 = ReflectionTestUtil.invoke(
				getMultiVMEhcachePortalCacheManagerConfigurator(
					true, false, false, false),
				"_getMergedPropertiesMap", null, null);

		Assert.assertEquals(
			mergedPropertiesMap3.toString(), 2, mergedPropertiesMap3.size());

		_assertMergedPropertiesMap(
			null,
			_getProperties(
				new ObjectValuePair<Object, Object>("key1", "value1"),
				new ObjectValuePair<Object, Object>("replicator", true)),
			mergedPropertiesMap3, "portalCacheName1");
		_assertMergedPropertiesMap(
			null,
			_getProperties(
				new ObjectValuePair<Object, Object>("key2Y", "value2Y"),
				new ObjectValuePair<Object, Object>("replicator", true)),
			mergedPropertiesMap3, "portalCacheName2Y");
	}

	@Test
	public void testGetPortalPropertiesString() {
		MultiVMEhcachePortalCacheManagerConfigurator
			multiVMEhcachePortalCacheManagerConfigurator =
				getMultiVMEhcachePortalCacheManagerConfigurator(
					true, true, true, true);

		Assert.assertNull(
			multiVMEhcachePortalCacheManagerConfigurator.
				getPortalPropertiesString("portal.property.Key1"));
		Assert.assertEquals(
			"key=value",
			multiVMEhcachePortalCacheManagerConfigurator.
				getPortalPropertiesString("portal.property.Key2"));
		Assert.assertEquals(
			"key1=value1,key2=value2",
			multiVMEhcachePortalCacheManagerConfigurator.
				getPortalPropertiesString("portal.property.Key3"));
	}

	@Override
	@Test
	public void testIsRequireSerialization() {
		super.testIsRequireSerialization();

		MultiVMEhcachePortalCacheManagerConfigurator
			multiVMEhcachePortalCacheManagerConfigurator1 =
				getMultiVMEhcachePortalCacheManagerConfigurator(
					true, true, true, true);

		Assert.assertTrue(
			"The true value should be returned if clusterEnabled is true",
			multiVMEhcachePortalCacheManagerConfigurator1.
				isRequireSerialization(new CacheConfiguration()));
	}

	@Test
	public void testManageConfiguration() {
		MultiVMEhcachePortalCacheManagerConfigurator
			multiVMEhcachePortalCacheManagerConfigurator1 =
				getMultiVMEhcachePortalCacheManagerConfigurator(
					false, true, true, true);

		// Test 1: clusterEnabled is false

		final boolean[] calledGetDefaultPortalCacheConfiguration = {false};

		multiVMEhcachePortalCacheManagerConfigurator1.manageConfiguration(
			new Configuration(),
			new PortalCacheManagerConfiguration(null, null, null) {

				@Override
				public PortalCacheConfiguration
					getDefaultPortalCacheConfiguration() {

					calledGetDefaultPortalCacheConfiguration[0] = true;

					return null;
				}

			});

		Assert.assertFalse(
			"The manageConfiguration method should be returned directly if " +
				"clusterEnabled is false",
			calledGetDefaultPortalCacheConfiguration[0]);

		// Test 2: clusterEnabled is true, _bootstrapLoaderProperties and
		// _replicatorProperties are non-empty

		MultiVMEhcachePortalCacheManagerConfigurator
			multiVMEhcachePortalCacheManagerConfigurator2 =
				getMultiVMEhcachePortalCacheManagerConfigurator(
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

		PortalCacheManagerConfiguration portalCacheManagerConfiguration1 =
			new PortalCacheManagerConfiguration(
				null,
				new PortalCacheConfiguration(
					PortalCacheConfiguration.PORTAL_CACHE_NAME_DEFAULT,
					portalCacheListenerPropertiesSet, null),
				null);

		multiVMEhcachePortalCacheManagerConfigurator2.manageConfiguration(
			new Configuration(), portalCacheManagerConfiguration1);

		_assertPortalCacheManagerConfiguration(
			portalCacheManagerConfiguration1, "portalCacheName1",
			_getProperties("key1=value1"),
			_getProperties(
				new ObjectValuePair<Object, Object>("key1", "value1"),
				new ObjectValuePair<Object, Object>(
					PortalCacheReplicator.REPLICATOR, true)),
			_getProperties(
				new ObjectValuePair<Object, Object>(
					PortalCacheReplicator.REPLICATOR, false)));
		_assertPortalCacheManagerConfiguration(
			portalCacheManagerConfiguration1, "portalCacheName2X",
			_getProperties("key2X=value2X"),
			_getProperties(
				new ObjectValuePair<Object, Object>(
					PortalCacheReplicator.REPLICATOR, true)),
			_getProperties(
				new ObjectValuePair<Object, Object>(
					PortalCacheReplicator.REPLICATOR, false)));
		_assertPortalCacheManagerConfiguration(
			portalCacheManagerConfiguration1, "portalCacheName2Y", null,
			_getProperties(
				new ObjectValuePair<Object, Object>("key2Y", "value2Y"),
				new ObjectValuePair<Object, Object>(
					PortalCacheReplicator.REPLICATOR, true)),
			_getProperties(
				new ObjectValuePair<Object, Object>(
					PortalCacheReplicator.REPLICATOR, false)));

		// Test 3: clusterEnabled and _bootstrapLoaderEnabled are true,
		// _bootstrapLoaderProperties is empty, _replicatorProperties is
		// non-empty

		MultiVMEhcachePortalCacheManagerConfigurator
			multiVMEhcachePortalCacheManagerConfigurator3 =
				getMultiVMEhcachePortalCacheManagerConfigurator(
					true, true, true, false);

		PortalCacheManagerConfiguration portalCacheManagerConfiguration2 =
			new PortalCacheManagerConfiguration(
				null,
				new PortalCacheConfiguration(
					PortalCacheConfiguration.PORTAL_CACHE_NAME_DEFAULT, null,
					null),
				null);

		multiVMEhcachePortalCacheManagerConfigurator3.manageConfiguration(
			new Configuration(), portalCacheManagerConfiguration2);

		_assertPortalCacheManagerConfiguration(
			portalCacheManagerConfiguration2, "portalCacheName1", null,
			_getProperties(
				new ObjectValuePair<Object, Object>("key1", "value1"),
				new ObjectValuePair<Object, Object>(
					PortalCacheReplicator.REPLICATOR, true)));
		_assertPortalCacheManagerConfiguration(
			portalCacheManagerConfiguration2, "portalCacheName2Y", null,
			_getProperties(
				new ObjectValuePair<Object, Object>("key2Y", "value2Y"),
				new ObjectValuePair<Object, Object>(
					PortalCacheReplicator.REPLICATOR, true)));

		// Test 4: clusterEnabled is true, _bootstrapLoaderEnabled is false,
		// _bootstrapLoaderProperties is empty, _replicatorProperties is
		// non-empty

		MultiVMEhcachePortalCacheManagerConfigurator
			multiVMEhcachePortalCacheManagerConfigurator4 =
				getMultiVMEhcachePortalCacheManagerConfigurator(
					true, false, true, false);

		PortalCacheManagerConfiguration portalCacheManagerConfiguration3 =
			new PortalCacheManagerConfiguration(
				null,
				new PortalCacheConfiguration(
					PortalCacheConfiguration.PORTAL_CACHE_NAME_DEFAULT, null,
					null),
				null);

		multiVMEhcachePortalCacheManagerConfigurator4.manageConfiguration(
			new Configuration(), portalCacheManagerConfiguration3);

		_assertPortalCacheManagerConfiguration(
			portalCacheManagerConfiguration3, "portalCacheName1", null,
			_getProperties(
				new ObjectValuePair<Object, Object>("key1", "value1"),
				new ObjectValuePair<Object, Object>(
					PortalCacheReplicator.REPLICATOR, true)));
		_assertPortalCacheManagerConfiguration(
			portalCacheManagerConfiguration3, "portalCacheName2Y", null,
			_getProperties(
				new ObjectValuePair<Object, Object>("key2Y", "value2Y"),
				new ObjectValuePair<Object, Object>(
					PortalCacheReplicator.REPLICATOR, true)));

		// Test 5: clusterEnabled is true, _bootstrapLoaderEnabled is true,
		// _bootstrapLoaderProperties is non-empty, _replicatorProperties is
		// empty

		MultiVMEhcachePortalCacheManagerConfigurator
			multiVMEhcachePortalCacheManagerConfigurator5 =
				getMultiVMEhcachePortalCacheManagerConfigurator(
					true, true, false, true);

		PortalCacheManagerConfiguration portalCacheManagerConfiguration4 =
			new PortalCacheManagerConfiguration(
				null,
				new PortalCacheConfiguration(
					PortalCacheConfiguration.PORTAL_CACHE_NAME_DEFAULT, null,
					null),
				null);

		multiVMEhcachePortalCacheManagerConfigurator5.manageConfiguration(
			new Configuration(), portalCacheManagerConfiguration4);

		_assertPortalCacheManagerConfiguration(
			portalCacheManagerConfiguration4, "portalCacheName1",
			_getProperties("key1=value1"), new Properties[0]);
		_assertPortalCacheManagerConfiguration(
			portalCacheManagerConfiguration4, "portalCacheName2X",
			_getProperties("key2X=value2X"), new Properties[0]);

		// Test 6: clusterEnabled is true, _bootstrapLoaderEnabled is true,
		// _bootstrapLoaderProperties and _replicatorProperties are non-empty

		MultiVMEhcachePortalCacheManagerConfigurator
			multiVMEhcachePortalCacheManagerConfigurator6 =
				getMultiVMEhcachePortalCacheManagerConfigurator(
					true, true, false, false);

		final boolean[] calledNewPortalCacheConfiguration = {false};

		PortalCacheConfiguration defaultPortalCacheConfiguration =
			new PortalCacheConfiguration(
				PortalCacheConfiguration.PORTAL_CACHE_NAME_DEFAULT,
				new HashSet<Properties>(), null) {

				@Override
				public PortalCacheConfiguration newPortalCacheConfiguration(
					String portalCacheName) {

					calledNewPortalCacheConfiguration[0] = true;

					return null;
				}

			};

		PortalCacheManagerConfiguration portalCacheManagerConfiguration5 =
			new PortalCacheManagerConfiguration(
				null, defaultPortalCacheConfiguration, null);

		Map<String, ObjectValuePair<Properties, Properties>>
			mergedPropertiesMap = ReflectionTestUtil.invoke(
				multiVMEhcachePortalCacheManagerConfigurator6,
				"_getMergedPropertiesMap", null, null);

		for (Map.Entry<String, ObjectValuePair<Properties, Properties>> entry :
				mergedPropertiesMap.entrySet()) {

			String portalCacheName = entry.getKey();

			portalCacheManagerConfiguration5.putPortalCacheConfiguration(
				portalCacheName,
				new PortalCacheConfiguration(
					portalCacheName, new HashSet<Properties>(), null));
		}

		multiVMEhcachePortalCacheManagerConfigurator6.manageConfiguration(
			new Configuration(), portalCacheManagerConfiguration5);

		Assert.assertFalse(
			"The portalCacheConfiguration should be get from " +
				"portalCacheManagerConfiguration if it exists",
			calledNewPortalCacheConfiguration[0]);
	}

	@Override
	@Test
	public void testParseCacheListenerConfigurations() {

		// Test 1: clusterEnabled is false, _bootstrapLoaderEnabled is true,
		// _bootstrapLoaderProperties and _replicatorProperties are empty

		super.testParseCacheListenerConfigurations();

		// Test 2: clusterEnabled and _bootstrapLoaderEnabled are true,
		// _bootstrapLoaderProperties and _replicatorProperties are empty

		MultiVMEhcachePortalCacheManagerConfigurator
			multiVMEhcachePortalCacheManagerConfigurator2 =
				getMultiVMEhcachePortalCacheManagerConfigurator(
					true, true, true, true);

		_assertPortalCacheConfiguration(
			"portalCacheNameOutsideProperties",
			_getProperties("key1=value1,key2=value2"),
			Collections.singleton(
				_getProperties(
					new ObjectValuePair<Object, Object>("key1", "value1"),
					new ObjectValuePair<Object, Object>("key2", "value2"),
					new ObjectValuePair<Object, Object>("replicator", true))),
			multiVMEhcachePortalCacheManagerConfigurator2.
				parseCacheListenerConfigurations(
					new CacheConfiguration(
						"portalCacheNameOutsideProperties", 0),
					true));

		// Test 3: clusterEnabled is true, _bootstrapLoaderEnabled is false,
		// _bootstrapLoaderProperties and _replicatorProperties are empty

		MultiVMEhcachePortalCacheManagerConfigurator
			multiVMEhcachePortalCacheManagerConfigurator3 =
				getMultiVMEhcachePortalCacheManagerConfigurator(
					true, false, true, true);

		_assertPortalCacheConfiguration(
			"portalCacheNameOutsideProperties", null,
			Collections.singleton(
				_getProperties(
					new ObjectValuePair<Object, Object>("key1", "value1"),
					new ObjectValuePair<Object, Object>("key2", "value2"),
					new ObjectValuePair<Object, Object>("replicator", true))),
			multiVMEhcachePortalCacheManagerConfigurator3.
				parseCacheListenerConfigurations(
					new CacheConfiguration(
						"portalCacheNameOutsideProperties", 0),
					true));

		// Test 4: clusterEnabled and _bootstrapLoaderEnabled are true,
		// _bootstrapLoaderProperties and _replicatorProperties are non-empty

		MultiVMEhcachePortalCacheManagerConfigurator
			multiVMEhcachePortalCacheManagerConfigurator4 =
				getMultiVMEhcachePortalCacheManagerConfigurator(
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

		_assertPortalCacheConfiguration(
			"portalCacheName1", _getProperties("key1=value1"),
			Collections.singleton(
				_getProperties(
					new ObjectValuePair<Object, Object>("key1", "value1"),
					new ObjectValuePair<Object, Object>("replicator", true))),
			portalCacheConfiguration4);
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

	protected MultiVMEhcachePortalCacheManagerConfigurator
		getMultiVMEhcachePortalCacheManagerConfigurator(
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

	private void _assertMergedPropertiesMap(
		Properties expectedBootstrapLoaderProperties,
		Properties expectedReplicatorProperties,
		Map<String, ObjectValuePair<Properties, Properties>>
			mergedPropertiesMap,
		String portalCacheName) {

		ObjectValuePair objectValuePair = mergedPropertiesMap.get(
			portalCacheName);

		Assert.assertEquals(
			expectedBootstrapLoaderProperties, objectValuePair.getKey());
		Assert.assertEquals(
			expectedReplicatorProperties, objectValuePair.getValue());
	}

	private void _assertPortalCacheConfiguration(
		String expectedName, Properties expectedBootstrapLoaderProperties,
		Set<Properties> expectedPortalCacheListenerPropertiesSet,
		PortalCacheConfiguration portalCacheConfiguration) {

		Assert.assertEquals(
			expectedName, portalCacheConfiguration.getPortalCacheName());
		Assert.assertEquals(
			expectedBootstrapLoaderProperties,
			portalCacheConfiguration.getPortalCacheBootstrapLoaderProperties());
		Assert.assertEquals(
			expectedPortalCacheListenerPropertiesSet,
			portalCacheConfiguration.getPortalCacheListenerPropertiesSet());
	}

	private void _assertPortalCacheManagerConfiguration(
		PortalCacheManagerConfiguration portalCacheManagerConfiguration,
		String portalCacheName,
		Properties expectedPortalCacheBootstrapLoaderProperties,
		Properties... expectedPortalCacheListenerPropertiesArgs) {

		PortalCacheConfiguration portalCacheConfiguration =
			portalCacheManagerConfiguration.getPortalCacheConfiguration(
				portalCacheName);

		Set<Properties> expectedPortalCacheListenerPropertiesSet =
			new HashSet<>();

		for (Properties expectedPortalCacheListenerProperties :
				expectedPortalCacheListenerPropertiesArgs) {

			expectedPortalCacheListenerPropertiesSet.add(
				expectedPortalCacheListenerProperties);
		}

		_assertPortalCacheConfiguration(
			portalCacheName, expectedPortalCacheBootstrapLoaderProperties,
			expectedPortalCacheListenerPropertiesSet, portalCacheConfiguration);
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

		try {
			properties.load(
				new UnsyncStringReader(
					StringUtil.replace(
						propertiesString, CharPool.COMMA,
						StringPool.NEW_LINE)));
		}
		catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}

		return properties;
	}

}
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
import com.liferay.portal.cache.configuration.PortalCacheConfiguration;
import com.liferay.portal.cache.configuration.PortalCacheManagerConfiguration;
import com.liferay.portal.cache.ehcache.internal.EhcacheConstants;
import com.liferay.portal.cache.ehcache.internal.EhcachePortalCacheConfiguration;
import com.liferay.portal.kernel.cache.PortalCacheListenerScope;
import com.liferay.portal.kernel.cache.PortalCacheManagerNames;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.util.ObjectValuePair;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheConfiguration.CacheEventListenerFactoryConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.FactoryConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Leon Chi
 */
public abstract class BaseEhcachePortalCacheManagerConfiguratorTestCase {

	@Test
	public void testClearListenerConfigurationsWithCacheConfiguration() {

		// Test clearListenerConfigurations(null)

		baseEhcachePortalCacheManagerConfigurator.clearListenerConfigrations(
			(CacheConfiguration)null);

		// Test cleanListenerConfigurations(CacheConfiguration)

		CacheConfiguration cacheConfiguration = new CacheConfiguration();

		cacheConfiguration.bootstrapCacheLoaderFactory(
			new CacheConfiguration.BootstrapCacheLoaderFactoryConfiguration());

		cacheConfiguration.addCacheEventListenerFactory(
			new CacheEventListenerFactoryConfiguration());

		Assert.assertNotNull(
			cacheConfiguration.getBootstrapCacheLoaderFactoryConfiguration());

		List<CacheEventListenerFactoryConfiguration>
			cacheEventListenerConfigurations =
				cacheConfiguration.getCacheEventListenerConfigurations();

		Assert.assertFalse(
			"We should prepare a non-empty cacheEventListenerConfigurations",
			cacheEventListenerConfigurations.isEmpty());

		baseEhcachePortalCacheManagerConfigurator.clearListenerConfigrations(
			cacheConfiguration);

		Assert.assertNull(
			cacheConfiguration.getBootstrapCacheLoaderFactoryConfiguration());
		Assert.assertTrue(
			"cacheEventListenerConfigurations should be cleared after " +
				"calling clearListenerConfigrations(cacheConfiguration)",
			cacheEventListenerConfigurations.isEmpty());
	}

	@Test
	public void testClearListenerConfigurationsWithConfiguration() {
		Configuration configuration = new Configuration();

		FactoryConfiguration<?> factoryConfiguration =
			new FactoryConfiguration();

		factoryConfiguration.setClass(
			SingleVMEhcachePortalCacheManagerConfiguratorTest.class.getName());

		configuration.addCacheManagerEventListenerFactory(factoryConfiguration);

		configuration.addCacheManagerPeerListenerFactory(
			new FactoryConfiguration());
		configuration.addCacheManagerPeerProviderFactory(
			new FactoryConfiguration());

		CacheConfiguration defaultCacheConfiguration = new CacheConfiguration();

		defaultCacheConfiguration.addCacheEventListenerFactory(
			new CacheEventListenerFactoryConfiguration());

		configuration.setDefaultCacheConfiguration(defaultCacheConfiguration);

		CacheConfiguration cacheConfiguration =
			defaultCacheConfiguration.clone();

		cacheConfiguration.setName(_TEST_CACHE_NAME);

		configuration.addCache(cacheConfiguration);

		Assert.assertSame(
			factoryConfiguration,
			configuration.getCacheManagerEventListenerFactoryConfiguration());
		Assert.assertNotNull(factoryConfiguration.getFullyQualifiedClassPath());

		List<FactoryConfiguration>
			cacheManagerPeerListenerFactoryConfigurations =
				configuration.
					getCacheManagerPeerListenerFactoryConfigurations();

		Assert.assertFalse(
			"We should prepare a non-empty " +
				"cacheManagerPeerListenerFactoryConfigurations",
			cacheManagerPeerListenerFactoryConfigurations.isEmpty());

		List<FactoryConfiguration>
			cacheManagerPeerProviderFactoryConfigurations =
				configuration.getCacheManagerPeerProviderFactoryConfiguration();

		Assert.assertFalse(
			"We should prepare a non-empty " +
				"cacheManagerPeerProviderFactoryConfigurations",
			cacheManagerPeerProviderFactoryConfigurations.isEmpty());

		List<CacheEventListenerFactoryConfiguration>
			defaultCacheEventListenerConfigurations =
				defaultCacheConfiguration.getCacheEventListenerConfigurations();

		Assert.assertFalse(
			"We should prepare a non-empty " +
				"defaultCacheEventListenerConfigurations",
			defaultCacheEventListenerConfigurations.isEmpty());

		List<CacheEventListenerFactoryConfiguration>
			cacheEventListenerConfigurations =
				cacheConfiguration.getCacheEventListenerConfigurations();

		Assert.assertFalse(
			"We should prepare a non-empty cacheEventListenerConfigurations",
			cacheEventListenerConfigurations.isEmpty());

		baseEhcachePortalCacheManagerConfigurator.clearListenerConfigrations(
			configuration);

		Assert.assertSame(
			factoryConfiguration,
			configuration.getCacheManagerEventListenerFactoryConfiguration());
		Assert.assertNull(factoryConfiguration.getFullyQualifiedClassPath());
		Assert.assertTrue(
			"cacheManagerPeerListenerFactoryConfigurations should be cleared " +
				"after calling clearListenerConfigrations(configuration)",
			cacheManagerPeerListenerFactoryConfigurations.isEmpty());

		Assert.assertTrue(
			"cacheManagerPeerProviderFactoryConfigurations should be cleared " +
				"after calling clearListenerConfigrations(configuration)",
			cacheManagerPeerProviderFactoryConfigurations.isEmpty());

		Assert.assertTrue(
			"defaultCacheEventListenerConfigurations should be cleared after " +
				"calling clearListenerConfigrations(configuration)",
			defaultCacheEventListenerConfigurations.isEmpty());

		Assert.assertTrue(
			"cacheEventListenerConfigurations should be cleared after " +
				"calling clearListenerConfigrations(configuration)",
			cacheEventListenerConfigurations.isEmpty());
	}

	@Test
	public void testGetConfigurationObjectValuePair() {
		try {
			baseEhcachePortalCacheManagerConfigurator.
				getConfigurationObjectValuePair("", null, true);

			Assert.fail("NullPointerException was not thrown");
		}
		catch (Exception e) {
			Assert.assertEquals(NullPointerException.class, e.getClass());
			Assert.assertEquals("Configuration path is null", e.getMessage());
		}

		ObjectValuePair<Configuration, PortalCacheManagerConfiguration>
			objectValuePair =
				baseEhcachePortalCacheManagerConfigurator.
					getConfigurationObjectValuePair(
						PortalCacheManagerNames.SINGLE_VM,
						SingleVMEhcachePortalCacheManagerConfiguratorTest.
							class.getResource("/ehcache/test-single-vm.xml"),
						true);

		Configuration configuration = objectValuePair.getKey();

		List<FactoryConfiguration>
			cacheManagerPeerListenerFactoryConfigurations =
				configuration.
					getCacheManagerPeerListenerFactoryConfigurations();

		Assert.assertTrue(
			"An empty cacheManagerPeerListenerFactoryConfigurations should " +
				"be returned",
			cacheManagerPeerListenerFactoryConfigurations.isEmpty());

		List<FactoryConfiguration>
			cacheManagerPeerProviderFactoryConfiguration =
				configuration.getCacheManagerPeerProviderFactoryConfiguration();

		Assert.assertTrue(
			"An empty cacheManagerPeerProviderFactoryConfiguration should be " +
				"returned",
			cacheManagerPeerProviderFactoryConfiguration.isEmpty());

		Assert.assertEquals(
			PortalCacheManagerNames.SINGLE_VM, configuration.getName());

		CacheConfiguration defaultCacheConfiguration =
			configuration.getDefaultCacheConfiguration();

		Assert.assertEquals(
			9999, defaultCacheConfiguration.getMaxElementsInMemory());

		Map<String, CacheConfiguration> cacheConfigurations =
			configuration.getCacheConfigurations();

		CacheConfiguration cacheConfiguration = cacheConfigurations.get(
			"com.liferay.portal.kernel.servlet.filters.invoker.InvokerFilter");

		Assert.assertEquals(10000, cacheConfiguration.getMaxElementsInMemory());

		PortalCacheManagerConfiguration portalCacheManagerConfiguration =
			objectValuePair.getValue();

		PortalCacheConfiguration portalCacheConfiguration =
			portalCacheManagerConfiguration.
				getDefaultPortalCacheConfiguration();

		Assert.assertEquals(
			PortalCacheConfiguration.PORTAL_CACHE_NAME_DEFAULT,
			portalCacheConfiguration.getPortalCacheName());
	}

	@Test
	public void testIsRequireSerialization() {
		_assertIsRequireSerializationByCacheConfiguration(
			true, "setOverflowToDisk");
		_assertIsRequireSerializationByCacheConfiguration(
			true, "setOverflowToOffHeap");
		_assertIsRequireSerializationByCacheConfiguration(
			true, "setDiskPersistent");
		_assertIsRequireSerializationByCacheConfiguration(false, null);

		_assertIsRequireSerializationByPersistenceStrategy(
			true, PersistenceConfiguration.Strategy.LOCALTEMPSWAP);
		_assertIsRequireSerializationByPersistenceStrategy(
			true, PersistenceConfiguration.Strategy.LOCALRESTARTABLE);
		_assertIsRequireSerializationByPersistenceStrategy(
			true, PersistenceConfiguration.Strategy.DISTRIBUTED);
		_assertIsRequireSerializationByPersistenceStrategy(
			false, PersistenceConfiguration.Strategy.NONE);
	}

	@Test
	public void testParseCacheEventListenerConfigurations() {
		Set<Properties> portalCacheListenerPropertiesSet =
			baseEhcachePortalCacheManagerConfigurator.
				parseCacheEventListenerConfigurations(null, true);

		Assert.assertTrue(
			"An empty portalCacheListenerPropertiesSet should be returned if " +
				"usingDefault is true",
			portalCacheListenerPropertiesSet.isEmpty());

		CacheEventListenerFactoryConfiguration
			cacheEventListenerFactoryConfiguration =
				new CacheEventListenerFactoryConfiguration();

		cacheEventListenerFactoryConfiguration.setClass(
			SingleVMEhcachePortalCacheManagerConfiguratorTest.class.getName());
		cacheEventListenerFactoryConfiguration.setListenFor("ALL");

		Properties expectedProperties = new Properties();

		expectedProperties.put(
			EhcacheConstants.CACHE_LISTENER_PROPERTIES_KEY_FACTORY_CLASS_NAME,
			SingleVMEhcachePortalCacheManagerConfiguratorTest.class.getName());
		expectedProperties.put(
			PortalCacheConfiguration.PORTAL_CACHE_LISTENER_PROPERTIES_KEY_SCOPE,
			PortalCacheListenerScope.ALL);

		Assert.assertEquals(
			Collections.singleton(expectedProperties),
			baseEhcachePortalCacheManagerConfigurator.
				parseCacheEventListenerConfigurations(
					Collections.singletonList(
						cacheEventListenerFactoryConfiguration),
					false));
	}

	@Test
	public void testParseCacheListenerConfigurations() {
		EhcachePortalCacheConfiguration ehcachePortalCacheConfiguration =
			(EhcachePortalCacheConfiguration)
				baseEhcachePortalCacheManagerConfigurator.
					parseCacheListenerConfigurations(
						new CacheConfiguration(_TEST_CACHE_NAME, 0), true);

		Assert.assertEquals(
			ehcachePortalCacheConfiguration.getPortalCacheName(),
			_TEST_CACHE_NAME);

		Set<Properties> portalCacheListenerPropertiesSet =
			ehcachePortalCacheConfiguration.
				getPortalCacheListenerPropertiesSet();

		Assert.assertTrue(
			"An empty portalCacheListenerPropertiesSet should be returned if " +
				"usingDefault is true",
			portalCacheListenerPropertiesSet.isEmpty());

		Assert.assertFalse(
			"isRequireSerialization() should return false",
			ehcachePortalCacheConfiguration.isRequireSerialization());
	}

	@Test
	public void testParseCacheManagerEventListenerConfigurations() {
		Assert.assertSame(
			Collections.emptySet(),
			baseEhcachePortalCacheManagerConfigurator.
				parseCacheManagerEventListenerConfigurations(null));

		FactoryConfiguration<?> factoryConfiguration =
			new FactoryConfiguration<>();

		factoryConfiguration.setClass(
			SingleVMEhcachePortalCacheManagerConfiguratorTest.class.getName());

		Properties expectedProperties = new Properties();

		expectedProperties.put(
			EhcacheConstants.
				CACHE_MANAGER_LISTENER_PROPERTIES_KEY_FACTORY_CLASS_NAME,
			SingleVMEhcachePortalCacheManagerConfiguratorTest.class.getName());

		Assert.assertEquals(
			Collections.singleton(expectedProperties),
			baseEhcachePortalCacheManagerConfigurator.
				parseCacheManagerEventListenerConfigurations(
					factoryConfiguration));
	}

	@Test
	public void testParseListenerConfigurations() {
		Configuration configuration = new Configuration();

		configuration.addCache(new CacheConfiguration(_TEST_CACHE_NAME, 0));

		PortalCacheManagerConfiguration portalCacheManagerConfiguration =
			baseEhcachePortalCacheManagerConfigurator.
				parseListenerConfigurations(configuration, true);

		Assert.assertNotNull(
			portalCacheManagerConfiguration.getPortalCacheConfiguration(
				_TEST_CACHE_NAME));

		Set<Properties> portalCacheManagerListenerPropertiesSet =
			portalCacheManagerConfiguration.
				getPortalCacheManagerListenerPropertiesSet();

		Assert.assertTrue(
			"An empty portalCacheManagerListenerPropertiesSet should be " +
				"returned if usingDefault is true",
			portalCacheManagerListenerPropertiesSet.isEmpty());

		PortalCacheConfiguration defaultPortalCacheConfiguration =
			portalCacheManagerConfiguration.
				getDefaultPortalCacheConfiguration();

		Assert.assertEquals(
			PortalCacheConfiguration.PORTAL_CACHE_NAME_DEFAULT,
			defaultPortalCacheConfiguration.getPortalCacheName());
	}

	@Test
	public void testParseProperties() {
		Properties properties =
			baseEhcachePortalCacheManagerConfigurator.parseProperties(
				null, StringPool.COMMA);

		Assert.assertTrue(
			"An empty properties should be returned if propertiesString is " +
				"null",
			properties.isEmpty());

		Properties expectedProperties = new Properties();

		expectedProperties.put("key1", "value1");
		expectedProperties.put("key2", "value2");

		Assert.assertEquals(
			expectedProperties,
			baseEhcachePortalCacheManagerConfigurator.parseProperties(
				"key1=value1,key2=value2".concat(StringPool.SPACE),
				StringPool.COMMA));
	}

	protected BaseEhcachePortalCacheManagerConfigurator
		baseEhcachePortalCacheManagerConfigurator;

	private void _assertIsRequireSerializationByCacheConfiguration(
		boolean expectedIsRequireSerialization, String methodName) {

		CacheConfiguration cacheConfiguration = new CacheConfiguration();

		if (methodName != null) {
			ReflectionTestUtil.invoke(
				cacheConfiguration, methodName, new Class<?>[] {boolean.class},
				true);
		}

		Assert.assertEquals(
			expectedIsRequireSerialization,
			baseEhcachePortalCacheManagerConfigurator.isRequireSerialization(
				cacheConfiguration));
	}

	private void _assertIsRequireSerializationByPersistenceStrategy(
		boolean expectedIsRequireSerialization,
		PersistenceConfiguration.Strategy strategy) {

		PersistenceConfiguration persistenceConfiguration =
			new PersistenceConfiguration();

		persistenceConfiguration.setStrategy(String.valueOf(strategy));

		CacheConfiguration cacheConfiguration = new CacheConfiguration();

		cacheConfiguration.addPersistence(persistenceConfiguration);

		Assert.assertEquals(
			expectedIsRequireSerialization,
			baseEhcachePortalCacheManagerConfigurator.isRequireSerialization(
				cacheConfiguration));
	}

	private static final String _TEST_CACHE_NAME = "testCacheName";

}
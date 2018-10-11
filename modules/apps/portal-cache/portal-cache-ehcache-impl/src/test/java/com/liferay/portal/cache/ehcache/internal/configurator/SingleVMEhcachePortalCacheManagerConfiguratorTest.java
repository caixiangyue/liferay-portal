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
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.CodeCoverageAssertor;
import com.liferay.portal.kernel.test.rule.NewEnv;
import com.liferay.portal.kernel.util.ObjectValuePair;
import com.liferay.portal.kernel.util.Props;
import com.liferay.portal.kernel.util.ProxyUtil;
import com.liferay.portal.test.rule.AdviseWith;
import com.liferay.portal.test.rule.AspectJNewEnvTestRule;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheConfiguration.BootstrapCacheLoaderFactoryConfiguration;
import net.sf.ehcache.config.CacheConfiguration.CacheEventListenerFactoryConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.FactoryConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Xiangyue Cai
 */
public class SingleVMEhcachePortalCacheManagerConfiguratorTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			AspectJNewEnvTestRule.INSTANCE,
			new CodeCoverageAssertor() {

				@Override
				public void appendAssertClasses(List<Class<?>> assertClasses) {
					assertClasses.add(
						BaseEhcachePortalCacheManagerConfigurator.class);
				}

			});

	@Test
	public void testClearListenerConfigurationsWithCacheConfiguration() {

		// Test clearListenerConfigurations(null)

		_singleVMEhcachePortalCacheManagerConfigurator.
			clearListenerConfigrations((CacheConfiguration)null);

		// Test cleanListenerConfigurations(CacheConfiguration)

		CacheConfiguration cacheConfiguration = new CacheConfiguration();

		cacheConfiguration.bootstrapCacheLoaderFactory(
			new BootstrapCacheLoaderFactoryConfiguration());

		Assert.assertNotNull(
			cacheConfiguration.getBootstrapCacheLoaderFactoryConfiguration());

		cacheConfiguration.addCacheEventListenerFactory(
			new CacheEventListenerFactoryConfiguration());

		List<?> cacheEventListenerFactoryConfigurations =
			cacheConfiguration.getCacheEventListenerConfigurations();

		Assert.assertFalse(
			cacheEventListenerFactoryConfigurations.toString(),
			cacheEventListenerFactoryConfigurations.isEmpty());

		_singleVMEhcachePortalCacheManagerConfigurator.
			clearListenerConfigrations(cacheConfiguration);

		Assert.assertNull(
			cacheConfiguration.getBootstrapCacheLoaderFactoryConfiguration());
		Assert.assertTrue(
			cacheEventListenerFactoryConfigurations.toString(),
			cacheEventListenerFactoryConfigurations.isEmpty());
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

		CacheConfiguration cacheConfiguration = new CacheConfiguration();

		cacheConfiguration.addCacheEventListenerFactory(
			new CacheEventListenerFactoryConfiguration());

		configuration.setDefaultCacheConfiguration(cacheConfiguration);

		cacheConfiguration = cacheConfiguration.clone();

		cacheConfiguration.setName(_TEST_CACHE_NAME);

		configuration.addCache(cacheConfiguration);

		_singleVMEhcachePortalCacheManagerConfigurator.
			clearListenerConfigrations(configuration);

		Assert.assertSame(
			factoryConfiguration,
			configuration.getCacheManagerEventListenerFactoryConfiguration());
		Assert.assertNull(factoryConfiguration.getFullyQualifiedClassPath());

		List<FactoryConfiguration> listenerFactoryConfigurations =
			configuration.getCacheManagerPeerListenerFactoryConfigurations();

		Assert.assertTrue(
			listenerFactoryConfigurations.toString(),
			listenerFactoryConfigurations.isEmpty());

		List<FactoryConfiguration> providerFactoryConfigurations =
			configuration.getCacheManagerPeerProviderFactoryConfiguration();

		Assert.assertTrue(
			providerFactoryConfigurations.toString(),
			providerFactoryConfigurations.isEmpty());

		CacheConfiguration defaultCacheConfiguration =
			configuration.getDefaultCacheConfiguration();

		List<?> factoryConfigurations =
			defaultCacheConfiguration.getCacheEventListenerConfigurations();

		Assert.assertTrue(
			factoryConfigurations.toString(), factoryConfigurations.isEmpty());

		Map<String, CacheConfiguration> cacheConfigurations =
			configuration.getCacheConfigurations();

		for (CacheConfiguration testCacheConfiguration :
				cacheConfigurations.values()) {

			factoryConfigurations =
				testCacheConfiguration.getCacheEventListenerConfigurations();

			Assert.assertTrue(
				factoryConfigurations.toString(),
				factoryConfigurations.isEmpty());
		}
	}

	@Test
	public void testGetConfigurationObjectValuePair() {
		try {
			_singleVMEhcachePortalCacheManagerConfigurator.
				getConfigurationObjectValuePair("", null, true);

			Assert.fail("NullPointerException was not thrown");
		}
		catch (Exception e) {
			Assert.assertEquals(NullPointerException.class, e.getClass());
			Assert.assertEquals("Configuration path is null", e.getMessage());
		}

		ObjectValuePair<Configuration, PortalCacheManagerConfiguration>
			objectValuePair =
				_singleVMEhcachePortalCacheManagerConfigurator.
					getConfigurationObjectValuePair(
						PortalCacheManagerNames.SINGLE_VM,
						SingleVMEhcachePortalCacheManagerConfiguratorTest.
							class.getResource("/ehcache/test-single-vm.xml"),
						true);

		Configuration configuration = objectValuePair.getKey();

		List<?> listenerFactoryConfigurations =
			configuration.getCacheManagerPeerListenerFactoryConfigurations();

		Assert.assertTrue(
			listenerFactoryConfigurations.toString(),
			listenerFactoryConfigurations.isEmpty());

		List<?> providerFactoryConfigurations =
			configuration.getCacheManagerPeerProviderFactoryConfiguration();

		Assert.assertTrue(
			providerFactoryConfigurations.toString(),
			providerFactoryConfigurations.isEmpty());

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
	public void testIsRequireSerializationByCacheConfiguration() {
		CacheConfiguration cacheConfiguration = new CacheConfiguration();

		cacheConfiguration.setOverflowToDisk(true);

		Assert.assertTrue(
			"isRequireSerialization() should be true if overflowToDisk is " +
				"set true",
			_singleVMEhcachePortalCacheManagerConfigurator.
				isRequireSerialization(cacheConfiguration));

		cacheConfiguration.setOverflowToDisk(false);
		cacheConfiguration.setOverflowToOffHeap(true);

		Assert.assertTrue(
			"isRequireSerialization() should be true if overflowToOffHeap is " +
				"set true",
			_singleVMEhcachePortalCacheManagerConfigurator.
				isRequireSerialization(cacheConfiguration));

		cacheConfiguration.setOverflowToOffHeap(false);
		cacheConfiguration.setDiskPersistent(true);

		Assert.assertTrue(
			"isRequireSerialization() should be true if diskPersistent is " +
				"set true",
			_singleVMEhcachePortalCacheManagerConfigurator.
				isRequireSerialization(cacheConfiguration));

		cacheConfiguration.setDiskPersistent(false);

		Assert.assertFalse(
			"isRequireSerialization() should be false if overflowToDisk " +
				"overflowToOffHeap and diskPersistent is set false",
			_singleVMEhcachePortalCacheManagerConfigurator.
				isRequireSerialization(cacheConfiguration));
	}

	@Test
	public void testIsRequireSerializationByPersistenceConfiguration() {
		_assertIsRequireSerializationByPersistenceConfiguration(
			true, PersistenceConfiguration.Strategy.LOCALTEMPSWAP);
		_assertIsRequireSerializationByPersistenceConfiguration(
			true, PersistenceConfiguration.Strategy.LOCALRESTARTABLE);
		_assertIsRequireSerializationByPersistenceConfiguration(
			true, PersistenceConfiguration.Strategy.DISTRIBUTED);
		_assertIsRequireSerializationByPersistenceConfiguration(
			false, PersistenceConfiguration.Strategy.NONE);
	}

	@Test
	public void testParseCacheEventListenerConfigurations() {
		Set<Properties> emptySet =
			_singleVMEhcachePortalCacheManagerConfigurator.
				parseCacheEventListenerConfigurations(null, true);

		Assert.assertTrue(emptySet.toString(), emptySet.isEmpty());

		CacheEventListenerFactoryConfiguration
			cacheEventListenerFactoryConfiguration =
				new CacheEventListenerFactoryConfiguration();

		cacheEventListenerFactoryConfiguration.setClass(
			SingleVMEhcachePortalCacheManagerConfiguratorTest.class.getName());
		cacheEventListenerFactoryConfiguration.setListenFor("ALL");

		List<CacheEventListenerFactoryConfiguration>
			cacheEventListenerFactoryConfigurationList = new ArrayList<>();

		cacheEventListenerFactoryConfigurationList.add(
			cacheEventListenerFactoryConfiguration);

		Set<Properties> portalCacheListenerPropertiesSet =
			_singleVMEhcachePortalCacheManagerConfigurator.
				parseCacheEventListenerConfigurations(
					cacheEventListenerFactoryConfigurationList, false);

		Assert.assertEquals(
			portalCacheListenerPropertiesSet.toString(), 1,
			portalCacheListenerPropertiesSet.size());

		for (Properties properties : portalCacheListenerPropertiesSet) {
			Assert.assertEquals(2, properties.size());

			Assert.assertEquals(
				SingleVMEhcachePortalCacheManagerConfiguratorTest.class.
					getName(),
				properties.get(
					EhcacheConstants.
						CACHE_LISTENER_PROPERTIES_KEY_FACTORY_CLASS_NAME));
			Assert.assertEquals(
				PortalCacheListenerScope.ALL,
				properties.get(
					PortalCacheConfiguration.
						PORTAL_CACHE_LISTENER_PROPERTIES_KEY_SCOPE));
		}
	}

	@Test
	public void testParseCacheListenerConfigurations() {
		EhcachePortalCacheConfiguration ehcachePortalCacheConfiguration =
			(EhcachePortalCacheConfiguration)
				_singleVMEhcachePortalCacheManagerConfigurator.
					parseCacheListenerConfigurations(
						new CacheConfiguration(_TEST_CACHE_NAME, 0), true);

		Assert.assertEquals(
			ehcachePortalCacheConfiguration.getPortalCacheName(),
			_TEST_CACHE_NAME);
		Assert.assertFalse(
			"isRequireSerialization() should be false if cacheConfiguration " +
				"is not set anything after being created by constructor",
			ehcachePortalCacheConfiguration.isRequireSerialization());

		Set<Properties> portalCacheListenerPropertiesSet =
			ehcachePortalCacheConfiguration.
				getPortalCacheListenerPropertiesSet();

		Assert.assertTrue(
			portalCacheListenerPropertiesSet.toString(),
			portalCacheListenerPropertiesSet.isEmpty());
	}

	@Test
	public void testParseCacheManagerEventListenerConfigurations() {
		Set<Properties> cacheManagerEventListenerConfigurations =
			_singleVMEhcachePortalCacheManagerConfigurator.
				parseCacheManagerEventListenerConfigurations(null);

		Assert.assertTrue(
			cacheManagerEventListenerConfigurations.toString(),
			cacheManagerEventListenerConfigurations.isEmpty());

		FactoryConfiguration<?> factoryConfiguration =
			new FactoryConfiguration<>();

		factoryConfiguration.setClass(
			SingleVMEhcachePortalCacheManagerConfiguratorTest.class.getName());

		cacheManagerEventListenerConfigurations =
			_singleVMEhcachePortalCacheManagerConfigurator.
				parseCacheManagerEventListenerConfigurations(
					factoryConfiguration);

		Assert.assertEquals(
			cacheManagerEventListenerConfigurations.toString(), 1,
			cacheManagerEventListenerConfigurations.size());

		for (Properties properties : cacheManagerEventListenerConfigurations) {
			Assert.assertEquals(
				SingleVMEhcachePortalCacheManagerConfiguratorTest.class.
					getName(),
				properties.getProperty(EhcacheConstants.
					CACHE_MANAGER_LISTENER_PROPERTIES_KEY_FACTORY_CLASS_NAME));
		}
	}

	@Test
	public void testParseListenerConfigurations() {
		Configuration configuration = new Configuration();

		configuration.addCache(new CacheConfiguration(_TEST_CACHE_NAME, 0));

		PortalCacheManagerConfiguration portalCacheManagerConfiguration =
			_singleVMEhcachePortalCacheManagerConfigurator.
				parseListenerConfigurations(configuration, true);

		Assert.assertNotNull(
			portalCacheManagerConfiguration.getPortalCacheConfiguration(
				_TEST_CACHE_NAME));

		Set<Properties> cacheManagerListenerPropertiesSet =
			portalCacheManagerConfiguration.
				getPortalCacheManagerListenerPropertiesSet();

		Assert.assertTrue(
			cacheManagerListenerPropertiesSet.toString(),
			cacheManagerListenerPropertiesSet.isEmpty());

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
			_singleVMEhcachePortalCacheManagerConfigurator.parseProperties(
				null, StringPool.COMMA);

		Assert.assertTrue(properties.toString(), properties.isEmpty());

		Properties expectedProperties = new Properties();

		expectedProperties.put("key1", "value1");
		expectedProperties.put("key2", "value2");

		Assert.assertEquals(
			expectedProperties,
			_singleVMEhcachePortalCacheManagerConfigurator.parseProperties(
				"key1=value1,key2=value2".concat(StringPool.SPACE),
				StringPool.COMMA));
	}

	@AdviseWith(adviceClasses = UnsyncStringReaderAdvice.class)
	@NewEnv(type = NewEnv.Type.CLASSLOADER)
	@Test
	public void testParsePropertiesException() {
		try {
			_singleVMEhcachePortalCacheManagerConfigurator.parseProperties(
				"key1=value1", StringPool.COMMA);

			Assert.fail("RuntimeException was not thrown");
		}
		catch (RuntimeException re) {
			Assert.assertSame(_IO_EXCEPTION, re.getCause());
		}
	}

	@Test
	public void testSetProps() {
		Props props = (Props)ProxyUtil.newProxyInstance(
			Props.class.getClassLoader(), new Class<?>[] {Props.class},
			(proxy, method, args) -> null);

		_singleVMEhcachePortalCacheManagerConfigurator.setProps(props);

		Assert.assertSame(
			props, _singleVMEhcachePortalCacheManagerConfigurator.props);
	}

	@Aspect
	public static class UnsyncStringReaderAdvice {

		@Around(
			"execution(public int com.liferay.portal.kernel.io.unsync." +
				"UnsyncStringReader.read(char[]))"
		)
		public Object read(ProceedingJoinPoint proceedingJoinPoint)
			throws IOException {

			throw _IO_EXCEPTION;
		}

	}

	private void _assertIsRequireSerializationByPersistenceConfiguration(
		boolean expected, PersistenceConfiguration.Strategy strategy) {

		PersistenceConfiguration persistenceConfiguration =
			new PersistenceConfiguration();

		persistenceConfiguration.setStrategy(String.valueOf(strategy));

		CacheConfiguration cacheConfiguration = new CacheConfiguration();

		cacheConfiguration.addPersistence(persistenceConfiguration);

		Assert.assertEquals(
			expected,
			_singleVMEhcachePortalCacheManagerConfigurator.
				isRequireSerialization(cacheConfiguration));
	}

	private static final IOException _IO_EXCEPTION = new IOException();

	private static final String _TEST_CACHE_NAME = "testCacheName";

	private final SingleVMEhcachePortalCacheManagerConfigurator
		_singleVMEhcachePortalCacheManagerConfigurator =
			new SingleVMEhcachePortalCacheManagerConfigurator();

}
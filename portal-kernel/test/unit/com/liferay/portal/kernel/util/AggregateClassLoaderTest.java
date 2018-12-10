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

package com.liferay.portal.kernel.util;

import com.liferay.petra.memory.EqualityWeakReference;
import com.liferay.portal.kernel.test.GCUtil;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.rule.CodeCoverageAssertor;

import java.io.File;
import java.io.IOException;

import java.net.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author Xiangyue Cai
 */
public class AggregateClassLoaderTest {

	@ClassRule
	public static final CodeCoverageAssertor codeCoverageAssertor =
		CodeCoverageAssertor.INSTANCE;

	@Test
	public void testAddClassLoaderWithClassloader() {
		AggregateClassLoader aggregateClassLoader = new AggregateClassLoader(
			_testClassLoader);

		_testAddClassLoader(
			aggregateClassLoader,
			new ClassLoader[] {
				_testExceptionClassLoader, _testExceptionClassLoader
			},
			_testExceptionClassLoader);

		_testAddClassLoader(
			aggregateClassLoader, new ClassLoader[] {aggregateClassLoader},
			_testExceptionClassLoader);

		_testAddClassLoader(
			aggregateClassLoader, new ClassLoader[] {_testClassLoader},
			_testExceptionClassLoader);
	}

	@Test
	public void testAddClassLoaderWithClassloaders() {
		_testAddClassLoader(
			new AggregateClassLoader(_testClassLoader),
			_testExceptionClassLoader, _testClassLoader,
			_testExceptionClassLoader);
	}

	@Test
	public void testAddClassLoaderWithCollectionClassloaders() {
		_testAddClassLoader(
			new AggregateClassLoader(_testClassLoader),
			new ArrayList<ClassLoader>() {
				{
					add(_testClassLoader);
					add(_testExceptionClassLoader);
				}
			},
			_testExceptionClassLoader);
	}

	@Test
	public void testConstructor() {
		AggregateClassLoader aggregateClassLoader = new AggregateClassLoader(
			_testClassLoader);

		Assert.assertEquals(_testClassLoader, aggregateClassLoader.getParent());
	}

	@Test
	public void testEquals() {
		AggregateClassLoader aggregateClassLoader1 = new AggregateClassLoader(
			AggregateClassLoader.class.getClassLoader());

		Assert.assertTrue(
			"equals() should be return true if they are the same reference",
			aggregateClassLoader1.equals(aggregateClassLoader1));

		Assert.assertFalse(
			"equals() should be return false if the object is not instance " +
				"of AggregateClassLoader",
			aggregateClassLoader1.equals(_testClassLoader));

		ClassLoader aggregateClassLoader2 =
			AggregateClassLoader.getAggregateClassLoader(
				null, _testClassLoader);

		Assert.assertFalse(
			"equals() should be return false if they contain different " +
				"_classLoaderReferences",
			aggregateClassLoader1.equals(aggregateClassLoader2));

		aggregateClassLoader2 = new AggregateClassLoader(null);

		Assert.assertFalse(
			"equals() should be return false if this object's parent " +
				"classloader is not null and they contain different parent " +
					"classloader",
			aggregateClassLoader1.equals(aggregateClassLoader2));

		Assert.assertFalse(
			"equals() should be return false if this object's parent " +
				"classloader is null and the other object's parent " +
					"classloader is not null",
			aggregateClassLoader2.equals(aggregateClassLoader1));

		aggregateClassLoader1 = new AggregateClassLoader(null);

		Assert.assertTrue(
			"equals() should be return true if they contain same " +
				"_classLoaderReferences and their parent classloader is null",
			aggregateClassLoader1.equals(aggregateClassLoader2));
	}

	@Test
	public void testFindClass() throws Exception {
		AggregateClassLoader aggregateClassLoader = new AggregateClassLoader(
			null);

		try {
			aggregateClassLoader.findClass(TestClassLoader.class.getName());
		}
		catch (ClassNotFoundException cnfe) {
			Assert.assertEquals(
				"Unable to find class ".concat(TestClassLoader.class.getName()),
				cnfe.getMessage());
		}

		aggregateClassLoader.addClassLoader(_testExceptionClassLoader);

		try {
			aggregateClassLoader.findClass(TestClassLoader.class.getName());
		}
		catch (ClassNotFoundException cnfe) {
			Assert.assertEquals(
				"Unable to find class ".concat(TestClassLoader.class.getName()),
				cnfe.getMessage());
		}

		aggregateClassLoader.addClassLoader(_testClassLoader);

		Assert.assertEquals(
			aggregateClassLoader.findClass(TestClassLoader.class.getName()),
			TestClassLoader.class);
	}

	@Test
	public void testGetAggregateClassLoaderWithClassLoaders() {
		Assert.assertNull(
			AggregateClassLoader.getAggregateClassLoader(new ClassLoader[0]));

		ClassLoader classLoader = AggregateClassLoader.getAggregateClassLoader(
			new ClassLoader[] {
				AggregateClassLoaderTest.class.getClassLoader()
			});

		Assert.assertEquals(AggregateClassLoader.class, classLoader.getClass());
	}

	@Test
	public void testGetAggregateClassLoaderWithParentClassLoader() {
		Assert.assertEquals(
			_testClassLoader,
			AggregateClassLoader.getAggregateClassLoader(_testClassLoader));

		Assert.assertEquals(
			new AggregateClassLoader(_testClassLoader),
			AggregateClassLoader.getAggregateClassLoader(
				new AggregateClassLoader(_testClassLoader),
				new AggregateClassLoader(_testClassLoader)));

		Assert.assertEquals(
			AggregateClassLoader.getAggregateClassLoader(
				AggregateClassLoader.class.getClassLoader(), _testClassLoader),
			AggregateClassLoader.getAggregateClassLoader(
				AggregateClassLoader.getAggregateClassLoader(
					AggregateClassLoader.class.getClassLoader(),
					_testClassLoader),
				_testClassLoader));

		Assert.assertEquals(
			new AggregateClassLoader(
				AggregateClassLoader.class.getClassLoader()),
			AggregateClassLoader.getAggregateClassLoader(
				new AggregateClassLoader(
					AggregateClassLoader.class.getClassLoader()),
				AggregateClassLoader.class.getClassLoader()));

		Assert.assertEquals(
			new AggregateClassLoader(
				AggregateClassLoader.class.getClassLoader()),
			AggregateClassLoader.getAggregateClassLoader(
				AggregateClassLoader.class.getClassLoader(),
				AggregateClassLoader.class.getClassLoader()));
	}

	@Test
	public void testGetClassLoaders() throws Exception {
		AggregateClassLoader aggregateClassLoader = new AggregateClassLoader(
			null);

		_testGetClassLoaders(
			aggregateClassLoader, _testClassLoader, new TestClassLoader());

		GCUtil.gc(true);

		_testGetClassLoaders(aggregateClassLoader, _testClassLoader);
	}

	@Test
	public void testGetResource() throws Exception {
		AggregateClassLoader aggregateClassLoader = new AggregateClassLoader(
			null);

		aggregateClassLoader.addClassLoader(_testExceptionClassLoader);

		Assert.assertNull(aggregateClassLoader.getResource(""));

		aggregateClassLoader.addClassLoader(_testClassLoader);

		File directory = new File(".");

		Assert.assertEquals(
			new URL(
				StringBundler.concat(
					"file:", directory.getCanonicalPath(),
					"/portal-kernel/test-coverage/")),
			aggregateClassLoader.getResource(""));
	}

	@Test
	public void testGetResources() throws IOException {
		AggregateClassLoader aggregateClassLoader = new AggregateClassLoader(
			AggregateClassLoader.class.getClassLoader());

		aggregateClassLoader.addClassLoader(_testClassLoader);

		Enumeration<URL> enumeration = aggregateClassLoader.getResources("");

		Assert.assertEquals(
			new ArrayList<URL>() {
				{
					addAll(Collections.list(_testClassLoader.getResources("")));
					ClassLoader classLoader =
						AggregateClassLoader.class.getClassLoader();

					addAll(Collections.list(classLoader.getResources("")));
				}
			},
			Collections.list(enumeration));

		aggregateClassLoader.addClassLoader(_testExceptionClassLoader);

		try {
			aggregateClassLoader.getResources("");
		}
		catch (Exception e) {
			Assert.assertTrue(
				"getResources() throws IOException if classloader's " +
					"getResources() throws IOException",
				e.getCause() instanceof IOException);
		}

		aggregateClassLoader = new AggregateClassLoader(null);

		try {
			aggregateClassLoader.getResources("");
		}
		catch (Exception e) {
			Assert.assertTrue(
				"getResources() throws NullPointerException if parent class " +
					"loader was null",
				e.getCause() instanceof NullPointerException);
		}
	}

	@Test
	public void testHashCode() {
	}

	@Test
	public void testLoadClass() throws Exception {
		AggregateClassLoader aggregateClassLoader = new AggregateClassLoader(
			null);

		try {
			aggregateClassLoader.loadClass(
				TestClassLoader.class.getName(), false);
		}
		catch (ClassNotFoundException cnfe) {
			Assert.assertEquals(
				"Unable to load class ".concat(TestClassLoader.class.getName()),
				cnfe.getMessage());
		}

		aggregateClassLoader.addClassLoader(_testExceptionClassLoader);

		aggregateClassLoader.addClassLoader(_testClassLoader);

		Assert.assertEquals(
			aggregateClassLoader.loadClass(
				TestClassLoader.class.getName(), false),
			TestClassLoader.class);

		Assert.assertEquals(
			aggregateClassLoader.loadClass(
				TestClassLoader.class.getName(), true),
			TestClassLoader.class);
	}

	private void _assertAddClassLoader(
		AggregateClassLoader aggregateClassLoader,
		ClassLoader expectedClassLoader) {

		List<EqualityWeakReference<ClassLoader>> classLoaderReferences =
			ReflectionTestUtil.getFieldValue(
				aggregateClassLoader, "_classLoaderReferences");

		Assert.assertEquals(
			Collections.singletonList(
				new EqualityWeakReference<>(expectedClassLoader)),
			classLoaderReferences);
	}

	private void _testAddClassLoader(
		AggregateClassLoader aggregateClassLoader,
		ClassLoader expectedClassLoader, ClassLoader... addedClassLoader) {

		aggregateClassLoader.addClassLoader(addedClassLoader);

		_assertAddClassLoader(aggregateClassLoader, expectedClassLoader);
	}

	private void _testAddClassLoader(
		AggregateClassLoader aggregateClassLoader,
		ClassLoader[] addedClassLoader, ClassLoader expectedClassLoader) {

		for (ClassLoader classLoader : addedClassLoader) {
			aggregateClassLoader.addClassLoader(classLoader);
		}

		_assertAddClassLoader(aggregateClassLoader, expectedClassLoader);
	}

	private void _testAddClassLoader(
		AggregateClassLoader aggregateClassLoader,
		List<ClassLoader> addedClassLoader, ClassLoader expectedClassLoader) {

		aggregateClassLoader.addClassLoader(addedClassLoader);

		_assertAddClassLoader(aggregateClassLoader, expectedClassLoader);
	}

	private void _testGetClassLoaders(
		AggregateClassLoader aggregateClassLoader,
		ClassLoader... expectedClassLoaders) {

		for (ClassLoader classLoader : expectedClassLoaders) {
			aggregateClassLoader.addClassLoader(classLoader);
		}

		Assert.assertEquals(
			Arrays.asList(expectedClassLoaders),
			aggregateClassLoader.getClassLoaders());
	}

	private final TestClassLoader _testClassLoader = new TestClassLoader();
	private final TestExceptionClassLoader _testExceptionClassLoader =
		new TestExceptionClassLoader();

	private class TestClassLoader extends ClassLoader {

		@Override
		protected Class<?> findClass(String name) {
			return TestClassLoader.class;
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) {
			return TestClassLoader.class;
		}

	}

	private class TestExceptionClassLoader extends ClassLoader {

		@Override
		public URL getResource(String name) {
			throw new RuntimeException();
		}

		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			throw new IOException();
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve)
			throws ClassNotFoundException {

			throw new ClassNotFoundException();
		}

	}

}
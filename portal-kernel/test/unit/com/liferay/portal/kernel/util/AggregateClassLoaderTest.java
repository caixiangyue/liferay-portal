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
	public void testAddClassLoader() {

		// parent class loader is null

		_testAddClassLoader(
			new ClassLoader[] {_testClassLoader1, _testClassLoader2},
			new ClassLoader[] {_testClassLoader1, _testClassLoader2}, null);

		_testAddClassLoader(
			new ClassLoader[] {_testClassLoader2},
			new ClassLoader[] {_testClassLoader2, _testClassLoader2}, null);

		_testAddClassLoader(
			new ClassLoader[] {_testClassLoader1},
			new ClassLoader[] {new AggregateClassLoader(_testClassLoader1)},
			null);

		_testAddClassLoader(
			new ClassLoader[] {_testClassLoader1, _testClassLoader2},
			new ClassLoader[] {
				AggregateClassLoader.getAggregateClassLoader(
					_testClassLoader1, _testClassLoader2)
			},
			null);

		// parent class loader is not null

		_testAddClassLoader(
			new ClassLoader[] {_testClassLoader2},
			new ClassLoader[] {_testClassLoader1, _testClassLoader2},
			_testClassLoader1);

		_testAddClassLoader(
			new ClassLoader[] {_testClassLoader2},
			new ClassLoader[] {_testClassLoader2, _testClassLoader2},
			_testClassLoader1);

		_testAddClassLoader(
			new ClassLoader[0],
			new ClassLoader[] {new AggregateClassLoader(_testClassLoader1)},
			_testClassLoader1);

		_testAddClassLoader(
			new ClassLoader[] {_testClassLoader2},
			new ClassLoader[] {
				AggregateClassLoader.getAggregateClassLoader(
					_testClassLoader1, _testClassLoader2)
			},
			_testClassLoader1);
	}

	@Test
	public void testConstructor() {
		AggregateClassLoader aggregateClassLoader = new AggregateClassLoader(
			_testClassLoader1);

		Assert.assertSame(_testClassLoader1, aggregateClassLoader.getParent());
	}

	@Test
	public void testEquals() {
		AggregateClassLoader aggregateClassLoader1 = new AggregateClassLoader(
			_testClassLoader1);

		Assert.assertTrue(
			"equals() should be return true if they are the same reference",
			aggregateClassLoader1.equals(aggregateClassLoader1));

		Assert.assertFalse(
			"equals() should be return false if the object is not instance " +
				"of AggregateClassLoader",
			aggregateClassLoader1.equals(_testClassLoader2));

		ClassLoader aggregateClassLoader2 =
			AggregateClassLoader.getAggregateClassLoader(
				null, _testClassLoader1);

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

		aggregateClassLoader.addClassLoader(_testClassLoader1);

		Assert.assertSame(
			aggregateClassLoader.findClass(TestClassLoader.class.getName()),
			TestClassLoader.class);

		Object findClassMethod = ReflectionTestUtil.getAndSetFieldValue(
			AggregateClassLoader.class, "_FIND_CLASS_METHOD", null);

		try {
			aggregateClassLoader.findClass(TestClassLoader.class.getName());
		}
		catch (ClassNotFoundException cnfe) {
			Assert.assertEquals(
				"Unable to find class ".concat(TestClassLoader.class.getName()),
				cnfe.getMessage());
		}
		finally {
			ReflectionTestUtil.setFieldValue(
				AggregateClassLoader.class, "_FIND_CLASS_METHOD",
				findClassMethod);
		}
	}

	@Test
	public void testGetAggregateClassLoaderWithClassLoaders() {
		Assert.assertNull(
			AggregateClassLoader.getAggregateClassLoader(new ClassLoader[0]));

		_assertAggregateClassLoader(
			_testClassLoader1, new ClassLoader[0],
			(AggregateClassLoader)AggregateClassLoader.getAggregateClassLoader(
				new ClassLoader[] {_testClassLoader1}));
	}

	@Test
	public void testGetAggregateClassLoaderWithParentClassLoader() {
		Assert.assertSame(
			_testClassLoader1,
			AggregateClassLoader.getAggregateClassLoader(_testClassLoader1));

		_assertAggregateClassLoader(
			_testClassLoader1, new ClassLoader[0],
			(AggregateClassLoader)AggregateClassLoader.getAggregateClassLoader(
				new AggregateClassLoader(_testClassLoader1),
				new AggregateClassLoader(_testClassLoader1)));

		_assertAggregateClassLoader(
			_testClassLoader1, new ClassLoader[] {_testClassLoader2},
			(AggregateClassLoader)AggregateClassLoader.getAggregateClassLoader(
				AggregateClassLoader.getAggregateClassLoader(
					_testClassLoader1, _testClassLoader2),
				_testClassLoader2));

		_assertAggregateClassLoader(
			_testClassLoader1, new ClassLoader[0],
			(AggregateClassLoader)AggregateClassLoader.getAggregateClassLoader(
				new AggregateClassLoader(_testClassLoader1),
				_testClassLoader1));

		_assertAggregateClassLoader(
			_testClassLoader1, new ClassLoader[] {_testClassLoader2},
			(AggregateClassLoader)AggregateClassLoader.getAggregateClassLoader(
				_testClassLoader1, _testClassLoader2));
	}

	@Test
	public void testGetClassLoaders() throws Exception {
		AggregateClassLoader aggregateClassLoader = new AggregateClassLoader(
			null);

		_testGetClassLoaders(
			aggregateClassLoader, _testClassLoader1, new TestClassLoader());

		GCUtil.gc(true);

		_testGetClassLoaders(aggregateClassLoader, _testClassLoader1);
	}

	@Test
	public void testGetResource() throws Exception {
		AggregateClassLoader aggregateClassLoader = new AggregateClassLoader(
			null);

		// trigger InvocationTargetException

		aggregateClassLoader.addClassLoader(_testExceptionClassLoader);

		Assert.assertNull(aggregateClassLoader.getResource(""));

		aggregateClassLoader.addClassLoader(_testClassLoader1);

		File directory = new File(".");

		Assert.assertEquals(
			new URL(
				StringBundler.concat(
					"file:", directory.getCanonicalPath(),
					"/portal-kernel/test-coverage/")),
			aggregateClassLoader.getResource(""));

		// trigger NullPointerException

		Object getResourceMethod = ReflectionTestUtil.getAndSetFieldValue(
			AggregateClassLoader.class, "_GET_RESOURCE_METHOD", null);

		try {
			Assert.assertNull(aggregateClassLoader.getResource(""));
		}
		finally {
			ReflectionTestUtil.setFieldValue(
				AggregateClassLoader.class, "_GET_RESOURCE_METHOD",
				getResourceMethod);
		}
	}

	@Test
	public void testGetResources() throws Exception {
		AggregateClassLoader aggregateClassLoader =
			(AggregateClassLoader)AggregateClassLoader.getAggregateClassLoader(
				_testClassLoader1, _testClassLoader2);

		Assert.assertEquals(
			new ArrayList<URL>() {
				{
					addAll(
						Collections.list(_testClassLoader1.getResources("")));
					addAll(
						Collections.list(_testClassLoader2.getResources("")));
				}
			},
			Collections.list(aggregateClassLoader.getResources("")));

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
		AggregateClassLoader aggregateClassLoader1 = new AggregateClassLoader(
			null);

		try {
			aggregateClassLoader1.loadClass(
				TestClassLoader.class.getName(), false);
		}
		catch (ClassNotFoundException cnfe) {
			Assert.assertEquals(
				"Unable to load class ".concat(TestClassLoader.class.getName()),
				cnfe.getMessage());
		}

		aggregateClassLoader1.addClassLoader(_testExceptionClassLoader);

		try {
			aggregateClassLoader1.loadClass(
				TestClassLoader.class.getName(), false);
		}
		catch (ClassNotFoundException cnfe) {
			Assert.assertEquals(
				"Unable to load class ".concat(TestClassLoader.class.getName()),
				cnfe.getMessage());
		}

		AggregateClassLoader aggregateClassLoader2 =
			(AggregateClassLoader)AggregateClassLoader.getAggregateClassLoader(
				null, _testClassLoader1);

		Assert.assertSame(
			aggregateClassLoader2.loadClass(
				TestClassLoader.class.getName(), true),
			TestClassLoader.class);
		Assert.assertSame(
			aggregateClassLoader2.loadClass(
				TestClassLoader.class.getName(), false),
			TestClassLoader.class);
	}

	private void _assertAggregateClassLoader(
		ClassLoader expectedParentClassLoader,
		ClassLoader[] expectedClassLoaders,
		AggregateClassLoader aggregateClassLoader) {

		Assert.assertSame(
			expectedParentClassLoader, aggregateClassLoader.getParent());

		_assertAggregatedClassLoaders(
			expectedClassLoaders, aggregateClassLoader);
	}

	private void _assertAggregatedClassLoaders(
		ClassLoader[] expectedClassLoaders,
		AggregateClassLoader aggregateClassLoader) {

		List<EqualityWeakReference<ClassLoader>> classLoaderReferences =
			ReflectionTestUtil.getFieldValue(
				aggregateClassLoader, "_classLoaderReferences");

		List<ClassLoader> classLoaders = new ArrayList<>(
			classLoaderReferences.size());

		for (EqualityWeakReference<ClassLoader> classLoaderReference :
				classLoaderReferences) {

			classLoaders.add(classLoaderReference.get());
		}

		Assert.assertArrayEquals(expectedClassLoaders, classLoaders.toArray());
	}

	private void _testAddClassLoader(
		ClassLoader[] expectedClassLoaders, ClassLoader[] classLoaders,
		ClassLoader parentClassLoader) {

		AggregateClassLoader aggregateClassLoader1 = new AggregateClassLoader(
			parentClassLoader);

		aggregateClassLoader1.addClassLoader(classLoaders);

		_assertAggregatedClassLoaders(
			expectedClassLoaders, aggregateClassLoader1);

		AggregateClassLoader aggregateClassLoader2 = new AggregateClassLoader(
			parentClassLoader);

		aggregateClassLoader2.addClassLoader(Arrays.asList(classLoaders));

		_assertAggregatedClassLoaders(
			expectedClassLoaders, aggregateClassLoader2);
	}

	private void _testGetClassLoaders(
		AggregateClassLoader aggregateClassLoader,
		ClassLoader... expectedClassLoaders) {

		aggregateClassLoader.addClassLoader(expectedClassLoaders);

		Assert.assertEquals(
			Arrays.asList(expectedClassLoaders),
			aggregateClassLoader.getClassLoaders());
	}

	private final TestClassLoader _testClassLoader1 = new TestClassLoader();
	private final TestClassLoader _testClassLoader2 = new TestClassLoader();
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
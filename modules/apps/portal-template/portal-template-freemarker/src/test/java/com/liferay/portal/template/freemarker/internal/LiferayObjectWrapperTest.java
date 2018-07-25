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

package com.liferay.portal.template.freemarker.internal;

import com.liferay.portal.kernel.templateparser.TemplateNode;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.CodeCoverageAssertor;
import com.liferay.portal.kernel.test.rule.NewEnv;
import com.liferay.portal.test.aspects.ReflectionUtilAdvice;
import com.liferay.portal.test.rule.AdviseWith;
import com.liferay.portal.test.rule.AspectJNewEnvTestRule;

import com.sun.org.apache.xerces.internal.impl.xs.opti.DefaultNode;

import freemarker.ext.beans.EnumerationModel;
import freemarker.ext.beans.MapModel;
import freemarker.ext.beans.ResourceBundleModel;
import freemarker.ext.beans.StringModel;

import freemarker.template.SimpleScalar;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateModel;

import java.lang.reflect.Field;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mockito;

/**
 * @author Xiangyue Cai
 */
public class LiferayObjectWrapperTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			AspectJNewEnvTestRule.INSTANCE, CodeCoverageAssertor.INSTANCE);

	@Before
	public void setUp() {
		_liferayObjectWrapper = new LiferayObjectWrapper();
	}

	@Test
	public void testHandleUnknownTypeAllowedCollection() {
		ObjectCollection objectCollection = new ObjectCollection();

		Object returnResult = _liferayObjectWrapper.handleUnknownType(
			objectCollection);

		Assert.assertTrue(returnResult instanceof SimpleSequence);
	}

	@Test
	public void testHandleUnknownTypeAllowedDate() {
		Date objectDate = Mockito.mock(Date.class);

		Object returnResult = _liferayObjectWrapper.handleUnknownType(
			objectDate);

		Assert.assertTrue(returnResult instanceof StringModel);
	}

	@Test
	public void testHandleUnknownTypeAllowedEnumeration() {
		Enumeration enumeration = Mockito.mock(Enumeration.class);

		Object returnResult = _liferayObjectWrapper.handleUnknownType(
			enumeration);

		Assert.assertTrue(returnResult instanceof EnumerationModel);
	}

	@Test
	public void testHandleUnknownTypeAllowedMap() {
		Map map = Mockito.mock(Map.class);

		Object returnResult = _liferayObjectWrapper.handleUnknownType(map);

		Assert.assertTrue(returnResult instanceof MapModel);
	}

	@Test
	public void testHandleUnknownTypeAllowedNode() {
		DefaultNode defaultNode = Mockito.mock(DefaultNode.class);

		Object returnResult = _liferayObjectWrapper.handleUnknownType(
			defaultNode);

		Assert.assertNull(returnResult);
	}

	@Test
	public void testHandleUnknownTypeAllowedResourceBundle() {
		ResourceBundle resourceBundle = Mockito.mock(ResourceBundle.class);

		Object returnResult = _liferayObjectWrapper.handleUnknownType(
			resourceBundle);

		Assert.assertTrue(returnResult instanceof ResourceBundleModel);
	}

	@Test
	public void testHandleUnknownTypeAllowedTemplateNode() {
		TemplateNode templateNode = Mockito.mock(TemplateNode.class);

		Object returnResult = _liferayObjectWrapper.handleUnknownType(
			templateNode);

		Assert.assertTrue(returnResult instanceof LiferayTemplateModel);
	}

	@AdviseWith(adviceClasses = ReflectionUtilAdvice.class)
	@NewEnv(type = NewEnv.Type.CLASSLOADER)
	@Test
	public void testInitializationFailure() throws ClassNotFoundException {
		Throwable throwable = new Throwable();

		ReflectionUtilAdvice.setDeclaredFieldThrowable(throwable);

		try {
			new LiferayObjectWrapper();

			Assert.fail();
		}
		catch (ExceptionInInitializerError eiie) {
			Assert.assertSame(throwable, eiie.getCause());
		}
	}

	@Test
	public void testLiferayObjectWrapperConstructor() {
		Field cacheClassNamesField = ReflectionTestUtil.getAndSetFieldValue(
			LiferayObjectWrapper.class, "_cacheClassNamesField", null);

		try {
			new LiferayObjectWrapper();
		}
		catch (Exception e) {
			Assert.assertTrue(e instanceof NullPointerException);
		}
		finally {
			ReflectionTestUtil.setFieldValue(
				LiferayObjectWrapper.class, "_cacheClassNamesField",
				cacheClassNamesField);
		}
	}

	@Test
	public void testWrapAllowedCollection() throws Exception {
		ObjectCollection objectCollection = new ObjectCollection();

		Object returnResult = _liferayObjectWrapper.wrap(objectCollection);

		Assert.assertTrue(returnResult instanceof SimpleSequence);
	}

	@Test
	public void testWrapAllowedInteger() throws Exception {
		Integer objectNewType = 0;

		_liferayObjectWrapper.handleUnknownType(objectNewType);

		Object returnResult = _liferayObjectWrapper.wrap(objectNewType);

		Assert.assertTrue(returnResult instanceof TemplateModel);
	}

	@Test
	public void testWrapAllowedMap() throws Exception {
		ObjectMap objectMap = Mockito.mock(ObjectMap.class);

		Object returnResult = _liferayObjectWrapper.wrap(objectMap);

		Assert.assertTrue(returnResult instanceof MapModel);
	}

	@Test
	public void testWrapAllowedNode() throws Exception {
		TemplateNode templateNode = Mockito.mock(TemplateNode.class);

		Object returnResult = _liferayObjectWrapper.wrap(templateNode);

		Assert.assertTrue(returnResult instanceof LiferayTemplateModel);
	}

	@Test
	public void testWrapAllowedNull() throws Exception {
		Object object = null;

		TemplateModel returnResult = _liferayObjectWrapper.wrap(object);

		Assert.assertNull(returnResult);
	}

	@Test
	public void testWrapAllowedString() throws Exception {
		String objectTemplateString = "";

		Object returnResult = _liferayObjectWrapper.wrap(objectTemplateString);

		Assert.assertTrue(returnResult instanceof SimpleScalar);
	}

	@Test
	public void testWrapAllowedStringModel() throws Exception {
		ObjectEmpty objectEmpty = new ObjectEmpty();

		Object returnResult = _liferayObjectWrapper.wrap(objectEmpty);

		Assert.assertTrue(returnResult instanceof StringModel);
	}

	@Test
	public void testWrapAllowedTemplateModel() throws Exception {
		TemplateModel templateModel = Mockito.mock(TemplateModel.class);

		Object returnResult = _liferayObjectWrapper.wrap(templateModel);

		Assert.assertTrue(returnResult instanceof TemplateModel);
	}

	private LiferayObjectWrapper _liferayObjectWrapper;

	private class ObjectCollection extends ArrayList {
	}

	private class ObjectEmpty {
	}

	private class ObjectMap extends HashMap {
	}

}
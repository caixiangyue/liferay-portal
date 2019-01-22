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

package com.liferay.portal.kernel.nio.intraband.welder;

import com.liferay.portal.kernel.nio.intraband.Intraband;
import com.liferay.portal.kernel.nio.intraband.RegistrationReference;
import com.liferay.portal.kernel.nio.intraband.welder.fifo.FIFOUtil;
import com.liferay.portal.kernel.nio.intraband.welder.fifo.FIFOWelder;
import com.liferay.portal.kernel.nio.intraband.welder.socket.SocketWelder;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.CodeCoverageAssertor;
import com.liferay.portal.kernel.test.rule.NewEnv;
import com.liferay.portal.kernel.test.rule.NewEnvTestRule;
import com.liferay.portal.kernel.util.OSDetector;
import com.liferay.portal.kernel.util.PropsKeys;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Shuyang Zhou
 */
@NewEnv(type = NewEnv.Type.CLASSLOADER)
public class WelderFactoryUtilTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			CodeCoverageAssertor.INSTANCE, NewEnvTestRule.INSTANCE);

	@NewEnv(type = NewEnv.Type.NONE)
	@Test
	public void testConstructor() {
		new WelderFactoryUtil();
	}

	@Test
	public void testCreateWelder() {
		System.setProperty(
			PropsKeys.INTRABAND_WELDER_IMPL, MockWelder.class.getName());

		try {
			Welder welder = WelderFactoryUtil.createWelder();

			Assert.assertNotNull(welder);
			Assert.assertSame(MockWelder.class, welder.getClass());
		}
		finally {
			System.clearProperty(PropsKeys.INTRABAND_WELDER_IMPL);
		}
	}

	@Test
	public void testCreateWelderFailed() {
		System.setProperty(
			PropsKeys.INTRABAND_WELDER_IMPL, PrivateMockWelder.class.getName());

		try {
			WelderFactoryUtil.createWelder();

			Assert.fail();
		}
		catch (RuntimeException re) {
			Assert.assertTrue(re.getCause() instanceof IllegalAccessException);
		}
		finally {
			System.clearProperty(PropsKeys.INTRABAND_WELDER_IMPL);
		}
	}

	@Test
	public void testGetWelderClassClassNotFound() {
		System.setProperty(PropsKeys.INTRABAND_WELDER_IMPL, "NoSuchClass");

		try {
			WelderFactoryUtil.getWelderClass();

			Assert.fail();
		}
		catch (RuntimeException re) {
			Assert.assertTrue(re.getCause() instanceof ClassNotFoundException);
		}
		finally {
			System.clearProperty(PropsKeys.INTRABAND_WELDER_IMPL);
		}
	}

	@Test
	public void testGetWelderClassCustomizedImpl() {
		System.setProperty(
			PropsKeys.INTRABAND_WELDER_IMPL, MockWelder.class.getName());

		try {
			Assert.assertSame(
				MockWelder.class, WelderFactoryUtil.getWelderClass());
		}
		finally {
			System.clearProperty(PropsKeys.INTRABAND_WELDER_IMPL);
		}
	}

	@Test
	public void testGetWelderClassOnNonwindowsWithFIFO() {
		ReflectionTestUtil.setFieldValue(
			FIFOUtil.class, "_FIFO_SUPPORTED", true);
		ReflectionTestUtil.setFieldValue(OSDetector.class, "_windows", false);

		Assert.assertSame(FIFOWelder.class, WelderFactoryUtil.getWelderClass());
	}

	@Test
	public void testGetWelderClassOnnonWindowsWithoutFIFO() {
		ReflectionTestUtil.setFieldValue(
			FIFOUtil.class, "_FIFO_SUPPORTED", false);
		ReflectionTestUtil.setFieldValue(OSDetector.class, "_windows", false);

		Assert.assertSame(
			SocketWelder.class, WelderFactoryUtil.getWelderClass());
	}

	@Test
	public void testGetWelderClassOnWindows() {
		ReflectionTestUtil.setFieldValue(OSDetector.class, "_windows", true);

		Assert.assertSame(
			SocketWelder.class, WelderFactoryUtil.getWelderClass());
	}

	protected static class MockWelder implements Welder {

		@Override
		public void destroy() {
		}

		@Override
		public RegistrationReference weld(Intraband intraband) {
			return null;
		}

	}

	private static class PrivateMockWelder implements Welder {

		@Override
		public void destroy() {
		}

		@Override
		public RegistrationReference weld(Intraband intraband) {
			return null;
		}

	}

}
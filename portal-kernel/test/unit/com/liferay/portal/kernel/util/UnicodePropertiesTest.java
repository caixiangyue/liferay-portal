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

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.test.CaptureHandler;
import com.liferay.portal.kernel.test.JDKLoggerTestUtil;
import com.liferay.portal.kernel.test.ReflectionTestUtil;

import java.io.IOException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Alexander Chow
 * @author Xiangyue Cai
 */
public class UnicodePropertiesTest {

	@Test
	public void testConstructorAndIsSafe() {
		UnicodeProperties unicodeProperties1 = new UnicodeProperties();

		Assert.assertFalse(
			"_safe should be false",
			ReflectionTestUtil.getFieldValue(unicodeProperties1, "_safe"));
		Assert.assertFalse(
			"isSafe() should be return false", unicodeProperties1.isSafe());

		UnicodeProperties unicodeProperties2 = new UnicodeProperties(true);

		Assert.assertTrue(
			"_safe should be true",
			ReflectionTestUtil.getFieldValue(unicodeProperties2, "_safe"));
		Assert.assertTrue(
			"isSafe() should be return true", unicodeProperties2.isSafe());
	}

	@Test
	public void testFastLoad() {
		UnicodeProperties unicodeProperties = new UnicodeProperties();

		unicodeProperties.fastLoad(null);

		unicodeProperties.fastLoad(_TEST_LINE_1);

		Assert.assertEquals(
			Collections.singletonMap(_TEST_KEY_1, _TEST_VALUE_1),
			unicodeProperties);

		unicodeProperties.fastLoad(_TEST_PROPS);

		_assertTestProps(unicodeProperties);
	}

	@Test
	public void testGetProperty() {

		// with key

		UnicodeProperties unicodeProperties = new UnicodeProperties();

		unicodeProperties.put(_TEST_KEY_1, _TEST_VALUE_1);

		Assert.assertEquals(
			_TEST_VALUE_1, unicodeProperties.getProperty(_TEST_KEY_1));

		// with key and defaultValue

		Assert.assertEquals(
			_TEST_VALUE_1,
			unicodeProperties.getProperty(_TEST_KEY_1, "testDefaultValue"));

		unicodeProperties.remove(_TEST_KEY_1);

		Assert.assertEquals(
			"testDefaultValue",
			unicodeProperties.getProperty(_TEST_KEY_1, "testDefaultValue"));
	}

	@Test
	public void testGetToStringLength() {
		UnicodeProperties unicodeProperties = new UnicodeProperties();

		Assert.assertEquals(-1, unicodeProperties.getToStringLength());
	}

	@Test
	public void testLoad() throws IOException {
		UnicodeProperties unicodeProperties1 = new UnicodeProperties();

		unicodeProperties1.load(null);

		unicodeProperties1.load(_TEST_PROPS);

		_assertTestProps(unicodeProperties1);
	}

	@Test
	public void testPutAll() {
		Map<String, String> testMap = new HashMap<>();

		testMap.put(_TEST_KEY_1, _TEST_VALUE_1);
		testMap.put(_TEST_KEY_2, _TEST_VALUE_2);
		testMap.put(_TEST_KEY_3, _TEST_VALUE_3);

		UnicodeProperties unicodeProperties = new UnicodeProperties();

		unicodeProperties.putAll(testMap);

		_assertTestProps(unicodeProperties);
	}

	@Test
	public void testPutWithKeyAndValue() {
		UnicodeProperties unicodeProperties = new UnicodeProperties();

		Assert.assertNull(unicodeProperties.put(null, null));

		unicodeProperties.put(_TEST_KEY_1, null);

		Assert.assertNull(unicodeProperties.get(_TEST_KEY_1));

		unicodeProperties.put(_TEST_KEY_1, _TEST_VALUE_1);

		Assert.assertEquals(
			Collections.singletonMap(_TEST_KEY_1, _TEST_VALUE_1),
			unicodeProperties);
	}

	@Test
	public void testPutWithLine() {
		UnicodeProperties unicodeProperties1 = new UnicodeProperties();

		// line with empty

		unicodeProperties1.put("");

		// line with POUND(#)

		unicodeProperties1.put("#");

		Assert.assertEquals(Collections.emptyMap(), unicodeProperties1);

		try (CaptureHandler captureHandler =
				JDKLoggerTestUtil.configureJDKLogger(
					UnicodeProperties.class.getName(), Level.ALL)) {

			List<LogRecord> logRecords = captureHandler.getLogRecords();

			Assert.assertEquals(logRecords.toString(), 0, logRecords.size());

			// line without EQUAL(=)

			unicodeProperties1.put(_TEST_KEY_1);

			logRecords = captureHandler.getLogRecords();

			Assert.assertEquals(logRecords.toString(), 1, logRecords.size());

			LogRecord logRecord = logRecords.get(0);

			Assert.assertEquals(
				"Invalid property on line ".concat(_TEST_KEY_1),
				logRecord.getMessage());
		}

		unicodeProperties1.put(_TEST_LINE_1);

		Assert.assertEquals(
			Collections.singletonMap(_TEST_KEY_1, _TEST_VALUE_1),
			unicodeProperties1);

		// _safe is true

		UnicodeProperties unicodeProperties2 = new UnicodeProperties(true);

		unicodeProperties2.put(
			_TEST_LINE_1.concat(_TEST_SAFE_NEWLINE_CHARACTER));

		Assert.assertEquals(
			Collections.singletonMap(
				_TEST_KEY_1, _TEST_VALUE_1.concat(StringPool.NEW_LINE)),
			unicodeProperties2);
	}

	@Test
	public void testRemove() {
		UnicodeProperties unicodeProperties = new UnicodeProperties();

		Assert.assertNull(unicodeProperties.remove(null));

		unicodeProperties.put(_TEST_KEY_1, _TEST_VALUE_1);

		Assert.assertEquals(1, unicodeProperties.size());

		Assert.assertNotNull(unicodeProperties.get(_TEST_KEY_1));

		Assert.assertEquals(
			_TEST_VALUE_1, unicodeProperties.remove(_TEST_KEY_1));

		Assert.assertEquals(0, unicodeProperties.size());

		Assert.assertNull(unicodeProperties.get(_TEST_KEY_1));
	}

	@Test
	public void testSetNullProperty() {
		UnicodeProperties unicodeProperties = new UnicodeProperties();

		int hashCode = unicodeProperties.hashCode();

		unicodeProperties.setProperty(null, "value");

		Assert.assertEquals(
			"setProperty() of null key must not change properties", hashCode,
			unicodeProperties.hashCode());

		unicodeProperties.setProperty("key", null);
		unicodeProperties.setProperty("key", "value");
		unicodeProperties.setProperty("key", null);

		Assert.assertEquals(
			"setProperty() of null value must remove entry", hashCode,
			unicodeProperties.hashCode());
	}

	@Test
	public void testToSortedString() {
		UnicodeProperties unicodeProperties1 = new UnicodeProperties();

		Assert.assertEquals(
			StringPool.BLANK, unicodeProperties1.toSortedString());

		unicodeProperties1.put(_TEST_KEY_1, _TEST_VALUE_1);

		Assert.assertEquals(
			_TEST_LINE_1.concat(StringPool.NEW_LINE),
			unicodeProperties1.toSortedString());

		UnicodeProperties unicodeProperties2 = new UnicodeProperties(true);

		unicodeProperties2.put(_TEST_KEY_1, _TEST_VALUE_1);
		unicodeProperties2.put(_TEST_KEY_2, _TEST_VALUE_2);
		unicodeProperties2.put(
			_TEST_KEY_3, _TEST_VALUE_3.concat(StringPool.NEW_LINE));

		unicodeProperties2.replace(_TEST_KEY_2, null);

		Assert.assertEquals(
			StringBundler.concat(
				_TEST_LINE_1, StringPool.NEW_LINE, _TEST_LINE_3,
				_TEST_SAFE_NEWLINE_CHARACTER, StringPool.NEW_LINE),
			unicodeProperties2.toSortedString());
	}

	private void _assertTestProps(UnicodeProperties unicodeProperties) {
		Assert.assertEquals(_TEST_VALUE_1, unicodeProperties.get(_TEST_KEY_1));
		Assert.assertEquals(_TEST_VALUE_2, unicodeProperties.get(_TEST_KEY_2));
		Assert.assertEquals(_TEST_VALUE_3, unicodeProperties.get(_TEST_KEY_3));
	}

	private static final String _TEST_KEY_1 = "testKey1";

	private static final String _TEST_KEY_2 = "testKey2";

	private static final String _TEST_KEY_3 = "testKey3";

	private static final String _TEST_LINE_1 = StringBundler.concat(
		_TEST_KEY_1, StringPool.EQUAL, UnicodePropertiesTest._TEST_VALUE_1);

	private static final String _TEST_LINE_2 = StringBundler.concat(
		_TEST_KEY_2, StringPool.EQUAL, UnicodePropertiesTest._TEST_VALUE_2);

	private static final String _TEST_LINE_3 = StringBundler.concat(
		_TEST_KEY_3, StringPool.EQUAL, UnicodePropertiesTest._TEST_VALUE_3);

	private static final String _TEST_PROPS = StringBundler.concat(
		_TEST_LINE_1, StringPool.NEW_LINE, _TEST_LINE_2, StringPool.NEW_LINE,
		_TEST_LINE_3);

	private static final String _TEST_SAFE_NEWLINE_CHARACTER =
		"_SAFE_NEWLINE_CHARACTER_";

	private static final String _TEST_VALUE_1 = "testValue1";

	private static final String _TEST_VALUE_2 = "testValue2";

	private static final String _TEST_VALUE_3 = "testValue3";

}
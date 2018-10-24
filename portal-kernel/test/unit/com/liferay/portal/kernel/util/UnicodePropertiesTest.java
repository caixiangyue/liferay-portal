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
	public void testFastLoad() {
		_testAndAssertFastLoad(Collections.emptyMap(), null);
		_testAndAssertFastLoad(
			Collections.singletonMap(_TEST_KEY_1, _TEST_VALUE_1), _TEST_LINE_1);
		_testAndAssertFastLoad(_testMap, _TEST_PROPS);
	}

	@Test
	public void testGetProperty() {
		UnicodeProperties unicodeProperties = new UnicodeProperties();

		// with key

		unicodeProperties.put(_TEST_KEY_1, _TEST_VALUE_1);

		Assert.assertEquals(
			_TEST_VALUE_1, unicodeProperties.getProperty(_TEST_KEY_1));
		Assert.assertEquals(
			_TEST_VALUE_1,
			unicodeProperties.getProperty(_TEST_KEY_1, "testDefaultValue"));

		// with key and defaultValue

		Assert.assertNull(unicodeProperties.getProperty(_TEST_KEY_2));
		Assert.assertEquals(
			"testDefaultValue",
			unicodeProperties.getProperty(_TEST_KEY_2, "testDefaultValue"));
	}

	@Test
	public void testGetToStringLength() {
		UnicodeProperties unicodeProperties = new UnicodeProperties();

		Assert.assertEquals(-1, unicodeProperties.getToStringLength());
	}

	@Test
	public void testIsSafe() {
		_assertIsSafe(false, new UnicodeProperties());
		_assertIsSafe(false, new UnicodeProperties(false));
		_assertIsSafe(true, new UnicodeProperties(true));
	}

	@Test
	public void testLoad() throws IOException {
		_testAndAssertLoad(Collections.emptyMap(), null);
		_testAndAssertLoad(
			Collections.singletonMap(_TEST_KEY_1, _TEST_VALUE_1), _TEST_LINE_1);
		_testAndAssertLoad(_testMap, _TEST_PROPS);
	}

	@Test
	public void testPutAll() {
		UnicodeProperties unicodeProperties = new UnicodeProperties();

		unicodeProperties.putAll(_testMap);

		Assert.assertEquals(_testMap, unicodeProperties);
	}

	@Test
	public void testPutKeyValue() {
		UnicodeProperties unicodeProperties = new UnicodeProperties();

		_testAndAssertPutKeyValue(
			null, Collections.emptyMap(), null, null, unicodeProperties);
		_testAndAssertPutKeyValue(
			null, Collections.emptyMap(), _TEST_KEY_1, null, unicodeProperties);
		_testAndAssertPutKeyValue(
			null, Collections.singletonMap(_TEST_KEY_1, _TEST_VALUE_1),
			_TEST_KEY_1, _TEST_VALUE_1, unicodeProperties);
		_testAndAssertPutKeyValue(
			_TEST_VALUE_1, Collections.emptyMap(), _TEST_KEY_1, null,
			unicodeProperties);
	}

	@Test
	public void testPutLines() {
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

	private void _assertIsSafe(
		boolean expectedValue, UnicodeProperties unicodeProperties) {

		Assert.assertEquals(expectedValue, unicodeProperties.isSafe());
		Assert.assertEquals(
			ReflectionTestUtil.getFieldValue(unicodeProperties, "_safe"),
			unicodeProperties.isSafe());
	}

	private void _testAndAssertFastLoad(
		Map<String, String> expectedUnicodeProperties, String props) {

		UnicodeProperties unicodeProperties = new UnicodeProperties();

		unicodeProperties.fastLoad(props);

		Assert.assertEquals(expectedUnicodeProperties, unicodeProperties);
	}

	private void _testAndAssertLoad(
			Map<String, String> expectedUnicodeProperties, String props)
		throws IOException {

		UnicodeProperties unicodeProperties = new UnicodeProperties();

		unicodeProperties.load(props);

		Assert.assertEquals(expectedUnicodeProperties, unicodeProperties);
	}

	private void _testAndAssertPutKeyValue(
		String expectedPutReturnValue,
		Map<String, String> expectedUnicodeProperties, String key, String value,
		UnicodeProperties unicodeProperties) {

		Assert.assertEquals(
			expectedPutReturnValue, unicodeProperties.put(key, value));
		Assert.assertEquals(expectedUnicodeProperties, unicodeProperties);
	}

	private static final String _TEST_KEY_1 = "testKey1";

	private static final String _TEST_KEY_2 = "testKey2";

	private static final String _TEST_KEY_3 = "testKey3";

	private static final String _TEST_LINE_1;

	private static final String _TEST_LINE_2;

	private static final String _TEST_LINE_3;

	private static final String _TEST_PROPS;

	private static final String _TEST_SAFE_NEWLINE_CHARACTER =
		"_SAFE_NEWLINE_CHARACTER_";

	private static final String _TEST_VALUE_1 = "testValue1";

	private static final String _TEST_VALUE_2 = "testValue2";

	private static final String _TEST_VALUE_3 = "testValue3";

	private static final Map<String, String> _testMap =
		new HashMap<String, String>() {
			{
				put(_TEST_KEY_1, _TEST_VALUE_1);
				put(_TEST_KEY_2, _TEST_VALUE_2);
				put(_TEST_KEY_3, _TEST_VALUE_3);
			}
		};

	static {
		_TEST_LINE_1 = StringBundler.concat(
			_TEST_KEY_1, StringPool.EQUAL, _TEST_VALUE_1);

		_TEST_LINE_2 = StringBundler.concat(
			_TEST_KEY_2, StringPool.EQUAL, _TEST_VALUE_2);

		_TEST_LINE_3 = StringBundler.concat(
			_TEST_KEY_3, StringPool.EQUAL, _TEST_VALUE_3);

		_TEST_PROPS = StringBundler.concat(
			_TEST_LINE_1, StringPool.NEW_LINE, _TEST_LINE_2,
			StringPool.NEW_LINE, _TEST_LINE_3);
	}

}
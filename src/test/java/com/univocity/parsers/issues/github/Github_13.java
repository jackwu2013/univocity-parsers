/*******************************************************************************
 * Copyright 2015 uniVocity Software Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.univocity.parsers.issues.github;

import static org.testng.Assert.*;

import java.io.*;
import java.math.*;
import java.util.*;

import org.testng.annotations.*;

import com.univocity.parsers.common.processor.*;
import com.univocity.parsers.conversions.*;
import com.univocity.parsers.csv.*;
import com.univocity.parsers.fixed.*;

/**
 *
 * From: https://github.com/uniVocity/univocity-parsers/issues/13
 *
 * @author uniVocity Software Pty Ltd - <a href="mailto:parsers@univocity.com">parsers@univocity.com</a>
 *
 */
public class Github_13 {

	enum ClientType {
		PERSONAL(2),
		BUSINESS(1);

		int typeCode;

		ClientType(int typeCode) {
			this.typeCode = typeCode;
		}
	}

	static final String CSV_INPUT = ""
			+ "Client, 1, Foo\n"
			+ "Account,  23234, HSBC, 123433-000, HSBCAUS\n"
			+ "Account,  11234, HSBC, 222343-130, HSBCCAD\n"
			+ "Client, 2, BAR\n"
			+ "Account,  1234, CITI, 213343-130, CITICAD\n";

	static final String FIXED_INPUT = ""
			+ "N#123123 1888858    58888548\n"
			+ "111222       3000FOO                               10\n"
			+ "333444       2000BAR                               60\n"
			+ "N#123124 1888844    58888544\n"
			+ "311222       3500FOO                               30\n";

	@Test
	public void processMultiRowFormatCsv() {
		final ObjectRowListProcessor clientProcessor = new ObjectRowListProcessor();
		clientProcessor.convertIndexes(Conversions.toEnum(ClientType.class, "typeCode", EnumSelector.CUSTOM_FIELD)).set(1);

		final ObjectRowListProcessor accountProcessor = new ObjectRowListProcessor();
		accountProcessor.convertFields(Conversions.toBigDecimal()).set("balance");

		ValueBasedSwitch valueSwitch = new ValueBasedSwitch(0) {
			@Override
			public void rowProcessorSwitched(RowProcessor from, RowProcessor to) {
				if (from == accountProcessor) {
					clientProcessor.getRows().addAll(accountProcessor.getRows());
					accountProcessor.getRows().clear();
				}
			}
		};
		valueSwitch.addSwitchForValue("Client", clientProcessor);
		valueSwitch.addSwitchForValue("Account", accountProcessor, "type", "balance", "bank", "account", "swift");

		CsvParserSettings settings = new CsvParserSettings();
		settings.setRowProcessor(valueSwitch);

		CsvParser parser = new CsvParser(settings);
		parser.parse(new StringReader(CSV_INPUT));

		List<Object[]> rows = clientProcessor.getRows();
		assertEquals(rows.size(), 5);
		assertEquals(rows.get(0)[1], ClientType.BUSINESS);
		assertEquals(rows.get(1)[1], new BigDecimal("23234"));
		assertEquals(rows.get(2)[1], new BigDecimal("11234"));
		assertEquals(rows.get(3)[1], ClientType.PERSONAL);
		assertEquals(rows.get(4)[1], new BigDecimal("1234"));
	}

	@Test
	public void processMultiRowFormatFixedWidth() {

		FixedWidthFieldLengths itemLengths = new FixedWidthFieldLengths(13, 4, 34, 2);
		FixedWidthParserSettings settings = new FixedWidthParserSettings(itemLengths);
		settings.addFormatForLookahead("N#", new FixedWidthFieldLengths(9, 11, 8)); //receipt lengths

		FixedWidthParser parser = new FixedWidthParser(settings);

		List<String[]> rows = parser.parseAll(new StringReader(FIXED_INPUT));
		assertEquals(rows.size(), 5);
		assertEquals(rows.get(0), new String[] { "N#123123", "1888858", "58888548" });
		assertEquals(rows.get(1), new String[] { "111222", "3000", "FOO", "10" });
		assertEquals(rows.get(2), new String[] { "333444", "2000", "BAR", "60" });
		assertEquals(rows.get(3), new String[] { "N#123124", "1888844", "58888544" });
		assertEquals(rows.get(4), new String[] { "311222", "3500", "FOO", "30" });

	}
}

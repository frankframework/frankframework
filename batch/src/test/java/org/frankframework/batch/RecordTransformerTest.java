package org.frankframework.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;

class RecordTransformerTest {

	@Test
	void testAlignForValLengthLeftAlignFillchar() {
		String s = "test";
		String res1 = RecordTransformer.align(s, 10, true, 'b');
		String res2 = RecordTransformer.align(s, 2, true, 'b');
		String res3 = RecordTransformer.align(s, 4, false, 'b');
		assertEquals("testbbbbbb", res1);
		assertEquals("te", res2);
		assertEquals("test", res3);
	}

	@Test
	void testAlignForValLengthRightAlignFillchar() {
		String s = "test";
		String res1 = RecordTransformer.align(s, 10, false, 'c');
		String res2 = RecordTransformer.align(s, 2, false, 'c');
		String res3 = RecordTransformer.align(s, 4, false, 'c');
		assertEquals("cccccctest", res1);
		assertEquals("te", res2);
		assertEquals("test", res3);
	}

	@Test
	void testGetFilledArray() {
		char[] arr = RecordTransformer.getFilledArray(5, 'a');
		assertEquals("aaaaa", new String(arr));
	}

	@Test
	void testParseException() {
		RecordTransformer.FixedDateOutput fixedDateOutput = new RecordTransformer.FixedDateOutput("dd-MM-yyyy", null, 0);

		StringBuilder result = new StringBuilder();

		// Expect exception because inFormatPattern is empty -- can't parse date without a pattern
		assertThrows(DateTimeParseException.class, () -> fixedDateOutput.appendValue(null, result, List.of("2021-12-01")));
	}

	@Test
	void testInputFieldIndexLt0() throws ConfigurationException {
		RecordTransformer.FixedDateOutput fixedDateOutput = new RecordTransformer.FixedDateOutput("dd-MM-yyyy", null, -1);

		StringBuilder result = new StringBuilder();
		fixedDateOutput.appendValue(null, result, List.of("2021-12-01T12:34:56"));

		// Expect today, because input field index == -1 (skipping inFormatPattern which is null here)
		String formattedToday = DateTimeFormatter.ofPattern("dd-MM-yyyy").format(LocalDate.now());
		assertEquals(formattedToday, result.toString());
	}

	@Test
	void testInputFieldIndex0() throws ConfigurationException {
		RecordTransformer.FixedDateOutput fixedDateOutput = new RecordTransformer.FixedDateOutput("dd-MM-yyyy", "yyyy-MM-dd HH:mm:ss", 0);

		StringBuilder result = new StringBuilder();
		fixedDateOutput.appendValue(null, result, List.of("2021-12-01 12:34:56"));

		// Expect 01-12-2021, because of the outFormatPattern
		assertEquals("01-12-2021", result.toString());
	}

	@Test
	void testInputWithoutTimeComponent() throws ConfigurationException {
		RecordTransformer.FixedDateOutput fixedDateOutput = new RecordTransformer.FixedDateOutput("yyyy-MM-dd HH:mm:ss", "yyMMdd", 0);

		StringBuilder result = new StringBuilder();
		fixedDateOutput.appendValue(null, result, List.of("091201"));

		assertEquals("2009-12-01 00:00:00", result.toString());
	}
}

package org.frankframework.parameters;

import static org.frankframework.parameters.DateParameter.TYPE_DATETIME_PATTERN;
import static org.frankframework.parameters.DateParameter.TYPE_DATE_PATTERN;
import static org.frankframework.parameters.DateParameter.TYPE_TIMESTAMP_PATTERN;
import static org.frankframework.parameters.DateParameter.TYPE_TIME_PATTERN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;

public class ParameterValueTest {

	private void testDateParameterToString(DateParameter.DateFormatType type, String pattern, String input, String expectedOutput) throws ParseException, ConfigurationException {
		DateFormat df = new SimpleDateFormat(pattern);
		Date date = df.parse(input);

		DateParameter parameter = new DateParameter();
		parameter.setName("InputMessage");
		parameter.setFormatType(type);
		parameter.configure();

		ParameterValue pv = new ParameterValue(parameter, date);

		assertInstanceOf(Date.class, pv.getValue());
		assertEquals(pv.asStringValue(), expectedOutput);
	}

	@Test
	public void testDateToString() throws ConfigurationException, ParseException {
		testDateParameterToString(DateParameter.DateFormatType.DATE, TYPE_DATE_PATTERN, "2024-08-09 15:00:00", "2024-08-09");
		testDateParameterToString(DateParameter.DateFormatType.DATE, TYPE_DATE_PATTERN, "2024-10-15", "2024-10-15");
		testDateParameterToString(DateParameter.DateFormatType.DATE, TYPE_DATE_PATTERN, "1980-05-20 14:15:16.100", "1980-05-20");

		assertThrows(ParseException.class, () -> testDateParameterToString(DateParameter.DateFormatType.DATE, TYPE_DATE_PATTERN, "20:00", ""));
	}

	@Test
	public void testDateTimeToString() throws ConfigurationException, ParseException {
		testDateParameterToString(DateParameter.DateFormatType.DATETIME, TYPE_DATETIME_PATTERN, "2024-08-09 18:15:25", "2024-08-09 18:15:25");
		testDateParameterToString(DateParameter.DateFormatType.DATETIME, TYPE_DATETIME_PATTERN, "2024-08-09 00:00:00", "2024-08-09 00:00:00");

		assertThrows(ParseException.class, () -> testDateParameterToString(DateParameter.DateFormatType.DATETIME, TYPE_DATETIME_PATTERN, "20:00", ""));
	}

	@Test
	public void testTimeToString() throws ConfigurationException, ParseException {
		testDateParameterToString(DateParameter.DateFormatType.TIME, TYPE_TIME_PATTERN, "19:25:45", "19:25:45");

		assertThrows(ParseException.class, () -> testDateParameterToString(DateParameter.DateFormatType.TIME, TYPE_TIME_PATTERN, "2024-08-09 15:00", ""));
	}

	@Test
	public void testTimestampToString() throws ConfigurationException, ParseException {
		testDateParameterToString(DateParameter.DateFormatType.TIMESTAMP, TYPE_TIMESTAMP_PATTERN, "2024-08-09 18:15:25.500", "2024-08-09 18:15:25.500");

		assertThrows(ParseException.class, () -> testDateParameterToString(DateParameter.DateFormatType.TIMESTAMP, TYPE_TIMESTAMP_PATTERN, "2024-08-09 15:00", ""));
	}

}

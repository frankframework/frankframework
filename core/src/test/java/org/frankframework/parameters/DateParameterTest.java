package org.frankframework.parameters;

import static org.frankframework.parameters.DateParameter.TYPE_DATE_PATTERN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationUtils;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.parameters.DateParameter.DateFormatType;
import org.frankframework.stream.Message;
import org.frankframework.util.DateFormatUtils;

public class DateParameterTest {

	protected void testFromStringToDateType(String input, String expected, DateFormatType type) throws ConfigurationException, ParameterException {
		DateParameter parameter = new DateParameter();
		parameter.setName("InputMessage");
		parameter.setValue(input);
		parameter.setFormatType(type);
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, null, true);

		assertInstanceOf(Date.class, result);
		assertEquals(expected, DateFormatUtils.format((Date) result));

		assertFalse(parameter.requiresInputValueForResolution());
	}

	@Test
	public void testParameterFromStringToDate() throws Exception {
		String input = "2022-01-23";
		String expected = "2022-01-23 00:00:00.000";
		testFromStringToDateType(input, expected, DateFormatType.DATE);
	}

	@Test
	public void testParameterFromStringToDateTime() throws Exception {
		String input = "2022-01-23 11:14:17";
		String expected = "2022-01-23 11:14:17.000";
		testFromStringToDateType(input, expected, DateFormatType.DATETIME);
	}

	@Test
	public void testParameterFromStringToTimestamp() throws Exception {
		String input = "2022-01-23 11:14:17.123";
		String expected = "2022-01-23 11:14:17.123";
		testFromStringToDateType(input, expected, DateFormatType.TIMESTAMP);
	}

	@Test
	public void testParameterFromStringToTime() throws Exception {
		String input = "11:14:17";
		String expected = "1970-01-01 11:14:17.000";
		testFromStringToDateType(input, expected, DateFormatType.TIME);
	}

	@Test
	public void testParameterFromXmlDateTime() throws Exception {
		String input = "2022-05-30T09:30:10+06:00";
		String expected = "2022-05-30 05:30:10.000";

		DateParameter parameter = new DateParameter();
		parameter.setName("InputMessage");
		parameter.setValue(input);
		parameter.setFormatType(DateFormatType.XMLDATETIME);
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, null, true);

		assertInstanceOf(Date.class, result);
		assertEquals(expected, DateFormatUtils.format((Date) result));

		assertFalse(parameter.requiresInputValueForResolution());
	}

	@Test
	public void testParameterFromStringToXmlDateTime() throws Exception {
		String input = "2022-01-23T11:14:17";
		String expected = "2022-01-23 11:14:17.000";

		DateParameter parameter = new DateParameter();
		parameter.setName("InputMessage");
		parameter.setValue(input);
		parameter.setFormatType(DateFormatType.XMLDATETIME);
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, null, true);

		assertInstanceOf(Date.class, result);
		assertEquals(expected, DateFormatUtils.format((Date) result));

		assertFalse(parameter.requiresInputValueForResolution());
	}

	@Test
	public void testParameterFromDateToXmlDateTime() throws Exception {
		Date date = new Date();

		PipeLineSession session = new PipeLineSession();
		session.put("originalMessage", date);

		DateParameter parameter = new DateParameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.setFormatType(DateFormatType.XMLDATETIME);
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);
		assertInstanceOf(Date.class, result);

		assertEquals(date, result);

		assertFalse(parameter.requiresInputValueForResolution());
		assertTrue(parameter.consumesSessionVariable("originalMessage"));
	}

	@Test
	public void testFixedDate() throws Exception {
		DateParameter p = new DateParameter();
		System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		try {
			p.setName("date");
			p.setPattern("{fixedDate}");
			p.setFormatType(DateFormatType.DATE);
			p.configure();
			PipeLineSession session = new PipeLineSession();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false); // Should return PutSystemDateInSession.FIXEDDATETIME
			assertTrue(result instanceof Date);

			Date resultDate = (Date) result;
			SimpleDateFormat sdf = new SimpleDateFormat(TYPE_DATE_PATTERN);
			String formattedDate = sdf.format(resultDate);
			assertEquals("2001-12-17", formattedDate);

		} finally {
			System.getProperties().remove(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY);
		}
	}

	@Test
	public void testFixedDateWithSession() throws Exception {
		DateParameter p = new DateParameter();
		p.setName("date");
		p.setPattern("{fixedDate}");
		p.setFormatType(DateFormatType.DATE);
		p.configure();
		PipeLineSession session = new PipeLineSession();
		session.put("fixedDate", "1995-01-23");
		session.put("stub4testtool.fixeddate", "1996-02-24");

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(result instanceof Date);

		Date resultDate = (Date) result;
		SimpleDateFormat sdf = new SimpleDateFormat(TYPE_DATE_PATTERN);
		String formattedDate = sdf.format(resultDate);
		assertEquals("1995-01-23", formattedDate);

		assertFalse(p.requiresInputValueForResolution());
	}

	@Test
	public void testFixedDateWithSessionFromTesttool() throws Exception {
		DateParameter p = new DateParameter();
		p.setName("date");
		p.setPattern("{fixedDate}");
		p.setFormatType(DateFormatType.DATE);
		p.configure();
		PipeLineSession session = new PipeLineSession();
		session.put("stub4testtool.fixeddate", "1996-02-24");

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		try {
			System.setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
			Object result = p.getValue(alreadyResolvedParameters, message, session, false);
			assertTrue(result instanceof Date);
			Date resultDate = (Date) result;
			SimpleDateFormat sdf = new SimpleDateFormat(TYPE_DATE_PATTERN);
			String formattedDate = sdf.format(resultDate);
			assertEquals("1996-02-24", formattedDate);
		} finally {
			System.getProperties().remove(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY);
		}
	}

	@Test
	public void testFixedDateWithDateInSessionFromTesttool() throws Exception {
		DateParameter p = new DateParameter();
		p.setName("date");
		p.setPattern("{fixedDate}");
		p.setFormatType(DateFormatType.DATE);
		p.configure();
		PipeLineSession session = new PipeLineSession();
		Date date = new Date();
		session.put("stub4testtool.fixeddate", date);

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		try {
			System.setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
			Object result = p.getValue(alreadyResolvedParameters, message, session, false);
			assertTrue(result instanceof Date);
			Date resultDate = (Date) result;
			SimpleDateFormat sdf = new SimpleDateFormat(TYPE_DATE_PATTERN);
			String formattedDate = sdf.format(resultDate);
			String formattedExpected = sdf.format(date);
			assertEquals(formattedExpected, formattedDate);
		} finally {
			System.getProperties().remove(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY);
		}
	}

	@Test
	public void testFixedDateWithDateObjectInSession() throws Exception {
		DateParameter p = new DateParameter();
		p.setName("date");
		p.setPattern("{fixedDate}");
		p.setFormatType(DateFormatType.DATE);
		p.configure();
		PipeLineSession session = new PipeLineSession();
		SimpleDateFormat sdf = new SimpleDateFormat(TYPE_DATE_PATTERN);
		session.put("fixedDate", sdf.parse("1995-01-23"));

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(result instanceof Date);

		Date resultDate = (Date) result;
		String formattedDate = sdf.format(resultDate);
		assertEquals("1995-01-23", formattedDate);
	}

	@Test
	public void testPatternNowWithDateType() throws Exception {
		DateParameter p = new DateParameter();
		System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		try {
			p.setName("date");
			p.setPattern("{now}");
			p.setFormatType(DateFormatType.DATE);
			p.configure();
			PipeLineSession session = new PipeLineSession();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false); // Should return PutSystemDateInSession.FIXEDDATETIME
			assertTrue(result instanceof Date);

			Date resultDate = (Date) result;
			SimpleDateFormat sdf = new SimpleDateFormat(TYPE_DATE_PATTERN);
			String formattedDate = sdf.format(resultDate);
			String expectedDate = sdf.format(new Date()); // dit gaat echt meestal wel goed
			assertEquals(expectedDate, formattedDate);

		} finally {
			System.getProperties().remove(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY);
		}
	}

	@Test
	public void testPatternFixedDateWithDateFormatTypeAndParameterTypeSet() throws Exception {
		String expectedDate = "2001-12-17 09:30:47.000";
		DateParameter p = new DateParameter();
		System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		try {
			p.setName("EsbSoapWrapperPipeTimestamp");
			p.setPattern("{fixeddate,date,yyyy-MM-dd'T'HH:mm:ss}");
			p.setFormatType(DateFormatType.TIMESTAMP);
			p.setFormatString("yyyy-MM-dd'T'HH:mm:ss");
			p.configure();
			PipeLineSession session = new PipeLineSession();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false); // Should return PutSystemDateInSession.FIXEDDATETIME
			assertTrue(result instanceof Date);

			SimpleDateFormat sdf = new SimpleDateFormat(DateFormatUtils.FORMAT_FULL_GENERIC);
			assertEquals(expectedDate, sdf.format(result));

		} finally {
			System.getProperties().remove(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY);
		}
	}

	@Test
	public void testPatternFixedDateWithParameterTypeDateTime() throws Exception {
		String expectedDate = "2001-12-17 09:30:47.000";
		DateParameter p = new DateParameter();
		System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		try {
			p.setName("EsbSoapWrapperPipeTimestamp");
			p.setPattern("{fixeddate}");
			p.setFormatType(DateFormatType.DATETIME);
			p.configure();
			PipeLineSession session = new PipeLineSession();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false); // Should return PutSystemDateInSession.FIXEDDATETIME
			assertTrue(result instanceof Date);

			SimpleDateFormat sdf = new SimpleDateFormat(DateFormatUtils.FORMAT_FULL_GENERIC);
			assertEquals(expectedDate, sdf.format(result));

		} finally {
			System.getProperties().remove(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY);
		}
	}

	@Test
	public void testPatternFixedDateWithParameterTypeTimestamp() throws Exception {
		String expectedDate = "2001-12-17 09:30:47.000";
		DateParameter p = new DateParameter();
		System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		try {
			p.setName("EsbSoapWrapperPipeTimestamp");
			p.setPattern("{fixeddate}");
			p.setFormatString("yyyy-MM-dd HH:mm:ss");
			p.setFormatType(DateFormatType.TIMESTAMP);
			p.configure();
			PipeLineSession session = new PipeLineSession();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false); // Should return PutSystemDateInSession.FIXEDDATETIME
			assertTrue(result instanceof Date);

			SimpleDateFormat sdf = new SimpleDateFormat(DateFormatUtils.FORMAT_FULL_GENERIC);
			assertEquals(expectedDate, sdf.format(result));

		} finally {
			System.getProperties().remove(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY);
		}
	}

	@Test
	public void testPatternFixedDateWithExtendedDateFormatTypeAndParameterTypeSet() throws Exception {
		String expectedDate = "2001-12-17 09:30:47.000";
		DateParameter p = new DateParameter();
		System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		try {
			p.setName("EsbSoapWrapperPipeTimestamp");
			p.setPattern("{fixeddate,date,yyyy-MM-dd HH:mm:ss.SSS}");
			p.setFormatType(DateFormatType.TIMESTAMP);
			p.configure();
			PipeLineSession session = new PipeLineSession();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false); // Should return PutSystemDateInSession.FIXEDDATETIME
			assertTrue(result instanceof Date);

			SimpleDateFormat sdf = new SimpleDateFormat(DateFormatUtils.FORMAT_FULL_GENERIC);
			assertEquals(expectedDate, sdf.format(result));

		} finally {
			System.getProperties().remove(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY);
		}
	}

	@Test
	public void testParameterFromDateToDate() throws Exception {
		Date date = new Date();

		PipeLineSession session = new PipeLineSession();
		session.put("originalMessage", date);

		DateParameter parameter = new DateParameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);

		assertInstanceOf(Date.class, result);
		assertEquals(date, result);
	}

	@Test
	public void testTimeParameterWithTimePatternWithoutFormat() throws Exception {

		// Arrange
		DateParameter parameter = new DateParameter();
		parameter.setName("time");
		parameter.setFormatType(DateFormatType.TIME);
		parameter.setPattern("{now,time}"); // Does not work if you do not specify 'time' in the pattern. Seems like a bug to me, inconsistent with how DateFormatType.DATE works
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");
		PipeLineSession session = new PipeLineSession();

		// Act
		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);

		// Assert
		assertInstanceOf(Date.class, result);
	}

	@Test
	public void testTimeParameterWithTimePatternWithFormat() throws Exception {

		// Arrange
		DateParameter parameter = new DateParameter();
		parameter.setName("time");
		parameter.setFormatType(DateFormatType.TIME);
		parameter.setPattern("{now,time,HH:mm:ss}"); // Does not work if you do not specify 'time' in the pattern. If you specify a format, it HAS to be this format.
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");
		PipeLineSession session = new PipeLineSession();

		// Act
		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);

		// Assert
		assertInstanceOf(Date.class, result);
	}

	@Test
	public void testDateParameterWithDatePatternWithoutFormat() throws Exception {

		// Arrange
		DateParameter parameter = new DateParameter();
		parameter.setName("date");
		parameter.setFormatType(DateFormatType.DATE);
		parameter.setPattern("{now}"); // Specifying just 'date' here does not work. Not consistent with DateFormatType.TIME
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");
		PipeLineSession session = new PipeLineSession();

		// Act
		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);

		// Assert
		assertInstanceOf(Date.class, result);
	}

	@Test
	public void testDateParameterWithDatePatternWithFormat() throws Exception {

		// Arrange
		DateParameter parameter = new DateParameter();
		parameter.setName("date");
		parameter.setFormatType(DateFormatType.DATE);
		parameter.setPattern("{now,date,yyyy-MM-dd}"); // Specifying full date format makes it work, but it HAS to be this format
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");
		PipeLineSession session = new PipeLineSession();

		// Act
		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);

		// Assert
		assertInstanceOf(Date.class, result);
	}
}

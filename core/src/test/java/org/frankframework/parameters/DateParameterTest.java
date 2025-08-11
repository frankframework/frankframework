package org.frankframework.parameters;

import static org.frankframework.parameters.DateParameter.TYPE_DATE_PATTERN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.util.ConfigurationUtils;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.parameters.DateParameter.DateFormatType;
import org.frankframework.stream.Message;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.TimeProvider;

@Isolated("Tests manipulate current time, so should not be run concurrently with other tests")
public class DateParameterTest {

	private TimeZone systemTimeZone;

	@BeforeEach
	void setup() {
		systemTimeZone = TimeZone.getDefault();
	}

	@AfterEach
	void tearDown() {
		System.getProperties().remove(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY);
		TimeZone.setDefault(systemTimeZone);
		TimeProvider.resetClock();
	}

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
		Date date = TimeProvider.nowAsDate();

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
	public void testWrongFormatString() {
		DateParameter p = new DateParameter();
		p.setName("date");
		p.setFormatType(DateFormatType.DATE);
		p.setFormatString("abc");
		ConfigurationException e = assertThrows(ConfigurationException.class, p::configure);
		assertEquals("invalid formatString [abc]: (IllegalArgumentException) Illegal pattern character 'b'", e.getMessage());
	}

	@Test
	public void testFixedDate() throws Exception {
		DateParameter p = new DateParameter();
		System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		try (PipeLineSession session = new PipeLineSession()) {
			p.setName("date");
			p.setPattern("{fixedDate}");
			p.setFormatType(DateFormatType.DATE);
			p.configure();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false); // Should return PutSystemDateInSession.FIXEDDATETIME
			Date resultDate = assertInstanceOf(Date.class, result);
			SimpleDateFormat sdf = new SimpleDateFormat(TYPE_DATE_PATTERN);
			String formattedDate = sdf.format(resultDate);
			assertEquals("2001-12-17", formattedDate);
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
		Date resultDate = assertInstanceOf(Date.class, result);
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

		System.setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");

		Object result = p.getValue(alreadyResolvedParameters, message, session, false);
		Date resultDate = assertInstanceOf(Date.class, result);
		SimpleDateFormat sdf = new SimpleDateFormat(TYPE_DATE_PATTERN);
		String formattedDate = sdf.format(resultDate);
		assertEquals("1996-02-24", formattedDate);
	}

	@Test
	public void testFixedDateWithDateInSessionFromTesttool() throws Exception {
		DateParameter p = new DateParameter();
		p.setName("date");
		p.setPattern("{fixedDate}");
		p.setFormatType(DateFormatType.DATE);
		p.configure();
		PipeLineSession session = new PipeLineSession();
		Date date = TimeProvider.nowAsDate();
		session.put("stub4testtool.fixeddate", date);

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		System.setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");

		Object result = p.getValue(alreadyResolvedParameters, message, session, false);
		Date resultDate = assertInstanceOf(Date.class, result);
		SimpleDateFormat sdf = new SimpleDateFormat(TYPE_DATE_PATTERN);
		String formattedDate = sdf.format(resultDate);
		String formattedExpected = sdf.format(date);
		assertEquals(formattedExpected, formattedDate);
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
		Date resultDate = assertInstanceOf(Date.class, result);
		String formattedDate = sdf.format(resultDate);
		assertEquals("1995-01-23", formattedDate);
	}

	@Test
	public void testPatternNowWithDateType() throws Exception {
		DateParameter p = new DateParameter();
		System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		try (PipeLineSession session = new PipeLineSession()) {
			p.setName("date");
			p.setPattern("{now}");
			p.setFormatType(DateFormatType.DATE);
			p.configure();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false); // Should return PutSystemDateInSession.FIXEDDATETIME
			Date resultDate = assertInstanceOf(Date.class, result);
			SimpleDateFormat sdf = new SimpleDateFormat(TYPE_DATE_PATTERN);
			String formattedDate = sdf.format(resultDate);
			String expectedDate = sdf.format(TimeProvider.nowAsDate()); // dit gaat echt meestal wel goed
			assertEquals(expectedDate, formattedDate);
		}
	}

	@Test
	public void testPatternFixedDateWithDateFormatTypeAndParameterTypeSet() throws Exception {
		String expectedDate = "2001-12-17 09:30:47.000";
		DateParameter p = new DateParameter();
		System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		try (PipeLineSession session = new PipeLineSession()) {
			p.setName("EsbSoapWrapperPipeTimestamp");
			p.setPattern("{fixeddate,date,yyyy-MM-dd'T'HH:mm:ss}");
			p.setFormatType(DateFormatType.TIMESTAMP);
			p.setFormatString("yyyy-MM-dd'T'HH:mm:ss");
			p.configure();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false); // Should return PutSystemDateInSession.FIXEDDATETIME
			assertInstanceOf(Date.class, result);

			SimpleDateFormat sdf = new SimpleDateFormat(DateFormatUtils.FORMAT_FULL_GENERIC);
			assertEquals(expectedDate, sdf.format(result));
		}
	}

	@Test
	public void testPatternFixedDateWithParameterTypeDateTime() throws Exception {
		String expectedDate = "2001-12-17 09:30:47.000";
		DateParameter p = new DateParameter();
		System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		try (PipeLineSession session = new PipeLineSession()) {
			p.setName("EsbSoapWrapperPipeTimestamp");
			p.setPattern("{fixeddate}");
			p.setFormatType(DateFormatType.DATETIME);
			p.configure();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false); // Should return PutSystemDateInSession.FIXEDDATETIME
			assertInstanceOf(Date.class, result);

			SimpleDateFormat sdf = new SimpleDateFormat(DateFormatUtils.FORMAT_FULL_GENERIC);
			assertEquals(expectedDate, sdf.format(result));
		}
	}

	@Test
	public void testPatternFixedDateWithParameterTypeTimestamp() throws Exception {
		String expectedDate = "2001-12-17 09:30:47.000";
		DateParameter p = new DateParameter();
		System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		try (PipeLineSession session = new PipeLineSession()) {
			p.setName("EsbSoapWrapperPipeTimestamp");
			p.setPattern("{fixeddate}");
			p.setFormatString("yyyy-MM-dd HH:mm:ss");
			p.setFormatType(DateFormatType.TIMESTAMP);
			p.configure();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false); // Should return PutSystemDateInSession.FIXEDDATETIME
			assertInstanceOf(Date.class, result);

			SimpleDateFormat sdf = new SimpleDateFormat(DateFormatUtils.FORMAT_FULL_GENERIC);
			assertEquals(expectedDate, sdf.format(result));
		}
	}

	@Test
	public void testPatternFixedDateWithExtendedDateFormatTypeAndParameterTypeSet() throws Exception {
		String expectedDate = "2001-12-17 09:30:47.000";
		DateParameter p = new DateParameter();
		System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		try (PipeLineSession session = new PipeLineSession()) {
			p.setName("EsbSoapWrapperPipeTimestamp");
			p.setPattern("{fixeddate,date,yyyy-MM-dd HH:mm:ss.SSS}");
			p.setFormatType(DateFormatType.TIMESTAMP);
			p.configure();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false); // Should return PutSystemDateInSession.FIXEDDATETIME
			assertInstanceOf(Date.class, result);

			SimpleDateFormat sdf = new SimpleDateFormat(DateFormatUtils.FORMAT_FULL_GENERIC);
			assertEquals(expectedDate, sdf.format(result));
		}
	}

	@Test
	public void testUnixParameterConvertsToDate() throws Exception {
		DateParameter p = new DateParameter();
		try (PipeLineSession session = new PipeLineSession()) {
			p.setName("date");
			p.setValue("1747401948"); // Value in seconds, not millis!!
			p.setFormatType(DateParameter.DateFormatType.UNIX);
			p.configure();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false);
			Date resultDate = assertInstanceOf(Date.class, result);
			assertEquals(1747401948_000L, resultDate.getTime());
		}
	}

	@Test
	public void testUnixParameterConvertsToDateChangeTZ() throws Exception {
		DateParameter p = new DateParameter();
		try (PipeLineSession session = new PipeLineSession()) {
			p.setName("date");
			p.setValue("1747401948"); // Value in seconds, not millis!!
			p.setFormatType(DateParameter.DateFormatType.UNIX);
			p.configure();

			// Change the system timezone to see if that affects how the date is resolved
			TimeProvider.setClock(Clock.systemUTC());
			TimeZone.setDefault(TimeZone.getTimeZone("Z"));

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false);
			Date resultDate = assertInstanceOf(Date.class, result);
			assertEquals(1747401948_000L, resultDate.getTime());
		}
	}

	@Test
	public void testUnixPatternConvertsToDate() throws Exception {
		TimeProvider.setTime(1747401948_000L);
		DateParameter p = new DateParameter();
		try (PipeLineSession session = new PipeLineSession()) {
			p.setName("unixTimestamp");
			p.setPattern("{now,millis,#}");
			p.setFormatType(DateFormatType.UNIX);
			p.configure();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false);
			Date resultDate = assertInstanceOf(Date.class, result);
			assertEquals(1747401948_000L, resultDate.getTime());
		}
	}

	@Test
	public void testUnixPatternConvertsToDateWithoutFormatType() throws Exception {
		TimeProvider.setTime(1747401948_000L);
		DateParameter p = new DateParameter();
		try (PipeLineSession session = new PipeLineSession()) {
			p.setName("unixTimestamp");
			p.setPattern("{now,millis}");
			p.configure();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false);
			Date resultDate = assertInstanceOf(Date.class, result);
			assertEquals(1747401948_000L, resultDate.getTime());
		}
	}

	@Test
	public void testParameterFromDateToDate() throws Exception {
		Date date = TimeProvider.nowAsDate();

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
	public void testTimeParameterWithTimePatternWithoutFormatPattern() throws Exception {

		// Arrange
		TimeProvider.setTime(LocalDateTime.of(2025, 3, 5, 11, 12, 55));
		DateParameter parameter = new DateParameter();
		parameter.setName("time");
		parameter.setFormatType(DateFormatType.TIME);
		parameter.setPattern("{now}"); // Now no longer necessary to specify "time" as part of the pattern when type is TIME.
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");
		PipeLineSession session = new PipeLineSession();

		// Act
		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);

		// Assert
		Date resultDate = assertInstanceOf(Date.class, result);
		Calendar resultCalendar = Calendar.getInstance();
		resultCalendar.setTime(resultDate);

		// Only a value for the time should be set. Year / Monday / Day are at Epoch start
		assertEquals(1970, resultCalendar.get(Calendar.YEAR));
		assertEquals(Calendar.JANUARY, resultCalendar.get(Calendar.MONTH));
		assertEquals(1, resultCalendar.get(Calendar.DAY_OF_MONTH));

		// Time-part should be set
		assertEquals(11, resultCalendar.get(Calendar.HOUR_OF_DAY));
		assertEquals(12, resultCalendar.get(Calendar.MINUTE));
		assertEquals(55, resultCalendar.get(Calendar.SECOND));
	}

	@Test
	public void testTimeParameterWithTimePatternWithoutFormat() throws Exception {

		// Arrange
		TimeProvider.setTime(LocalDateTime.of(2025, 3, 5, 11, 12, 55));
		DateParameter parameter = new DateParameter();
		parameter.setName("time");
		parameter.setFormatType(DateFormatType.TIME);
		parameter.setPattern("{now,time}"); // Specifying "time" in the pattern used to be required for TIME parameters now it is optional

		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");
		PipeLineSession session = new PipeLineSession();

		// Act
		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);

		// Assert
		Date resultDate = assertInstanceOf(Date.class, result);
		Calendar resultCalendar = Calendar.getInstance();
		resultCalendar.setTime(resultDate);

		// Only a value for the time should be set. Year / Monday / Day are at Epoch start
		assertEquals(1970, resultCalendar.get(Calendar.YEAR));
		assertEquals(Calendar.JANUARY, resultCalendar.get(Calendar.MONTH));
		assertEquals(1, resultCalendar.get(Calendar.DAY_OF_MONTH));

		// Time-part should be set
		assertEquals(11, resultCalendar.get(Calendar.HOUR_OF_DAY));
		assertEquals(12, resultCalendar.get(Calendar.MINUTE));
		assertEquals(55, resultCalendar.get(Calendar.SECOND));
	}

	@Test
	public void testTimeParameterWithTimePatternWithFormat() throws Exception {

		// Arrange
		TimeProvider.setTime(LocalDateTime.of(2025, 3, 5, 11, 12, 55));
		DateParameter parameter = new DateParameter();
		parameter.setName("time");
		parameter.setFormatType(DateFormatType.TIME);
		parameter.setPattern("{now,time,HH:mm:ss}"); // Specifying "time" in the pattern used to be required for TIME parameters now it is optional. A formatString if specified has to be this format for TIME parameters, cannot exclude the seconds (yet).
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");
		PipeLineSession session = new PipeLineSession();

		// Act
		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);

		// Assert
		Date resultDate = assertInstanceOf(Date.class, result);
		Calendar resultCalendar = Calendar.getInstance();
		resultCalendar.setTime(resultDate);

		// Only a value for the time should set. Year / Monday / Day are at Epoch start
		assertEquals(1970, resultCalendar.get(Calendar.YEAR));
		assertEquals(Calendar.JANUARY, resultCalendar.get(Calendar.MONTH));
		assertEquals(1, resultCalendar.get(Calendar.DAY_OF_MONTH));

		// Time-part should be set
		assertEquals(11, resultCalendar.get(Calendar.HOUR_OF_DAY));
		assertEquals(12, resultCalendar.get(Calendar.MINUTE));
		assertEquals(55, resultCalendar.get(Calendar.SECOND));
	}

	@Test
	@Disabled("Formatting time without seconds does not yet work and the change to make that possible looks too big to add to this PR")
	public void testTimeParameterWithTimePatternWithFormatNoSeconds() throws Exception {

		// Arrange
		TimeProvider.setTime(LocalDateTime.of(2025, 3, 5, 11, 12, 55));
		DateParameter parameter = new DateParameter();
		parameter.setName("time");
		parameter.setFormatType(DateFormatType.TIME);
		parameter.setPattern("{now,time,HH:mm}"); // Does not work if you do not specify 'time' in the pattern. If you specify a format, it HAS to be this format.
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");
		PipeLineSession session = new PipeLineSession();

		// Act
		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);

		// Assert
		Date resultDate = assertInstanceOf(Date.class, result);
		Calendar resultCalendar = Calendar.getInstance();
		resultCalendar.setTime(resultDate);

		// Only a value for the time should set. Year / Monday / Day are at Epoch start
		assertEquals(1970, resultCalendar.get(Calendar.YEAR));
		assertEquals(Calendar.JANUARY, resultCalendar.get(Calendar.MONTH));
		assertEquals(1, resultCalendar.get(Calendar.DAY_OF_MONTH));

		// Time-part should be set but without seconds
		assertEquals(11, resultCalendar.get(Calendar.HOUR_OF_DAY));
		assertEquals(12, resultCalendar.get(Calendar.MINUTE));
		assertEquals(0, resultCalendar.get(Calendar.SECOND));
	}

	@Test
	public void testDateParameterWithDatePatternWithoutFormat() throws Exception {

		// Arrange
		TimeProvider.setTime(LocalDateTime.of(2025, 3, 5, 11, 12, 55));
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
		Date resultDate = assertInstanceOf(Date.class, result);
		Calendar resultCalendar = Calendar.getInstance();
		resultCalendar.setTime(resultDate);

		// Only a value for the date should be set.
		assertEquals(2025, resultCalendar.get(Calendar.YEAR));
		assertEquals(Calendar.MARCH, resultCalendar.get(Calendar.MONTH));
		assertEquals(5, resultCalendar.get(Calendar.DAY_OF_MONTH));

		// Time-part should not be set
		assertEquals(0, resultCalendar.get(Calendar.HOUR_OF_DAY));
		assertEquals(0, resultCalendar.get(Calendar.MINUTE));
		assertEquals(0, resultCalendar.get(Calendar.SECOND));
	}

	@Test
	public void testDateParameterWithDatePatternWithFormat() throws Exception {

		// Arrange
		TimeProvider.setTime(LocalDateTime.of(2025, 3, 5, 11, 12, 55));
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
		Date resultDate = assertInstanceOf(Date.class, result);
		Calendar resultCalendar = Calendar.getInstance();
		resultCalendar.setTime(resultDate);
		assertEquals(2025, resultCalendar.get(Calendar.YEAR));
		assertEquals(Calendar.MARCH, resultCalendar.get(Calendar.MONTH));
		assertEquals(5, resultCalendar.get(Calendar.DAY_OF_MONTH));

		// Time-part should not be set
		assertEquals(0, resultCalendar.get(Calendar.HOUR_OF_DAY));
		assertEquals(0, resultCalendar.get(Calendar.MINUTE));
		assertEquals(0, resultCalendar.get(Calendar.SECOND));
	}
}

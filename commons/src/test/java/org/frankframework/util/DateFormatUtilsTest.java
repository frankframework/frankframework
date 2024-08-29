package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.TimeZone;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import lombok.extern.log4j.Log4j2;

/**
 * DateUtils Tester.
 * The assertions are in the form of testing the date in most cases, not the time
 * because of the time difference between timezones that might occur.
 *
 * @author Ricardo
 */
@Log4j2
public class DateFormatUtilsTest {

	private static final TimeZone CI_TZ = TimeZone.getDefault();
	private static final TimeZone TEST_TZ = TimeZone.getTimeZone("UTC");

	@BeforeAll
	public static void setUp() {
		if (!CI_TZ.hasSameRules(TEST_TZ)) {
			log.warn("CI TimeZone [{}] differs from test TimeZone [{}]", CI_TZ::getDisplayName, TEST_TZ::getDisplayName);
		}
	}

	/**
	 * Tests have been written in UTC, adjust the TimeZone for CI running with a different default TimeZone
	 */
	private Date adjustForTimezone(Date date) {
		if (CI_TZ.hasSameRules(TEST_TZ)) {
			return date;
		} else {
			return new Date(adjustForTimezone(date.getTime()));
		}
	}

	/**
	 * Tests have been written in UTC, adjust the TimeZone for CI running with a different default TimeZone
	 */
	private long adjustForTimezone(long epochMillis) {
		if (CI_TZ.hasSameRules(TEST_TZ)) {
			return epochMillis;
		} else {
			int offset = CI_TZ.getOffset(epochMillis);
			return epochMillis - offset;
		}
	}

	@Test
	public void testFormatLong() {
		String date = DateFormatUtils.format(adjustForTimezone(1380931200000L));
		assertEquals("2013-10-05 00:00:00.000", date);
	}

	@Test
	public void testFormatDate() {
		String date = DateFormatUtils.format(adjustForTimezone(new Date(1380931200000L)));
		assertEquals("2013-10-05 00:00:00.000", date);
	}

	@Test
	public void testFormatForDateDateFormat() {
		Date d = adjustForTimezone(new Date(1503600000L));
		String time = DateFormatUtils.format(d, DateFormatUtils.FULL_GENERIC_FORMATTER);
		assertEquals("1970-01-18 09:40:00.000", time);
	}

	@Test
	public void testParseToDate() {
		LocalDate date = DateFormatUtils.parseToLocalDate("05-10-13", DateFormatUtils.SHORT_DATE_FORMATTER);
		assertEquals(1380931200L, date.toEpochSecond(LocalTime.MIN, ZoneOffset.UTC));
	}

	@Test
	@Disabled("Currently cannot parse in either 2 or 4 digit year format. Have to be specific.")
	public void testParseToDateFullYear() {
		LocalDate date = DateFormatUtils.parseToLocalDate("05-10-2014", DateFormatUtils.SHORT_DATE_FORMATTER);
		assertEquals(1412467200L, date.toEpochSecond(LocalTime.MIN, ZoneOffset.UTC));
	}

	@Test
	public void unableToParseDate() {
		assertThrows(DateTimeParseException.class, () -> DateFormatUtils.parseToLocalDate("05/10/98", DateFormatUtils.SHORT_DATE_FORMATTER));
	}

	@Test
	public void unableToParseFullGenericWithoutTime() {
		assertThrows(DateTimeParseException.class, () -> DateFormatUtils.parseToInstant("2000-01-01", DateFormatUtils.FULL_GENERIC_FORMATTER));
	}

	@Test
	public void testParseToLocalDate() {
		LocalDate localDate = DateFormatUtils.parseToLocalDate("2024-05-16");
		assertAll(
				() -> assertEquals(2024, localDate.getYear()),
				() -> assertEquals(Month.MAY, localDate.getMonth()),
				() -> assertEquals(16, localDate.getDayOfMonth())
		);
	}

	@Test
	public void testParseLocalDateInvalidDatePattern() {
		assertThrows(IllegalArgumentException.class, () -> DateFormatUtils.parseToLocalDate("not a date"));
	}

	@Test
	public void testParseGenericDateInvalidDatePattern() {
		assertThrows(IllegalArgumentException.class, () -> DateFormatUtils.parseGenericDate("not a date"));
	}

	@Test
	public void testParseAnyDate1() {
		assertThrows(IllegalArgumentException.class, () -> DateFormatUtils.parseAnyDate("12-2013-10"));
	}

	@Test
	public void testParseAnyDate2() {
		Date date = DateFormatUtils.parseAnyDate("2013-12-10 12:41:43");
		assertEquals(adjustForTimezone(1386679303000L), date.getTime());
	}

	@Test
	public void testParseAnyDate3() {
		Date date = DateFormatUtils.parseAnyDate("05/10/98 05:47:13");
		assertEquals(adjustForTimezone(907566433000L), date.getTime());
	}

	@Test
	public void testParseAnyDate4() {
		Date date = DateFormatUtils.parseAnyDate("2013-12-10T12:41:43");
		assertEquals(adjustForTimezone(1386679303000L), date.getTime());
	}

	@Test
	public void testParseAnyDate5() {
		Date date = DateFormatUtils.parseAnyDate("2013-12-10");
		assertEquals(1386633600000L, date.getTime());
	}

	@Test
	public void testParseGenericDate1() {
		assertThrows(IllegalArgumentException.class, () -> DateFormatUtils.parseGenericDate("12-2013-10"));
	}

	@Test
	public void testParseGenericDate2() {
		Instant date = DateFormatUtils.parseGenericDate("2013-12-10 12:41:43");
		assertEquals(adjustForTimezone(1386679303000L), date.toEpochMilli());
	}

	@Test
	public void testParseGenericDate3() {
		// NB: At some point in future it will no longer make sense to refer to '98 and 1998.
		// As currently implemented, this test will start failing in 2058 because there's
		// rolling cut-off for what dates to interpret as 20th century instead of 21st that is
		// based on current year minus 60.
		Instant date = DateFormatUtils.parseGenericDate("05/10/98 05:47:13");
		assertEquals(adjustForTimezone(907566433000L), date.toEpochMilli());
	}

	@Test
	public void testParseGenericDate4() throws Exception {
		Instant date = DateFormatUtils.parseGenericDate("2013-12-10T12:41:43");
		assertEquals(adjustForTimezone(1386679303000L), date.toEpochMilli());
	}

	@Test
	public void testParseGenericDate5() throws Exception {
		Instant date = DateFormatUtils.parseGenericDate("2013-12-10");
		assertEquals(1386633600000L, date.toEpochMilli());
	}


	@Test
	public void testInstantInsteadOfDate() {
		// Arrange
		Instant theMoment = Instant.now()
				.atZone(ZoneOffset.UTC)
				.withYear(2023)
				.withMonth(5)
				.withDayOfMonth(4)
				.withHour(11)
				.withMinute(11)
				.withSecond(11)
				.toInstant();

		// Act
		String dateString = DateFormatUtils.format(theMoment, DateFormatUtils.GENERIC_DATETIME_FORMATTER.withZone(ZoneId.of("UTC")));

		// Assert
		assertEquals("2023-05-04 11:11:11", dateString);
	}
}

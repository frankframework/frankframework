package nl.nn.adapterframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
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

	private static final TimeZone CI_TZ = Calendar.getInstance().getTimeZone();
	private static final TimeZone TEST_TZ = TimeZone.getTimeZone("UTC");

	@BeforeAll
	public static void setUp() {
		if(!CI_TZ.hasSameRules(TEST_TZ)) {
			log.warn("CI TimeZone [{}] differs from test TimeZone [{}]", CI_TZ::getDisplayName, TEST_TZ::getDisplayName);
		}
	}

	/**
	 * Tests have been written in UTC, adjust the TimeZone for CI running with a different default TimeZone
	 */
	private Date getCorrectedDate(Date date) {
		// TODO: Rewrite this to not use Date and Calendar classes. In principle, this shouldn't matter at all with use of proper java.time.Instant
		if(CI_TZ.hasSameRules(TEST_TZ)) {
			return date;
		} else {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);
			int offset = CI_TZ.getOffset(calendar.getTime().getTime());
			calendar.add(Calendar.MILLISECOND, - offset);
			log.info("adjusting date [{}] with offset [{}] to [{}]", ()->date, ()->offset, calendar::getTime);
			return calendar.getTime();
		}
	}

	/**
	 * Tests have been written in UTC, adjust the TimeZone for CI running with a different default TimeZone
	 */
	private long getCorrectedDate(long l) {
		Date date = new Date(l);
		return getCorrectedDate(date).getTime();
	}

	@Test
	public void testFormatLong() {
		String date = DateFormatUtils.format(getCorrectedDate(1380931200000L));
		assertEquals("2013-10-05 00:00:00.000", date);
	}

	@Test
	public void testFormatDate() {
		String date = DateFormatUtils.format(getCorrectedDate(new Date(1380931200000L)));
		assertEquals("2013-10-05 00:00:00.000", date);
	}

	@Test
	public void testFormatForDateDateFormat() {
		Date d = getCorrectedDate(new Date(1503600000L));
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
		assertThrows(DateTimeParseException.class, ()->DateFormatUtils.parseToLocalDate("05/10/98", DateFormatUtils.SHORT_DATE_FORMATTER));
	}

	@Test
	public void unableToParseFullGenericWithoutTime() {
		assertThrows(DateTimeParseException.class, ()-> DateFormatUtils.parseToInstant("2000-01-01", DateFormatUtils.FULL_GENERIC_FORMATTER));
	}

	@Test
	public void testParseAnyDate1() throws Exception {
		Date date = DateFormatUtils.parseAnyDate("12-2013-10");
		assertEquals(getCorrectedDate(1386633600000L), date.getTime());
	}

	@Test
	public void testParseAnyDate2() throws Exception {
		Date date = DateFormatUtils.parseAnyDate("2013-12-10 12:41:43");
		assertEquals(getCorrectedDate(1386679303000L), date.getTime());
	}

	@Test
	public void testParseAnyDate3() throws Exception {
		Date date = DateFormatUtils.parseAnyDate("05/10/98 05:47:13");
		assertEquals(getCorrectedDate(907566433000L), date.getTime());
	}

	@Test
	public void testParseAnyDate4() throws Exception {
		Date date = DateFormatUtils.parseAnyDate("2013-12-10T12:41:43");
		assertEquals(getCorrectedDate(1386679303000L), date.getTime());
	}


	@Test
	public void testInstantInsteadOfDate() {
		// Arrange
		Instant theMoment = Instant.now().atZone(ZoneOffset.UTC).withYear(2023).withMonth(5).withDayOfMonth(4).withHour(11).withMinute(11).withSecond(11).toInstant();

		// Act
		String dateString = DateFormatUtils.format(theMoment, DateFormatUtils.GENERIC_DATETIME_FORMATTER.withZone(ZoneId.of("UTC")));

		// Assert
		assertEquals("2023-05-04 11:11:11", dateString);
	}
}

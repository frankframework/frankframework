package nl.nn.adapterframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.jupiter.api.BeforeAll;
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
public class DateUtilsTest {

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
	public void testFormatLong() throws Exception {
		String date = DateUtils.format(getCorrectedDate(1380931200000L));
		assertEquals("2013-10-05 00:00:00.000", date);
	}

	@Test
	public void testFormatDate() throws Exception {
		String date = DateUtils.format(getCorrectedDate(new Date(1380931200000L)));
		assertEquals("2013-10-05 00:00:00.000", date);
	}

	@Test
	public void testFormatForDateDateFormat() throws Exception {
		Date d = getCorrectedDate(new Date(1503600000));
		String time = DateUtils.format(d, DateUtils.FORMAT_FULL_GENERIC);
		assertEquals("1970-01-18 09:40:00.000", time);

	}

	@Test
	public void testParseToDate() throws Exception {
		Date date = DateUtils.parseToDate("05-10-13", DateUtils.FORMAT_DATE);
		assertEquals(getCorrectedDate(1380931200000L), date.getTime());
	}

	@Test
	public void testParseToDateFullYear() throws Exception {
		Date date = DateUtils.parseToDate("05-10-2014", DateUtils.FORMAT_DATE);
		assertEquals(getCorrectedDate(1412467200000L), date.getTime());
	}

	@Test
	public void unableToParseDate() throws Exception {
		Date date = DateUtils.parseToDate("05/10/98", DateUtils.FORMAT_DATE);
		assertNull(date);
	}

	@Test
	public void unableToParseFullGenericWithoutTime() throws Exception {
		Date date = DateUtils.parseToDate("2000-01-01", DateUtils.FORMAT_FULL_GENERIC);
		assertNull(date);
	}

	@Test
	public void testParseXmlDate() throws Exception {
		Date date = DateUtils.parseXmlDateTime("2013-12-10");
		assertEquals(getCorrectedDate(1386633600000L), date.getTime());
	}

	@Test
	public void testParseXmlDateTime() throws Exception {
		Date date = DateUtils.parseXmlDateTime("2013-12-10T12:41:43");
		assertEquals(getCorrectedDate(1386679303000L), date.getTime());
	}

	@Test
	public void testParseXmlInvalidDateTime() throws Exception {
		assertThrows(IllegalArgumentException.class, ()-> DateUtils.parseXmlDateTime("2013-12-10 12:41:43"));
	}

	@Test
	public void testParseAnyDate1() throws Exception {
		Date date = DateUtils.parseAnyDate("12-2013-10");
		assertEquals(getCorrectedDate(1386633600000L), date.getTime());
	}

	@Test
	public void testParseAnyDate2() throws Exception {
		Date date = DateUtils.parseAnyDate("2013-12-10 12:41:43");
		assertEquals(getCorrectedDate(1386679303000L), date.getTime());
	}

	@Test
	public void testParseAnyDate3() throws Exception {
		Date date = DateUtils.parseAnyDate("05/10/98 05:47:13");
		assertEquals(getCorrectedDate(907566433000L), date.getTime());
	}

	@Test
	public void testConvertDate() throws Exception {
		String date = DateUtils.convertDate(DateUtils.FORMAT_DATE, DateUtils.FORMAT_FULL_GENERIC, "18-03-13");
		assertEquals("2013-03-18 00:00:00.000", date);
	}

	@Test
	public void testChangeDateForDateYearsMonthsDays() throws Exception {
		String date = DateUtils.changeDate("2013-10-10", 2, 3, 5);
		assertEquals("2016-01-15", date);
	}

	@Test
	public void testChangeDateForDateYearsMonthsDaysDateFormat() throws Exception {
		String date = DateUtils.changeDate("10-10-13", 2, 3, 5, DateUtils.FORMAT_DATE);
		assertEquals("15-01-16", date);
	}

	@Test
	public void testIsSameDay() throws Exception {
		Date d1 = DateUtils.parseAnyDate("10-10-2013");
		Date d2 = DateUtils.parseAnyDate("2013-10-10");
		boolean b = DateUtils.isSameDay(d1, d2);
		assertEquals(true, b);
	}

	@Test
	public void Should_ReturnDateObject_When_StringWithDateIsProvided() {
		String s = "2023-12-09";
		assertInstanceOf(Date.class, DateUtils.parseXmlDateTime(s));
	}

	@Test
	public void Should_ReturnDateObject_When_StringWithDateTimeIsProvided() {
		String s = "2023-12-09T00:00:00";
		assertInstanceOf(Date.class, DateUtils.parseXmlDateTime(s));
	}

	@Test
	public void Should_ReturnDateObject_When_StringWithFebruary29thInLeapYearIsProvided() {
		String s = "2024-02-29T00:00:00";
		assertInstanceOf(Date.class, DateUtils.parseXmlDateTime(s));
	}

	@Test
	public void Should_ReturnDateObject_When_StringWithDateTimeWithYear2400IsProvided() {
		String s = "2400-02-29T18:08:05";
		assertInstanceOf(Date.class, DateUtils.parseXmlDateTime(s));
	}

	@Test
	public void Should_ThrowError_When_StringWithHoursOutOfBoundsIsProvided() {
		String s = "2023-13-09T24:08:05";
		assertThrows(DateTimeParseException.class, () -> DateUtils.parseXmlDateTime(s));
	}

	@Test
	public void Should_ThrowError_When_StringWithMinutesOutOfBoundsIsProvided() {
		String s = "2023-13-09T18:60:05";
		assertThrows(DateTimeParseException.class, () -> DateUtils.parseXmlDateTime(s));
	}

	@Test
	public void Should_ThrowError_When_StringWithSecondsOutOfBoundsIsProvided() {
		String s = "2023-13-09T18:08:60";
		assertThrows(DateTimeParseException.class, () -> DateUtils.parseXmlDateTime(s));
	}

	@Test
	public void Should_ThrowError_When_StringWithMonthOutOfBoundsIsProvided() {
		String s = "2023-13-09T18:08:05";
		assertThrows(DateTimeParseException.class, () -> DateUtils.parseXmlDateTime(s));
	}

	@Test
	public void Should_ThrowError_When_StringWithDayOutOfBoundsIsProvided() {
		String s = "2023-11-31T18:08:05";
		assertThrows(DateTimeParseException.class, () -> DateUtils.parseXmlDateTime(s));
	}

	@Test //A year is a leap year when it dividable by 4, but not if dividable by 100, except dividable by 400
	public void Should_ThrowError_When_StringWithDateTimeWithYearDividableBy100WithFebruary29thIsProvided() {
		String s = "2100-02-29T18:08:05";
		assertThrows(DateTimeParseException.class, () -> DateUtils.parseXmlDateTime(s));
	}
}

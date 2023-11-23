package nl.nn.adapterframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
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
		Date d = getCorrectedDate(new Date(1503600000));
		String time = DateFormatUtils.format(d, DateFormatUtils.FORMAT_FULL_GENERIC);
		assertEquals("1970-01-18 09:40:00.000", time);
	}

	@Test
	public void testParseToDate() {
		Date date = DateFormatUtils.parseToDate("05-10-13", DateFormatUtils.FORMAT_DATE);
		assertEquals(getCorrectedDate(1380931200000L), date.getTime());
	}

	@Test
	public void testParseToDateFullYear() {
		Date date = DateFormatUtils.parseToDate("05-10-2014", DateFormatUtils.FORMAT_DATE);
		assertEquals(getCorrectedDate(1412467200000L), date.getTime());
	}

	@Test
	public void unableToParseDate() {
		Date date = DateFormatUtils.parseToDate("05/10/98", DateFormatUtils.FORMAT_DATE);
		assertNull(date);
	}

	@Test
	public void unableToParseFullGenericWithoutTime() {
		Date date = DateFormatUtils.parseToDate("2000-01-01", DateFormatUtils.FORMAT_FULL_GENERIC);
		assertNull(date);
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
	public void testInstantInsteadOfDate() {
		DateFormat format = new SimpleDateFormat(DateFormatUtils.FORMAT_GENERICDATETIME);
		String dateString = format.format(Instant.now().toEpochMilli());
		System.out.println(dateString);
		assertInstanceOf(String.class, dateString);
	}
}

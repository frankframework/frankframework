package nl.nn.adapterframework.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * DateUtils Tester.
 * The assertions are in the form of testing the date in most cases, not the time
 * because of the time difference between the EU and US where Travis servers are located.
 * @author <Sina Sen>
 */
public class DateUtilsTest {

	private static final TimeZone CI_TZ = Calendar.getInstance().getTimeZone();
	private static final TimeZone TEST_TZ = TimeZone.getTimeZone("UTC");
	private static Logger LOG = LogUtil.getLogger(DateUtilsTest.class);

	@BeforeClass
	public static void setUp() {
		LOG.info("adjusting date settings from [{}] to [{}]", CI_TZ::getDisplayName, TEST_TZ::getDisplayName);
	}

	/**
	 * Tests have been written in UTC, adjust the TimeZone for CI running with a different default TimeZone
	 */
	private Date getCorrectedDate(Date date) {
		if (CI_TZ.hasSameRules(TEST_TZ)) {
			return date;
		} else {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);
			calendar.add(Calendar.MILLISECOND, - CI_TZ.getOffset(calendar.getTime().getTime()));
			return calendar.getTime();
//			ZoneOffset zoneOffSet= ZoneOffset.of("+02:00");
//			OffsetDateTime offsetDateTime = OffsetDateTime.now(zoneOffSet);
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

	@Test(expected = IllegalArgumentException.class)
	public void testParseXmlInvalidDateTime() throws Exception {
		DateUtils.parseXmlDateTime("2013-12-10 12:41:43");
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
}

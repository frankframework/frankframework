package nl.nn.adapterframework.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

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
	private static final int TZ_DIFF = (CI_TZ.getRawOffset() - CI_TZ.getDSTSavings());

	@BeforeClass
	public static void setUp() {
		if(TZ_DIFF != 0) {
			System.out.println("adjusting date settings from ["+CI_TZ.getDisplayName()+"] to [" + TEST_TZ.getDisplayName() + "] offset ["+TZ_DIFF+"]");
		}
	}

	/**
	 * Tests have been written in UTC, adjust the TimeZone so Travis/Azure/GitHub CI don't fail when running in other TimeZones
	 */
	private Date getCorrectedDate(Date date) {
		if(TZ_DIFF != 0) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);
			calendar.add(Calendar.MILLISECOND, -TZ_DIFF);
			calendar.setTimeZone(TEST_TZ);
			return calendar.getTime();
		} else {
			return date;
		}
	}

	/**
	 * Tests have been written in UTC, adjust the TimeZone so Travis/Azure/GitHub CI don't fail when running in other TimeZones
	 */
	private long getCorrectedDate(long l) {
		Date date = new Date(l);
		return getCorrectedDate(date).getTime();
	}

	@Test
	public void testFormatLong() throws Exception {
		String date = DateUtils.format(getCorrectedDate(1380924000000L));
		assertEquals("2013-10-05 00:00:00.000", date);
	}

	@Test
	public void testFormatDate() throws Exception {
		String date = DateUtils.format(getCorrectedDate(new Date(1380924000000L)));
		assertEquals("2013-10-05 00:00:00.000", date);
	}

	@Test
	public void testFormatForDateDateFormat() throws Exception {
		Date d = getCorrectedDate(new Date(1500000000));
		String time = DateUtils.format(d, DateUtils.FORMAT_FULL_GENERIC);
		assertEquals("1970-01-18 09:40:00.000", time);

	}

	@Test
	public void testParseToDate() throws Exception {
		Date date = DateUtils.parseToDate("05-10-13", DateUtils.FORMAT_DATE);
		assertEquals(getCorrectedDate(1380924000000L), date.getTime());
	}

	@Test
	public void testParseToDateFullYear() throws Exception {
		Date date = DateUtils.parseToDate("05-10-2014", DateUtils.FORMAT_DATE);
		assertEquals(getCorrectedDate(1412460000000L), date.getTime());
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
		assertEquals(getCorrectedDate(1386630000000L), date.getTime());
	}

	@Test
	public void testParseXmlDateTime() throws Exception {
		Date date = DateUtils.parseXmlDateTime("2013-12-10T12:41:43");
		assertEquals(getCorrectedDate(1386675703000L), date.getTime());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testParseXmlInvalidDateTime() throws Exception {
		DateUtils.parseXmlDateTime("2013-12-10 12:41:43");
	}

	@Test
	public void testParseAnyDate1() throws Exception {
		Date date = DateUtils.parseAnyDate("12-2013-10");
		assertEquals(getCorrectedDate(1386630000000L), date.getTime());
	}

	@Test
	public void testParseAnyDate2() throws Exception {
		Date date = DateUtils.parseAnyDate("2013-12-10 12:41:43");
		assertEquals(getCorrectedDate(1386675703000L), date.getTime());
	}

	@Test
	public void testParseAnyDate3() throws Exception {
		Date date = DateUtils.parseAnyDate("05/10/98 05:47:13");
		assertEquals(getCorrectedDate(907559233000L), date.getTime());
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

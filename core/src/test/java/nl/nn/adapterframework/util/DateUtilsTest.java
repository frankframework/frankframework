package nl.nn.adapterframework.util;

import org.junit.Test;

import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

/**
 * DateUtils Tester.
 * The assertions are in the form of testing the date in most cases, not the time
 * because of the time difference between the EU and US where Travis servers are located.
 * @author <Sina Sen>
 */
public class DateUtilsTest {

	public Calendar now = Calendar.getInstance();
	public String tz = now.getTimeZone().getDisplayName(false, TimeZone.SHORT);

	/**
	 * Method: format(Date date, String dateFormat)
	 */
	@Test
	public void testFormatForDateDateFormat() throws Exception {
		Date d = new Date(1500000000);
		String s = DateUtils.format(d, DateUtils.FORMAT_FULL_GENERIC);
		assertEquals("1970-01-18", s.substring(0, 10));

	}

	/**
	 * Method: parseToDate(String s, String dateFormat)
	 */
	@Test
	public void testParseToDate() throws Exception {
		Date d = DateUtils.parseToDate("05-10-13", DateUtils.FORMAT_DATE);
		int len = d.toString().length();
		assertEquals("Sat Oct 05", d.toString().substring(0, 10));
		assertEquals("2013", d.toString().substring(len-4, len));
	}

	/**
	 * Method: parseXmlDateTime(String s)
	 */
	@Test
	public void testParseXmlDateTime() throws Exception {
		Date d = DateUtils.parseXmlDateTime("2013-10-10");
		int len = d.toString().length();
		assertEquals("Thu Oct 10", d.toString().substring(0, 10));
		assertEquals("2013", d.toString().substring(len-4, len));
	}

	/**
	 * Method: parseAnyDate(String dateInAnyFormat)
	 */
	@Test
	public void testParseAnyDate() throws Exception {
		Date d = DateUtils.parseAnyDate("10-2013-10");
		int len = d.toString().length();
		assertEquals("Thu Oct 10", d.toString().substring(0, 10));
		assertEquals("2013", d.toString().substring(len-4, len));

	}

	/**
	 * Method: formatOptimal(Date d)
	 */
	@Test
	public void testFormatOptimal() throws Exception {
		Date d = new Date(1500000000);
		String s = DateUtils.formatOptimal(d);
		assertEquals("1970-01-18 09:40", s);
	}

	/**
	 * Method: nextHigherValue(Date d)
	 */
	@Test
	public void testNextHigherValue() throws Exception {
		Date d = new Date(1500000000);
		Date s = DateUtils.nextHigherValue(d);
		assertEquals("Sun Jan 18", s.toString().substring(0, 10));
	}

	/**
	 * Method: convertDate(String from, String to, String value)
	 */
	@Test
	public void testConvertDate() throws Exception {
		String s = DateUtils.convertDate(DateUtils.FORMAT_DATE, DateUtils.FORMAT_FULL_GENERIC, "18-03-13");
		assertEquals("2013-03-18 00:00:00.000", s);
	}

	/**
	 * Method: changeDate(String date, int years, int months, int days)
	 */
	@Test
	public void testChangeDateForDateYearsMonthsDays() throws Exception {
		String date = DateUtils.changeDate("2013-10-10", 2, 3, 5);
		assertEquals("2016-01-15", date);
	}

	/**
	 * Method: changeDate(String date, int years, int months, int days, String dateFormat)
	 */
	@Test
	public void testChangeDateForDateYearsMonthsDaysDateFormat() throws Exception {
		String date = DateUtils.changeDate("10-10-13", 2, 3, 5, DateUtils.FORMAT_DATE);
		assertEquals("15-01-16", date);
	}

	/**
	 * Method: isSameDay(Date date1, Date date2)
	 */
	@Test
	public void testIsSameDay() throws Exception {
		Date d1 = DateUtils.parseAnyDate("10-10-2013");
		Date d2 = DateUtils.parseAnyDate("2013-10-10");
		boolean b = DateUtils.isSameDay(d1, d2);
		assertEquals(true, b);
	}


}

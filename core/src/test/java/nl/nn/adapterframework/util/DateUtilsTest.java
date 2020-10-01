package nl.nn.adapterframework.util;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * DateUtils Tester.
 *
 * @author <Sina Sen>
 */
public class DateUtilsTest {


	/**
	 * Method: format(Date date, String dateFormat)
	 */
	@Test
	public void testFormatForDateDateFormat() throws Exception {
		Date d = new Date(1500000000);
		String s = DateUtils.format(d, DateUtils.FORMAT_FULL_GENERIC);
		assertEquals("1970-01-18 09:40:00.000", s);

	}

	/**
	 * Method: parseToDate(String s, String dateFormat)
	 */
	@Test
	public void testParseToDate() throws Exception {
		Date d = DateUtils.parseToDate("05-10-13", DateUtils.FORMAT_DATE);
		assertEquals("Sat Oct 05 00:00:00 CEST 2013", d.toString());
	}

	/**
	 * Method: parseXmlDateTime(String s)
	 */
	@Test
	public void testParseXmlDateTime() throws Exception {
		Date d = DateUtils.parseXmlDateTime("2013-10-10");
		assertEquals("Thu Oct 10 00:00:00 CEST 2013", d.toString());
	}

	/**
	 * Method: parseAnyDate(String dateInAnyFormat)
	 */
	@Test
	public void testParseAnyDate() throws Exception {
		Date d = DateUtils.parseAnyDate("10-2013-10");
		assertEquals("Thu Oct 10 00:00:00 CEST 2013", d.toString());

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
		assertEquals("Sun Jan 18 09:41:00 CET 1970", s.toString());
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

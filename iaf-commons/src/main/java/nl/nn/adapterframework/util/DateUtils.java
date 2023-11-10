/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.util;

import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

import lombok.extern.log4j.Log4j2;

/**
 * Utilities for formatting and parsing dates.
 *
 * @author Johan Verrips IOS
 */
@Log4j2
public class DateUtils {
	public static final String fullIsoFormat          = "yyyy-MM-dd'T'HH:mm:sszzz";
	public static final String shortIsoFormat         = "yyyy-MM-dd";

	public static final String FORMAT_FULL_GENERIC      = "yyyy-MM-dd HH:mm:ss.SSS";
	public static final String FORMAT_MILLISECONDS	   ="######.###";
	public static final String FORMAT_GENERICDATETIME  ="yyyy-MM-dd HH:mm:ss";
	public static final String FORMAT_DATE             ="dd-MM-yy";
	public static final String FORMAT_TIME_HMS         ="HH:mm:ss";

	public static String format(Date date, String dateFormat) {
		SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
		return formatter.format(date);
	}

	public static String format(long date, String dateFormat) {
		return format(new Date(date), dateFormat);
	}

	public static String format(long date) {
		return format(new Date(date), FORMAT_FULL_GENERIC);
	}
	public static String format(Date date) {
		return format(date, FORMAT_FULL_GENERIC);
	}


	/**
	 * Get current date-time timestamp in ISO 8601 format.
	 */
	public static String getIsoTimeStamp() {
		return format(new Date(), fullIsoFormat);
	}

	/**
	 * Get current date-time timestamp in generic format.
	 */
	public static String getTimeStamp() {
		return format(new Date(), FORMAT_GENERICDATETIME);
	}

	/**
	 * Parses a string to a Date, according to the pattern
	 */
	public static Date parseToDate(String s, String dateFormat) {
		SimpleDateFormat df = new SimpleDateFormat(dateFormat);
		ParsePosition p = new ParsePosition(0);
		return df.parse(s, p);
	}

	/**
	 * Parses a string to a Date using LocalDateTime with SystemDefault ZoneId
	 */
	public static Date parseXmlDateTime(String s) {
		s = s.trim();
		if (!s.contains("T") && s.length() == 10) {
			s += "T00:00:00";
		}
		LocalDateTime localDateTime = LocalDateTime.parse(s);
		return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
	}
	/**
	 * Parses a string to a Date using CalendarParser
	 */
	public static Date parseAnyDate(String dateInAnyFormat) throws CalendarParserException {
		Calendar c = CalendarParser.parse(dateInAnyFormat);
		return new Date(c.getTimeInMillis());
	}

	/**
	 * Convert date format
	 *
	 * @param 	from	String	date format from.
	 * @param 	to		String	date format to.
	 * @param 	value	String	date to reformat.
	 */
	public static String convertDate(String from, String to, String value) throws ParseException {
		log.debug("convertDate from [" + from + "] to [" + to + "] value [" + value + "]");
		String result = "";

		SimpleDateFormat formatterFrom = new SimpleDateFormat(from);
		SimpleDateFormat formatterTo = new SimpleDateFormat(to);
		Date d = formatterFrom.parse(value);
		String tempStr = formatterFrom.format(d);

		if (tempStr.equals(value)) {
			result = formatterTo.format(d);
		} else {
			log.warn("Error on validating input (" + value + ") with reverse check [" + tempStr+"]");
			throw new ParseException("Error on validating input (" + value + ") with reverse check [" + tempStr+"]",0);
		}

		log.debug("convertDate result ["+result+"]");
		return result;
	}


	/**
	 *
	 * Add a number of years, months, days to a date specified in a shortIsoFormat, and return it in the same format.
	 * Als een datum component niet aangepast hoeft te worden, moet 0 meegegeven worden.
	 * Dus bijv: changeDate("2006-03-23", 2, 1, -4) = "2008-05-19"
	 *
	 * @param 	date	A String representing a date in format yyyy-MM-dd.
	 * @param 	years
	 * @param 	months
	 * @param 	days
	 */
	public static String changeDate(String date, int years, int months, int days) throws ParseException {
		return changeDate(date, years, months, days, "yyyy-MM-dd");
	}

	/**
	 *
	 * Add a number of years, months, days to a date specified in a certain format, and return it in the same format.
	 * Als een datum component niet aangepast hoeft te worden, moet 0 meegegeven worden.
	 * Dus bijv: changeDate("2006-03-23", 2, 1, -4) = "2008-05-19"
	 *
	 * @param 	date	A String representing a date in format (dateFormat).
	 * @param 	years	int
	 * @param 	months	int
	 * @param 	days	int
	 * @param 	dateFormat	A String representing the date format of date.
	 */
	public static String changeDate(String date, int years, int months, int days, String dateFormat) throws ParseException {
		if (log.isDebugEnabled()) log.debug("changeDate date ["+date+"] years ["+years+"] months ["+months+"] days ["+days+"]");
		String result = "";

		SimpleDateFormat df = new SimpleDateFormat(dateFormat);
		Date d = df.parse(date);
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		cal.add(Calendar.YEAR, years);
		cal.add(Calendar.MONTH, months);
		cal.add(Calendar.DAY_OF_MONTH, days);
		result = df.format(cal.getTime());

		log.debug("changeDate result ["+result+"]");
		return result;
	}

	public static boolean isSameDay(Date date1, Date date2) {
		return org.apache.commons.lang3.time.DateUtils.isSameDay(date1, date2);
	}

}

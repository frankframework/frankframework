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

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import lombok.extern.log4j.Log4j2;

/**
 * Utilities for formatting and parsing dates.
 *
 * @author Johan Verrips IOS
 */
@Log4j2
public class DateFormatUtils {
	public static final String FORMAT_FULL_ISO = "yyyy-MM-dd'T'HH:mm:sszzz";
	public static final String FORMAT_FULL_ISO_TIMESTAMP_NO_TZ = "yyyy-MM-dd'T'HH:mm:ss.SSS";
	public static final DateFormat FULL_ISO_FORMATTER = new SimpleDateFormat(FORMAT_FULL_ISO);
	public static final String FORMAT_SHORT_ISO = "yyyy-MM-dd";

	public static final String FORMAT_FULL_GENERIC = "yyyy-MM-dd HH:mm:ss.SSS";
	public static final DateFormat FULL_GENERIC_FORMATTER = new SimpleDateFormat(FORMAT_FULL_GENERIC);
	public static final String FORMAT_MILLISECONDS = "######.###";
	public static final String FORMAT_GENERICDATETIME = "yyyy-MM-dd HH:mm:ss";
	public static final DateFormat GENERIC_DATETIME_FORMATTER = new SimpleDateFormat(FORMAT_GENERICDATETIME);
	public static final String FORMAT_DATE = "dd-MM-yy";
	public static final String FORMAT_TIME_HMS = "HH:mm:ss";


	public static String now() {
		return format(new Date());
	}

	@Deprecated
	public static String now(String format) {
		return format(new Date(), new SimpleDateFormat(format));
	}

	public static String format(long date) {
		return format(new Date(date));
	}

	public static String format(long date, String format) {
		return format(new Date(date), format);
	}

	public static String format(Date date) {
		return format(date, FULL_GENERIC_FORMATTER);
	}

	public static String format(Date date, DateFormat formatter) {
		return formatter.format(date);
	}

	public static String format(Date date, String dateFormat) {
		return format(date, new SimpleDateFormat(dateFormat));
	}

	/**
	 * Get current date-time timestamp in generic format.
	 */
	public static String getTimeStamp() {
		return now(FORMAT_GENERICDATETIME);
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
	 * Parses a string to a Date using CalendarParser
	 */
	@Deprecated
	public static Date parseAnyDate(String dateInAnyFormat) throws CalendarParserException {
		Calendar c = CalendarParser.parse(dateInAnyFormat.replace('T', ' ')); // "Parse any format" doesn't parse ISO dates with a 'T' between date and time.
		return new Date(c.getTimeInMillis());
	}
}

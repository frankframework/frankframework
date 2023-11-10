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

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;

import org.apache.xmlbeans.GDate;

import lombok.extern.log4j.Log4j2;

/**
 * Utilities for formatting and parsing dates.
 *
 * @author Johan Verrips IOS
 */
@Log4j2
public class DateUtils {
	public static final String fullIsoFormat = "yyyy-MM-dd'T'HH:mm:sszzz";
	public static final DateTimeFormatter fullIsoFormatter = DateTimeFormatter.ofPattern(fullIsoFormat);
	public static final DateTimeFormatter shortIsoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd[' ']['T'][H:mm[:ss[.S]]][X]");
	public static final String FORMAT_FULL_GENERIC = "yyyy-MM-dd HH:mm:ss.SSS";
	public static final DateTimeFormatter FULL_GENERIC_FORMATTER = DateTimeFormatter.ofPattern(FORMAT_FULL_GENERIC);
	public static final String FORMAT_MILLISECONDS = "######.###";
	public static final DateTimeFormatter GENERIC_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	public static final String FORMAT_DATE = "dd-MM-yy";
	public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(FORMAT_DATE);
	public static final DateTimeFormatter TIME_HMS_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

	public static String format(Instant instant, String dateFormat) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
		return format(instant, formatter);
	}

	public static String format(Instant instant, DateTimeFormatter formatter) {
		LocalDateTime ldt = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
		return ldt.format(formatter);
	}

	public static String format(Date date, String dateFormat) {
		return format(date.toInstant(), dateFormat);
	}

	public static String format(Date date, DateTimeFormatter formatter) {
		return format(date.toInstant(), formatter);
	}

	public static String format(long date, String dateFormat) {
		return format(Instant.ofEpochMilli(date), dateFormat);
	}

	public static String format(long date, DateTimeFormatter formatter) {
		return format(Instant.ofEpochMilli(date), formatter);
	}

	public static String format(long date) {
		return format(Instant.ofEpochMilli(date), FULL_GENERIC_FORMATTER);
	}

	public static String format(Date date) {
		return format(date.toInstant(), FULL_GENERIC_FORMATTER);
	}

	/**
	 * Get current date-time timestamp in ISO 8601 format.
	 */
	public static String getIsoTimeStamp() {
		return format(Instant.now(), fullIsoFormatter);
	}

	/**
	 * Get current date-time timestamp in generic format.
	 */
	public static String getTimeStamp() {
		return format(Instant.now(), GENERIC_DATETIME_FORMATTER);
	}

	/**
	 * Parses a string to a Date, according to the pattern
	 */
	public static Date parseToDate(String s, String pattern) throws DateTimeException {
		return parseToDate(s, DateTimeFormatter.ofPattern(addTimePatternIfNecessary(pattern)));
	}

	private static String addTimePatternIfNecessary(String pattern) {
		String timePattern = "'T'HH:mm:ss";
		if (!pattern.contains(timePattern)) {
			pattern += timePattern;
		}
		return pattern;
	}

	public static Date parseToDate(String s, DateTimeFormatter df) throws DateTimeException {
		Instant instant = parseToInstant(s, df);
		if (instant == null) {
			return null;
		}
		return new Date(instant.toEpochMilli());
	}

	private static String addTimeIfNecessary(String value) {
		value = value.replace("/", "-");
		String[] splitValues = value.split("[-:]");
        if (splitValues.length == 3) {
            String value1 = getString(value, splitValues);
            if (value1 != null) return value1;
            return value.trim() + "T00:00:00";
        }
		return value;
	}

	private static String getString(String value, String[] splitValues) {
		boolean yearHasMoreThanTwoChars = false;
		for (String splitValue : splitValues) {
			if (splitValue.length() > 2) {
				yearHasMoreThanTwoChars = true;
				break;
			}
		}
		if (!yearHasMoreThanTwoChars) {
			return value;
		}
		return null;
	}

	public static Instant parseToInstant(String s, String pattern) throws DateTimeException {
		return parseToInstant(s, DateTimeFormatter.ofPattern(addTimePatternIfNecessary(pattern)));
	}

	public static Instant parseToInstant(String s, DateTimeFormatter df) throws DateTimeParseException {
		LocalDateTime ldt = LocalDateTime.parse(addTimeIfNecessary(s), df);
		return ldt.atZone(ZoneId.systemDefault()).toInstant();
	}

	/**
	 * Parses a string to a Date using XML Schema dateTime data type (GDate)
	 */
	public static Date parseXmlDateTime(String s) {
		GDate gdate = new org.apache.xmlbeans.GDate(s);
		return gdate.getDate();
	}

	/**
	 * Parses a string to a Date using CalendarParser
	 */
	public static Date parseAnyDate(String dateInAnyFormat) throws CalendarParserException {
		Calendar c = CalendarParser.parse(dateInAnyFormat);
		return new Date(c.getTimeInMillis());
	}

	public static boolean isSameDay(Date date1, Date date2) {
		return org.apache.commons.lang3.time.DateUtils.isSameDay(date1, date2);
	}

}

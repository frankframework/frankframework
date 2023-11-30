/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2023 WeAreFrank!

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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.TemporalQueries;
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
	public static final String FORMAT_FULL_GENERIC = "yyyy-MM-dd HH:mm:ss.SSS";
	public static final String FORMAT_DATETIME_GENERIC = "yyyy-MM-dd HH:mm:ss";

	private static final String FORMAT_DATE_ISO = "yyyy-MM-dd";
	private static final String FORMAT_SHORT_DATE = "dd-MM-yy";
	private static final String FORMAT_TIME_HMS = "HH:mm:ss";
	public static final DateTimeFormatter FULL_ISO_FORMATTER = buildFormatter(FORMAT_FULL_ISO);

	public static final DateTimeFormatter FULL_ISO_TIMESTAMP_NO_TZ_FORMATTER = buildFormatter(FORMAT_FULL_ISO_TIMESTAMP_NO_TZ);
	public static final DateTimeFormatter FULL_GENERIC_FORMATTER = buildFormatter(FORMAT_FULL_GENERIC);
	public static final DateTimeFormatter GENERIC_DATETIME_FORMATTER = buildFormatter(FORMAT_DATETIME_GENERIC);
	public static final DateTimeFormatter ISO_DATE_FORMATTER = buildFormatter(FORMAT_DATE_ISO);
	public static final DateTimeFormatter SHORT_DATE_FORMATTER = buildFormatter(FORMAT_SHORT_DATE);
	public static final DateTimeFormatter TIME_HMS_FORMATTER = buildFormatter(FORMAT_TIME_HMS);

	public static DateTimeFormatter buildFormatter(String format) {
		return DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault()).withResolverStyle(ResolverStyle.LENIENT);
	}

	public static String now() {
		return format(Instant.now());
	}

	@Deprecated
	public static String now(String format) {
		return format(Instant.now(), buildFormatter(format));
	}

	public static String now(DateTimeFormatter formatter) {
		return format(Instant.now(), formatter);
	}

	@Deprecated
	public static String format(Date date) {
		return date == null ? null : format(date.toInstant());
	}

	public static String format(long date) {
		return format(date, FULL_GENERIC_FORMATTER);
	}

	public static String format(Instant instant) {
		return format(instant, FULL_GENERIC_FORMATTER);
	}

	public static String format(long date, DateTimeFormatter formatter) {
		return format(Instant.ofEpochMilli(date), formatter);
	}

	@Deprecated
	public static String format(Date date, DateTimeFormatter formatter) {
		return format(date.toInstant(), formatter);
	}

	public static String format(Instant instant, DateTimeFormatter formatter) {
		return formatter.format(instant);
	}

	/**
	 * Get current date-time timestamp in generic format.
	 */
	public static String getTimeStamp() {
		return GENERIC_DATETIME_FORMATTER.format(Instant.now());
	}

	public static Instant parseToInstant(String s, DateTimeFormatter parser) throws DateTimeParseException {
		return parser.parse(s, Instant::from);
	}

	public static java.time.LocalDate parseToLocalDate(String s, DateTimeFormatter parser) throws DateTimeParseException {
		return parser.parse(s, TemporalQueries.localDate());
	}

	/**
	 * Parses a string to a Date using CalendarParser
	 */
	@Deprecated
	public static Date parseAnyDate(String dateInAnyFormat) throws CalendarParserException {
		// TODO: Fix this crap
		// Either: https://stackoverflow.com/a/3390252/3588231
		// Or: https://github.com/sisyphsu/dateparser
		Calendar c = CalendarParser.parse(dateInAnyFormat.replace('T', ' ')); // "Parse any format" doesn't parse ISO dates with a 'T' between date and time.
		return new Date(c.getTimeInMillis());
	}
}

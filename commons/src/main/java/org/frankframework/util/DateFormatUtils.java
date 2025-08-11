/*
   Copyright 2013 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
package org.frankframework.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Date;
import java.util.Map;
import java.util.regex.Pattern;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Utilities for formatting and parsing dates.
 */
public class DateFormatUtils {
	public static final String FORMAT_FULL_ISO = "yyyy-MM-dd'T'HH:mm:sszzz";
	public static final String FORMAT_FULL_ISO_TIMESTAMP_NO_TZ = "yyyy-MM-dd'T'HH:mm:ss.SSS";
	public static final String FORMAT_FULL_GENERIC = "yyyy-MM-dd HH:mm:ss.SSS";
	public static final String FORMAT_DATETIME_GENERIC = "yyyy-MM-dd HH:mm:ss";
	public static final DateTimeFormatter FULL_ISO_FORMATTER = buildFormatter(FORMAT_FULL_ISO);
	public static final DateTimeFormatter FULL_ISO_TIMESTAMP_NO_TZ_FORMATTER = buildFormatter(FORMAT_FULL_ISO_TIMESTAMP_NO_TZ);
	public static final DateTimeFormatter FULL_GENERIC_FORMATTER = buildFormatter(FORMAT_FULL_GENERIC);
	public static final DateTimeFormatter GENERIC_DATETIME_FORMATTER = buildFormatter(FORMAT_DATETIME_GENERIC);
	private static final String FORMAT_DATE_ISO = "yyyy-MM-dd";
	public static final DateTimeFormatter ISO_DATE_FORMATTER = buildFormatter(FORMAT_DATE_ISO);
	private static final String FORMAT_SHORT_DATE = "dd-MM-yy";
	public static final DateTimeFormatter SHORT_DATE_FORMATTER = buildFormatter(FORMAT_SHORT_DATE);
	private static final String FORMAT_TIME_HMS = "HH:mm:ss";
	public static final DateTimeFormatter TIME_HMS_FORMATTER = buildFormatter(FORMAT_TIME_HMS);

	public static final DateTimeFormatter HTTP_DATE_HEADER_FORMAT = DateTimeFormatter.RFC_1123_DATE_TIME;

	private static final Map<Pattern, DateTimeFormatter> DATE_EXPRESSION_PARSER_MAP;

	private DateFormatUtils() {
		// Private constructor so that the utility-class cannot be instantiated.
	}

	static {
		DATE_EXPRESSION_PARSER_MAP = Map.ofEntries(
				Map.entry(Pattern.compile("^\\d{8}$"), buildFormatter("yyyyMMdd")),
				Map.entry(Pattern.compile("^\\d{1,2}-\\d{1,2}-\\d{4}$"), buildFormatter("dd-MM-yyyy")),
				Map.entry(Pattern.compile("^\\d{1,2}-\\d{1,2}-\\d{2}$"), SHORT_DATE_FORMATTER),
				Map.entry(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}$"), ISO_DATE_FORMATTER),
				Map.entry(Pattern.compile("^\\d{1,2}/\\d{1,2}/\\d{4}$"), buildFormatter("MM/dd/yyyy")),
				Map.entry(Pattern.compile("^\\d{4}/\\d{1,2}/\\d{1,2}$"), buildFormatter("yyyy/MM/dd")),
				Map.entry(Pattern.compile("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}$"), buildFormatter("dd MMM yyyy")),
				Map.entry(Pattern.compile("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}$"), buildFormatter("dd MMMM yyyy")),
				Map.entry(Pattern.compile("^\\d{12}$"), buildFormatter("yyyyMMddHHmm")),
				Map.entry(Pattern.compile("^\\d{8}\\s\\d{4}$"), buildFormatter("yyyyMMdd HHmm")),
				Map.entry(Pattern.compile("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}$"), buildFormatter("dd-MM-yyyy HH:mm")),
				Map.entry(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}$"), buildFormatter("yyyy-MM-dd HH:mm")),
				Map.entry(Pattern.compile("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}$"), buildFormatter("MM/dd/yyyy HH:mm")),
				Map.entry(Pattern.compile("^\\d{1,2}/\\d{1,2}/\\d{2}\\s\\d{1,2}:\\d{2}:\\d{2}$"), new DateTimeFormatterBuilder()
						.appendPattern("dd/MM/")
						.appendValueReduced(ChronoField.YEAR, 2, 2, Year.now().getValue() - 60)
						.appendPattern(" HH:mm:ss")
						.toFormatter()
						.withZone(ZoneId.systemDefault())
						.withResolverStyle(ResolverStyle.LENIENT)),
				Map.entry(Pattern.compile("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}$"), buildFormatter("yyyy/MM/dd HH:mm")),
				Map.entry(Pattern.compile("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}$"), buildFormatter("dd MMM yyyy HH:mm")),
				Map.entry(Pattern.compile("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}$"), buildFormatter("dd MMMM yyyy HH:mm")),
				Map.entry(Pattern.compile("^\\d{14}$"), buildFormatter("yyyyMMddHHmmss")),
				Map.entry(Pattern.compile("^\\d{8}\\s\\d{6}$"), buildFormatter("yyyyMMdd HHmmss")),
				Map.entry(Pattern.compile("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$"), buildFormatter("dd-MM-yyyy HH:mm:ss")),
				Map.entry(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$"), GENERIC_DATETIME_FORMATTER),
				Map.entry(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}\\.\\d{3}$"), FULL_GENERIC_FORMATTER),
				Map.entry(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{2}:\\d{2}\\.\\d{3}$"), FULL_ISO_TIMESTAMP_NO_TZ_FORMATTER),
				Map.entry(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{2}:\\d{2}\\w+$"), FULL_ISO_FORMATTER),
				Map.entry(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{2}:\\d{2}$"), buildFormatter("yyyy-MM-dd'T'HH:mm:ss")),
				Map.entry(Pattern.compile("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$"), buildFormatter("MM/dd/yyyy HH:mm:ss")),
				Map.entry(Pattern.compile("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$"), buildFormatter("yyyy/MM/dd HH:mm:ss")),
				Map.entry(Pattern.compile("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$"), buildFormatter("dd MMM yyyy HH:mm:ss")),
				Map.entry(Pattern.compile("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$"), buildFormatter("dd MMMM yyyy HH:mm:ss"))
		);
	}

	public static DateTimeFormatter buildFormatter(String format) {
		return DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault()).withResolverStyle(ResolverStyle.LENIENT);
	}

	public static String now() {
		return format(TimeProvider.now());
	}

	@Deprecated
	public static String now(String format) {
		return format(TimeProvider.now(), buildFormatter(format));
	}

	public static String now(@Nonnull DateTimeFormatter formatter) {
		return format(TimeProvider.now(), formatter);
	}

	@Deprecated
	public static String format(Date date) {
		return date == null ? null : format(date.toInstant());
	}

	public static String format(long date) {
		return format(date, FULL_GENERIC_FORMATTER);
	}

	public static String format(@Nonnull Instant instant) {
		return format(instant, FULL_GENERIC_FORMATTER);
	}

	public static String format(long date, @Nonnull DateTimeFormatter formatter) {
		return format(Instant.ofEpochMilli(date), formatter);
	}

	@Deprecated
	public static String format(Date date, @Nonnull DateTimeFormatter formatter) {
		return format(date.toInstant(), formatter);
	}

	public static String format(@Nonnull Instant instant, @Nonnull DateTimeFormatter formatter) {
		return formatter.format(instant);
	}

	/**
	 * Get current date-time timestamp in generic format.
	 */
	public static String getTimeStamp() {
		return GENERIC_DATETIME_FORMATTER.format(TimeProvider.now());
	}

	public static Instant parseToInstant(String dateString, @Nonnull DateTimeFormatter parser) throws DateTimeParseException {
		TemporalAccessor temporalAccessor = parser.parse(dateString);
		if (temporalAccessor.isSupported(ChronoField.INSTANT_SECONDS)) {
			return Instant.from(temporalAccessor);
		} else {
			LocalDate localDate = temporalAccessor.query(TemporalQueries.localDate());
			return Instant.ofEpochSecond(localDate.toEpochSecond(LocalTime.MIN, ZoneOffset.UTC));
		}
	}

	public static LocalDate parseToLocalDate(String dateString) throws DateTimeParseException {
		DateTimeFormatter parser = determineDateFormat(dateString);
		if (parser == null) {
			throw new IllegalArgumentException("Cannot determine date-format for input [" + dateString + "]");
		}
		return parser.parse(dateString, TemporalQueries.localDate());
	}

	public static LocalDate parseToLocalDate(String dateString, @Nonnull DateTimeFormatter parser) throws DateTimeParseException {
		return parser.parse(dateString, TemporalQueries.localDate());
	}

	/**
	 * Parses a string to a Date using CalendarParser
	 */
	@Deprecated
	@Nonnull
	public static Date parseAnyDate(@Nonnull String dateInAnyFormat) throws DateTimeParseException, IllegalArgumentException {
		return Date.from(parseGenericDate(dateInAnyFormat));
	}

	@Nonnull
	public static Instant parseGenericDate(@Nonnull String dateString) throws DateTimeParseException, IllegalArgumentException {
		// Date parsing based on: https://stackoverflow.com/a/3390252/3588231
		// An alternative would have been this library: https://github.com/sisyphsu/dateparser
		// But I prefer the clarity of having more direct control and using standard Java APIs.
		DateTimeFormatter parser = determineDateFormat(dateString);
		if (parser == null) {
			throw new IllegalArgumentException("Cannot determine date-format for input [" + dateString + "]");
		}
		return parseToInstant(dateString, parser);
	}

	/**
	 * Java time API is more strict compared to the old Date API. This method allows for parsing dates with optional components.
	 *
	 * @param format
	 */
	public static DateTimeFormatter getDateTimeFormatterWithOptionalComponents(String format) {
		return new DateTimeFormatterBuilder()
				.appendPattern(format)
				.parseDefaulting(ChronoField.YEAR_OF_ERA, 1970)
				.parseDefaulting(ChronoField.MONTH_OF_YEAR, LocalDate.MIN.getMonthValue())
				.parseDefaulting(ChronoField.DAY_OF_MONTH, LocalDate.MIN.getDayOfMonth())
				.parseDefaulting(ChronoField.HOUR_OF_DAY, LocalTime.MIN.getHour())
				.parseDefaulting(ChronoField.MINUTE_OF_HOUR, LocalTime.MIN.getMinute())
				.parseDefaulting(ChronoField.SECOND_OF_MINUTE, LocalTime.MIN.getSecond())
				.parseDefaulting(ChronoField.NANO_OF_SECOND, LocalTime.MIN.getNano())
				.toFormatter()
				.withZone(ZoneId.systemDefault());
	}

	@Nullable
	private static DateTimeFormatter determineDateFormat(String dateString) {
		for (Map.Entry<Pattern, DateTimeFormatter> entry : DATE_EXPRESSION_PARSER_MAP.entrySet()) {
			if (entry.getKey().matcher(dateString).matches()) {
				return entry.getValue();
			}
		}
		return null;
	}
}

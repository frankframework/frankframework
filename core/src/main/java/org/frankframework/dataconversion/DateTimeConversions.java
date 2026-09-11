/*
   Copyright 2026 WeAreFrank!

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
package org.frankframework.dataconversion;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

import org.frankframework.util.AppConstants;
import org.frankframework.util.DateFormatUtils;

final class DateTimeConversions {

	private static final String DATE_TIME_FORMAT_KEY = "jdbc.timestampFormat";
	private static final String DATE_TIME_FORMAT_TZ_KEY = "jdbc.timestampWithTimezoneFormat";
	private static final String DATE_FORMAT_KEY = "jdbc.dateFormat";
	private static final String TIME_FORMAT_KEY = "jdbc.timeFormat";

	private static final String TIME_FORMAT = AppConstants.getInstance().getString(TIME_FORMAT_KEY, DateFormatUtils.FORMAT_TIME_HMS);
	private static final String DATE_FORMAT = AppConstants.getInstance().getString(DATE_FORMAT_KEY, DateFormatUtils.FORMAT_DATE_ISO);
	private static final String DATE_TIME_FORMAT = AppConstants.getInstance().getString(DATE_TIME_FORMAT_KEY, DateFormatUtils.FORMAT_FULL_ISO_TIMESTAMP_NO_TZ);
	private static final String DATE_TIME_TZ_FORMAT = AppConstants.getInstance().getString(DATE_TIME_FORMAT_TZ_KEY, DateFormatUtils.FORMAT_FULL_ISO);

	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_FORMAT);
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT).withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter DATE_TIME_TZ_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_TZ_FORMAT);

	private DateTimeConversions() {
		// Private constructor to prevent instance creation
	}

	static final class TemporalAccessorConverter extends StringableDataConverter<TemporalAccessor> {
		@Override
		public String asString(TemporalAccessor data) {
			return switch (data) {
				case Instant instant -> DATE_TIME_FORMATTER.format(instant);
				case LocalDateTime localDateTime -> DATE_TIME_FORMATTER.format(localDateTime);
				case LocalDate date -> DATE_FORMATTER.format(date);
				case LocalTime time -> TIME_FORMATTER.format(time);
				case ZonedDateTime zonedDateTime -> DATE_TIME_TZ_FORMATTER.format(zonedDateTime);
				default -> data.toString();
			};
		}
	}

	static final class DateConverter extends StringableDataConverter<Date> {
		@Override
		public String asString(Date data) {
			return switch (data) {
				case java.sql.Time time -> TIME_FORMATTER.format(time.toLocalTime());
				case java.sql.Timestamp timestamp -> DATE_TIME_FORMATTER.format(timestamp.toLocalDateTime());
				case java.sql.Date date -> DATE_FORMATTER.format(date.toLocalDate());
				default -> DATE_TIME_FORMATTER.format(data.toInstant());
			};
		}
	}
}

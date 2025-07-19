/*
   Copyright 2025 WeAreFrank!

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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

import jakarta.annotation.Nonnull;

/**
 * This class provides a way to get current time, but also control time in unit tests.
 */
public class TimeProvider {

	/**
	 * Clock currently being used.
	 */
	private static Clock clock = Clock.systemUTC();

	private TimeProvider() {
		// Private constructor to prevent creating instances of static utility classes
	}

	/**
	 * Get the current system time as {@link Instant}.
	 */
	public static Instant now() {
		return clock.instant();
	}

	/**
	 * Get the current system time as clock millis (equivalent to {@link System#currentTimeMillis()}).
	 */
	public static long nowAsMillis() {
		return clock.millis();
	}

	/**
	 * Get the current system time as {@link ZonedDateTime}
	 */
	public static ZonedDateTime nowAsZonedDateTime() {
		return ZonedDateTime.now(clock);
	}

	/**
	 * Get the current system time as {@link LocalDateTime}
	 */
	public static LocalDateTime nowAsLocalDateTime() {
		return LocalDateTime.now(clock);
	}

	/**
	 * Get the current system time as a legacy {@link Date}.
	 */
	public static Date nowAsDate() {
		return Date.from(now());
	}

	/**
	 * ONLY FOR UNIT TESTING! Set a clock to use. Typically this is for testing, when you want to use a more specialized {@link Clock} to set a specific time or behaviour than can
	 * be done by setting a clock with a fixed point in time.
	 *
	 * @param clock {@link Clock} to use.
	 */
	public static void setClock(@Nonnull Clock clock) {
		TimeProvider.clock = clock;
	}

	/**
	 * Get the {@link Clock} currently being used. This clock can be used to get the current time, or as a base-clock to create other specialized clocks for testing, which is the
	 * reason to make it accessible.
	 */
	public static Clock getClock() {
		return TimeProvider.clock;
	}

	/**
	 * ONLY FOR UNIT TESTING! Set the clock to a fixed date-time from the local date-time passed in.
	 * @param localDateTime Time to which to set the clock.
	 */
	public static void setTime(LocalDateTime localDateTime) {
		TimeProvider.clock = Clock.fixed(localDateTime.toInstant(ZoneOffset.from(localDateTime)), ZoneId.systemDefault());
	}

	/**
	 * ONLY FOR UNIT TESTING! Set the clock to a fixed date-time from the date-time & timezone passed in.
	 * @param zonedDateTime The time to which to set the clock.
	 */
	public static void setTime(ZonedDateTime zonedDateTime) {
		TimeProvider.clock = Clock.fixed(zonedDateTime.toInstant(), ZoneId.systemDefault());
	}

	/**
	 * ONLY FOR UNIT TESTING! Resets the clock to the default System UTC clock.
	 * This should be used in the test-teardown method of unit tests when the unit tests have set the clock to a none-default clock.
	 */
	public static void resetClock() {
		TimeProvider.clock = Clock.systemUTC();
	}
}

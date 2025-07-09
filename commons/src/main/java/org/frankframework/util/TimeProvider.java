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
import java.util.Date;

/**
 * This class provides a way to get current time, but also control time in unit tests.
 */
public class TimeProvider {

	public static Clock clock = Clock.systemUTC();

	private TimeProvider() {
		// Private constructor to prevent creating instances of static utility classes
	}

	public static Instant now() {
		return clock.instant();
	}

	public static Date nowAsDate() {
		return Date.from(now());
	}
}

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
package org.frankframework.pipes;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.networknt.schema.ExecutionContext;
import com.networknt.schema.format.Format;

import lombok.extern.log4j.Log4j2;

/**
 * Frequently, JSON 'date-time' formats defined in openapi specifications are not strictly compliant with RFC 3339, as they use offsets
 * like +0100 instead of +01:00. This class is a custom implementation of the 'date-time' format that first tries to parse the value
 * using the strict RFC 3339 format, and if that fails, it falls back to a more lenient format that accepts offsets without a colon.
 *
 * @see <a href="https://github.com/networknt/json-schema-validator/blob/master/doc/custom-dialect.md">Custom Dialect Documentation</a>
 * @author evandongen
 */
@Log4j2
public class OpenApiLenientDateTimeFormat implements Format {

	@Override
	public String getName() {
		return "date-time";
	}

	@Override
	public boolean matches(ExecutionContext executionContext, String value) {
		if (value == null) {
			return true;
		}

		// 1. Try strict RFC 3339 (default expected behaviour)
		try {
			OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
			return true;
		} catch (DateTimeParseException ignored) {
			// don't do anything, try to match below without ':'
		}

		// 2. Fallback: accept +0100 (no colon in offset)
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
			OffsetDateTime.parse(value, formatter);

			return true;
		} catch (DateTimeParseException ex) {
			log.debug("Invalid {}: {}", "date-time", ex.getMessage());
			return false;
		}
	}
}

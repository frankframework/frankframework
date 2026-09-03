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
package org.frankframework.http;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;

import lombok.experimental.UtilityClass;

@UtilityClass
public class HttpStatusResolver {

	/**
	 * Tries to resolve the given status code to a valid HTTP status code. Throws an IllegalArgumentException if it's not valid.
	 */
	public static @NonNull HttpStatus resolveHttpStatus(int statusCode) {
		HttpStatus httpStatus = HttpStatus.resolve(statusCode);

		if (httpStatus == null) {
			throw new IllegalArgumentException("Invalid HTTP status code: " + statusCode);
		}

		return httpStatus;
	}

	/**
	 * Tries to resolve the given status code to a valid HTTP status code. Returns the int value if it's valid.
	 */
	public static int resolveHttpStatusCode(int statusCode) {
		return resolveHttpStatus(statusCode).value();
	}
}

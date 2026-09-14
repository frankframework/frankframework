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

import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.Assert;

import lombok.experimental.UtilityClass;

@UtilityClass
public class HttpStatusResolver {

	/**
	 * Tries to resolve the given status code to a HTTP status code.
	 */
	public static @NonNull HttpStatusCode getHttpStatusCode(int statusCode) {
		return HttpStatusCode.valueOf(
				validateHttpStatusCode(statusCode)
		);
	}

	/**
	 * Custom status codes don't have a reason phrase, so this method returns an empty string for those. For valid HTTP status codes, it returns the reason phrase.
	 */
	public static Optional<String> getReasonPhrase(int statusCode) {
		if (getHttpStatusCode(statusCode) instanceof HttpStatus httpStatus) {
			return Optional.of(httpStatus.getReasonPhrase());
		}

		return Optional.empty();
	}

	/**
	 * Validates that the given status code is a three-digit positive integer between 100 and 599. If the status code is invalid thows IllegalArgumentException.
	 */
	public static int validateHttpStatusCode(int statusCode) {
		// Asset here since HttpStatusCode.valueOf() validates status codes >= 600 as valid
		Assert.isTrue(statusCode >= 100 && statusCode <= 599,
				() -> "Status code '" + statusCode + "' should be a three-digit positive integer between 100 and 599");

		return statusCode;
	}
}

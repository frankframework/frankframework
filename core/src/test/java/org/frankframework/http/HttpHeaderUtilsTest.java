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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class HttpHeaderUtilsTest {

	// Test the examples as given in RFC 6266 section 4.3, which are valid Content-Disposition headers that can be encoded in ISO-8859-1
	@ParameterizedTest
	@ValueSource(strings = {
			"attachment; filename=example.html",
			"INLINE; FILENAME= \"an example.html\"",
			"attachment; filename*= UTF-8''%e2%82%ac%20rates",
			"attachment; filename=\"EURO rates\"; filename*=utf-8''%e2%82%ac%20rates"
	})
	void willNotThrowForValidValues(String contentDispositionHeader) {
		assertDoesNotThrow(() -> HttpHeaderUtils.checkContentDispositionValueForValidFilename(contentDispositionHeader));
	}

	// Test some invalid Content-Disposition headers that contain characters that cannot be encoded in ISO-8859-1
	@ParameterizedTest
	@ValueSource(strings = {
			"INLINE; FILENAME= \"€ rates\"",
			"INLINE; FILENAME=€_rates.doc",
			"attachment; filename= UTF-8''%e2%82%ac%20rates",
			"attachment; filename=utf-8''%e2%82%ac%20rates"
	})
	void willThrowForInvalidValues(String contentDispositionHeader) {
		assertThrows(IllegalArgumentException.class, () -> HttpHeaderUtils.checkContentDispositionValueForValidFilename(contentDispositionHeader));
	}
}

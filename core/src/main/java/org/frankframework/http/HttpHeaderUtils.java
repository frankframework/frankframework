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

import java.net.URLDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class HttpHeaderUtils {
	private static final CharsetEncoder ISO_8859_1_ENCODER = StandardCharsets.ISO_8859_1.newEncoder();

	private HttpHeaderUtils() {
		// NO OP constructor to prevent this class from being initialized.
	}

	/**
	 * Matches the plain {@code filename=} parameter in a Content-Disposition header (case-insensitive), but NOT the extended {@code filename*=}
	 * variant (RFC 5987), whose value is always percent-encoded and therefore safe.
	 * <br />
	 * Captures the filename value in group 1 (quoted) or group 2 (unquoted).
	 */
	private static final Pattern FILENAME_PARAM_PATTERN = Pattern.compile(
			"(?:^|;)\\s*filename\\s*=\\s*(?:\"([^\"]*)\"|([^;\\s]*))",
			Pattern.CASE_INSENSITIVE
	);

	/**
	 * Guards that the 'filename=' parameter of a Content-Disposition header value can be encoded in ISO-8859-1, as defined in
	 * <a href="https://www.rfc-editor.org/info/rfc6266/#section-4.3">RFC 6266</a>.
	 * The extended 'filename*=' parameter (RFC 5987) is not checked because its value uses percent-encoding and is always safe.
	 */
	public static void checkContentDispositionValueForValidFilename(@NonNull String contentDispositionHeader) {
		Matcher matcher = FILENAME_PARAM_PATTERN.matcher(contentDispositionHeader);
		while (matcher.find()) {
			String filename = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);

			if (filename == null) {
				continue;
			}

			if (!ISO_8859_1_ENCODER.canEncode(getStringToCheck(filename))) {
				throw new IllegalArgumentException("Content-Disposition 'filename' parameter contains characters that cannot be encoded in ISO-8859-1: " + filename);
			}
		}
	}

	/**
	 * Decode the filename value so that sequences like %e2%82%ac (€ in UTF-8) are evaluated as their intended characters (U+20ac, which is Unicode for the
	 * euro sign)
	 */
	@Nullable
	private static String getStringToCheck(String filename) {
		try {
			return URLDecoder.decode(filename, StandardCharsets.UTF_8);
		} catch (IllegalArgumentException e) {
			// Malformed percent-encoding: fall back to checking the raw value. For example: file%.txt
			return filename;
		}
	}
}

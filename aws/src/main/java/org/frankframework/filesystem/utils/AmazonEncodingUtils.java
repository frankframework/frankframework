/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.filesystem.utils;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.mail.internet.MimeUtility;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class AmazonEncodingUtils {

	/**
	 * A regex that matches any character outside the ASCII character set, and any of the ASCII control characters.
	 * If a value contains any of these characters, the value should be encoded.
	 */
	private static final Pattern ENCODABLE_CHARS_RE = Pattern.compile("[^\\x20-\\x7F]");
	private static final Pattern RFC2047_DECODING_RE = Pattern.compile("=\\?([^?]+)\\?([BQ])\\?([^?]+)\\?=");

	private AmazonEncodingUtils() {
		// No-op, prevents instance-creation.
	}

	/**
	 * Encode a value for passing it as user metadata for an S3 object.
	 * <br/>
	 * See: <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/UsingMetadata.html">Using Metadata in the AWS S3 Userguide</a>.
	 *
	 * @param value Value to be encoded.
	 * @return Encoded value.
	 */
	public static String rfc2047Encode(String value) {
		if (!ENCODABLE_CHARS_RE.matcher(value).find()) return value;

		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		byte[] encoded = Base64.getMimeEncoder().encode(bytes);

		String result = "=?UTF-8?B?" + new String(encoded, StandardCharsets.US_ASCII) + "?=";
		log.debug("Encoded value [{}] to [{}]", value, result);
		return result;
	}

	/**
	 * Decode a user metadata value returned from the S3 API.
	 * <br/>
	 * See: <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/UsingMetadata.html">Using Metadata in the AWS S3 Userguide</a>.
	 *
	 * @param value Encoded value
	 * @return Readable value
	 */
	public static String rfc2047Decode(String value) {
		Matcher matcher = RFC2047_DECODING_RE.matcher(value);
		if (!matcher.matches()) return value;

		String charSet = matcher.group(1);
		String encodingType = matcher.group(2);
		String data = matcher.group(3);


		String result = switch (encodingType) {
			case "B" -> new String(Base64.getMimeDecoder().decode(data), Charset.forName(charSet));
			case "Q" -> parseQuotedPrintableString(data, charSet);
			default -> throw new IllegalArgumentException("Unsupported encoding type [" + encodingType + "]");
		};
		log.debug("Decoded value [{}] to [{}]", value, result);
		return result;
	}

	private static String parseQuotedPrintableString(String input, String outputCharset) {
		try {
			return new String(MimeUtility.decode(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), "quoted-printable").readAllBytes(), outputCharset);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}
}

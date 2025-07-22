/*
   Copyright 2023 WeAreFrank!

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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class XmlEncodingUtils {
	public static final char REPLACE_NON_XML_CHAR = 0x00BF; // Inverted question mark.

	private XmlEncodingUtils() {
		// Private constructor so that the utility-class cannot be instantiated.
	}

	/**
	 * See {@link #encodeChars(String, boolean)}.
	 */
	public static String encodeChars(String string) {
		return encodeChars(string, false);
	}

	/**
	 * Translates special characters to XML equivalents like <b>&gt;</b> and <b>&amp;</b>. Please note that non-valid XML chars are not changed,
	 * hence you might want to use {@link #replaceNonValidXmlCharacters(String)} or {@link #stripNonValidXmlCharacters(String, boolean)} too.
	 */
	public static String encodeChars(String string, boolean escapeNewLines) {
		if (string == null) {
			return null;
		}
		int length = string.length();
		char[] characters = new char[length];
		string.getChars(0, length, characters, 0);
		return encodeChars(characters, 0, length, escapeNewLines);
	}

	public static String encodeCharsAndReplaceNonValidXmlCharacters(String string) {
		return encodeChars(replaceNonValidXmlCharacters(string));
	}

	/**
	 * Translates special characters to xml equivalents like <b>&gt;</b> and <b>&amp;</b>. Please note that non-valid xml chars
	 * are not changed, hence you might want to use {@link #replaceNonValidXmlCharacters(String)} or {@link #stripNonValidXmlCharacters(String, boolean)} too.
	 */
	public static String encodeChars(char[] chars, int offset, int length, boolean escapeNewLines) {
		if (length <= 0) {
			return "";
		}
		StringBuilder encoded = new StringBuilder(length);
		String escape;
		for (int i = 0; i < length; i++) {
			char c = chars[offset + i];
			escape = escapeChar(c, escapeNewLines);
			if (escape == null)
				encoded.append(c);
			else
				encoded.append(escape);
		}
		return encoded.toString();
	}

	/**
	 * Translates the five reserved XML characters (&lt; &gt; &amp; &quot; &apos;) to their normal selves
	 */
	public static String decodeChars(String string) {
		StringBuilder decoded = new StringBuilder();

		boolean inEscape = false;
		int escapeStartPos = 0;

		for (int i = 0; i < string.length(); i++) {
			char cur = string.charAt(i);
			if (inEscape) {
				if (cur == ';') {
					inEscape = false;
					String escapedString = string.substring(escapeStartPos, i + 1);
					char unEscape = unEscapeString(escapedString);
					if (unEscape == 0x0) {
						decoded.append(escapedString);
					} else {
						decoded.append(unEscape);
					}
				}
			} else {
				if (cur == '&') {
					inEscape = true;
					escapeStartPos = i;
				} else {
					decoded.append(cur);
				}
			}
		}
		if (inEscape) {
			decoded.append(string.substring(escapeStartPos));
		}
		return decoded.toString();
	}

	/**
	 * Conversion of special xml signs. Please note that non-valid xml chars are not changed, hence you might want to use
	 * {@link #replaceNonValidXmlCharacters(String)} or {@link #stripNonValidXmlCharacters(String, boolean)} too.
	 **/
	private static String escapeChar(char c, boolean escapeNewLines) {
		return switch (c) {
			case ('<') -> "&lt;";
			case ('>') -> "&gt;";
			case ('&') -> "&amp;";
			case ('\"') -> "&quot;";
			case ('\'') -> "&#39;";    // return "&apos;"; // apos does not work in Internet Explorer
			case ('\n') -> escapeNewLines ? "&#10;" : null;
			default -> null;
		};
	}

	private static char unEscapeString(String str) {
		if ("&lt;".equalsIgnoreCase(str))
			return '<';
		else if ("&gt;".equalsIgnoreCase(str))
			return '>';
		else if ("&amp;".equalsIgnoreCase(str))
			return '&';
		else if ("&quot;".equalsIgnoreCase(str))
			return '\"';
		else if ("&apos;".equalsIgnoreCase(str) || "&#39;".equalsIgnoreCase(str))
			return '\'';
		else
			return 0x0;
	}

	/**
	 * Replaces non-unicode-characters by '0x00BF' (inverted question mark).
	 */
	public static int replaceNonPrintableCharacters(char[] buf, int offset, int len) {
		if (len < 0) {
			return len;
		}
		int c;
		int charCount = 0;
		int counter = 0;
		int readPos = 0;
		int writePos = 0;
		while (readPos < len) { // while no shift needs to be made, loop and replace where necessary
			c = Character.codePointAt(buf, readPos + offset);
			charCount = Character.charCount(c);
			if (isPrintableUnicodeChar(c, true)) {
				readPos += charCount;
			} else {
				buf[offset + readPos] = REPLACE_NON_XML_CHAR;
				if (charCount == 1) {
					readPos++;
				} else {
					writePos = readPos + 1;
					readPos += charCount;
					break;
				}
			}
		}
		while (readPos < len) { // continue with loop and shift after replacement was shorter than original
			c = Character.codePointAt(buf, readPos + offset);
			charCount = Character.charCount(c);
			if (isPrintableUnicodeChar(c, true)) {
				for (int j = 0; j < charCount; j++) {
					buf[offset + writePos++] = buf[offset + readPos++];
				}
			} else {
				buf[offset + writePos++] = REPLACE_NON_XML_CHAR;
				readPos += charCount;
				counter++;
			}
		}
		log.debug("replaced [{}] non valid xml characters to [{}] in char array of length [{}]", counter, REPLACE_NON_XML_CHAR, len);
		return writePos > 0 ? writePos : readPos;
	}

	/**
	 * Replaces non-unicode-characters by '0x00BF' (inverted question mark)
	 * appended with #, the character number and ;.
	 */
	public static String replaceNonValidXmlCharacters(String string) {
		return replaceNonValidXmlCharacters(string, REPLACE_NON_XML_CHAR, true, true);
	}

	public static String replaceNonValidXmlCharacters(String string, char to, boolean appendCharNum, boolean allowUnicodeSupplementaryCharacters) {
		if (string == null) {
			return null;
		}
		int length = string.length();
		StringBuilder encoded = new StringBuilder(length);
		int c;
		int counter = 0;
		for (int i = 0; i < length; i += Character.charCount(c)) {
			c = string.codePointAt(i);
			if (isPrintableUnicodeChar(c, allowUnicodeSupplementaryCharacters)) {
				encoded.appendCodePoint(c);
			} else {
				if (appendCharNum) {
					encoded.append(to).append("#").append(c).append(";");
				} else {
					encoded.append(to);
				}
				counter++;
			}
		}
		if (counter > 0) {
			log.debug("replaced [{}] non valid xml characters to [{}] in string of length [{}]", counter, to, length);
		}
		return encoded.toString();
	}

	public static String stripNonValidXmlCharacters(String string, boolean allowUnicodeSupplementaryCharacters) {
		int length = string.length();
		StringBuilder encoded = new StringBuilder(length);
		int c;
		int counter = 0;
		for (int i = 0; i < length; i += Character.charCount(c)) {
			c = string.codePointAt(i);

			if (isPrintableUnicodeChar(c, allowUnicodeSupplementaryCharacters)) {
				encoded.appendCodePoint(c);
			} else {
				counter++;
			}
		}
		log.debug("stripped [{}] non valid xml characters in string of length [{}]", counter, length);
		return encoded.toString();
	}

	public static boolean isPrintableUnicodeChar(int c) {
		return isPrintableUnicodeChar(c, false);
	}

	public static boolean isPrintableUnicodeChar(int c, boolean allowUnicodeSupplementaryCharacters) {
		return (c == 0x0009)
				|| (c == 0x000A)
				|| (c == 0x000D)
				|| (c >= 0x0020 && c <= 0xD7FF)
				|| (c >= 0xE000 && c <= 0xFFFD)
				|| (allowUnicodeSupplementaryCharacters && (c >= 0x00010000 && c <= 0x0010FFFF));
	}

	/**
	 * Reads binary XML data and uses the XML declaration encoding to turn it into character data.
	 */
	public static String readXml(InputStream inputStream, String defaultCharset) throws IOException {
		BOMInputStream bOMInputStream = new BOMInputStream(inputStream, ByteOrderMark.UTF_8, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE);
		ByteOrderMark bom = bOMInputStream.getBOM();
		String charsetName = bom == null ? defaultCharset : bom.getCharsetName();
		return readXml(StreamUtil.streamToBytes(bOMInputStream), charsetName);
	}

	public static String readXml(byte[] source, String defaultEncoding) throws UnsupportedEncodingException {
		String charset = StringUtils.isEmpty(defaultEncoding) ? StreamUtil.DEFAULT_INPUT_STREAM_ENCODING : defaultEncoding;
		int length = source.length;

		String firstPart = new String(source, 0, length < 100 ? length : 100, charset);
		if (StringUtils.isEmpty(firstPart)) {
			return null;
		}

		if (firstPart.startsWith("<?xml")) {
			int endPos = firstPart.indexOf("?>") + 2;
			if (endPos < 2) {
				throw new IllegalArgumentException("no valid xml declaration in string [" + firstPart + "]");
			}

			String declaration = firstPart.substring(6, endPos - 2);
			log.debug("parsed declaration [{}]", declaration);
			final String encodingTarget = "encoding=\"";
			int encodingStart = declaration.indexOf(encodingTarget);
			if (encodingStart > 0) {
				encodingStart += encodingTarget.length();
				log.debug("encoding-declaration [{}]", declaration.substring(encodingStart));
				int encodingEnd = declaration.indexOf("\"", encodingStart);
				if (encodingEnd > 0) {
					charset = declaration.substring(encodingStart, encodingEnd);
					log.debug("parsed charset [{}]", charset);
				} else {
					log.warn("no end in encoding attribute in declaration [{}]", declaration);
				}
			} else {
				log.warn("no encoding attribute in declaration [{}]", declaration);
			}
		}
		return new String(source, charset);
	}

}

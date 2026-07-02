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
package org.frankframework.util.stream;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;

import lombok.extern.log4j.Log4j2;

import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;

@Log4j2
public class ReplacingParameterVariablesInputStream extends FilterInputStream {
	// while matching, this is where the bytes go.
	final int[] buf;
	private int matchedIndex;
	private int unbufferIndex;

	private static final byte CLOSING_CURLY_BRACE = 125;

	private final byte[] variablePrefix = "?{".getBytes();
	private final ParameterValueList paramList;

	private State state = State.NOT_MATCHED;
	private InputStream matchedDelegate;

	// simple state machine for keeping track of what we are doing
	private enum State {
		NOT_MATCHED, INCOMPLETE_MATCH, MATCHING, REPLACING, UNBUFFER
	}

	public ReplacingParameterVariablesInputStream(InputStream in, ParameterValueList paramList) {
		super(in);
		String longestKey = paramList.stream().map(ParameterValue::getName).max(Comparator.comparingInt(String::length)).get();
		buf = new int[longestKey.length() + 3];
		this.paramList = paramList;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		// copy of parent logic; we need to call our own read() instead of super.read(), which delegates instead of calling our read
		if (b == null) {
			throw new NullPointerException();
		} else if (off < 0 || len < 0 || len > b.length - off) {
			throw new IndexOutOfBoundsException();
		} else if (len == 0) {
			return 0;
		}

		int c = read();
		if (c == -1) {
			return -1;
		}
		b[off] = (byte) c;

		int i = 1;
		for (; i < len; i++) {
			c = read();
			if (c == -1) {
				break;
			}
			b[off + i] = (byte) c;
		}
		return i;

	}

	@Override
	public int read(byte[] b) throws IOException {
		// call our own read
		return read(b, 0, b.length);
	}

	@Override
	public int read() throws IOException {
		// use a simple state machine to figure out what we are doing
		int next;
		switch (state) {
			default:
			case NOT_MATCHED:
				// we are not currently matching, replacing, or unbuffering
				next = super.read();
				if (variablePrefix[0] != next) {
					return next;
				}

				// clear whatever was there
				Arrays.fill(buf, 0);
				// make sure we start at 0
				matchedIndex = 0;

				buf[matchedIndex++] = next;
				state = State.INCOMPLETE_MATCH;
				// recurse to continue matching
				return read();

			case INCOMPLETE_MATCH:
				next = super.read();
				if (variablePrefix[matchedIndex] == next) {
					buf[matchedIndex++] = next;
					state = State.MATCHING;
				} else {
					// mismatch -> unbuffer
					buf[matchedIndex++] = next;
					state = State.UNBUFFER;
					unbufferIndex = 0;
				}
				return read();

			case MATCHING:
				// The previous bytes matched part of the pattern
				next = super.read();
				// fill the buffer till it's either full or we find a '}'.
				buf[matchedIndex++] = next;

				// Buffer is full, we didn't find a match
				if (matchedIndex == buf.length && next != CLOSING_CURLY_BRACE) {
					// mismatch -> unbuffer
					state = State.UNBUFFER;
					unbufferIndex = 0;
				}
				if (next == CLOSING_CURLY_BRACE) {
					// We've fully matched the pattern '?{...}' and are returning bytes from the replacement
					String key = readMatchingBuffer();
					ParameterValue value = paramList.findParameterValue(key);
					if (value != null) {
						Message paramValue = value.asMessage();
						log.info("replaceing [{}] with value from [{}]", key, paramValue);
						matchedDelegate = paramValue.asInputStream();
					} else {
						log.debug("key [{}] does not exist in parameter list, no replacement", key);
					}

					// Start replacing
					state = State.REPLACING;
				}

				return read();

			case REPLACING:
				if (matchedDelegate == null) {
					// Should not be possible but just in case...
					state = State.NOT_MATCHED;
					return read();
				}

				next = matchedDelegate.read();
				if (next == -1) {
					matchedDelegate = null;
					return read();
				}
				return next;

			case UNBUFFER:
				// we partially matched the pattern before encountering a non matching byte
				// we need to serve up the buffered bytes before we go back to NOT_MATCHED
				next = buf[unbufferIndex++];
				if (unbufferIndex == matchedIndex) {
					state = State.NOT_MATCHED;
					matchedIndex = 0;
				}
				return next;
		}
	}

	private String readMatchingBuffer() {
		byte[] result = new byte[matchedIndex-3];
		for (int i = 2; i < matchedIndex -1; i++) {
			result[i-2] = Integer.valueOf(buf[i]).byteValue();
		}
		return new String(result, StandardCharsets.UTF_8);
	}
}

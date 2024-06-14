/*
   Copyright 2017 Simon Haoran Liang, 2019-2024 WeAreFrank!

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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.IntStream;

import jakarta.annotation.Nonnull;

/**
 * Copyright 2019-2024 WeAreFrank!
 * Based on original concept created by simon on 8/29/17.
 * Copyright 2017 Simon Haoran Liang
 * <a href="https://gist.github.com/lhr0909/e6ac2d6dd6752871eb57c4b083799947">...</a>
 */
public class ReplacingVariablesInputStream extends FilterInputStream {

	private static final byte BYTE_VALUE_END_OF_STREAM = -1;
	private static final byte CLOSING_CURLY_BRACE = 125;
	private final byte[] variablePrefix;
	private final Map<String, String> keyValuePairs;
	private final Queue<Integer> inQueue;
	private final Queue<Integer> outQueue;
	private boolean lookingForSuffix = false;

	protected ReplacingVariablesInputStream(InputStream in, String variablePrefix, Map<String, String> keyValuePairs) {
		super(in);
		this.variablePrefix = (variablePrefix + "{").getBytes();
		this.keyValuePairs = keyValuePairs;

		this.inQueue = new LinkedList<>();
		this.outQueue = new LinkedList<>();
	}

	@Override
	public int read() throws IOException {
		while (outQueue.isEmpty()) {
			readAhead();

			if (isMatchFound()) {
				byte[] matchingKeyInBytes = getMatchingKey();
				byte[] replacementValue = getReplacementValue(matchingKeyInBytes);

				IntStream.range(0, inQueue.size())
						.forEach(a -> inQueue.remove());

				IntStream.range(0, replacementValue.length)
						.forEach(b -> outQueue.offer((int) replacementValue[b]));
			} else if (!lookingForSuffix) {
				outQueue.add(inQueue.remove());
			}
		}

		return outQueue.remove();
	}

	private byte[] getReplacementValue(byte[] matchingKeyInBytes) {
		String matchingKey = new String(matchingKeyInBytes);

		if (keyValuePairs.containsKey(matchingKey)) {
			return keyValuePairs.get(matchingKey).getBytes();
		}

		return "".getBytes();
	}

	/**
	 * @return the matching value between prefix and suffix. For instance: ${parameter} returns 'parameter' as a byte[]
	 */
	private byte[] getMatchingKey() {
		List<Integer> list = new ArrayList<>(inQueue);

		// Remove variablePrefix
		for (byte b : variablePrefix) {
			if (list.get(0) == b) {
				list.remove(0);
			}
		}

		// Remove suffix
		if (list.get(list.size() - 1) == CLOSING_CURLY_BRACE) {
			list.remove(list.size() - 1);
		}

		// Convert matchingValue to byte[]
		byte[] result = new byte[list.size()];
		for (int i = 0; i < list.size(); i++) {
			result[i] = list.get(i).byteValue();
		}

		return result;
	}

	private void readAhead() throws IOException {
		// Read in small steps until the variablePrefix is found or when a prefix is found,
		// keep reading until the suffix is found.
		int searchLength = (variablePrefix.length == 0) ? 1 : variablePrefix.length;

		while (lookingForSuffix || (inQueue.size() < searchLength)) {
			int next = super.read();

			inQueue.offer(next);

			if (BYTE_VALUE_END_OF_STREAM == next) {
				break;
			}

			// Break out of the wile if we're looking for the CLOSING_CURLY_BRACE
			if (lookingForSuffix && CLOSING_CURLY_BRACE == next) {
				break;
			}
		}
	}

	private boolean isMatchFound() {
		Iterator<Integer> iterator = inQueue.iterator();

		if (!lookingForSuffix) {
			// Try to find the prefix, eg: '${'
			for (byte b : variablePrefix) {
				if (!iterator.hasNext() || b != iterator.next()) {
					return false;
				}
			}
			lookingForSuffix = true;
		} else {
			// Try to find the suffix, '}'
			while (iterator.hasNext()) {
				Integer next = iterator.next();
				if (CLOSING_CURLY_BRACE == next) {
					lookingForSuffix = false;
					return true;
				}

				// Edge case: string contains a prefix but is missing a suffix
				if (BYTE_VALUE_END_OF_STREAM == next) {
					lookingForSuffix = false;
					return false;
				}
			}
		}

		return false;
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	/**
	 * copied straight from InputStream implementation, just needed to use {@link #read()} from this class
	 *
	 * @see InputStream#read(byte[], int, int)
	 */
	@Override
	public int read(@Nonnull byte[] b, int off, int len) throws IOException {
		if (off < 0 || len < 0 || len > b.length - off) {
			throw new IndexOutOfBoundsException();
		} else if (len == 0) {
			return 0;
		}

		int c = read();
		if (c == BYTE_VALUE_END_OF_STREAM) {
			return BYTE_VALUE_END_OF_STREAM;
		}
		b[off] = (byte) c;

		int i = 1;
		try {
			for (; i < len; i++) {
				c = read();
				if (c == BYTE_VALUE_END_OF_STREAM) {
					break;
				}
				b[off + i] = (byte) c;
			}
		} catch (IOException ee) {
		}
		return i;
	}
}

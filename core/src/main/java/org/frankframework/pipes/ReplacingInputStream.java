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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.IntStream;

import jakarta.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.frankframework.util.XmlEncodingUtils;

/**
 * Copyright 2019-2024 WeAreFrank!
 * Based on original concept created by simon on 8/29/17.
 * Copyright 2017 Simon Haoran Liang
 * <a href="https://gist.github.com/lhr0909/e6ac2d6dd6752871eb57c4b083799947">...</a>
 */
public class ReplacingInputStream extends FilterInputStream {

	private final boolean allowUnicodeSupplementaryCharacters;
	private final Queue<Integer> inQueue;
	private final char nonXmlReplacementCharacter;
	private final Queue<Integer> outQueue;
	private final boolean replaceNonXmlChars;
	private final byte[] replacement;
	private final byte[] search;

	public ReplacingInputStream(InputStream in, String search, String replacement, boolean replaceNonXmlChars,
								String nonXmlReplacementCharacter, boolean allowUnicodeSupplementaryCharacters) {

		super(in);
		this.replaceNonXmlChars = replaceNonXmlChars;
		this.nonXmlReplacementCharacter = StringUtils.isEmpty(nonXmlReplacementCharacter) ? 0 : nonXmlReplacementCharacter.charAt(0);
		this.allowUnicodeSupplementaryCharacters = allowUnicodeSupplementaryCharacters;

		this.inQueue = new LinkedList<>();
		this.outQueue = new LinkedList<>();

		this.search = (search == null) ? "".getBytes() : search.getBytes();
		this.replacement = (replacement == null) ? "".getBytes() : replacement.getBytes();
	}

	@Override
	public int read() throws IOException {
		while (outQueue.isEmpty()) {
			readAhead();

			int length = search.length;
			if (length > 0 && isMatchFound()) {
				IntStream.range(0, length)
						.forEach(a -> inQueue.remove());

				IntStream.range(0, replacement.length)
						.forEach(b -> outQueue.offer((int) replacement[b]));
			} else {
				outQueue.add(inQueue.remove());
			}
		}

		return outQueue.remove();
	}

	/**
	 * Make sure to return false here, because we don't support it.
	 * See org.frankframework.stream.Message#readBytesFromInputStream(int)
	 */
	@Override
	public boolean markSupported() {
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
		if (c == -1) {
			return -1;
		}
		b[off] = (byte) c;

		int i = 1;
		try {
			for (; i < len; i++) {
				c = read();
				if (c == -1) {
					break;
				}
				b[off + i] = (byte) c;
			}
		} catch (IOException ee) {
		}
		return i;
	}

	private int getNextValue() throws IOException {
		int next = super.read();

		if (next != -1 && replaceNonXmlChars && !XmlEncodingUtils.isPrintableUnicodeChar(next, allowUnicodeSupplementaryCharacters)) {
			// '0' is the default value for a char
			if (nonXmlReplacementCharacter != 0) {
				next = nonXmlReplacementCharacter;
			} else {
				// If the character needs to be replaced, but no replacementChar was defined, skip to next.
				next = getNextValue();
			}
		}
		return next;
	}

	private boolean isMatchFound() {
		Iterator<Integer> iterator = inQueue.iterator();

		for (byte b : search) {
			if (!iterator.hasNext() || b != iterator.next()) {
				return false;
			}
		}

		return true;
	}

	private void readAhead() throws IOException {
		// Work up some look-ahead with a sensible default if search is empty
		int searchLength = (search.length == 0) ? 1 : search.length;

		while (inQueue.size() < searchLength) {
			int next = getNextValue();

			inQueue.offer(next);

			if (next == -1) {
				break;
			}
		}
	}
}

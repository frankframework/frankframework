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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.util.XmlEncodingUtils;

/**
 * Copyright 2019-2024 WeAreFrank!
 * Based on original concept created by simon on 8/29/17.
 * Copyright 2017 Simon Haoran Liang
 * <a href="https://gist.github.com/lhr0909/e6ac2d6dd6752871eb57c4b083799947">...</a>
 * <p>
 * Find / Replace InputStream implementation
 *
 * @author Erik van Dongen
 */
public class ReplacingInputStream extends InputStream {

	private final boolean allowUnicodeSupplementaryCharacters;
	private final Queue<Integer> inQueue;
	private final char nonXmlReplacementCharacter;
	private final Queue<Integer> outQueue;
	private final boolean replaceNonXmlChars;
	private final byte[] replacement;
	private final byte[] search;
	private final InputStream in;

	@Override
	public void close() throws IOException {
		if (this.in != null) {
			this.in.close();
		}
	}

	public ReplacingInputStream(InputStream in, String search, String replacement, boolean replaceNonXmlChars,
								String nonXmlReplacementCharacter, boolean allowUnicodeSupplementaryCharacters) {

		this.in = in;
		this.replaceNonXmlChars = replaceNonXmlChars;
		this.nonXmlReplacementCharacter = StringUtils.isEmpty(nonXmlReplacementCharacter) ? 0 : nonXmlReplacementCharacter.charAt(0);
		this.allowUnicodeSupplementaryCharacters = allowUnicodeSupplementaryCharacters;

		this.inQueue = new ArrayDeque<>();
		this.outQueue = new ArrayDeque<>();

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

	private int getNextValue() throws IOException {
		int next = in.read();

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

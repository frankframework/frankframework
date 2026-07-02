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

import org.apache.commons.lang3.StringUtils;

import org.frankframework.util.XmlEncodingUtils;

/**
 * Simple InputStream that can replace non-XML character occurrences with something else.
 */
public class ReplaceNonXmlCharsInputStream extends FilterInputStream {
	private final boolean allowUnicodeSupplementaryCharacters;
	private final char nonXmlReplacementCharacter;

	public ReplaceNonXmlCharsInputStream(InputStream in, String nonXmlReplacementCharacter, boolean allowUnicodeSupplementaryCharacters) {
		super(in);
		this.nonXmlReplacementCharacter = StringUtils.isBlank(nonXmlReplacementCharacter) ? 0 : nonXmlReplacementCharacter.charAt(0);
		this.allowUnicodeSupplementaryCharacters = allowUnicodeSupplementaryCharacters;
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
		int next = super.read();
		if (next != -1 && !XmlEncodingUtils.isPrintableUnicodeChar(next, allowUnicodeSupplementaryCharacters)) {
			// '0' is the default value for a char
			if (nonXmlReplacementCharacter != 0) {
				return nonXmlReplacementCharacter;
			} else {
				// If the character needs to be replaced, but no replacementChar was defined, skip to next.
				return read();
			}
		}

		return next;
	}
}

/*
   Copyright 2013 Nationale-Nederlanden

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

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Reader that encapsulates a file within a prefix and a postfix.
 *
 * @author  Gerrit van Brakel
 * @since   4.3
 */
public class EncapsulatingReader extends FilterReader {

	String prefix;
	String postfix;
	boolean encodePrintable;

	boolean readPrefix=false;
	boolean readReader=false;
	int position=0;

	public EncapsulatingReader(Reader in, String prefix, String postfix, boolean encodePrintable) {
		super(in);
		this.prefix=prefix;
		this.postfix=postfix;
		this.encodePrintable = encodePrintable;
	}

	public EncapsulatingReader(Reader in, String prefix, String postfix) {
		this(in, prefix, postfix, false);
	}

	private char charPrintable(char c) {
		if (!encodePrintable || XmlEncodingUtils.isPrintableUnicodeChar(c)) {
			return c;
		} else {
			return XmlEncodingUtils.REPLACE_NON_XML_CHAR;
		}
	}

	@Override
	public int read() throws IOException {
		if (!readPrefix) {
			if (position<prefix.length()) {
				return prefix.charAt(position++);
			}
			readPrefix=true;
			position=0;
		}
		if (!readReader) {
			int result = in.read();
			if (result>=0) {
				return charPrintable((char)result);
			}
			readReader=true;
		}
		if (position<postfix.length()) {
			return postfix.charAt(position++);
		}
		return -1;
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		int charsRead=0;
		if (!readPrefix) {
			while (position<prefix.length() && charsRead<len) {
				cbuf[off+charsRead++]=prefix.charAt(position++);
			}
			readPrefix=true;
			position=0;
			if (charsRead>0) {
				return charsRead;
			}
		}
		if (!readReader) {
			charsRead = in.read(cbuf, off, len);
			if (charsRead>0) {
				if (encodePrintable) {
					for (int i=off; i<off+charsRead; i++) {
						cbuf[i]=charPrintable(cbuf[i]);
					}
				}
				return charsRead;
			}
			readReader=true;
			charsRead=0;
		}
		while (position<postfix.length() && charsRead<len) {
			cbuf[off+charsRead++]=postfix.charAt(position++);
		}
		if (charsRead>0) {
			return charsRead;
		}
		return -1;
	}

	@Override
	public boolean ready() throws IOException {
		return !readPrefix || (!readReader && in.ready()) || position<postfix.length();
	}

	@Override
	public void reset() throws IOException {
		in.reset();
		readPrefix=false;
		readReader=false;
		position=0;
	}

}

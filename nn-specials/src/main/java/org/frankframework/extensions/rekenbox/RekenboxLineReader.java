/*
   Copyright 2002, 2013 Nationale-Nederlanden, 2023 WeAreFrank!

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
package org.frankframework.extensions.rekenbox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * This subclass of BufferedReader modifies the meaning of readLine such that
 * lines break on '\n' OR ';' (OR ";\n"). This is nessecary to read files from
 * the calcbox, which can be sent without linebreaks but which always end with a
 * ';'.
 *
 * @author leeuwt
 *
 *         Change History Author Date Version Details Tim N. van der Leeuw
 *         30-07-2002 1.0 Initial release
 *
 *
 */
class RekenboxLineReader extends BufferedReader {
	private int m_pushbackBuffer = -1;
	private boolean m_pushbackValid = false;

	/**
	 * Constructor for RekenboxLineReader.
	 *
	 * @param in Underlying Reader
	 * @param sz Size of read-buffer.
	 *
	 */
	public RekenboxLineReader(Reader in, int sz) {
		super(in, sz);
	}

	/**
	 * Constructor for RekenboxLineReader.
	 *
	 * @param in Underlying Reader
	 *
	 */
	public RekenboxLineReader(Reader in) {
		super(in);
	}

	/**
	 * @see BufferedReader#readLine()
	 */
	public String readLine() throws IOException {
		StringBuilder str;
		boolean eos;

		str = new StringBuilder(1024);
		eos = false;

		if(havePushback()) {
			char c = (char) getPushback();
			if(c == -1) {
				return null;
			}
			str.append(c);
		}

		while(true) {
			int b = read();
			if(b == -1) {
				eos = true;
				break;
			} else if(b == '\n') {
				break;
			}
//          BUGFIX: ACHTER DE if GEZET
//          str.append((char)b);
			if(b == ';') {
				int b2 = read();
				if(b2 != '\n') {
					pushback(b2);
				}
				break;
			} else {
				str.append((char) b);
			}
		}
		if(eos && str.isEmpty()) {
			return null;
		}
		return str.toString();
	}

	/**
	 * Method havePushback. Checks if the pushback-buffer is in use.
	 *
	 * @return boolean <code>true</code> if there is a character in the
	 *         pushback-buffer, <code>false</code> if not.
	 */
	protected boolean havePushback() {
		return m_pushbackValid;
	}

	/**
	 * Method getPushback. Returns the value of the pushback-buffer.
	 *
	 * @return int The character which was in the pushback-buffer.
	 */
	protected int getPushback() {
		if(m_pushbackValid) {
			m_pushbackValid = false;
			int result = m_pushbackBuffer;
			m_pushbackBuffer = -1;
			return result;
		}
		throw new IllegalStateException("Attempting to read pushback " + "character from stream when it's not available.");
	}

	/**
	 * Method pushback.
	 *
	 * @param b Integer to push back
	 */
	protected void pushback(int b) {
		if(m_pushbackValid) {
			throw new IllegalStateException("Attempting to push multiple " + "characters back into stream.");
		}
		m_pushbackValid = true;
		m_pushbackBuffer = b;
	}
}

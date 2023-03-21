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
package nl.nn.adapterframework.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import nl.nn.adapterframework.core.IDataIterator;
import nl.nn.adapterframework.core.SenderException;

/**
 * Iterator that iterates over all lines in a Reader.
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 */
public class ReaderLineIterator implements IDataIterator<String> {

	private BufferedReader reader;
	private String line;

	public ReaderLineIterator(Reader inputreader) throws SenderException {
		super();
		reader = new BufferedReader(inputreader);
		try {
			line = reader.readLine();
		} catch (IOException e) {
			throw new SenderException(e);
		}
	}

	@Override
	public boolean hasNext() {
		return line != null;
	}

	@Override
	public String next() throws SenderException {
		String result = line;
		try {
			line = reader.readLine();
		} catch (IOException e) {
			throw new SenderException(e);
		}
		return result; 
	}

	@Override
	public void close() throws SenderException {
		try {
			reader.close();
		} catch (IOException e) {
			throw new SenderException(e);
		}
	}

}

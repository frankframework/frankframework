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
package nl.nn.adapterframework.pipes;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

import nl.nn.adapterframework.core.IDataIterator;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.ReaderLineIterator;

/**
 * Sends a message to a Sender for each line of its input, that must be an InputStream.
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 */
public class StreamLineIteratorPipe extends IteratingPipe {

	private String endOfLineString;
	
	protected Reader getReader(Object input, IPipeLineSession session, String correlationID, Map threadContext) throws SenderException {
		if (input==null) {
			throw new SenderException("input is null. Must supply stream as input");
		}
		if (!(input instanceof InputStream)) {
			throw new SenderException("input must be of type InputStream");
		}
		Reader reader=new InputStreamReader((InputStream)input);
		return reader;
	}

	protected IDataIterator getIterator(Object input, IPipeLineSession session, String correlationID, Map threadContext) throws SenderException {
		return new ReaderLineIterator(getReader(input,session, correlationID,threadContext));
	}

	protected String getItem(IDataIterator it) throws SenderException {
		String item = (String)it.next();
		if (getEndOfLineString()!=null) {
			while (!item.endsWith(getEndOfLineString()) && it.hasNext()) {
				item = item + System.getProperty("line.separator") + (String)it.next();
			}
		}
		return item;
	}

	@IbisDoc({"when set to <code>false</code>, the inputstream is not closed after it has been used", "true"})
	public void setCloseInputstreamOnExit(boolean b) {
		setCloseIteratorOnExit(b);
	}
	public boolean isCloseInputstreamOnExit() {
		return isCloseIteratorOnExit();
	}

	@IbisDoc({"when set, each line has to end with this string. if the line doesn't end with this string next lines are added (including line separators) until the total line ends with the given string", ""})
	public void setEndOfLineString(String string) {
		endOfLineString = string;
	}
	public String getEndOfLineString() {
		return endOfLineString;
	}
}

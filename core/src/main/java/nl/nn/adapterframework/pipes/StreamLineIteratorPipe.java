/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2021 WeAreFrank!

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

import java.io.Reader;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.core.IDataIterator;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PeekableDataIterator;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ReaderLineIterator;

/**
 * Sends a message to a Sender for each line of its input, that must be an InputStream.
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 */
public class StreamLineIteratorPipe extends StringIteratorPipe {

	private @Getter String endOfLineString;
	private @Getter String startOfLineString;
	
	protected Reader getReader(Message input, PipeLineSession session, Map<String,Object> threadContext) throws SenderException {
		if (input==null) {
			throw new SenderException("cannot obtain reader from null input");
		}
		try {
			return input.asReader();
		} catch (Exception e) {
			throw new SenderException(getLogPrefix(session)+"cannot open stream", e);
		}
	}

	@Override
	protected IDataIterator<String> getIterator(Message input, PipeLineSession session, Map<String,Object> threadContext) throws SenderException {
		return new PeekableDataIterator<>(new ReaderLineIterator(getReader(input,session, threadContext)));
	}

	@Override
	protected String getItem(IDataIterator<String> it) throws SenderException {
		StringBuffer item = new StringBuffer(it.next());
		if (StringUtils.isNotEmpty(getEndOfLineString()) || StringUtils.isNotEmpty(getStartOfLineString())) {
			String peeked = ((PeekableDataIterator<String>)it).peek();
			while (peeked!=null && 
					(StringUtils.isEmpty(getStartOfLineString()) || !peeked.startsWith(getStartOfLineString())) && 
					(StringUtils.isEmpty(getEndOfLineString())   || !item.toString().endsWith(getEndOfLineString()))) {
				item.append(System.getProperty("line.separator")).append(it.next());
				peeked = ((PeekableDataIterator<String>)it).peek();
			}
		}
		return item.toString();
	}

	@IbisDoc({"1", "If set, each record has to end with this string. If a line read doesn't end with this string more lines are added (including line separators) until the total record ends with the given string", ""})
	public void setEndOfLineString(String string) {
		endOfLineString = string;
	}
	@IbisDoc({"2", "Marks the start of a new record. If set, a new record is started when this line is read.", ""})
	public void setStartOfLineString(String string) {
		startOfLineString = string;
	}

	@IbisDoc({"3", "If set to <code>false</code>, the inputstream is not closed after it has been used", "true"})
	public void setCloseInputstreamOnExit(boolean b) {
		setCloseIteratorOnExit(b);
	}

}

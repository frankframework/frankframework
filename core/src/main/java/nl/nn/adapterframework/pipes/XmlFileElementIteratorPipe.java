/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.xml.SaxException;

/**
 * Sends a message to a Sender for each element in the XML file that the input message refers to.
 * 
 * @author  Peter Leeuwenburgh
 */
@Deprecated
@ConfigurationWarning("Please replace with ForEachChildElementPipe. ElementName and elementChain can be replaced with containerElement and/or targetElement. It is not a 1 to 1 replacement, different values may be required!")
public class XmlFileElementIteratorPipe extends IteratingPipe<String> {

	private String elementName = null;
	private String elementChain = null;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (!StringUtils.isEmpty(getElementName())) {
			if (!StringUtils.isEmpty(getElementChain())) {
				throw new ConfigurationException("cannot have both an elementName and an elementChain specified");
			}
		} else {
			if (StringUtils.isEmpty(getElementChain())) {
				throw new ConfigurationException("an elementName or an elementChain must be specified");
			}
		}
	}

	private class ItemCallbackCallingHandler extends DefaultHandler {

		private ItemCallback callback;
		private StringBuffer elementBuffer = new StringBuffer();
		private List<String> elements = new ArrayList<String>();
		private boolean sElem = false;
		private Exception rootException = null;
		private int startLength;
		private StopReason stopReason;
		private TimeoutException timeOutException;

		public ItemCallbackCallingHandler(ItemCallback callback) {
			this.callback = callback;
			elementBuffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			startLength = elementBuffer.length();
		}

		@Override
		public void startDocument() throws SAXException {
			try {
				callback.startIterating();
			} catch (SenderException | TimeoutException | IOException e) {
				throw new SaxException(e);
			}

			super.startDocument();
		}

		@Override
		public void endDocument() throws SAXException {
			super.endDocument();
			try {
				callback.endIterating();
			} catch (SenderException | TimeoutException | IOException e) {
				throw new SaxException(e);
			}
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			elements.add(localName);
			if ((getElementName() != null && localName.equals(getElementName()))
					|| (getElementChain() != null && elementsToString().equals(getElementChain()))) {
				sElem = true;
			}
			if (sElem) {
				elementBuffer.append("<" + localName);
				for (int i = 0; i < attributes.getLength(); i++) {
					elementBuffer.append(" " + attributes.getLocalName(i) + "=\"" + attributes.getValue(i) + "\"");
				}
				elementBuffer.append(">");
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			int lastIndex = elements.size() - 1;
			String lastElement = (String) elements.get(lastIndex);
			if (!lastElement.equals(localName)) {
				throw new SAXException("expected end element [" + lastElement + "] but got end element [" + localName + "]");
			}
			if (sElem) {
				elementBuffer.append("</" + localName + ">");
			}
			if ((getElementName() != null && localName.equals(getElementName()))
					|| (getElementChain() != null && elementsToString().equals(getElementChain()))) {
				try {
					stopReason = callback.handleItem(elementBuffer.toString());
					elementBuffer.setLength(startLength);
					sElem = false;
				} catch (Exception e) {
					if (e instanceof TimeoutException) {
						timeOutException = (TimeoutException) e;
					}
					rootException = e;
					Throwable rootCause = e;
					while (rootCause.getCause() != null) {
						rootCause = rootCause.getCause();
					}
					SAXException se = new SAXException(e);
					se.setStackTrace(rootCause.getStackTrace());
					throw se;

				}
				if (isStopRequested()) {
					throw new SAXException("stop maar");
				}
			}
			elements.remove(lastIndex);
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			if (sElem) {
				elementBuffer.append(XmlUtils.encodeChars(ch, start, length));
			}
		}

		private String elementsToString() {
			String chain = null;
			for (Iterator<String> it = elements.iterator(); it.hasNext();) {
				String element = (String) it.next();
				if (chain == null) {
					chain = element;
				} else {
					chain = chain + ";" + element;
				}
			}
			return chain;
		}

		public Exception getRootException() {
			return rootException;
		}

		public boolean isStopRequested() {
			return stopReason != null;
		}

		public TimeoutException getTimeOutException() {
			return timeOutException;
		}
	}

	@Override
	protected StopReason iterateOverInput(Message input, PipeLineSession session, Map<String,Object> threadContext, ItemCallback callback) throws SenderException, TimeoutException {
		InputStream xmlInput;
		try {
			xmlInput = new FileInputStream(input.asString());
		} catch (IOException e) {
			throw new SenderException("could not find file [" + input + "]", e);
		}
		ItemCallbackCallingHandler handler = new ItemCallbackCallingHandler(callback);

		log.debug("obtaining list of elements [" + getElementName() + "] using sax parser");
		try {
			SAXParserFactory parserFactory = XmlUtils.getSAXParserFactory();
			parserFactory.setNamespaceAware(true);
			SAXParser saxParser = parserFactory.newSAXParser();
			saxParser.parse(xmlInput, handler);
		} catch (Exception e) {
			if (handler.getTimeOutException() != null) {
				throw handler.getTimeOutException();
			}
			if (!handler.isStopRequested()) {
				throw new SenderException("Could not extract list of elements [" + getElementName() + "] using sax parser", e);
			}
			try {
				handler.endDocument();
			} catch (SAXException e1) {
				throw new SenderException("could not endDocument after stop was requested",e1);
			}
		}
		return handler.stopReason;
	}

	/** the name of the element to iterate over (alternatively: <code>elementchain</code>) */
	public void setElementName(String string) {
		elementName = string;
	}

	public String getElementName() {
		return elementName;
	}

	/** the name of the element to iterate over, preceded with all ancestor elements and separated by semicolons (e.g. adapter;pipeline;pipe) */
	public void setElementChain(String string) {
		elementChain = string;
	}

	public String getElementChain() {
		return elementChain;
	}
}

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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Sends a message to a Sender for each element in the XML file that the input message refers to.
 *
 * <p><b>Configuration </b><i>(where deviating from IteratingPipe)</i><b>:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setElementName(String) elementName}</td><td>the name of the element to iterate over (alternatively: <code>elementChain</code>)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setElementChain(String) elementChain}</td><td>the name of the element to iterate over, preceded with all ancestor elements and separated by semicolons (e.g. "adapter;pipeline;pipe")</td><td>&nbsp;</td></tr>
 * </table></p>
 * 
 * @author  Peter Leeuwenburgh
 */
public class XmlFileElementIteratorPipe extends IteratingPipe {

	private String elementName = null;
	private String elementChain = null;

	public void configure() throws ConfigurationException {
		super.configure();
		if (!StringUtils.isEmpty(getElementName())) {
			if (!StringUtils.isEmpty(getElementChain())) {
				throw new ConfigurationException(getLogPrefix(null) + "cannot have both an elementName and an elementChain specified");
			}
		} else {
			if (StringUtils.isEmpty(getElementChain())) {
				throw new ConfigurationException(getLogPrefix(null) + "an elementName or an elementChain must be specified");
			}
		}
	}

	private class ItemCallbackCallingHandler extends DefaultHandler {
		ItemCallback callback;
		StringBuffer elementBuffer = new StringBuffer();
		List elements = new ArrayList();
		boolean sElem = false;
		Exception rootException = null;
		int startLength;
		boolean stopRequested;
		TimeOutException timeOutException;

		public ItemCallbackCallingHandler(ItemCallback callback) {
			this.callback = callback;
			elementBuffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			startLength = elementBuffer.length();
		}

		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			elements.add(localName);
			if ((getElementName() != null && localName.equals(getElementName()))
					|| (getElementChain() != null && elementsToString().equals(
							getElementChain()))) {
				sElem = true;
			}
			if (sElem) {
				elementBuffer.append("<" + localName);
				for (int i = 0; i < attributes.getLength(); i++) {
					elementBuffer.append(" " + attributes.getLocalName(i)
							+ "=\"" + attributes.getValue(i) + "\"");
				}
				elementBuffer.append(">");
			}
		}

		public void endElement(String uri, String localName, String qName)
				throws SAXException {
			int lastIndex = elements.size() - 1;
			String lastElement = (String) elements.get(lastIndex);
			if (!lastElement.equals(localName)) {
				throw new SAXException("expected end element [" + lastElement
						+ "] but got end element [" + localName + "]");
			}
			if (sElem) {
				elementBuffer.append("</" + localName + ">");
			}
			if ((getElementName() != null && localName.equals(getElementName()))
					|| (getElementChain() != null && elementsToString().equals(
							getElementChain()))) {
				try {
					stopRequested = !callback.handleItem(elementBuffer
							.toString());
					elementBuffer.setLength(startLength);
					sElem = false;
				} catch (Exception e) {
					if (e instanceof TimeOutException) {
						timeOutException = (TimeOutException) e;
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
				if (stopRequested) {
					throw new SAXException("stop maar");
				}
			}
			elements.remove(lastIndex);
		}

		public void characters(char[] ch, int start, int length)
				throws SAXException {
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
			return stopRequested;
		}

		public TimeOutException getTimeOutException() {
			return timeOutException;
		}
	}

	protected void iterateInput(Object input, IPipeLineSession session,
			String correlationID, Map threadContext, ItemCallback callback)
			throws SenderException, TimeOutException {
		InputStream xmlInput;
		try {
			xmlInput = new FileInputStream((String) input);
		} catch (FileNotFoundException e) {
			throw new SenderException("could not find file [" + input + "]", e);
		}
		ItemCallbackCallingHandler handler = new ItemCallbackCallingHandler(
				callback);

		log.debug("obtaining list of elements [" + getElementName()
				+ "] using sax parser");
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
				throw new SenderException(
						"Could not extract list of elements [" + getElementName()
								+ "] using sax parser", e);
			}
		}
	}

	public void setElementName(String string) {
		elementName = string;
	}

	public String getElementName() {
		return elementName;
	}

	public void setElementChain(String string) {
		elementChain = string;
	}

	public String getElementChain() {
		return elementChain;
	}
}

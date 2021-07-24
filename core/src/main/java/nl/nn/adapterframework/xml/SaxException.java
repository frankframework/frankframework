/*
   Copyright 2019, 2021 WeAreFrank!

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
package nl.nn.adapterframework.xml;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public class SaxException extends SAXException {

	private static final long serialVersionUID = -3772313635358961207L;

	public SaxException() {
		super();
	}

	public SaxException(Exception e) {
		super(e);
		try {
			initCause(e); // this fixes stacktrace under IBM JDK, does nothing under standard JDK
		} catch (Exception e2) { // Jboss throws 'IllegalStateException: Can't overwrite cause'
			addSuppressed(e2);
		}
	}

	public SaxException(String message, Exception e) {
		super(message, e);
		try {
			initCause(e); // this fixes stacktrace under IBM JDK, does nothing under standard JDK
		} catch (Exception e2) { // Jboss throws 'IllegalStateException: Can't overwrite cause'
			addSuppressed(e2);
		}
	}

	public SaxException(String message) {
		super(message);
	}

	public static String getLocatedMessage(Locator locator, String message) {
		StringBuilder buf = new StringBuilder();
		if (locator.getPublicId() != null)   buf.append("publicId [").append(locator.getPublicId()).append("] ");
		if (locator.getSystemId() != null)   buf.append("systemId [").append(locator.getSystemId()).append("] ");
		if (locator.getLineNumber() != -1)   buf.append("line [").append(locator.getLineNumber()).append("] ");
		if (locator.getColumnNumber() != -1) buf.append("column [").append(locator.getColumnNumber()).append("] ");
		if (StringUtils.isNotEmpty(message)) {
			buf.append(message);
		}
		return buf.toString();
	}

	@Override
	public String toString() {
		return getClass().getName()+": "+getMessage(); // avoid duplicates in stack traces
	}

	public static SAXException createSaxException(String message, Locator locator, Exception e) {
		if (locator!=null) {
			// prefer this solution of creating a SaxException with locatin info in the messgage over creating 
			// a SaxParseException, because that causes the location info to be duplicated in a combined errormessage
			return new SaxException(getLocatedMessage(locator,message), e); 
		}
		return new SaxException(message, e);
	}

}

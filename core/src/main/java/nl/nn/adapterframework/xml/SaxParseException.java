/*
   Copyright 2019 Integration Partners

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

import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

public class SaxParseException extends SAXParseException {

	private static final long serialVersionUID = -5177059993976219302L;

	public SaxParseException(String message, Locator locator, Exception e) {
		super(message, locator, e);
		initCause(e); // this fixes stacktrace under IBM JDK, does nothing under standard JDK
	}

	public SaxParseException(String message, Locator locator) {
		super(message, locator);
	}

	public SaxParseException(String message, String publicId, String systemId, int lineNumber, int columnNumber, Exception e) {
		super(message, publicId, systemId, lineNumber, columnNumber, e);
		initCause(e); // this fixes stacktrace under IBM JDK, does nothing under standard JDK
	}

	public SaxParseException(String message, String publicId, String systemId, int lineNumber, int columnNumber) {
		super(message, publicId, systemId, lineNumber, columnNumber);	
	}

	@Override
	public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getName());
        String message = getLocalizedMessage();
        if (getPublicId()!=null)    buf.append(" publicId [").append(getPublicId()).append("]");
        if (getSystemId()!=null)    buf.append(" systemId[").append(getSystemId()).append("]");
        if (getLineNumber()!=-1)    buf.append(" lineNumber[").append(getLineNumber()).append("]");
        if (getColumnNumber()!=-1)  buf.append(" columnNumber[").append(getColumnNumber()).append("]");

       //append the exception message at the end
        if (message!=null)     buf.append(": ").append(message);
        return buf.toString();
	}

}

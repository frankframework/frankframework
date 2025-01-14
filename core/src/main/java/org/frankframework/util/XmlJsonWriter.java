/*
   Copyright 2021 WeAreFrank!

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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.frankframework.xml.SaxException;

public class XmlJsonWriter extends DefaultHandler implements ContentHandler {

	private final Writer writer;
	private boolean commaRequired=false;
	private boolean stringOpen=false;

	public XmlJsonWriter(Writer writer) {
		this.writer=writer;
	}

	public XmlJsonWriter() {
		this(new StringWriter());
	}

	@Override
	public void endDocument() throws SAXException {
		try {
			writer.close();
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	@Override
	public void startElement(String uri, String localname, String qname, Attributes attrs) throws SAXException {
		try {
			if (commaRequired) {
				writer.write(",");
			}
			commaRequired=false;
			if (attrs!=null) {
				String key=attrs.getValue("key");
				if (key!=null) {
					writer.append('"').append(key).append("\":");
				}
			}
			if ("array".equals(localname)) {
				writer.write("[");
			} else if ("map".equals(localname)) {
				writer.write("{");
			} else if ("null".equals(localname)) {
				writer.write("null");
			} else if ("string".equals(localname)) {
				stringOpen=true;
			}
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	@Override
	public void endElement(String uri, String localname, String qname) throws SAXException {
		try {
			if ("array".equals(localname)) {
				writer.write("]");
			} else if ("map".equals(localname)) {
				writer.write("}");
			} else if ("string".equals(localname)) {
				stringOpen=false;
			}
			commaRequired=true;
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	@Override
	public void characters(char[] chars, int start, int length) throws SAXException {
		try {
			if (stringOpen) writer.write('"');
			writer.write(chars, start, length);
			if (stringOpen) writer.write('"');
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	@Override
	public String toString() {
		return writer.toString().trim();
	}

}

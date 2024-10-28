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
package org.frankframework.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;

import org.frankframework.util.StreamUtil;
import org.frankframework.xml.XmlWriter;

public class XmlWriterTest {

	private final boolean TEST_CDATA = true;
	private final String CDATA_START = TEST_CDATA ? "<![CDATA[" : "";
	private final String CDATA_END = TEST_CDATA ? "]]>" : "";

	protected String testString="<root><sub name=\"P &amp; Q €\">abc&amp;€</sub><sub>"+CDATA_START+"<a>a&amp;b€</a>"+CDATA_END+"</sub><!--this is comment--></root>";

	@Test
	public void testToStream() throws Exception {
		ByteArrayOutputStream target = new ByteArrayOutputStream();
		XmlWriter xmlWriter = new XmlWriter(target);

		sendEvents(xmlWriter);

		String actual = new String (target.toString(StreamUtil.DEFAULT_INPUT_STREAM_ENCODING));
		assertEquals(testString, actual);
	}

	@Test
	public void testToWriter() throws Exception {
		StringWriter target = new StringWriter();
		XmlWriter xmlWriter = new XmlWriter(target);

		sendEvents(xmlWriter);

		String actual = new String (target.toString());
		assertEquals(testString, actual);
	}

	private void sendEvents(ContentHandler handler) throws SAXException {
		String line;
		handler.startDocument();
		handler.startElement("","root","root", new AttributesImpl());
			AttributesImpl atts = new AttributesImpl();
			atts.addAttribute("", "name", "name", "string", "P & Q €");
			handler.startElement("","sub","sub", atts);
				line="abc&€";
				handler.characters(line.toCharArray(),0,line.length());
			handler.endElement("","sub","sub");
			handler.startElement("","sub","sub", new AttributesImpl());
				((LexicalHandler)handler).startCDATA();
				line="<a>a&amp;b€</a>";
				handler.characters(line.toCharArray(),0,line.length());
				((LexicalHandler)handler).endCDATA();
			handler.endElement("","sub","sub");
			line="this is comment";
			((LexicalHandler)handler).comment(line.toCharArray(),0,line.length());
		handler.endElement("","root","root");
		handler.endDocument();
	}
}

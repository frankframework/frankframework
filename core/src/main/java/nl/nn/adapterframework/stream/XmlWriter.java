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
package nl.nn.adapterframework.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.xml.SaxException;

public class XmlWriter extends DefaultHandler implements LexicalHandler {
	protected Logger log = LogUtil.getLogger(this);
	
	private Writer writer;
	private boolean includeComments=true;
	
	private boolean elementJustStarted=false;
	private boolean inCdata;

	public XmlWriter() {
		writer=new StringWriter();
	}
	
	public XmlWriter(Writer writer) {
		this.writer=writer;
	}
	
	public XmlWriter(OutputStream stream) {
		try {
			this.writer=new OutputStreamWriter(stream,StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
		} catch (UnsupportedEncodingException e) {
			log.error(e);
		}
	}

	@Override
	public void endDocument() throws SAXException {
		try {
			writer.flush();
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		try {
			if (elementJustStarted) {
				writer.append(">");
			}
			writer.append("<"+qName);
			for (int i=0; i<attributes.getLength(); i++) {
				writer.append(" "+attributes.getQName(i)+"=\""+XmlUtils.encodeChars(attributes.getValue(i))+"\"");
			}
			elementJustStarted=true;
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		try {
			if (elementJustStarted) {
				elementJustStarted=false;
				writer.append("/>");
			} else {
				writer.append("</"+qName+">");
			}
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		try {
			if (elementJustStarted) {
				elementJustStarted=false;
				writer.append(">");
			}
			if (inCdata) {
				writer.append("<![CDATA[").append(new String(ch, start, length)).append("]]>");
			} else {
				writer.append(XmlUtils.encodeChars(new String(ch, start, length)));
			}
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	@Override
	public void comment(char[] ch, int start, int length) throws SAXException {
		try {
			if (includeComments) {
				writer.append("<!--").append(new String(ch, start, length)).append("-->");
			}
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	@Override
	public void startDTD(String arg0, String arg1, String arg2) throws SAXException {
//		System.out.println("startDTD");
	}

	@Override
	public void endDTD() throws SAXException {
//		System.out.println("endDTD");
	}

	@Override
	public void startCDATA() throws SAXException {
//		System.out.println("startCDATA");
		inCdata=true;
	}

	@Override
	public void endCDATA() throws SAXException {
//		System.out.println("endCDATA");
		inCdata=false;
	}

	@Override
	public void startEntity(String arg0) throws SAXException {
//		System.out.println("startEntity ["+arg0+"]");
	}
	@Override
	public void endEntity(String arg0) throws SAXException {
//		System.out.println("endEntity ["+arg0+"]");
	}

	@Override
	public String toString() {
		return writer.toString();
	}


}

/*
   Copyright 2020 WeAreFrank!

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
package nl.nn.adapterframework.jdbc.transformer;

import java.io.IOException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.XmlEncodingUtils;
import nl.nn.adapterframework.util.XmlUtils;

public abstract class AbstractQueryOutputTransformer extends XMLFilterImpl {
	private boolean parsingDefinitions = false;
	private String currentField;
	protected StringBuilder output, currentBuilder;

	public AbstractQueryOutputTransformer() {
		super();
	}

	public AbstractQueryOutputTransformer(XMLReader parent) {
		super(parent);
	}

	public String parse(Message message) throws IOException, SAXException {
		output = new StringBuilder();
		XmlUtils.parseXml(message.asInputSource(), this);
		return output.toString();
	}

	@Override
	public void startDocument() throws SAXException {
		startOut();
		super.startDocument();
	}

	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
		endOut();
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		if (localName.equalsIgnoreCase("field")) {
			if (parsingDefinitions) {
				addFieldDefinition(atts);
			} else if ("true".equalsIgnoreCase(atts.getValue("null"))) {
				addField(atts.getValue("name"), "");
			} else {
				currentBuilder = new StringBuilder();
				currentField = atts.getValue("name");
			}
		} else if (localName.equalsIgnoreCase("fielddefinition")) {
			startDefinitions();
		} else if (localName.equalsIgnoreCase("row")) {
			startRow();
		} else if (localName.equalsIgnoreCase("rowset")) {
			startRowSet();
		}
		super.startElement(uri, localName, qName, atts);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (localName.equalsIgnoreCase("field")) {
			if (currentBuilder != null)
				addField(currentField, XmlEncodingUtils.decodeChars(currentBuilder.toString()));
			currentBuilder = null;
			currentField = null;
		} else if (localName.equalsIgnoreCase("fielddefinition")) {
			endDefinitions();
		} else if (localName.equalsIgnoreCase("row")) {
			endRow();
		} else if (localName.equalsIgnoreCase("rowset")) {
			endRowSet();
		}
		super.endElement(uri, localName, qName);
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (currentBuilder != null) {
			String value = new String(ch).substring(start, start + length);
			currentBuilder.append(value);
		}
		super.characters(ch, start, length);
	}

	protected void startDefinitions() {
		parsingDefinitions = true;
	}

	protected void endDefinitions() {
		parsingDefinitions = false;
	}

	protected abstract void startOut();

	protected abstract void endOut();

	protected abstract void startRow();

	protected abstract void endRow();

	protected abstract void startRowSet();

	protected abstract void endRowSet();

	protected abstract void addFieldDefinition(Attributes atts);

	protected abstract void addField(String fieldName, String value);
}

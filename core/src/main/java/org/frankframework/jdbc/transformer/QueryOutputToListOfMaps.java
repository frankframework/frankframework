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
package org.frankframework.jdbc.transformer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import org.frankframework.stream.Message;

public class QueryOutputToListOfMaps extends AbstractQueryOutputTransformer {

	private List<Map<String, String>> rowset;
	private Map<String, String> currentRow;

	public QueryOutputToListOfMaps() throws SAXException {
		super();
	}

	public QueryOutputToListOfMaps(XMLReader parent) {
		super(parent);
	}

	public List<Map<String, String>> parseString(String message) throws IOException, SAXException, NullPointerException {
		try (Message closeable = new Message(message)) {
			return parseMessage(closeable);
		}
	}

	public List<Map<String, String>> parseMessage(Message message) throws IOException, SAXException, NullPointerException {
		super.parse(message);
		return rowset;
	}

	@Override
	protected void startOut() {
	}

	@Override
	protected void endOut() {
	}

	@Override
	protected void startRow() {
		currentRow = new HashMap<>();
	}

	@Override
	protected void endRow() {
		rowset.add(currentRow);
	}

	@Override
	protected void startRowSet() {
		rowset = new ArrayList<>();
	}

	@Override
	protected void endRowSet() {
	}

	@Override
	protected void addFieldDefinition(Attributes atts) {
	}

	@Override
	protected void addField(String fieldName, String value) {
		currentRow.put(fieldName, value);
	}
}

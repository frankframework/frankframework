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

import org.apache.commons.text.StringEscapeUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class QueryOutputToJson extends AbstractQueryOutputTransformer {
	private boolean rowsExist = false;

	public QueryOutputToJson() throws SAXException {
		super();
	}
	public QueryOutputToJson(XMLReader parent) {
		super(parent);
	}

	@Override
	protected void startOut() {
		output.append("{\n");
	}

	@Override
	protected void endOut() {
		output.append("}");
	}

	@Override
	protected void startRow() {
		rowsExist = true;
		output.append("\t\t{\n");
	}

	@Override
	protected void endRow() {
		// Delete last comma
		output.deleteCharAt(output.length() - 2);
		output.append("\t\t},\n");
	}

	@Override
	protected void startDefinitions() {
		output.append("\t\"fielddefinition\": [\n");
		super.startDefinitions();
	}

	@Override
	protected void endDefinitions() {
		// Delete last comma
		output.deleteCharAt(output.length() - 2);
		output.append("\n\t],\n");
		super.endDefinitions();
	}

	@Override
	protected void startRowSet() {
		output.append("\t\"rowset\": [\n");
	}

	@Override
	protected void endRowSet() {
		// Delete last comma
		if (rowsExist)
			output.deleteCharAt(output.length() - (2));
		output.append("\t]\n");
	}

	@Override
	protected void addFieldDefinition(Attributes atts) {
		output.append("\t\t{\n");
		for(int i = 0; i < atts.getLength(); i ++) {
			output
					.append("\t\t\t\"")
					.append(StringEscapeUtils.escapeEcmaScript(atts.getLocalName(i)))
					.append("\":\"")
					.append(StringEscapeUtils.escapeEcmaScript(atts.getValue(i)))
					.append("\",\n");
		}
		// Delete last comma
		if (atts.getLength() > 0)
			output.deleteCharAt(output.length() - 2);

		output.append("\t\t},\n");
	}

	@Override
	protected void addField(String fieldName, String value) {
		output
				.append("\t\t\t\"")
				.append(StringEscapeUtils.escapeEcmaScript(fieldName))
				.append("\":\"")
				.append(StringEscapeUtils.escapeEcmaScript(value))
				.append("\",\n");
	}
}

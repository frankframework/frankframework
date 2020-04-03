package nl.nn.adapterframework.jdbc.transformer;

import org.apache.commons.lang.StringEscapeUtils;
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
					.append(StringEscapeUtils.escapeJavaScript(atts.getLocalName(i)))
					.append("\":\"")
					.append(StringEscapeUtils.escapeJavaScript(atts.getValue(i)))
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
				.append(StringEscapeUtils.escapeJavaScript(fieldName))
				.append("\":\"")
				.append(StringEscapeUtils.escapeJavaScript(value))
				.append("\",\n");
	}
}

package nl.nn.adapterframework.jdbc.transformer;

import org.apache.commons.lang.StringEscapeUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class QueryOutputToCSV extends AbstractQueryOutputTransformer {
	public QueryOutputToCSV() throws SAXException {
		super();
	}

	public QueryOutputToCSV(XMLReader parent) {
		super(parent);
	}

	@Override
	protected void startRow() {
		output.append("\n");
	}

	@Override
	protected void endRow() {
		// Delete last comma
		output.deleteCharAt(output.length() - 1);
	}

	@Override
	protected void addFieldDefinition(Attributes atts) {
		output
				.append(StringEscapeUtils.escapeCsv(atts.getValue("name")))
				.append(",");
	}

	@Override
	protected void addField(String fieldName, String value) {
		output
				.append(StringEscapeUtils.escapeCsv(value))
				.append(",");
	}

	@Override
	protected void endDefinitions() {
		// Delete last comma
		output.deleteCharAt(output.length() - 1);
		super.endDefinitions();
	}

	@Override
	protected void startOut() {
	}

	@Override
	protected void endOut() {
	}

	@Override
	protected void startRowSet() {
	}

	@Override
	protected void endRowSet() {
	}
}

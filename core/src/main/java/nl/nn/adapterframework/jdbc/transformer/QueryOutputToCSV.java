package nl.nn.adapterframework.jdbc.transformer;

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
		super.startRow();
		output.append("\n");
	}

	@Override
	protected void endRow() {
		// Delete last comma
		output.deleteCharAt(output.length() - 1);
		super.endRow();
	}

	@Override
	protected void addFieldDefinition(Attributes atts) {
		output.append("\"").append(atts.getValue("name")).append("\",");
		super.addFieldDefinition(atts);
	}

	@Override
	protected void addField(String fieldName, String value) {
		output.append("\"").append(value).append("\",");
		super.addField(fieldName, value);
	}

	@Override
	protected void endDefinitions() {
		// Delete last comma
		output.deleteCharAt(output.length() - 1);
		super.endDefinitions();
	}
}

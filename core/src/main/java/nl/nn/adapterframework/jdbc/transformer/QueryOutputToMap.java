package nl.nn.adapterframework.jdbc.transformer;

import nl.nn.adapterframework.stream.Message;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryOutputToMap extends AbstractQueryOutputTransformer {
	List<Map<String, String>> rowset;
	Map<String, String> currentRow;

	public QueryOutputToMap() throws SAXException {
		super();
	}

	public QueryOutputToMap(XMLReader parent) {
		super(parent);
	}

	public List<Map<String, String>> parseString(String message) throws IOException, SAXException, NullPointerException {
		return parseMessage(new Message(message));
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

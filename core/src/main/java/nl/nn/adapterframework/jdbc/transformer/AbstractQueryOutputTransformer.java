package nl.nn.adapterframework.jdbc.transformer;

import nl.nn.adapterframework.stream.Message;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;

public class AbstractQueryOutputTransformer extends XMLFilterImpl {
	private boolean parsingDefinitions = false;
	protected StringBuilder output, currentField;

	public AbstractQueryOutputTransformer() throws SAXException {
		super(XMLReaderFactory.createXMLReader());
	}

	public AbstractQueryOutputTransformer(XMLReader parent) {
		super(parent);
	}

	public String parse(Message message) throws IOException, SAXException {
		output = new StringBuilder();
		this.parse(message.asInputSource());
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
				currentField = new StringBuilder();
			}
		} else if (localName.equalsIgnoreCase("fielddefinition")) {
			startDefinitions();
		} else if(localName.equalsIgnoreCase("row")) {
			startRow();
		}
		super.startElement(uri, localName, qName, atts);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (localName.equalsIgnoreCase("field")) {
			if (currentField != null)
				addField("lol", currentField.toString());
			currentField = null;
		} else if (localName.equalsIgnoreCase("fielddefinition")) {
			endDefinitions();
		}else if(localName.equalsIgnoreCase("row")) {
			endRow();
		}
		super.endElement(uri, localName, qName);
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (currentField != null) {
			String value = new String(ch).substring(start, start + length);
			currentField.append(value);
		}
		super.characters(ch, start, length);
	}

	protected void startOut() {
		System.err.println("Startout");
	};
	protected void endOut() {
		System.err.println("Endout");
	}
	protected void startRow() {
		System.err.println(" ------------ Start ROW");
	}
	protected void endRow() {
		System.err.println("End ROW");
	}
	protected void startDefinitions() {
		parsingDefinitions = true;
		System.err.println(" ------------ Start Definitions");
	}
	protected void endDefinitions() {
		parsingDefinitions = false;
		System.err.println("End Definitions");
	}
	protected void addFieldDefinition(Attributes atts) {
		for(int i = 0; i < atts.getLength(); i++) {
			System.err.println("-- " + atts.getLocalName(i) + " : " + atts.getValue(i));
		}
	}
	protected void addField(String fieldName, String value) {
		System.err.println(fieldName + " : " + value);
	}
}

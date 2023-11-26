package nl.nn.adapterframework.configuration;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.xml.FullXmlFilter;
import nl.nn.adapterframework.xml.WritableAttributes;

public class ClassNameRewriter extends FullXmlFilter {

	public ClassNameRewriter(ContentHandler handler) {
		super(handler);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		WritableAttributes writableAttributes = new WritableAttributes(attributes);
		String className = writableAttributes.getValue("className");
		if (className != null) {
			if (className.startsWith("nl.nn.adapterframework.")) {
				writableAttributes.setValue("className", className.replace("nl.nn.adapterframework.", "org.frankframework."));
			}
		}
		super.startElement(uri, localName, qName, writableAttributes);
	}
}

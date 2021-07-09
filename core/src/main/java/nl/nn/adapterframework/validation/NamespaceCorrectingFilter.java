package nl.nn.adapterframework.validation;

import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.xml.FullXmlFilter;

public class NamespaceCorrectingFilter extends FullXmlFilter {

	private final Map<String, String> namespaceCorrections;

	public NamespaceCorrectingFilter(ContentHandler handler, Map<String, String> namespaceCorrections) {
		super(handler);
		this.namespaceCorrections = namespaceCorrections;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		super.startElement(correctNamespace(uri), localName, qName, atts);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		super.endElement(correctNamespace(uri), localName, qName);
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		super.startPrefixMapping(prefix, correctNamespace(uri));
	}


	/**
	 * Uses a Map to 'correct' values. If there is no corresponding key in the map,
	 * the value itself will simply be returned, otherwise the corrected value which
	 * is the value in the map.
	 */
	private String correctNamespace(String ns) {
		return namespaceCorrections.containsKey(ns) ? namespaceCorrections.get(ns) : ns;
	}

}

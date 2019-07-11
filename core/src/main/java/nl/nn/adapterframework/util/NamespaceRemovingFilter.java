package nl.nn.adapterframework.util;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

public class NamespaceRemovingFilter extends XMLFilterImpl {
//	Logger log = LogUtil.getLogger(this.getClass());
	
	NamespaceRemovingFilter(XMLReader xmlReader) {
		super(xmlReader);
	}
		
	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
//		log.debug("startElement("+uri+","+localName+","+qName+")");
		super.startElement("", localName, qName, new NamespaceRemovingAttributesWrapper(atts));
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		//log.debug("endElement("+uri+","+localName+","+qName+")");
		super.endElement("", localName, qName);
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		//log.debug("startPrefixMapping("+prefix+","+uri+")");
		super.startPrefixMapping(prefix, "");
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		//log.debug("endPrefixMapping("+prefix+")");
		super.endPrefixMapping(prefix);
	}

}


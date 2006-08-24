/*
 * $Log: RootElementFindingHandler.java,v $
 * Revision 1.1  2006-08-24 09:24:52  europe\L190409
 * separated finding wrong root from non-wellformedness;
 * used RootElementFindingHandler in XmlUtils.isWellFormed()
 *
 * Revision 1.1  2006/08/23 14:00:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.util;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX ContentHandler that allows to extract name of root element. 
 * 
 * @author Gerrit van Brakel
 * @since  
 * @version Id
 */
public class RootElementFindingHandler extends DefaultHandler {

	private boolean firstElement = true;
	public String rootElementName = null;

	public void startElement(String namespaceURI, String lName, String qName, Attributes attrs)
		throws SAXException {
		if (firstElement) {
			rootElementName = lName;
			firstElement = false;
		}
	}

	public String getRootElementName() {
		return rootElementName;
	}

}

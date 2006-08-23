/*
 * $Log: RootElementCheckingHandler.java,v $
 * Revision 1.1  2006-08-23 14:00:19  europe\L190409
 * first version
 *
 */
package nl.nn.adapterframework.util;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX ContentHandler that asserts value of root element. 
 * 
 * @author Gerrit van Brakel
 * @since  
 * @version Id
 */
public class RootElementCheckingHandler extends DefaultHandler {

	private boolean firstElement = true;
	public String rootElementName = null;

	public RootElementCheckingHandler(String rootElementName) {
		super();
		this.rootElementName=rootElementName;
	}

	public void startElement(String namespaceURI, String lName, String qName, Attributes attrs)
		throws SAXException {
		if (firstElement) {
			if (!lName.equals(rootElementName)) {
				throw new SAXException("RootElement ["+lName+"] does not match required [" +rootElementName+"]");
			}
			firstElement = false;
		}
	}

}

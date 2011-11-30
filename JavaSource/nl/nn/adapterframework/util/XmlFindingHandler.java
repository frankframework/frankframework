package nl.nn.adapterframework.util;

import java.util.Iterator;
import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX ContentHandler that allows to extract document info. 
 * 
 * @author Gerrit van Brakel
 * @since  
 * @version Id
 */
public class XmlFindingHandler extends DefaultHandler {

	private boolean firstElement = true;
	public String rootElementName = null;
	public String elementName = null;
	private int level = -1;
	private Vector elements = new Vector();

	public void startElement(String namespaceURI, String lName, String qName, Attributes attrs)
		throws SAXException {
			level++;
			elements.add(lName);
			elementName = lName;
			if (firstElement) {
				rootElementName = lName;
				firstElement = false;
			}
	}

	public void endElement(String namespaceURI, String lName, String qName)
		throws SAXException {
			elements.remove(level);
			level--;
	}

	public String getRootElementName() {
		return rootElementName;
	}

	public String getElementName() {
		return elementName;
	}

	public String getXpath() {
		String xpath = "";
		Iterator it = elements.iterator();
		if (it.hasNext()) {
			xpath = (String)it.next();
		}
		while (it.hasNext()) {
			xpath = xpath + "/" + it.next();
		}
		return xpath;
	}
}

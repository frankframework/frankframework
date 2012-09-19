/*
 * $Log: XmlValidatorContentHandler.java,v $
 * Revision 1.1  2012-09-19 09:49:58  m00f069
 * - Set reasonSessionKey to "failureReason" and xmlReasonSessionKey to "xmlFailureReason" by default
 * - Fixed check on unknown namspace in case root attribute or xmlReasonSessionKey is set
 * - Fill reasonSessionKey with a message when an exception is thrown by parser instead of the ErrorHandler being called
 * - Added/fixed check on element of soapBody and soapHeader
 * - Cleaned XML validation code a little (e.g. moved internal XmlErrorHandler class (double code in two classes) to an external class, removed MODE variable and related code)
 *
 */
package nl.nn.adapterframework.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.xerces.xni.grammars.Grammar;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

/**
 * SAX ContentHandler used during XML validation for some additional validation
 * checks and getting more information in case validation fails. 
 * 
 * @author Gerrit van Brakel
 * @author Jaco de Groot
 */
public class XmlValidatorContentHandler extends DefaultHandler2 {
	public String elementName = null;
	private int level = -1;
	private List<String> elements = new ArrayList<String>();
	private Map<String, Grammar>grammarsValidation;
	private Set<List<String>> singleLeafValidations;
	
	public XmlValidatorContentHandler(Map<String, Grammar> grammarsValidation,
			Set<List<String>> singleLeafValidations) {
		this.grammarsValidation = grammarsValidation;
		this.singleLeafValidations = singleLeafValidations;
	}
	
	public void startElement(String namespaceURI, String lName, String qName,
			Attributes attrs) throws SAXException {
		if (singleLeafValidations != null) {
			for (List<String> path: singleLeafValidations) {
				int i = elements.size();
				if (path.size() == i + 1 
						&& elements.equals(path.subList(0, i))
						&& !path.get(i).equals(lName)) {
					throw new UnknownElementException("Unknown element '"
							+ lName + "' at '" + getXpath()
							+ "', expecting element '" + path.get(i) + "'.");
				}
			}
		}
		level++;
		elements.add(lName);
		elementName = lName;
	}

	public void endElement(String namespaceURI, String lName, String qName)
			throws SAXException {
		elements.remove(level);
		level--;
	}

	public void startPrefixMapping(String prefix, String namespace) throws SAXException {
		if (grammarsValidation != null) {
			Grammar grammar = grammarsValidation.get(namespace);
			if (grammar == null) {
				throw new UnknownNamespaceException("Unknown namespace " + namespace);
			}
		}
	}

	public String getElementName() {
		return elementName;
	}

	public String getXpath() {
		String xpath = "/";
		Iterator<String> it = elements.iterator();
		if (it.hasNext()) {
			xpath = xpath + (String)it.next();
		}
		while (it.hasNext()) {
			xpath = xpath + "/" + it.next();
		}
		return xpath;
	}

	public static class UnknownElementException extends SAXException {
		private static final long serialVersionUID = 1L;

		public UnknownElementException(String s) {
			super(s);
		}
	}

	public static class UnknownNamespaceException extends SAXException {
		private static final long serialVersionUID = 1L;

		public UnknownNamespaceException(String s) {
			super(s);
		}
	}

}

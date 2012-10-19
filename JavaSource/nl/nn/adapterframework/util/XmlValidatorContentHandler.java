/*
 * $Log: XmlValidatorContentHandler.java,v $
 * Revision 1.9  2012-10-19 09:33:47  m00f069
 * Made WsdlXmlValidator extent Xml/SoapValidator to make it use the same validation logic, cleaning XercesXmlValidator on the way
 *
 * Revision 1.8  2012/10/16 15:50:57  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Prevent NullPointerException when no root validations need to be done
 *
 * Revision 1.7  2012/10/15 13:49:40  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Check root elements to not occur more than once.
 *
 * Revision 1.6  2012/10/13 15:45:17  Jaco de Groot <jaco.de.groot@ibissource.org>
 * When checking for unknown namespaces also execute check when empty namespace is found
 *
 * Revision 1.5  2012/10/13 12:37:16  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Check whether all root elements have been found
 *
 * Revision 1.4  2012/10/01 07:59:29  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Improved messages stored in reasonSessionKey and xmlReasonSessionKey
 * Cleaned XML validation code and documentation a bit.
 *
 * Revision 1.3  2012/09/28 13:51:49  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Restored illegalRoot forward and XML_VALIDATOR_ILLEGAL_ROOT_MONITOR_EVENT with new check on root implementation.
 *
 * Revision 1.2  2012/09/19 21:40:37  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added ignoreUnknownNamespaces attribute
 *
 * Revision 1.1  2012/09/19 09:49:58  Jaco de Groot <jaco.de.groot@ibissource.org>
 * - Set reasonSessionKey to "failureReason" and xmlReasonSessionKey to "xmlFailureReason" by default
 * - Fixed check on unknown namspace in case root attribute or xmlReasonSessionKey is set
 * - Fill reasonSessionKey with a message when an exception is thrown by parser instead of the ErrorHandler being called
 * - Added/fixed check on element of soapBody and soapHeader
 * - Cleaned XML validation code a little (e.g. moved internal XmlErrorHandler class (double code in two classes) to an external class, removed MODE variable and related code)
 *
 */
package nl.nn.adapterframework.util;

import java.util.ArrayList;
import java.util.HashSet;
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
	private static final int MAX_NAMESPACE_WARNINGS = 5;

	// state
	private int level = -1;
	private List<String> elements = new ArrayList<String>();

	private final Set<String> validNamespaces;
	private final Set<List<String>> rootValidations;
	private final Set<List<String>> rootElementsFound = new HashSet<List<String>>();
	private final boolean ignoreUnknownNamespaces;
	private XmlValidatorErrorHandler xmlValidatorErrorHandler;
	private int namespaceWarnings = 0;

	/**
	 * 
	 * @param grammarsValidation
	 * @param rootValidations
	 *         contains path's (just a single element in case of the root of the
	 *         entire xml) to root elements which should be checked upon
	 * @param ignoreUnknownNamespaces
	 */
	public XmlValidatorContentHandler(Set<String> validNamespaces,
				Set<List<String>> rootValidations,
				boolean ignoreUnknownNamespaces) {
		this.validNamespaces = validNamespaces;
		this.rootValidations = rootValidations;
		this.ignoreUnknownNamespaces = ignoreUnknownNamespaces;
	}

	public void setXmlValidatorErrorHandler(
			XmlValidatorErrorHandler xmlValidatorErrorHandler) {
		this.xmlValidatorErrorHandler = xmlValidatorErrorHandler;
	}

	@Override
	public void startElement(String namespaceURI, String lName, String qName,
			Attributes attrs) throws SAXException {
		if (rootValidations != null) {
			for (List<String> path: rootValidations) {
				int i = elements.size();
				if (path.size() == i + 1
						&& elements.equals(path.subList(0, i))) {
					if (path.get(i).equals(lName)) {
						if (rootElementsFound.contains(path)) {
							String message = "Element '" + lName
									+ "' should occur only once.";
							if (xmlValidatorErrorHandler != null) {
								xmlValidatorErrorHandler.addReason(message,
										null);
							} else {
								throw new IllegalRootElementException(message);
							}
						} else {
							rootElementsFound.add(path);
						}
					} else {
						String message = "Illegal element '" + lName
								+ "'. Element '" + path.get(i) + "' expected.";
						if (xmlValidatorErrorHandler != null) {
							xmlValidatorErrorHandler.addReason(message, null);
						} else {
							throw new IllegalRootElementException(message);
						}
					}
				}
			}
		}
		level++;
		elements.add(lName);
		checkNamespaceExistance(namespaceURI);
	}

	@Override
	public void endElement(String namespaceURI, String lName, String qName)
			throws SAXException {
		elements.remove(level);
		level--;
	}

	@Override
	public void endDocument() throws SAXException {
		if (rootValidations != null) {
			for (List<String> path: rootValidations) {
				if (!rootElementsFound.contains(path)) {
					String message = "Element " + getXpath(path) + " not found";
					if (xmlValidatorErrorHandler != null) {
						xmlValidatorErrorHandler.addReason(message, null);
					} else {
						throw new IllegalRootElementException(message);
					}
				}
			}
		}
	}

	protected void checkNamespaceExistance(String namespace)
			throws UnknownNamespaceException {
		if (!ignoreUnknownNamespaces && validNamespaces != null
				&& namespaceWarnings <= MAX_NAMESPACE_WARNINGS) {
			if (!validNamespaces.contains(namespace) && !("".equals(namespace)
					&& validNamespaces.contains(null))) {
				String message = "Unknown namespace '" + namespace + "'";
				namespaceWarnings++;
				if (namespaceWarnings > MAX_NAMESPACE_WARNINGS) {
					message = message
							+ " (maximum number of namespace warnings reached)";
				}
				if (xmlValidatorErrorHandler != null) {
					xmlValidatorErrorHandler.addReason(message, null);
				} else {
					throw new UnknownNamespaceException(message);
				}
			}
		}
	}

	public String getXpath() {
		return getXpath(elements);
	}

	public String getXpath(List<String> path) {
		StringBuilder xpath = new StringBuilder("/");
		Iterator<String> it = path.iterator();
		if (it.hasNext()) {
			xpath.append(it.next());
		}
		while (it.hasNext()) {
			xpath.append('/').append(it.next());
		}
		return xpath.toString();
	}

	public static class IllegalRootElementException extends SAXException {
		private static final long serialVersionUID = 1L;

		public IllegalRootElementException(String s) {
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

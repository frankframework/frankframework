/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
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
	private final Map<List<String>, List<String>> invalidRootNamespaces;
	private final Set<List<String>> rootElementsFound = new HashSet<List<String>>();
	private final boolean ignoreUnknownNamespaces;
	private XmlValidatorErrorHandler xmlValidatorErrorHandler;
	private int namespaceWarnings = 0;

	/**
	 *
	 * @param validNamespaces validNamespacesgrammarsValidation
	 * @param rootValidations
	 *         contains path's (just a single element in case of the root of the
	 *         entire xml) to root elements which should be checked upon
	 * @param ignoreUnknownNamespaces
	 */
	public XmlValidatorContentHandler(Set<String> validNamespaces, Set<List<String>> rootValidations, Map<List<String>, List<String>> invalidRootNamespaces, Boolean ignoreUnknownNamespaces) {
		this.validNamespaces = validNamespaces;
		this.rootValidations = rootValidations;
		this.invalidRootNamespaces = invalidRootNamespaces;

		if (ignoreUnknownNamespaces==null) {
			this.ignoreUnknownNamespaces=false; // to avoid NullPointerException when not initialized
		} else {
			this.ignoreUnknownNamespaces = ignoreUnknownNamespaces;
		}
	}

	public void setXmlValidatorErrorHandler(
			XmlValidatorErrorHandler xmlValidatorErrorHandler) {
		this.xmlValidatorErrorHandler = xmlValidatorErrorHandler;
	}

	@Override
	public void startElement(String namespaceURI, String lName, String qName, Attributes attrs) throws SAXException {
		if (rootValidations != null) {
			for (List<String> path: rootValidations) {
				int i = elements.size();
				if (path.size() == i + 1
						&& elements.equals(path.subList(0, i))) {
					String validElements = path.get(i);
					if (StringUtils.isEmpty(validElements)) {
						String message = "Illegal element '" + lName + "'. No element expected.";
						if (xmlValidatorErrorHandler != null) {
							xmlValidatorErrorHandler.addReason(message, null, null);
						} else {
							throw new IllegalRootElementException(message);
						}
					} else {
						List<String> validElementsAsList = Arrays.asList(validElements.split(",", -1));
						if (validElementsAsList.contains(lName)) {
							if (rootElementsFound.contains(path)) {
								String message = "Element(s) '" + lName + "' should occur only once.";
								if (xmlValidatorErrorHandler != null) {
									xmlValidatorErrorHandler.addReason(message, null, null);
								} else {
									throw new IllegalRootElementException(message);
								}
							} else {
								String message = null;
								if (invalidRootNamespaces != null) {
									List<String> invalidNamespaces = invalidRootNamespaces.get(path);
									if (invalidNamespaces != null && invalidNamespaces.contains(namespaceURI)) {
										message = "Invalid namespace '" + namespaceURI + "' for element '" + lName + "'";
										if (xmlValidatorErrorHandler != null) {
											xmlValidatorErrorHandler.addReason(message, null, null);
										} else {
											throw new UnknownNamespaceException(message);
										}
									}
								}
								rootElementsFound.add(path);
							}
						} else {
							String message = "Illegal element '" + lName + "'. Element(s) '" + validElements + "' expected.";
							if (xmlValidatorErrorHandler != null) {
								xmlValidatorErrorHandler.addReason(message, null, null);
							} else {
								throw new IllegalRootElementException(message);
							}
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
	public void endElement(String namespaceURI, String lName, String qName) throws SAXException {
		elements.remove(level);
		level--;
	}

	@Override
	public void endDocument() throws SAXException {
		if (rootValidations != null) {
			for (List<String> path: rootValidations) {
				String validElements = path.get(path.size() - 1);
				List<String> validElementsAsList = Arrays.asList(validElements.split("\\,", -1));
				if (!validElementsAsList.contains("") && !rootElementsFound.contains(path)) {
					String message = "Element(s) '" + validElements + "' not found";
					if (xmlValidatorErrorHandler != null) {
						xmlValidatorErrorHandler.addReason(message, getXpath(path.subList(0, path.size() - 1)), null);
					} else {
						throw new IllegalRootElementException(message);
					}
				}
			}
		}
	}

	protected void checkNamespaceExistance(String namespace) throws UnknownNamespaceException {
		if (!ignoreUnknownNamespaces && validNamespaces != null && namespaceWarnings <= MAX_NAMESPACE_WARNINGS) {
			if (!validNamespaces.contains(namespace) && !("".equals(namespace) && validNamespaces.contains(null))) {
				String message = "Unknown namespace '" + namespace + "'";
				namespaceWarnings++;
				if (namespaceWarnings > MAX_NAMESPACE_WARNINGS) {
					message = message + " (maximum number of namespace warnings reached)";
				}
				if (xmlValidatorErrorHandler != null) {
					xmlValidatorErrorHandler.addReason(message, null, null);
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

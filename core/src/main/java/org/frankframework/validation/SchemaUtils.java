/*
   Copyright 2013, 2015 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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
package org.frankframework.validation;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import javanet.staxutils.XMLStreamEventWriter;
import javanet.staxutils.XMLStreamUtils;
import javanet.staxutils.events.AttributeEvent;
import javanet.staxutils.events.StartElementEvent;
import org.apache.logging.log4j.Logger;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.configuration.SuppressKeys;
import org.frankframework.core.IConfigurationAware;
import org.frankframework.core.IScopeProvider;
import org.frankframework.util.LogUtil;
import org.frankframework.util.XmlUtils;
import org.frankframework.validation.xsd.StringXsd;

/**
 * @author Michiel Meeuwissen
 * @author Jaco de Groot
 */
public class SchemaUtils {
	protected static final Logger LOG = LogUtil.getLogger(SchemaUtils.class);

	public static final String XSD		  = XMLConstants.W3C_XML_SCHEMA_NS_URI;//"http://www.w3.org/2001/XMLSchema";

	public static final QName SCHEMA		 = new QName(XSD,  "schema");
	public static final QName ELEMENT		 = new QName(XSD,  "element");
	public static final QName IMPORT		 = new QName(XSD,  "import");
	public static final QName INCLUDE		 = new QName(XSD,  "include");
	public static final QName REDEFINE		 = new QName(XSD,  "redefine");
	public static final QName TNS			 = new QName(null, "targetNamespace");
	public static final QName ELFORMDEFAULT	 = new QName(null, "elementFormDefault");
	public static final QName SCHEMALOCATION = new QName(null, "schemaLocation");
	public static final QName NAMESPACE		 = new QName(null, "namespace");
	public static final QName NAME			 = new QName(null, "name");

	public static final QName WSDL_SCHEMA = new QName(XSD, "schema", "");


	public static Map<String, Set<IXSD>> getXsdsGroupedByNamespace(Set<IXSD> xsds, boolean sort) {
		Map<String, Set<IXSD>> result;
		if (sort) {
			result = new TreeMap<>();
		} else {
			result = new LinkedHashMap<>();
		}
		for (IXSD xsd : xsds) {
			Set<IXSD> set = result.computeIfAbsent(xsd.getNamespace(), key -> {
				if (sort) {
					return new TreeSet<>();
				} else {
					return new LinkedHashSet<>();
				}
			});
			set.add(xsd);
		}
		return result;
	}

	public static void mergeRootXsdsGroupedByNamespaceToSchemasWithIncludes(Map<String, Set<IXSD>> rootXsdsGroupedByNamespace, XMLStreamWriter xmlStreamWriter) throws XMLStreamException {
		// As the root XSD's are written as includes there's no need to change the imports and includes in the root XSD's.
		for (Map.Entry<String, Set<IXSD>> entry: rootXsdsGroupedByNamespace.entrySet()) {
			String namespace = entry.getKey();
			Set<IXSD> xsds = entry.getValue();
			xmlStreamWriter.writeStartElement(XSD, "schema");
			xmlStreamWriter.writeAttribute("targetNamespace", namespace);
			for (IXSD xsd : xsds) {
				xmlStreamWriter.writeEmptyElement(XSD, "include");
				xmlStreamWriter.writeAttribute("schemaLocation", xsd.getResourceTarget());
			}
			xmlStreamWriter.writeEndElement();
		}
	}


	/**
	 * Returns XSDs when xmlStreamWriter is null, otherwise write to xmlStreamWriter.
	 *
	 * @return XSDs when xmlStreamWriter is null, otherwise write to xmlStreamWriter
	 */
	public static Set<IXSD> mergeXsdsGroupedByNamespaceToSchemasWithoutIncludes(IScopeProvider scopeProvider, Map<String, Set<IXSD>> xsdsGroupedByNamespace, XMLStreamWriter xmlStreamWriter) throws XMLStreamException, IOException, ConfigurationException {
		Set<IXSD> resultXsds = new LinkedHashSet<>();
		// iterate over the namespaces
		for (Map.Entry<String, Set<IXSD>> entry: xsdsGroupedByNamespace.entrySet()) {
			String namespace = entry.getKey();
			Set<IXSD> xsds = entry.getValue();

			addXsdMergeWarnings(scopeProvider, xsds, namespace);

			// Get attributes of root elements and get import elements from all XSDs
			List<Attribute> rootAttributes = new ArrayList<>();
			List<Namespace> rootNamespaceAttributes = new ArrayList<>();
			List<XMLEvent> imports = new ArrayList<>();
			for (IXSD xsd: xsds) {
				xsdToXmlStreamWriter(xsd, false, true, false, false, rootAttributes, rootNamespaceAttributes, imports);
			}
			// Write XSD's with merged root element
			StringXsd resultXsd = null;
			XMLStreamWriter w;
			if (xmlStreamWriter == null) {
				StringWriter schemaContentsWriter = new StringWriter();
				resultXsd = new StringXsd();
				resultXsd.setSchemaContentsWriter(schemaContentsWriter);
				w = XmlUtils.REPAIR_NAMESPACES_OUTPUT_FACTORY.createXMLStreamWriter(schemaContentsWriter);
			} else {
				w = xmlStreamWriter;
			}
			int i = 0;
			// perform the merge
			for (IXSD xsd: xsds) {
				i++;
				boolean skipRootElementStart;
				boolean skipRootElementEnd;
				if (xsds.size() == 1) {
					skipRootElementStart = false;
					skipRootElementEnd = false;
				} else {
					skipRootElementStart = i != 1;
					skipRootElementEnd = i < xsds.size();
				}
				xsdToXmlStreamWriter(xsd, w, false, true, skipRootElementStart, skipRootElementEnd, rootAttributes, rootNamespaceAttributes, imports, false);
			}
			// TODO: After creating merged XSD, while we still know what the source files are, we should now validate for duplicate elements
			if (resultXsd != null) {
				IXSD firstXsd = xsds.iterator().next();
				resultXsd.setImportedSchemaLocationsToIgnore(firstXsd.getImportedSchemaLocationsToIgnore());
				resultXsd.setUseBaseImportedSchemaLocationsToIgnore(firstXsd.isUseBaseImportedSchemaLocationsToIgnore());
				resultXsd.setImportedNamespacesToIgnore(firstXsd.getImportedNamespacesToIgnore());
				resultXsd.initFromXsds(namespace, scopeProvider, xsds);
				resultXsds.add(resultXsd);
			}
		}
		return resultXsds;
	}

	private static void addXsdMergeWarnings(final IScopeProvider scopeProvider, final Set<IXSD> xsds, final String namespace) {
		// If there are multiple XSDs for the namespace, give ConfigWarning that explains wherefrom
		// each file is included.
		if (xsds.size() <= 1) {
			return;
		}
		StringBuilder message = new StringBuilder("Multiple XSDs for namespace '" + namespace + "' will be merged: ");
		for (IXSD xsd: xsds) {
			message.append("\n - XSD path '")
					.append(xsd.getResourceTarget())
					.append("'");

			if (xsd.getImportParent() != null) {
				message.append(", included from '")
						.append(xsd.getImportParent().getResourceTarget()).append("'");
			}
		}
		message.append("\nPlease check that there are no overlapping definitions between these XSDs.");
		LOG.info(message);

		// And check for duplicate XSDs included multiple times from multiple locations
		if (scopeProvider instanceof IConfigurationAware aware) {
			IXSD[] xsdList = xsds.toArray(new IXSD[0]);
			for (int i = 0; i < xsdList.length - 1; i++) {
				IXSD xsd1 = xsdList[i];
				for (int j = i + 1; j < xsdList.length; j++) {
					IXSD xsd2 = xsdList[j];
					if (xsd1.compareToByContents(xsd2) == 0) {
						StringBuilder duplicateXsdError = new StringBuilder("Identical XSDs with different source path imported for same namespace. This is likely an error.\n Namespace: '");
						duplicateXsdError.append(namespace)
								.append("'.\n Path '").append(xsd1.getResourceTarget()).append("'");
						if (xsd1.getImportParent() != null) {
							duplicateXsdError.append(" imported from '").append(xsd1.getImportParent().getResourceTarget()).append("'");
						}
						duplicateXsdError.append(",\n and also Path '").append(xsd2.getResourceTarget()).append("'");
						if (xsd2.getImportParent() != null) {
							duplicateXsdError.append(" imported from '").append(xsd2.getImportParent().getResourceTarget()).append("'");
						}
						ConfigurationWarnings.add(aware, LOG, duplicateXsdError.toString(), SuppressKeys.XSD_VALIDATION_ERROR_SUPPRESS_KEY);
					}
				}
			}
		}
	}

	public static void xsdToXmlStreamWriter(final IXSD xsd, XMLStreamWriter xmlStreamWriter) throws IOException, ConfigurationException {
		xsdToXmlStreamWriter(xsd, xmlStreamWriter, true, false, false, false, null, null, null, false);
	}

	private static void xsdToXmlStreamWriter(final IXSD xsd, boolean standalone, boolean stripSchemaLocationFromImport, boolean skipRootStartElement, boolean skipRootEndElement, List<Attribute> rootAttributes, List<Namespace> rootNamespaceAttributes, List<XMLEvent> imports) throws IOException, ConfigurationException {
		StringWriter dummySchemaContentsWriter = new StringWriter();
		try {
			XMLStreamWriter w = XmlUtils.REPAIR_NAMESPACES_OUTPUT_FACTORY.createXMLStreamWriter(dummySchemaContentsWriter);
			xsdToXmlStreamWriter(xsd, w, standalone, stripSchemaLocationFromImport, skipRootStartElement, skipRootEndElement, rootAttributes, rootNamespaceAttributes, imports, true);
		} catch (XMLStreamException e) {
			throw new ConfigurationException("unable to create stream writer", e);
		}
	}

	/**
	 * Including a {@link IXSD} into an {@link XMLStreamWriter} while parsing it. It is parsed
	 * (using a low level {@link XMLEventReader}) so that certain things can be corrected on the fly.
	 *
	 * @param xsd
	 * @param xmlStreamWriter
	 * @param standalone When standalone the start and end document contants are ignored, hence the xml declaration is ignored.
	 * @param stripSchemaLocationFromImport Useful when generating a WSDL which should contain all XSD's inline (without includes or imports).
	 *        The XSD might have an import with schemaLocation to make it valid on it's own, when stripSchemaLocationFromImport is true it will be removed.
	 */
	public static void xsdToXmlStreamWriter(final IXSD xsd, XMLStreamWriter xmlStreamWriter, boolean standalone, boolean stripSchemaLocationFromImport, boolean skipRootStartElement, boolean skipRootEndElement, List<Attribute> rootAttributes, List<Namespace> rootNamespaceAttributes, List<XMLEvent> imports, boolean noOutput) throws IOException, ConfigurationException {
		Map<String, String> namespacesToCorrect = new HashMap<>();
		NamespaceCorrectingXMLStreamWriter namespaceCorrectingXMLStreamWriter = new NamespaceCorrectingXMLStreamWriter(xmlStreamWriter, namespacesToCorrect);
		final XMLStreamEventWriter streamEventWriter = new XMLStreamEventWriter(namespaceCorrectingXMLStreamWriter);
		XMLEvent event = null;
		try (Reader reader = xsd.getReader()) {
			if (reader == null) {
				throw new IllegalStateException(xsd + " not found");
			}
			XMLEventReader er = XmlUtils.INPUT_FACTORY.createXMLEventReader(reader);
			while (er.hasNext()) {
				event = er.nextEvent();
				switch (event.getEventType()) {
					case XMLStreamConstants.START_DOCUMENT:
					case XMLStreamConstants.END_DOCUMENT:
						if (! standalone) {
							continue;
						}
					//$FALL-THROUGH$
					case XMLStreamConstants.SPACE:
					case XMLStreamConstants.COMMENT:
						break;
					case XMLStreamConstants.START_ELEMENT:
						StartElement startElement = event.asStartElement();
						if (SCHEMA.equals(startElement.getName())) {
							if (skipRootStartElement) {
								continue;
							}
							if (rootAttributes != null) {
								// Collect or write attributes of schema element.
								if (noOutput) {
									// First call to this method collecting
									// schema attributes.
									Iterator<Attribute> iterator = startElement.getAttributes();
									while (iterator.hasNext()) {
										Attribute attribute = iterator.next();
										boolean add = true;
										for (Attribute attribute2 : rootAttributes) {
											if (XmlUtils.attributesEqual(attribute, attribute2)) {
												add = false;
											}
										}
										if (add) {
											rootAttributes.add(attribute);
										}
									}
									Iterator<Namespace> namespaceIterator = startElement.getNamespaces();
									while (namespaceIterator.hasNext()) {
										Namespace attribute = namespaceIterator.next();
										boolean add = true;
										for (Namespace attribute2 : rootNamespaceAttributes) {
											if (XmlUtils.attributesEqual(attribute, attribute2)) {
												add = false;
											}
										}
										if (add) {
											rootNamespaceAttributes.add(attribute);
										}
									}
								} else {
									// Second call to this method writing attributes from previous call.
									startElement = XmlUtils.EVENT_FACTORY.createStartElement(
											startElement.getName().getPrefix(),
											startElement.getName().getNamespaceURI(),
											startElement.getName().getLocalPart(),
											rootAttributes.iterator(),
											rootNamespaceAttributes.iterator(),
											startElement.getNamespaceContext());
								}
							}
							// Don't modify the reserved namespace http://www.w3.org/XML/1998/namespace which is by definition bound to the prefix xml (see http://www.w3.org/TR/xml-names/#ns-decl).
							if (xsd.isAddNamespaceToSchema() && !"http://www.w3.org/XML/1998/namespace".equals(xsd.getNamespace())) {
								event = XmlUtils.mergeAttributes(startElement,
										Arrays.asList(new AttributeEvent(TNS, xsd.getNamespace()), new AttributeEvent(ELFORMDEFAULT, "qualified")).iterator(),
										Collections.singletonList(XmlUtils.EVENT_FACTORY.createNamespace(xsd.getNamespace())).iterator(),
										XmlUtils.EVENT_FACTORY
									);
								if (!event.equals(startElement)) {
									Attribute tns = startElement.getAttributeByName(TNS);
									if (tns != null) {
										String s = tns.getValue();
										if (!s.equals(xsd.getNamespace())) {
											namespacesToCorrect.put(s, xsd.getNamespace());
										}
									}
								}
							} else {
								event = startElement;
							}
							if (imports != null && !noOutput) {
								// Second call to this method writing imports collected in previous call.
								// List contains start and end elements, hence add 2 on every iteration.
								for (int i = 0; i < imports.size(); i = i + 2) {
									boolean skip = false;
									for (int j = 0; j < i; j = j + 2) {
										Attribute attribute1 = imports.get(i).asStartElement().getAttributeByName(NAMESPACE);
										Attribute attribute2 = imports.get(j).asStartElement().getAttributeByName(NAMESPACE);
										if (attribute1 != null && attribute2 != null && attribute1.getValue().equals(attribute2.getValue())) {
											skip = true;
										}
									}
									if (!skip) {
										streamEventWriter.add(event);
										event = imports.get(i);
										streamEventWriter.add(event);
										event = imports.get(i + 1);
									}
								}
							}
						} else if (startElement.getName().equals(INCLUDE)) {
							continue;
//						} else if (startElement.getName().equals(REDEFINE)) {
//							continue;
						} else if (startElement.getName().equals(IMPORT)) {
							if (imports == null || noOutput) {
								// Not collecting or writing import elements.
								Attribute schemaLocation = startElement.getAttributeByName(SCHEMALOCATION);
								if (schemaLocation != null) {
									String location = schemaLocation.getValue();
									if (stripSchemaLocationFromImport) {
										List<Attribute> attributes = new ArrayList<>();
										Iterator<Attribute> iterator = startElement.getAttributes();
										while (iterator.hasNext()) {
											Attribute a = iterator.next();
											if (!SCHEMALOCATION.equals(a.getName())) {
												attributes.add(a);
											}
										}
										event = new StartElementEvent(
												startElement.getName(),
												attributes.iterator(),
												startElement.getNamespaces(),
												startElement.getNamespaceContext(),
												startElement.getLocation(),
												startElement.getSchemaType());
									} else {
										String relativeTo = xsd.getParentLocation();
										if (relativeTo.length() > 0 && location.startsWith(relativeTo)) {
											location = location.substring(relativeTo.length());
										}
										event = XMLStreamUtils.mergeAttributes(startElement, Collections.singletonList(new AttributeEvent(SCHEMALOCATION, location)).iterator(), XmlUtils.EVENT_FACTORY);
									}
								}
							}
							if (imports != null) {
								// Collecting or writing import elements.
								if (noOutput) {
									// First call to this method collecting
									// imports.
									imports.add(event);
								}
								continue;
							}
						}
						break;
					case XMLStreamConstants.END_ELEMENT:
						EndElement endElement = event.asEndElement();
						if (endElement.getName().equals(SCHEMA)) {
							if (skipRootEndElement) {
								continue;
							}
						} else if (endElement.getName().equals(INCLUDE)) {
							continue;
//						} else if (endElement.getName().equals(REDEFINE)) {
//							continue;
						} else if (imports != null) {
							if (endElement.getName().equals(IMPORT)) {
								if (noOutput) {
									imports.add(event);
								}
								continue;
							}
						}
						break;
					default:
						// simply copy
				}
				if (!noOutput) {
					streamEventWriter.add(event);
				}
			}
			streamEventWriter.flush();
		} catch (XMLStreamException e) {
			throw new ConfigurationException(xsd + " (" + event.getLocation() + ")", e);
		}
	}


	public static Reader toReader(javax.wsdl.Definition wsdlDefinition, javax.wsdl.extensions.schema.Schema wsdlSchema) throws javax.wsdl.WSDLException {
		return new StringReader(toString(wsdlDefinition, wsdlSchema));
	}

	public static String toString(javax.wsdl.Definition wsdlDefinition, javax.wsdl.extensions.schema.Schema wsdlSchema) throws javax.wsdl.WSDLException {
		StringWriter w = new StringWriter();
		PrintWriter res = new PrintWriter(w);
		/*
		 * Following block is synchronized to avoid a StackOverflowError (see
		 * https://issues.apache.org/jira/browse/AXIS2-4517)
		 */
		synchronized (wsdlDefinition) {
			com.ibm.wsdl.extensions.schema.SchemaSerializer schemaSerializer = new com.ibm.wsdl.extensions.schema.SchemaSerializer();
			schemaSerializer.marshall(Object.class, WSDL_SCHEMA, wsdlSchema, res, wsdlDefinition, wsdlDefinition.getExtensionRegistry());
		}
		return w.toString().trim();
	}

	public static void sortByDependencies(Set<IXSD> xsds, List<Schema> schemas) throws ConfigurationException {
		Set<IXSD> xsdsWithDependencies = new LinkedHashSet<>();
		for (IXSD xsd : xsds) {
			if (xsd.hasDependency(xsds)) {
				xsdsWithDependencies.add(xsd);
			} else {
				schemas.add(xsd);
			}
		}
		if (xsds.size() == xsdsWithDependencies.size()) {
			if (LOG.isDebugEnabled()) {
				StringBuilder message = new StringBuilder("Circular dependencies between schemas:");
				for (IXSD xsd : xsdsWithDependencies) {
					message.append(" [").append(xsd.toString()).append(" with target namespace ").append(xsd.getTargetNamespace()).append(" and imported namespaces ").append(xsd.getImportedNamespaces()).append("]");
				}
				LOG.debug(message.toString());
			}
			schemas.addAll(xsds);
			return;
		}
		if (!xsdsWithDependencies.isEmpty()) {
			sortByDependencies(xsdsWithDependencies, schemas);
		}
	}
}

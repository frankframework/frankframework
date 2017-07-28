/*
   Copyright 2013, 2015 Nationale-Nederlanden

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javanet.staxutils.XMLStreamEventWriter;
import javanet.staxutils.XMLStreamUtils;
import javanet.staxutils.events.AttributeEvent;
import javanet.staxutils.events.StartElementEvent;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.log4j.Logger;

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

	public static final nl.nn.javax.xml.namespace.QName WSDL_SCHEMA = new nl.nn.javax.xml.namespace.QName(XSD, "schema", "");

	public static Set<XSD> getXsdsRecursive(Set<XSD> xsds)
			throws ConfigurationException {
		return getXsdsRecursive(xsds, true);
	}
	
	public static Set<XSD> getXsdsRecursive(Set<XSD> xsds, boolean ignoreRedefine)
			throws ConfigurationException {
		Set<XSD> xsdsRecursive = new HashSet<XSD>();
		xsdsRecursive.addAll(xsds);
		for (XSD xsd : xsds) {
			xsdsRecursive.addAll(xsd.getXsdsRecursive(ignoreRedefine));
		}
		return xsdsRecursive;
	}

	public static Map<String, Set<XSD>> getXsdsGroupedByNamespace(Set<XSD> xsds,
			boolean sort) throws XMLStreamException, IOException {
		Map<String, Set<XSD>> result;
		if (sort) {
			result = new TreeMap<String, Set<XSD>>();
		} else {
			result = new HashMap<String, Set<XSD>>();
		}
		for (XSD xsd : xsds) {
			Set<XSD> set = result.get(xsd.getNamespace());
			if (set == null) {
				if (sort) {
					set = new TreeSet<XSD>();
				} else {
					set = new HashSet<XSD>();
				}
				result.put(xsd.getNamespace(), set);
			}
			set.add(xsd);
		}
		return result;
	}

	public static void
			mergeRootXsdsGroupedByNamespaceToSchemasWithIncludes(
			Map<String, Set<XSD>> rootXsdsGroupedByNamespace,
			XMLStreamWriter xmlStreamWriter)
			throws IOException, XMLStreamException {
		// As the root XSD's are written as includes there's no need to change
		// the imports and includes in the root XSD's.
		for (String namespace: rootXsdsGroupedByNamespace.keySet()) {
			xmlStreamWriter.writeStartElement(XSD, "schema");
			xmlStreamWriter.writeAttribute("targetNamespace", namespace);
			for (XSD xsd : rootXsdsGroupedByNamespace.get(namespace)) {
				xmlStreamWriter.writeEmptyElement(XSD, "include");
				xmlStreamWriter.writeAttribute("schemaLocation",
						xsd.getResourceTarget());
			}
			xmlStreamWriter.writeEndElement();
		}
	}

	
	/**
	 * @return XSD's when xmlStreamWriter is null, otherwise write to
	 *		 xmlStreamWriter
	 */
	public static Set<XSD> mergeXsdsGroupedByNamespaceToSchemasWithoutIncludes(
			ClassLoader classLoader, Map<String,
			Set<XSD>> xsdsGroupedByNamespace, XMLStreamWriter xmlStreamWriter)
			throws XMLStreamException, IOException, ConfigurationException {
		Set<XSD> resultXsds = new HashSet<XSD>();
		for (String namespace: xsdsGroupedByNamespace.keySet()) {
			Set<XSD> xsds = xsdsGroupedByNamespace.get(namespace);
			// Get attributes of root elements and get import elements from all XSD's
			List<Attribute> rootAttributes = new ArrayList<Attribute>();
			List<Attribute> rootNamespaceAttributes = new ArrayList<Attribute>();
			List<XMLEvent> imports = new ArrayList<XMLEvent>();
			for (XSD xsd: xsds) {
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				XMLStreamWriter w = XmlUtils.REPAIR_NAMESPACES_OUTPUT_FACTORY.createXMLStreamWriter(byteArrayOutputStream, XmlUtils.STREAM_FACTORY_ENCODING);
				xsdToXmlStreamWriter(xsd, w, false, true, false, false,
						rootAttributes, rootNamespaceAttributes, imports, true);
			}
			// Write XSD's with merged root element
			XSD resultXsd = null;
			XMLStreamWriter w;
			if (xmlStreamWriter == null) {
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				resultXsd = new XSD();
				resultXsd.setClassLoader(classLoader);
				resultXsd.setNamespace(namespace);
				resultXsd.setByteArrayOutputStream(byteArrayOutputStream);
				resultXsd.setSourceXsds(xsds);
				w = XmlUtils.REPAIR_NAMESPACES_OUTPUT_FACTORY.createXMLStreamWriter(byteArrayOutputStream, XmlUtils.STREAM_FACTORY_ENCODING);
			} else {
				w = xmlStreamWriter;
			}
			int i = 0;
			for (XSD xsd: xsds) {
				i++;
				boolean skipFirstElement = true;
				boolean skipLastElement = true;
				if (xsds.size() == 1) {
					skipFirstElement = false;
					skipLastElement = false;
				} else {
					if (i == 1) {
						skipFirstElement = false;
					} else if (i == xsds.size()) {
						skipLastElement = false;
					}
				}
				xsdToXmlStreamWriter(xsd, w, false, true,
						skipFirstElement, skipLastElement, rootAttributes,
						rootNamespaceAttributes, imports, false);
			}
			if (resultXsd != null) {
				XSD firstXsd = xsds.iterator().next();
				resultXsd.setImportedSchemaLocationsToIgnore(firstXsd.getImportedSchemaLocationsToIgnore());
				resultXsd.setUseBaseImportedSchemaLocationsToIgnore(firstXsd.isUseBaseImportedSchemaLocationsToIgnore());
				resultXsd.setImportedNamespacesToIgnore(firstXsd.getImportedNamespacesToIgnore());
				resultXsd.init();
				resultXsds.add(resultXsd);
			}
		}
		return resultXsds;
	}

	public static void xsdToXmlStreamWriter(final XSD xsd,
			XMLStreamWriter xmlStreamWriter)
			throws IOException, ConfigurationException {
		xsdToXmlStreamWriter(xsd, xmlStreamWriter, true, false, false, false,
				null, null, null, false);
	}

	/**
	 * Including a {@link nl.nn.adapterframework.validation.XSD} into an
	 * {@link javax.xml.stream.XMLStreamWriter} while parsing it. It is parsed
	 * (using a low level {@link javax.xml.stream.XMLEventReader} so that
	 * certain things can be corrected on the fly.
	 * @param xsd
	 * @param xmlStreamWriter
	 * @param standalone
	 * When standalone the start and end document contants are ignored, hence
	 * the xml declaration is ignored.
	 * @param stripSchemaLocationFromImport
	 * Useful when generating a WSDL which should contain all XSD's inline
	 * (without includes or imports). The XSD might have an import with
	 * schemaLocation to make it valid on it's own, when
	 * stripSchemaLocationFromImport is true it will be removed.
	 * @throws java.io.IOException
	 * @throws javax.xml.stream.XMLStreamException
	 */
	public static void xsdToXmlStreamWriter(final XSD xsd,
			XMLStreamWriter xmlStreamWriter, boolean standalone,
			boolean stripSchemaLocationFromImport, boolean skipRootStartElement,
			boolean skipRootEndElement, List<Attribute> rootAttributes,
			List<Attribute> rootNamespaceAttributes, List<XMLEvent> imports,
			boolean noOutput) throws IOException, ConfigurationException {
		Map<String, String> namespacesToCorrect = new HashMap<String, String>();
		NamespaceCorrectingXMLStreamWriter namespaceCorrectingXMLStreamWriter =
				new NamespaceCorrectingXMLStreamWriter(xmlStreamWriter,
						namespacesToCorrect);
		final XMLStreamEventWriter streamEventWriter = new XMLStreamEventWriter(
				namespaceCorrectingXMLStreamWriter);
		InputStream in = null;
		in = xsd.getInputStream();
		if (in == null) {
			throw new IllegalStateException("" + xsd + " not found");
		}
		XMLEvent event = null;
		try {
			XMLEventReader er =
					XmlUtils.INPUT_FACTORY.createXMLEventReader(
							in, XmlUtils.STREAM_FACTORY_ENCODING);
			while (er.hasNext()) {
				event = er.nextEvent();
				switch (event.getEventType()) {
					case XMLStreamConstants.START_DOCUMENT:
					case XMLStreamConstants.END_DOCUMENT:
						if (! standalone) {
							continue;
						}
						// fall through
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
									iterator = startElement.getNamespaces();
									while (iterator.hasNext()) {
										Attribute attribute = iterator.next();
										boolean add = true;
										for (Attribute attribute2 : rootNamespaceAttributes) {
											if (XmlUtils.attributesEqual(attribute, attribute2)) {
												add = false;
											}
										}
										if (add) {
											rootNamespaceAttributes.add(attribute);
										}
									}
								} else {
									// Second call to this method writing attributes
									// from previous call.
									startElement = XmlUtils.EVENT_FACTORY.createStartElement(
											startElement.getName().getPrefix(),
											startElement.getName().getNamespaceURI(),
											startElement.getName().getLocalPart(),
											rootAttributes.iterator(),
											rootNamespaceAttributes.iterator(),
											startElement.getNamespaceContext());
								}
							}
							// Don't modify the reserved namespace
							// http://www.w3.org/XML/1998/namespace
							// which is by definition bound to the prefix xml
							// (see http://www.w3.org/TR/xml-names/#ns-decl).
							if (xsd.isAddNamespaceToSchema()
									&& !xsd.getNamespace().equals("http://www.w3.org/XML/1998/namespace")
									) {
								event = XmlUtils.mergeAttributes(startElement,
										Arrays.asList(
											new AttributeEvent(TNS, xsd.getNamespace()),
											new AttributeEvent(ELFORMDEFAULT, "qualified")
										).iterator(),
										Arrays.asList(
											XmlUtils.EVENT_FACTORY.createNamespace(xsd.getNamespace())
										).iterator(),
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
								// Second call to this method writing imports
								// collected in previous call.
								// List contains start and end elements, hence add
								// 2 on every iteration.
								for (int i = 0; i < imports.size(); i = i + 2) {
									boolean skip = false;
									for (int j = 0; j < i; j = j + 2) {
										Attribute attribute1 =
												imports.get(i).asStartElement().getAttributeByName(NAMESPACE);
										Attribute attribute2 =
												imports.get(j).asStartElement().getAttributeByName(NAMESPACE);
										if (attribute1 != null && attribute2 != null
												&& attribute1.getValue().equals(attribute2.getValue())) {
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
										List<Attribute> attributes = new ArrayList<Attribute>();
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
										event =
											XMLStreamUtils.mergeAttributes(startElement,
												Collections.singletonList(new AttributeEvent(SCHEMALOCATION, location)).iterator(), XmlUtils.EVENT_FACTORY);
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
			throw new ConfigurationException(
					xsd.toString() + " (" + event.getLocation() + "): "
					+ e.getMessage(), e);
		}
	}


	public static InputStream toInputStream(
			nl.nn.javax.wsdl.Definition wsdlDefinition,
			nl.nn.javax.wsdl.extensions.schema.Schema wsdlSchema
			) throws nl.nn.javax.wsdl.WSDLException,
			UnsupportedEncodingException {
		return new ByteArrayInputStream(
				toString(wsdlDefinition, wsdlSchema).getBytes("UTF-8"));
	}

	public static String toString(
			nl.nn.javax.wsdl.Definition wsdlDefinition,
			nl.nn.javax.wsdl.extensions.schema.Schema wsdlSchema
			) throws nl.nn.javax.wsdl.WSDLException {
		StringWriter w = new StringWriter();
		PrintWriter res = new PrintWriter(w);
		/*
		 * Following block is synchronized to avoid a StackOverflowError (see
		 * https://issues.apache.org/jira/browse/AXIS2-4517)
		 */
		synchronized (wsdlDefinition) {
			nl.nn.com.ibm.wsdl.extensions.schema.SchemaSerializer schemaSerializer =
					new nl.nn.com.ibm.wsdl.extensions.schema.SchemaSerializer();
			schemaSerializer.marshall(Object.class, WSDL_SCHEMA, wsdlSchema, res,
					wsdlDefinition, wsdlDefinition.getExtensionRegistry());
		}
		return w.toString().trim();
	}

	public static void sortByDependencies(Set<XSD> xsds, List<Schema> schemas) throws ConfigurationException {
		Set<XSD> xsdsWithDependencies = new HashSet<XSD>();
		Iterator<XSD> iterator = xsds.iterator();
		while (iterator.hasNext()) {
			XSD xsd = iterator.next();
			if (xsd.hasDependency(xsds)) {
				xsdsWithDependencies.add(xsd);
			} else {
				schemas.add((Schema)xsd);
			}
		}
		if (xsds.size() == xsdsWithDependencies.size()) {
			String message = "Circural dependencies between schemas:";
			for (XSD xsd : xsdsWithDependencies) {
				message = message + " [" + xsd.toString() + " with target namespace " + xsd.getTargetNamespace() + " and imported namespaces " + xsd.getImportedNamespaces() + "]";
			}
			throw new ConfigurationException(message);
		}
		if (xsdsWithDependencies.size() > 0) {
			sortByDependencies(xsdsWithDependencies, schemas);
		}
	}
}

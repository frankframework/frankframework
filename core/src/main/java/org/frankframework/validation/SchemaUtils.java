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

import jakarta.annotation.Nonnull;

import javanet.staxutils.XMLStreamEventWriter;
import javanet.staxutils.XMLStreamUtils;
import javanet.staxutils.events.AttributeEvent;
import javanet.staxutils.events.StartElementEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.configuration.SuppressKeys;
import org.frankframework.core.IConfigurationAware;
import org.frankframework.core.IScopeProvider;
import org.frankframework.util.XmlUtils;
import org.frankframework.validation.xsd.StringXsd;

/**
 * @author Michiel Meeuwissen
 * @author Jaco de Groot
 */
public class SchemaUtils {
	protected static final Logger LOG = LogManager.getLogger(SchemaUtils.class);

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
		// As the root XSDs are written as includes there's no need to change the imports and includes in the root XSDs.
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
				collectImportsAndAttributes(xsd, rootAttributes, rootNamespaceAttributes, imports);
			}
			// Write XSDs with merged root element
			XMLStreamWriter w;
			StringWriter schemaContentsWriter;
			if (xmlStreamWriter == null) {
				schemaContentsWriter = new StringWriter();
				w = XmlUtils.REPAIR_NAMESPACES_OUTPUT_FACTORY.createXMLStreamWriter(schemaContentsWriter);
			} else {
				schemaContentsWriter = null;
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
				xsdToXmlStreamWriter(xsd, w, skipRootElementStart, skipRootElementEnd, rootAttributes, rootNamespaceAttributes, imports);
			}
			// TODO: After creating merged XSD, while we still know what the source files are, we should now validate for duplicate elements
			if (xmlStreamWriter == null) {
				w.close();
				StringXsd resultXsd = new StringXsd(schemaContentsWriter.toString());
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

	public static void writeStandaloneXsd(final @Nonnull IXSD xsd, @Nonnull XMLStreamWriter xmlStreamWriter) throws IOException, ConfigurationException {
		final Map<String, String> namespacesToCorrect = new HashMap<>();
		final NamespaceCorrectingXMLStreamWriter namespaceCorrectingXMLStreamWriter = new NamespaceCorrectingXMLStreamWriter(xmlStreamWriter, namespacesToCorrect);
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
					case XMLStreamConstants.START_ELEMENT:
						StartElement startElement = event.asStartElement();
						if (startElement.getName().equals(SCHEMA)) {
							event = fixupSchemaStartEvent(xsd, startElement, namespacesToCorrect);
						} else if (startElement.getName().equals(IMPORT)) {
							event = fixupImportStartElement(xsd, startElement);
						} else if (startElement.getName().equals(INCLUDE)) {
							// Don't output the includes
							break;
						}
						streamEventWriter.add(event);
						break;
					case XMLStreamConstants.END_ELEMENT:
						EndElement endElement = event.asEndElement();
						// Don't output the includes
						if (!endElement.getName().equals(INCLUDE)) {
							streamEventWriter.add(event);
						}
						break;
					default:
						// simply copy
						streamEventWriter.add(event);
				}
			}
			streamEventWriter.flush();
		} catch (XMLStreamException e) {
			throw new ConfigurationException(xsd + " (" + (event != null ? event.getLocation() : "<no location>") + ")", e);
		}
	}

	private static XMLEvent fixupImportStartElement(@Nonnull IXSD xsd, @Nonnull StartElement startElement) {
		Attribute schemaLocation = startElement.getAttributeByName(SCHEMALOCATION);
		if (schemaLocation == null) {
			return startElement;
		}
		String location = schemaLocation.getValue();
		String relativeTo = xsd.getParentLocation();
		if (!relativeTo.isEmpty() && location.startsWith(relativeTo)) {
			location = location.substring(relativeTo.length());
		}
		return XMLStreamUtils.mergeAttributes(startElement, List.of(new AttributeEvent(SCHEMALOCATION, location)).iterator(), XmlUtils.EVENT_FACTORY);
	}

	private static void collectImportsAndAttributes(final @Nonnull IXSD xsd, @Nonnull List<Attribute> rootAttributes, @Nonnull List<Namespace> rootNamespaceAttributes, @Nonnull List<XMLEvent> imports) throws IOException, ConfigurationException {
		XMLEvent event = null;
		try (Reader reader = xsd.getReader()) {
			if (reader == null) {
				throw new IllegalStateException(xsd + " not found");
			}
			XMLEventReader er = XmlUtils.INPUT_FACTORY.createXMLEventReader(reader);
			while (er.hasNext()) {
				event = er.nextEvent();
				switch (event.getEventType()) {
					case XMLStreamConstants.START_ELEMENT:
						StartElement startElement = event.asStartElement();
						if (startElement.getName().equals(SCHEMA)) {
							// Collect or write attributes of schema element.
							Iterator<Attribute> iterator = startElement.getAttributes();
							addUniqueAttributes(rootAttributes, iterator);

							Iterator<Namespace> namespaceIterator = startElement.getNamespaces();
							addUniqueAttributes(rootNamespaceAttributes, namespaceIterator);
						} else if (startElement.getName().equals(IMPORT)) {
							// Collecting import elements.
							addImportStartElement(imports, startElement);
						}
						break;
					case XMLStreamConstants.END_ELEMENT:
						EndElement endElement = event.asEndElement();
						if (endElement.getName().equals(IMPORT)) {
							imports.add(event);
						}
						break;
					default:
						// No-op
				}
			}
		} catch (XMLStreamException e) {
			throw new ConfigurationException(xsd + " (" + (event != null ? event.getLocation() : "<no location>") + ")", e);
		}
	}

	private static void addImportStartElement(@Nonnull List<XMLEvent> imports, StartElement startElement) {
		Attribute schemaLocation = startElement.getAttributeByName(SCHEMALOCATION);
		if (schemaLocation != null) {
			List<Attribute> attributes = new ArrayList<>();
			Iterator<Attribute> iterator = startElement.getAttributes();
			while (iterator.hasNext()) {
					Attribute a = iterator.next();
					if (!SCHEMALOCATION.equals(a.getName())) {
						attributes.add(a);
					}
				}
			StartElementEvent startElementEvent = new StartElementEvent(
						startElement.getName(),
						attributes.iterator(),
						startElement.getNamespaces(),
						startElement.getNamespaceContext(),
						startElement.getLocation(),
						startElement.getSchemaType());
			imports.add(startElementEvent);
		} else {
			imports.add(startElement);
		}
	}

	private static <T extends Attribute> void addUniqueAttributes(@Nonnull List<T> rootAttributes, Iterator<T> iterator) {
		while (iterator.hasNext()) {
			T attribute = iterator.next();
			for (Attribute attribute2 : rootAttributes) {
				if (XmlUtils.attributesEqual(attribute, attribute2)) {
					return;
				}
			}
			rootAttributes.add(attribute);
		}
	}

	/**
	 * Internal method used for XSD merging and rewriting. The method has two modes: one to collect information from
	 * all XSDs that need to be merged, and one to produce parts of the merged XSD.
	 * <p>
	 * Including a {@link IXSD} into an {@link XMLStreamWriter} while parsing it. It is parsed
	 * (using a low level {@link XMLEventReader}) so that certain things can be corrected on the fly.
	 * </p>
	 *
	 * @param xsd                     The XSD to write to the {@link XMLStreamWriter}.
	 * @param xmlStreamWriter         The {@link XMLStreamWriter} to use for output. If {@code null}, then only parse the XSD collect information about imports and attributes.
	 * @param skipRootStartElement    When writing merged XSDs, only when writing the first XSD the root start element should be included.
	 * @param skipRootEndElement      When writing merged XSDs, only when writing the last XSD the root start element should be included.
	 * @param rootAttributes          List to which to collect information on attributes on the XSD root element, or from which to write the XSD root element attributes, depending on the mode.
	 * @param rootNamespaceAttributes List to which to collect root namespace attributes, or from which to write the root namespace attributes, depending on the mode.
	 * @param imports                 List to which to collect XSD imports, or from which to write all XSD imports, depending on the mode.
	 */
	private static void xsdToXmlStreamWriter(final @Nonnull IXSD xsd, @Nonnull XMLStreamWriter xmlStreamWriter, boolean skipRootStartElement, boolean skipRootEndElement, @Nonnull List<Attribute> rootAttributes, @Nonnull List<Namespace> rootNamespaceAttributes, @Nonnull List<XMLEvent> imports) throws IOException, ConfigurationException {
		final Map<String, String> namespacesToCorrect = new HashMap<>();
		final NamespaceCorrectingXMLStreamWriter namespaceCorrectingXMLStreamWriter = new NamespaceCorrectingXMLStreamWriter(xmlStreamWriter, namespacesToCorrect);
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
					case XMLStreamConstants.START_DOCUMENT,
						 XMLStreamConstants.END_DOCUMENT:
						// Don't output
						continue;
					case XMLStreamConstants.START_ELEMENT:
						StartElement startElement = event.asStartElement();
						if (SCHEMA.equals(startElement.getName())) {
							if (skipRootStartElement) {
								// Don't output
								continue;
							}
							// Second call to this method writing attributes from previous call.
							writeXsdRootStartElement(xsd, rootAttributes, rootNamespaceAttributes, startElement, streamEventWriter, namespacesToCorrect);

							// Second call to this method writing imports collected in previous call.
							writeImports(imports, streamEventWriter);
						} else if (!startElement.getName().equals(INCLUDE) && !startElement.getName().equals(IMPORT)) {
							streamEventWriter.add(event);
						}
						break;
					case XMLStreamConstants.END_ELEMENT:
						EndElement endElement = event.asEndElement();
						if (endElement.getName().equals(SCHEMA)) {
							if (skipRootEndElement) {
								break;
							}
							streamEventWriter.add(event);
						} else if (!endElement.getName().equals(INCLUDE) && !endElement.getName().equals(IMPORT)) {
							streamEventWriter.add(event);
						}
						break;
					default:
						// simply copy
						streamEventWriter.add(event);
				}
			}
			streamEventWriter.flush();
		} catch (XMLStreamException e) {
			throw new ConfigurationException(xsd + " (" + (event != null ? event.getLocation() : "<no location>") + ")", e);
		}
	}

	private static void writeImports(@Nonnull List<XMLEvent> imports, XMLStreamEventWriter streamEventWriter) throws XMLStreamException {
		// List contains start and end elements, hence add 2 on every iteration.
		for (int i = 0; i < imports.size(); i = i + 2) {
			boolean skip = false;
			for (int j = 0; j < i; j = j + 2) {
				Attribute attribute1 = imports.get(i).asStartElement().getAttributeByName(NAMESPACE);
				Attribute attribute2 = imports.get(j).asStartElement().getAttributeByName(NAMESPACE);
				if (attribute1 != null && attribute2 != null && attribute1.getValue().equals(attribute2.getValue())) {
					skip = true;
					break;
				}
			}
			if (!skip) {
				XMLEvent importStart = imports.get(i);
				streamEventWriter.add(importStart);
				XMLEvent importEnd = imports.get(i + 1);
				streamEventWriter.add(importEnd);
			}
		}
	}

	private static void writeXsdRootStartElement(@Nonnull IXSD xsd, @Nonnull List<Attribute> rootAttributes, @Nonnull List<Namespace> rootNamespaceAttributes, StartElement startElement, XMLStreamEventWriter streamEventWriter, Map<String, String> namespacesToCorrect) throws XMLStreamException {
		StartElement actualStartElement = XmlUtils.EVENT_FACTORY.createStartElement(
				startElement.getName().getPrefix(),
				startElement.getName().getNamespaceURI(),
				startElement.getName().getLocalPart(),
				rootAttributes.iterator(),
				rootNamespaceAttributes.iterator(),
				startElement.getNamespaceContext());
		streamEventWriter.add(fixupSchemaStartEvent(xsd, actualStartElement, namespacesToCorrect));
	}

	private static @Nonnull XMLEvent fixupSchemaStartEvent(@Nonnull IXSD xsd, @Nonnull StartElement originalStartElement, @Nonnull Map<String, String> namespacesToCorrect) {
		// Don't modify the reserved namespace http://www.w3.org/XML/1998/namespace which is by definition bound to the prefix xml (see http://www.w3.org/TR/xml-names/#ns-decl).
		if (!xsd.isAddNamespaceToSchema() || "http://www.w3.org/XML/1998/namespace".equals(xsd.getNamespace())) {
			return originalStartElement;
		}
		XMLEvent event = XmlUtils.mergeAttributes(
				originalStartElement,
				List.of(new AttributeEvent(TNS, xsd.getNamespace()), new AttributeEvent(ELFORMDEFAULT, "qualified")).iterator(),
				List.of(XmlUtils.EVENT_FACTORY.createNamespace(xsd.getNamespace())).iterator(),
				XmlUtils.EVENT_FACTORY
			);
		if (!event.equals(originalStartElement)) {
			Attribute tns = originalStartElement.getAttributeByName(TNS);
			if (tns != null) {
				String s = tns.getValue();
				if (!s.equals(xsd.getNamespace())) {
					namespacesToCorrect.put(s, xsd.getNamespace());
				}
			}
		}
		return event;
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

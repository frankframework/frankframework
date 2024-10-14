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

import com.google.common.collect.Streams;

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
	 * Write merged XSDs to xmlStreamWriter.
	 *
	 */
	public static void mergeXsdsGroupedByNamespaceToSchemasWithoutIncludes(@Nonnull IScopeProvider scopeProvider, @Nonnull Map<String, Set<IXSD>> xsdsGroupedByNamespace, @Nonnull XMLStreamWriter xmlStreamWriter) throws IOException, ConfigurationException {
		// iterate over the namespaces
		for (Map.Entry<String, Set<IXSD>> entry: xsdsGroupedByNamespace.entrySet()) {
			String namespace = entry.getKey();
			Set<IXSD> xsds = entry.getValue();

			// Write XSDs with merged root element to the provided xmlStreamWriter
			mergeXsds(scopeProvider, xsds, namespace, xmlStreamWriter);
		}
	}

	/**
	 * Returns merged XSDs.
	 *
	 * @return merged XSDs
	 */
	public static @Nonnull Set<IXSD> mergeXsdsGroupedByNamespaceToSchemasWithoutIncludes(IScopeProvider scopeProvider, Map<String, Set<IXSD>> xsdsGroupedByNamespace) throws XMLStreamException, IOException, ConfigurationException {
		Set<IXSD> resultXsds = new LinkedHashSet<>();
		// iterate over the namespaces
		for (Map.Entry<String, Set<IXSD>> entry: xsdsGroupedByNamespace.entrySet()) {
			String namespace = entry.getKey();
			Set<IXSD> xsds = entry.getValue();

			// Write XSDs with merged root element to new XMLStreamWriter
			StringWriter contents = new StringWriter();
			XMLStreamWriter xmlStreamWriter = XmlUtils.REPAIR_NAMESPACES_OUTPUT_FACTORY.createXMLStreamWriter(contents);

			mergeXsds(scopeProvider, xsds, namespace, xmlStreamWriter);

			xmlStreamWriter.close();
			StringXsd resultXsd = new StringXsd(contents.toString());
			IXSD firstXsd = xsds.iterator().next();
			resultXsd.setImportedSchemaLocationsToIgnore(firstXsd.getImportedSchemaLocationsToIgnore());
			resultXsd.setUseBaseImportedSchemaLocationsToIgnore(firstXsd.isUseBaseImportedSchemaLocationsToIgnore());
			resultXsd.setImportedNamespacesToIgnore(firstXsd.getImportedNamespacesToIgnore());
			resultXsd.initFromXsds(namespace, scopeProvider, xsds);

			resultXsds.add(resultXsd);
		}
		return resultXsds;
	}

	private static void mergeXsds(IScopeProvider scopeProvider, Set<IXSD> xsds, String namespace, XMLStreamWriter xmlStreamWriter) throws IOException, ConfigurationException {
		if (xsds.isEmpty()) {
			return;
		}
		addXsdMergeWarnings(scopeProvider, xsds, namespace);

		// Get attributes of root elements and get import elements from all XSDs
		List<Attribute> rootAttributes = new ArrayList<>();
		List<Namespace> rootNamespaceAttributes = new ArrayList<>();
		Set<StartElementWrapper> imports = new LinkedHashSet<>();
		String xsdPrefix = null;
		IXSD firstXsd = null;
		for (IXSD xsd: xsds) {
			if (firstXsd == null) {
				firstXsd = xsd;
			}
			xsdPrefix = collectImportsAndSchemaRootAttributes(xsd, rootAttributes, rootNamespaceAttributes, imports);
		}
		if (firstXsd == null || xsdPrefix == null) {
			throw new IllegalStateException("Must pass at least one xsd, should have been checked already");
		}
		// TODO: After collecting all imports, while we still know what the source files are, we should now validate for duplicate elements

		final Map<String, String> namespacesToCorrect = new HashMap<>();
		final NamespaceCorrectingXMLStreamWriter namespaceCorrectingXMLStreamWriter = new NamespaceCorrectingXMLStreamWriter(xmlStreamWriter, namespacesToCorrect);
		final XMLStreamEventWriter streamEventWriter = new XMLStreamEventWriter(namespaceCorrectingXMLStreamWriter);
		try {
			writeXsdRootStartElement(firstXsd, xsdPrefix, rootAttributes, rootNamespaceAttributes, streamEventWriter, namespacesToCorrect);
			writeImports(imports, streamEventWriter);
		} catch (XMLStreamException e) {
			throw new ConfigurationException("Cannot create XSD", e);
		}
		// perform the merge
		for (IXSD xsd: xsds) {
			xsdToXmlStreamWriter(xsd, streamEventWriter);
		}
		try {
			writeXsdRootEndElement(xsdPrefix, rootNamespaceAttributes, streamEventWriter);
			streamEventWriter.flush();
		} catch (XMLStreamException e) {
			throw new ConfigurationException("Cannot finish the XSD", e);
		}
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

	private static @Nonnull Reader createXsdReader(@Nonnull IXSD xsd) throws IOException {
		Reader reader = xsd.getReader();
		if (reader == null) {
			throw new IllegalArgumentException(xsd + " not found");
		}
		return reader;
	}

	/**
	 * Write the XSD out to the XMLStreamWriter as a standalone XSD.
	 *
	 * @param xsd XSD to be written
	 * @param xmlStreamWriter Target XMLStreamWriter
	 * @throws IOException When reading or writing fails
	 * @throws ConfigurationException When there was an exception in the XML
	 */
	public static void writeStandaloneXsd(final @Nonnull IXSD xsd, @Nonnull XMLStreamWriter xmlStreamWriter) throws IOException, ConfigurationException {
		final Map<String, String> namespacesToCorrect = new HashMap<>();
		final NamespaceCorrectingXMLStreamWriter namespaceCorrectingXMLStreamWriter = new NamespaceCorrectingXMLStreamWriter(xmlStreamWriter, namespacesToCorrect);
		final XMLStreamEventWriter streamEventWriter = new XMLStreamEventWriter(namespaceCorrectingXMLStreamWriter);
		XMLEvent event = null;
		try (Reader reader = createXsdReader(xsd)) {
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

	private record StartElementWrapper(StartElement startElement) {

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof StartElementWrapper other)) {
				return false;
			}
			Attribute attribute1 = this.startElement.getAttributeByName(NAMESPACE);
			Attribute attribute2 = other.startElement.getAttributeByName(NAMESPACE);
			return XmlUtils.attributesEqual(attribute1, attribute2);
		}

		@Override
		public int hashCode() {
			Attribute nsAttribute = this.startElement.getAttributeByName(NAMESPACE);
			if (nsAttribute == null) return 0;
			return nsAttribute.getName().hashCode() +
					31 * nsAttribute.getValue().hashCode();
		}
	}

	/**
	 * Internal method to collect information needed when merging XSDs: XSD Imports, Schema root attributes, and
	 * Namespace declarations on the Schema root element.
	 *
	 * @param xsd An XSD from which to collect information
	 * @param rootAttributes List to collect Schema root attributes into
	 * @param rootNamespaceAttributes List to collect Schema root namespaces into
	 * @param imports Set to collect all XSD imports into
	 * @throws IOException Thrown when there was an exception reading or writing the XSD
	 * @throws ConfigurationException Thrown when there is an XML parsing or writing error
	 */
	private static String collectImportsAndSchemaRootAttributes(final @Nonnull IXSD xsd, @Nonnull List<Attribute> rootAttributes, @Nonnull List<Namespace> rootNamespaceAttributes, @Nonnull Set<StartElementWrapper> imports) throws IOException, ConfigurationException {
		XMLEvent event = null;
		String xsdPrefix = null;
		try (Reader reader = createXsdReader(xsd)) {
			XMLEventReader er = XmlUtils.INPUT_FACTORY.createXMLEventReader(reader);
			while (er.hasNext()) {
				event = er.nextEvent();
				// No-op
				if (event.getEventType() == XMLStreamConstants.START_ELEMENT) {
					StartElement startElement = event.asStartElement();
					if (startElement.getName().equals(SCHEMA)) {
						xsdPrefix = startElement.getName().getPrefix();
						// Collect or write attributes of schema element.
						addUniqueAttributes(rootAttributes, startElement.getAttributes());
						addUniqueAttributes(rootNamespaceAttributes, startElement.getNamespaces());
					} else if (startElement.getName().equals(IMPORT)) {
						// Collecting import elements.
						imports.add(new StartElementWrapper(stripSchemaLocation(startElement)));
					}
				}
			}
			return xsdPrefix;
		} catch (XMLStreamException e) {
			throw new ConfigurationException(xsd + " (" + (event != null ? event.getLocation() : "<no location>") + ")", e);
		}
	}

	private static StartElement stripSchemaLocation(@Nonnull StartElement startElement) {
		Attribute schemaLocation = startElement.getAttributeByName(SCHEMALOCATION);
		if (schemaLocation == null) {
			return startElement;
		}
		// Recreate the startElement but now without the schemaLocation attribute

		// With a stream from the iterator we can lazily iterate over the elements while filtering.
		Iterator<Attribute> filteredAttributes = Streams.stream(startElement.getAttributes())
				.filter(a -> !a.getName().equals(SCHEMALOCATION))
				.iterator();
		return new StartElementEvent(
				startElement.getName(),
				filteredAttributes,
				startElement.getNamespaces(),
				startElement.getNamespaceContext(),
				startElement.getLocation(),
				startElement.getSchemaType()
		);
	}

	private static <T extends Attribute> void addUniqueAttributes(@Nonnull List<T> collectedAttributes, Iterator<T> newAttributes) {
		newAttributes.forEachRemaining(attribute -> {
			if (doesNotContain(collectedAttributes, attribute)) {
				collectedAttributes.add(attribute);
			}
		});
	}

	private static <T extends Attribute> boolean doesNotContain(@Nonnull List<T> collectedAttributes, @Nonnull T attribute) {
		return collectedAttributes
				.stream()
				.noneMatch(attribute2 -> XmlUtils.attributesEqual(attribute, attribute2));
	}

	/**
	 * Internal method used for XSD merging.
	 * <p>
	 * Including a {@link IXSD} into an {@link XMLStreamEventWriter} while parsing it. It is parsed
	 * (using a low level {@link XMLEventReader}) so that certain things can be corrected on the fly.
	 * </p>
	 *
	 * @param xsd               The XSD to write to the {@link XMLStreamEventWriter}.
	 * @param streamEventWriter The {@link XMLStreamEventWriter} to which the merged XSD is written.
	 */
	private static void xsdToXmlStreamWriter(final @Nonnull IXSD xsd, @Nonnull XMLStreamEventWriter streamEventWriter) throws IOException, ConfigurationException {
		XMLEvent event = null;
		try (Reader reader = createXsdReader(xsd)) {
			XMLEventReader er = XmlUtils.INPUT_FACTORY.createXMLEventReader(reader);
			while (er.hasNext()) {
				event = er.nextEvent();
				switch (event.getEventType()) {
					case XMLStreamConstants.START_DOCUMENT,
						 XMLStreamConstants.END_DOCUMENT:
						// Don't output
						break;
					case XMLStreamConstants.START_ELEMENT,
						 XMLStreamConstants.END_ELEMENT:
						QName elementName = getQName(event);
						if (!elementName.equals(SCHEMA) && !elementName.equals(INCLUDE) && !elementName.equals(IMPORT)) {
							streamEventWriter.add(event);
						}
						break;
					default:
						// simply copy
						streamEventWriter.add(event);
				}
			}
		} catch (XMLStreamException e) {
			throw new ConfigurationException(xsd + " (" + (event != null ? event.getLocation() : "<no location>") + ")", e);
		}
	}

	private static QName getQName(XMLEvent event) {
		if (event.isStartElement()) {
			return event.asStartElement().getName();
		} else if (event.isEndElement()) {
			return event.asEndElement().getName();
		} else {
			throw new IllegalArgumentException("Unexpected event type: [" + event + "]; expected start or end element");
		}
	}

	private static void writeImports(@Nonnull Set<StartElementWrapper> imports, XMLStreamEventWriter streamEventWriter) throws XMLStreamException {
		for (StartElementWrapper xsdImport : imports) {
			streamEventWriter.add(xsdImport.startElement);
			streamEventWriter.add(XmlUtils.EVENT_FACTORY.createEndElement(xsdImport.startElement.getName(), xsdImport.startElement.getNamespaces()));
		}
	}

	private static void writeXsdRootStartElement(@Nonnull IXSD xsd, @Nonnull String xsdPrefix, @Nonnull List<Attribute> rootAttributes, @Nonnull List<Namespace> rootNamespaceAttributes, XMLStreamEventWriter streamEventWriter, Map<String, String> namespacesToCorrect) throws XMLStreamException {
		StartElement startElement = XmlUtils.EVENT_FACTORY.createStartElement(
				xsdPrefix,
				SCHEMA.getNamespaceURI(),
				SCHEMA.getLocalPart(),
				rootAttributes.iterator(),
				rootNamespaceAttributes.iterator());
		streamEventWriter.add(fixupSchemaStartEvent(xsd, startElement, namespacesToCorrect));
	}

	private static void writeXsdRootEndElement(@Nonnull String xsdPrefix, @Nonnull List<Namespace> rootNamespaceAttributes, XMLStreamEventWriter streamEventWriter) throws XMLStreamException {
		EndElement endElement = XmlUtils.EVENT_FACTORY.createEndElement(
				xsdPrefix,
				SCHEMA.getNamespaceURI(),
				SCHEMA.getLocalPart(),
				rootNamespaceAttributes.iterator()
				);
		streamEventWriter.add(endElement);
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

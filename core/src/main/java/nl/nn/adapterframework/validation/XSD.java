/*
   Copyright 2013, 2015-2017 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.custommonkey.xmlunit.Diff;
import org.xml.sax.InputSource;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IScopeProvider;
import nl.nn.adapterframework.util.FilenameUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.validation.xsd.ResourceXsd;

/**
 * The representation of a XSD.
 *
 * @author Michiel Meeuwissen
 * @author  Jaco de Groot
 */
public abstract class XSD implements IXSD, Comparable<XSD> {
	private static final Logger LOG = LogUtil.getLogger(XSD.class);

	private @Getter IScopeProvider scopeProvider;
	private @Setter String resourceInternalReference;
	private @Getter String resourceTarget;
	private String toString;
	private @Getter String namespace;
	private @Getter @Setter boolean addNamespaceToSchema = false;
	private @Getter Set<String> importedSchemaLocationsToIgnore = Collections.emptySet();
	protected @Getter @Setter boolean useBaseImportedSchemaLocationsToIgnore = false;
	private @Getter Set<String> importedNamespacesToIgnore = Collections.emptySet();
	private @Getter @Setter String parentLocation;
	private @Getter @Setter boolean rootXsd = true;
	private @Getter @Setter String targetNamespace;
	private final @Getter List<String> rootTags = new ArrayList<>();
	private final @Getter Set<String> importedNamespaces = new HashSet<>();
	private @Getter String xsdTargetNamespace;
	private @Getter String xsdDefaultNamespace;

	protected XSD() {
		super();
	}

	public void setImportedNamespacesToIgnore(Set<String> toIgnore) {
		this.importedNamespacesToIgnore = toIgnore;
	}

	public void setImportedNamespacesToIgnore(String toIgnore) {
		this.importedNamespacesToIgnore = setOf(toIgnore);
	}

	public void setImportedSchemaLocationsToIgnore(Set<String> toIgnore) {
		this.importedSchemaLocationsToIgnore = toIgnore;
	}

	public void setImportedSchemaLocationsToIgnore(String toIgnore) {
		this.importedSchemaLocationsToIgnore = setOf(toIgnore);
	}

	protected void initNoNamespace(IScopeProvider scopeProvider, String resourceRef) throws ConfigurationException {
		initNamespace(null, scopeProvider, resourceRef);
	}

	protected void initNamespace(String namespace, IScopeProvider scopeProvider, String resourceRef) throws ConfigurationException {
		this.namespace=namespace;
		this.scopeProvider=scopeProvider;
		resourceTarget = resourceRef;
		toString = resourceRef;
		if (resourceInternalReference != null) {
			resourceTarget = resourceTarget + "-" + resourceInternalReference + ".xsd";
			toString =  toString + "!" + resourceInternalReference;
		}
		resourceTarget = FilenameUtils.normalize(resourceTarget, true);
		if (parentLocation == null) {
			this.parentLocation = "";
		}
		init();
	}

	public void initFromXsds(String namespace, IScopeProvider scopeProvider, Set<IXSD> sourceXsds) throws ConfigurationException {
		this.namespace=namespace;
		this.scopeProvider=scopeProvider;
		this.resourceTarget = FilenameUtils.normalize(
			sourceXsds.stream()
			.map(IXSD::getResourceTarget)
			.map(xsd -> xsd.replace("/", "_"))
			.collect(Collectors.joining(", ", "[", "].xsd"))
		);
		this.toString = namespace + ":" + sourceXsds.stream()
			.map(Objects::toString)
			.collect(Collectors.joining(", ", "[", "]"));
		if (parentLocation == null) {
			this.parentLocation = "";
		}
		init();
	}

	/*
	 * Determine:
	 *  - schema target namespace
	 *  - schema default namespace
	 *  - imported namespaces
	 *  - list of potential root elements
	 */
	private void init() throws ConfigurationException {
		try (Reader reader = getReader()) {
			XMLEventReader er = XmlUtils.INPUT_FACTORY.createXMLEventReader(reader);
			int elementDepth = 0;
			while (er.hasNext()) {
				XMLEvent e = er.nextEvent();
				int eventType = e.getEventType();
				if (eventType == XMLStreamConstants.END_ELEMENT) {
					elementDepth--;
				}
				if (eventType != XMLStreamConstants.START_ELEMENT) {
					continue;
				}
				elementDepth++;
				StartElement el = e.asStartElement();
				if (el.getName().equals(SchemaUtils.SCHEMA)) {
					// determine for target namespace of the schema
					Attribute a = el.getAttributeByName(SchemaUtils.TNS);
					if (a != null) {
						xsdTargetNamespace = a.getValue();
					}
					@SuppressWarnings("unchecked")
					Iterator<Namespace> nsIterator = el.getNamespaces();
					// search for default namespace of the schema, i.e. a namespace definition without a prefix
					while (nsIterator.hasNext() && StringUtils.isEmpty(xsdDefaultNamespace)) {
						Namespace ns = nsIterator.next();
						if (StringUtils.isEmpty(ns.getPrefix())) {
							xsdDefaultNamespace = ns.getNamespaceURI();
						}
					}
				} else if (el.getName().equals(SchemaUtils.IMPORT)) {
					// find imported namespaces
					Attribute a = el.getAttributeByName(SchemaUtils.NAMESPACE);
					if (a == null) {
						continue;
					}
					if (StringUtils.isNotEmpty(a.getValue()) && getImportedNamespacesToIgnore().contains(a.getValue())) {
						continue;
					}
					importedNamespaces.add(a.getValue());
				} else if (el.getName().equals(SchemaUtils.ELEMENT) && (elementDepth == 2)) {
					rootTags.add(el.getAttributeByName(SchemaUtils.NAME).getValue());
				}
			}
			er.close();
			this.targetNamespace = xsdTargetNamespace;
			if (namespace == null) {
				// In case WsdlXmlValidator doesn't have schemaLocation
				namespace = xsdTargetNamespace;
			}
		} catch (IOException e) {
			String message = "IOException reading XSD";
			LOG.error(message, e);
			throw new ConfigurationException(message, e);
		} catch (XMLStreamException e) {
			String message = "XMLStreamException reading XSD";
			LOG.error(message, e);
			throw new ConfigurationException(message, e);
		}
	}

	@Override
	public String getResourceBase() {
		throw new NotImplementedException("Only for ResourceXsd");
	}

	@Override
	public String toString() {
		return toString;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof XSD) {
			XSD other = (XSD) o;
			return compareTo(other) == 0;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getResourceTarget().hashCode();
	}

	@Override
	public int compareTo(XSD x) { // CompareTo is required for WSDL generation
		if (x == null) return 1;
		if (namespace != null && x.namespace != null) {
			int c = namespace.compareTo(x.namespace);
			if (c != 0) return c;
		}
		return compareToByReferenceOrContents(x);
	}

	public int compareToByReferenceOrContents(XSD x) {
		return compareToByContents(x);
	}

	public int compareToByContents(XSD x) {
		try {
			InputSource control = new InputSource(getReader());
			InputSource test = new InputSource(x.getReader());
			Diff diff = new Diff(control, test);
			if (diff.similar()) {
				return 0;
			}
			// TODO: check necessity of this compare. If Diff says they are different, is it useful to check again for the plain contents?
			return StreamUtil.readerToString(getReader(), "\n", false).compareTo(StreamUtil.readerToString(x.getReader(), "\n", false));
		} catch (Exception e) {
			LOG.warn("Exception during XSD compare", e);
			return 1;
		}
	}

	public static Set<IXSD> getXsdsRecursive(Set<IXSD> xsds) throws ConfigurationException {
		return getXsdsRecursive(xsds, false);
	}

	public static Set<IXSD> getXsdsRecursive(Set<IXSD> xsds, boolean supportRedefine) throws ConfigurationException {
		Map<String, IXSD> xsdsRecursive = new LinkedHashMap<>();
		// First add all XSDs to a map and ensure the keys used are unique, before recursively loading more.
		// All top level XSDs need to be added, with a unique systemId. If they come from a WSDL, they all have the same systemId,
		// so we use (normalized!) resourceTarget which appears to be unique and stable.
		for (IXSD xsd: xsds) {
			String xsdKey = getXsdLoadingMapKey(xsd);
			if (xsdsRecursive.containsKey(xsdKey)) {
				throw new IllegalStateException("XSD key [" + xsdKey + "] already in map which is supposed to be unique, input XSDs: [" + xsds + "]");
			}
			xsdsRecursive.put(xsdKey, xsd);
		}
		loadAllXsdsRecursive(xsds, xsdsRecursive, supportRedefine);
		return new LinkedHashSet<>(xsdsRecursive.values());
	}

	private static void loadAllXsdsRecursive(Collection<IXSD> xsds, Map<String, IXSD> xsdsRecursive, boolean supportRedefine) throws ConfigurationException {
		for (IXSD xsd : xsds) {
			loadXsdsRecursive(xsd, xsdsRecursive, supportRedefine);
		}
	}

	private static void loadXsdsRecursive(IXSD xsd, Map<String, IXSD> xsds, boolean supportRedefine) throws ConfigurationException {
		try (Reader reader = xsd.getReader()) {
			if (reader == null) {
				// NB: Perhaps this should just throw an IllegalStateException. But I'm afraid to break some unforeseen edge case that's not covered by any of the tests.
				LOG.warn("<*> XSD without Reader; skipping: [{}]/[{}]", xsd::getResourceTarget, xsd::getTargetNamespace);
				return;
			}
			List<IXSD> schemasToLoad = new ArrayList<>();
			XMLEventReader er = XmlUtils.INPUT_FACTORY.createXMLEventReader(reader);
			while (er.hasNext()) {
				XMLEvent e = er.nextEvent();
				if (e.getEventType() != XMLStreamConstants.START_ELEMENT) {
					continue;
				}
				StartElement el = e.asStartElement();
				if (!el.getName().equals(SchemaUtils.IMPORT) &&
					!el.getName().equals(SchemaUtils.INCLUDE) &&
					(!el.getName().equals(SchemaUtils.REDEFINE) || !supportRedefine)
				) {
					continue;
				}
				Attribute schemaLocationAttribute = el.getAttributeByName(SchemaUtils.SCHEMALOCATION);
				if (schemaLocationAttribute == null) {
					continue;
				}
				Attribute namespaceAttribute = el.getAttributeByName(SchemaUtils.NAMESPACE);
				String namespace;
				if (el.getName().equals(SchemaUtils.IMPORT)) {
					if (namespaceAttribute == null
						&& StringUtils.isEmpty(xsd.getXsdDefaultNamespace())
						&& StringUtils.isNotEmpty(xsd.getXsdTargetNamespace())) {
						// TODO: concerning import without namespace when in head xsd default namespace doesn't exist and targetNamespace does)
						namespace = null;
					} else {
						if (namespaceAttribute != null) {
							namespace = namespaceAttribute.getValue();
						} else {
							namespace = xsd.getTargetNamespace();
						}
					}
				} else {
					namespace = xsd.getNamespace();
				}

				// ignore import without namespace when in head xsd default namespace and targetNamespace exists
				if (el.getName().equals(SchemaUtils.IMPORT)
					&& namespaceAttribute == null
					&& StringUtils.isNotEmpty(xsd.getXsdDefaultNamespace())
					&& StringUtils.isNotEmpty(xsd.getXsdTargetNamespace())) {

					// Skip
					continue;
				}

				// ignore import without namespace when in head xsd default namespace and targetNamespace exists.
				String sl = schemaLocationAttribute.getValue();
				if (StringUtils.isNotEmpty(sl)) {
					if (xsd.isUseBaseImportedSchemaLocationsToIgnore()) {
						sl = FilenameUtils.getName(sl);
					}
					if (xsd.getImportedSchemaLocationsToIgnore().contains(sl)) {
						// Skip
						continue;
					}
				}
				if (StringUtils.isNotEmpty(namespace)
					&& xsd.getImportedNamespacesToIgnore().contains(namespace)) {
					// Skip
					continue;
				}

				ResourceXsd x = new ResourceXsd();
				x.setAddNamespaceToSchema(xsd.isAddNamespaceToSchema());
				x.setImportedSchemaLocationsToIgnore(xsd.getImportedSchemaLocationsToIgnore());
				x.setUseBaseImportedSchemaLocationsToIgnore(xsd.isUseBaseImportedSchemaLocationsToIgnore());
				x.setImportedNamespacesToIgnore(xsd.getImportedNamespacesToIgnore());
				x.setParentLocation(xsd.getResourceBase());
				x.setRootXsd(false);
				x.initNamespace(namespace, xsd.getScopeProvider(), xsd.getResourceBase() + schemaLocationAttribute.getValue());

				String xsdKey = getXsdLoadingMapKey(x);
				if (!xsds.containsKey(xsdKey)) {
					LOG.trace("Adding xsd [{}] to set ", xsdKey);
					xsds.put(xsdKey, x);
					schemasToLoad.add(x);
				} else {
					LOG.trace("xsd [{}] already in set ", xsdKey);
				}
			}
			er.close();
			loadAllXsdsRecursive(schemasToLoad, xsds, supportRedefine);
		} catch (IOException e) {
			String message = "IOException reading XSD";
			LOG.error(message, e);
			throw new ConfigurationException(message, e);
		} catch (XMLStreamException e) {
			String message = "XMLStreamException reading XSD";
			LOG.error(message, e);
			throw new ConfigurationException(message, e);
		}
	}

	private static Set<String> setOf(String commaSeparatedItems) {
		if (commaSeparatedItems == null || commaSeparatedItems.isEmpty()) {
			return Collections.emptySet();
		}
		return new HashSet<>(Arrays.asList(commaSeparatedItems.trim().split("\\s*\\,\\s*", -1)));
	}

	private static String getXsdLoadingMapKey(IXSD xsd) {
		return xsd.getResourceTarget();
	}

	@Override
	public String getSystemId() {
		return getTargetNamespace(); // used by IntraGrammarPoolEntityResolver
	}

	/**
	 * convenience method to test adding namespaces to schemas, in the same way that SchemaUtils.mergeXsdsGroupedByNamespaceToSchemasWithoutIncludes() does this.
	 */
	public String addTargetNamespace() throws ConfigurationException {
		try {
			List<Attribute> rootAttributes = new ArrayList<>();
			List<Namespace> rootNamespaceAttributes = new ArrayList<>();
			List<XMLEvent> imports = new ArrayList<>();
			StringWriter writer = new StringWriter();
			XMLStreamWriter w = XmlUtils.REPAIR_NAMESPACES_OUTPUT_FACTORY.createXMLStreamWriter(writer);
			SchemaUtils.xsdToXmlStreamWriter(this, w, true, false, false, false, rootAttributes, rootNamespaceAttributes, imports, true);
			SchemaUtils.xsdToXmlStreamWriter(this, w, true, false, false, false, rootAttributes, rootNamespaceAttributes, imports, false);
			return writer.toString();
		} catch (XMLStreamException | IOException e) {
			throw new ConfigurationException(toString(), e);
		}
	}

}

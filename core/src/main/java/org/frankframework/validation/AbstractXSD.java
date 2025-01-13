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
package org.frankframework.validation;

import static org.frankframework.validation.SchemaUtils.isElement;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.custommonkey.xmlunit.Diff;
import org.xml.sax.InputSource;

import com.google.common.collect.Streams;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IScopeProvider;
import org.frankframework.util.LogUtil;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.StringUtil;
import org.frankframework.util.XmlUtils;
import org.frankframework.validation.xsd.ResourceXsd;

/**
 * The representation of a XSD.
 *
 * @author Michiel Meeuwissen
 * @author  Jaco de Groot
 */
public abstract class AbstractXSD implements IXSD {
	private static final Logger LOG = LogUtil.getLogger(AbstractXSD.class);

	/**
	 * See {@link #cacheCompareContentsResult(IXSD, int)} for explanation of what cache keys and values are.
	 */
	private final Map<Integer, Integer> compareByContentsCache = new HashMap<>();

	private @Getter IScopeProvider scopeProvider;
	private @Setter String resourceInternalReference;
	private @Getter String resourceTarget;
	private String toString;
	private @Getter @Setter boolean addNamespaceToSchema = false;
	private @Getter String namespace = ""; // Namespace properties may be used as map-keys so should preferably not be NULL
	private @Getter @Setter String targetNamespace = "";
	private @Getter String xsdTargetNamespace = "";
	private @Getter String xsdDefaultNamespace = "";
	protected @Getter @Setter boolean useBaseImportedSchemaLocationsToIgnore = false;
	private @Getter Set<String> importedSchemaLocationsToIgnore = Collections.emptySet();
	private @Getter Set<String> importedNamespacesToIgnore = Collections.emptySet();
	private @Getter @Setter String parentLocation;
	private @Getter @Setter boolean rootXsd = true;
	private final @Getter List<String> rootTags = new ArrayList<>();
	private final @Getter Set<String> importedNamespaces = new HashSet<>();
	private @Setter IXSD importParent;

	protected AbstractXSD() {
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
		this.namespace=namespace != null ? namespace : "";
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
		this.namespace=namespace != null ? namespace : "";
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
				if (isElement(el, SchemaUtils.SCHEMA)) {
					// determine for target namespace of the schema
					Attribute a = el.getAttributeByName(SchemaUtils.TNS);
					if (a != null) {
						xsdTargetNamespace = a.getValue() != null ? a.getValue() : "";
					}
					if (StringUtils.isEmpty(xsdDefaultNamespace)) {
						xsdDefaultNamespace = findDefaultNamespace(el);
					}
				} else if (isElement(el, SchemaUtils.IMPORT)) {
					// find imported namespaces
					Attribute a = el.getAttributeByName(SchemaUtils.NAMESPACE);
					if (a == null) {
						continue;
					}
					if (StringUtils.isNotEmpty(a.getValue()) && getImportedNamespacesToIgnore().contains(a.getValue())) {
						continue;
					}
					importedNamespaces.add(a.getValue());
				} else if (isElement(el, SchemaUtils.ELEMENT) && (elementDepth == 2)) {
					rootTags.add(el.getAttributeByName(SchemaUtils.NAME).getValue());
				}
			}
			er.close();
			this.targetNamespace = xsdTargetNamespace;
			if (StringUtils.isEmpty(namespace) && xsdTargetNamespace != null) {
				// In case WsdlXmlValidator doesn't have schemaLocation
				namespace = xsdTargetNamespace;
			}
		} catch (Exception e) {
			String message = e.getClass().getSimpleName() + " reading XSD";
			LOG.error(message, e);
			throw new ConfigurationException(message, e);
		}
	}

	private String findDefaultNamespace(StartElement el) {
		Iterator<Namespace> nsIterator = el.getNamespaces();
		return Streams.stream(nsIterator)
				.filter(ns -> StringUtils.isEmpty(ns.getPrefix()) && StringUtils.isNotEmpty(ns.getNamespaceURI()))
				.findFirst()
				.map(Namespace::getNamespaceURI)
				.orElse("");
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
		if (this == o) return true;
		if (o instanceof AbstractXSD other) {
			return compareTo(other) == 0;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getResourceTarget().hashCode();
	}

	@Override
	public int compareTo(@Nonnull IXSD other) { // CompareTo is required for WSDL generation
		if (this == other) return 0;
		if (namespace != null && other.getNamespace() != null) {
			int c = namespace.compareTo(other.getNamespace());
			if (c != 0) return c;
		}
		return compareToByReferenceOrContents(other);
	}

	protected int compareToByReferenceOrContents(IXSD other) {
		return compareToByContents(other);
	}

	@Override
	public int compareToByContents(@Nonnull IXSD other) {
		Integer cachedResult = compareByContentsCache.get(System.identityHashCode(other));
		if (cachedResult != null) {
			return cachedResult;
		}
		try {
			InputSource control = new InputSource(getReader());
			InputSource test = new InputSource(other.getReader());
			Diff diff = new Diff(control, test);
			if (diff.similar()) {
				return cacheCompareContentsResult(other, 0);
			}
			// When "diff" says they're different we still need to order them for the "compareTo" result, so we make strings and compare the strings.
			int compareResult = asString().compareTo(other.asString());
			return cacheCompareContentsResult(other, compareResult);
		} catch (Exception e) {
			LOG.warn("Exception during XSD compare", e);
			return 1;
		}
	}

	@Override
	public @Nonnull String asString() throws IOException {
		return StreamUtil.readerToString(getReader(), "\n", false);
	}

	/**
	 * The operation {@link #compareToByContents(IXSD)} is quite expensive, and sometimes we compare the very same XSD documents
	 * multiple times. Caching the results gives a minor speedup on startup of the framework.
	 * <p>
	 * The cache is keyed by the {@link System#identityHashCode} of the other XSD against which the comparison was made and stores the previous content-comparison result.
	 * The corresponding cache of the other XSD is also updated, keyed by our own identityHashCode. The identityHashCode is chosen because it's fast, and because
	 * it is guaranteed to be different for instances which might otherwise calculate the same {@link #hashCode()}.
	 * <br/>
	 * The cache is updated both for this instance, and for other.
	 * </p>
	 * @param other The other XSD against which comparison has been made.
	 * @param compareResult Result of the {@link #compareToByContents(IXSD)} operation
	 * @return Returns the value of {@code compareResult} parameter for more fluent use of this method.
	 */
	private int cacheCompareContentsResult(IXSD other, int compareResult) {
		compareByContentsCache.put(System.identityHashCode(other), compareResult);
		if (other instanceof AbstractXSD xsd) {
			xsd.compareByContentsCache.put(System.identityHashCode(this), compareResult);
		}
		return compareResult;
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
				if (!isElement(el, SchemaUtils.IMPORT, SchemaUtils.INCLUDE) &&
					(!isElement(el, SchemaUtils.REDEFINE) || !supportRedefine)
				) {
					continue;
				}
				Attribute schemaLocationAttribute = el.getAttributeByName(SchemaUtils.SCHEMALOCATION);
				if (schemaLocationAttribute == null) {
					continue;
				}
				Attribute namespaceAttribute = el.getAttributeByName(SchemaUtils.NAMESPACE);
				String namespace = deriveNamespace(xsd, el, namespaceAttribute);

				if (isImportToIgnore(xsd, el, schemaLocationAttribute, namespaceAttribute, namespace)) continue;

				addXsd(xsd, xsds, namespace, schemaLocationAttribute, schemasToLoad);
			}
			er.close();
			loadAllXsdsRecursive(schemasToLoad, xsds, supportRedefine);
		} catch (IOException | XMLStreamException | RuntimeException e) {
			String message = e.getClass().getSimpleName() + " reading XSD";
			LOG.error(message, e);
			throw new ConfigurationException(message, e);
		}
	}

	private static boolean isImportToIgnore(IXSD xsd, StartElement el, Attribute schemaLocationAttribute, Attribute namespaceAttribute, String namespace) {
		// ignore import without namespace when in head xsd default namespace and targetNamespace exists
		if (isElement(el, SchemaUtils.IMPORT)
			&& namespaceAttribute == null
			&& StringUtils.isNoneEmpty(xsd.getXsdDefaultNamespace(), xsd.getXsdTargetNamespace())) {

			// Skip
			return true;
		}

		// ignore import without namespace when in head xsd default namespace and targetNamespace exists.
		String sl = schemaLocationAttribute.getValue();
		if (StringUtils.isNotEmpty(sl)) {
			if (xsd.isUseBaseImportedSchemaLocationsToIgnore()) {
				sl = FilenameUtils.getName(sl);
			}
			if (xsd.getImportedSchemaLocationsToIgnore().contains(sl)) {
				// Skip
				return true;
			}
		}
		// Skip
		return StringUtils.isNotEmpty(namespace)
				&& xsd.getImportedNamespacesToIgnore().contains(namespace);
	}

	private static String deriveNamespace(IXSD xsd, StartElement el, Attribute namespaceAttribute) {
		String namespace;
		if (isElement(el, SchemaUtils.IMPORT)) {
			if (namespaceAttribute == null
				&& StringUtils.isEmpty(xsd.getXsdDefaultNamespace())
				&& StringUtils.isNotEmpty(xsd.getXsdTargetNamespace())) {
				// TODO: concerning import without namespace when in head xsd default namespace doesn't exist and targetNamespace does)
				namespace = "";
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
		return namespace;
	}

	private static void addXsd(IXSD xsd, Map<String, IXSD> xsds, String namespace, Attribute schemaLocationAttribute, List<IXSD> schemasToLoad) throws ConfigurationException {
		ResourceXsd x = new ResourceXsd();
		x.setAddNamespaceToSchema(xsd.isAddNamespaceToSchema());
		x.setImportedSchemaLocationsToIgnore(xsd.getImportedSchemaLocationsToIgnore());
		x.setUseBaseImportedSchemaLocationsToIgnore(xsd.isUseBaseImportedSchemaLocationsToIgnore());
		x.setImportedNamespacesToIgnore(xsd.getImportedNamespacesToIgnore());
		x.setParentLocation(xsd.getResourceBase());
		x.setRootXsd(false);
		x.setImportParent(xsd);
		x.initNamespace(namespace, xsd.getScopeProvider(), getResourceRef(xsd.getResourceBase(), schemaLocationAttribute.getValue()));

		String xsdKey = getXsdLoadingMapKey(x);
		if (!xsds.containsKey(xsdKey)) {
			LOG.trace("Adding xsd [{}] to set ", xsdKey);
			xsds.put(xsdKey, x);
			schemasToLoad.add(x);
		} else {
			LOG.trace("xsd [{}] already in set ", xsdKey);
		}
	}

	private static String getResourceRef(final String resourceBase, final String schemaLocation) {
		if (schemaLocation.startsWith("/")) {
			return schemaLocation;
		} else {
			return resourceBase + schemaLocation;
		}
	}

	private static Set<String> setOf(String commaSeparatedItems) {
		return StringUtil.splitToStream(commaSeparatedItems).collect(Collectors.toUnmodifiableSet());
	}

	private static String getXsdLoadingMapKey(IXSD xsd) {
		return xsd.getNamespace() + "|" + xsd.getResourceTarget();
	}

	@Override
	public String getSystemId() {
		return getTargetNamespace(); // used by IntraGrammarPoolEntityResolver
	}

	@Nullable
	@Override
	public IXSD getImportParent() {
		return importParent;
	}
}

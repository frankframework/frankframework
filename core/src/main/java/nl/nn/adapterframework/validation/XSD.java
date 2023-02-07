/*
   Copyright 2013, 2015-2017 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.wsdl.WSDLException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.custommonkey.xmlunit.Diff;
import org.xml.sax.InputSource;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.classloaders.ClassLoaderBase;
import nl.nn.adapterframework.core.IScopeProvider;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * The representation of a XSD.
 *
 * @author Michiel Meeuwissen
 * @author  Jaco de Groot
 */
public class XSD implements Schema, Comparable<XSD> {
	private static final Logger LOG = LogUtil.getLogger(XSD.class);

	private IScopeProvider scopeProvider;
	private javax.wsdl.Definition wsdlDefinition;
	private javax.wsdl.extensions.schema.Schema wsdlSchema;
	private String resource;
	private @Setter String resourceInternalReference;
	private URL url;
	private @Setter ByteArrayOutputStream byteArrayOutputStream;
	private @Getter String resourceTarget;
	private String toString;
	private @Getter String namespace;
	private @Getter @Setter boolean addNamespaceToSchema = false;
	private @Getter @Setter String importedSchemaLocationsToIgnore;
	protected @Getter @Setter boolean useBaseImportedSchemaLocationsToIgnore = false;
	private @Getter @Setter String importedNamespacesToIgnore;
	private @Getter @Setter String parentLocation;
	private @Getter @Setter boolean rootXsd = true;
	private @Getter @Setter String targetNamespace;
	private @Getter List<String> rootTags = new ArrayList<String>();
	private @Getter Set<String> importedNamespaces = new HashSet<String>();
	private String xsdTargetNamespace;
	private String xsdDefaultNamespace;


	public void setWsdlSchema(javax.wsdl.Definition wsdlDefinition, javax.wsdl.extensions.schema.Schema wsdlSchema) {
		this.wsdlDefinition = wsdlDefinition;
		this.wsdlSchema = wsdlSchema;
	}



	public void initNoNamespace(IScopeProvider scopeProvider, String noNamespaceSchemaLocation) throws ConfigurationException {
		this.scopeProvider=scopeProvider;
		this.resource=noNamespaceSchemaLocation;
		url = ClassUtils.getResourceURL(scopeProvider, noNamespaceSchemaLocation);
		if (url == null) {
			throw new ConfigurationException("Cannot find [" + noNamespaceSchemaLocation + "]");
		}
		resourceTarget = noNamespaceSchemaLocation;
		toString = noNamespaceSchemaLocation;
		init();
	}

	public void initNamespace(String namespace, IScopeProvider scopeProvider, String resourceRef) throws ConfigurationException {
		this.namespace=namespace;
		this.scopeProvider=scopeProvider;
		resource=resourceRef;
		resource = Misc.replace(resource, "%20", " ");
		url = ClassUtils.getResourceURL(scopeProvider, resource);
		if (url == null) {
			throw new ConfigurationException("Cannot find [" + resource + "]");
		}
		resourceTarget = resource;
		toString = resource;
		if (resourceInternalReference != null) {
			resourceTarget = resourceTarget + "-" + resourceInternalReference + ".xsd";
			toString =  toString + "!" + resourceInternalReference;
		}
		if (parentLocation == null) {
			this.parentLocation = "";
		}
		init();
	}

	public void initFromXsds(String namespace, IScopeProvider scopeProvider, Set<XSD> sourceXsds) throws ConfigurationException {
		this.namespace=namespace;
		this.scopeProvider=scopeProvider;
		resourceTarget = "[";
		toString = "[";
		boolean first = true;
		for (XSD xsd : sourceXsds) {
			if (first) {
				first = false;
			} else {
				resourceTarget = resourceTarget + ", ";
				toString = toString + ", ";
			}
			resourceTarget = resourceTarget + xsd.getResourceTarget().replaceAll("/", "_");
			toString = toString + xsd.toString();
		}
		resourceTarget = resourceTarget + "].xsd";
		toString = toString + "]";
		if (parentLocation == null) {
			this.parentLocation = "";
		}
		init();
	}

	private void init() throws ConfigurationException {
		try {
			Reader reader = getReader();
			XMLEventReader er = XmlUtils.INPUT_FACTORY.createXMLEventReader(reader);
			int elementDepth = 0;
			while (er.hasNext()) {
				XMLEvent e = er.nextEvent();
				switch (e.getEventType()) {
				case XMLStreamConstants.START_ELEMENT:
					elementDepth++;
					StartElement el = e.asStartElement();
					if (el.getName().equals(SchemaUtils.SCHEMA)) {
						Attribute a = el.getAttributeByName(SchemaUtils.TNS);
						if (a != null) {
							xsdTargetNamespace = a.getValue();
						}
						Iterator<Namespace> nsIterator = el.getNamespaces();
						while (nsIterator.hasNext() && StringUtils.isEmpty(xsdDefaultNamespace)) {
							Namespace ns = nsIterator.next();
							if (StringUtils.isEmpty(ns.getPrefix())) {
								xsdDefaultNamespace = ns.getNamespaceURI();
							}
						}
					} else if (el.getName().equals(SchemaUtils.IMPORT)) {
						Attribute a =
								el.getAttributeByName(SchemaUtils.NAMESPACE);
						if (a != null) {
							boolean skip = false;
							List<String> ans = null;
							if (StringUtils.isNotEmpty(getImportedNamespacesToIgnore())) {
								ans = listOf(getImportedNamespacesToIgnore());
							}
							if (StringUtils.isNotEmpty(a.getValue()) && ans != null) {
								if (ans.contains(a.getValue())) {
									skip = true;
								}
							}
							if (!skip) {
								importedNamespaces.add(a.getValue());
							}
						}
					} else if (el.getName().equals(SchemaUtils.ELEMENT)) {
						if (elementDepth == 2) {
							rootTags.add(el.getAttributeByName(SchemaUtils.NAME).getValue());
						}
					}
					break;
				case XMLStreamConstants.END_ELEMENT:
					elementDepth--;
					break;
				}
			}
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

	public String getResourceBase() {
		return resource.substring(0, resource.lastIndexOf('/') + 1);
	}

	@Override
	public String toString() {
		return toString;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof XSD) {
			XSD other = (XSD) o;
			if (compareTo(other) == 0) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getResourceTarget().hashCode();
	}

	@Override
	public int compareTo(XSD x) {
		if (x == null) return 1;
		if (namespace != null && x.namespace != null) {
			int c = namespace.compareTo(x.namespace);
			if (c != 0) return c;
		}
		if (wsdlSchema != null || url == null || (url.toString().compareTo(x.url.toString()) != 0)) {
			// Compare XSD content to prevent copies of the same XSD showing up
			// more than once in the WSDL. For example the
			// CommonMessageHeader.xsd used by the EsbSoapValidator will
			// normally also be imported by the XSD for the business response
			// message (for the Result part).
			try {
				InputSource control = new InputSource(getReader());
				InputSource test = new InputSource(x.getReader());
				Diff diff = new Diff(control, test);
				if (diff.similar()) {
					return 0;
				} else if (wsdlSchema != null || url == null) {
					return Misc.readerToString(getReader(), "\n", false).compareTo(Misc.readerToString(x.getReader(), "\n", false));
				}
			} catch (Exception e) {
				LOG.warn("Exception during XSD compare", e);
			}
		}
		return url.toString().compareTo(x.url.toString());
	}

	@Override
	public Reader getReader() throws IOException {
		if (wsdlSchema != null) {
			try {
				return SchemaUtils.toReader(wsdlDefinition, wsdlSchema);
			} catch (WSDLException e) {
				throw new IOException(e);
			}
		} else if (byteArrayOutputStream != null) {
			return StreamUtil.getCharsetDetectingInputStreamReader(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
		} else {
			return StreamUtil.getCharsetDetectingInputStreamReader(url.openStream());
		}
	}


	public Set<XSD> getXsdsRecursive(boolean supportRedifine) throws ConfigurationException {
		return getXsdsRecursive(new HashSet<XSD>(), supportRedifine);
	}

	public Set<XSD> getXsdsRecursive(Set<XSD> xsds, boolean supportRedefine) throws ConfigurationException {
		try {
			Reader reader = getReader();
			if (reader == null) {
				return null;
			}
			XMLEventReader er = XmlUtils.INPUT_FACTORY.createXMLEventReader(reader);
			while (er.hasNext()) {
				XMLEvent e = er.nextEvent();
				switch (e.getEventType()) {
				case XMLStreamConstants.START_ELEMENT:
					StartElement el = e.asStartElement();
					if (el.getName().equals(SchemaUtils.IMPORT) ||
						el.getName().equals(SchemaUtils.INCLUDE)||
						(el.getName().equals(SchemaUtils.REDEFINE) && supportRedefine)
						) {
						Attribute schemaLocationAttribute = el.getAttributeByName(SchemaUtils.SCHEMALOCATION);
						Attribute namespaceAttribute = el.getAttributeByName(SchemaUtils.NAMESPACE);
						String namespace = this.namespace;
						if (el.getName().equals(SchemaUtils.IMPORT)) {
							if (namespaceAttribute == null && StringUtils.isEmpty(xsdDefaultNamespace) && StringUtils.isNotEmpty(xsdTargetNamespace)) {
								// TODO: concerning import without namespace when in head xsd default namespace doesn't exist and targetNamespace does)
								namespace = null;
							} else {
								if (namespaceAttribute != null) {
									namespace = namespaceAttribute.getValue();
								} else {
									namespace = targetNamespace;
								}
							}
						}
						if (schemaLocationAttribute != null) {
							boolean skip = false;
							if (el.getName().equals(SchemaUtils.IMPORT) && namespaceAttribute == null) {
								if (StringUtils.isNotEmpty(xsdDefaultNamespace) && StringUtils.isNotEmpty(xsdTargetNamespace)) {
									// ignore import without namespace when in head xsd default namespace and targetNamespace exists)
									skip = true;
								}
							}
							if (!skip) {
								String sl = schemaLocationAttribute.getValue();
								List<String> aslti = null;
								if (StringUtils.isNotEmpty(getImportedSchemaLocationsToIgnore())) {
									aslti = listOf(getImportedSchemaLocationsToIgnore());
								}
								if (StringUtils.isNotEmpty(sl) && aslti != null) {
									if (isUseBaseImportedSchemaLocationsToIgnore()) {
										sl = FilenameUtils.getName(sl);
									}
									if (aslti.contains(sl)) {
										skip = true;
									}
								}
							}
							if (!skip && StringUtils.isNotEmpty(namespace)) {
								List<String> ans = null;
								if (StringUtils.isNotEmpty(getImportedNamespacesToIgnore())) {
									ans = listOf(getImportedNamespacesToIgnore());
									if (ans.contains(namespace)) {
										skip = true;
									}
								}
							}
							if (!skip) {
								XSD x = new XSD();
								x.setAddNamespaceToSchema(isAddNamespaceToSchema());
								x.setImportedSchemaLocationsToIgnore(getImportedSchemaLocationsToIgnore());
								x.setUseBaseImportedSchemaLocationsToIgnore(isUseBaseImportedSchemaLocationsToIgnore());
								x.setImportedNamespacesToIgnore(getImportedNamespacesToIgnore());
								x.setParentLocation(getResourceBase());
								x.setRootXsd(false);
								x.initNamespace(namespace, scopeProvider, getResourceBase() + schemaLocationAttribute.getValue());
								if (xsds.add(x)) {
									x.getXsdsRecursive(xsds, supportRedefine);
								}
							}
						}
					}
					break;
				}
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
		return xsds;
	}

	private List<String> listOf(String commaSeparatedItems) {
		return Arrays.asList(commaSeparatedItems.trim().split("\\s*\\,\\s*", -1));
	}

	@Override
	public String getSystemId() {
		if (resource == null) {
			return getTargetNamespace(); // used by IntraGrammarPoolEntityResolver
		}
		return ClassLoaderBase.CLASSPATH_RESOURCE_SCHEME+resource; //Must prefix this with the `classpath:` protocol else Xerces will append the `file:` protocol
	}

	public boolean hasDependency(Set<XSD> xsds) {
		for (XSD xsd : xsds) {
			if (getImportedNamespaces().contains(xsd.getTargetNamespace())) {
				return true;
			}
		}
		return false;
	}

	public void addTargetNamespace() throws ConfigurationException {
		try {
			List<Attribute> rootAttributes = new ArrayList<Attribute>();
			List<Namespace> rootNamespaceAttributes = new ArrayList<Namespace>();
			List<XMLEvent> imports = new ArrayList<XMLEvent>();
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			XMLStreamWriter w = XmlUtils.REPAIR_NAMESPACES_OUTPUT_FACTORY.createXMLStreamWriter(byteArrayOutputStream, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
			SchemaUtils.xsdToXmlStreamWriter(this, w, true, false, false, false, rootAttributes, rootNamespaceAttributes, imports, true);
			SchemaUtils.xsdToXmlStreamWriter(this, w, true, false, false, false, rootAttributes, rootNamespaceAttributes, imports, false);
			setByteArrayOutputStream(byteArrayOutputStream);
		} catch (XMLStreamException | IOException e) {
			throw new ConfigurationException(toString(), e);
		}
	}

}

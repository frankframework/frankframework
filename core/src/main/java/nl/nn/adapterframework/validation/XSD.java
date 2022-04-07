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
import java.io.InputStream;
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
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.custommonkey.xmlunit.Diff;
import org.xml.sax.InputSource;

import nl.nn.adapterframework.configuration.ConfigurationException;
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
	private String resourceInternalReference;
	private URL url;
	private ByteArrayOutputStream byteArrayOutputStream;
	private String resourceTarget;
	private String toString;
	private String namespace;
	private boolean addNamespaceToSchema = false;
	private String importedSchemaLocationsToIgnore;
	protected boolean useBaseImportedSchemaLocationsToIgnore = false;
	private String importedNamespacesToIgnore;
	private String parentLocation;
	private boolean isRootXsd = true;
	private String targetNamespace;
	private List<String> rootTags = new ArrayList<String>();
	private Set<String> importedNamespaces = new HashSet<String>();
	private String xsdTargetNamespace;
	private String xsdDefaultNamespace;


	public void setWsdlSchema(javax.wsdl.Definition wsdlDefinition, javax.wsdl.extensions.schema.Schema wsdlSchema) {
		this.wsdlDefinition = wsdlDefinition;
		this.wsdlSchema = wsdlSchema;
	}

	public void setResourceInternalReference(String resourceInternalReference) {
		this.resourceInternalReference = resourceInternalReference;
	}

	public void setByteArrayOutputStream(ByteArrayOutputStream byteArrayOutputStream) {
		this.byteArrayOutputStream = byteArrayOutputStream;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setAddNamespaceToSchema(boolean addNamespaceToSchema) {
		this.addNamespaceToSchema = addNamespaceToSchema;
	}

	public boolean isAddNamespaceToSchema() {
		return addNamespaceToSchema;
	}

	public void setImportedSchemaLocationsToIgnore(String string) {
		importedSchemaLocationsToIgnore = string;
	}

	public String getImportedSchemaLocationsToIgnore() {
		return importedSchemaLocationsToIgnore;
	}

	public boolean isUseBaseImportedSchemaLocationsToIgnore() {
		return useBaseImportedSchemaLocationsToIgnore;
	}

	public void setUseBaseImportedSchemaLocationsToIgnore(boolean useBaseImportedSchemaLocationsToIgnore) {
		this.useBaseImportedSchemaLocationsToIgnore = useBaseImportedSchemaLocationsToIgnore;
	}

	public void setImportedNamespacesToIgnore(String string) {
		importedNamespacesToIgnore = string;
	}

	public String getImportedNamespacesToIgnore() {
		return importedNamespacesToIgnore;
	}

	public void setParentLocation(String parentLocation) {
		this.parentLocation = parentLocation;
	}

	public String getParentLocation() {
		return parentLocation;
	}

	public void setRootXsd(boolean isRootXsd) {
		this.isRootXsd = isRootXsd;
	}

	public boolean isRootXsd() {
		return isRootXsd;
	}

	public void setTargetNamespace(String targetNamespace) {
		this.targetNamespace = targetNamespace;
	}

	public String getTargetNamespace() {
		return targetNamespace;
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
			InputStream in = getInputStream();
			XMLEventReader er = XmlUtils.INPUT_FACTORY.createXMLEventReader(in, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
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

	public String getResourceTarget() {
		return resourceTarget;
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
				InputSource control = new InputSource(getInputStream());
				InputSource test = new InputSource(x.getInputStream());
				Diff diff = new Diff(control, test);
				if (diff.similar()) {
					return 0;
				} else if (wsdlSchema != null || url == null) {
					return Misc.streamToString(getInputStream(), "\n", false).compareTo(Misc.streamToString(x.getInputStream(), "\n", false));
				}
			} catch (Exception e) {
				LOG.warn("Exception during XSD compare", e);
			}
		}
		return url.toString().compareTo(x.url.toString());
	}

	@Override
	public InputStream getInputStream() throws IOException, ConfigurationException {
		if (wsdlSchema != null) {
			try {
				return SchemaUtils.toInputStream(wsdlDefinition, wsdlSchema);
			} catch (WSDLException e) {
				throw new ConfigurationException(e);
			}
		} else if (byteArrayOutputStream != null) {
			return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
		} else {
			return url.openStream();
		}
	}

	public Set<String> getImportedNamespaces() {
		return importedNamespaces;
	}

	public Set<XSD> getXsdsRecursive() throws ConfigurationException {
		return getXsdsRecursive(true);
	}

	public Set<XSD> getXsdsRecursive(boolean ignoreRedefine) throws ConfigurationException {
		return getXsdsRecursive(new HashSet<XSD>(), ignoreRedefine);
	}

	public Set<XSD> getXsdsRecursive(Set<XSD> xsds) throws ConfigurationException {
		return getXsdsRecursive(xsds, true);
	}

	public Set<XSD> getXsdsRecursive(Set<XSD> xsds, boolean ignoreRedefine) throws ConfigurationException {
		try {
			InputStream in = getInputStream();
			if (in == null) {
				return null;
			}
			XMLEventReader er = XmlUtils.INPUT_FACTORY.createXMLEventReader(in, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
			while (er.hasNext()) {
				XMLEvent e = er.nextEvent();
				switch (e.getEventType()) {
				case XMLStreamConstants.START_ELEMENT:
					StartElement el = e.asStartElement();
					if (el.getName().equals(SchemaUtils.IMPORT) ||
						el.getName().equals(SchemaUtils.INCLUDE)||
						(el.getName().equals(SchemaUtils.REDEFINE) && !ignoreRedefine)
						) {
						Attribute schemaLocationAttribute = el.getAttributeByName(SchemaUtils.SCHEMALOCATION);
						Attribute namespaceAttribute = el.getAttributeByName(SchemaUtils.NAMESPACE);
						String namespace = this.namespace;
						boolean addNamespaceToSchema = this.addNamespaceToSchema;
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
								x.setAddNamespaceToSchema(addNamespaceToSchema);
								x.setImportedSchemaLocationsToIgnore(getImportedSchemaLocationsToIgnore());
								x.setUseBaseImportedSchemaLocationsToIgnore(isUseBaseImportedSchemaLocationsToIgnore());
								x.setImportedNamespacesToIgnore(getImportedNamespacesToIgnore());
								x.setParentLocation(getResourceBase());
								x.setRootXsd(false);
								x.initNamespace(namespace, scopeProvider, getResourceBase() + schemaLocationAttribute.getValue());
								if (xsds.add(x)) {
									x.getXsdsRecursive(xsds, ignoreRedefine);
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

	public List<String> getRootTags() {
		return rootTags;
	}

	@Override
	public String getSystemId() {
		if (url == null) {
			return null;
		}
		return url.toExternalForm();
	}

	public boolean hasDependency(Set<XSD> xsds) {
		for (XSD xsd : xsds) {
			if (getImportedNamespaces().contains(xsd.getTargetNamespace())) {
				return true;
			}
 		}
		return false;
	}

}

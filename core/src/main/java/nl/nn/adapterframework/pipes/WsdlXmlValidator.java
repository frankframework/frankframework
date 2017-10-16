/*
   Copyright 2013, 2015, 2016 Nationale-Nederlanden

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
package nl.nn.adapterframework.pipes;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.soap.SoapValidator;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.validation.SchemaUtils;
import nl.nn.adapterframework.validation.XSD;
import nl.nn.javax.wsdl.Definition;
import nl.nn.javax.wsdl.WSDLException;
import nl.nn.javax.wsdl.extensions.ExtensibilityElement;
import nl.nn.javax.wsdl.extensions.schema.Schema;
import nl.nn.javax.wsdl.factory.WSDLFactory;
import nl.nn.javax.wsdl.xml.WSDLLocator;
import nl.nn.javax.wsdl.xml.WSDLReader;
import nl.nn.javax.xml.namespace.QName;

/**
 * XmlValidator that will read the XSD's to use from a WSDL. As it extends the
 * SoapValidator is will also add the SOAP envelope XSD.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>*</td><td>all attributes available on {@link SoapValidator} can be used</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setWsdl(String) wsdl}</td><td>the WSDL to read the XSD's from</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSchemaLocation(String) schemaLocation}</td><td>see schemaLocation attribute on XmlValidator except that the schema locations are referring to schema's in the WSDL, schema1 refers to the first, schema2 refers to the second and so on. If not set, all schema's are derived from the WSDL</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSchemaLocationToAdd(String) schemaLocationToAdd}</td><td>Pairs of URI references which will be added to the WSDL</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSoapBodyNamespace(String) soapBodyNamespace}</td><td>creates <code>schemaLocation</code> attribute based on the WSDL and replaces the namespace of the SOAP Body element</td><td>&nbsp;</td></tr>
 * </table>
 * 
 * @author Michiel Meeuwissen
 * @author Jaco de Groot
 */
public class WsdlXmlValidator extends SoapValidator {
	private static final Logger LOG = LogUtil.getLogger(WsdlXmlValidator.class);

    private String soapBodyNamespace  = "";

	private static final WSDLFactory FACTORY;
	static {
		WSDLFactory f;
		try {
			f = WSDLFactory.newInstance();
		} catch (WSDLException e) {
			f = null;
			LOG.error(e.getMessage(), e);
		}
		FACTORY = f;
	}

	private static final String RESOURCE_INTERNAL_REFERENCE_PREFIX = "schema";
	
	private String wsdl;
	private Definition definition;
	private String schemaLocationToAdd;

	public void setWsdl(String wsdl) throws ConfigurationException {
		this.wsdl = wsdl;
		WSDLReader reader  = FACTORY.newWSDLReader();
		reader.setFeature("javax.wsdl.verbose", false);
		reader.setFeature("javax.wsdl.importDocuments", true);
		ClassLoaderWSDLLocator wsdlLocator = new ClassLoaderWSDLLocator(classLoader, wsdl);
		URL url = wsdlLocator.getUrl();
		if (wsdlLocator.getUrl() == null) {
			throw new ConfigurationException("Could not find WSDL: " + wsdl);
		}
		try {
			definition = reader.readWSDL(wsdlLocator);
		} catch (WSDLException e) {
			throw new ConfigurationException("WSDLException reading WSDL or import from url: " + url, e);
		}
		if (wsdlLocator.getIOException() != null) {
			throw new ConfigurationException("IOException reading WSDL or import from url: " + url,
					wsdlLocator.getIOException());
		}
	}

	public String getWsdl() {
		return wsdl;
	}

	@Override
	public void configure() throws ConfigurationException {
		addSoapEnvelopeToSchemaLocation = false;

		if (ConfigurationUtils.stubConfiguration()) {
			// config warning only in stub mode (for now) to avoid lot of
			// warnings in IJA_API in DTAP
			if (StringUtils.isNotEmpty(getSchemaLocation())
					&& !isAddNamespaceToSchema()) {
				ConfigurationWarnings configWarnings = ConfigurationWarnings
						.getInstance();
				String msg = getLogPrefix(null)
						+ "attribute [schemaLocation] for wsdl [" + getWsdl()
						+ "] should only be set when addNamespaceToSchema=true";
				configWarnings.add(log, msg);
			}
		}
		if (StringUtils.isNotEmpty(getSoapBodyNamespace())
				&& StringUtils.isNotEmpty(getSchemaLocation())) {
			ConfigurationWarnings configWarnings = ConfigurationWarnings
					.getInstance();
			String msg = getLogPrefix(null)
					+ "attribute [schemaLocation] for wsdl [" + getWsdl()
					+ "] should only be set when attribute [soapBodyNamespace] is not set";
			configWarnings.add(log, msg);
		}
		if (StringUtils.isNotEmpty(getSoapBodyNamespace())
				&& !isAddNamespaceToSchema()) {
			ConfigurationWarnings configWarnings = ConfigurationWarnings
					.getInstance();
			String msg = getLogPrefix(null)
					+ "attribute [soapBodyNamespace] for wsdl [" + getWsdl()
					+ "] should only be set when addNamespaceToSchema=true";
			configWarnings.add(log, msg);
		}

		String wsdlSchemaLocation = null;
		StringBuilder sb = new StringBuilder();
		int counter = 0;
		boolean soapBodyFound = false;
		for (Object o : definition.getTypes().getExtensibilityElements()) {
			if (o instanceof Schema) {
				Schema schema = (Schema) o;
				String tns = schema.getElement()
						.getAttribute("targetNamespace");
				if (StringUtils.isNotEmpty(getSoapBodyNamespace())) {
					NodeList childNodes = schema.getElement().getChildNodes();
					for (int i = 0; i < childNodes.getLength(); i++) {
						Node n = childNodes.item(i);
						if (n.getNodeType() == Node.ELEMENT_NODE
								&& n.getLocalName().equals("element")) {
							String name = n.getAttributes().getNamedItem("name")
									.getNodeValue();
							if (getSoapBody().equals(name)) {
								if (soapBodyFound) {
									throw new ConfigurationException(
											getLogPrefix(null) + "soapBody ["
													+ getSoapBody()
													+ "] exists multiple times, not possible to create schemaLocation from soapBodyNamespace");
								}
								tns = getSoapBodyNamespace();
								soapBodyFound = true;
							}
						}
					}
				}
				if (sb.length() > 0) {
					sb.append(" ");
				}
				sb.append(tns);
				sb.append(" ");
				sb.append("schema" + ++counter);
			}
			if (sb.length() > 0) {
				wsdlSchemaLocation = sb.toString();
			}

			if (wsdlSchemaLocation != null) {
				if (ConfigurationUtils.stubConfiguration()) {
					// config warning only in stub mode (for now) to avoid lot
					// of warnings in IJA_API in DTAP
					if (StringUtils.isNotEmpty(getSchemaLocation())
							&& isAddNamespaceToSchema()) {
						if (getSchemaLocation().replaceAll("\\s", " ")
								.equals(wsdlSchemaLocation)) {
							ConfigurationWarnings configWarnings = ConfigurationWarnings
									.getInstance();
							String msg = getLogPrefix(null)
									+ "attribute [schemaLocation] for wsdl ["
									+ getWsdl()
									+ "] already has a default value ["
									+ wsdlSchemaLocation + "]";
							configWarnings.add(log, msg);
						}
					}
				}

				if (StringUtils.isNotEmpty(getSoapBodyNamespace())) {
					setSchemaLocation(wsdlSchemaLocation);
				}
			}
		}

		super.configure();
	}

	@Override
	protected void checkSchemaSpecified() throws ConfigurationException {
		if (StringUtils.isEmpty(getWsdl())) {
			throw new ConfigurationException(getLogPrefix(null) + "wsdl attribute cannot be empty");
		}
	}


	protected static void addNamespaces(Schema schema, Map<String, String> namespaces) {
		for (Map.Entry<String,String> e : namespaces.entrySet()) {
			String key = e.getKey().length() == 0 ? "xmlns" : ("xmlns:" + e.getKey());
			if (schema.getElement().getAttribute(key).length() == 0) {
				schema.getElement().setAttribute(key, e.getValue());
			}
		}
	}

	@Override
	public String getSchemasId() {
		return wsdl;
	}

	@Override
	public Set<XSD> getXsds() throws ConfigurationException {
		Set<XSD> xsds = new HashSet<XSD>();
		if (getSoapVersion() == null || "1.1".equals(getSoapVersion()) || "any".equals(getSoapVersion())) {
			XSD xsd = new XSD();
			xsd.setClassLoader(classLoader);
			xsd.setNamespace(SoapValidator.SoapVersion.VERSION_1_1.namespace);
			xsd.setResource(SoapValidator.SoapVersion.VERSION_1_1.location);
			xsd.init();
			xsds.add(xsd);
		}
		if ("1.2".equals(getSoapVersion()) || "any".equals(getSoapVersion())) {
			XSD xsd = new XSD();
			xsd.setClassLoader(classLoader);
			xsd.setNamespace(SoapValidator.SoapVersion.VERSION_1_2.namespace);
			xsd.setResource(SoapValidator.SoapVersion.VERSION_1_2.location);
			xsd.init();
			xsds.add(xsd);
		}
		if (StringUtils.isNotEmpty(getSchemaLocationToAdd())) {
			StringTokenizer st = new StringTokenizer(getSchemaLocationToAdd(), ", \t\r\n\f");
			while (st.hasMoreTokens()) {
				XSD xsd = new XSD();
				xsd.setClassLoader(classLoader);
				xsd.setNamespace(st.nextToken());
				if (st.hasMoreTokens()) {
					xsd.setResource(st.nextToken());
				}
				xsd.init();
				xsds.add(xsd);
			}
		}
		List<Schema> schemas = new ArrayList<Schema>();
		List types = definition.getTypes().getExtensibilityElements();
		for (Iterator i = types.iterator(); i.hasNext();) {
			ExtensibilityElement type = (ExtensibilityElement)i.next();
			QName qn = type.getElementType();
			if (SchemaUtils.WSDL_SCHEMA.equals(qn)) {
				final Schema schema = (Schema) type;
				addNamespaces(schema, definition.getNamespaces());
				schemas.add(schema);
			}
		}
		List<Schema> filteredSchemas;
		Map<Schema, String> filteredReferences = null;
		Map<Schema, String> filteredNamespaces = null;
		if (StringUtils.isEmpty(schemaLocation)) {
			filteredSchemas = schemas;
		} else {
			filteredSchemas = new ArrayList<Schema>();
			filteredReferences = new HashMap<Schema, String>();
			filteredNamespaces = new HashMap<Schema, String>();
			String[] split =  schemaLocation.trim().split("\\s+");
			if (split.length % 2 != 0) throw new ConfigurationException("The schema must exist from an even number of strings, but it is " + schemaLocation);
			for (int i = 0; i < split.length; i += 2) {
				if (!split[i + 1].startsWith(RESOURCE_INTERNAL_REFERENCE_PREFIX)) {
					throw new ConfigurationException("Schema reference " + split[i + 1] + " should start with '" + RESOURCE_INTERNAL_REFERENCE_PREFIX + "'");
				}
				try {
					int j = Integer.parseInt(split[i + 1].substring(RESOURCE_INTERNAL_REFERENCE_PREFIX.length())) - 1;
					filteredSchemas.add(schemas.get(j));
					filteredReferences.put(schemas.get(j), RESOURCE_INTERNAL_REFERENCE_PREFIX + (j + 1));
					filteredNamespaces.put(schemas.get(j), split[i]);
				} catch(Exception e) {
					throw new ConfigurationException("Schema reference " + split[i + 1] + " not valid or not found");
				}
			}
		}
		for (Schema schema : filteredSchemas) {
			XSD xsd = new XSD();
			xsd.setClassLoader(classLoader);
			xsd.setWsdlSchema(definition, schema);
			xsd.setResource(getWsdl());
			if (StringUtils.isNotEmpty(schemaLocation)) {
				xsd.setResourceInternalReference(filteredReferences.get(schema));
				xsd.setNamespace(filteredNamespaces.get(schema));
			} else {
				xsd.setResourceInternalReference(RESOURCE_INTERNAL_REFERENCE_PREFIX + (filteredSchemas.indexOf(schema) + 1));
			}
			xsd.setAddNamespaceToSchema(isAddNamespaceToSchema());
			xsd.setImportedSchemaLocationsToIgnore(getImportedSchemaLocationsToIgnore());
			xsd.setUseBaseImportedSchemaLocationsToIgnore(isUseBaseImportedSchemaLocationsToIgnore());
			xsd.setImportedNamespacesToIgnore(getImportedNamespacesToIgnore());
			xsd.init();
			xsds.add(xsd);
		}
		return xsds;
	}

	public void setSchemaLocationToAdd(String schemaLocationToAdd) {
		this.schemaLocationToAdd = schemaLocationToAdd;
	}

	public String getSchemaLocationToAdd() {
		return schemaLocationToAdd;
	}

    public void setSoapBodyNamespace(String soapBodyNamespace) {
        this.soapBodyNamespace = soapBodyNamespace;
    }

    public String getSoapBodyNamespace() {
        return soapBodyNamespace;
    }
}

class ClassLoaderWSDLLocator implements WSDLLocator {
	private ClassLoader classLoader;
	private String wsdl;
	private URL url;
	private IOException ioException;
	private String latestImportURI;

	ClassLoaderWSDLLocator(ClassLoader classLoader, String wsdl) {
		this.classLoader = classLoader;
		this.wsdl = wsdl;
		url = ClassUtils.getResourceURL(classLoader, wsdl);
	}

	public URL getUrl() {
		return url;
	}

	public IOException getIOException() {
		return ioException;
	}

	public InputSource getBaseInputSource() {
		return getInputSource(url);
	}

	public String getBaseURI() {
		return wsdl;
	}

	public InputSource getImportInputSource(String parentLocation, String importLocation) {
		if (parentLocation == null) {
			parentLocation = wsdl;
		}
		int i = parentLocation.lastIndexOf('/');
		if (i == -1) {
			latestImportURI = importLocation;
		} else {
			latestImportURI = parentLocation.substring(0, i + 1) + importLocation;
		}
		return getInputSource(latestImportURI);
	}

	public String getLatestImportURI() {
		return latestImportURI;
	}

	public void close() {
	}

	private InputSource getInputSource(String resource) {
		return getInputSource(ClassUtils.getResourceURL(classLoader, resource));
	}

	private InputSource getInputSource(URL url) {
		InputStream inputStream = null;
		if (url != null) {
			try {
				inputStream = url.openStream();
			} catch (IOException e) {
				ioException = e;
			}
		}
		InputSource source = null;
		if (inputStream != null) {
			source = new InputSource(inputStream);
			source.setSystemId(url.toString());
		}
		return source;
	}
}
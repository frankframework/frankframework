/*
   Copyright 2013-2019 Nationale-Nederlanden, 2020-2021 WeAreFrank!

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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLLocator;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IScopeProvider;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.soap.SoapValidator;
import nl.nn.adapterframework.soap.SoapVersion;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.validation.SchemaUtils;
import nl.nn.adapterframework.validation.XSD;

/**
 * XmlValidator that will read the XSD's to use from a WSDL. As it extends the
 * SoapValidator is will also add the SOAP envelope XSD.
 * 
 * @author Michiel Meeuwissen
 * @author Jaco de Groot
 */
public class WsdlXmlValidator extends SoapValidator {
	private static final Logger LOG = LogUtil.getLogger(WsdlXmlValidator.class);

	private String soapBodyNamespace = "";

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

	public static final String RESOURCE_INTERNAL_REFERENCE_PREFIX = "schema";
	
	private String wsdl;
	private Definition definition;
	private String schemaLocationToAdd;

	@IbisDoc({"the wsdl to read the xsd's from", " "})
	public void setWsdl(String wsdl) throws ConfigurationException {
		this.wsdl = wsdl;
		WSDLReader reader  = FACTORY.newWSDLReader();
		reader.setFeature("javax.wsdl.verbose", false);
		reader.setFeature("javax.wsdl.importDocuments", true);
		ClassLoaderWSDLLocator wsdlLocator = new ClassLoaderWSDLLocator(this, wsdl);
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
			throw new ConfigurationException("IOException reading WSDL or import from url: " + url, wsdlLocator.getIOException());
		}
	}

	private String getWsdl() {
		return wsdl;
	}

	@Override
	public void configure() throws ConfigurationException {
		addSoapEnvelopeToSchemaLocation = false;

		if (StringUtils.isNotEmpty(getSchemaLocation()) && !isAddNamespaceToSchema()) {
			ConfigurationWarnings.add(this, log, "attribute [schemaLocation] for wsdl [" + getWsdl() + "] should only be set when addNamespaceToSchema=true");
		}
		if (StringUtils.isNotEmpty(getSoapBodyNamespace()) && StringUtils.isNotEmpty(getSchemaLocation())) {
			ConfigurationWarnings.add(this, log, "attribute [schemaLocation] for wsdl [" + getWsdl() + "] should only be set when attribute [soapBodyNamespace] is not set");
		}
		if (StringUtils.isNotEmpty(getSoapBodyNamespace()) && !isAddNamespaceToSchema()) {
			ConfigurationWarnings.add(this, log, "attribute [soapBodyNamespace] for wsdl [" + getWsdl() + "] should only be set when addNamespaceToSchema=true");
		}

		StringBuilder sb = new StringBuilder();
		StringBuilder sbx = new StringBuilder();
		int counter = 0;
		int soapBodyFoundCounter = 0;
		for (Object o : definition.getTypes().getExtensibilityElements()) {
			if (o instanceof Schema) {
				Schema schema = (Schema) o;
				String tns = schema.getElement()
						.getAttribute("targetNamespace");
				NodeList childNodes = schema.getElement().getChildNodes();
				boolean soapBodyFound = false;
				for (int i = 0; i < childNodes.getLength(); i++) {
					Node n = childNodes.item(i);
					if (n.getNodeType() == Node.ELEMENT_NODE
							&& n.getLocalName().equals("element")) {
						String name = n.getAttributes().getNamedItem("name")
								.getNodeValue();
						if (getSoapBody().equals(name)) {
							soapBodyFound = true;
							soapBodyFoundCounter++;
							if (StringUtils.isNotEmpty(getSoapBodyNamespace())) {
								tns = getSoapBodyNamespace();
							}
						}
					}
				}
				if (sb.length() > 0) {
					sb.append(" ");
					sbx.append(" ");
				}
				sb.append(tns);
				if (soapBodyFound) {
					sbx.append(".*");
				} else {
					sbx.append(Pattern.quote(tns));
				}
				sb.append(" ");
				sbx.append(" ");
				String schemaWithNumber = RESOURCE_INTERNAL_REFERENCE_PREFIX + ++counter; 
				sb.append(schemaWithNumber);
				sbx.append(schemaWithNumber);
			}
		}

		if (StringUtils.isNotEmpty(getSoapBodyNamespace())
				&& soapBodyFoundCounter > 1) {
			throw new ConfigurationException("soapBody [" + getSoapBody() + "] exists multiple times, not possible to create schemaLocation from soapBodyNamespace");

		}		
		
		if (sb.length() > 0) {
			String wsdlSchemaLocation = sb.toString();
			if (StringUtils.isNotEmpty(getSchemaLocation()) && isAddNamespaceToSchema()) {
				String formattedSchemaLocation = getFormattedSchemaLocation(getSchemaLocation());
				if (formattedSchemaLocation.equals(wsdlSchemaLocation)) {
					ConfigurationWarnings.add(this, log, "attribute [schemaLocation] for wsdl [" + getWsdl() + "] already has a default value [" + wsdlSchemaLocation + "]");
				} else {
					if (soapBodyFoundCounter == 1) {
						String wsdlSchemaLocationRegex = sbx.toString();
						if (formattedSchemaLocation.matches(wsdlSchemaLocationRegex)) {
							ConfigurationWarnings.add(this, log, "use attribute [soapBodyNamespace] instead of attribute [schemaLocation] with value [" + wsdlSchemaLocation + "] for wsdl [" + getWsdl() + "]");
						}
					}
				}
			}
			if (StringUtils.isNotEmpty(getSoapBodyNamespace())) {
				setSchemaLocation(wsdlSchemaLocation);
			}
		}

		super.configure();
	}

	private static String getFormattedSchemaLocation(String schemaLocation) {
		List<SchemaLocation> schemaLocationList = new ArrayList<SchemaLocation>();
		String[] schemaLocationArray = schemaLocation.trim().split("\\s+");
		for (int i = 0; i < schemaLocationArray.length; i++) {
			String namespace = schemaLocationArray[i];
			String schema = "";
			if (i + 1 < schemaLocationArray.length) {
				i++;
				schema = schemaLocationArray[i];
			}
			schemaLocationList.add(new SchemaLocation(namespace, schema));
		}
		Collections.sort(schemaLocationList);
		StringBuilder sb = new StringBuilder();
		for (SchemaLocation schemaLocationItem : schemaLocationList) {
			if (sb.length() > 0) {
				sb.append(" ");
			}
			sb.append(schemaLocationItem.toString());
		}
		return sb.toString();
	}
	
	@Override
	protected void checkSchemaSpecified() throws ConfigurationException {
		if (StringUtils.isEmpty(getWsdl())) {
			throw new ConfigurationException("wsdl attribute cannot be empty");
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
		SoapVersion soapVersion = getSoapVersionEnum();
		if (soapVersion == null || soapVersion==SoapVersion.SOAP11 || soapVersion==SoapVersion.AUTO) {
			XSD xsd = new XSD();
			xsd.initNamespace(SoapVersion.SOAP11.namespace, this, SoapVersion.SOAP11.location);
			xsds.add(xsd);
		}
		if (soapVersion==SoapVersion.SOAP12 || soapVersion==SoapVersion.AUTO) {
			XSD xsd = new XSD();
			xsd.initNamespace(SoapVersion.SOAP12.namespace, this, SoapVersion.SOAP12.location);
			xsds.add(xsd);
		}
		if (StringUtils.isNotEmpty(getSchemaLocationToAdd())) {
			StringTokenizer st = new StringTokenizer(getSchemaLocationToAdd(), ", \t\r\n\f");
			while (st.hasMoreTokens()) {
				XSD xsd = new XSD();
				xsd.initNamespace(st.nextToken(), this, st.hasMoreTokens() ? st.nextToken():null);
				xsds.add(xsd);
			}
		}
		List<Schema> schemas = new ArrayList<Schema>();
		List<ExtensibilityElement> types = definition.getTypes().getExtensibilityElements();
		for (Iterator<ExtensibilityElement> i = types.iterator(); i.hasNext();) {
			ExtensibilityElement type = i.next();
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
		if (StringUtils.isEmpty(getSchemaLocation())) {
			filteredSchemas = schemas;
		} else {
			filteredSchemas = new ArrayList<Schema>();
			filteredReferences = new HashMap<Schema, String>();
			filteredNamespaces = new HashMap<Schema, String>();
			String[] split =  getSchemaLocation().trim().split("\\s+");
			if (split.length % 2 != 0) throw new ConfigurationException("The schema must exist from an even number of strings, but it is [" + getSchemaLocation() +"]");
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
			xsd.setWsdlSchema(definition, schema);
			if (StringUtils.isNotEmpty(getSchemaLocation())) {
				xsd.setResourceInternalReference(filteredReferences.get(schema));
//				xsd.setNamespace(filteredNamespaces.get(schema));
			} else {
				xsd.setResourceInternalReference(RESOURCE_INTERNAL_REFERENCE_PREFIX + (filteredSchemas.indexOf(schema) + 1));
			}
			xsd.setAddNamespaceToSchema(isAddNamespaceToSchema());
			xsd.setImportedSchemaLocationsToIgnore(getImportedSchemaLocationsToIgnore());
			xsd.setUseBaseImportedSchemaLocationsToIgnore(isUseBaseImportedSchemaLocationsToIgnore());
			xsd.setImportedNamespacesToIgnore(getImportedNamespacesToIgnore());
			xsd.initNamespace(StringUtils.isNotEmpty(getSchemaLocation())?filteredNamespaces.get(schema):null,this, getWsdl());
			xsds.add(xsd);
		}
		return xsds;
	}

	public String toExtendedString() {
		return "[" + getConfigurationClassLoader() + "][" + FilenameUtils.normalize(getWsdl()) + "][" + getSoapBody() + "][" + getOutputSoapBody() + "][" + getSoapBodyNamespace() + "]";
	}
	
	@IbisDoc({"1", "pairs of uri references which will be added to the wsdl", " "})
	public void setSchemaLocationToAdd(String schemaLocationToAdd) {
		this.schemaLocationToAdd = schemaLocationToAdd;
	}

	public String getSchemaLocationToAdd() {
		return schemaLocationToAdd;
	}

	@IbisDoc({"2", "creates <code>schemalocation</code> attribute based on the wsdl and replaces the namespace of the soap body element", " " })
	public void setSoapBodyNamespace(String soapBodyNamespace) {
		this.soapBodyNamespace = soapBodyNamespace;
	}

	public String getSoapBodyNamespace() {
		return soapBodyNamespace;
	}
}

class ClassLoaderWSDLLocator implements WSDLLocator, IScopeProvider {
	private @Getter ClassLoader configurationClassLoader = null;
	private String wsdl;
	private URL url;
	private IOException ioException;
	private String latestImportURI;

	ClassLoaderWSDLLocator(WsdlXmlValidator wsdlXmlValidator, String wsdl) {
		configurationClassLoader = wsdlXmlValidator.getConfigurationClassLoader();
		this.wsdl = wsdl;
		url = ClassUtils.getResourceURL(this, wsdl);
	}

	public URL getUrl() {
		return url;
	}

	public IOException getIOException() {
		return ioException;
	}

	@Override
	public InputSource getBaseInputSource() {
		return getInputSource(url);
	}

	@Override
	public String getBaseURI() {
		return wsdl;
	}

	@Override
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

	@Override
	public String getLatestImportURI() {
		return latestImportURI;
	}

	@Override
	public void close() {
	}

	private InputSource getInputSource(String resource) {
		return getInputSource(ClassUtils.getResourceURL(this, resource));
	}

	private InputSource getInputSource(URL url) {
		InputStream inputStream = null;
		InputSource source = null;
		if (url != null) {
			try {
				inputStream = url.openStream();
			} catch (IOException e) {
				ioException = e;
			}
			if (inputStream != null) {
				source = new InputSource(inputStream);
				source.setSystemId(url.toString());
			}
		}
		return source;
	}
}

class SchemaLocation implements Comparable<SchemaLocation> {
	private String namespace;
	private String schema;
	private String schemaFormatted;

	SchemaLocation(String namespace, String schema) {
		this.namespace = namespace;
		this.schema = schema;
		if (StringUtils.isNotEmpty(schema) && schema.startsWith(
				WsdlXmlValidator.RESOURCE_INTERNAL_REFERENCE_PREFIX)) {
			String schemaNumberString = StringUtils.substringAfter(schema,
					WsdlXmlValidator.RESOURCE_INTERNAL_REFERENCE_PREFIX);
			if (StringUtils.isNumeric(schemaNumberString)) {
				this.schemaFormatted = WsdlXmlValidator.RESOURCE_INTERNAL_REFERENCE_PREFIX
						+ StringUtils.leftPad(schemaNumberString, 3, "0");
			} else {
				this.schemaFormatted = schema;
			}
		} else {
			this.schemaFormatted = schema;
		}
	}

	@Override
	public int compareTo(SchemaLocation schemaLocation) {
		return schemaFormatted.compareTo(schemaLocation.schemaFormatted);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof SchemaLocation) {
			SchemaLocation other = (SchemaLocation) o;
			if (compareTo(other) == 0) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public String toString() {
		return namespace + " " + schema;
	}
}
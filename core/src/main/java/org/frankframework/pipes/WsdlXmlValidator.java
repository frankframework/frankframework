/*
   Copyright 2013-2019 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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
package org.frankframework.pipes;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Part;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLLocator;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.IScopeProvider;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.SharedWsdlDefinitions;
import org.frankframework.soap.SoapValidator;
import org.frankframework.soap.SoapVersion;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.validation.IXSD;
import org.frankframework.validation.SchemaUtils;
import org.frankframework.validation.XmlValidatorException;
import org.frankframework.validation.xsd.ResourceXsd;
import org.frankframework.validation.xsd.WsdlXsd;

/**
 * XmlValidator that will read the XSD's to use from a WSDL. As it extends the
 * SoapValidator is will also add the SOAP envelope XSD.
 *
 * @author Michiel Meeuwissen
 * @author Jaco de Groot
 */
public class WsdlXmlValidator extends SoapValidator {
	private static final Logger LOG = LogUtil.getLogger(WsdlXmlValidator.class);

	private static final WSDLFactory FACTORY;
	public static final String RESOURCE_INTERNAL_REFERENCE_PREFIX = "schema";

	private @Getter String soapBodyNamespace = "";
	private @Getter String wsdl;
	private @Getter String schemaLocationToAdd;

	private @Setter SharedWsdlDefinitions sharedWsdlDefinitions;
	private Definition definition;


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

	@Override
	public void configure() throws ConfigurationException {
		addSoapEnvelopeToSchemaLocation = false;

		definition = sharedWsdlDefinitions.getOrCompute(wsdl, this::getDefinition);

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
			if (!(o instanceof Schema schema)) {
				continue;
			}
			String tns = schema.getElement().getAttribute("targetNamespace");
			NodeList childNodes = schema.getElement().getChildNodes();
			boolean soapBodyFound = false;
			for (int i = 0; i < childNodes.getLength(); i++) {
				Node n = childNodes.item(i);
				if (n.getNodeType() == Node.ELEMENT_NODE && "element".equals(n.getLocalName())) {
					String name = n.getAttributes().getNamedItem("name").getNodeValue();
					if (getSoapBody().equals(name)) {
						soapBodyFound = true;
						soapBodyFoundCounter++;
						if (StringUtils.isNotEmpty(getSoapBodyNamespace())) {
							tns = getSoapBodyNamespace();
						}
					}
				}
			}
			if (!sb.isEmpty()) {
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

		if (StringUtils.isNotEmpty(getSoapBodyNamespace()) && soapBodyFoundCounter > 1) {
			throw new ConfigurationException("soapBody [" + getSoapBody() + "] exists multiple times, not possible to create schemaLocation from soapBodyNamespace");
		}

		if (!sb.isEmpty()) {
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

	protected Definition getDefinition(String wsdl) throws ConfigurationException {
		WSDLReader reader  = FACTORY.newWSDLReader();
		reader.setFeature("javax.wsdl.verbose", false);
		reader.setFeature("javax.wsdl.importDocuments", true);
		ClassLoaderWSDLLocator wsdlLocator = new ClassLoaderWSDLLocator(this, wsdl);
		URL url = wsdlLocator.getUrl();
		Definition definition;
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
		return definition;
	}

	@Override
	protected PipeForward validate(Message messageToValidate, PipeLineSession session, boolean responseMode, String messageRoot) throws XmlValidatorException, PipeRunException, ConfigurationException {
		String soapAction = session.get(SoapBindingConstants.SOAP_ACTION, "");
		if(StringUtils.isNotEmpty(soapAction)) {
			String soapBodyFromSoapAction = getSoapBodyFromSoapAction(soapAction, responseMode);
			if(soapBodyFromSoapAction == null) {
				log.debug("Could not determine messageRoot from soapAction original messageRoot [{}] will be used", messageRoot);
			} else if(!soapBodyFromSoapAction.equalsIgnoreCase(messageRoot)) {
				log.debug("messageRoot [{}] is determined from soapAction [{}]", soapBodyFromSoapAction, soapAction);
				messageRoot = soapBodyFromSoapAction;
			}
		}
		return super.validate(messageToValidate, session, responseMode, messageRoot);
	}

	@SuppressWarnings("unchecked")
	private String getSoapBodyFromSoapAction(String soapAction, boolean responseMode) {
		Map<QName, Binding> bindings = definition.getBindings();
		return bindings.values().stream()
				.flatMap(binding -> ((List<BindingOperation>) binding.getBindingOperations()).stream())
				.filter(bindingOperation -> isMatchingSoapAction(soapAction, bindingOperation))
				.findFirst()
				.map(bindingOperation -> responseMode ? bindingOperation.getOperation().getOutput().getMessage() : bindingOperation.getOperation().getInput().getMessage())
				.map(message -> (Collection<Part>)message.getParts().values())
				.map(WsdlXmlValidator::mapPartsToSoapBody)
				.orElse(null);
	}

	private static String mapPartsToSoapBody(Collection<Part> parts) {
		return parts.stream()
				.map(Part::getElementName)
				.map(QName::getLocalPart)
				.collect(Collectors.joining(","));
	}

	@SuppressWarnings("unchecked")
	private static boolean isMatchingSoapAction(String soapAction, BindingOperation bindingOperation) {
		return bindingOperation.getExtensibilityElements().stream()
				.filter(SOAPOperation.class::isInstance)
				.map(element -> ((SOAPOperation) element).getSoapActionURI())
				.anyMatch(soapActionFromDefinition -> soapActionFromDefinition.equals(soapAction));
	}

	private static String getFormattedSchemaLocation(String schemaLocation) {
		List<SchemaLocation> schemaLocationList = new ArrayList<>();
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
			if (!sb.isEmpty()) {
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
			String key = e.getKey().isEmpty() ? "xmlns" : ("xmlns:" + e.getKey());
			if (schema.getElement().getAttribute(key).isEmpty()) {
				schema.getElement().setAttribute(key, e.getValue());
			}
		}
	}

	@Override
	public String getSchemasId() {
		return wsdl;
	}

	@Override
	public Set<IXSD> getXsds() throws ConfigurationException {
		Set<IXSD> xsds = new LinkedHashSet<>();
		SoapVersion soapVersion = getSoapVersion();
		if (soapVersion == null || soapVersion==SoapVersion.SOAP11 || soapVersion==SoapVersion.AUTO) {
			ResourceXsd xsd = new ResourceXsd();
			xsd.initNamespace(SoapVersion.SOAP11.namespace, this, SoapVersion.SOAP11.location);
			xsds.add(xsd);
		}
		if (soapVersion==SoapVersion.SOAP12 || soapVersion==SoapVersion.AUTO) {
			ResourceXsd xsd = new ResourceXsd();
			xsd.initNamespace(SoapVersion.SOAP12.namespace, this, SoapVersion.SOAP12.location);
			xsds.add(xsd);
		}
		if (StringUtils.isNotEmpty(getSchemaLocationToAdd())) {
			StringTokenizer st = new StringTokenizer(getSchemaLocationToAdd(), ", \t\r\n\f");
			while (st.hasMoreTokens()) {
				ResourceXsd xsd = new ResourceXsd();
				xsd.initNamespace(st.nextToken(), this, st.hasMoreTokens() ? st.nextToken():null);
				xsds.add(xsd);
			}
		}
		List<Schema> schemas = new ArrayList<>();
		List<ExtensibilityElement> types = definition.getTypes().getExtensibilityElements();
		for (ExtensibilityElement type : types) {
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
			filteredSchemas = new ArrayList<>();
			filteredReferences = new HashMap<>();
			filteredNamespaces = new HashMap<>();
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
			WsdlXsd xsd = new WsdlXsd();
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

	/** The WSDL to read the XSDs from */
	public void setWsdl(String wsdl) {
		this.wsdl = wsdl;
	}

	/** Name of the child element of the SOAP body, or a comma separated list of names to choose from (only one is allowed) (WSDL generator will use the first element) (use empty value to allow an empty SOAP body, for example to allow element x and an empty SOAP body use: x,). In case the request contains SOAPAction header and the WSDL contains an element specific to that SOAPAction, it will use that element as SOAP body. */
	@Override
	public void setSoapBody(String soapBody) {
		super.setSoapBody(soapBody);
	}

	/** Pairs of URI references which will be added to the WSDL */
	public void setSchemaLocationToAdd(String schemaLocationToAdd) {
		this.schemaLocationToAdd = schemaLocationToAdd;
	}

	/** Creates <code>schemaLocation</code> attribute based on the WSDL and replaces the namespace of the soap body element */
	public void setSoapBodyNamespace(String soapBodyNamespace) {
		this.soapBodyNamespace = soapBodyNamespace;
	}

}

class ClassLoaderWSDLLocator implements WSDLLocator, IScopeProvider {
	private final @Getter ClassLoader configurationClassLoader;
	private final String wsdl;
	private final @Getter URL url;
	private @Getter IOException iOException;
	private @Getter String latestImportURI;

	ClassLoaderWSDLLocator(WsdlXmlValidator wsdlXmlValidator, String wsdl) {
		configurationClassLoader = wsdlXmlValidator.getConfigurationClassLoader();
		this.wsdl = wsdl;
		url = ClassLoaderUtils.getResourceURL(this, wsdl);
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
		if (i == -1 || importLocation.startsWith("/")) {
			latestImportURI = importLocation;
		} else {
			latestImportURI = parentLocation.substring(0, i + 1) + importLocation;
		}
		return getInputSource(latestImportURI);
	}


	@Override
	public void close() {
	}

	private InputSource getInputSource(String resource) {
		return getInputSource(ClassLoaderUtils.getResourceURL(this, resource));
	}

	private InputSource getInputSource(URL url) {
		InputStream inputStream = null;
		InputSource source = null;
		if (url != null) {
			try {
				inputStream = url.openStream();
			} catch (IOException e) {
				iOException = e;
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
	private final String namespace;
	private final String schema;
	private final String schemaFormatted;

	SchemaLocation(String namespace, String schema) {
		this.namespace = namespace;
		this.schema = schema;
		if (StringUtils.isNotEmpty(schema) && schema.startsWith(
				WsdlXmlValidator.RESOURCE_INTERNAL_REFERENCE_PREFIX)) {
			String schemaNumberString = StringUtils.substringAfter(schema,
					WsdlXmlValidator.RESOURCE_INTERNAL_REFERENCE_PREFIX);
			if (StringUtils.isNumeric(schemaNumberString)) {
				this.schemaFormatted = WsdlXmlValidator.RESOURCE_INTERNAL_REFERENCE_PREFIX + StringUtils.leftPad(schemaNumberString, 3, "0");
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
		if (o instanceof SchemaLocation other) {
			return compareTo(other) == 0;
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

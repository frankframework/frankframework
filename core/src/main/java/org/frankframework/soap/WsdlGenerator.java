/*
   Copyright 2013, 2015, 2016 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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
package org.frankframework.soap;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IListener;
import org.frankframework.core.IXmlValidator;
import org.frankframework.core.PipeLine;
import org.frankframework.http.WebServiceListener;
import org.frankframework.jms.JmsListener;
import org.frankframework.receivers.JavaListener;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.StreamUtil;
import org.frankframework.validation.IXSD;
import org.frankframework.validation.SchemaUtils;
import org.frankframework.validation.AbstractXSD;
import org.frankframework.validation.xsd.ResourceXsd;

/**
 *  Utility class to generate the WSDL. Straight-forwardly implemented using stax only.
 *
 *  An object of this class represents the WSDL associated with one IBIS pipeline.
 *
 * @author  Michiel Meeuwissen
 * @author  Jaco de Groot
 */
public class WsdlGenerator {
	protected Logger log = LogUtil.getLogger(this);

	protected static final String WSDL_NAMESPACE				 = "http://schemas.xmlsoap.org/wsdl/";
	protected static final String WSDL_NAMESPACE_PREFIX		  = "wsdl";
	protected static final String XSD_NAMESPACE				  = SchemaUtils.XSD;
	protected static final String XSD_NAMESPACE_PREFIX		   = "xsd";
	protected static final String WSDL_SOAP_NAMESPACE			= "http://schemas.xmlsoap.org/wsdl/soap/";
	protected static final String WSDL_SOAP_NAMESPACE_PREFIX	 = "soap";
	protected static final String WSDL_SOAP12_NAMESPACE		  = "http://schemas.xmlsoap.org/wsdl/soap12/";
	protected static final String WSDL_SOAP12_NAMESPACE_PREFIX   = "soap12";
	protected static final String SOAP_HTTP_NAMESPACE			= "http://schemas.xmlsoap.org/soap/http";
	protected static final String SOAP_JMS_NAMESPACE			 = "http://www.w3.org/2010/soapjms/";
	public static final String SOAP_JMS_NAMESPACE_PREFIX	  = "jms";
	protected static final String TARGET_NAMESPACE_PREFIX		= "tns";

	public static final String WSDL_EXTENSION					= ".wsdl";

	protected static final List<String> excludeXsds = new ArrayList<>();
	static {
		excludeXsds.add(SoapVersion.SOAP11.namespace); // SOAP envelope 1.1
		excludeXsds.add(SoapVersion.SOAP12.namespace); // SOAP envelope 1.2
	}

	private boolean indent = true;
	private boolean useIncludes = false;

	private final String name;
	private final String fileName;
	private final PipeLine pipeLine;
	private final IXmlValidator inputValidator;
	private final IXmlValidator outputValidator;
	private String webServiceListenerNamespace;
	private Set<IXSD> xsds;
	private Set<IXSD> rootXsds;
	private Map<String, Set<IXSD>> xsdsGroupedByNamespace;
	private LinkedHashMap<IXSD, String> prefixByXsd;
	private LinkedHashMap<String, String> namespaceByPrefix;
	private String inputRoot;
	private String outputRoot;
	private QName inputHeaderElement;
	private boolean inputHeaderIsOptional = false;
	private QName inputBodyElement;
	private QName outputHeaderElement;
	private boolean outputHeaderIsOptional = false;
	private QName outputBodyElement;

	private boolean httpActive = false;
	private boolean jmsActive = false;

	private final String targetNamespace;
	private String targetNamespacePrefix = TARGET_NAMESPACE_PREFIX;
	private String wsdlSoapNamespace = WSDL_SOAP_NAMESPACE;
	private String wsdlSoapPrefix = WSDL_SOAP_NAMESPACE_PREFIX;

	private WsdlGeneratorExtensionContext extensionContext = null;

	private String documentation;

	private final List<String> warnings = new ArrayList<>();

	public WsdlGenerator(PipeLine pipeLine) {
		this(pipeLine, null);
	}

	public WsdlGenerator(PipeLine pipeLine, String generationInfo) {
		this.pipeLine = pipeLine;
		this.name = this.pipeLine.getAdapter().getName();
		if (this.name == null) {
			throw new IllegalArgumentException("Adapter has no name");
		}
		inputValidator = (IXmlValidator)pipeLine.getInputValidator();
		if(inputValidator == null) {
			throw new IllegalStateException("No inputvalidator provided");
		}

		if (inputValidator.getConfigurationException() != null) {
			if (inputValidator.getConfigurationException().getMessage() != null) {
				throw new IllegalStateException(inputValidator.getConfigurationException().getMessage());
			}
			throw new IllegalStateException(inputValidator.getConfigurationException().toString());
		}
		outputValidator = (IXmlValidator)pipeLine.getOutputValidator();
		if (outputValidator != null && outputValidator.getConfigurationException() != null) {
			if (outputValidator.getConfigurationException().getMessage() != null) {
				throw new IllegalStateException(outputValidator.getConfigurationException().getMessage());
			}
			throw new IllegalStateException(outputValidator.getConfigurationException().toString());
		}
		String fileName = getName();
		AppConstants appConstants = AppConstants.getInstance(pipeLine.getAdapter().getConfigurationClassLoader());
		String tns = appConstants.getProperty("wsdl." + getName() + ".targetNamespace");
		if (tns == null) {
			tns = appConstants.getProperty("wsdl.targetNamespace");
		}
		if (tns == null) {
			if (inputValidator instanceof WsdlGeneratorExtension wsdlGeneratorExtension) {
				extensionContext = wsdlGeneratorExtension.buildExtensionContext(pipeLine);

				fileName = extensionContext.getFilename();
				tns = extensionContext.getTNS();
				warnings.addAll(extensionContext.getWarnings());
			}
			if (tns == null) {
				for(IListener<?> listener : WsdlGeneratorUtils.getListeners(pipeLine.getAdapter())) {
					if (listener instanceof WebServiceListener serviceListener) {
						webServiceListenerNamespace = serviceListener.getServiceNamespaceURI();
						tns = webServiceListenerNamespace;
					}
				}
				if (tns == null) {
					tns = WsdlGeneratorUtils.getFirstNamespaceFromSchemaLocation(inputValidator);
				}
				if (tns != null) {
					if (tns.endsWith("/")) {
						tns = tns + "wsdl/";
					} else {
						tns = tns + "/wsdl/";
					}
				} else {
					tns = "${wsdl." + getName() + ".targetNamespace}";
				}
			}
		}
		if (inputValidator.getSchema() != null && webServiceListenerNamespace == null) {
			throw new IllegalStateException("Adapter has an inputValidator using the schema attribute in which case a WebServiceListener with serviceNamespaceURI is required");
		}
		if (outputValidator != null && (outputValidator.getSchema() != null && webServiceListenerNamespace == null)) {
				throw new IllegalStateException("Adapter has an outputValidator using the schema attribute in which case a WebServiceListener with serviceNamespaceURI is required");

		}
		this.fileName = fileName;
		this.targetNamespace = WsdlGeneratorUtils.validUri(tns);
		if (inputValidator instanceof SoapValidator validator && validator.getSoapVersion()==SoapVersion.SOAP12) {
			wsdlSoapNamespace = WSDL_SOAP12_NAMESPACE;
			wsdlSoapPrefix = WSDL_SOAP12_NAMESPACE_PREFIX;
		}
		if (StringUtils.isEmpty(getDocumentation())) {
			documentation = "Generated" + (generationInfo != null ? " " + generationInfo : "") + " as " + getFilename() + WSDL_EXTENSION + " on " + DateFormatUtils.getTimeStamp() + ".";
		}
		if (inputValidator.getDocumentation() != null) {
			documentation = documentation + inputValidator.getDocumentation();
		}
	}

	public String getName() {
		return name;
	}

	public String getFilename() {
		return fileName;
	}

	public String getTargetNamespace() {
		return targetNamespace;
	}

	public String getDocumentation() {
		return documentation;
	}

	public void setDocumentation(String documentation) {
		this.documentation = documentation;
	}

	public boolean isIndent() {
		return indent;
	}

	public void setIndent(boolean indent) {
		this.indent = indent;
	}

	public boolean isUseIncludes() {
		return useIncludes;
	}

	public void setUseIncludes(boolean useIncludes) {
		this.useIncludes = useIncludes;
	}

	public String getTargetNamespacePrefix() {
		return targetNamespacePrefix;
	}

	public void setTargetNamespacePrefix(String targetNamespacePrefix) {
		this.targetNamespacePrefix = targetNamespacePrefix;
	}

	public void init() throws ConfigurationException {
		Set<IXSD> outputXsds = new HashSet<>();
		xsds = new HashSet<>();
		rootXsds = new HashSet<>();
		Set<IXSD> inputRootXsds = new HashSet<>(getXsds(inputValidator));
		rootXsds.addAll(inputRootXsds);
		Set<IXSD> inputXsds = new HashSet<>(AbstractXSD.getXsdsRecursive(inputRootXsds));
		xsds.addAll(inputXsds);
		if (outputValidator != null) {
			Set<IXSD> outputRootXsds = new HashSet<>(getXsds(outputValidator));
			rootXsds.addAll(outputRootXsds);
			outputXsds.addAll(AbstractXSD.getXsdsRecursive(outputRootXsds));
			xsds.addAll(outputXsds);
		}
		prefixByXsd = new LinkedHashMap<>();
		namespaceByPrefix = new LinkedHashMap<>();
		int prefixCount = 1;
		xsdsGroupedByNamespace =
				SchemaUtils.groupXsdsByNamespace(xsds, true);
		for (Map.Entry<String, Set<IXSD>> entry: xsdsGroupedByNamespace.entrySet()) {
			// When a schema has targetNamespace="http://www.w3.org/XML/1998/namespace"
			// it needs to be ignored as prefix xml is the only allowed prefix
			// for namespace http://www.w3.org/XML/1998/namespace. The xml
			// prefix doesn't have to be declared as the prefix xml is by
			// definition bound to the namespace name http://www.w3.org/XML/1998/namespace
			// (see http://www.w3.org/TR/xml-names/#ns-decl).
			String namespace = entry.getKey();
			if ("http://www.w3.org/XML/1998/namespace".equals(namespace)) {
				continue;
			}
			for (IXSD xsd: entry.getValue()) {
				prefixByXsd.put(xsd, "ns" + prefixCount);
			}
			namespaceByPrefix.put("ns" + prefixCount, namespace);
			prefixCount++;
		}
		for (IXSD xsd : xsds) {
			if (StringUtils.isEmpty(xsd.getTargetNamespace()) && !xsd.isAddNamespaceToSchema()) {
				warn("XSD '" + xsd + "' doesn't have a targetNamespace and addNamespaceToSchema is false");
			}
		}
		inputRoot = getRoot(inputValidator);
		inputHeaderElement = getHeaderElement(inputValidator, inputXsds);
		inputHeaderIsOptional = isHeaderOptional(inputValidator);
		inputBodyElement = getBodyElement(inputValidator, inputXsds, "inputValidator");
		if (outputValidator != null) {
			outputRoot = getRoot(outputValidator);
			outputHeaderElement = getHeaderElement(outputValidator, outputXsds);
			outputHeaderIsOptional = isHeaderOptional(outputValidator);
			outputBodyElement = getBodyElement(outputValidator, outputXsds, "outputValidator");
		}
		for (IListener<?> listener : WsdlGeneratorUtils.getListeners(pipeLine.getAdapter())) {
			if (listener instanceof WebServiceListener) {
				httpActive = true;
			} else if (listener instanceof JmsListener) {
				jmsActive = true;
			} else if (listener instanceof JavaListener jl) {
				if (jl.isHttpWsdl()) httpActive = true;
			}
		}
	}

	protected boolean isHeaderOptional(IXmlValidator xmlValidator) {
		if (xmlValidator instanceof SoapValidator validator) {
			String root = validator.getSoapHeader();
			if (StringUtils.isNotEmpty(root)) {
				String[] roots = root.trim().split(",", -1);
				for (String s : roots) {
					if (s.trim().isEmpty()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public Set<IXSD> getXsds(IXmlValidator xmlValidator) throws ConfigurationException {
		Set<IXSD> xsds = new HashSet<>();
		String inputSchema = xmlValidator.getSchema();
		if (inputSchema != null) {
			// In case of a WebServiceListener using soap=true it might be
			// valid to use the schema attribute (in which case the schema
			// doesn't have a namespace) as the WebServiceListener will
			// remove the soap envelop and body element before it is
			// validated. In this case we use the serviceNamespaceURI from
			// the WebServiceListener as the namespace for the schema.
			ResourceXsd xsd = new ResourceXsd();
			//xsd.setNamespace(webServiceListenerNamespace);
			xsd.setAddNamespaceToSchema(true);
			xsd.initNamespace(webServiceListenerNamespace, pipeLine, inputSchema);
			xsds.add(xsd);
		} else {
			xsds = xmlValidator.getXsds();
			Set<IXSD> remove = new HashSet<>();
			for (IXSD xsd : xsds) {
				if (excludeXsds.contains(xsd.getNamespace())) {
					remove.add(xsd);
				}
			}
			xsds.removeAll(remove);
		}
		return xsds;
	}

	/**
	 * Generates a zip file (and writes it to the given outputstream), containing
	 * the WSDL and all referenced XSD's.
	 *
	 * @see #wsdl(java.io.OutputStream, String)
	 */
	public void zip(OutputStream stream, String defaultLocation) throws IOException, ConfigurationException, XMLStreamException {
		try (ZipOutputStream out = new ZipOutputStream(stream)) {

			// First an entry for the WSDL itself:
			ZipEntry wsdlEntry = new ZipEntry(getFilename() + ".wsdl");
			out.putNextEntry(wsdlEntry);
			wsdl(out, defaultLocation);
			out.closeEntry();

			// And then all XSD's
			Set<String> entries = new HashSet<>();
			for (IXSD xsd : xsds) {
				String zipName = xsd.getResourceTarget();
				if (entries.add(zipName)) {
					ZipEntry xsdEntry = new ZipEntry(zipName);
					out.putNextEntry(xsdEntry);
					XMLStreamWriter writer = WsdlGeneratorUtils.getWriter(out, false);
					SchemaUtils.writeStandaloneXsd(xsd, writer);
					writer.flush();
					out.closeEntry();
				} else {
					warn("Duplicate xsds in " + this + " " + xsd + " " + xsds);
				}
			}
		}
	}

	/**
	 * Writes the WSDL to an output stream
	 */
	public void wsdl(OutputStream out, String defaultLocation) throws XMLStreamException, IOException, ConfigurationException {
		XMLStreamWriter w = WsdlGeneratorUtils.getWriter(out, isIndent());

		w.writeStartDocument(StreamUtil.DEFAULT_INPUT_STREAM_ENCODING, "1.0");
		w.setPrefix(WSDL_NAMESPACE_PREFIX, WSDL_NAMESPACE);
		w.setPrefix(XSD_NAMESPACE_PREFIX, XSD_NAMESPACE);
		w.setPrefix(wsdlSoapPrefix, wsdlSoapNamespace);
		if (jmsActive) {
			if (extensionContext != null) {
				extensionContext.setExtensionNamespacePrefixes(w);
			} else {
				w.setPrefix(SOAP_JMS_NAMESPACE_PREFIX, SOAP_JMS_NAMESPACE);
			}
		}
		w.setPrefix(getTargetNamespacePrefix(), getTargetNamespace());
		for (Map.Entry<String, String> entry : namespaceByPrefix.entrySet()) {
			w.setPrefix(entry.getKey(), entry.getValue());
		}
		w.writeStartElement(WSDL_NAMESPACE, "definitions"); {
			w.writeNamespace(WSDL_NAMESPACE_PREFIX, WSDL_NAMESPACE);
			w.writeNamespace(XSD_NAMESPACE_PREFIX, XSD_NAMESPACE);
			w.writeNamespace(wsdlSoapPrefix, wsdlSoapNamespace);
			if (extensionContext != null) {
				extensionContext.addExtensionNamespaces(w);
			}
			w.writeNamespace(getTargetNamespacePrefix(), getTargetNamespace());
			for (Map.Entry<String, String> entry: namespaceByPrefix.entrySet()) {
				w.writeNamespace(entry.getKey(), entry.getValue());
			}
			w.writeAttribute("targetNamespace", getTargetNamespace());

			documentation(w);
			types(w);
			messages(w);
			portType(w);
			binding(w);
			service(w, defaultLocation);
		}
		warnings(w);
		w.writeEndDocument();
		w.close();
	}

	/**
	 * Outputs a 'documentation' section of the WSDL
	 */
	protected void documentation(XMLStreamWriter w) throws XMLStreamException {
		if (documentation != null) {
			w.writeStartElement(WSDL_NAMESPACE, "documentation");
			w.writeCharacters(documentation);
			w.writeEndElement();
		}
	}

	/**
	 * Output the 'types' section of the WSDL
	 * @param w Writer to which XML is written.
	 * @throws XMLStreamException Thrown is there is an exception writing to stream.
	 * @throws IOException Thrown if there's an IOException during writing.
	 * @throws ConfigurationException Thrown if there's an error during configuration.
	 */
	protected void types(XMLStreamWriter w) throws XMLStreamException, IOException, ConfigurationException {
		w.writeStartElement(WSDL_NAMESPACE, "types");
		if (isUseIncludes()) {
			SchemaUtils.mergeRootXsdsGroupedByNamespaceToSchemasWithIncludes(
					SchemaUtils.groupXsdsByNamespace(rootXsds, true), w);
		}  else {
			SchemaUtils.mergeXsdsGroupedByNamespaceToSchemasWithoutIncludes(pipeLine, xsdsGroupedByNamespace, w);
		}
		w.writeEndElement();
	}

	/**
	 * Outputs the 'messages' section.
	 * @param w Writer to which XML is written.
	 * @throws XMLStreamException Thrown is there is an exception writing to stream.
	 *
	 */
	protected void messages(XMLStreamWriter w) throws XMLStreamException {
		List<QName> parts = new ArrayList<>();
		addHeaderElement(w, parts, inputHeaderElement, inputHeaderIsOptional, inputBodyElement, inputRoot);
		if (outputValidator != null) {
			parts.clear();
			addHeaderElement(w, parts, outputHeaderElement, outputHeaderIsOptional, outputBodyElement, outputRoot);
		}
	}

	private void addHeaderElement(XMLStreamWriter w, List<QName> parts, QName headerElement, boolean headerIsOptional, QName bodyElement, String root) throws XMLStreamException {
		if (headerElement != null && !headerIsOptional) {
			parts.add(headerElement);
		}
		if (bodyElement != null) {
			parts.add(bodyElement);
		}
		message(w, root, parts);
		if (headerIsOptional) {
			parts.clear();
			if (headerElement != null) {
				parts.add(headerElement);
				message(w, root + "_" + headerElement.getLocalPart(), parts);
			}
		}
	}

	protected void message(XMLStreamWriter w, String root, Collection<QName> parts) throws XMLStreamException {
		if (!parts.isEmpty()) {
			w.writeStartElement(WSDL_NAMESPACE, "message");
			w.writeAttribute("name", "Message_" + root); {
				for (QName part : parts) {
					w.writeEmptyElement(WSDL_NAMESPACE, "part");
					w.writeAttribute("name", "Part_" + part.getLocalPart());
					String type = part.getPrefix() + ":" + part.getLocalPart();
					w.writeAttribute("element", type);
				}
			}
			w.writeEndElement();
		}
	}

	protected void portType(XMLStreamWriter w) throws XMLStreamException {
		w.writeStartElement(WSDL_NAMESPACE, "portType");
		w.writeAttribute("name", "PortType_" + getName()); {
			for (IListener<?> listener : WsdlGeneratorUtils.getListeners(pipeLine.getAdapter())) {
				if (listener instanceof WebServiceListener || listener instanceof JmsListener) {
					w.writeStartElement(WSDL_NAMESPACE, "operation");
					w.writeAttribute("name", "Operation_" + WsdlGeneratorUtils.getNCName(getSoapAction(listener))); {
						if (StringUtils.isNotEmpty(inputRoot)) {
							w.writeEmptyElement(WSDL_NAMESPACE, "input");
							w.writeAttribute("message", getTargetNamespacePrefix() + ":" + "Message_" + inputRoot);
						}
						if (StringUtils.isNotEmpty(outputRoot)) {
							w.writeEmptyElement(WSDL_NAMESPACE, "output");
							w.writeAttribute("message", getTargetNamespacePrefix() + ":" + "Message_" + outputRoot);
						}
					}
					w.writeEndElement();
				}
			}
		}
		w.writeEndElement();
	}

	protected String getSoapAction(IListener<?> listener) {
		AppConstants appConstants = AppConstants.getInstance(pipeLine.getAdapter().getConfiguration().getClassLoader());
		String sa = appConstants.getProperty("wsdl." + getName() + "." + listener.getName() + ".soapAction");
		if (sa != null) {
			return sa;
		}
		sa = appConstants.getProperty("wsdl." + getName() + ".soapAction");
		if (sa != null) {
			return sa;
		}
		sa = appConstants.getProperty("wsdl.soapAction");
		if (sa != null) {
			return sa;
		}
		if (extensionContext != null && extensionContext.hasSOAPActionName()) {
			return extensionContext.getSOAPActionName();
		}
		return "${wsdl." + getName() + "." + listener.getName() + ".soapAction}";
	}

	protected String getLocation(String defaultLocation) {
		AppConstants appConstants = AppConstants.getInstance(pipeLine.getAdapter().getConfiguration().getClassLoader());
		String sa = appConstants.getProperty("wsdl." + getName() + ".location");
		if (sa != null) {
			return sa;
		}
		sa = appConstants.getProperty("wsdl.location");
		if (sa != null) {
			return sa;
		}
		return defaultLocation;
	}

	protected void binding(XMLStreamWriter w) throws XMLStreamException {
		String httpPrefix = "";
		String jmsPrefix = "";
		if (httpActive && jmsActive) {
			httpPrefix = "Http";
			jmsPrefix = "Jms";
		}
		if (httpActive) {
			httpBinding(w, httpPrefix);
		}
		if (jmsActive) {
			jmsBinding(w, jmsPrefix);
		}
	}

	protected void httpBinding(XMLStreamWriter w, String namePrefix) throws XMLStreamException {
		w.writeStartElement(WSDL_NAMESPACE, "binding");
		w.writeAttribute("name", namePrefix + "Binding_" + WsdlGeneratorUtils.getNCName(getName()));
		w.writeAttribute("type", getTargetNamespacePrefix() + ":" + "PortType_" + getName()); {
			w.writeEmptyElement(wsdlSoapNamespace, "binding");
			w.writeAttribute("transport", SOAP_HTTP_NAMESPACE);
			w.writeAttribute("style", "document");
			for (IListener<?> listener : WsdlGeneratorUtils.getListeners(pipeLine.getAdapter())) {
				if (listener instanceof WebServiceListener) {
					writeSoapOperation(w, listener);
				}
			}
		}
		w.writeEndElement();
	}

	public void writeSoapOperation(XMLStreamWriter w, IListener<?> listener) throws XMLStreamException {
		w.writeStartElement(WSDL_NAMESPACE, "operation");
		String soapAction = getSoapAction(listener);
		w.writeAttribute("name", "Operation_" + WsdlGeneratorUtils.getNCName(soapAction)); {
			w.writeEmptyElement(wsdlSoapNamespace, "operation");
			w.writeAttribute("style", "document");
			if(!soapAction.startsWith("${")) {
				w.writeAttribute("soapAction", soapAction);
			}
			w.writeStartElement(WSDL_NAMESPACE, "input"); {
				writeSoapHeader(w, inputRoot, inputHeaderElement, inputHeaderIsOptional);
				writeSoapBody(w, inputBodyElement);
			}
			w.writeEndElement();
			if (outputValidator != null) {
				w.writeStartElement(WSDL_NAMESPACE, "output"); {
					writeSoapHeader(w, outputRoot, outputHeaderElement, outputHeaderIsOptional);
					writeSoapBody(w, outputBodyElement);
				}
				w.writeEndElement();
			}
		}
		w.writeEndElement();
	}

	protected void writeSoapHeader(XMLStreamWriter w, String root, QName headerElement, boolean isHeaderOptional) throws XMLStreamException {
		if (headerElement != null) {
			w.writeEmptyElement(wsdlSoapNamespace, "header");
			w.writeAttribute("part", "Part_" + headerElement.getLocalPart());
			w.writeAttribute("use", "literal");
			w.writeAttribute("message", getTargetNamespacePrefix() + ":" + "Message_" + root + (isHeaderOptional ? "_" + headerElement.getLocalPart() : ""));
		}
	}

	protected void writeSoapBody(XMLStreamWriter w, QName bodyElement) throws XMLStreamException {
		if (bodyElement != null) {
			w.writeEmptyElement(wsdlSoapNamespace, "body");
			w.writeAttribute("parts", "Part_" + bodyElement.getLocalPart());
			w.writeAttribute("use", "literal");
		}
	}

	protected void jmsBinding(XMLStreamWriter w, String namePrefix) throws XMLStreamException {
		w.writeStartElement(WSDL_NAMESPACE, "binding");
		w.writeAttribute("name", namePrefix + "Binding_" + WsdlGeneratorUtils.getNCName(getName()));
		w.writeAttribute("type", getTargetNamespacePrefix() + ":" + "PortType_" + getName()); {
			w.writeEmptyElement(wsdlSoapNamespace, "binding");
			w.writeAttribute("style", "document");
			if (extensionContext != null) {
				extensionContext.addJmsBindingInfo(w, this, pipeLine);
			} else {
				w.writeAttribute("transport", SOAP_JMS_NAMESPACE);
			}
		}
		w.writeEndElement();
	}

	protected void service(XMLStreamWriter w, String defaultLocation) throws XMLStreamException {
		String httpPrefix = "";
		String jmsPrefix = "";
		if (httpActive && jmsActive) {
			httpPrefix = "Http";
			jmsPrefix = "Jms";
		}
		if (httpActive) {
			httpService(w, defaultLocation, httpPrefix);
		}
		if (jmsActive) {
			for (IListener<?> listener : WsdlGeneratorUtils.getListeners(pipeLine.getAdapter())) {
				if (listener instanceof JmsListener jmsListener) {
					jmsService(w, jmsListener, jmsPrefix);
				}
			}
		}
	}

	protected void httpService(XMLStreamWriter w, String defaultLocation, String namePrefix) throws XMLStreamException {
		w.writeStartElement(WSDL_NAMESPACE, "service");
		w.writeAttribute("name", "Service_" + WsdlGeneratorUtils.getNCName(getName())); {
			w.writeStartElement(WSDL_NAMESPACE, "port");
			w.writeAttribute("name", namePrefix + "Port_" + WsdlGeneratorUtils.getNCName(getName()));
			w.writeAttribute("binding", getTargetNamespacePrefix() + ":" + namePrefix + "Binding_" + WsdlGeneratorUtils.getNCName(getName())); {
				w.writeEmptyElement(wsdlSoapNamespace, "address");
				w.writeAttribute("location", getLocation(defaultLocation));
			}
			w.writeEndElement();
		}
		w.writeEndElement();
	}

	protected void jmsService(XMLStreamWriter w, JmsListener listener, String namePrefix) throws XMLStreamException {
		w.writeStartElement(WSDL_NAMESPACE, "service");
		w.writeAttribute("name", "Service_" + WsdlGeneratorUtils.getNCName(getName())); {
			if (extensionContext == null) {
				// Per example of https://docs.jboss.org/author/display/JBWS/SOAP+over+JMS
				w.writeStartElement(SOAP_JMS_NAMESPACE, "jndiConnectionFactoryName");
				w.writeCharacters(listener.getQueueConnectionFactoryName());
			}
			w.writeStartElement(WSDL_NAMESPACE, "port");
			w.writeAttribute("name", namePrefix + "Port_" + WsdlGeneratorUtils.getNCName(getName()));
			w.writeAttribute("binding", getTargetNamespacePrefix() + ":" + namePrefix + "Binding_" + WsdlGeneratorUtils.getNCName(getName())); {
				w.writeEmptyElement(wsdlSoapNamespace, "address");
				String destinationName = listener.getDestinationName();
				if (destinationName != null) {
					w.writeAttribute("location", getLocation(destinationName));
				}
				if (extensionContext != null) {
					extensionContext.addJmsServiceInfo(w, listener);
				}
			}
			w.writeEndElement();
		}
		w.writeEndElement();
	}

	protected void warnings(XMLStreamWriter w) throws XMLStreamException {
		for (String warning : warnings) {
			w.writeComment(warning);
		}
	}

	protected String getRoot(IXmlValidator xmlValidator) {
		return getRoot(xmlValidator, false);
	}

	protected String getRoot(IXmlValidator xmlValidator, boolean outputMode) {
		String rootSpecification = xmlValidator.getMessageRoot();
		if (StringUtils.isNotEmpty(rootSpecification) && rootSpecification.indexOf(',')>=0) {
			log.warn("validator [{}] is configured with multiple root elements [{}] in mode [{}]; will use only first", xmlValidator.getName(), rootSpecification, outputMode?"response":"request");
			rootSpecification = rootSpecification.split(",")[0].trim();
		}
		return rootSpecification;
	}

	protected QName getRootElement(Set<IXSD> xsds, String root) {
		return getRootElement(xsds, root, null);
	}

	protected QName getRootElement(Set<IXSD> xsds, String root, String namespace) {
		String firstRoot;
		if (!root.trim().isEmpty()) {
			String[] roots = root.trim().split(",", -1);
			firstRoot = roots[0].trim();
			for (IXSD xsd : xsds) {
				for (String rootTag : xsd.getRootTags()) {
					if (firstRoot.equals(rootTag) && (StringUtils.isEmpty(namespace) || namespace.equals(xsd.getNamespace()))) {
							String prefix = prefixByXsd.get(xsd);
							return new QName(namespaceByPrefix.get(prefix), firstRoot, prefix);

					}
				}
			}
			if (StringUtils.isEmpty(namespace)) {
				warn("Root element '" + firstRoot + "' not found in XSD's");
			} else {
				warn("Root element '" + firstRoot + "' with namespace '" + namespace + "' not found in XSD's");
			}
		}
		return null;
	}

	protected QName getHeaderElement(IXmlValidator xmlValidator, Set<IXSD> xsds) {
		if (xmlValidator instanceof SoapValidator validator) {
			String root = validator.getSoapHeader();
			String namespace = validator.getSoapHeaderNamespace();
			if (StringUtils.isNotEmpty(root)) {
				return getRootElement(xsds, root, namespace);
			}
		}
		return null;
	}

	protected QName getBodyElement(IXmlValidator xmlValidator, Set<IXSD> xsds, String type) {
		return getBodyElement(xmlValidator, xsds, type, false);
	}

	protected QName getBodyElement(IXmlValidator xmlValidator, Set<IXSD> xsds, String type, boolean outputMode) {
		String root;
//		if (xmlValidator instanceof SoapValidator) {
//			if (outputMode) {
//				root = ((SoapValidator)xmlValidator).getOutputSoapBody();
//			} else {
//				root = ((SoapValidator)xmlValidator).getSoapBody();
//			}
//			if (StringUtils.isEmpty(root)) {
//				warn("Attribute soapBody for " + type + " not found or empty");
//			}
//		} else {
//			root = xmlValidator.getRoot();
		root = xmlValidator.getMessageRoot();
				if (StringUtils.isEmpty(root)) {
				warn("Attribute root for " + type + " not found or empty");
			}
//		}
		if (StringUtils.isNotEmpty(root)) {
			return getRootElement(xsds, root);
		}
		return null;
	}

	protected void warn(String warning, Exception e) {
		warn(warning+": ("+ClassUtils.nameOf(e)+") "+e.getMessage());
	}

	protected void warn(String warning) {
		warning = "Warning: " + warning;
		if (!warnings.contains(warning)) {
			warnings.add(warning);
		}
	}
}

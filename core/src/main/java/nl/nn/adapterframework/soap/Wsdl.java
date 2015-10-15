/*
   Copyright 2013, 2015 Nationale-Nederlanden

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
package nl.nn.adapterframework.soap;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.naming.NamingException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.extensions.esb.EsbSoapValidator;
import nl.nn.adapterframework.extensions.esb.EsbSoapWrapperPipe;
import nl.nn.adapterframework.http.WebServiceListener;
import nl.nn.adapterframework.jms.JmsListener;
import nl.nn.adapterframework.pipes.XmlValidator;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.validation.SchemaUtils;
import nl.nn.adapterframework.validation.XSD;

import org.apache.commons.lang.StringUtils;


/**
 *  Utility class to generate the WSDL. Straight-forwardly implemented using stax only.
 *
 *  An object of this class represents the WSDL associated with one IBIS pipeline.
 *
 * @author  Michiel Meeuwissen
 * @author  Jaco de Groot
 */
public class Wsdl {
    protected static final String WSDL_NAMESPACE                 = "http://schemas.xmlsoap.org/wsdl/";
    protected static final String WSDL_NAMESPACE_PREFIX          = "wsdl";
    protected static final String XSD_NAMESPACE                  = SchemaUtils.XSD;
    protected static final String XSD_NAMESPACE_PREFIX           = "xsd";
    protected static final String SOAP_NAMESPACE                 = "http://schemas.xmlsoap.org/wsdl/soap/";
    protected static final String SOAP_NAMESPACE_PREFIX          = "soap";
    protected static final String SOAP12_NAMESPACE               = "http://schemas.xmlsoap.org/wsdl/soap12/";
    protected static final String SOAP12_NAMESPACE_PREFIX        = "soap12";
    protected static final String SOAP_HTTP_NAMESPACE            = "http://schemas.xmlsoap.org/soap/http";
    protected static final String SOAP_JMS_NAMESPACE             = "http://www.w3.org/2010/soapjms/";
    protected static final String SOAP_JMS_NAMESPACE_PREFIX      = "jms";
    // Tibco BW will not detect the transport when SOAP_JMS_NAMESPACE is being used instead of ESB_SOAP_JMS_NAMESPACE.
    protected static final String ESB_SOAP_JMS_NAMESPACE         = "http://www.tibco.com/namespaces/ws/2004/soap/binding/JMS";
    protected static final String ESB_SOAP_JNDI_NAMESPACE        = "http://www.tibco.com/namespaces/ws/2004/soap/apis/jndi";
    protected static final String ESB_SOAP_JNDI_NAMESPACE_PREFIX = "jndi";
    protected static final String ESB_SOAP_TNS_BASE_URI          = "http://nn.nl/WSDL";
    protected static final String TARGET_NAMESPACE_PREFIX        = "tns";

    protected static final List<String> excludeXsds = new ArrayList<String>();
    static {
        excludeXsds.add("http://schemas.xmlsoap.org/soap/envelope/"); // SOAP envelope 1.1
        excludeXsds.add("http://www.w3.org/2003/05/soap-envelope");   // SOAP envelope 1.2
    };

    private boolean indent = true;
    private boolean useIncludes = false;

    private final String name;
    private final String filename;
    private final PipeLine pipeLine;
    private final XmlValidator inputValidator;
    private final XmlValidator outputValidator;
    private String webServiceListenerNamespace;
    private Set<XSD> inputXsds;
    private Set<XSD> outputXsds;
    private Set<XSD> xsds;
    private Set<XSD> rootXsds;
    private Map<String, Set<XSD>> xsdsGroupedByNamespace;
    private LinkedHashMap<XSD, String> prefixByXsd;
    private LinkedHashMap<String, String> namespaceByPrefix;
    private String inputRoot;
    private String outputRoot;
    private QName inputHeaderElement;
    private QName inputBodyElement;
    private QName outputHeaderElement;
    private QName outputBodyElement;

    private boolean httpActive = false;
    private boolean jmsActive = false;

    private final String targetNamespace;
    private String targetNamespacePrefix = TARGET_NAMESPACE_PREFIX;
    private String soapNamespace = SOAP_NAMESPACE;
    private String soapPrefix = SOAP_NAMESPACE_PREFIX;

    private boolean esbSoap = false;
    private String esbSoapBusinessDomain;
    private String esbSoapServiceName;
    private String esbSoapServiceContext;
    private String esbSoapServiceContextVersion;
    private String esbSoapOperationName;
    private String esbSoapOperationVersion;

    private String documentation;

    private List<String> warnings = new ArrayList<String>();

    public Wsdl(PipeLine pipeLine) {
        this.pipeLine = pipeLine;
        this.name = this.pipeLine.getAdapter().getName();
        if (this.name == null) {
            throw new IllegalArgumentException("Adapter has no name");
        }
        inputValidator = (XmlValidator)pipeLine.getInputValidator();
        if (inputValidator == null) {
            throw new IllegalStateException("Adapter has no input validator");
        }
        if (inputValidator.getConfigurationException() != null) {
            if (inputValidator.getConfigurationException().getMessage() != null) {
                throw new IllegalStateException(inputValidator.getConfigurationException().getMessage());
            } else {
                throw new IllegalStateException(inputValidator.getConfigurationException().toString());
            }
        }
        outputValidator = (XmlValidator)pipeLine.getOutputValidator();
        if (outputValidator != null && outputValidator.getConfigurationException() != null) {
            if (outputValidator.getConfigurationException().getMessage() != null) {
                throw new IllegalStateException(outputValidator.getConfigurationException().getMessage());
            } else {
                throw new IllegalStateException(outputValidator.getConfigurationException().toString());
            }
        }
        String filename = getName();
        AppConstants appConstants = AppConstants.getInstance();
        String tns = appConstants.getResolvedProperty("wsdl." + getName() + ".targetNamespace");
        if (tns == null) {
            tns = appConstants.getResolvedProperty("wsdl.targetNamespace");
        }
        if (tns == null) {
            if (inputValidator instanceof EsbSoapValidator) {
                esbSoap = true;
                String schemaLocation = WsdlUtils.getFirstNamespaceFromSchemaLocation(inputValidator);
                if (EsbSoapWrapperPipe.isValidNamespace(schemaLocation)) {
                    String s = WsdlUtils.getFirstNamespaceFromSchemaLocation(inputValidator);
                    int i = s.lastIndexOf('/');
                    esbSoapOperationVersion = s.substring(i + 1);
                    s = s.substring(0, i);
                    i = s.lastIndexOf('/');
                    esbSoapOperationName = s.substring(i + 1);
                    s = s.substring(0, i);
                    i = s.lastIndexOf('/');
                    esbSoapServiceContextVersion = s.substring(i + 1);
                    s = s.substring(0, i);
                    i = s.lastIndexOf('/');
                    esbSoapServiceContext = s.substring(i + 1);
                    s = s.substring(0, i);
                    i = s.lastIndexOf('/');
                    esbSoapServiceName = s.substring(i + 1);
                    s = s.substring(0, i);
                    i = s.lastIndexOf('/');
                    esbSoapBusinessDomain = s.substring(i + 1);
                } else {
                    warn("Namespace '" + schemaLocation + "' invalid according to ESB SOAP standard");
                    IPipe outputWrapper = pipeLine.getOutputWrapper();
                    if (outputWrapper != null
                            && outputWrapper instanceof EsbSoapWrapperPipe) {
                        EsbSoapWrapperPipe esbSoapWrapper = (EsbSoapWrapperPipe)outputWrapper;
                        esbSoapBusinessDomain = esbSoapWrapper.getBusinessDomain();
                        esbSoapServiceName = esbSoapWrapper.getServiceName();
                        esbSoapServiceContext = esbSoapWrapper.getServiceContext();
                        esbSoapServiceContextVersion = esbSoapWrapper.getServiceContextVersion();
                        esbSoapOperationName = esbSoapWrapper.getOperationName();
                        esbSoapOperationVersion = esbSoapWrapper.getOperationVersion();
                    }
                }
                if (esbSoapBusinessDomain == null) {
                    warn("Could not determine business domain");
                } else if (esbSoapServiceName == null) {
                    warn("Could not determine service name");
                } else if (esbSoapServiceContext == null) {
                    warn("Could not determine service context");
                } else if (esbSoapServiceContextVersion == null) {
                    warn("Could not determine service context version");
                } else if (esbSoapOperationName == null) {
                    warn("Could not determine operation name");
                } else if (esbSoapOperationVersion == null) {
                    warn("Could not determine operation version");
                } else {
                    String wsdlType = "abstract";
                    for (IListener listener : WsdlUtils.getListeners(pipeLine.getAdapter())) {
                        if (listener instanceof WebServiceListener
                                || listener instanceof JmsListener) {
                            wsdlType = "concrete";
                        }
                    }
                    filename = esbSoapBusinessDomain + "_"
                            + esbSoapServiceName + "_"
                            + esbSoapServiceContext + "_"
                            + esbSoapServiceContextVersion + "_"
                            + esbSoapOperationName + "_"
                            + esbSoapOperationVersion + "_"
                            + wsdlType;
                    tns = ESB_SOAP_TNS_BASE_URI + "/"
                            + esbSoapBusinessDomain + "/"
                            + esbSoapServiceName + "/"
                            + esbSoapServiceContext + "/"
                            + esbSoapServiceContextVersion + "/"
                            + esbSoapOperationName + "/"
                            + esbSoapOperationVersion;
                    String inputParadigm = WsdlUtils.getEsbSoapParadigm(inputValidator);
                    if (inputParadigm != null) {
                        if (!"Action".equals(inputParadigm)
                                && !"Event".equals(inputParadigm)
                                && !"Request".equals(inputParadigm)
                                && !"Solicit".equals(inputParadigm)) {
                            warn("Paradigm for input message which was extracted from soapBody should be on of Action, Event, Request or Solicit instead of '"
                                    + inputParadigm + "'");
                        }
                    } else {
                        warn("Could not extract paradigm from soapBody attribute of inputValidator (should end with _Action, _Event, _Request or _Solicit)");
                    }
                    if (outputValidator != null) {
                        String outputParadigm = WsdlUtils.getEsbSoapParadigm(outputValidator);
                        if (outputParadigm != null) {
                            if (!"Response".equals(outputParadigm)) {
                                warn("Paradigm for output message which was extracted from soapBody should be Response instead of '"
                                        + outputParadigm + "'");
                            }
                        } else {
                            warn("Could not extract paradigm from soapBody attribute of outputValidator (should end with _Response)");
                        }
                    }
                }
            }
            if (tns == null) {
                for(IListener listener : WsdlUtils.getListeners(pipeLine.getAdapter())) {
                    if (listener instanceof WebServiceListener) {
                        webServiceListenerNamespace = ((WebServiceListener)listener).getServiceNamespaceURI();
                        tns = webServiceListenerNamespace;
                    }
                }
                if (tns == null) {
                    tns = WsdlUtils.getFirstNamespaceFromSchemaLocation(inputValidator);
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
        if (outputValidator != null) {
            if (outputValidator.getSchema() != null && webServiceListenerNamespace == null) {
                throw new IllegalStateException("Adapter has an outputValidator using the schema attribute in which case a WebServiceListener with serviceNamespaceURI is required");
            }
        }
        this.filename = filename;
        this.targetNamespace = WsdlUtils.validUri(tns);
        if (inputValidator instanceof SoapValidator
                && "1.2".equals(((SoapValidator)inputValidator).getSoapVersion())) {
            soapNamespace = SOAP12_NAMESPACE;
            soapPrefix = SOAP12_NAMESPACE_PREFIX;
        }
    }

    public String getName() {
        return name;
    }

    public String getFilename() {
        return filename;
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

    public void init() throws IOException, XMLStreamException, ConfigurationException {
        inputXsds = new HashSet<XSD>();
        outputXsds = new HashSet<XSD>();
        xsds = new HashSet<XSD>();
        rootXsds = new HashSet<XSD>();
        Set<XSD> inputRootXsds = new HashSet<XSD>();
        inputRootXsds.addAll(getXsds(inputValidator));
        rootXsds.addAll(inputRootXsds);
        inputXsds.addAll(SchemaUtils.getXsdsRecursive(inputRootXsds));
        xsds.addAll(inputXsds);
        if (outputValidator != null) {
            Set<XSD> outputRootXsds = new HashSet<XSD>();
            outputRootXsds.addAll(getXsds(outputValidator));
            rootXsds.addAll(outputRootXsds);
            outputXsds.addAll(SchemaUtils.getXsdsRecursive(outputRootXsds));
            xsds.addAll(outputXsds);
        }
        prefixByXsd = new LinkedHashMap<XSD, String>();
        namespaceByPrefix = new LinkedHashMap<String, String>();
        int prefixCount = 1;
        xsdsGroupedByNamespace =
                SchemaUtils.getXsdsGroupedByNamespace(xsds, true);
        for (String namespace: xsdsGroupedByNamespace.keySet()) {
            // When a schema has targetNamespace="http://www.w3.org/XML/1998/namespace"
            // it needs to be ignored as prefix xml is the only allowed prefix
            // for namespace http://www.w3.org/XML/1998/namespace. The xml
            // prefix doesn't have to be declared as the prefix xml is by
            // definition bound to the namespace name http://www.w3.org/XML/1998/namespace
            // (see http://www.w3.org/TR/xml-names/#ns-decl).
            if (!"http://www.w3.org/XML/1998/namespace".equals(namespace)) {
                for (XSD xsd: xsdsGroupedByNamespace.get(namespace)) {
                    prefixByXsd.put(xsd, "ns" + prefixCount);
                }
                namespaceByPrefix.put("ns" + prefixCount, namespace);
                prefixCount++;
            }
        }
        for (XSD xsd : xsds) {
            if (StringUtils.isEmpty(xsd.getTargetNamespace())
                    && !xsd.isAddNamespaceToSchema()) {
                warn("XSD '" + xsd
                        + "' doesn't have a targetNamespace and addNamespaceToSchema is false");
            }
        }
        inputRoot = getRoot(inputValidator);
        inputHeaderElement = getHeaderElement(inputValidator, inputXsds);
        inputBodyElement = getBodyElement(inputValidator, inputXsds, "inputValidator");
        if (outputValidator != null) {
            outputRoot = getRoot(outputValidator);
            outputHeaderElement = getHeaderElement(outputValidator, outputXsds);
            outputBodyElement = getBodyElement(outputValidator, outputXsds, "outputValidator");
        }
        for (IListener listener : WsdlUtils.getListeners(pipeLine.getAdapter())) {
            if (listener instanceof WebServiceListener) {
                httpActive = true;
            } else if (listener instanceof JmsListener) {
                jmsActive = true;
            }
        }
    }

    public Set<XSD> getXsds(XmlValidator xmlValidator)
            throws IOException, XMLStreamException, ConfigurationException {
        Set<XSD> xsds = new HashSet<XSD>();
        String inputSchema = xmlValidator.getSchema();
        if (inputSchema != null) {
            // In case of a WebServiceListener using soap=true it might be
            // valid to use the schema attribute (in which case the schema
            // doesn't have a namespace) as the WebServiceListener will
            // remove the soap envelop and body element before it is
            // validated. In this case we use the serviceNamespaceURI from
            // the WebServiceListener as the namespace for the schema.
            XSD xsd = new XSD();
            xsd.setNamespace(webServiceListenerNamespace);
            xsd.setResource(inputSchema);
            xsd.setAddNamespaceToSchema(true);
            xsd.init();
            xsds.add(xsd);
        } else {
            xsds = xmlValidator.getXsds();
            Set<XSD> remove = new HashSet<XSD>();
            for (XSD xsd : xsds) {
                if (excludeXsds.contains(xsd.getNamespace())) {
                    remove.add(xsd);
                }
            }
            xsds.removeAll(remove);
        }
        return xsds;
    }

    /**
     * Generates a zip file (and writes it to the given outputstream), containing the WSDL and all referenced XSD's.
     * @see #wsdl(java.io.OutputStream, String)
     */
    public void zip(OutputStream stream, String servletName) throws IOException, ConfigurationException, XMLStreamException, NamingException {
        ZipOutputStream out = new ZipOutputStream(stream);

        // First an entry for the WSDL itself:
        ZipEntry wsdlEntry = new ZipEntry(getFilename() + ".wsdl");
        out.putNextEntry(wsdlEntry);
        wsdl(out, servletName);
        out.closeEntry();

        //And then all XSD's
        Set<String> entries = new HashSet<String>();
        for (XSD xsd : xsds) {
            String zipName = xsd.getResourceTarget();
            if (entries.add(zipName)) {
                ZipEntry xsdEntry = new ZipEntry(zipName);
                out.putNextEntry(xsdEntry);
                XMLStreamWriter writer = WsdlUtils.getWriter(out, false);
                SchemaUtils.xsdToXmlStreamWriter(xsd, writer);
                out.closeEntry();
            } else {
                warn("Duplicate xsds in " + this + " " + xsd + " " + xsds);
            }
        }
        out.close();
    }

    /**
     * Writes the WSDL to an output stream
     * @param out
     * @param servlet  The servlet what is used as the web service (because this needs to be present in the WSDL)
     * @throws XMLStreamException
     * @throws IOException
     */
    public void wsdl(OutputStream out, String servlet) throws XMLStreamException, IOException, ConfigurationException,  NamingException {
        XMLStreamWriter w = WsdlUtils.getWriter(out, isIndent());

        w.writeStartDocument(XmlUtils.STREAM_FACTORY_ENCODING, "1.0");
        w.setPrefix(WSDL_NAMESPACE_PREFIX, WSDL_NAMESPACE);
        w.setPrefix(XSD_NAMESPACE_PREFIX, XSD_NAMESPACE);
        w.setPrefix(soapPrefix, soapNamespace);
        if (jmsActive) {
            if (esbSoap) {
                w.setPrefix(SOAP_JMS_NAMESPACE_PREFIX, ESB_SOAP_JMS_NAMESPACE);
                w.setPrefix(ESB_SOAP_JNDI_NAMESPACE_PREFIX, ESB_SOAP_JNDI_NAMESPACE);
            } else {
                w.setPrefix(SOAP_JMS_NAMESPACE_PREFIX, SOAP_JMS_NAMESPACE);
            }
        }
        w.setPrefix(getTargetNamespacePrefix(), getTargetNamespace());
        for (String prefix: namespaceByPrefix.keySet()) {
            w.setPrefix(prefix, namespaceByPrefix.get(prefix));
        }
        w.writeStartElement(WSDL_NAMESPACE, "definitions"); {
            w.writeNamespace(WSDL_NAMESPACE_PREFIX, WSDL_NAMESPACE);
            w.writeNamespace(XSD_NAMESPACE_PREFIX, XSD_NAMESPACE);
            w.writeNamespace(soapPrefix, soapNamespace);
            if (esbSoap) {
                w.writeNamespace(ESB_SOAP_JNDI_NAMESPACE_PREFIX, ESB_SOAP_JNDI_NAMESPACE);
            }
            w.writeNamespace(getTargetNamespacePrefix(), getTargetNamespace());
            for (String prefix: namespaceByPrefix.keySet()) {
                w.writeNamespace(prefix, namespaceByPrefix.get(prefix));
            }
            w.writeAttribute("targetNamespace", getTargetNamespace());

            documentation(w);
            types(w);
            messages(w);
            portType(w);
            binding(w);
            service(w, servlet);
        }
        w.writeEndDocument();
        warnings(w);
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
     * @param w
     * @throws XMLStreamException
     * @throws IOException
     */
    protected void types(XMLStreamWriter w) throws XMLStreamException, IOException, ConfigurationException {
        w.writeStartElement(WSDL_NAMESPACE, "types");
        if (isUseIncludes()) {
            SchemaUtils.mergeRootXsdsGroupedByNamespaceToSchemasWithIncludes(
                    SchemaUtils.getXsdsGroupedByNamespace(rootXsds, true), w);
        }  else {
            SchemaUtils.mergeXsdsGroupedByNamespaceToSchemasWithoutIncludes(
                    xsdsGroupedByNamespace, w);
        }
        w.writeEndElement();
    }

    /**
     * Outputs the 'messages' section.
     * @param w
     * @throws XMLStreamException
     * @throws IOException
     * @throws ConfigurationException 
     */
    protected void messages(XMLStreamWriter w) throws XMLStreamException, IOException, ConfigurationException {
        List<QName> parts = new ArrayList<QName>();
        if (inputHeaderElement != null) {
            parts.add(inputHeaderElement);
        }
        if (inputBodyElement != null) {
            parts.add(inputBodyElement);
        }
        message(w, inputRoot, parts);
        if (outputValidator != null) {
            parts.clear();
            if (outputHeaderElement != null) {
                parts.add(outputHeaderElement);
            }
            if (outputBodyElement != null) {
                parts.add(outputBodyElement);
            }
            message(w, outputRoot, parts);
        }
    }

    protected void message(XMLStreamWriter w, String root, Collection<QName> parts) throws XMLStreamException, IOException {
        if (!parts.isEmpty()) {
            w.writeStartElement(WSDL_NAMESPACE, "message");
            w.writeAttribute("name", "Message_" + root);
            {
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

    protected void portType(XMLStreamWriter w) throws XMLStreamException, IOException {
        w.writeStartElement(WSDL_NAMESPACE, "portType");
        w.writeAttribute("name", "PortType_" + getName()); {
            for (IListener listener : WsdlUtils.getListeners(pipeLine.getAdapter())) {
                if (listener instanceof WebServiceListener || listener instanceof JmsListener) {
                    w.writeStartElement(WSDL_NAMESPACE, "operation");
                    w.writeAttribute("name", "Operation_" + WsdlUtils.getNCName(getSoapAction(listener))); {
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

    protected String getSoapAction(IListener listener) {
        AppConstants appConstants = AppConstants.getInstance();
        String sa = appConstants.getResolvedProperty("wsdl." + getName() + "." + listener.getName() + ".soapAction");
        if (sa != null) return sa;
        sa = appConstants.getResolvedProperty("wsdl." + getName() + ".soapAction");
        if (sa != null) return sa;
        sa = appConstants.getResolvedProperty("wsdl.soapAction");
        if (sa != null) return sa;
        if (esbSoapOperationName != null && esbSoapOperationVersion != null) {
            return esbSoapOperationName + "_" + esbSoapOperationVersion;
        }
        return "${wsdl." + getName() + "." + listener.getName() + ".soapAction}";
    }

    protected String getLocation(String defaultLocation) {
        AppConstants appConstants = AppConstants.getInstance();
        String sa = appConstants.getResolvedProperty("wsdl." + getName() + ".location");
        if (sa != null) return sa;
        sa = appConstants.getResolvedProperty("wsdl.location");
        if (sa != null) return sa;
        return defaultLocation;
    }

    protected void binding(XMLStreamWriter w) throws XMLStreamException, IOException, ConfigurationException {
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

    protected void httpBinding(XMLStreamWriter w, String namePrefix) throws XMLStreamException, IOException, ConfigurationException {
        w.writeStartElement(WSDL_NAMESPACE, "binding");
        w.writeAttribute("name", namePrefix + "Binding_" + WsdlUtils.getNCName(getName()));
        w.writeAttribute("type", getTargetNamespacePrefix() + ":" + "PortType_" + getName()); {
            w.writeEmptyElement(soapNamespace, "binding");
            w.writeAttribute("transport", SOAP_HTTP_NAMESPACE);
            w.writeAttribute("style", "document");
            for (IListener listener : WsdlUtils.getListeners(pipeLine.getAdapter())) {
                if (listener instanceof WebServiceListener) {
                    writeSoapOperation(w, listener);
                }
            }
        }
        w.writeEndElement();
    }

    protected void writeSoapOperation(XMLStreamWriter w, IListener listener) throws XMLStreamException, IOException, ConfigurationException {
        w.writeStartElement(WSDL_NAMESPACE, "operation");
        w.writeAttribute("name", "Operation_" + WsdlUtils.getNCName(getSoapAction(listener))); {
            w.writeEmptyElement(soapNamespace, "operation");
            w.writeAttribute("style", "document");
            w.writeAttribute("soapAction", getSoapAction(listener));
            w.writeStartElement(WSDL_NAMESPACE, "input"); {
                writeSoapHeader(w, inputRoot, inputHeaderElement);
                writeSoapBody(w, inputBodyElement);
            }
            w.writeEndElement();
            if (outputValidator != null) {
                w.writeStartElement(WSDL_NAMESPACE, "output"); {
                    writeSoapHeader(w, outputRoot, outputHeaderElement);
                    writeSoapBody(w, outputBodyElement);
                }
                w.writeEndElement();
            }
        }
        w.writeEndElement();
    }

    protected void writeSoapHeader(XMLStreamWriter w, String root, QName headerElement) throws XMLStreamException, IOException {
        if (headerElement != null) {
            w.writeEmptyElement(soapNamespace, "header");
            w.writeAttribute("part", "Part_" + headerElement.getLocalPart());
            w.writeAttribute("use", "literal");
            w.writeAttribute("message", getTargetNamespacePrefix() + ":" + "Message_" + root);
        }
    }

    protected void writeSoapBody(XMLStreamWriter w, QName bodyElement) throws XMLStreamException, IOException {
        if (bodyElement != null) {
            w.writeEmptyElement(soapNamespace, "body");
            w.writeAttribute("parts", "Part_" + bodyElement.getLocalPart());
            w.writeAttribute("use", "literal");
        }
    }

    protected void jmsBinding(XMLStreamWriter w, String namePrefix) throws XMLStreamException, IOException, ConfigurationException {
        w.writeStartElement(WSDL_NAMESPACE, "binding");
        w.writeAttribute("name", namePrefix + "Binding_" + WsdlUtils.getNCName(getName()));
        w.writeAttribute("type", getTargetNamespacePrefix() + ":" + "PortType_" + getName()); {
            w.writeEmptyElement(soapNamespace, "binding");
            w.writeAttribute("style", "document");
            if (esbSoap) {
                w.writeAttribute("transport", ESB_SOAP_JMS_NAMESPACE);
                w.writeEmptyElement(ESB_SOAP_JMS_NAMESPACE, "binding");
                w.writeAttribute("messageFormat", "Text");
                for (IListener listener : WsdlUtils.getListeners(pipeLine.getAdapter())) {
                    if (listener instanceof JmsListener) {
                        writeSoapOperation(w, listener);
                    }
                }
            } else {
                w.writeAttribute("transport", SOAP_JMS_NAMESPACE);
            }
        }
        w.writeEndElement();
    }

    protected void service(XMLStreamWriter w, String servlet) throws XMLStreamException, NamingException {
        String httpPrefix = "";
        String jmsPrefix = "";
        if (httpActive && jmsActive) {
            httpPrefix = "Http";
            jmsPrefix = "Jms";
        }
        if (httpActive) {
            httpService(w, servlet, httpPrefix);
        }
        if (jmsActive) {
            for (IListener listener : WsdlUtils.getListeners(pipeLine.getAdapter())) {
                if (listener instanceof JmsListener) {
                    jmsService(w, (JmsListener)listener, jmsPrefix);
                }
            }
        }
    }

    protected void httpService(XMLStreamWriter w, String servlet, String namePrefix) throws XMLStreamException {
        w.writeStartElement(WSDL_NAMESPACE, "service");
        w.writeAttribute("name", "Service_" + WsdlUtils.getNCName(getName())); {
            w.writeStartElement(WSDL_NAMESPACE, "port");
            w.writeAttribute("name", namePrefix + "Port_" + WsdlUtils.getNCName(getName()));
            w.writeAttribute("binding", getTargetNamespacePrefix() + ":" + namePrefix + "Binding_" + WsdlUtils.getNCName(getName())); {
                w.writeEmptyElement(soapNamespace, "address");
                w.writeAttribute("location", getLocation(servlet));
            }
            w.writeEndElement();
        }
        w.writeEndElement();
    }

    protected void jmsService(XMLStreamWriter w, JmsListener listener, String namePrefix) throws XMLStreamException, NamingException {
        w.writeStartElement(WSDL_NAMESPACE, "service");
        w.writeAttribute("name", "Service_" + WsdlUtils.getNCName(getName())); {
            if (!esbSoap) {
                // Per example of https://docs.jboss.org/author/display/JBWS/SOAP+over+JMS
                w.writeStartElement(SOAP_JMS_NAMESPACE, "jndiConnectionFactoryName");
                w.writeCharacters(listener.getQueueConnectionFactoryName());
            }
            w.writeStartElement(WSDL_NAMESPACE, "port");
            w.writeAttribute("name", namePrefix + "Port_" + WsdlUtils.getNCName(getName()));
            w.writeAttribute("binding", getTargetNamespacePrefix() + ":" + namePrefix + "Binding_" + WsdlUtils.getNCName(getName())); {
                w.writeEmptyElement(soapNamespace, "address");
                String destinationName = listener.getDestinationName();
                if (destinationName != null) {
                    w.writeAttribute("location", getLocation(destinationName));
                }
                if (esbSoap) {
                    writeEsbSoapJndiContext(w, listener);
                    w.writeStartElement(ESB_SOAP_JMS_NAMESPACE, "connectionFactory"); {
                        w.writeCharacters("externalJndiName-for-"
                                + listener.getQueueConnectionFactoryName()
                                + "-on-"
                                + AppConstants.getInstance().getResolvedProperty("otap.stage"));
                        w.writeEndElement();
                    }
                    w.writeStartElement(ESB_SOAP_JMS_NAMESPACE, "targetAddress"); {
                        w.writeAttribute("destination",
                                listener.getDestinationType().toLowerCase());
                        String queueName = listener.getPhysicalDestinationShortName();
                        if (queueName == null) {
                            queueName = "queueName-for-"
                                    + listener.getDestinationName() + "-on-"
                                    + AppConstants.getInstance().getResolvedProperty("otap.stage");
                        }
                        w.writeCharacters(queueName);
                        w.writeEndElement();
                    }
                }
            }
            w.writeEndElement();
        }
        w.writeEndElement();
    }

    protected void writeEsbSoapJndiContext(XMLStreamWriter w, JmsListener listener) throws XMLStreamException, NamingException {
        w.writeStartElement(ESB_SOAP_JNDI_NAMESPACE, "context"); {
            w.writeStartElement(ESB_SOAP_JNDI_NAMESPACE, "property"); {
                w.writeAttribute("name", "java.naming.factory.initial");
                w.writeAttribute("type", "java.lang.String");
                w.writeCharacters("com.tibco.tibjms.naming.TibjmsInitialContextFactory");
                w.writeEndElement();
            }
            w.writeStartElement(ESB_SOAP_JNDI_NAMESPACE, "property"); {
                w.writeAttribute("name", "java.naming.provider.url");
                w.writeAttribute("type", "java.lang.String");
                String qcf = listener.getQueueConnectionFactoryName();
                if (StringUtils.isEmpty(qcf)) {
                    warn("Attribute queueConnectionFactoryName empty for listener '" + listener.getName() + "'");
                } else {
                    try {
                        qcf = URLEncoder.encode(qcf, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        warn("Could not encode queueConnectionFactoryName for listener '" + listener.getName() + "'");
                    }
                }
                String stage = AppConstants.getInstance().getResolvedProperty("otap.stage");
                if (StringUtils.isEmpty(stage)) {
                    warn("Property otap.stage empty");
                } else {
                    try {
                        stage = URLEncoder.encode(stage, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        warn("Could not encode property otap.stage");
                    }
                }
                w.writeCharacters("tibjmsnaming://host-for-" + qcf + "-on-"
                        + stage + ":37222");
                w.writeEndElement();
            }
            w.writeStartElement(ESB_SOAP_JNDI_NAMESPACE, "property"); {
                w.writeAttribute("name", "java.naming.factory.object");
                w.writeAttribute("type", "java.lang.String");
                w.writeCharacters("com.tibco.tibjms.custom.CustomObjectFactory");
                w.writeEndElement();
            }
        }
        w.writeEndElement();
    }

    protected void warnings(XMLStreamWriter w) throws XMLStreamException {
        for (String warning : warnings) {
            w.writeComment(warning);
        }
    }

    protected PipeLine getPipeLine() {
        return pipeLine;
    }

    protected String getRoot(XmlValidator xmlValidator) {
        if (xmlValidator instanceof SoapValidator) {
            return ((SoapValidator)xmlValidator).getSoapBody();
        } else {
            return xmlValidator.getRoot();
        }
    }

    protected QName getRootElement(Set<XSD> xsds, String root) {
        for (XSD xsd : xsds) {
            for (String rootTag : xsd.getRootTags()) {
                if (root.equals(rootTag)) {
                    String prefix = prefixByXsd.get(xsd);
                    return new QName(namespaceByPrefix.get(prefix), root, prefix);
                }
            }
        }
        warn("Root element '" + root + "' not found in XSD's");
        return null;
    }

    protected QName getHeaderElement(XmlValidator xmlValidator, Set<XSD> xsds) {
        if (xmlValidator instanceof SoapValidator) {
            String root = ((SoapValidator)xmlValidator).getSoapHeader();
            if (StringUtils.isNotEmpty(root)) {
                return getRootElement(xsds, root);
            }
        }
        return null;
    }

    protected QName getBodyElement(XmlValidator xmlValidator, Set<XSD> xsds, String type) {
        String root;
        if (xmlValidator instanceof SoapValidator) {
            root = ((SoapValidator)xmlValidator).getSoapBody();
            if (StringUtils.isEmpty(root)) {
                warn("Attribute soapBody for " + type + " not found or empty");
            }
        } else {
            root = xmlValidator.getRoot();
            if (StringUtils.isEmpty(root)) {
                warn("Attribute root for " + type + " not found or empty");
            }
        }
        if (StringUtils.isNotEmpty(root)) {
            return getRootElement(xsds, root);
        }
        return null;
    }

    protected void warn(String warning) {
        warning = "Warning: " + warning;
        if (!warnings.contains(warning)) {
            warnings.add(warning);
        }
    }

}

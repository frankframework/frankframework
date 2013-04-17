/*
   Copyright 2013 Nationale-Nederlanden

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
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.naming.NamingException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

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
class Wsdl {
    protected static final String WSDL                  = "http://schemas.xmlsoap.org/wsdl/";
    protected static final String SOAP_WSDL             = "http://schemas.xmlsoap.org/wsdl/soap/";
    protected static final String SOAP_HTTP             = "http://schemas.xmlsoap.org/soap/http";
    protected static final String SOAP_JMS              = "http://www.w3.org/2010/soapjms/";

    // Tibco BW will not detect the transport when SOAP_JMS is being used
    // instead of ESB_SOAP_JMS.
    protected static final String ESB_SOAP_JMS          = "http://www.tibco.com/namespaces/ws/2004/soap/binding/JMS";
    protected static final String ESB_SOAP_JNDI         = "http://www.tibco.com/namespaces/ws/2004/soap/apis/jndi";
    protected static final String ESB_SOAP_TNS_BASE_URI = "http://nn.nl/WSDL";

    protected static final List<String> excludeXsds = new ArrayList<String>();
    static {
        excludeXsds.add("http://schemas.xmlsoap.org/soap/envelope/");
    };

    private boolean indent = true;
    private boolean useIncludes = false;

    private final String name;
    private final String filename;
    private final String targetNamespace;
    private final PipeLine pipeLine;
    private final XmlValidator inputValidator;
    private final XmlValidator outputValidator;
    private String webServiceListenerNamespace;
    private Set<XSD> rootXsds;
    private Set<XSD> xsdsRecursive;
    private Map<String, Set<XSD>> rootXsdsGroupedByNamespace;
    private LinkedHashMap<XSD, String> prefixByXsd;
    private LinkedHashMap<String, String> namespaceByPrefix;

    private String wsdlInputMessageName = "PipeLineInput";
    private String wsdlOutputMessageName = "PipeLineOutput";
    private String wsdlPortTypeName = "PipeLine";
    private String wsdlOperationName = "Process";

    private boolean esbSoap = false;
    private String esbSoapBusinessDomain;
    private String esbSoapServiceName;
    private String esbSoapServiceContext;
    private String esbSoapServiceContextVersion;
    private String esbSoapOperationName;
    private String esbSoapOperationVersion;

    private String documentation;

    private List<String> warnings = new ArrayList<String>();

    Wsdl(PipeLine pipeLine) {
        this.pipeLine = pipeLine;
        this.name = this.pipeLine.getAdapter().getName();
        if (this.name == null) {
            throw new IllegalArgumentException("The adapter '" + pipeLine.getAdapter() + "' has no name");
        }
        inputValidator = (XmlValidator)pipeLine.getInputValidator();
        if (inputValidator == null) {
            throw new IllegalStateException("The adapter '" + getName() + "' has no input validator");
        }
        outputValidator = (XmlValidator)pipeLine.getOutputValidator();
        String filename = name;
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
                    for(IListener l : WsdlUtils.getListeners(pipeLine.getAdapter())) {
                        if (l instanceof WebServiceListener
                                || l instanceof JmsListener) {
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
                        wsdlInputMessageName = esbSoapOperationName + "_"
                            + esbSoapOperationVersion + "_" + inputParadigm;
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
                            wsdlOutputMessageName = esbSoapOperationName + "_"
                                + esbSoapOperationVersion + "_" + outputParadigm;
                            if (!"Response".equals(outputParadigm)) {
                                warn("Paradigm for output message which was extracted from soapBody should be Response instead of '"
                                        + outputParadigm + "'");
                            }
                        } else {
                            warn("Could not extract paradigm from soapBody attribute of outputValidator (should end with _Response)");
                        }
                    }
                    wsdlPortTypeName = esbSoapOperationName + "_Interface_" + esbSoapOperationVersion;
                    wsdlOperationName = esbSoapOperationName + "_" + esbSoapOperationVersion;
                }
            }
            if (tns == null) {
                for(IListener l : WsdlUtils.getListeners(pipeLine.getAdapter())) {
                    if (l instanceof WebServiceListener) {
                        webServiceListenerNamespace = ((WebServiceListener)l).getServiceNamespaceURI();
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
        this.filename = filename;
        this.targetNamespace = WsdlUtils.validUri(tns);
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

    public void init() throws IOException, XMLStreamException {
        init(false);
    }

    public void init(boolean checkSchemaLocationOnly) throws IOException, XMLStreamException {
        Set<XSD> xsds = new TreeSet<XSD>();
        xsds.addAll(initXsds(inputValidator, checkSchemaLocationOnly));
        if (outputValidator != null) {
            xsds.addAll(initXsds(outputValidator, checkSchemaLocationOnly));
        }
        if (!checkSchemaLocationOnly) {
            rootXsds = new TreeSet<XSD>();
            xsdsRecursive = new TreeSet<XSD>();
            rootXsds.addAll(xsds);
            xsdsRecursive.addAll(SchemaUtils.getXsdsRecursive(rootXsds));
            prefixByXsd = new LinkedHashMap<XSD, String>();
            namespaceByPrefix = new LinkedHashMap<String, String>();
            int prefixCount = 1;
            rootXsdsGroupedByNamespace =
                    SchemaUtils.getXsdsGroupedByNamespace(rootXsds);
            for (String namespace: rootXsdsGroupedByNamespace.keySet()) {
                xsds = rootXsdsGroupedByNamespace.get(namespace);
                for (XSD xsd: xsds) {
                    prefixByXsd.put(xsd, "ns" + prefixCount);
                }
                namespaceByPrefix.put("ns" + prefixCount, namespace);
                prefixCount++;
            }
            for (XSD xsd : xsdsRecursive) {
                if (StringUtils.isEmpty(xsd.targetNamespace)
                        && !xsd.addNamespaceToSchema) {
                    warn("XSD '" + xsd.getBaseUrl() + xsd.getName()
                            + "' doesn't have a targetNamespace and addNamespaceToSchema is false");
                }
            }
        }
    }

    public Set<XSD> initXsds(XmlValidator xmlValidator,
            boolean checkSchemaLocationOnly)
                    throws IOException, XMLStreamException {
        Set<XSD> xsds = new TreeSet<XSD>();
        String inputSchema = xmlValidator.getSchema();
        if (inputSchema != null) {
            // In case of a WebServiceListener using soap=true it might be
            // valid to use the schema attribute (in which case the schema
            // doesn't have a namespace) as the WebServiceListener will
            // remove the soap envelop and body element before it is
            // validated. In this case we use the serviceNamespaceURI from
            // the WebServiceListener as the namespace for the schema.
            if (webServiceListenerNamespace != null) {
                if (!checkSchemaLocationOnly) {
                    XSD xsd = SchemaUtils.getXSD(inputSchema, webServiceListenerNamespace, true, true);
                    xsds.add(xsd);
                }
            } else {
                throw new IllegalStateException("The adapter " + pipeLine + " has a validator using the schema attribute but a namespace is required");
            }
        } else {
            xsds = SchemaUtils.getXsds(xmlValidator.getSchemaLocation(),
                    excludeXsds, xmlValidator.isAddNamespaceToSchema(),
                    checkSchemaLocationOnly);
        }
        return xsds;
    }

    /**
     * Generates a zip file (and writes it to the given outputstream), containing the WSDL and all referenced XSD's.
     * @see {@link #wsdl(java.io.OutputStream, String)}
     */
    public void zip(OutputStream stream, String servletName) throws IOException, XMLStreamException, NamingException {
        ZipOutputStream out = new ZipOutputStream(stream);

        // First an entry for the WSDL itself:
        ZipEntry wsdlEntry = new ZipEntry(getFilename() + ".wsdl");
        out.putNextEntry(wsdlEntry);
        wsdl(out, servletName);
        out.closeEntry();

        //And then all XSD's
        Set<String> entries = new HashSet<String>();
        for (XSD xsd : xsdsRecursive) {
            String zipName = xsd.getBaseUrl() + xsd.getName();
            if (entries.add(zipName)) {
                ZipEntry xsdEntry = new ZipEntry(zipName);
                out.putNextEntry(xsdEntry);
                XMLStreamWriter writer = WsdlUtils.getWriter(out, false);
                SchemaUtils.xsdToXmlStreamWriter(xsd, writer, true);
                out.closeEntry();
            } else {
                warn("Duplicate xsds in " + this + " " + xsd + " " + xsdsRecursive);
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
    public void wsdl(OutputStream out, String servlet) throws XMLStreamException, IOException, NamingException {
        XMLStreamWriter w = WsdlUtils.getWriter(out, isIndent());

        w.writeStartDocument(XmlUtils.STREAM_FACTORY_ENCODING, "1.0");
        w.setPrefix("wsdl", WSDL);
        w.setPrefix("xsd",  SchemaUtils.XSD);
        w.setPrefix("soap", SOAP_WSDL);
        if (esbSoap) {
            w.setPrefix("jms",  ESB_SOAP_JMS);
            w.setPrefix("jndi", ESB_SOAP_JNDI);
        } else {
            w.setPrefix("jms",  SOAP_JMS);
        }
        w.setPrefix("ibis", getTargetNamespace());
        for (String prefix: namespaceByPrefix.keySet()) {
            w.setPrefix(prefix, namespaceByPrefix.get(prefix));
        }
        w.writeStartElement(WSDL, "definitions"); {
            w.writeNamespace("wsdl", WSDL);
            w.writeNamespace("xsd",  SchemaUtils.XSD);
            w.writeNamespace("soap", SOAP_WSDL);
            if (esbSoap) {
                w.writeNamespace("jndi", ESB_SOAP_JNDI);
            }
            w.writeNamespace("ibis", getTargetNamespace());
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
            w.writeStartElement(WSDL, "documentation");
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
    protected void types(XMLStreamWriter w) throws XMLStreamException, IOException {
        w.writeStartElement(WSDL, "types");
        if (isUseIncludes()) {
            SchemaUtils.mergeRootXsdsGroupedByNamespaceToSchemasWithIncludes(
                    rootXsdsGroupedByNamespace, w);
        }  else {
            Map<String, Set<XSD>> xsdsGroupedByNamespace =
                    SchemaUtils.getXsdsGroupedByNamespace(xsdsRecursive);
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
     */
    protected void messages(XMLStreamWriter w) throws XMLStreamException, IOException {
        List<QName> parts = new ArrayList<QName>();
        parts.addAll(getInputHeaderTags());
        parts.addAll(getInputBodyTags());
        message(w, wsdlInputMessageName, parts);
        XmlValidator outputValidator = (XmlValidator) pipeLine.getOutputValidator();
        if (outputValidator != null) {
            parts.clear();
            parts.addAll(getOutputHeaderTags());
            parts.addAll(getOutputBodyTags());
            message(w, wsdlOutputMessageName, parts);
        }
    }

    protected void message(XMLStreamWriter w, String name, Collection<QName> tags) throws XMLStreamException, IOException {
        if (tags == null) throw new IllegalArgumentException("Tag cannot be null for " + name);
        if (!tags.isEmpty()) {
            w.writeStartElement(WSDL, "message");
            w.writeAttribute("name", name);
            {
                for (QName tag : tags) {
                    w.writeEmptyElement(WSDL, "part");
                    w.writeAttribute("name", getIbisName(tag));
                    String typ = tag.getPrefix() + ":" + tag.getLocalPart();
                    w.writeAttribute("element", typ);
                }
            }
            w.writeEndElement();
        }
    }

    protected void portType(XMLStreamWriter w) throws XMLStreamException, IOException {
        w.writeStartElement(WSDL, "portType");
        w.writeAttribute("name", wsdlPortTypeName); {
            w.writeStartElement(WSDL, "operation");
            w.writeAttribute("name", wsdlOperationName); {
                w.writeEmptyElement(WSDL, "input");
                w.writeAttribute("message", "ibis:" + wsdlInputMessageName);
                if (outputValidator != null) {
                    w.writeEmptyElement(WSDL, "output");
                    w.writeAttribute("message", "ibis:" + wsdlOutputMessageName);
                }
            }
            w.writeEndElement();
        }
        w.writeEndElement();
    }

    protected String getSoapAction() {
        AppConstants appConstants = AppConstants.getInstance();
        String sa = appConstants.getResolvedProperty("wsdl." + getName() + ".soapAction");
        if (sa != null) return sa;
        sa = appConstants.getResolvedProperty("wsdl.soapAction");
        if (sa != null) return sa;
        if (esbSoapOperationName != null && esbSoapOperationVersion != null) {
            return esbSoapOperationName + "_" + esbSoapOperationVersion;
        }
        return "${wsdl." + getName() + ".soapAction}";
    }

    protected void binding(XMLStreamWriter w) throws XMLStreamException, IOException {
        for (IListener listener : WsdlUtils.getListeners(pipeLine.getAdapter())) {
            if (listener instanceof WebServiceListener) {
                httpBinding(w);
            } else if (listener instanceof JmsListener) {
                jmsBinding(w);
            }
        }
    }

    protected void httpBinding(XMLStreamWriter w) throws XMLStreamException, IOException {
        w.writeStartElement(WSDL, "binding");
        w.writeAttribute("name", "SoapBinding");
        w.writeAttribute("type", "ibis:" + wsdlPortTypeName); {
            w.writeEmptyElement(SOAP_WSDL, "binding");
            w.writeAttribute("transport", SOAP_HTTP);
            w.writeAttribute("style", "document");
            writeSoapOperation(w);
        }
        w.writeEndElement();
    }

    protected void writeSoapOperation(XMLStreamWriter w) throws XMLStreamException, IOException {

        w.writeStartElement(WSDL, "operation");
        w.writeAttribute("name", wsdlOperationName); {

            w.writeEmptyElement(SOAP_WSDL, "operation");
            w.writeAttribute("style", "document");
            w.writeAttribute("soapAction", getSoapAction());

            w.writeStartElement(WSDL, "input"); {
                writeSoapHeader(w, wsdlInputMessageName, getInputHeaderTags());
                writeSoapBody(w, getInputBodyTags());
            }
            w.writeEndElement();

            if (outputValidator != null) {
                w.writeStartElement(WSDL, "output"); {
                    writeSoapHeader(w, wsdlOutputMessageName, getOutputHeaderTags());
                    writeSoapBody(w, getOutputBodyTags());
                }
                w.writeEndElement();
            }

        }
        w.writeEndElement();
    }

    protected void writeSoapHeader(XMLStreamWriter w, String wsdlMessageName, Collection<QName> tags) throws XMLStreamException, IOException {
        if (!tags.isEmpty()) {
            if (tags.size() > 1) {
                warn("Can only deal with one soap header. Taking only the first of " + tags);
            }
            w.writeEmptyElement(SOAP_WSDL, "header");
            w.writeAttribute("part", getIbisName(tags.iterator().next()));
            w.writeAttribute("use",     "literal");
            w.writeAttribute("message", "ibis:" + wsdlMessageName);
        }
    }

    protected void writeSoapBody(XMLStreamWriter w, Collection<QName> tags) throws XMLStreamException, IOException {
        w.writeEmptyElement(SOAP_WSDL, "body");
        writeParts(w, tags);
        w.writeAttribute("use", "literal");
    }

    protected void writeParts(XMLStreamWriter w, Collection<QName> tags) throws XMLStreamException {
        StringBuilder builder = new StringBuilder();
        for (QName outputTag : tags) {
            if (builder.length() > 0) builder.append(" ");
            builder.append(getIbisName(outputTag));
        }
        w.writeAttribute("parts", builder.toString());
    }

    protected String getIbisName(QName qname) {
        return qname.getLocalPart();
    }

    protected void jmsBinding(XMLStreamWriter w) throws XMLStreamException, IOException {
        w.writeStartElement(WSDL, "binding");
        w.writeAttribute("name", "SoapBinding");
        w.writeAttribute("type", "ibis:" + wsdlPortTypeName); {
            w.writeEmptyElement(SOAP_WSDL, "binding");
            w.writeAttribute("style", "document");
            if (esbSoap) {
                w.writeAttribute("transport", ESB_SOAP_JMS);
                w.writeEmptyElement(ESB_SOAP_JMS, "binding");
                w.writeAttribute("messageFormat", "Text");
                writeSoapOperation(w);
            } else {
                w.writeAttribute("transport", SOAP_JMS);
            }
        }
        w.writeEndElement();
    }

    protected void service(XMLStreamWriter w, String servlet) throws XMLStreamException, NamingException {
        for (IListener listener : WsdlUtils.getListeners(pipeLine.getAdapter())) {
            if (listener instanceof WebServiceListener) {
                httpService(w, servlet);
            } else if (listener instanceof JmsListener) {
                jmsService(w, (JmsListener) listener);
            }
        }
    }

    protected void httpService(XMLStreamWriter w, String servlet) throws XMLStreamException {
        w.writeStartElement(WSDL, "service");
        w.writeAttribute("name", WsdlUtils.getNCName(getName())); {
            w.writeStartElement(WSDL, "port");
            w.writeAttribute("name", "SoapHttp");
            w.writeAttribute("binding", "ibis:SoapBinding"); {
                w.writeEmptyElement(SOAP_WSDL, "address");
                w.writeAttribute("location", servlet);

            }
            w.writeEndElement();
        }
        w.writeEndElement();
    }

    protected void jmsService(XMLStreamWriter w, JmsListener listener) throws XMLStreamException, NamingException {
        w.writeStartElement(WSDL, "service");
        w.writeAttribute("name", WsdlUtils.getNCName(getName())); {
            if (!esbSoap) {
                // Per example of https://docs.jboss.org/author/display/JBWS/SOAP+over+JMS
                w.writeStartElement(SOAP_JMS, "jndiConnectionFactoryName");
                w.writeCharacters(listener.getQueueConnectionFactoryName());
            }
            w.writeStartElement(WSDL, "port");
            w.writeAttribute("name", "SoapJMS");
            w.writeAttribute("binding", "ibis:SoapBinding"); {
                w.writeEmptyElement(SOAP_WSDL, "address");
                String destinationName = listener.getDestinationName();
                if (destinationName != null) {
                    w.writeAttribute("location", destinationName);
                }
                if (esbSoap) {
                    writeEsbSoapJndiContext(w, listener);
                    w.writeStartElement(ESB_SOAP_JMS, "connectionFactory"); {
                        w.writeCharacters("externalJndiName-for-"
                                + listener.getQueueConnectionFactoryName()
                                + "-on-"
                                + AppConstants.getInstance().getResolvedProperty("otap.stage"));
                        w.writeEndElement();
                    }
                    w.writeStartElement(ESB_SOAP_JMS, "targetAddress"); {
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
        w.writeStartElement(ESB_SOAP_JNDI, "context"); {
            w.writeStartElement(ESB_SOAP_JNDI, "property"); {
                w.writeAttribute("name", "java.naming.factory.initial");
                w.writeAttribute("type", "java.lang.String");
                w.writeCharacters("com.tibco.tibjms.naming.TibjmsInitialContextFactory");
                w.writeEndElement();
            }
            w.writeStartElement(ESB_SOAP_JNDI, "property"); {
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
            w.writeStartElement(ESB_SOAP_JNDI, "property"); {
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

    protected Collection<QName> getHeaderTags(XmlValidator xmlValidator) throws XMLStreamException, IOException {
        if (xmlValidator instanceof SoapValidator) {
            String root = ((SoapValidator)xmlValidator).getSoapHeader();
            QName q = getRootTag(root);
            if (q != null) {
                return Collections.singleton(q);
            }
        }
        return Collections.emptyList();
    }

    protected Collection<QName> getRootTags(XmlValidator xmlValidator) throws IOException, XMLStreamException {
        String root;
        if (xmlValidator instanceof SoapValidator) {
            root = ((SoapValidator)xmlValidator).getSoapBody();
        } else {
            root = xmlValidator.getRoot();
        }
        QName q = getRootTag(root);
        if (q != null) {
            return Collections.singleton(q);
        }
        return Collections.emptyList();
    }

    protected QName getRootTag(String tag) throws XMLStreamException, IOException {
        if (StringUtils.isNotEmpty(tag)) {
            for (XSD xsd : rootXsds) {
                for (String rootTag : xsd.rootTags) {
                    if (tag.equals(rootTag)) {
                        String prefix = prefixByXsd.get(xsd);
                        return new QName(namespaceByPrefix.get(prefix), tag, prefix);
                    }
                }
            }
            warn("Root element '" + tag + "' not found in XSD's");
        }
        return null;
    }

    protected Collection<QName> getInputHeaderTags() throws IOException, XMLStreamException {
        return getHeaderTags(inputValidator);
    }

    protected Collection<QName> getInputBodyTags() throws IOException, XMLStreamException {
        return getRootTags(inputValidator);
    }

    protected Collection<QName> getOutputHeaderTags() throws IOException, XMLStreamException {
        return getHeaderTags(outputValidator);
    }

    protected Collection<QName> getOutputBodyTags() throws IOException, XMLStreamException {
        return getRootTags((XmlValidator)getPipeLine().getOutputValidator());
    }

    protected void warn(String warning) {
        warning = "Warning: " + warning;
        if (!warnings.contains(warning)) {
            warnings.add(warning);
        }
    }

}

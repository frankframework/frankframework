/*
 * $Log: Wsdl.java,v $
 * Revision 1.16  2012-10-10 09:43:53  m00f069
 * Added comment on ESB_SOAP_JMS
 *
 * Revision 1.15  2012/10/04 11:28:57  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Fixed ESB Soap namespace
 * Added location (url) of WSDL generation to the WSDL documentation
 * Show warning add the bottom of the WSDL (if any) instead of Ibis logging
 *
 * Revision 1.14  2012/10/03 14:30:46  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Different filename for ESB Soap WSDL
 *
 * Revision 1.13  2012/10/03 12:22:41  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Different transport uri, jndi properties and connectionFactory for ESB Soap.
 * Fill targetAddress with a value when running locally.
 *
 * Revision 1.12  2012/10/02 16:12:14  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Bugfix for one-way WSDL (switched esbSoapOperationName and esbSoapOperationVersion).
 * Log a warning in case paradigm could not be extracted from the soap body.
 *
 * Revision 1.11  2012/10/01 15:23:44  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Strip schemaLocation from xsd import in case of generated WSDL with inline XSD's.
 *
 * Revision 1.10  2012/09/28 14:39:47  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Bugfix WSLD target namespace for ESB Soap, part XSD should be WSDL
 *
 * Revision 1.9  2012/09/27 14:28:59  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Better error message / adapter name when constructor throws exception.
 *
 * Revision 1.8  2012/09/27 13:44:31  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Updates in generating wsdl namespace, wsdl input message name, wsdl output message name, wsdl port type name and wsdl operation name in case of EsbSoap
 *
 * Revision 1.7  2012/09/26 12:41:05  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Bugfix in WSDL generator: Wrong prefix being used in element attribute of PipeLineInput and PipeLineOutput message part when using EsbSoapValidator.
 *
 * Revision 1.6  2012/08/23 11:57:43  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Updates from Michiel
 *
 * Revision 1.5  2012/05/08 15:53:59  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Fix invalid chars in wsdl:service name.
 *
 * Revision 1.4  2012/03/30 17:03:45  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Michiel added JMS binding/service to WSDL generator, made WSDL validator work for Bis WSDL and made console show syntax problems for schema's used in XmlValidator
 *
 * Revision 1.3  2012/03/16 15:35:43  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Michiel added EsbSoapValidator and WsdlXmlValidator, made WSDL's available for all adapters and did a bugfix on XML Validator where it seems to be dependent on the order of specified XSD's
 *
 * Revision 1.2  2011/12/15 10:08:06  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added CVS log
 *
 */
package nl.nn.adapterframework.soap;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.naming.NamingException;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.extensions.esb.EsbSoapWrapperPipe;
import nl.nn.adapterframework.http.WebServiceListener;
import nl.nn.adapterframework.jms.JmsListener;
import nl.nn.adapterframework.pipes.XmlValidator;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;

import org.apache.commons.lang.StringUtils;


/**
 *  Utility class to generate the WSDL. Straight-forwardly implemented using stax only.
 *
 *  An object of this class represents the WSDL associated with one IBIS pipeline.
 *
 *
 *  TODO http://cxf.547215.n5.nabble.com/ClassName-quot-is-already-defined-in-quot-during-compilation-after-code-generation-td4299849.html
 *
 *  TODO perhaps use wsdl4j or easywsdl to generate the WSDL more genericly (for easy switching between 1.1 and 2.0).
 *
 * @author  Michiel Meeuwissen
 */
class Wsdl {
    protected static final String XSD                   = XMLConstants.W3C_XML_SCHEMA_NS_URI;//"http://www.w3.org/2001/XMLSchema";
    protected static final String WSDL                  = "http://schemas.xmlsoap.org/wsdl/";
    protected static final String SOAP_WSDL             = "http://schemas.xmlsoap.org/wsdl/soap/";
    protected static final String SOAP_HTTP             = "http://schemas.xmlsoap.org/soap/http";
    protected static final String SOAP_JMS              = "http://www.w3.org/2010/soapjms/";
    protected static final String ESB_SOAP_JMS          = "http://www.tibco.com/namespaces/ws/2004/soap/binding/JMS";//Tibco BW will not detect the transport when SOAP_JMS is being used instead
    protected static final String ESB_SOAP_JNDI         = "http://www.tibco.com/namespaces/ws/2004/soap/apis/jndi";
    protected static final String ESB_SOAP_TNS_BASE_URI = "http://nn.nl/WSDL";

    protected static final QName NAME           = new QName(null, "name");
    protected static final QName TNS            = new QName(null, "targetNamespace");
    protected static final QName ELFORMDEFAULT  = new QName(null, "elementFormDefault");
    protected static final QName SCHEMA         = new QName(XSD,  "schema");
    protected static final QName ELEMENT        = new QName(XSD,  "element");
    protected static final QName IMPORT         = new QName(XSD,  "import");
    protected static final QName INCLUDE        = new QName(XSD,  "include");
    protected static final QName SCHEMALOCATION = new QName(null, "schemaLocation");
    protected static final QName NAMESPACE      = new QName(null, "namespace");
    protected static final QName XMLNS          = new QName(null, XMLConstants.XMLNS_ATTRIBUTE);

    private final String name;
    private final String filename;
    private final String targetNamespace;
    private final boolean indentWsdl;
    private final PipeLine pipeLine;
    private final XmlValidator inputValidator;
    private String webServiceListenerNamespace;

    private boolean recursiveXsds = false;
    private boolean includeXsds = false;

    private Set<XSD> xsds = null;

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

    Wsdl(PipeLine pipeLine, boolean indent, String documentation) {
        this.pipeLine = pipeLine;
        this.indentWsdl = indent;
        this.documentation = documentation;
        this.name = this.pipeLine.getAdapter().getName();
        if (this.name == null) {
            throw new IllegalArgumentException("The adapter '" + pipeLine.getAdapter() + "' has no name");
        }
        inputValidator = (XmlValidator)pipeLine.getInputValidator();
        if (inputValidator == null) {
            throw new IllegalStateException("The adapter '" + getName() + "' has no input validator");
        }
        AppConstants appConstants = AppConstants.getInstance();
        String tns = appConstants.getResolvedProperty("wsdl." + getName() + ".targetNamespace");
        if (tns == null) {
            tns = appConstants.getResolvedProperty("wsdl.targetNamespace");
        }
        if (tns == null) {
            EsbSoapWrapperPipe inputWrapper = getEsbSoapInputWrapper();
            EsbSoapWrapperPipe outputWrapper = getEsbSoapOutputWrapper();
            if (outputWrapper != null || (inputWrapper != null
                    && EsbSoapWrapperPipe.isValidNamespace(getFirstNamespaceFromSchemaLocation(inputValidator)))) {
                esbSoap = true;
                String outputParadigm = null;
                if (outputWrapper != null) {
                    esbSoapBusinessDomain = outputWrapper.getBusinessDomain();
                    esbSoapServiceName = outputWrapper.getServiceName();
                    esbSoapServiceContext = outputWrapper.getServiceContext();
                    esbSoapServiceContextVersion = outputWrapper.getServiceContextVersion();
                    esbSoapOperationName = outputWrapper.getOperationName();
                    esbSoapOperationVersion = outputWrapper.getOperationVersion();
                    outputParadigm = outputWrapper.getParadigm();
                } else {
                    // One-way WSDL
                    String s = getFirstNamespaceFromSchemaLocation(inputValidator);
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
                }
                String wsdlType = "abstract";
                for(IListener l : WsdlUtils.getListeners(pipeLine.getAdapter())) {
                    if (l instanceof JmsListener) {
                        wsdlType = "concrete";
                    }
                }
                filename = esbSoapBusinessDomain + "_"
                        + esbSoapServiceName + "_"
                        + esbSoapServiceContext + "_"
                        + esbSoapServiceContextVersion + "_"
                        + esbSoapOperationName + "_" + esbSoapOperationVersion
                        + "_" + wsdlType;
                tns = ESB_SOAP_TNS_BASE_URI + "/"
                        + esbSoapBusinessDomain + "/"
                        + esbSoapServiceName + "/"
                        + esbSoapServiceContext + "/"
                        + esbSoapServiceContextVersion;
                String inputParadigm = null;
                if (inputValidator instanceof SoapValidator) {
                    String soapBody = ((SoapValidator)inputValidator).getSoapBody();
                    if (soapBody != null) {
                        int i = soapBody.lastIndexOf('_');
                        if (i != -1) {
                            inputParadigm = soapBody.substring(i + 1);
                        }
                    }
                }
                if (inputParadigm != null) {
                    wsdlInputMessageName = esbSoapOperationName + "_"
                        + esbSoapOperationVersion + "_" + inputParadigm;
                } else {
                    warn("Could not extract paradigm from soapBody attribute of inputValidator");
                }
                if (outputParadigm != null) {
                    wsdlOutputMessageName = esbSoapOperationName + "_"
                        + esbSoapOperationVersion + "_" + outputParadigm;
                }
                wsdlPortTypeName = esbSoapOperationName + "_Interface_" + esbSoapOperationVersion;
                wsdlOperationName = esbSoapOperationName + "_" + esbSoapOperationVersion;
            } else {
                filename = name;
                for(IListener l : WsdlUtils.getListeners(pipeLine.getAdapter())) {
                    if (l instanceof WebServiceListener) {
                        webServiceListenerNamespace = ((WebServiceListener)l).getServiceNamespaceURI();
                        tns = webServiceListenerNamespace;
                    }
                }
                if (tns == null) {
                    tns = getFirstNamespaceFromSchemaLocation(inputValidator);
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
        } else {
            filename = name;
        }
        this.targetNamespace = WsdlUtils.validUri(tns);
    }

    /**
     * Writes the WSDL to an output stream
     * @param out
     * @param servlet  The servlet what is used as the web service (because this needs to be present in the WSDL)
     * @throws XMLStreamException
     * @throws IOException
     */
    public void wsdl(OutputStream out, String servlet) throws XMLStreamException, IOException, URISyntaxException, NamingException {
        XMLStreamWriter w = WsdlUtils.createWriter(out, indentWsdl);

        w.writeStartDocument(WsdlUtils.ENCODING, "1.0");
        w.setPrefix("wsdl", WSDL);
        w.setPrefix("xsd",  XSD);
        w.setPrefix("soap", SOAP_WSDL);
        if (esbSoap) {
            w.setPrefix("jms",  ESB_SOAP_JMS);
            w.setPrefix("jndi", ESB_SOAP_JNDI);
        } else {
            w.setPrefix("jms",  SOAP_JMS);
        }
        w.setPrefix("ibis", getTargetNamespace());
        for (XSD xsd : getXSDs()) {
            w.setPrefix(xsd.pref, xsd.nameSpace);
        }
        w.writeStartElement(WSDL, "definitions"); {
            w.writeNamespace("wsdl", WSDL);
            w.writeNamespace("xsd",  XSD);
            w.writeNamespace("soap", SOAP_WSDL);
            if (esbSoap) {
                w.writeNamespace("jndi", ESB_SOAP_JNDI);
            }
            w.writeNamespace("ibis", getTargetNamespace());
            for (XSD xsd : getXSDs()) {
                w.writeNamespace(xsd.pref, xsd.nameSpace);
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
     * Generates a zip file (and writes it to the given outputstream), containing the WSDL and all referenced XSD's.
     * @see {@link #wsdl(java.io.OutputStream, String)}
     */
    public void zip(OutputStream stream, String servletName) throws IOException, XMLStreamException, URISyntaxException, NamingException {
        ZipOutputStream out = new ZipOutputStream(stream);

        // First an entry for the WSDL itself:
        ZipEntry wsdlEntry = new ZipEntry(getFilename() + ".wsdl");
        out.putNextEntry(wsdlEntry);
        wsdl(out, servletName);
        out.closeEntry();

        //And then all XSD's
        setRecursiveXsds(true);
        Set<String> entries = new HashSet<String>();
        Map<String, String> correctingNamespaces = new HashMap<String, String>();
        for (XSD xsd : getXSDs()) {
            String zipName = xsd.getBaseUrl() + xsd.getName();
            if (entries.add(zipName)) {
                ZipEntry xsdEntry = new ZipEntry(zipName);
                out.putNextEntry(xsdEntry);
                XMLStreamWriter writer = WsdlUtils.createWriter(out, false);
                WsdlUtils.includeXSD(xsd, writer, correctingNamespaces, true, false);
                out.closeEntry();
            } else {
                warn("Duplicate xsds in " + this + " " + xsd + " " + getXSDs());
            }
        }
        out.close();
    }

    public String getName() {
        return name;
    }

    public String getFilename() {
        return filename;
    }

    protected String getTargetNamespace() {
        return targetNamespace;
    }

    private List<XSD> parseSchema(String schemaLocation) throws MalformedURLException, URISyntaxException {
        List<XSD> result = new ArrayList<XSD>();
        if (schemaLocation != null) {
            String[] split =  schemaLocation.split("\\s+");
            if (split.length % 2 != 0) throw new IllegalStateException("The schema must exist from an even number of strings, but it is " + schemaLocation);
            for (int i = 0; i < split.length; i += 2) {
                result.add(getXSD(split[i], split[i + 1]));
            }
        }
        return result;
    }


    /**
     * Returns a map: namespace -> Collection of all relevant XSD's
     * @return
     * @throws XMLStreamException
     * @throws IOException
     */
    Map<String, Collection<XSD>> getMappedXSDs() throws XMLStreamException, IOException, URISyntaxException {
        Map<String, Collection<XSD>> result = new HashMap<String, Collection<XSD>>();
        for (XSD xsd : getXSDs()) {
            Collection<XSD> col = result.get(xsd.nameSpace);
            if (col == null) {
                col = new ArrayList<XSD>();
                result.put(xsd.nameSpace, col);
            }
            col.add(xsd);
        }
        return result;
    }
    Set<XSD> getXSDs() throws IOException, XMLStreamException, URISyntaxException {
        if (xsds == null) {
            xsds = new TreeSet<XSD>();
            String inputSchema = inputValidator.getSchema();
            if (inputSchema != null) {
                // In case of a WebServiceListener using soap=true it might be
                // valid to use the schema attribute (in which case the schema
                // doesn't have a namespace) as the WebServiceListener will
                // remove the soap envelop and body element before it is
                // validated. In this case we use the serviceNamespaceURI from
                // the WebServiceListener as the namespace for the schema.
                if (webServiceListenerNamespace != null) {
                    XSD x = getXSD(webServiceListenerNamespace, inputSchema);
                    if (recursiveXsds) {
                        x.getImportXSDs(xsds);
                    }
                } else {
                    throw new IllegalStateException("The adapter " + pipeLine + " has an input validator using the schema attribute but a namespace is required");
                }
            }
            for (XSD x : parseSchema(inputValidator.getSchemaLocation())) {
                if (recursiveXsds) {
                    x.getImportXSDs(xsds);
                }
            }
            XmlValidator outputValidator = (XmlValidator) pipeLine.getOutputValidator();
            if (outputValidator != null) {
                String outputSchema = outputValidator.getSchema();
                if (outputSchema != null) {
                    getXSD(null, outputSchema);
                }
                parseSchema(outputValidator.getSchemaLocation());
            }
        }
        return xsds;
    }
    protected XSD getXSD(String nameSpace) throws IOException, XMLStreamException, URISyntaxException {
        if (nameSpace == null) throw new IllegalArgumentException("Cannot get an XSD for null namespace");
        for (XSD xsd : getXSDs()) {
            if (xsd.nameSpace.equals(nameSpace)) {
                return xsd;
            }
        }
        throw new IllegalArgumentException("No xsd for namespace '" + nameSpace + "' found (known are " + getXSDs() + ")");
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
    protected void types(XMLStreamWriter w) throws XMLStreamException, IOException, URISyntaxException {
        w.writeStartElement(WSDL, "types");
        Map<String, String> correctingNamesSpaces = new HashMap<String, String>();
        if (includeXsds) {
            for (XSD xsd : getXSDs()) {
                WsdlUtils.includeXSD(xsd, w, correctingNamesSpaces, false, true);
            }
        }  else {
            for (Map.Entry<String, Collection<XSD>> xsd: getMappedXSDs().entrySet()) {
                WsdlUtils.xsincludeXSDs(xsd.getKey(), xsd.getValue(), w, correctingNamesSpaces);
            }
        }
        w.writeEndElement();
    }


    /**
     * Outputs the 'messages' section.
     * @param w
     * @throws XMLStreamException
     * @throws IOException
     */
    protected void messages(XMLStreamWriter w) throws XMLStreamException, IOException, URISyntaxException {
        message(w, "Header", getHeaderTags());
        message(w, wsdlInputMessageName, getInputTags());
        XmlValidator outputValidator = (XmlValidator) pipeLine.getOutputValidator();
        if (outputValidator != null) {
            Collection<QName> out = getOutputTags();
            message(w, wsdlOutputMessageName, out);
        } else {
            // One-way WSDL
        }
        //message(w, "PipeLineFault", "error", "bla:bloe");
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

    protected void portType(XMLStreamWriter w) throws XMLStreamException, IOException, URISyntaxException {
        w.writeStartElement(WSDL, "portType");
        w.writeAttribute("name", wsdlPortTypeName); {
            w.writeStartElement(WSDL, "operation");
            w.writeAttribute("name", wsdlOperationName); {
                w.writeEmptyElement(WSDL, "input");
                w.writeAttribute("message", "ibis:" + wsdlInputMessageName);

                if (getOutputTags() != null) {
                    w.writeEmptyElement(WSDL, "output");
                    w.writeAttribute("message", "ibis:" + wsdlOutputMessageName);
                }
                /*
               w.writeEmptyElement(WSDL, "fault");
               w.writeAttribute("message", "ibis:PipeLineFault");
               */

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

    private EsbSoapWrapperPipe getEsbSoapInputWrapper() {
        IPipe inputWrapper = pipeLine.getInputWrapper();
        if (inputWrapper instanceof  EsbSoapWrapperPipe) {
            return (EsbSoapWrapperPipe) inputWrapper;
        }
        return null;
    }

    private EsbSoapWrapperPipe getEsbSoapOutputWrapper() {
        IPipe outputWrapper = pipeLine.getOutputWrapper();
        if (outputWrapper instanceof  EsbSoapWrapperPipe) {
            return (EsbSoapWrapperPipe) outputWrapper;
        }
        return null;
    }

    protected void binding(XMLStreamWriter w) throws XMLStreamException, IOException, URISyntaxException {
        for (IListener listener : WsdlUtils.getListeners(pipeLine.getAdapter())) {
            if (listener instanceof WebServiceListener) {
                httpBinding(w);
            } else if (listener instanceof JmsListener) {
                jmsBinding(w, (JmsListener) listener);
            } else {
                w.writeComment("Binding: Unrecognized listener " + listener.getClass() + ": " + listener.getName());
            }
        }
    }

    protected void httpBinding(XMLStreamWriter w) throws XMLStreamException, IOException, URISyntaxException {
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

    protected void writeSoapOperation(XMLStreamWriter w) throws XMLStreamException, IOException, URISyntaxException {

        w.writeStartElement(WSDL, "operation");
        w.writeAttribute("name", wsdlOperationName); {

            w.writeEmptyElement(SOAP_WSDL, "operation");
            w.writeAttribute("style", "document");
            w.writeAttribute("soapAction", getSoapAction());


            w.writeStartElement(WSDL, "input"); {
                writeSoapHeader(w);
                Collection<QName>  inputTags = getInputTags();
                //w.writeEmptyElement(input.xsd.nameSpace, input.getRootTag());
                w.writeEmptyElement(SOAP_WSDL, "body");
                writeParts(w, inputTags);
                w.writeAttribute("use", "literal");
            }
            w.writeEndElement();

            Collection<QName> outputTags = getOutputTags();
            if (outputTags != null) {
                w.writeStartElement(WSDL, "output"); {
                    writeSoapHeader(w);
                    ///w.writeEmptyElement(outputTag.xsd.nameSpace, outputTag.getRootTag());
                    w.writeEmptyElement(SOAP_WSDL, "body");
                    writeParts(w, outputTags);
                    w.writeAttribute("use", "literal");
                }
                w.writeEndElement();
            }

            /*
            w.writeStartElement(WSDL, "fault"); {
                w.writeEmptyElement(SOAP_WSDL, "error");
                w.writeAttribute("use", "literal");
            }
            w.writeEndElement();
            */
        }
        w.writeEndElement();
    }

    protected void writeSoapHeader(XMLStreamWriter w) throws XMLStreamException, IOException, URISyntaxException {
        Collection<QName>  headers = getHeaderTags();
        if (! headers.isEmpty()) {
            if (headers.size() > 1) {
                warn("Can only deal with one soap header. Taking only the first of " + headers);
            }
            w.writeEmptyElement(SOAP_WSDL, "header");
            w.writeAttribute("part", getIbisName(headers.iterator().next()));
            w.writeAttribute("use",     "literal");
            w.writeAttribute("message", "ibis:Header");
        }
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

    protected void jmsBinding(XMLStreamWriter w, JmsListener listener) throws XMLStreamException, IOException, URISyntaxException {
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
            } else {
                w.writeComment("Service: Unrecognized listener " + listener.getClass() + " " + listener);
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
                        String destinationType = listener.getDestinationType();
                        if (destinationType != null) {
                            w.writeAttribute("destination", destinationType);
                        }
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
                String qcf = "";
                String stage = "";
                try {
                    qcf = URLEncoder.encode(
                            listener.getQueueConnectionFactoryName(), "UTF-8");
                    stage = URLEncoder.encode(
                            AppConstants.getInstance().getResolvedProperty("otap.stage"),
                            "UTF-8");
                } catch (UnsupportedEncodingException e) {
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

    protected Collection<QName> getHeaderTags(XmlValidator xmlValidator) throws XMLStreamException, IOException, URISyntaxException {
        if (xmlValidator instanceof SoapValidator) {
            String root = ((SoapValidator)xmlValidator).getSoapHeader();
            QName q = getRootTag(root);
            if (q != null) {
                return Collections.singleton(q);
            }
        }
        return Collections.emptyList();
    }

    protected Collection<QName> getRootTags(XmlValidator xmlValidator) throws IOException, XMLStreamException, URISyntaxException {
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

    protected QName getRootTag(String tag) throws XMLStreamException, IOException, URISyntaxException {
        if (StringUtils.isNotEmpty(tag)) {
            for (XSD xsd : xsds) {
                for (String rootTag : xsd.rootTags) {
                    if (tag.equals(rootTag)) {
                        return xsd.getTag(tag);
                    }
                }
            }
            warn("Root element '" + tag + "' not found in XSD's");
        }
        return null;
    }

    protected Collection<QName> getHeaderTags() throws IOException, XMLStreamException, URISyntaxException {
        return getHeaderTags(inputValidator);
    }

    protected Collection<QName> getInputTags() throws IOException, XMLStreamException, URISyntaxException {
        return getRootTags(inputValidator);
    }

    protected Collection<QName> getOutputTags() throws IOException, XMLStreamException, URISyntaxException {
        XmlValidator outputValidator = (XmlValidator) getPipeLine().getOutputValidator();
        if (outputValidator != null) {
            return getRootTags((XmlValidator) getPipeLine().getOutputValidator());
        } else {
            // One-way WSDL
            return null;
        }
    }

    protected String getFirstNamespaceFromSchemaLocation(XmlValidator inputValidator) {
        String schemaLocation = inputValidator.getSchemaLocation();
        if (schemaLocation != null) {
            String[] split =  schemaLocation.split("\\s+");
            if (split.length > 0) {
                return split[0];
            }
        }
        return null;
    }

    protected void warn(String warning) {
        warning = "Warning: " + warning;
        if (!warnings.contains(warning)) {
            warnings.add(warning);
        }
    }

    private XSD getXSD(String ns, String resource) throws URISyntaxException {
        URI url = ClassUtils.getResourceURL(resource).toURI();
        if (url == null) {
            throw new IllegalArgumentException("No such resource " + resource);
        }
        for (XSD xsd : xsds) {
            if (xsd.nameSpace == null) {
                if (xsd.url.equals(url)) {
                    return xsd;
                }
            } else  {
                if (xsd.nameSpace.equals(ns) && xsd.url.equals(url)){
                    return xsd;
                }
            }
        }
        XSD xsd = new XSD("", ns, url, xsds.size() + 1);
        xsds.add(xsd);
        return  xsd;
    }

    public boolean isRecursiveXsds() {
        return recursiveXsds;
    }

    /**
     * Make an effort to collect all XSD's also the included ones in {@link #getXSDs()}
     * @param recursiveXsds
     */
    public void setRecursiveXsds(boolean recursiveXsds) {
        this.recursiveXsds = recursiveXsds;
        xsds = null;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public boolean isIncludeXsds() {
        return includeXsds;
    }

    public void setIncludeXsds(boolean includeXsds) {
        this.includeXsds = includeXsds;
    }

    /*
        public void easywsdl(OutputStream out, String servlet) throws XMLStreamException, IOException, SchemaException, URISyntaxException {
            Description description = WSDLFactory.newInstance().newDescription(AbsItfDescription.WSDLVersionConstants.WSDL11);

            SchemaFactory fact = SchemaFactory.newInstance();
            Types t = description.getTypes();
            for (XSD xsd : getXsds()) {
                description.addNamespace(xsd.pref, xsd.nameSpace);
                SchemaReader sr = fact.newSchemaReader();
                Schema s = sr.read(xsd.url);
                t.addSchema(s);
            }
            InterfaceType it = description.createInterface();
            Operation o = it.createOperation();
            Input i = o.createInput();
            i.setName("IbisInput");
            //i.
            // s
            t.addOperation(it.createOperation());
            description.addInterface();
            Binding binding = description.createBinding();
            BindingOperation op = binding.createBindingOperation();
            //description.addBinding(binding);
        }
    */
}

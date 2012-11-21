/*
 * $Log: Wsdl.java,v $
 * Revision 1.27  2012-11-21 14:11:00  m00f069
 * Bugfix: Get root tags from root xsd's only otherwise when root tag name found in non root xsd, the non root xsd may be used which has no prefix, hence an exception is thrown.
 *
 * Revision 1.26  2012/10/26 15:43:18  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Made WSDL without separate XSD's the default
 *
 * Revision 1.25  2012/10/24 14:34:00  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Load imported XSD's into the WSDL too
 * When more than one XSD with the same namespace is present merge them into one schema element in the WSDL
 * Exclude SOAP Envelope XSD
 *
 * Revision 1.24  2012/10/19 11:52:28  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Removed unused param
 *
 * Revision 1.23  2012/10/17 13:02:10  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Check paradigm against list of valid values.
 * Use schemaLocation instead of outputWrapper as main source for ESB SOAP vars.
 *
 * Revision 1.22  2012/10/17 08:40:49  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added esbSoapOperationName and esbSoapOperationVersion
 *
 * Revision 1.21  2012/10/12 13:07:47  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Removed "Unrecognized listener" comment from WSDL (when no useful listener found WSDL is marked abstract)
 *
 * Revision 1.20  2012/10/12 09:55:17  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Some extra checks on configuration at WSDL listing time to prevent the user from being disappointed at WSDL generation time.
 * Handle retrieval of XSD's from outputValidator the same as for inputValidator (check on usage of schema instead of schemaLocation attribute and usage of recursiveXsds)
 *
 * Revision 1.19  2012/10/11 10:01:58  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Use concrete filename with WebServiceListener too
 *
 * Revision 1.18  2012/10/11 09:45:58  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added WSDL filename to WSDL documentation
 *
 * Revision 1.17  2012/10/11 09:10:43  Jaco de Groot <jaco.de.groot@ibissource.org>
 * To lower case on targetAddress destination (QUEUE -> queue)
 *
 * Revision 1.16  2012/10/10 09:43:53  Jaco de Groot <jaco.de.groot@ibissource.org>
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
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;

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
 * @author  Jaco de Groot
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

    protected static final List<String> excludeXsds = new ArrayList<String>();
    static {
        excludeXsds.add("http://schemas.xmlsoap.org/soap/envelope/");
    };

    private final String name;
    private final String filename;
    private final String targetNamespace;
    private final PipeLine pipeLine;
    private final XmlValidator inputValidator;
    private final XmlValidator outputValidator;
    private String webServiceListenerNamespace;

    private boolean indent = true;
    private boolean useSeparateXsds = false;
    private LinkedHashMap<String, String> namespacesByPrefix;
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
            EsbSoapWrapperPipe inputWrapper = getEsbSoapInputWrapper();
            EsbSoapWrapperPipe outputWrapper = getEsbSoapOutputWrapper();
            if (inputWrapper != null || outputWrapper != null) {
                esbSoap = true;
                String schemaLocation = getFirstNamespaceFromSchemaLocation(inputValidator);
                if (EsbSoapWrapperPipe.isValidNamespace(schemaLocation)) {
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
                } else {
                    warn("Namespace '" + schemaLocation + "' invalid according to ESB SOAP standard");
                    if (outputWrapper != null) {
                        esbSoapBusinessDomain = outputWrapper.getBusinessDomain();
                        esbSoapServiceName = outputWrapper.getServiceName();
                        esbSoapServiceContext = outputWrapper.getServiceContext();
                        esbSoapServiceContextVersion = outputWrapper.getServiceContextVersion();
                        esbSoapOperationName = outputWrapper.getOperationName();
                        esbSoapOperationVersion = outputWrapper.getOperationVersion();
                    } else {
                        // One-way WSDL
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
                    String inputParadigm = getEsbSoapParadigm(inputValidator);
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
                        warn("Could not extract paradigm from soapBody attribute of inputValidator");
                    }
                    String outputParadigm = getEsbSoapParadigm(outputValidator);
                    if (outputParadigm != null) {
                        wsdlOutputMessageName = esbSoapOperationName + "_"
                            + esbSoapOperationVersion + "_" + outputParadigm;
                        if (!"Response".equals(outputParadigm)) {
                            warn("Paradigm for output message which was extracted from soapBody should be Response instead of '"
                                    + outputParadigm + "'");
                        }
                    } else {
                        if (outputWrapper != null) {
                            warn("Could not extract paradigm from soapBody attribute of outputValidator");
                        } else {
                            // One-way WSDL
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
        }
        this.filename = filename;
        this.targetNamespace = WsdlUtils.validUri(tns);
        try {
            // Check on IllegalStateException which might occur while generating
            // the WSDL and throw them now to prevent the user from being
            // disappointed at WSDL generation time.
            getXsds(true);
        } catch(Exception e) {
            if (e instanceof IllegalStateException) {
                throw (IllegalStateException)e;
            }
        }
    }

    /**
     * Writes the WSDL to an output stream
     * @param out
     * @param servlet  The servlet what is used as the web service (because this needs to be present in the WSDL)
     * @throws XMLStreamException
     * @throws IOException
     */
    public void wsdl(OutputStream out, String servlet) throws XMLStreamException, IOException, NamingException {
        XMLStreamWriter w = WsdlUtils.createWriter(out, isIndent());

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
        for (String prefix: getPrefixes()) {
            w.setPrefix(prefix, getNamespace(prefix));
        }
        w.writeStartElement(WSDL, "definitions"); {
            w.writeNamespace("wsdl", WSDL);
            w.writeNamespace("xsd",  XSD);
            w.writeNamespace("soap", SOAP_WSDL);
            if (esbSoap) {
                w.writeNamespace("jndi", ESB_SOAP_JNDI);
            }
            w.writeNamespace("ibis", getTargetNamespace());
            for (String prefix: getPrefixes()) {
                w.writeNamespace(prefix, getNamespace(prefix));
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
    public void zip(OutputStream stream, String servletName) throws IOException, XMLStreamException, NamingException {
        ZipOutputStream out = new ZipOutputStream(stream);

        // First an entry for the WSDL itself:
        ZipEntry wsdlEntry = new ZipEntry(getFilename() + ".wsdl");
        out.putNextEntry(wsdlEntry);
        wsdl(out, servletName);
        out.closeEntry();

        //And then all XSD's
        Set<String> entries = new HashSet<String>();
        Map<String, String> correctingNamespaces = new HashMap<String, String>();
        for (XSD xsd : getXsds()) {
            String zipName = xsd.getBaseUrl() + xsd.getName();
            if (entries.add(zipName)) {
                ZipEntry xsdEntry = new ZipEntry(zipName);
                out.putNextEntry(xsdEntry);
                XMLStreamWriter writer = WsdlUtils.createWriter(out, false);
                WsdlUtils.includeXSD(xsd, writer, correctingNamespaces, true);
                out.closeEntry();
            } else {
                warn("Duplicate xsds in " + this + " " + xsd + " " + getXsds());
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

    public boolean isUseSeparateXsds() {
        return useSeparateXsds;
    }

    public void setUseSeparateXsds(boolean useSeparateXsds) {
        this.useSeparateXsds = useSeparateXsds;
    }

    public static String getEsbSoapParadigm(XmlValidator xmlValidator) {
        if (xmlValidator instanceof SoapValidator) {
            String soapBody = ((SoapValidator)xmlValidator).getSoapBody();
            if (soapBody != null) {
                int i = soapBody.lastIndexOf('_');
                if (i != -1) {
                    return soapBody.substring(i + 1);
                }
            }
        }
        return null;
    }

    public static String getFirstNamespaceFromSchemaLocation(XmlValidator inputValidator) {
        String schemaLocation = inputValidator.getSchemaLocation();
        if (schemaLocation != null) {
            String[] split =  schemaLocation.trim().split("\\s+");
            if (split.length > 0) {
                return split[0];
            }
        }
        return null;
    }

    public static Map<String, Collection<XSD>> getXsdsGroupedByNamespace(
    		Set<XSD> xsds, boolean rootXsdsOnly)
    				throws XMLStreamException, IOException {
        Map<String, Collection<XSD>> result = new HashMap<String, Collection<XSD>>();
        for (XSD xsd : xsds) {
            if (xsd.isRootXsd || !rootXsdsOnly) {
                Collection<XSD> col = result.get(xsd.nameSpace);
                if (col == null) {
                    col = new ArrayList<XSD>();
                    result.put(xsd.nameSpace, col);
                }
                col.add(xsd);
            }
        }
        return result;
    }

    private List<String> getPrefixes() throws IOException, XMLStreamException {
        if (namespacesByPrefix == null) {
            getXsds();
        }
    	List<String> result = new ArrayList<String>();
    	for (String prefix: namespacesByPrefix.keySet()) {
    		result.add(prefix);
    	}
        return result;
    }

    private String getNamespace(String prefix) throws IOException, XMLStreamException {
        if (namespacesByPrefix == null) {
            getXsds();
        }
        return namespacesByPrefix.get(prefix);
    }

    private Set<XSD> getXsds() throws IOException, XMLStreamException {
        return getXsds(false);
    }

    private Set<XSD> getXsds(boolean checkSchemaLocationOnly) throws IOException, XMLStreamException {
        if (xsds == null) {
            if (!checkSchemaLocationOnly) {
                xsds = new TreeSet<XSD>();
            }
            xsds.addAll(initXsds(inputValidator, checkSchemaLocationOnly));
            if (outputValidator != null) {
                xsds.addAll(initXsds(outputValidator, checkSchemaLocationOnly));
            }
            if (!checkSchemaLocationOnly) {
                namespacesByPrefix = new LinkedHashMap<String, String>();
                int prefixCount = 1;
                Map<String, Collection<XSD>> xsdsGroupedByNamespace =
                        getXsdsGroupedByNamespace(xsds, true);
                for (String namespace: xsdsGroupedByNamespace.keySet()) {
                    Collection<XSD> xsds = xsdsGroupedByNamespace.get(namespace);
                    for (XSD xsd: xsds) {
                        xsd.prefix = "ns" + prefixCount;
                    }
                    namespacesByPrefix.put("ns" + prefixCount, namespace);
                    prefixCount++;
                }
            }
        }
        return xsds;
    }

    private Set<XSD> initXsds(XmlValidator xmlValidator,
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
                    XSD xsd = initXSD(webServiceListenerNamespace, inputSchema, true);
                    xsds.add(xsd);
                    xsds.addAll(xsd.getImportXsds());
                }
            } else {
                throw new IllegalStateException("The adapter " + pipeLine + " has a validator using the schema attribute but a namespace is required");
            }
        } else {
            Set<XSD> rootXsds = initXsds(xmlValidator.getSchemaLocation(),
                    checkSchemaLocationOnly);
            xsds.addAll(rootXsds);
            for (XSD xsd : rootXsds) {
                xsds.addAll(xsd.getImportXsds());
            }
        }
        return xsds;
    }

    private Set<XSD> initXsds(String schemaLocation,
            boolean checkSchemaLocationOnly) throws MalformedURLException {
        Set<XSD> xsds = new TreeSet<XSD>();
        if (schemaLocation != null) {
            String[] split =  schemaLocation.trim().split("\\s+");
            if (split.length % 2 != 0) throw new IllegalStateException("The schema must exist from an even number of strings, but it is " + schemaLocation);
            if (!checkSchemaLocationOnly) {
                for (int i = 0; i < split.length; i += 2) {
                    if (!excludeXsds.contains(split[i])) {
                        xsds.add(initXSD(split[i], split[i + 1], true));
                    }
                }
            }
        }
        return xsds;
    }

    private XSD initXSD(String ns, String resource, boolean rootXsd) {
        URL url = ClassUtils.getResourceURL(resource);
        if (url == null) {
            throw new IllegalArgumentException("No such resource " + resource);
        }
        XSD xsd = new XSD("", ns, url, rootXsd);
        return  xsd;
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
        Map<String, String> correctingNamesSpaces = new HashMap<String, String>();
        Map<String, Collection<XSD>> xsdsGroupedByNamespace;
        if (isUseSeparateXsds()) {
            xsdsGroupedByNamespace = getXsdsGroupedByNamespace(getXsds(), true);
            for (String namespace: xsdsGroupedByNamespace.keySet()) {
                WsdlUtils.xsincludeXsds(namespace,
                        xsdsGroupedByNamespace.get(namespace), w,
                        correctingNamesSpaces, true);
            }
        }  else {
            xsdsGroupedByNamespace = getXsdsGroupedByNamespace(getXsds(), false);
            for (String namespace: xsdsGroupedByNamespace.keySet()) {
                Collection<XSD> xsds = xsdsGroupedByNamespace.get(namespace);
                if (xsds.size() == 1) {
                    WsdlUtils.includeXSD(xsds.iterator().next(), w,
                            correctingNamesSpaces, false, true);
                           
                } else {
                    // Get attributes of root element and imports from all XSD's
                    List<Attribute> rootAttributes = new ArrayList<Attribute>();
                    List<Attribute> rootNamespaceAttributes = new ArrayList<Attribute>();
                    List<XMLEvent> imports = new ArrayList<XMLEvent>();
                    for (XSD xsd: xsds) {
                        WsdlUtils.includeXSD(xsd, w, correctingNamesSpaces,
                                false, true, false, false, rootAttributes,
                                rootNamespaceAttributes, imports, true);
                    }
                    // Write XSD's with merged root element and imports
                    int i = 0;
                    for (XSD xsd: xsds) {
                        i++;
                        boolean skipFirstElement = false;
                        boolean skipLastElement = false;
                        if (xsds.size() > 1) {
                            if (i == 1) {
                                skipLastElement = true;
                            } else if (i == xsds.size()) {
                                skipFirstElement = true;
                            }
                        }
                        WsdlUtils.includeXSD(xsd, w, correctingNamesSpaces,
                                false, true, skipFirstElement, skipLastElement,
                                rootAttributes, rootNamespaceAttributes,
                                imports, false);
                    }
                }
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
    protected void messages(XMLStreamWriter w) throws XMLStreamException, IOException {
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

    protected void portType(XMLStreamWriter w) throws XMLStreamException, IOException {
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

    protected void writeSoapHeader(XMLStreamWriter w) throws XMLStreamException, IOException {
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

    protected EsbSoapWrapperPipe getEsbSoapInputWrapper() {
        IPipe inputWrapper = pipeLine.getInputWrapper();
        if (inputWrapper instanceof  EsbSoapWrapperPipe) {
            return (EsbSoapWrapperPipe) inputWrapper;
        }
        return null;
    }

    protected EsbSoapWrapperPipe getEsbSoapOutputWrapper() {
        IPipe outputWrapper = pipeLine.getOutputWrapper();
        if (outputWrapper instanceof  EsbSoapWrapperPipe) {
            return (EsbSoapWrapperPipe) outputWrapper;
        }
        return null;
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
            for (XSD xsd : xsds) {
                if (xsd.isRootXsd) {
                    for (String rootTag : xsd.rootTags) {
                        if (tag.equals(rootTag)) {
                            return xsd.getTag(tag);
                        }
                    }
                }
            }
            warn("Root element '" + tag + "' not found in XSD's");
        }
        return null;
    }

    protected Collection<QName> getHeaderTags() throws IOException, XMLStreamException {
        return getHeaderTags(inputValidator);
    }

    protected Collection<QName> getInputTags() throws IOException, XMLStreamException {
        return getRootTags(inputValidator);
    }

    protected Collection<QName> getOutputTags() throws IOException, XMLStreamException {
        XmlValidator outputValidator = (XmlValidator) getPipeLine().getOutputValidator();
        if (outputValidator != null) {
            return getRootTags((XmlValidator) getPipeLine().getOutputValidator());
        } else {
            // One-way WSDL
            return null;
        }
    }

    protected void warn(String warning) {
        warning = "Warning: " + warning;
        if (!warnings.contains(warning)) {
            warnings.add(warning);
        }
    }

    /*
        public void easywsdl(OutputStream out, String servlet) throws XMLStreamException, IOException, SchemaException {
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

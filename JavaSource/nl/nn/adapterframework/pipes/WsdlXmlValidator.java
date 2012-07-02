/*
 * $Log: WsdlXmlValidator.java,v $
 * Revision 1.5  2012-07-02 08:39:08  m00f069
 * Added note about WebSphere versions
 * Added CVS log
 *
 */
package nl.nn.adapterframework.pipes;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.wsdl.Definition;
import javax.wsdl.Operation;
import javax.wsdl.Part;
import javax.wsdl.PortType;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.schema.SchemaImport;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.soap.SoapValidator;
import nl.nn.adapterframework.util.ClassUtils;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.ibm.wsdl.extensions.schema.SchemaSerializer;

/**
 * Wsdl based input validator. Given an WSDL, it validates input. Note: Doesn't
 * work/compile with WebSphere 6.1, with WebSphere 7.0 it does.
 * 
 * @author Michiel Meeuwissen
 */
public class WsdlXmlValidator extends FixedForwardPipe {

    private static final Logger LOG = LogManager.getLogger(WsdlXmlValidator.class);

    private static final QName SCHEMA       = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "schema", "");
    private static final Set<QName> SCHEMAS = new HashSet<QName>(Arrays.asList(
        SCHEMA,
        new QName("http://www.w3.org/2000/10/XMLSchema", "schema", "")));

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


    private static DOMImplementationLS DOM;
    static {
        try {
            DOMImplementation impl  = DOMImplementationRegistry.newInstance().getDOMImplementation("XML 3.0");
            DOM = (DOMImplementationLS) impl;
        } catch (ClassNotFoundException e) {
            LOG.error(e.getMessage(), e);
        } catch (InstantiationException e) {
            LOG.error(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            LOG.error(e.getMessage(), e);
        }


    }

    private Definition def;
    private boolean throwException = false;

    private SoapValidator.SoapVersion validateSoapEnvelope = SoapValidator.SoapVersion.VERSION_1_1;


    public void setWsdl(String uri) throws IOException, WSDLException {
        def = getDefinition(ClassUtils.getResourceURL(uri));
    }


    public boolean isValidateSoapEnvelope() {
        return validateSoapEnvelope != null;
    }
    /**
     * You can disable validating the SOAP envelope. If for some reason that is possible and desirable.
     * @param validateSoapEnvelope false, true, 1.1 or 1.2
     */
    public void setValidateSoapEnvelope(String validateSoapEnvelope) {
        if (validateSoapEnvelope == null || "false".equals(validateSoapEnvelope)) {
            this.validateSoapEnvelope = null;
        } else if ("true".equals(validateSoapEnvelope)) {
            this.validateSoapEnvelope = SoapValidator.SoapVersion.VERSION_1_2;
        } else {
            this.validateSoapEnvelope = SoapValidator.SoapVersion.fromAttribute(validateSoapEnvelope);
        }
    }


    @Override
    public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
        try {
            pipe((String) input);
            return new PipeRunResult(getForward(), input);
        } catch (WSDLException e) {
            LOG.error(e.getMessage(), e);
            return new PipeRunResult(findForward("illegalWsdl"), input);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return new PipeRunResult(findForward("failure"), input);
        } catch (SAXException e) {
            LOG.error(e.getMessage(), e);
            if (isThrowException()) {
                throw new PipeRunException(this, getLogPrefix(session), e);
            } else {
                return new PipeRunResult(findForward("parserError"), input);
            }
        }
    }

    protected void pipe(String input) throws IOException, WSDLException, SAXException {
        final javax.xml.validation.Schema xsd;
        if (isValidateSoapEnvelope()) {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setResourceResolver(getLSResourceResolver());
            xsd = factory.newSchema(new Source[] {
                new StreamSource(SoapValidator.class.getResourceAsStream(validateSoapEnvelope.xsd)),
                new DOMSource(getInputSchema().getElement())
            });
        } else {
            Schema inputSchema = getInputSchema();
            if (inputSchema == null) throw new IllegalStateException("No input schema found on " + def);
            xsd = getSchema(inputSchema);
        }

        validate(new StringReader(input), xsd);

    }

    protected Definition getDefinition(URL url) throws WSDLException, IOException {
        InputSource source = new InputSource(url.openStream());
        source.setSystemId(url.toString());
        WSDLReader reader  = FACTORY.newWSDLReader();
        reader.setFeature("javax.wsdl.verbose",         true);
        reader.setFeature("javax.wsdl.importDocuments", true);
        return reader.readWSDL(url.toString(), source);
    }

    protected void addNamespaces(Schema schema, Map<String, String> namespaces) {
        for (Map.Entry<String,String> e : namespaces.entrySet()) {
            String key = e.getKey().length() == 0 ? "xmlns" : ("xmlns:" + e.getKey());
            if (schema.getElement().getAttribute(key).length() == 0) {
                schema.getElement().setAttribute(key, e.getValue());
            }
        }
    }

    protected LSResourceResolver getLSResourceResolver() {
        return new LSResourceResolver() {
            //@Override // java 6
            public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
                LSInput lsinput = DOM.createLSInput();
                Schema schema = getSchema(namespaceURI);
                if (schema == null) {
                    throw new IllegalStateException("No schema referenced by " + namespaceURI);
                }
                addNamespaces(schema, def.getNamespaces());

                try {
                    lsinput.setCharacterStream(toReader(schema));
                } catch (WSDLException e) {
                    LOG.error(e.getMessage(), e);
                }
                return lsinput;
            }
        };
    }

    protected javax.xml.validation.Schema getSchema(Schema wsdlSchema) throws WSDLException, SAXException {
        SchemaFactory factory = SchemaFactory.newInstance(wsdlSchema.getElementType().getNamespaceURI());
        factory.setResourceResolver(getLSResourceResolver());
        addNamespaces(wsdlSchema, def.getNamespaces());
        return factory.newSchema(new StreamSource(toReader(wsdlSchema)));
    }

    protected void validate(
        final Reader input,
        final javax.xml.validation.Schema xsd) throws WSDLException, IOException, SAXException {


        Validator validator = xsd.newValidator();
        validator.setResourceResolver(getLSResourceResolver());
        Source source = new StreamSource(input);
        validator.validate(source);
    }

    protected  String toString(Schema wsdlSchema) throws WSDLException {
        SchemaSerializer schemaSerializer = new SchemaSerializer();
        StringWriter w = new StringWriter();
        PrintWriter res = new PrintWriter(w);
        schemaSerializer.marshall(Object.class,
            SCHEMA,
            wsdlSchema,
            res,
            def,
            def.getExtensionRegistry());
        return w.toString().trim();
    }

    protected Reader toReader(Schema wsdlSchema) throws WSDLException {
        return new StringReader(toString(wsdlSchema));
    }

    protected Schema getInputSchema() {
        Map<String, PortType> portTypes = def.getPortTypes();
        PortType type = (PortType) ((Map.Entry) portTypes.entrySet().iterator().next()).getValue();
        Operation operation = (Operation) type.getOperations().get(0);
        Part requestPart = (Part) operation.getInput().getMessage().getParts().values().iterator().next();
        String tns = requestPart.getElementName().getNamespaceURI();
        Schema target = null;
        for (Iterator i = def.getTypes().getExtensibilityElements().iterator(); i.hasNext();) {
            ExtensibilityElement element = (ExtensibilityElement) i.next();
            QName qn = element.getElementType();
            if (SCHEMAS.contains(qn)) {
                Schema schema = (Schema) element;
                String schemaTns = schema.getElement().getAttribute("targetNamespace");
                if (schemaTns.equalsIgnoreCase(tns)) {
                    target = schema;
                    break;
                }
                List<SchemaImport> si = (List) schema.getImports().get(tns);
                if (si != null && si.size() > 0) {
                    target = schema;
                    break;
                }
            }
        }
        return target;

    }


    /**
     * Indicates wether to throw an error (piperunexception) when
     * the xml is not compliant
     */
    public void setThrowException(boolean throwException) {
        this.throwException = throwException;
    }

    public boolean isThrowException() {
        return throwException;
    }
    /**
     * Given a namespace calculates the SchemaImport for a WSDL.
     * @OTODO This seems uncessarily cumbersome
     *
     */
    protected Schema getSchema(final String nameSpace) {
        List types = def.getTypes().getExtensibilityElements();
        for (Iterator i = types.iterator(); i.hasNext();) {
            ExtensibilityElement type = (ExtensibilityElement) i.next();
            QName qn = type.getElementType();
            if (SCHEMA.equals(qn)) {
                Schema schema = (Schema) type;
                if (schema.getElement().getAttribute("targetNamespace").equals(nameSpace)) {
                    return schema;
                }
                Map<String, List<SchemaImport>> imports = schema.getImports();
                List<SchemaImport> si= (List) imports.get(nameSpace);
                if (si != null) {
                    for (Iterator j = si.iterator(); j.hasNext();) {
                        SchemaImport s = (SchemaImport) j.next();
                        if (s.getReferencedSchema() != null) {
                            return s.getReferencedSchema();
                        }
                    }
                }
            }
        }
        return null;
    }
}

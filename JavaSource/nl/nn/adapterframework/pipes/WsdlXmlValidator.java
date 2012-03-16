package nl.nn.adapterframework.pipes;

import java.io.*;
import java.net.URL;
import java.util.*;

import javax.wsdl.*;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.schema.SchemaImport;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.ibm.wsdl.extensions.schema.SchemaSerializer;

import nl.nn.adapterframework.core.*;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * Wsdl based input validator. Given an WSDL, it validates input.
 * @author Michiel Meeuwissen
 */
public class WsdlXmlValidator extends FixedForwardPipe {

    private static final Logger LOG = LogManager.getLogger(WsdlXmlValidator.class);

    private static final QName SCHEMA       = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI,"schema","");
    private static final Set<QName> SCHEMAS = new HashSet(Arrays.asList(
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


    public void setWsdl(String uri) throws IOException, WSDLException {
        def = getDefinition(ClassUtils.getResourceURL(uri));
    }

    @Override
    public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
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
        Schema inputSchema = getInputSchema();
        if (inputSchema == null) throw new IllegalStateException("No input schema found on " + def);
        validate(new StringReader(input), inputSchema);

    }

    protected Definition getDefinition(URL url) throws WSDLException, IOException {
        InputSource source = new InputSource(url.openStream());
        source.setSystemId(url.toString());
        WSDLReader reader  = FACTORY.newWSDLReader();
        reader.setFeature("javax.wsdl.verbose",         true);
        reader.setFeature("javax.wsdl.importDocuments", true);
        return reader.readWSDL(url.toString(), source);
    }

    protected void validate(
        final Reader input,
        final Schema wsdlSchema) throws WSDLException, IOException, SAXException {

        SchemaFactory factory = SchemaFactory.newInstance(wsdlSchema.getElementType().getNamespaceURI());
        factory.setResourceResolver(new LSResourceResolver() {
            //@Override // java 6
            public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
                LSInput lsinput = DOM.createLSInput();
                List<SchemaImport> schemas = (List) wsdlSchema.getImports().get(namespaceURI);
                if (schemas == null) {
                    throw new IllegalStateException("No schemas found for " + namespaceURI + " in " + wsdlSchema.getImports());
                }
                SchemaImport schemaImport = schemas.get(0);
                String nameSpace = schemaImport.getNamespaceURI();
                schemaImport = getSchema(nameSpace);
                if (schemaImport == null || schemaImport.getReferencedSchema() == null) {
                    throw new IllegalStateException("No schema referenced by " + nameSpace);
                }

                try {
                    lsinput.setCharacterStream(toReader(schemaImport.getReferencedSchema()));
                } catch (WSDLException e) {
                    LOG.error(e.getMessage(), e);
                }
                return lsinput;
            }
        });
        javax.xml.validation.Schema xsd = factory.newSchema(new StreamSource(toReader(wsdlSchema)));
        Validator validator = xsd.newValidator();
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
    protected SchemaImport getSchema(String nameSpace) {
        List types = def.getTypes().getExtensibilityElements();
        for (Iterator i = types.iterator(); i.hasNext();) {
            ExtensibilityElement type = (ExtensibilityElement) i.next();
            QName qn = type.getElementType();
            if (SCHEMA.equals(qn)) {
                Schema schema = (Schema) type;
                Map<String, List<SchemaImport>> imports = schema.getImports();
                List<SchemaImport> si= (List) imports.get(nameSpace);
                if (si != null) {
                    for (Iterator j = si.iterator(); j.hasNext();) {
                        SchemaImport s = (SchemaImport) j.next();
                        if (s.getReferencedSchema() != null) {
                            return s;
                        }
                    }
                }
            }
        }
        return null;
    }
}

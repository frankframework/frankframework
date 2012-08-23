package nl.nn.adapterframework.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.core.IPipeLineSession;

/**
 * Straightforward XML-validation based on javax.validation. This is work in programs.
 * @author Michiel Meeuwissen
 */
public class JavaxXmlValidator extends AbstractXmlValidator {

    protected static final Logger LOG = LogUtil.getLogger(JavaxXmlValidator.class);

    // TODO I think many (if not all) schemas can simply be registered globally, because xmlns should be uniquely defined.
    // missing a generic generic mechanism for now
    private static final Map<String, URL> globalRegistry = new HashMap<String, URL>();

    static {
        globalRegistry.put("http://schemas.xmlsoap.org/soap/envelope/", ClassUtils.getResourceURL("/Tibco/xsd/soap/envelope.xsd"));
        //globalRegistry.put("http://ing.nn.afd/AFDTypes",                ClassUtils.getResourceURL("/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/AFDTypes.xsd"));
    }




    private Schema schema = null;

    @Override
    public String validate(Object input/*impossible to understand*/,
                           IPipeLineSession session, String logPrefix) throws XmlValidatorException {
        InputSource source = getInputSource(input);
        SAXSource sax = new SAXSource(source);
        return validate(sax);
    }

    protected String validate(Source source) throws XmlValidatorException {
        Schema xsd;
        try {
            xsd = getSchemaObject();
        } catch (SAXException e) {
            throw new XmlValidatorException(e.getMessage());
        } catch (IOException e) {
            throw new XmlValidatorException(e.getMessage());
        } catch (XMLStreamException e) {
            throw new XmlValidatorException(e.getMessage());
        }
        try {
            Validator validator = xsd.newValidator();
            //validator.setResourceResolver(resourceResolver);
            //validator.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
            validator.validate(source);
        } catch (SAXException e) {
            throw new XmlValidatorException(e.getMessage());
        } catch (IOException e) {
            throw new XmlValidatorException(e.getMessage(), e);
        }
        return XML_VALIDATOR_VALID_MONITOR_EVENT;
    }

    @Override
    public void setSchemaLocation(String schemaLocation) {
        super.setSchemaLocation(schemaLocation);
        schema = null;
    }


    /**
     * Returns the {@link Schema} associated with this validator. This ia an XSD schema containing knowledge about the
     * schema source as returned by {@link #getSchemaSources()}
     * This schema object is cached until the next call to {@link #setSchemaLocation(String)}.
     */
    protected Schema getSchemaObject() throws SAXException, IOException, XMLStreamException {
        if (schema == null) {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Collection<Source> sources = getSchemaSources().values();
            schema = factory.newSchema(sources.toArray(new Source[sources.size()]));
        }
        return schema;
    }


    /**
     * Generates a map of all the xsd's that are to be used during validation.
     * This is a map that connects the xml-namespaces to these xsd's.
     * It contains the globablly registered entries from {@link #globalRegistry} plus the explicitely configured entries
     * returned by {@link #getSchemaLocation()}.
     */
    protected Map<String, Source> getSchemaSources() throws IOException, XMLStreamException {
        Map<String, Source> schemaSources = new HashMap<String, Source>();
        for (Map.Entry<String, URL> entry : globalRegistry.entrySet()) {
            StreamSource ss = new StreamSource(entry.getValue().openStream(), entry.getValue().toExternalForm());
            ss.setPublicId(entry.getKey());
            schemaSources.put(entry.getKey(), ss);
        }
        String sl = getSchemaLocation();
        if (sl != null) {
            String[] schema = getSchemaLocation().trim().split("\\s+");
            for (int i = 0; i < schema.length; i += 2) {
                String namespace = schema[i];
                URL url = ClassUtils.getResourceURL(schema[i + 1]);
                InputStream input = isAddNamespaceToSchema() ? XsdUtils.targetNameSpaceAdding(url.openStream(), namespace) : url.openStream();
                StreamSource ss = new StreamSource(input, url.toExternalForm());
                ss.setPublicId(namespace);
                schemaSources.put(schema[i], ss);
            }
        }
        return schemaSources;
    }


}

package nl.nn.adapterframework.soap;

import java.util.*;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.pipes.XmlValidator;

/**
 * XmlValidator that will automatically add the SOAP envelope XSD.
 * @author Michiel Meeuwissen
 * @author Jaco de Groot
 */
public class SoapValidator extends XmlValidator {

    private static final Logger LOG = LogManager.getLogger(SoapValidator.class);

    private String soapBody   = "";
    private String soapHeader = "";

    private SoapVersion[] versions = new SoapVersion[] {SoapVersion.fromAttribute("1.1")};

    private String setSchemaLocation = "";


    @Override
    public void configure() throws ConfigurationException {
        super.configure();
        this.setSchemaLocation(setSchemaLocation);

    }

    @Override
    public void setSchemaLocation(String schemaLocation) {
        super.setSchemaLocation(StringUtils.join(versions, " ") + (schemaLocation.length() > 0 ? " "  : "") + schemaLocation);
        setSchemaLocation = schemaLocation;
    }


    @Override
    public String getRoot() {
        return "Envelope";
    }
    @Override
    public void setRoot(String r) {
        throw new IllegalArgumentException("The root element of a soap envelope is always " + getRoot());
    }

    public void setVersion(String s) {
        if ("any".equals(s) || StringUtils.isBlank(s)) {
            this.versions = SoapVersion.values();
        } else {
            this.versions = new SoapVersion[] {SoapVersion.fromAttribute(s)};
        }
        setSchemaLocation(setSchemaLocation);
    }

    public Collection<QName> getSoapBodyTags() {
        return Collections.unmodifiableCollection(parseQNameList(soapBody));
    }

    public void setSoapBody(String soapBody) {
        this.soapBody = soapBody;
    }
    public Collection<QName> getSoapHeaderTags() {
        return Collections.unmodifiableCollection(parseQNameList(soapHeader));
    }


    public void setSoapHeader(String soapHeader) {
        this.soapHeader = soapHeader;
    }

    protected int getDefaultNamespaceIndex() {
        return versions.length;
    }

    protected String getDefaultNamespace() {
        if (StringUtils.isNotBlank(getSchemaLocation())) {
            String[] schemas = getSchemaLocation().split("\\s+");
            if (schemas.length > getDefaultNamespaceIndex() * 2) {
                return schemas[getDefaultNamespaceIndex() * 2];
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    protected Collection<QName> parseQNameList(String s) {

        List<QName> result = new ArrayList<QName>();
        if (StringUtils.isNotBlank(s)) {
            for(String qname : s.split("\\s+")) {
                if (! qname.startsWith("{")) {
                    String xmlns = getDefaultNamespace();
                    if (xmlns != null) {
                        LOG.info("no namespace found for " + qname  + " taking default " + xmlns);
                        qname = '{' + xmlns + '}' + qname;
                    }
                }
                result.add(QName.valueOf(qname));
            }
        }
        return result;
    }


    public static enum SoapVersion {

        VERSION_1_1("http://schemas.xmlsoap.org/soap/envelope/", "/xml/xsd/soap/envelope.xsd"),
        VERSION_1_2("http://www.w3.org/2003/05/soap-envelope",   "/xml/xsd/soap/envelope-1.2.xsd");

        public final String schema;
        public final String xsd;

        SoapVersion(String schema, String s) {
            this.schema = schema;
            this.xsd    = s;
        }

        public static SoapVersion fromAttribute(String s) {
            if (StringUtils.isBlank(s)) return VERSION_1_1;
            return valueOf("VERSION_" + s.replaceAll("\\.", "_"));
        }
        @Override
        public String toString() {
            return schema + " " + xsd;
        }

    }

}

/*
 * $Log: SoapValidator.java,v $
 * Revision 1.8  2012-09-26 12:41:05  m00f069
 * Bugfix in WSDL generator: Wrong prefix being used in element attribute of PipeLineInput and PipeLineOutput message part when using EsbSoapValidator.
 *
 * Revision 1.7  2012/09/19 09:49:58  Jaco de Groot <jaco.de.groot@ibissource.org>
 * - Set reasonSessionKey to "failureReason" and xmlReasonSessionKey to "xmlFailureReason" by default
 * - Fixed check on unknown namspace in case root attribute or xmlReasonSessionKey is set
 * - Fill reasonSessionKey with a message when an exception is thrown by parser instead of the ErrorHandler being called
 * - Added/fixed check on element of soapBody and soapHeader
 * - Cleaned XML validation code a little (e.g. moved internal XmlErrorHandler class (double code in two classes) to an external class, removed MODE variable and related code)
 *
 * Revision 1.6  2012/09/14 13:35:48  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added CVS log
 *
 */
package nl.nn.adapterframework.soap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.pipes.XmlValidator;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

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
        if (StringUtils.isNotEmpty(soapBody)) {
            List<String> path = new ArrayList<String>();
            path.add("Envelope");
            path.add("Body");
            path.add(soapBody);
            validator.addSingleLeafValidation(path);
        }
        if (StringUtils.isNotEmpty(soapHeader)) {
            List<String> path = new ArrayList<String>();
            path.add("Envelope");
            path.add("Header");
            path.add(soapHeader);
            validator.addSingleLeafValidation(path);
        }
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

    public void setSoapBody(String soapBody) {
        this.soapBody = soapBody;
    }

    public String getSoapBody() {
        return soapBody;
    }

    public void setSoapHeader(String soapHeader) {
        this.soapHeader = soapHeader;
    }

    public String getSoapHeader() {
        return soapHeader;
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

package nl.nn.adapterframework.soap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import nl.nn.adapterframework.pipes.XmlValidator;

/**
 * @author Michiel Meeuwissen
 * @TODO implement actual validation. For now this is only used for WSDL generation
 */
public class SoapValidator extends XmlValidator {

    private static final Logger LOG = LogManager.getLogger(SoapValidator.class);

    public static final String SOAP_ENVELOPE     = "http://schemas.xmlsoap.org/soap/envelope/";
    public static final String SOAP_ENVELOPE_XSD = "/xml/xsd/soap/envelope.xsd";

    private String soapBody   = "";
    private String soapHeader = "";

    @Override
    public String getSchemaLocation() {
        return SOAP_ENVELOPE + " " + SOAP_ENVELOPE_XSD + " " + super.getSchemaLocation();
    }
    @Override
    public String getRoot() {
        return "envelope";
    }
    @Override
    public void setRoot(String r) {
        throw new IllegalArgumentException("The root element of a soap envelope is always " + getRoot());
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

    protected String getDefaultNamespace() {
        if (StringUtils.isNotBlank(super.getSchemaLocation())) {
            return super.getSchemaLocation().split("\\s+")[0];
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


}

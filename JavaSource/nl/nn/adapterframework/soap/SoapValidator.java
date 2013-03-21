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
/*
 * $Log: SoapValidator.java,v $
 * Revision 1.13  2012-10-19 11:54:07  m00f069
 * Bugfix double occurrence of CommonMessageHeader.xsd in schemaLocation
 *
 * Revision 1.12  2012/10/19 09:33:47  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Made WsdlXmlValidator extent Xml/SoapValidator to make it use the same validation logic, cleaning XercesXmlValidator on the way
 *
 * Revision 1.11  2012/10/12 16:17:17  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Made (Esb)SoapValidator set SoapNamespace to an empty value, hence validate the SOAP envelope against the SOAP XSD.
 * Made (Esb)SoapValidator check for SOAP Envelope element
 *
 * Revision 1.10  2012/10/01 07:59:29  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Improved messages stored in reasonSessionKey and xmlReasonSessionKey
 * Cleaned XML validation code and documentation a bit.
 *
 * Revision 1.9  2012/09/28 14:40:15  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Bugfix WSDL target namespace for one-way ESB Soap (when getting it from the namespace of the first XSD)
 *
 * Revision 1.8  2012/09/26 12:41:05  Jaco de Groot <jaco.de.groot@ibissource.org>
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
import java.util.List;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.pipes.XmlValidator;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * XmlValidator that will automatically add the SOAP envelope XSD.
 * @author Michiel Meeuwissen
 * @author Jaco de Groot
 */
public class SoapValidator extends XmlValidator {

    private static final Logger LOG = LogUtil.getLogger(SoapValidator.class);

    private String soapBody   = "";
    private String soapHeader = "";

    private SoapVersion[] versions = new SoapVersion[] {SoapVersion.fromAttribute("1.1")};

    private String setSchemaLocation = "";


    @Override
    public void configure() throws ConfigurationException {
        setSoapNamespace("");
        super.setRoot(getRoot());
        super.configure();
        if (StringUtils.isNotEmpty(soapBody)) {
            List<String> path = new ArrayList<String>();
            path.add("Envelope");
            path.add("Body");
            path.add(soapBody);
            validator.addRootValidation(path);
        }
        if (StringUtils.isNotEmpty(soapHeader)) {
            List<String> path = new ArrayList<String>();
            path.add("Envelope");
            path.add("Header");
            path.add(soapHeader);
            validator.addRootValidation(path);
        }
    }

    @Override
    public void setSchemaLocation(String schemaLocation) {
        super.setSchemaLocation(schemaLocation + (schemaLocation.length() > 0 ? " "  : "") + StringUtils.join(versions, " "));
        setSchemaLocation = schemaLocation;
    }

    @Override
    public void setSchema(String schema) {
        throw new IllegalArgumentException("The schema attribute isn't supported");
    }

    @Override
    public void setNoNamespaceSchemaLocation(String noNamespaceSchemaLocation) {
        throw new IllegalArgumentException("The noNamespaceSchemaLocation attribute isn't supported");
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

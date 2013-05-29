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

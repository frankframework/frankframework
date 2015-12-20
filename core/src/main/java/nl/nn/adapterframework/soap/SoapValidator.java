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
import java.util.Arrays;
import java.util.List;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.pipes.XmlValidator;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * XmlValidator that will automatically add the SOAP envelope XSD to the set of
 * XSD's used for validation.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>*</td><td>all attributes available on {@link XmlValidator} can be used except the root attribute</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRoot(String) root}</td><td>always Envelope (not allowed to change)</td><td>Envelope</td></tr>
 * <tr><td>{@link #setSoapBody(String) soapBody}</td><td>name of the child element of the SOAP body</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSoapHeader(String) soapHeader}</td><td>name of the child element of the SOAP header</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSoapVersion(String) soapVersion}</td><td>SOAP envelope XSD version to use: 1.1, 1.2 or any (both 1.1 and 1.2)</td><td>1.1</td></tr>
 * </table>
 *
 * @author Michiel Meeuwissen
 * @author Jaco de Groot
 */
public class SoapValidator extends XmlValidator {

    private static final Logger LOG = LogUtil.getLogger(SoapValidator.class);

    private String soapBody    = "";
    private String soapHeader  = "";
    private String soapVersion = "1.1";

    private SoapVersion[] versions = new SoapVersion[] {SoapVersion.fromAttribute("1.1")};

    protected boolean addSoapEnvelopeToSchemaLocation = true;


    @Override
    public void configure() throws ConfigurationException {
        setSoapNamespace("");
        super.setRoot(getRoot());
        if ("any".equals(soapVersion) || StringUtils.isBlank(soapVersion)) {
            versions = SoapVersion.values();
        } else {
            versions = new SoapVersion[] {SoapVersion.fromAttribute(soapVersion)};
        }
        if (addSoapEnvelopeToSchemaLocation) {
            super.setSchemaLocation(schemaLocation + (schemaLocation.length() > 0 ? " "  : "") + StringUtils.join(versions, " "));
        }
        if (StringUtils.isEmpty(soapBody)) {
            ConfigurationWarnings configWarnings = ConfigurationWarnings
                    .getInstance();
            configWarnings.add(log, "soapBody not specified");
        }
        validator.addRootValidation(Arrays.asList("Envelope", "Body", soapBody));
        validator.addRootValidation(Arrays.asList("Envelope", "Header", soapHeader));
        List<String> invalidRootNamespaces = new ArrayList<String>();
        for (SoapVersion version : versions) {
            invalidRootNamespaces.add(version.getNamespace());
        }
        validator.addInvalidRootNamespaces(Arrays.asList("Envelope", "Body", soapBody), invalidRootNamespaces);
        validator.addInvalidRootNamespaces(Arrays.asList("Envelope", "Header", soapHeader), invalidRootNamespaces);
        super.configure();
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

    public void setSoapVersion(String soapVersion) {
        this.soapVersion = soapVersion;
    }

    public String getSoapVersion() {
        return soapVersion;
    }

    public static enum SoapVersion {

        VERSION_1_1("http://schemas.xmlsoap.org/soap/envelope/", "/xml/xsd/soap/envelope.xsd"),
        VERSION_1_2("http://www.w3.org/2003/05/soap-envelope",   "/xml/xsd/soap/envelope-1.2.xsd");

        public final String namespace;
        public final String location;

        SoapVersion(String namespace, String location) {
            this.namespace = namespace;
            this.location = location;
        }

        public static SoapVersion fromAttribute(String s) {
            if (StringUtils.isBlank(s)) return VERSION_1_1;
            return valueOf("VERSION_" + s.replaceAll("\\.", "_"));
        }

        public String getNamespace() {
            return namespace;
        }

        @Override
        public String toString() {
            return namespace + " " + location;
        }

    }

}

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

import nl.nn.adapterframework.doc.IbisDoc;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.pipes.Json2XmlValidator;
import nl.nn.adapterframework.pipes.XmlValidator;
import nl.nn.adapterframework.util.LogUtil;

/**
 * XmlValidator that will automatically add the SOAP envelope XSD to the set of
 * XSD's used for validation.
 *
 * <b><A name="note1">Note 1:</A></b>
 * Before the <code>outputSoapBody</code> attribute was introduced, two validators were used for a request-reply pattern (an inputValidator for the request and an outputValidator for the reply).
 * These inputValidator and outputValidator were identical except for the child element of the SOAP body. Because validators use relatively a lot of memory, the <code>outputSoapBody</code> attribute was added which replaces the outputValidator.
 * Both the request and the reply are then validated by the inputValidator.
 *
 * @author Michiel Meeuwissen
 * @author Jaco de Groot
 */
public class SoapValidator extends Json2XmlValidator {

    private static final Logger LOG = LogUtil.getLogger(SoapValidator.class);

    private String soapBody    = "";
    private String outputSoapBody    = "";
    private String soapHeader  = "";
    private String soapHeaderNamespace  = "";
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
        addRequestRootValidation(Arrays.asList("Envelope", "Body", soapBody));
        if (StringUtils.isNotEmpty(outputSoapBody)) {
            addResponseRootValidation(Arrays.asList("Envelope", "Body", outputSoapBody));
        }
        addRequestRootValidation(Arrays.asList("Envelope", "Header", soapHeader));
        List<String> invalidRootNamespaces = new ArrayList<String>();
        for (SoapVersion version : versions) {
            invalidRootNamespaces.add(version.getNamespace());
        }
        addInvalidRootNamespaces(Arrays.asList("Envelope", "Body", soapBody), invalidRootNamespaces);
        addInvalidRootNamespaces(Arrays.asList("Envelope", "Header", soapHeader), invalidRootNamespaces);
        super.configure();
    }

    @Override
	protected boolean isConfiguredForMixedValidation() {
		return StringUtils.isNotEmpty(getOutputSoapBody());
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
	public String getMessageRoot() {
		return getSoapBody();
	}
	@Override
	public String getResponseRoot() {
		return getOutputSoapBody();
	}


    @Override
    public String getRoot() {
        return "Envelope";
    }

	@IbisDoc({"always envelope (not allowed to change)", "envelope"})
    @Override
    public void setRoot(String r) {
        throw new IllegalArgumentException("The root element of a soap envelope is always " + getRoot());
    }

	@IbisDoc({"name of the child element of the soap body. or a comma separated list of names to choose from (only one is allowed) (wsdl generator will use the first element) (use empty value to allow an empty soap body, for example to allow element x and an empty soap body use: x,)", ""})
    public void setSoapBody(String soapBody) {
        this.soapBody = soapBody;
    }

    public String getSoapBody() {
        return soapBody;
    }

	@IbisDoc({"identical to the <code>soapbody</code> attribute except that it's used for the output message instead of the input message. for more information see <a href=\"#note1\">note 1</a>", ""})
    public void setOutputSoapBody(String outputSoapBody) {
        this.outputSoapBody = outputSoapBody;
    }

    public String getOutputSoapBody() {
        return outputSoapBody;
    }

	@IbisDoc({"name of the child element of the soap header. or a comma separated list of names to choose from (only one is allowed) (wsdl generator will use the first element) (use empty value to allow an empty soap header, for example to allow element x and an empty soap header use: x,)", ""})
    public void setSoapHeader(String soapHeader) {
        this.soapHeader = soapHeader;
    }

    public String getSoapHeader() {
        return soapHeader;
    }

	@IbisDoc({"can be used when the soap header element exists multiple times", ""})
    public void setSoapHeaderNamespace(String soapHeaderNamespace) {
        this.soapHeaderNamespace = soapHeaderNamespace;
    }

    public String getSoapHeaderNamespace() {
        return soapHeaderNamespace;
    }

	@IbisDoc({"soap envelope xsd version to use: 1.1, 1.2 or any (both 1.1 and 1.2)", "1.1"})
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

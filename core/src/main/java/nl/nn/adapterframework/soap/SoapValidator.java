/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.pipes.Json2XmlValidator;

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

	private String soapBody = "";
	private String outputSoapBody = "";
	private String soapHeader = "";
	private String soapHeaderNamespace = "";
	private SoapVersion soapVersion = SoapVersion.SOAP11;
	private boolean allowPlainXml = false;
	public static final String SOAP_ENVELOPE = "Envelope";
	public static final String SOAP_BODY = "Body";
	public static final String SOAP_HEADER = "Header";

	protected boolean addSoapEnvelopeToSchemaLocation = true;

	@Override
	public void configure() throws ConfigurationException {
		setSoapNamespace("");
		if (isAllowPlainXml()) {
			//super.setRoot("Envelope,"+soapBody);
			addRequestRootValidation(Arrays.asList(SOAP_ENVELOPE+","+soapBody));
		} else {
			super.setRoot(getRoot());
		}
		if (addSoapEnvelopeToSchemaLocation) {
			super.setSchemaLocation(getSchemaLocation() + (getSchemaLocation().length() > 0 ? " " : "") + soapVersion.getSchemaLocation());
		}
		if (StringUtils.isEmpty(soapBody)) {
			ConfigurationWarnings.add(this, log, "soapBody not specified");
		}
		if (!isAllowPlainXml()) {
			addRequestRootValidation(Arrays.asList(SOAP_ENVELOPE, SOAP_BODY, soapBody));
			if (StringUtils.isNotEmpty(outputSoapBody)) {
				addResponseRootValidation(Arrays.asList(SOAP_ENVELOPE, SOAP_BODY, outputSoapBody));
			}
			addRequestRootValidation(Arrays.asList(SOAP_ENVELOPE, SOAP_HEADER, soapHeader));
			List<String> invalidRootNamespaces = new ArrayList<String>();
			for (String namespace:soapVersion.getNamespaces()) {
				invalidRootNamespaces.add(namespace);
			}
			addInvalidRootNamespaces(Arrays.asList(SOAP_ENVELOPE, SOAP_BODY, soapBody), invalidRootNamespaces);
			addInvalidRootNamespaces(Arrays.asList(SOAP_ENVELOPE, SOAP_HEADER, soapHeader), invalidRootNamespaces);
		}
		super.configure();
	}

	@Override
	protected Set<List<String>> createRootValidation(String messageRoot) {
		Set<List<String>> messageRootValidations = new LinkedHashSet<List<String>>();
		if (isAllowPlainXml()) {
			messageRootValidations.add(Arrays.asList(SOAP_ENVELOPE+","+messageRoot)); // cannot test for messageRoot in SOAP message with current rootvalidation structure
		} else {
			messageRootValidations.add(Arrays.asList(SOAP_ENVELOPE, SOAP_BODY, messageRoot));
		}
		return messageRootValidations;
	}

	@Override
	protected boolean isConfiguredForMixedValidation() {
		return StringUtils.isNotEmpty(getOutputSoapBody());
	}

	@Deprecated
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
		return SOAP_ENVELOPE;
	}

	@IbisDoc({ "always envelope (not allowed to change)", "envelope" })
	@Override
	public void setRoot(String r) {
		throw new IllegalArgumentException("The root element of a soap envelope is always " + getRoot());
	}

	@IbisDoc({"1", "name of the child element of the SOAP body, or a comma separated list of names to choose from (only one is allowed) (wsdl generator will use the first element) (use empty value to allow an empty soap body, for example to allow element x and an empty soap body use: x,)", "" })
	public void setSoapBody(String soapBody) {
		this.soapBody = soapBody;
	}
	public String getSoapBody() {
		return soapBody;
	}

	@IbisDoc({"2", "identical to the <code>soapBody</code> attribute except that it's used for the output message instead of the input message. For more information see <a href=\"#note1\">note 1</a>", "" })
	public void setOutputSoapBody(String outputSoapBody) {
		this.outputSoapBody = outputSoapBody;
	}
	public String getOutputSoapBody() {
		return outputSoapBody;
	}

	@IbisDoc({"3", "name of the child element of the SOAP header, or a comma separated list of names to choose from (only one is allowed) (wsdl generator will use the first element) (use empty value to allow an empty soap header, for example to allow element x and an empty soap header use: x,)", "" })
	public void setSoapHeader(String soapHeader) {
		this.soapHeader = soapHeader;
	}
	public String getSoapHeader() {
		return soapHeader;
	}

	@IbisDoc({"4", "can be used when the SOAP header element exists multiple times", "" })
	public void setSoapHeaderNamespace(String soapHeaderNamespace) {
		this.soapHeaderNamespace = soapHeaderNamespace;
	}
	public String getSoapHeaderNamespace() {
		return soapHeaderNamespace;
	}

	@IbisDoc({"5", "SOAP envelope XSD version to use: 1.1, 1.2 or any (both 1.1 and 1.2)", "1.1" })
	public void setSoapVersion(String soapVersion) {
		this.soapVersion = SoapVersion.getSoapVersion(soapVersion);
	}
	public SoapVersion getSoapVersionEnum() {
		return soapVersion;
	}

	@IbisDoc({"6", "allow plain XML, without a SOAP Envelope, too. Be aware that setting this true inhibits the capability to test for exit specific response roots in SOAP messages", "false"})
	public void setAllowPlainXml(boolean allowPlainXml) {
		this.allowPlainXml = allowPlainXml;
	}
	public boolean isAllowPlainXml() {
		return allowPlainXml;
	}

}

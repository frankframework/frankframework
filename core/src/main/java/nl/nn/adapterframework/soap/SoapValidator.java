/*
   Copyright 2013 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.pipes.Json2XmlValidator;
import nl.nn.adapterframework.validation.RootValidation;
import nl.nn.adapterframework.validation.RootValidations;

/**
 * XmlValidator that will automatically add the SOAP envelope XSD to the set of XSD's used for validation.
 *
 * <b><A name="note1">Note 1:</A></b>
 * Before the <code>outputSoapBody</code> attribute was introduced, two validators were used for a request-reply pattern (an inputValidator for the request and an outputValidator for the reply).
 * These inputValidator and outputValidator were identical except for the child element of the SOAP body. Because validators use relatively a lot of memory, the <code>outputSoapBody</code> attribute was added which replaces the outputValidator.
 * Both the request and the reply are then validated by the inputValidator.
 * <p>To generate a wsdl with a soap action included one of the following properties must be set to the expected soapAction</p>
 * <table border="1">
 * <tr><td>wsdl.${adapterName}.${listenerName}.soapAction</td></tr>
 * <tr><td>wsdl.${adapterName}.soapAction</td></tr>
 * <tr><td>wsdl.soapAction</td></tr>
 * </table>
 *
 * @author Michiel Meeuwissen
 * @author Jaco de Groot
 */
public class SoapValidator extends Json2XmlValidator {

	private @Getter String soapBody = "";
	private @Getter String outputSoapBody = "";
	private @Getter String soapHeader = "";
	private @Getter String soapHeaderNamespace = "";
	private @Getter SoapVersion soapVersion = SoapVersion.SOAP11;
	private @Getter boolean allowPlainXml = false;
	public static final String SOAP_ENVELOPE = "Envelope";
	public static final String SOAP_BODY = "Body";
	public static final String SOAP_HEADER = "Header";

	protected boolean addSoapEnvelopeToSchemaLocation = true;

	@Override
	public void configure() throws ConfigurationException {
		setSoapNamespace("");
		if (isAllowPlainXml()) {
			//super.setRoot("Envelope,"+soapBody);
			addRequestRootValidation(new RootValidation(SOAP_ENVELOPE+","+soapBody));
		} else {
			super.setRoot(getRoot());
		}
		if (addSoapEnvelopeToSchemaLocation) {
			if (StringUtils.isEmpty(getSchemaLocation())) {
				throw new ConfigurationException("schemaLocation must be specified");
			}
			super.setSchemaLocation(getSchemaLocation() + (getSchemaLocation().length() > 0 ? " " : "") + soapVersion.getSchemaLocation());
		}
		if (StringUtils.isEmpty(soapBody)) {
			ConfigurationWarnings.add(this, log, "soapBody not specified");
		}
		if (!isAllowPlainXml()) {
			addRequestRootValidation(new RootValidation(SOAP_ENVELOPE, SOAP_BODY, soapBody));
			if (StringUtils.isNotEmpty(outputSoapBody)) {
				addResponseRootValidation(new RootValidation(SOAP_ENVELOPE, SOAP_BODY, outputSoapBody));
			}
			addRequestRootValidation(new RootValidation(SOAP_ENVELOPE, SOAP_HEADER, soapHeader));
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
	protected RootValidations createRootValidation(String messageRoot) {
		if (isAllowPlainXml()) {
			return new RootValidations(SOAP_ENVELOPE+","+messageRoot); // cannot test for messageRoot in SOAP message with current rootvalidation structure
		}
		return new RootValidations(SOAP_ENVELOPE, SOAP_BODY, messageRoot);
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
	@Deprecated
	public void setRoot(String r) {
		throw new IllegalArgumentException("The root element of a soap envelope is always " + getRoot());
	}

	@IbisDoc({"Name of the child element of the SOAP body, or a comma separated list of names to choose from (only one is allowed) (wsdl generator will use the first element) (use empty value to allow an empty soap body, for example to allow element x and an empty soap body use: x,)", "" })
	public void setSoapBody(String soapBody) {
		this.soapBody = soapBody;
	}

	@IbisDoc({"Identical to the <code>soapBody</code> attribute except that it's used for the output message instead of the input message. For more information see <a href=\"#note1\">note 1</a>", "" })
	public void setOutputSoapBody(String outputSoapBody) {
		this.outputSoapBody = outputSoapBody;
	}

	@IbisDoc({"Name of the child element of the SOAP header, or a comma separated list of names to choose from (only one is allowed) (wsdl generator will use the first element) (use empty value to allow an empty soap header, for example to allow element x and an empty soap header use: x,)", "" })
	public void setSoapHeader(String soapHeader) {
		this.soapHeader = soapHeader;
	}

	@IbisDoc({"Can be used when the SOAP header element exists multiple times", "" })
	public void setSoapHeaderNamespace(String soapHeaderNamespace) {
		this.soapHeaderNamespace = soapHeaderNamespace;
	}

	@IbisDoc({"SOAP envelope XSD version to use", "1.1" })
	public void setSoapVersion(SoapVersion soapVersion) {
		this.soapVersion = soapVersion;
	}

	@IbisDoc({"Allow plain XML, without a SOAP Envelope, too. Be aware that setting this true inhibits the capability to test for exit specific response roots in SOAP messages", "false"})
	public void setAllowPlainXml(boolean allowPlainXml) {
		this.allowPlainXml = allowPlainXml;
	}
}

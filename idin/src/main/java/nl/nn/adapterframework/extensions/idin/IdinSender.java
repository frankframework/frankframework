/*
   Copyright 2018 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.idin;

import static nl.nn.adapterframework.util.DateFormatUtils.FULL_GENERIC_FORMATTER;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

import net.bankid.merchant.library.AssuranceLevel;
import net.bankid.merchant.library.AuthenticationRequest;
import net.bankid.merchant.library.AuthenticationResponse;
import net.bankid.merchant.library.Communicator;
import net.bankid.merchant.library.Configuration;
import net.bankid.merchant.library.DirectoryResponse;
import net.bankid.merchant.library.ErrorResponse;
import net.bankid.merchant.library.SamlResponse;
import net.bankid.merchant.library.ServiceId;
import net.bankid.merchant.library.StatusRequest;
import net.bankid.merchant.library.StatusResponse;
import net.bankid.merchant.library.internal.DirectoryResponseBase.Issuer;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.senders.SenderWithParametersBase;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.DateFormatUtils;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Requires the net.bankid.merchant.library V1.06+.
 * Compile with Java 1.7+
 *
 * @author Niels Meijer
 */
public class IdinSender extends SenderWithParametersBase implements HasPhysicalDestination {
	private final String domain = "Idin";
	private String merchantID = null;
	private int merchantSubID = 0;
	private String merchantReturnUrl = null;

	private String acquirerDirectoryUrl = null;
	private String acquirerTransactionUrl = null;
	private String acquirerStatusUrl = null;

	private String keyStoreLocation = null;
	private CredentialFactory keyStoreCredentials = null;

	private String merchantCertificateAlias = null;
	private CredentialFactory merchantCertificateCredentials = null;
	private String acquirerCertificateAlias = null;
	private String acquirerAlternativeCertificateAlias = null;
	private String SAMLCertificateAlias = null;
	private CredentialFactory SAMLCertificateCredentials = null;

	private boolean logsEnabled = false;
	private boolean serviceLogsEnabled = false;
	private String serviceLogsLocation = null;
	private String serviceLogsPattern = "%Y-%M-%D\\%h%m%s.%f-%a.xml";

	private String action = "DIRECTORY";
	private final List<String> actions = Arrays.asList("DIRECTORY", "RESPONSE", "AUTHENTICATE");

	Configuration idinConfig = null;
	Communicator communicator = null;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if(StringUtils.isNotEmpty(getAction()) && !actions.contains(getAction()))
			throw new ConfigurationException(getLogPrefix()+"unknown action ["+getAction()+"] supported methods are "+actions.toString()+"");

		//Create a new instance and populate it
		idinConfig = Configuration.defaultInstance();

		if(StringUtils.isNotEmpty(getMerchantID()))
			idinConfig.setMerchantID(getMerchantID());
		if(getMerchantSubID() > 0)
			idinConfig.setMerchantSubID(getMerchantSubID());
		if(StringUtils.isNotEmpty(getMerchantReturnUrl()))
			idinConfig.setMerchantReturnUrl(getMerchantReturnUrl());

		if(StringUtils.isNotEmpty(getAcquirerDirectoryUrl()))
			idinConfig.setAcquirerDirectoryURL(getAcquirerDirectoryUrl());
		if(StringUtils.isNotEmpty(getAcquirerTransactionUrl()))
			idinConfig.setAcquirerTransactionURL(getAcquirerTransactionUrl());
		if(StringUtils.isNotEmpty(getAcquirerStatusUrl()))
			idinConfig.setAcquirerStatusURL(getAcquirerStatusUrl());

		if(StringUtils.isNotEmpty(getKeyStoreLocation())) {
			idinConfig.setKeyStoreLocation(getKeyStoreLocation());
			if(StringUtils.isNotEmpty(getKeyStorePassword()))
				idinConfig.setKeyStorePassword(getKeyStorePassword());
		}

		if(StringUtils.isNotEmpty(getMerchantCertificateAlias())) {
			idinConfig.setMerchantCertificateAlias(getMerchantCertificateAlias());
			if(StringUtils.isNotEmpty(getMerchantCertificatePassword()))
				idinConfig.setMerchantCertificatePassword(getMerchantCertificatePassword());
		}

		if(StringUtils.isNotEmpty(getAcquirerCertificateAlias()))
			idinConfig.setAcquirerCertificateAlias(getAcquirerCertificateAlias());
		if(StringUtils.isNotEmpty(getAcquirerAlternativeCertificateAlias()))
			idinConfig.setAcquirerAlternateCertificateAlias(getAcquirerAlternativeCertificateAlias());

		if(StringUtils.isNotEmpty(getSAMLCertificateAlias())) {
			idinConfig.setSamlCertificateAlias(getSAMLCertificateAlias());
			if(StringUtils.isNotEmpty(getSAMLCertificatePassword()))
				idinConfig.setSamlCertificatePassword(getSAMLCertificatePassword());
		}

		if(getLogsEnabled())
			idinConfig.setLogsEnabled(getLogsEnabled());
		if(getServiceLogsEnabled())
			idinConfig.setServiceLogsEnabled(getServiceLogsEnabled());
		if(StringUtils.isNotEmpty(getServiceLogsLocation()))
			idinConfig.setServiceLogsLocation(getServiceLogsLocation());
		if(StringUtils.isNotEmpty(getServiceLogsPattern()))
			idinConfig.setServiceLogsPattern(getServiceLogsPattern());

		idinConfig.setTls12Enabled(true);

		communicator = new Communicator(idinConfig);
	}

	public Communicator getCommunicator() {
		return communicator;
	}

	@Override
	public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {

		Element queryElement = null;
		try {
			if (XmlUtils.isWellFormed(message.asString(), "idin")) {
				queryElement = XmlUtils.buildElement(message.asString());
			} else {
				queryElement = XmlUtils.buildElement("<idin/>");
			}
		} catch (DomBuilderException | IOException e) {
			throw new SenderException(e);
		}

		XmlBuilder result = new XmlBuilder("result");
		ErrorResponse error = null;

		if(getAction().equals("DIRECTORY")) {
			DirectoryResponse response = getCommunicator().getDirectory();

			if(response.getIsError()) {
				error = response.getErrorResponse();
			}
			else {
				XmlBuilder issuers = new XmlBuilder("issuers");

				if(XmlUtils.getChildTagAsBoolean(queryElement, "issuersByCountry")) {
					for (Entry<String, List<Issuer>> entry : response.getIssuersByCountry().entrySet()) {
						XmlBuilder countryXml = new XmlBuilder("country");
						String country = entry.getKey();
						countryXml.addAttribute("name", country);

						for(Issuer issuer : entry.getValue()) {
							XmlBuilder issuerXml = new XmlBuilder("issuer");
							issuerXml.setValue(issuer.getIssuerName());
							issuerXml.addAttribute("id", issuer.getIssuerID());
							countryXml.addSubElement(issuerXml);
						}
						issuers.addSubElement(countryXml);
					}
				}
				else {
					for(Issuer issuer : response.getIssuers()) {
						XmlBuilder issuerXml = new XmlBuilder("issuer");
						issuerXml.setValue(issuer.getIssuerName());
						issuerXml.addAttribute("id", issuer.getIssuerID());
						issuerXml.addAttribute("country", issuer.getIssuerCountry());
						issuers.addSubElement(issuerXml);
					}
				}
				result.addSubElement(issuers);

				XmlBuilder timestamp = new XmlBuilder("timestamp");
				Date txDate = response.getDirectoryDateTimestamp().toGregorianCalendar().getTime();
				timestamp.setValue(DateFormatUtils.format(txDate, FULL_GENERIC_FORMATTER), false);
				result.addSubElement(timestamp);
			}

			if(StringUtils.isNotEmpty(response.getRawMessage())) {
				log.debug(response.getRawMessage());
			}
		}
		else if(getAction().equals("RESPONSE")) {
			String transactionID = XmlUtils.getChildTagAsString(queryElement, "transactionID");
			if(StringUtils.isEmpty(transactionID))
				throw new SenderException("no transactionID was supplied");

			StatusRequest statusRequest = new StatusRequest();
			statusRequest.setTransactionID(transactionID);
			StatusResponse response = getCommunicator().getResponse(statusRequest);

			if(response.getIsError()) {
				error = response.getErrorResponse();
			}
			else {
				XmlBuilder status = new XmlBuilder("status");
				status.setValue(response.getStatus(), false);
				result.addSubElement(status);

				if(response.getStatus() == StatusResponse.Success) {
					SamlResponse saml = response.getSamlResponse();
					XmlBuilder samlXml = new XmlBuilder("saml");

					XmlBuilder acquirerId = new XmlBuilder("acquirerId");
					acquirerId.setValue(saml.getAcquirerID());
					samlXml.addSubElement(acquirerId);

					XmlBuilder attributes = new XmlBuilder("attributes");
					for (Entry<String, String> entry : saml.getAttributes().entrySet()) {
						XmlBuilder attribute = new XmlBuilder("attribute");
						attribute.addAttribute("name", entry.getKey());
						attribute.setValue(entry.getValue());
						attributes.addSubElement(attribute);
					}
					samlXml.addSubElement(attributes);

					XmlBuilder merchantReference = new XmlBuilder("merchantReference");
					merchantReference.setValue(saml.getMerchantReference());
					samlXml.addSubElement(merchantReference);

					XmlBuilder version = new XmlBuilder("version");
					version.setValue(saml.getAcquirerID());
					samlXml.addSubElement(version);

					result.addSubElement(samlXml);
				}

				XmlBuilder transactionIdXml = new XmlBuilder("transactionID");
				transactionIdXml.setValue(response.getTransactionID(), false);
				result.addSubElement(transactionIdXml);

				XmlBuilder timestamp = new XmlBuilder("timestamp");
				Date txDate = response.getStatusDateTimestamp().toGregorianCalendar().getTime();
				timestamp.setValue(DateFormatUtils.format(txDate, FULL_GENERIC_FORMATTER), false);
				result.addSubElement(timestamp);
			}

			if(StringUtils.isNotEmpty(response.getRawMessage())) {
				log.debug(response.getRawMessage());
			}
		}
		else if(getAction().equals("AUTHENTICATE")) {
			AuthenticationRequest authRequest = new AuthenticationRequest();

			String issuerId = XmlUtils.getChildTagAsString(queryElement, "issuerId");
			if(StringUtils.isEmpty(issuerId))
				throw new SenderException("no issuerId was supplied");
			authRequest.setIssuerID(issuerId);

			String language = XmlUtils.getChildTagAsString(queryElement, "language");
			if(StringUtils.isNotEmpty(language))
				authRequest.setLanguage(language);

			String expirationPeriod = XmlUtils.getChildTagAsString(queryElement, "expirationPeriod");
			if(StringUtils.isNotEmpty(expirationPeriod)) {
				try {
					Duration duration = DatatypeFactory.newInstance().newDuration(expirationPeriod);
					authRequest.setExpirationPeriod(duration);
				} catch (DatatypeConfigurationException e) {
					throw new SenderException(e);
				}
			}

			String requestedServiceId = XmlUtils.getChildTagAsString(queryElement, "requestedServiceId");
			if(StringUtils.isNotEmpty(requestedServiceId)) {
				authRequest.setRequestedServiceID(new ServiceId(requestedServiceId));
			}

			String merchantReference = XmlUtils.getChildTagAsString(queryElement, "merchantReference");
			if(StringUtils.isNotEmpty(requestedServiceId))
				authRequest.setMerchantReference(merchantReference);

			AssuranceLevel assuranceLevel = AssuranceLevel.Loa3;
			String assurance = XmlUtils.getChildTagAsString(queryElement, "assuranceLevel");
			if(StringUtils.isNotEmpty(assurance))
				assuranceLevel = AssuranceLevel.valueOf(assurance);
			authRequest.setAssuranceLevel(assuranceLevel);


			String entranceCode = XmlUtils.getChildTagAsString(queryElement, "entranceCode");
			if(StringUtils.isNotEmpty(entranceCode))
				authRequest.setEntranceCode(entranceCode);

			AuthenticationResponse response = getCommunicator().newAuthenticationRequest(authRequest);
			if(response.getIsError()) {
				error = response.getErrorResponse();
			}
			else {
				XmlBuilder authenticationURL = new XmlBuilder("authenticationURL");
				authenticationURL.setValue(response.getIssuerAuthenticationURL(), false);
				result.addSubElement(authenticationURL);

				XmlBuilder transactionIdXml = new XmlBuilder("transactionID");
				transactionIdXml.setValue(response.getTransactionID(), false);
				result.addSubElement(transactionIdXml);

				XmlBuilder creationTime = new XmlBuilder("creationTime");
				Date txDate = response.getTransactionCreateDateTimestamp().toGregorianCalendar().getTime();
				creationTime.setValue(DateFormatUtils.format(txDate, FULL_GENERIC_FORMATTER), false);
				result.addSubElement(creationTime);
			}

			if(StringUtils.isNotEmpty(response.getRawMessage())) {
				log.debug(response.getRawMessage());
			}
		}

		if(error != null) {
			XmlBuilder errorXml = new XmlBuilder("error");
			XmlBuilder statusCodeXml = new XmlBuilder("statusCode");
			statusCodeXml.setValue(error.getErrorCode());
			errorXml.addSubElement(statusCodeXml);

			XmlBuilder detailsXml = new XmlBuilder("details");
			detailsXml.setValue(error.getErrorDetails());
			errorXml.addSubElement(detailsXml);

			XmlBuilder messageXml = new XmlBuilder("message");
			messageXml.setValue(error.getErrorMessage());
			errorXml.addSubElement(messageXml);

			result.addSubElement(errorXml);
		}

		return new SenderResult(result.toXML());
	}

	@Override
	public String getPhysicalDestinationName() {
		StringBuilder destination = new StringBuilder();
		if(StringUtils.isNotEmpty(getMerchantReturnUrl()))
			destination.append(" returnUrl["+getMerchantReturnUrl()+"]");
		if(StringUtils.isNotEmpty(getAcquirerDirectoryUrl()))
			destination.append(" directoryUrl["+getAcquirerDirectoryUrl()+"]");
		if(StringUtils.isNotEmpty(getAcquirerTransactionUrl()))
			destination.append(" transactionUrl["+getAcquirerTransactionUrl()+"]");
		if(StringUtils.isNotEmpty(getAcquirerStatusUrl()))
			destination.append(" statusUrl["+getAcquirerStatusUrl()+"]");

		return destination.toString().trim();
	}


	/**
	 * @param action for the sender to execute, has to be one of "DIRECTORY", "RESPONSE" or "AUTHENTICATE".
	 */
	public void setAction(String action) {
		this.action = action.toUpperCase();
	}
	public String getAction() {
		return this.action;
	}

	/**
	 * This is the contract number for iDIN the Merchant received from its Acquirer after registration,
	 * and is used to unambiguously identify the Merchant. This number is 10-digits long, where the
	 * first four digits are equal to the AcquirerID.
	 * @param merchantMerchantID The contract number for the iDIN Merchant. Leading zeros must be included
	 */
	public void setMerchantID(String merchantMerchantID) {
		this.merchantID = merchantMerchantID;
	}
	public String getMerchantID() {
		return this.merchantID;
	}

	/**
	 * The SubID that uniquely defines the name and address of the Merchant to be used for iDIN,
	 * if operating under different brands or trading entities. The Merchant obtains the SubID
	 * from its Acquirer after registration for iDIN. A Merchant can request permission from
	 * the Acquirer to use one or more SubIDs.
	 *
	 * @param merchantSubID Unless agreed otherwise with the Acquirer, the Merchant has to use 0 (default)!
	 */
	public void setMerchantSubID(int merchantSubID) {
		this.merchantSubID = merchantSubID;
	}
	public int getMerchantSubID() {
		return this.merchantSubID;
	}

	/**
	 * The web address provided by the Merchant in the transaction request that is used to redirect the
	 * Consumer back to the Merchant after completing the authentication in the Issuer domain. The URL
	 * does not necessarily begin with http:// or https://, it can also start with an app handler
	 * e.g. companyname-nlservice://.
	 * @param merchantReturnUrl URL either http://..., https://... or app-hander e.g. company-service://...
	 */
	public void setMerchantReturnUrl(String merchantReturnUrl) {
		this.merchantReturnUrl = merchantReturnUrl;
	}
	public String getMerchantReturnUrl() {
		return this.merchantReturnUrl;
	}


	/**
	 * @param acquirerDirectoryUrl The web address of the Acquirer's Routing service platform from where the
	 * list of Issuers is retrieved (using a directory request).
	 */
	public void setAcquirerDirectoryUrl(String acquirerDirectoryUrl) {
		this.acquirerDirectoryUrl = acquirerDirectoryUrl;
	}
	public String getAcquirerDirectoryUrl() {
		return this.acquirerDirectoryUrl;
	}

	/**
	 * @param acquirerTransactionUrl The web address of the Acquirer's Routing Service platform
	 * where the transactions (authentication requests) are initiated.
	 */
	public void setAcquirerTransactionUrl(String acquirerTransactionUrl) {
		this.acquirerTransactionUrl = acquirerTransactionUrl;
	}
	public String getAcquirerTransactionUrl() {
		return this.acquirerTransactionUrl;
	}

	/**
	 * @param acquirerStatusUrl The web address of the Acquirer's Routing Service platform to where
	 * the library sends status request messages.
	 */
	public void setAcquirerStatusUrl(String acquirerStatusUrl) {
		this.acquirerStatusUrl = acquirerStatusUrl;
	}
	public String getAcquirerStatusUrl() {
		return this.acquirerStatusUrl;
	}


	/**
	 * The Java iDIN Software Library needs to access a keystore located in the Java classpath to
	 * store all the required certificates
	 *
	 * @param keyStoreLocation A file path and name, accessible to the library, which
	 * is the Java keystore file where the certificates are stored
	 */
	public void setKeyStoreLocation(String keyStoreLocation) {
		this.keyStoreLocation = keyStoreLocation;
	}
	public String getKeyStoreLocation() {
		return this.keyStoreLocation;
	}

	/**
	 * The password used to access the keystore
	 * @param keyStorePassword The password for the keystore
	 */
	public void setKeyStorePassword(String keyStorePassword) {
		this.keyStoreCredentials = new CredentialFactory(null, null, keyStorePassword);
	}
	/**
	 * The AuthAlias used to access the keystore
	 * @param keyStoreAuthAlias The AuthAlias that contains the password for the keystore
	 */
	public void setKeyStoreAuthAlias(String keyStoreAuthAlias) {
		this.keyStoreCredentials = new CredentialFactory(keyStoreAuthAlias);
	}
	public String getKeyStorePassword() {
		if(keyStoreCredentials == null)
			return null;

		return keyStoreCredentials.getPassword();
	}

	/**
	 * This is the certificate owned by the Merchant. It's the private certificate
	 * used to sign messages sent by the Merchant to the Acquirer's Routing Service platform. Its public
	 * key is also used by the Acquirer to authenticate incoming messages from the Merchant. The
	 * Merchant certificate must be in PKCS#12 format which has the extension .p12 or .pfx
	 *
	 * @param merchantCertificateAlias The alias assigned to the signing certificate in the keystore file.
	 * This could be the alias you supplied explicitly when importing an existing certificate in the keystore,
	 * or it could be an alias automatically assigned by the keytool application.
	 */
	public void setMerchantCertificateAlias(String merchantCertificateAlias) {
		this.merchantCertificateAlias = merchantCertificateAlias;
	}
	public String getMerchantCertificateAlias() {
		return this.merchantCertificateAlias;
	}

	/**
	 * In case the merchant certificate has been password protected
	 * @param merchantCertificatePassword The password for the Merchant Certificate
	 */
	public void setMerchantCertificatePassword(String merchantCertificatePassword) {
		this.merchantCertificateCredentials = new CredentialFactory(null, null, merchantCertificatePassword);
	}
	/**
	 * In case the merchant certificate has been password protected
	 * @param merchantCertificateAuthAlias The AuthAlias that contains the password for the Merchant Certificate
	 */
	public void setMerchantCertificateAuthAlias(String merchantCertificateAuthAlias) {
		this.merchantCertificateCredentials = new CredentialFactory(merchantCertificateAuthAlias);
	}
	public String getMerchantCertificatePassword() {
		if(merchantCertificateCredentials == null)
			return null;

		return merchantCertificateCredentials.getPassword();
	}

	/**
	 * This is the public certificate used to authenticate incoming messages from the Acquirer. The library
	 * only needs its public key. The public certificate must be in PEM format (base64 ASCII) and typically
	 * has the file extension .cer,.crt or .pem.
	 *
	 * @param acquirerCertificateAlias : The alias assigned to the Acquirer's certificate in the keystore.
	 * This could be the alias you supplied explicitly when importing an existing certificate in the keystore,
	 * or it could be an alias automatically assigned by the keytool application.
	 */
	public void setAcquirerCertificateAlias(String acquirerCertificateAlias) {
		this.acquirerCertificateAlias = acquirerCertificateAlias;
	}
	public String getAcquirerCertificateAlias() {
		return this.acquirerCertificateAlias;
	}

	/**
	 * This is the public certificate used to authenticate incoming messages from the Acquirer. The library
	 * only needs its public key. The public certificate must be in PEM format (base64 ASCII) and typically
	 * has the file extension .cer,.crt or .pem.
	 *
	 * @param acquirerAlternativeCertificateAlias : The alias assigned to the Acquirer's certificate in the keystore.
	 * This could be the alias you supplied explicitly when importing an existing certificate in the keystore,
	 * or it could be an alias automatically assigned by the keytool application.
	 */
	public void setAcquirerAlternativeCertificateAlias(String acquirerAlternativeCertificateAlias) {
		this.acquirerAlternativeCertificateAlias = acquirerAlternativeCertificateAlias;
	}
	public String getAcquirerAlternativeCertificateAlias() {
		return this.acquirerAlternativeCertificateAlias;
	}

	/**
	 * This is the certificate owned by the Merchant. Its public key is used by the Issuer to encrypt information.
	 * The Merchant can then use the private key to decrypt that information. The SAML certificate must be in
	 * PKCS#12 format which has the extension .p12 or .pfx;
	 *
	 * @param SAMLCertificateAlias The alias assigned to the SAML certificate in the keystore.
	 * This could  be the alias supplied explicitly when importing an existing certificate in the keystore,
	 * or it could be an alias automatically assigned by the keytool application.
	 */
	public void setSAMLCertificateAlias(String SAMLCertificateAlias) {
		this.SAMLCertificateAlias = SAMLCertificateAlias;
	}
	public String getSAMLCertificateAlias() {
		return this.SAMLCertificateAlias;
	}

	/**
	 * In case the SAML certificate has been password protected
	 * @param SAMLCertificatePassword The password for the SAML Certificate
	 */
	public void setSAMLCertificatePassword(String SAMLCertificatePassword) {
		this.SAMLCertificateCredentials = new CredentialFactory(null, null, SAMLCertificatePassword);
	}
	/**
	 * In case the SAML certificate has been password protected
	 * @param SAMLCertificateAuthAlias The AuthAlias that contains the password for the SAML Certificate
	 */
	public void setSAMLCertificateAuthAlias(String SAMLCertificateAuthAlias) {
		this.SAMLCertificateCredentials = new CredentialFactory(SAMLCertificateAuthAlias);
	}
	public String getSAMLCertificatePassword() {
		if(SAMLCertificateCredentials == null)
			return null;

		return SAMLCertificateCredentials.getPassword();
	}


	public void setLogsEnabled(boolean logsEnabled) {
		this.logsEnabled = logsEnabled;
	}
	public boolean getLogsEnabled() {
		return this.logsEnabled;
	}

	public void setServiceLogsEnabled(boolean serviceLogsEnabled) {
		this.serviceLogsEnabled = serviceLogsEnabled;
	}
	public boolean getServiceLogsEnabled() {
		return this.serviceLogsEnabled;
	}

	public void setServiceLogsLocation(String serviceLogsLocation) {
		this.serviceLogsLocation = serviceLogsLocation;
	}
	public String getServiceLogsLocation() {
		return this.serviceLogsLocation;
	}

	public void setServiceLogsPattern(String serviceLogsPattern) {
		this.serviceLogsPattern = serviceLogsPattern;
	}
	public String getServiceLogsPattern() {
		return this.serviceLogsPattern;
	}

	@Override
	public String getDomain() {
		return domain;
	}
}

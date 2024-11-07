/*
   Copyright 2018 Nationale-Nederlanden, 2024 WeAreFrank!

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
package org.frankframework.extensions.idin;

import static org.frankframework.util.DateFormatUtils.FULL_GENERIC_FORMATTER;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.ParserConfigurationException;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import lombok.Getter;
import lombok.Setter;
import net.bankid.merchant.library.AssuranceLevel;
import net.bankid.merchant.library.AuthenticationRequest;
import net.bankid.merchant.library.AuthenticationResponse;
import net.bankid.merchant.library.Communicator;
import net.bankid.merchant.library.Configuration;
import net.bankid.merchant.library.DirectoryResponse;
import net.bankid.merchant.library.ErrorResponse;
import net.bankid.merchant.library.IMessenger;
import net.bankid.merchant.library.SamlResponse;
import net.bankid.merchant.library.ServiceId;
import net.bankid.merchant.library.StatusRequest;
import net.bankid.merchant.library.StatusResponse;
import net.bankid.merchant.library.internal.DirectoryResponseBase.Issuer;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.doc.Mandatory;
import org.frankframework.senders.AbstractSenderWithParameters;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.DomBuilderException;
import org.frankframework.util.XmlBuilder;
import org.frankframework.util.XmlEncodingUtils;
import org.frankframework.util.XmlUtils;

/**
 * Requires the net.bankid.merchant.library V1.2.9
 *
 * @author Niels Meijer
 */
public class IdinSender extends AbstractSenderWithParameters implements HasPhysicalDestination {
	private @Getter final String domain = "iDin";

	private @Getter String merchantID = null;
	private @Getter int merchantSubID = 0;

	private @Getter boolean tls12Enabled=true;
	private @Getter String keyStoreLocation = null;
	private CredentialFactory keyStoreCredentials = null;

	private @Getter String iDinConfigurationXML = null;
	private @Getter String merchantReturnUrl = null;
	private @Getter String merchantReturnUrlSessionKey;

	private @Getter String acquirerDirectoryUrl = null;
	private @Getter String acquirerTransactionUrl = null;
	private @Getter String acquirerStatusUrl = null;

	private @Getter String merchantCertificateAlias = null;
	private @Getter CredentialFactory merchantCertificateCredentials = null;
	private @Getter String acquirerCertificateAlias = null;
	private @Getter String acquirerAlternativeCertificateAlias = null;
	private @Getter String samlCertificateAlias = null;
	private @Getter CredentialFactory samlCertificateCredentials = null;

	private @Getter boolean logsEnabled = false;
	private @Getter boolean serviceLogsEnabled = false;
	private @Getter String serviceLogsLocation = null;
	private @Getter String serviceLogsPattern = "%Y-%M-%D\\%h%m%s.%f-%a.xml";

	private Action action = Action.DIRECTORY;

	public enum Action {
		DIRECTORY, RESPONSE, AUTHENTICATE
	}

	private Configuration defaultIdinConfig = null;
	private @Setter IMessenger messenger = null; // use the default package private one...

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		defaultIdinConfig = getConfiguration();

		if(StringUtils.isEmpty(defaultIdinConfig.getMerchantReturnUrl()) && StringUtils.isEmpty(getMerchantReturnUrlSessionKey())) {
			throw new ConfigurationException("no MerchantReturnUrl set");
		}

		// iDin currently works (somehow *full-stop*) with the jdk's default HttpURLConnection. It's not possible to set custom keystores, proxy's etc.
		// In order to implement this properly we should consider making the iDinSender a HttpSender, so it inherits the nightmare of configurable SSL and proxy settings.
		// A 'proxy authentication fix' is below, this will however be used by default by ALL HttpURLConnection's in this JVM, therefore highly discouraged!

//		cf=new CredentialFactory(getProxyAuthAliasName(), null, null);
//		Authenticator.setDefault( new Authenticator() {
//            @Override
//            protected PasswordAuthentication getPasswordAuthentication() {
//                if (getRequestorType().equals(RequestorType.PROXY)) {
//                    return new PasswordAuthentication(cf.getUsername(), cf.getPassword().toCharArray());
//                }
//                return super.getPasswordAuthentication();
//            }
//        } );
	}

	private Configuration getConfiguration() throws ConfigurationException {
		Configuration idinConfig = createConfiguration();

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

		if(StringUtils.isNotEmpty(getSamlCertificateAlias())) {
			idinConfig.setSamlCertificateAlias(getSamlCertificateAlias());
			if(StringUtils.isNotEmpty(getSAMLCertificatePassword()))
				idinConfig.setSamlCertificatePassword(getSAMLCertificatePassword());
		}

		if(isLogsEnabled())
			idinConfig.setLogsEnabled(true);
		if(isServiceLogsEnabled())
			idinConfig.setServiceLogsEnabled(true);
		if(StringUtils.isNotEmpty(getServiceLogsLocation()))
			idinConfig.setServiceLogsLocation(getServiceLogsLocation());
		if(StringUtils.isNotEmpty(getServiceLogsPattern()))
			idinConfig.setServiceLogsPattern(getServiceLogsPattern());

		idinConfig.setTls12Enabled(isTls12Enabled());

		try {
			idinConfig.Setup(idinConfig); // Somehow required to setup the KeyStoreKeyProviderFactory.
		} catch (IOException e) {
			throw new ConfigurationException("unable to setup keyProvider");
		}

		return idinConfig;
	}

	protected Configuration createConfiguration() throws ConfigurationException {
		Configuration config = null;
		if(StringUtils.isNotEmpty(getIDinConfigurationXML())) {
			URL defaultIdinConfigXML = ClassLoaderUtils.getResourceURL(this, getIDinConfigurationXML());
			if(defaultIdinConfigXML == null) {
				throw new ConfigurationException("unable to find iDin configuration from XML file ["+getIDinConfigurationXML()+"]");
			}

			try {
				config = new Configuration();
				config.Load(defaultIdinConfigXML.openStream());
			} catch (ParserConfigurationException | SAXException | IOException e) {
				throw new ConfigurationException("unable to read iDin configuration from XML file ["+getIDinConfigurationXML()+"]", e);
			}
		} else {
			config = new Configuration(getMerchantID(),
										getMerchantSubID(),
										getMerchantReturnUrl(),
										getKeyStoreLocation(),
										getKeyStorePassword(),
										getMerchantCertificateAlias(),
										getMerchantCertificatePassword(),
										getAcquirerCertificateAlias(),
										getAcquirerAlternativeCertificateAlias(),
										getAcquirerDirectoryUrl(),
										getAcquirerTransactionUrl(),
										getAcquirerStatusUrl(),
										isLogsEnabled(),
										isServiceLogsEnabled(),
										getServiceLogsLocation(),
										getServiceLogsPattern(),
										isTls12Enabled(),
										null);
		}
		return config;
	}

	protected Communicator createCommunicator(PipeLineSession session) throws SenderException {
		final DynamicMessengerCommunicator communicator;
		if(StringUtils.isNotEmpty(getMerchantReturnUrlSessionKey())) {
			String returnUrl = session.getString(getMerchantReturnUrlSessionKey());
			if(StringUtils.isEmpty(returnUrl)) {
				throw new SenderException("dynamic MerchantReturnUrl not found in sessionkey ["+getMerchantReturnUrlSessionKey()+"]");
			}

			Configuration idinConfig = new Configuration();
			try {
				idinConfig.Setup(defaultIdinConfig); // Vague copy method...
			} catch (IOException e) {
				throw new SenderException("unable to copy configuration");
			}
			idinConfig.setMerchantReturnUrl(returnUrl);
			communicator = new DynamicMessengerCommunicator(idinConfig);
		} else {
			communicator = new DynamicMessengerCommunicator(defaultIdinConfig);
		}

		if(messenger != null) {
			communicator.setMessenger(messenger);
		}

		return communicator;
	}

	@Override
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException {

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

		Communicator communicator = createCommunicator(session);

		XmlBuilder result = new XmlBuilder("result");
		ErrorResponse error = null;

		if(action == Action.DIRECTORY) {
			DirectoryResponse response = communicator.getDirectory();

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
				timestamp.setValue(toFormattedDate(response.getDirectoryDateTimestamp()), false);
				result.addSubElement(timestamp);

				log.debug("received directory response [{}]", response::getRawMessage);
			}
		}
		else if(action == Action.RESPONSE) {
			String transactionID = XmlUtils.getChildTagAsString(queryElement, "transactionID");
			if(StringUtils.isEmpty(transactionID))
				throw new SenderException("no transactionID was supplied");

			StatusRequest statusRequest = new StatusRequest();
			statusRequest.setTransactionID(transactionID);
			StatusResponse response = communicator.getResponse(statusRequest);

			if(response.getIsError()) {
				error = response.getErrorResponse();
				log.debug("received status response error [{}]", response::getRawMessage);
			}
			else {
				XmlBuilder status = new XmlBuilder("status");
				status.setValue(response.getStatus(), false);
				result.addSubElement(status);

				if(StatusResponse.Success.equals(response.getStatus())) {
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

					XmlBuilder transactionIdXml = new XmlBuilder("transactionID");
					transactionIdXml.setValue(response.getTransactionID(), false);
					result.addSubElement(transactionIdXml);

					XmlBuilder timestamp = new XmlBuilder("timestamp");
					timestamp.setValue(toFormattedDate(response.getStatusDateTimestamp()), false);
					result.addSubElement(timestamp);
				}

				log.debug("received status [{}] response [{}]", response::getStatus, response::getRawMessage);
			}
		}
		else if(action == Action.AUTHENTICATE) {
			AuthenticationRequest authRequest = new AuthenticationRequest();

			String issuerId = XmlUtils.getChildTagAsString(queryElement, "issuerId");
			if(StringUtils.isEmpty(issuerId))
				throw new SenderException("no issuerId was supplied");
			authRequest.setIssuerID(issuerId);

			String requestedServiceId = XmlUtils.getChildTagAsString(queryElement, "requestedServiceId");
			if(StringUtils.isEmpty(requestedServiceId)) {
				throw new SenderException("no requestedServiceId was supplied");
			}
			authRequest.setRequestedServiceID(new ServiceId(requestedServiceId));

			String entranceCode = XmlUtils.getChildTagAsString(queryElement, "entranceCode");
			if(StringUtils.isEmpty(entranceCode)) {
				throw new SenderException("no entranceCode was supplied");
			}
			authRequest.setEntranceCode(entranceCode);

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


			String merchantReference = XmlUtils.getChildTagAsString(queryElement, "merchantReference");
			if(StringUtils.isNotEmpty(merchantReference))
				authRequest.setMerchantReference(merchantReference);

			AssuranceLevel assuranceLevel = AssuranceLevel.Loa3;
			String assurance = XmlUtils.getChildTagAsString(queryElement, "assuranceLevel");
			if(StringUtils.isNotEmpty(assurance))
				assuranceLevel = AssuranceLevel.valueOf(assurance);
			authRequest.setAssuranceLevel(assuranceLevel);


			AuthenticationResponse response = communicator.newAuthenticationRequest(authRequest);
			if(response.getIsError()) {
				error = response.getErrorResponse();
				log.debug("received authentication response error [{}]", response::getRawMessage);
			}
			else {
				XmlBuilder authenticationURL = new XmlBuilder("authenticationURL");
				String url = XmlEncodingUtils.encodeChars(response.getIssuerAuthenticationURL());
				authenticationURL.setValue(url, false);
				result.addSubElement(authenticationURL);

				XmlBuilder transactionIdXml = new XmlBuilder("transactionID");
				transactionIdXml.setValue(response.getTransactionID(), false);
				result.addSubElement(transactionIdXml);

				XmlBuilder creationTime = new XmlBuilder("createDateTimestamp");
				creationTime.setValue(toFormattedDate(response.getTransactionCreateDateTimestamp()), false);
				result.addSubElement(creationTime);

				log.debug("received authentication response [{}]", response::getRawMessage);
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

		return new SenderResult(result.asXmlString());
	}

	@Override
	public String getPhysicalDestinationName() {
		StringBuilder destination = new StringBuilder();
		if(StringUtils.isNotEmpty(getMerchantReturnUrl()))
			destination.append(" returnUrl["+getMerchantReturnUrl()+"]");
		if(StringUtils.isNotEmpty(getMerchantReturnUrlSessionKey()))
			destination.append(" returnUrl[dynamicReturnUrl]");

		if(StringUtils.isNotEmpty(getAcquirerDirectoryUrl()))
			destination.append(" directoryUrl["+getAcquirerDirectoryUrl()+"]");
		if(StringUtils.isNotEmpty(getAcquirerTransactionUrl()))
			destination.append(" transactionUrl["+getAcquirerTransactionUrl()+"]");
		if(StringUtils.isNotEmpty(getAcquirerStatusUrl()))
			destination.append(" statusUrl["+getAcquirerStatusUrl()+"]");

		return destination.toString().trim();
	}

	private String toFormattedDate(XMLGregorianCalendar xmlGregorianCalendar) {
		Instant txDate = xmlGregorianCalendar.toGregorianCalendar().getTime().toInstant();
		return DateFormatUtils.format(txDate, FULL_GENERIC_FORMATTER);
	}


	/**
	 * @param action for the sender to execute, has to be one of "DIRECTORY", "RESPONSE" or "AUTHENTICATE".
	 */
	public void setAction(Action action) {
		this.action = action;
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


	/**
	 * @param acquirerDirectoryUrl The web address of the Acquirer's Routing service platform from where the
	 * list of Issuers is retrieved (using a directory request).
	 */
	public void setAcquirerDirectoryUrl(String acquirerDirectoryUrl) {
		this.acquirerDirectoryUrl = acquirerDirectoryUrl;
	}

	/**
	 * @param acquirerTransactionUrl The web address of the Acquirer's Routing Service platform
	 * where the transactions (authentication requests) are initiated.
	 */
	public void setAcquirerTransactionUrl(String acquirerTransactionUrl) {
		this.acquirerTransactionUrl = acquirerTransactionUrl;
	}

	/**
	 * @param acquirerStatusUrl The web address of the Acquirer's Routing Service platform to where
	 * the library sends status request messages.
	 */
	public void setAcquirerStatusUrl(String acquirerStatusUrl) {
		this.acquirerStatusUrl = acquirerStatusUrl;
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

	/**
	 * This is the certificate owned by the Merchant. Its public key is used by the Issuer to encrypt information.
	 * The Merchant can then use the private key to decrypt that information. The SAML certificate must be in
	 * PKCS#12 format which has the extension .p12 or .pfx;
	 *
	 * @param samlCertificateAlias The alias assigned to the SAML certificate in the keystore.
	 * This could  be the alias supplied explicitly when importing an existing certificate in the keystore,
	 * or it could be an alias automatically assigned by the keytool application.
	 */
	public void setSamlCertificateAlias(String samlCertificateAlias) {
		this.samlCertificateAlias = samlCertificateAlias;
	}

	/**
	 * In case the SAML certificate has been password protected
	 * @param samlCertificatePassword The password for the SAML Certificate
	 */
	public void setSAMLCertificatePassword(String samlCertificatePassword) {
		this.samlCertificateCredentials = new CredentialFactory(null, null, samlCertificatePassword);
	}
	/**
	 * In case the SAML certificate has been password protected
	 * @param samlCertificateAuthAlias The AuthAlias that contains the password for the SAML Certificate
	 */
	public void setSAMLCertificateAuthAlias(String samlCertificateAuthAlias) {
		this.samlCertificateCredentials = new CredentialFactory(samlCertificateAuthAlias);
	}
	public String getSAMLCertificatePassword() {
		if(samlCertificateCredentials == null)
			return null;

		return samlCertificateCredentials.getPassword();
	}


	public void setLogsEnabled(boolean logsEnabled) {
		this.logsEnabled = logsEnabled;
	}

	public void setServiceLogsEnabled(boolean serviceLogsEnabled) {
		this.serviceLogsEnabled = serviceLogsEnabled;
	}

	public void setServiceLogsLocation(String serviceLogsLocation) {
		this.serviceLogsLocation = serviceLogsLocation;
	}

	public void setServiceLogsPattern(String serviceLogsPattern) {
		this.serviceLogsPattern = serviceLogsPattern;
	}

	/**
	 * @param tls12Enabled the tls12Enabled to set
	 */
	public void setTls12Enabled(boolean tls12Enabled) {
		this.tls12Enabled = tls12Enabled;
	}

	/**
	 * @param merchantReturnUrlSessionKey the merchantReturnUrlSessionKey to set
	 */
	public void setMerchantReturnUrlSessionKey(String merchantReturnUrlSessionKey) {
		this.merchantReturnUrlSessionKey = merchantReturnUrlSessionKey;
	}

	/**
	 * Load configuration from XML. Attributes may overwrite this 'default'.
	 */
	@Mandatory
	public void setConfigurationXML(String iDinConfigurationXML) {
		this.iDinConfigurationXML = iDinConfigurationXML;
	}
}

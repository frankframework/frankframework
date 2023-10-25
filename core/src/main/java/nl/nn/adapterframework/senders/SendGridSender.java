/*
   Copyright 2019, 2021-2022 WeAreFrank!

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
package nl.nn.adapterframework.senders;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.sendgrid.Client;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Attachments.Builder;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDocRef;
import nl.nn.adapterframework.encryption.HasKeystore;
import nl.nn.adapterframework.encryption.HasTruststore;
import nl.nn.adapterframework.encryption.KeystoreType;
import nl.nn.adapterframework.http.HttpSender;
import nl.nn.adapterframework.http.HttpSenderBase;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Sender that sends a mail via SendGrid v3 (cloud-based SMTP provider).
 *
 * Sample XML file can be found in the path: iaf-core/src/test/resources/emailSamplesXML/emailSample.xml
 * @author alisihab
 */
public class SendGridSender extends MailSenderBase implements HasKeystore, HasTruststore {

	private final String HTTPSENDERBASE = "nl.nn.adapterframework.http.HttpSenderBase";

	private String url="http://smtp.sendgrid.net";
	private SendGrid sendGrid;
	private HttpSenderBase httpSender;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getCredentialFactory().getPassword())) {
			throw new ConfigurationException("Please provide an API key");
		}
		httpSender = new HttpSender();
		httpSender.setUrl(url);
		httpSender.configure();
	}

	@Override
	public void open() throws SenderException {
		super.open();
		httpSender.open();

		CloseableHttpClient httpClient = httpSender.getHttpClient();
		if(httpClient == null)
			throw new SenderException("no HttpClient found, did it initialize properly?");

		Client client = new Client(httpClient);
		sendGrid = new SendGrid(getCredentialFactory().getPassword(), client);
	}

	@Override
	public void close() throws SenderException {
		super.close();
		httpSender.close();
	}

	@Override
	public String sendEmail(MailSession mailSession) throws SenderException {
		String result;
		Mail mail;

		try {
			mail = createEmail(mailSession);
		} catch (Exception e) {
			throw new SenderException("Exception occured while composing email", e);
		}

		try {
			Request request = new Request();
			request.setMethod(Method.POST);
			request.setEndpoint("mail/send");
			request.setBody(mail.build());
			Response response = sendGrid.api(request);
			result = response.getBody();
			log.debug("Mail send result" + result);
			return result;
		} catch (Exception e) {
			throw new SenderException(
					getLogPrefix() + "exception sending mail with subject [" + mail.getSubject() + "]", e);
		}
	}

	/**
	 * Creates sendgrid mail object
	 */
	private Mail createEmail(MailSession mailSession) throws SenderException, DomBuilderException, IOException {
		Mail mail = new Mail();
		Personalization personalization = new Personalization();

		List<EMail> emailList = mailSession.getRecipientList();
		EMail from = mailSession.getFrom();
		EMail replyTo = mailSession.getReplyTo();
		setEmailAddresses(mail, personalization, emailList, from, replyTo);

		String subject = mailSession.getSubject();
		setSubject(mail, personalization, subject);

		setMessage(mail, mailSession);

		List<MailAttachmentStream> attachmentList = mailSession.getAttachmentList();
		setAttachments(mail, attachmentList);

		Collection<Node> headers = mailSession.getHeaders();
		setHeader(mail, personalization, headers);
		mail.addPersonalization(personalization);
		return mail;
	}

	/**
	 * Sets header of mail object if header exists
	 * @param mail
	 * @param personalization
	 * @param headers
	 */
	private void setHeader(Mail mail, Personalization personalization, Collection<Node> headers) {
		if (headers != null && !headers.isEmpty()) {
			Iterator<Node> iter = headers.iterator();
			while (iter.hasNext()) {
				Element headerElement = (Element) iter.next();
				String headerName = headerElement.getAttribute("name");
				String headerValue = XmlUtils.getStringValue(headerElement);
				personalization.addHeader(headerName, headerValue);
				mail.addHeader(headerName, headerValue);
			}
		}
	}

	/**
	 * Adds attachments to mail Object if there is any
	 * @param mail
	 * @param attachmentList
	 * @throws SenderException
	 * @throws IOException
	 */
	private void setAttachments(Mail mail, List<MailAttachmentStream> attachmentList) throws SenderException, IOException {
		if (attachmentList != null) {
			Iterator<MailAttachmentStream> iter = attachmentList.iterator();
			while (iter.hasNext()) {
				MailAttachmentStream mailAttachment = iter.next();
				Builder sendGridAttachment = new Attachments.Builder(mailAttachment.getName(), mailAttachment.getContent());
				sendGridAttachment.withType(mailAttachment.getMimeType());
				mail.addAttachments(sendGridAttachment.build());
			}
		}
	}

	/**
	 * Sets content of email to mail Object
	 */
	private void setMessage(Mail mail, MailSession mailSession) {
		String message = mailSession.getMessage();
		String messageType = mailSession.getMessageType();

		String messageContent = null;
		if (StringUtils.isNotEmpty(message)) {
			Content content = new Content();
			if (mailSession.isMessageBase64()) {
				messageContent = new String(Base64.decodeBase64(message));
			} else {
				messageContent = message;
			}
			if ("text/html".equalsIgnoreCase(messageType)) {
				content.setType("text/html");
				content.setValue(messageContent);
			} else {
				content.setType("text/plain");
				content.setValue(messageContent);
			}
			mail.addContent(content);
		}
	}

	/**
	 * Sets subject to the mail address
	 * @param mail
	 * @param personalization
	 * @param subject
	 */
	private void setSubject(Mail mail, Personalization personalization, String subject) {
		if (StringUtils.isNotEmpty(subject)) {
			personalization.setSubject(subject);
			mail.setSubject(subject);
		} else {
			String defaultSubject = getDefaultSubject();
			personalization.setSubject(defaultSubject);
			mail.setSubject(defaultSubject);
		}
	}

	/**
	 * Sets recipients, sender and replyto to mail object
	 * @param mail
	 * @param personalization
	 * @param list
	 * @param replyTo
	 * @param from
	 * @throws SenderException
	 */
	private void setEmailAddresses(Mail mail, Personalization personalization, List<EMail> list, EMail from,
			EMail replyTo) throws SenderException {
		if (list != null && !list.isEmpty()) {
			for (EMail e : list) {
				if (!isRecipientWhitelisted(e)) {
					log.warn("Recipient [{}] ignored, not in domain whitelist [{}]", ()->e, this::getDomainWhitelist);
					continue;
				}

				if ("cc".equalsIgnoreCase(e.getType())) {
					Email cc = new Email();
					cc.setName(e.getName());
					cc.setEmail(e.getAddress());
					personalization.addCc(cc);
				} else if ("bcc".equalsIgnoreCase(e.getType())) {
					Email bcc = new Email();
					bcc.setName(e.getName());
					bcc.setEmail(e.getAddress());
					personalization.addBcc(bcc);
				} else if ("to".equalsIgnoreCase(e.getType())) {
					Email to = new Email();
					to.setEmail(e.getAddress());
					to.setName(e.getName());
					personalization.addTo(to);
				} else {
					throw new SenderException("Recipients not found");
				}
			}
		} else {
			throw new SenderException("Recipients not found");
		}
		Email fromEmail = new Email();
		if (from != null && from.getAddress() != null && !from.getAddress().isEmpty()) {
			fromEmail.setEmail(from.getAddress());
			fromEmail.setEmail(from.getAddress());
			fromEmail.setName(from.getName());
			mail.setFrom(fromEmail);
		} else {
			throw new SenderException("Sender mail address cannot be empty");
		}

		if (replyTo != null && !replyTo.getAddress().isEmpty()) {
			Email replyToEmail = new Email();
			replyToEmail.setEmail(replyTo.getAddress());
			replyToEmail.setName(replyTo.getName());
			mail.setReplyTo(replyToEmail);
		}
	}

	//Properties inherited from HttpSenderBase

	@Override
	@IbisDocRef({HTTPSENDERBASE})
	public void setTimeout(int i) {
		super.setTimeout(i);
		httpSender.setTimeout(i);
	}

	@IbisDocRef({HTTPSENDERBASE})
	public void setMaxConnections(int i) {
		httpSender.setMaxConnections(i);
	}

	@IbisDocRef({HTTPSENDERBASE})
	public void setMaxExecuteRetries(int i) {
		httpSender.setMaxExecuteRetries(i);
	}


	@IbisDocRef({HTTPSENDERBASE})
	public void setProxyHost(String string) {
		httpSender.setProxyHost(string);
	}

	@IbisDocRef({HTTPSENDERBASE})
	public void setProxyPort(int i) {
		httpSender.setProxyPort(i);
	}

	@IbisDocRef({HTTPSENDERBASE})
	public void setProxyAuthAlias(String string) {
		httpSender.setProxyAuthAlias(string);
	}

	@IbisDocRef({HTTPSENDERBASE})
	public void setProxyUsername(String string) {
		httpSender.setProxyUsername(string);
	}
	@Deprecated
	@ConfigurationWarning("Please use \"proxyUsername\" instead")
	public void setProxyUserName(String string) {
		setProxyUsername(string);
	}
	@IbisDocRef({HTTPSENDERBASE})
	public void setProxyPassword(String string) {
		httpSender.setProxyPassword(string);
	}

	@IbisDocRef({HTTPSENDERBASE})
	public void setProxyRealm(String string) {
		httpSender.setProxyRealm(string);
	}



	@Deprecated
	@ConfigurationWarning("Please use attribute keystore instead")
	public void setCertificate(String string) {
		setKeystore(string);
	}
	@Deprecated
	@ConfigurationWarning("has been replaced with keystoreType")
	public void setCertificateType(KeystoreType value) {
		setKeystoreType(value);
	}
	@Deprecated
	@ConfigurationWarning("Please use attribute keystoreAuthAlias instead")
	public void setCertificateAuthAlias(String string) {
		setKeystoreAuthAlias(string);
	}
	@Deprecated
	@ConfigurationWarning("Please use attribute keystorePassword instead")
	public void setCertificatePassword(String string) {
		setKeystorePassword(string);
	}


	@Override
	@IbisDocRef({HTTPSENDERBASE})
	public void setKeystore(String keystore) {
		httpSender.setKeystore(keystore);
	}
	@Override
	public String getKeystore() {
		return httpSender.getKeystore();
	}

	@Override
	@IbisDocRef({HTTPSENDERBASE})
	public void setKeystoreType(KeystoreType keystoreType) {
		httpSender.setKeystoreType(keystoreType);
	}
	@Override
	public KeystoreType getKeystoreType() {
		return httpSender.getKeystoreType();
	}

	@Override
	@IbisDocRef({HTTPSENDERBASE})
	public void setKeystoreAuthAlias(String keystoreAuthAlias) {
		httpSender.setKeystoreAuthAlias(keystoreAuthAlias);
	}
	@Override
	public String getKeystoreAuthAlias() {
		return httpSender.getKeystoreAuthAlias();
	}

	@Override
	@IbisDocRef({HTTPSENDERBASE})
	public void setKeystorePassword(String keystorePassword) {
		httpSender.setKeystorePassword(keystorePassword);
	}
	@Override
	public String getKeystorePassword() {
		return httpSender.getKeystorePassword();
	}

	@Override
	@IbisDocRef({HTTPSENDERBASE})
	public void setKeystoreAlias(String keystoreAlias) {
		httpSender.setKeystoreAlias(keystoreAlias);
	}
	@Override
	public String getKeystoreAlias() {
		return httpSender.getKeystoreAlias();
	}

	@Override
	@IbisDocRef({HTTPSENDERBASE})
	public void setKeystoreAliasAuthAlias(String keystoreAliasAuthAlias) {
		httpSender.setKeystoreAliasAuthAlias(keystoreAliasAuthAlias);
	}
	@Override
	public String getKeystoreAliasAuthAlias() {
		return httpSender.getKeystoreAliasAuthAlias();
	}

	@Override
	@IbisDocRef({HTTPSENDERBASE})
	public void setKeystoreAliasPassword(String keystoreAliasPassword) {
		httpSender.setKeystoreAliasPassword(keystoreAliasPassword);
	}
	@Override
	public String getKeystoreAliasPassword() {
		return httpSender.getKeystoreAliasPassword();
	}

	@Override
	@IbisDocRef({HTTPSENDERBASE})
	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		httpSender.setKeyManagerAlgorithm(keyManagerAlgorithm);
	}
	@Override
	public String getKeyManagerAlgorithm() {
		return httpSender.getKeyManagerAlgorithm();
	}

	@Override
	@IbisDocRef({HTTPSENDERBASE})
	public void setTruststore(String truststore) {
		httpSender.setTruststore(truststore);
	}
	@Override
	public String getTruststore() {
		return httpSender.getTruststore();
	}

	@Override
	@IbisDocRef({HTTPSENDERBASE})
	public void setTruststoreType(KeystoreType truststoreType) {
		httpSender.setTruststoreType(truststoreType);
	}
	@Override
	public KeystoreType getTruststoreType() {
		return httpSender.getTruststoreType();
	}


	@Override
	@IbisDocRef({HTTPSENDERBASE})
	public void setTruststoreAuthAlias(String truststoreAuthAlias) {
		httpSender.setTruststoreAuthAlias(truststoreAuthAlias);
	}
	@Override
	public String getTruststoreAuthAlias() {
		return httpSender.getTruststoreAuthAlias();
	}

	@Override
	@IbisDocRef({HTTPSENDERBASE})
	public void setTruststorePassword(String truststorePassword) {
		httpSender.setTruststorePassword(truststorePassword);
	}
	@Override
	public String getTruststorePassword() {
		return httpSender.getTruststorePassword();
	}

	@Override
	@IbisDocRef({HTTPSENDERBASE})
	public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
		httpSender.setTrustManagerAlgorithm(trustManagerAlgorithm);
	}
	@Override
	public String getTrustManagerAlgorithm() {
		return httpSender.getTrustManagerAlgorithm();
	}

	@Override
	@IbisDocRef({HTTPSENDERBASE})
	public void setVerifyHostname(boolean verifyHostname) {
		httpSender.setVerifyHostname(verifyHostname);
	}
	@Override
	public boolean isVerifyHostname() {
		return httpSender.isVerifyHostname();
	}

	@Override
	@IbisDocRef({HTTPSENDERBASE})
	public void setAllowSelfSignedCertificates(boolean testModeNoCertificatorCheck) {
		httpSender.setAllowSelfSignedCertificates(testModeNoCertificatorCheck);
	}
	@Override
	public boolean isAllowSelfSignedCertificates() {
		return httpSender.isAllowSelfSignedCertificates();
	}

	@Override
	@IbisDocRef({HTTPSENDERBASE})
	public void setIgnoreCertificateExpiredException(boolean ignoreCertificateExpiredException) {
		httpSender.setIgnoreCertificateExpiredException(ignoreCertificateExpiredException);
	}
	@Override
	public boolean isIgnoreCertificateExpiredException() {
		return httpSender.isIgnoreCertificateExpiredException();
	}


	@IbisDocRef({HTTPSENDERBASE})
	public void setFollowRedirects(boolean b) {
		httpSender.setFollowRedirects(b);
	}

	@IbisDocRef({HTTPSENDERBASE})
	public void setStaleChecking(boolean b) {
		httpSender.setStaleChecking(b);
	}

	@IbisDocRef({HTTPSENDERBASE})
	public void setStaleTimeout(int timeout) {
		httpSender.setStaleTimeout(timeout);
	}

	@IbisDocRef({HTTPSENDERBASE})
	public void setProtocol(String protocol) {
		httpSender.setProtocol(protocol);
	}
}

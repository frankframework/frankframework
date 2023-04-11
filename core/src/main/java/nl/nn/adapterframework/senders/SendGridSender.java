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

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.ReferTo;
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
	public String sendEmail(MailSessionBase mailSession) throws SenderException {
		String result = null;

		Mail mail = null;

		try {
			mail = createEmail((GridMailSession)mailSession);
		} catch (Exception e) {
			throw new SenderException("Exception occured while composing email", e);
		}

		if (mailSession.hasWhitelistedRecipients()) {
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
		} else {
			if (log.isDebugEnabled()) {
				log.debug("No recipients left after whitelisting, mail is not send");
			}
			return "Mail not send, no recipients left after whitelisting";
		}
	}

	@Override
	protected GridMailSession createMailSession() throws SenderException {
		return new GridMailSession();
	}

	/**
	 * Creates sendgrid mail object
	 */
	private Mail createEmail(GridMailSession gridMailSession) throws SenderException, DomBuilderException, IOException {
		Mail mail = new Mail();
		Personalization personalization = gridMailSession.getPersonalization();

		EMail from = gridMailSession.getFrom();
		EMail replyTo = gridMailSession.getReplyto();
		setEmailAddresses(mail, gridMailSession, from, replyTo);

		String subject = gridMailSession.getSubject();
		setSubject(mail, personalization, subject);

		setMessage(mail, gridMailSession);

		List<MailAttachmentStream> attachmentList = gridMailSession.getAttachmentList();
		setAttachments(mail, attachmentList);

		Collection<Node> headers = gridMailSession.getHeaders();
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
	 */
	private void setAttachments(Mail mail, List<MailAttachmentStream> attachmentList) {
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
	private void setMessage(Mail mail, MailSessionBase mailSession) {
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
	 */
	private void setEmailAddresses(Mail mail, GridMailSession gridMailSession, EMail from, EMail replyTo) throws SenderException {
		gridMailSession.setRecipientsOnMessage(new StringBuffer());

		Email fromEmail = new Email();
		if (from != null && from.getAddress() != null && !from.getAddress().isEmpty()) {
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
	@ReferTo(HttpSenderBase.class)
	public void setTimeout(int i) {
		super.setTimeout(i);
		httpSender.setTimeout(i);
	}

	@ReferTo(HttpSenderBase.class)
	public void setMaxConnections(int i) {
		httpSender.setMaxConnections(i);
	}

	@ReferTo(HttpSenderBase.class)
	public void setMaxExecuteRetries(int i) {
		httpSender.setMaxExecuteRetries(i);
	}


	@ReferTo(HttpSenderBase.class)
	public void setProxyHost(String string) {
		httpSender.setProxyHost(string);
	}

	@ReferTo(HttpSenderBase.class)
	public void setProxyPort(int i) {
		httpSender.setProxyPort(i);
	}

	@ReferTo(HttpSenderBase.class)
	public void setProxyAuthAlias(String string) {
		httpSender.setProxyAuthAlias(string);
	}

	@ReferTo(HttpSenderBase.class)
	public void setProxyUsername(String string) {
		httpSender.setProxyUsername(string);
	}

	@Deprecated
	@ConfigurationWarning("Please use \"proxyUsername\" instead")
	public void setProxyUserName(String string) {
		setProxyUsername(string);
	}
	@ReferTo(HttpSenderBase.class)
	public void setProxyPassword(String string) {
		httpSender.setProxyPassword(string);
	}

	@ReferTo(HttpSenderBase.class)
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
	@ReferTo(HttpSenderBase.class)
	public void setKeystore(String keystore) {
		httpSender.setKeystore(keystore);
	}
	@Override
	public String getKeystore() {
		return httpSender.getKeystore();
	}

	@Override
	@ReferTo(HttpSenderBase.class)
	public void setKeystoreType(KeystoreType keystoreType) {
		httpSender.setKeystoreType(keystoreType);
	}
	@Override
	public KeystoreType getKeystoreType() {
		return httpSender.getKeystoreType();
	}

	@Override
	@ReferTo(HttpSenderBase.class)
	public void setKeystoreAuthAlias(String keystoreAuthAlias) {
		httpSender.setKeystoreAuthAlias(keystoreAuthAlias);
	}
	@Override
	public String getKeystoreAuthAlias() {
		return httpSender.getKeystoreAuthAlias();
	}

	@Override
	@ReferTo(HttpSenderBase.class)
	public void setKeystorePassword(String keystorePassword) {
		httpSender.setKeystorePassword(keystorePassword);
	}
	@Override
	public String getKeystorePassword() {
		return httpSender.getKeystorePassword();
	}

	@Override
	@ReferTo(HttpSenderBase.class)
	public void setKeystoreAlias(String keystoreAlias) {
		httpSender.setKeystoreAlias(keystoreAlias);
	}
	@Override
	public String getKeystoreAlias() {
		return httpSender.getKeystoreAlias();
	}

	@Override
	@ReferTo(HttpSenderBase.class)
	public void setKeystoreAliasAuthAlias(String keystoreAliasAuthAlias) {
		httpSender.setKeystoreAliasAuthAlias(keystoreAliasAuthAlias);
	}
	@Override
	public String getKeystoreAliasAuthAlias() {
		return httpSender.getKeystoreAliasAuthAlias();
	}

	@Override
	@ReferTo(HttpSenderBase.class)
	public void setKeystoreAliasPassword(String keystoreAliasPassword) {
		httpSender.setKeystoreAliasPassword(keystoreAliasPassword);
	}
	@Override
	public String getKeystoreAliasPassword() {
		return httpSender.getKeystoreAliasPassword();
	}

	@Override
	@ReferTo(HttpSenderBase.class)
	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		httpSender.setKeyManagerAlgorithm(keyManagerAlgorithm);
	}
	@Override
	public String getKeyManagerAlgorithm() {
		return httpSender.getKeyManagerAlgorithm();
	}

	@Override
	@ReferTo(HttpSenderBase.class)
	public void setTruststore(String truststore) {
		httpSender.setTruststore(truststore);
	}
	@Override
	public String getTruststore() {
		return httpSender.getTruststore();
	}

	@Override
	@ReferTo(HttpSenderBase.class)
	public void setTruststoreType(KeystoreType truststoreType) {
		httpSender.setTruststoreType(truststoreType);
	}
	@Override
	public KeystoreType getTruststoreType() {
		return httpSender.getTruststoreType();
	}


	@Override
	@ReferTo(HttpSenderBase.class)
	public void setTruststoreAuthAlias(String truststoreAuthAlias) {
		httpSender.setTruststoreAuthAlias(truststoreAuthAlias);
	}
	@Override
	public String getTruststoreAuthAlias() {
		return httpSender.getTruststoreAuthAlias();
	}

	@Override
	@ReferTo(HttpSenderBase.class)
	public void setTruststorePassword(String truststorePassword) {
		httpSender.setTruststorePassword(truststorePassword);
	}
	@Override
	public String getTruststorePassword() {
		return httpSender.getTruststorePassword();
	}

	@Override
	@ReferTo(HttpSenderBase.class)
	public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
		httpSender.setTrustManagerAlgorithm(trustManagerAlgorithm);
	}
	@Override
	public String getTrustManagerAlgorithm() {
		return httpSender.getTrustManagerAlgorithm();
	}

	@Override
	@ReferTo(HttpSenderBase.class)
	public void setVerifyHostname(boolean verifyHostname) {
		httpSender.setVerifyHostname(verifyHostname);
	}
	@Override
	public boolean isVerifyHostname() {
		return httpSender.isVerifyHostname();
	}

	@Override
	@ReferTo(HttpSenderBase.class)
	public void setAllowSelfSignedCertificates(boolean testModeNoCertificatorCheck) {
		httpSender.setAllowSelfSignedCertificates(testModeNoCertificatorCheck);
	}
	@Override
	public boolean isAllowSelfSignedCertificates() {
		return httpSender.isAllowSelfSignedCertificates();
	}

	@Override
	@ReferTo(HttpSenderBase.class)
	public void setIgnoreCertificateExpiredException(boolean ignoreCertificateExpiredException) {
		httpSender.setIgnoreCertificateExpiredException(ignoreCertificateExpiredException);
	}
	@Override
	public boolean isIgnoreCertificateExpiredException() {
		return httpSender.isIgnoreCertificateExpiredException();
	}

	@ReferTo(HttpSenderBase.class)
	public void setFollowRedirects(boolean b) {
		httpSender.setFollowRedirects(b);
	}

	@ReferTo(HttpSenderBase.class)
	public void setStaleChecking(boolean b) {
		httpSender.setStaleChecking(b);
	}

	@ReferTo(HttpSenderBase.class)
	public void setStaleTimeout(int timeout) {
		httpSender.setStaleTimeout(timeout);
	}


	@ReferTo(HttpSenderBase.class)
	public void setProtocol(String protocol) {
		httpSender.setProtocol(protocol);
	}

	public class GridMailSession extends MailSessionBase {
		private @Getter @Setter Personalization personalization = null;

		public GridMailSession() throws SenderException {
			super();
			this.setPersonalization(new Personalization());
		}

		@Override
		protected void addRecipientToMessage(EMail recipient) throws SenderException {
			if ("cc".equalsIgnoreCase(recipient.getType())) {
				Email cc = new Email();
				cc.setName(recipient.getName());
				cc.setEmail(recipient.getAddress());
				personalization.addCc(cc);
			} else if ("bcc".equalsIgnoreCase(recipient.getType())) {
				Email bcc = new Email();
				bcc.setName(recipient.getName());
				bcc.setEmail(recipient.getAddress());
				personalization.addBcc(bcc);
			} else if ("to".equalsIgnoreCase(recipient.getType())) {
				Email to = new Email();
				to.setEmail(recipient.getAddress());
				to.setName(recipient.getName());
				personalization.addTo(to);
			} else {
				throw new SenderException("Recipients not found");
			}
		}

		@Override
		protected boolean hasWhitelistedRecipients() {
			List<Email> tos = personalization.getTos();
			List<Email> ccs = personalization.getCcs();
			List<Email> bccs = personalization.getBccs();

			return (tos != null && !tos.isEmpty())
				|| (ccs != null && !ccs.isEmpty())
				|| (bccs != null && !bccs.isEmpty());
		}
	}
}

/*
   Copyright 2019, 2021-2023 WeAreFrank!

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
package org.frankframework.senders;

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

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.SenderException;
import org.frankframework.doc.ReferTo;
import org.frankframework.encryption.HasKeystore;
import org.frankframework.encryption.HasTruststore;
import org.frankframework.encryption.KeystoreType;
import org.frankframework.http.AbstractHttpSession;
import org.frankframework.http.HttpSession;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.util.XmlUtils;

/**
 * Sender that sends a mail via SendGrid v3 (cloud-based SMTP provider).
 *
 * Sample XML file can be found in the path: iaf-core/src/test/resources/emailSamplesXML/emailSample.xml
 * @author alisihab
 */
public class SendGridSender extends AbstractMailSender implements HasKeystore, HasTruststore {

	private SendGrid sendGrid;
	private AbstractHttpSession httpSession;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getCredentialFactory().getPassword())) {
			throw new ConfigurationException("Please provide an API key");
		}
		httpSession = new HttpSession();
		httpSession.configure();
	}

	@Override
	public void start() {
		super.start();
		httpSession.start();

		CloseableHttpClient httpClient = httpSession.getHttpClient();
		if(httpClient == null)
			throw new LifecycleException("no HttpClient found, did it initialize properly?");

		Client client = new Client(httpClient);
		sendGrid = new SendGrid(getCredentialFactory().getPassword(), client);
	}

	@Override
	public void stop() {
		super.stop();
		httpSession.stop();
	}

	@Override
	public void sendEmail(MailSessionBase mailSession) throws SenderException {
		Mail mail;

		try {
			mail = createEmail((GridMailSession)mailSession);
		} catch (Exception e) {
			throw new SenderException("Exception occurred while composing email", e);
		}

		if (mailSession.hasWhitelistedRecipients()) {
			try {
				Request request = new Request();
				request.setMethod(Method.POST);
				request.setEndpoint("mail/send");
				request.setBody(mail.build());
				Response response = sendGrid.api(request);
				log.debug("SendGrid mail result: [{}]", response::getBody);
			} catch (Exception e) {
				throw new SenderException("exception sending mail with subject [" + mail.getSubject() + "]", e);
			}
		} else {
			log.debug("no recipients left after whitelisting, mail is not send");
		}
	}

	@Override
	protected GridMailSession createMailSession() throws SenderException {
		return new GridMailSession();
	}

	/**
	 * Creates sendgrid mail object
	 */
	private Mail createEmail(GridMailSession gridMailSession) throws SenderException {
		Mail mail = new Mail();
		Personalization personalization = gridMailSession.getPersonalization();

		EMail from = gridMailSession.getFrom();
		EMail replyTo = gridMailSession.getReplyTo();
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
	 * @param mail {@link Mail} address to send to
	 * @param personalization {@link Personalization} options of the mail
	 * @param headers Mail headers, as {@link Collection}
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
		gridMailSession.setRecipientsOnMessage(new StringBuilder());

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

	//Properties inherited from HttpSessionBase

	@Override
	@ReferTo(AbstractHttpSession.class)
	public void setTimeout(int i) {
		super.setTimeout(i);
		httpSession.setTimeout(i);
	}

	@ReferTo(AbstractHttpSession.class)
	public void setMaxConnections(int i) {
		httpSession.setMaxConnections(i);
	}

	@ReferTo(AbstractHttpSession.class)
	public void setMaxExecuteRetries(int i) {
		httpSession.setMaxExecuteRetries(i);
	}


	@ReferTo(AbstractHttpSession.class)
	public void setProxyHost(String string) {
		httpSession.setProxyHost(string);
	}

	@ReferTo(AbstractHttpSession.class)
	public void setProxyPort(int i) {
		httpSession.setProxyPort(i);
	}

	@ReferTo(AbstractHttpSession.class)
	public void setProxyAuthAlias(String string) {
		httpSession.setProxyAuthAlias(string);
	}

	@ReferTo(AbstractHttpSession.class)
	public void setProxyUsername(String string) {
		httpSession.setProxyUsername(string);
	}

	@ReferTo(AbstractHttpSession.class)
	public void setProxyPassword(String string) {
		httpSession.setProxyPassword(string);
	}

	@ReferTo(AbstractHttpSession.class)
	public void setProxyRealm(String string) {
		httpSession.setProxyRealm(string);
	}

	@Override
	@ReferTo(AbstractHttpSession.class)
	public void setKeystore(String keystore) {
		httpSession.setKeystore(keystore);
	}

	@Override
	public String getKeystore() {
		return httpSession.getKeystore();
	}

	@Override
	@ReferTo(AbstractHttpSession.class)
	public void setKeystoreType(KeystoreType keystoreType) {
		httpSession.setKeystoreType(keystoreType);
	}

	@Override
	public KeystoreType getKeystoreType() {
		return httpSession.getKeystoreType();
	}

	@Override
	@ReferTo(AbstractHttpSession.class)
	public void setKeystoreAuthAlias(String keystoreAuthAlias) {
		httpSession.setKeystoreAuthAlias(keystoreAuthAlias);
	}

	@Override
	public String getKeystoreAuthAlias() {
		return httpSession.getKeystoreAuthAlias();
	}

	@Override
	@ReferTo(AbstractHttpSession.class)
	public void setKeystorePassword(String keystorePassword) {
		httpSession.setKeystorePassword(keystorePassword);
	}

	@Override
	public String getKeystorePassword() {
		return httpSession.getKeystorePassword();
	}

	@Override
	@ReferTo(AbstractHttpSession.class)
	public void setKeystoreAlias(String keystoreAlias) {
		httpSession.setKeystoreAlias(keystoreAlias);
	}

	@Override
	public String getKeystoreAlias() {
		return httpSession.getKeystoreAlias();
	}

	@Override
	@ReferTo(AbstractHttpSession.class)
	public void setKeystoreAliasAuthAlias(String keystoreAliasAuthAlias) {
		httpSession.setKeystoreAliasAuthAlias(keystoreAliasAuthAlias);
	}

	@Override
	public String getKeystoreAliasAuthAlias() {
		return httpSession.getKeystoreAliasAuthAlias();
	}

	@Override
	@ReferTo(AbstractHttpSession.class)
	public void setKeystoreAliasPassword(String keystoreAliasPassword) {
		httpSession.setKeystoreAliasPassword(keystoreAliasPassword);
	}

	@Override
	public String getKeystoreAliasPassword() {
		return httpSession.getKeystoreAliasPassword();
	}

	@Override
	@ReferTo(AbstractHttpSession.class)
	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		httpSession.setKeyManagerAlgorithm(keyManagerAlgorithm);
	}

	@Override
	public String getKeyManagerAlgorithm() {
		return httpSession.getKeyManagerAlgorithm();
	}

	@Override
	@ReferTo(AbstractHttpSession.class)
	public void setTruststore(String truststore) {
		httpSession.setTruststore(truststore);
	}

	@Override
	public String getTruststore() {
		return httpSession.getTruststore();
	}

	@Override
	@ReferTo(AbstractHttpSession.class)
	public void setTruststoreType(KeystoreType truststoreType) {
		httpSession.setTruststoreType(truststoreType);
	}

	@Override
	public KeystoreType getTruststoreType() {
		return httpSession.getTruststoreType();
	}

	@Override
	@ReferTo(AbstractHttpSession.class)
	public void setTruststoreAuthAlias(String truststoreAuthAlias) {
		httpSession.setTruststoreAuthAlias(truststoreAuthAlias);
	}

	@Override
	public String getTruststoreAuthAlias() {
		return httpSession.getTruststoreAuthAlias();
	}

	@Override
	@ReferTo(AbstractHttpSession.class)
	public void setTruststorePassword(String truststorePassword) {
		httpSession.setTruststorePassword(truststorePassword);
	}

	@Override
	public String getTruststorePassword() {
		return httpSession.getTruststorePassword();
	}

	@Override
	@ReferTo(AbstractHttpSession.class)
	public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
		httpSession.setTrustManagerAlgorithm(trustManagerAlgorithm);
	}
	@Override
	public String getTrustManagerAlgorithm() {
		return httpSession.getTrustManagerAlgorithm();
	}

	@Override
	@ReferTo(AbstractHttpSession.class)
	public void setVerifyHostname(boolean verifyHostname) {
		httpSession.setVerifyHostname(verifyHostname);
	}
	@Override
	public boolean isVerifyHostname() {
		return httpSession.isVerifyHostname();
	}

	@Override
	@ReferTo(AbstractHttpSession.class)
	public void setAllowSelfSignedCertificates(boolean testModeNoCertificatorCheck) {
		httpSession.setAllowSelfSignedCertificates(testModeNoCertificatorCheck);
	}
	@Override
	public boolean isAllowSelfSignedCertificates() {
		return httpSession.isAllowSelfSignedCertificates();
	}

	@Override
	@ReferTo(AbstractHttpSession.class)
	public void setIgnoreCertificateExpiredException(boolean ignoreCertificateExpiredException) {
		httpSession.setIgnoreCertificateExpiredException(ignoreCertificateExpiredException);
	}
	@Override
	public boolean isIgnoreCertificateExpiredException() {
		return httpSession.isIgnoreCertificateExpiredException();
	}

	@ReferTo(AbstractHttpSession.class)
	public void setFollowRedirects(boolean b) {
		httpSession.setFollowRedirects(b);
	}

	@ReferTo(AbstractHttpSession.class)
	public void setStaleChecking(boolean b) {
		httpSession.setStaleChecking(b);
	}

	@ReferTo(AbstractHttpSession.class)
	public void setStaleTimeout(int timeout) {
		httpSession.setStaleTimeout(timeout);
	}


	@ReferTo(AbstractHttpSession.class)
	public void setProtocol(String protocol) {
		httpSession.setProtocol(protocol);
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

/*
   Copyright 2019 Integration Partners

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

import com.sendgrid.Attachments;
import com.sendgrid.Attachments.Builder;
import com.sendgrid.Client;
import com.sendgrid.Content;
import com.sendgrid.Email;
import com.sendgrid.Mail;
import com.sendgrid.Method;
import com.sendgrid.Personalization;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
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
public class SendGridSender extends MailSenderBase {

	private SendGrid sendGrid;
	private HttpSenderBase httpclient;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getCredentialFactory().getPassword())) {
			throw new ConfigurationException("Please provide an API key");
		}
		httpclient = new HttpSender();
		httpclient.configure();
	}

	@Override
	public void open() throws SenderException {
		super.open();
		httpclient.open();

		CloseableHttpClient httpClient = httpclient.getHttpClient();
		if(httpClient == null)
			throw new SenderException("no HttpClient found, did it initialize properly?");

		Client client = new Client(httpClient);
		sendGrid = new SendGrid(getCredentialFactory().getPassword(), client);
	}

	@Override
	public void close() throws SenderException {
		super.close();
		httpclient.close();
	}

	@Override
	public void sendEmail(MailSession mailSession) throws SenderException {
		String result = null;

		Mail mail = null;

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
		EMail replyTo = mailSession.getReplyto();
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
		if (headers != null && headers.size() > 0) {
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
	@IbisDoc({"10", "timeout in ms of obtaining a connection/result. 0 means no timeout", "10000"})
	public void setTimeout(int i) {
		super.setTimeout(i);
		httpclient.setTimeout(i);
	}

	@IbisDoc({"11", "the maximum number of concurrent connections", "10"})
	public void setMaxConnections(int i) {
		httpclient.setMaxConnections(i);
	}

	@IbisDoc({"12", "the maximum number of times it the execution is retried", "1"})
	public void setMaxExecuteRetries(int i) {
		httpclient.setMaxExecuteRetries(i);
	}


	@IbisDoc({"20", "hostname of the proxy", ""})
	public void setProxyHost(String string) {
		httpclient.setProxyHost(string);
	}

	@IbisDoc({"21", "port of the proxy", "80"})
	public void setProxyPort(int i) {
		httpclient.setProxyPort(i);
	}

	@IbisDoc({"22", "alias used to obtain credentials for proxy authentication", ""})
	public void setProxyAuthAlias(String string) {
		httpclient.setProxyAuthAlias(string);
	}

	@IbisDoc({"23", "username used to obtain credentials for proxy authentication", ""})
	public void setProxyUsername(String string) {
		httpclient.setProxyUsername(string);
	}
	@Deprecated
	@ConfigurationWarning("Please use \"proxyUsername\" instead")
	public void setProxyUserName(String string) {
		setProxyUsername(string);
	}
	@IbisDoc({"24", "password used to obtain credentials for proxy authentication", ""})
	public void setProxyPassword(String string) {
		httpclient.setProxyPassword(string);
	}

	@IbisDoc({"35", "realm used for proxy authentication", ""})
	public void setProxyRealm(String string) {
		httpclient.setProxyRealm(string);
	}





	@IbisDoc({"40", "resource url to certificate to be used for authentication", ""})
	public void setCertificate(String string) {
		httpclient.setCertificate(string);
	}

	@IbisDoc({"41", "alias used to obtain truststore password", ""})
	public void setTruststoreAuthAlias(String string) {
		httpclient.setTruststoreAuthAlias(string);
	}

	@IbisDoc({"42", "certificate password", " "})
	public void setCertificatePassword(String string) {
		httpclient.setCertificatePassword(string);
	}

	@IbisDoc({"", "pkcs12"})
	public void setKeystoreType(String string) {
		httpclient.setKeystoreType(string);
	}

	@IbisDoc({"43", "", "pkcs12"})
	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		httpclient.setKeyManagerAlgorithm(keyManagerAlgorithm);
	}

	@IbisDoc({"50", "resource url to truststore to be used for authentication", ""})
	public void setTruststore(String string) {
		httpclient.setTruststore(string);
	}

	@IbisDoc({"51", "alias used to obtain truststore password", ""})
	public void setCertificateAuthAlias(String string) {
		httpclient.setCertificateAuthAlias(string);
	}

	@IbisDoc({"52", "truststore password", " "})
	public void setTruststorePassword(String string) {
		httpclient.setTruststorePassword(string);
	}

	@IbisDoc({"53", "type of truststore", "jks"})
	public void setTruststoreType(String string) {
		httpclient.setTruststoreType(string);
	}

	@IbisDoc({"54", "", " "})
	public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
		httpclient.setTrustManagerAlgorithm(trustManagerAlgorithm);
	}

	@IbisDoc({"55", "when true, the hostname in the certificate will be checked against the actual hostname", "true"})
	public void setVerifyHostname(boolean b) {
		httpclient.setVerifyHostname(b);
	}

	@IbisDoc({"56", "when true, self signed certificates are accepted", "false"})
	public void setAllowSelfSignedCertificates(boolean allowSelfSignedCertificates) {
		httpclient.setAllowSelfSignedCertificates(allowSelfSignedCertificates);
	}

	@IbisDoc({"57", "when true, the certificateexpiredexception is ignored", "false"})
	public void setIgnoreCertificateExpiredException(boolean b) {
		httpclient.setIgnoreCertificateExpiredException(b);
	}

	
	@IbisDoc({"61", "when true, a redirect request will be honoured, e.g. to switch to https", "true"})
	public void setFollowRedirects(boolean b) {
		httpclient.setFollowRedirects(b);
	}

	@IbisDoc({"62", "controls whether connections checked to be stale, i.e. appear open, but are not.", "true"})
	public void setStaleChecking(boolean b) {
		httpclient.setStaleChecking(b);
	}
	
	@IbisDoc({"63", "Used when StaleChecking=true. Timeout when stale connections should be closed.", "5000"})
	public void setStaleTimeout(int timeout) {
		httpclient.setStaleTimeout(timeout);
	}


	@IbisDoc({"67", "Secure socket protocol (such as 'SSL' and 'TLS') to use when a SSLContext object is generated. If empty the protocol 'SSL' is used", "SSL"})
	public void setProtocol(String protocol) {
		httpclient.setProtocol(protocol);
	}
}

package nl.nn.adapterframework.senders;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.activation.DataHandler;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.xerces.impl.dv.util.Base64;
import org.w3c.dom.Element;

import com.sendgrid.Attachments;
import com.sendgrid.Content;
import com.sendgrid.Email;
import com.sendgrid.Mail;
import com.sendgrid.Method;
import com.sendgrid.Personalization;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;

/**
 * Sender that sends a mail via SendGrid v3 (cloud-based SMTP provider).
 * Sample XML file can be found in the path: iaf-core/src/test/resources/emailSamplesXML/emailSample1.xml
 * @author alisihab
 */
public class SendGridSender extends MailSenderBase {

	private String alias;
	private String userName;
	private String password;
	private CredentialFactory cf;

	/**
	 * Configure credentials
	 */
	public void configure() throws ConfigurationException {
		cf = new CredentialFactory(getAlias(), getUserName(), getPassword());
		super.configure();
	}

	@Override
	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc)
			throws SenderException, TimeOutException {
		String result = null;

		SendGrid sendGrid = new SendGrid(cf.getPassword());
		Mail mail = null;
		try {
			mail = createEmail(message, prc);
		} catch (DomBuilderException e1) {
			e1.printStackTrace();
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
			throw new SenderException(getLogPrefix() + "exception sending mail with subject ["
					+ mail.getSubject() + "]", e);
		}

		return correlationID;
	}

	/**
	 * Creates sendgrid mail object
	 * @param input : XML file content
	 * @param prc	
	 * @return 
	 * @throws SenderException
	 * @throws DomBuilderException
	 */
	private Mail createEmail(String input, ParameterResolutionContext prc) throws SenderException,
			DomBuilderException {
		Mail mail = new Mail();
		Personalization personalization = new Personalization();
		MailSession mailSession = extract(input, prc);

		List<EMail> emailList = mailSession.getEmailList();
		EMail from = mailSession.getFrom();
		EMail replyTo = mailSession.getReplyto();
		setEmailAddresses(mail, personalization, emailList, from, replyTo);

		String subject = mailSession.getSubject();
		setSubject(mail, personalization, subject);

		String message = mailSession.getMessage();
		String messageType = mailSession.getMessageType();
		String messageBase64 = mailSession.getMessageBase64();
		setMessage(mail, message, messageType, messageBase64);

		List<Attachment> attachmentList = mailSession.getAttachmentList();
		setAttachments(mail, attachmentList);

		Collection headers = mailSession.getHeaders();
		setHeader(mail, headers);
		mail.addPersonalization(personalization);
		return mail;
	}

	/**
	 * Sets header of mail object if header exists
	 * @param mail 
	 * @param headers 
	 */
	private void setHeader(Mail mail, Collection headers) {
		if (headers != null && headers.size() > 0) {
			Iterator iter = headers.iterator();
			while (iter.hasNext()) {
				Element headerElement = (Element) iter.next();
				String headerName = headerElement.getAttribute("name");
				String headerValue = XmlUtils.getStringValue(headerElement);
				mail.addHeader(headerName, headerValue);
			}
		}
	}

	/**
	 * Adds attachments to mail Object if there is any
	 * @param mail 
	 * @param attachmentList 
	 */
	private void setAttachments(Mail mail, List<Attachment> attachmentList) {
		if (attachmentList != null) {
			Iterator iter = attachmentList.iterator();
			while (iter.hasNext()) {
				Attachment attachmentElement = (Attachment) iter.next();
				Attachments attachment = new Attachments();
				if (attachmentElement.getAttachmentText() instanceof DataHandler) {
					try {
						File file = new File(
								new URL(attachmentElement.getAttachmentURL()).getPath());
						attachmentElement.setAttachmentText(encodeFileToBase64Binary(file));
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
				} else {
					byte[] aTextBytes;
					String text = (String) attachmentElement.getAttachmentText();
					if ("true".equalsIgnoreCase(text)) {
						aTextBytes = decodeBase64ToBytes(text);
					} else {
						aTextBytes = text.getBytes();
					}
					attachmentElement.setAttachmentText(Base64.encode(aTextBytes));
				}
				attachment.setContent((String) attachmentElement.getAttachmentText());
				attachment.setFilename(attachmentElement.getAttachmentName());
				mail.addAttachments(attachment);
			}
		}
	}

	/**
	 * Sets content of email to mail Object
	 * @param mail 
	 * @param message 
	 * @param messageBase64 
	 * @param messageType 
	 */
	private void setMessage(Mail mail, String message, String messageType, String messageBase64) {
		String messageContent = null;
		if (StringUtils.isNotEmpty(message)) {
			Content content = new Content();
			if ("true".equalsIgnoreCase(messageBase64)) {
				messageContent = decodeBase64ToString(message);
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
	 * Sets subject of mail object
	 * @param personalization 
	 * @param mail 
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
	 */
	private void setEmailAddresses(Mail mail, Personalization personalization, List<EMail> list,
			EMail from, EMail replyTo) {
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
			}
		}
		Email fromEmail = new Email();
		if (from.getAddress().isEmpty()) {
			fromEmail.setEmail(from.getAddress());
		} else {
			fromEmail.setEmail(from.getAddress());
			fromEmail.setName(from.getName());
		}
		mail.setFrom(fromEmail);
		Email replyToEmail = new Email();
		replyToEmail.setEmail(replyTo.getAddress());
		replyToEmail.setName(replyTo.getName());
		mail.setReplyTo(replyToEmail);
	}

	/**
	 * Encodes file to base64 
	 * @param file : attachment
	 * @return
	 */
	private static String encodeFileToBase64Binary(File file) {
		String encodedfile = null;
		try {
			FileInputStream fileInputStreamReader = new FileInputStream(file);
			byte[] bytes = new byte[(int) file.length()];
			fileInputStreamReader.read(bytes);
			encodedfile = new String(Base64.encode(bytes));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return encodedfile;
	}

	private byte[] decodeBase64ToBytes(String str) {
		byte[] bytesDecoded = Base64.decode(str);
		return bytesDecoded;
	}

	private String decodeBase64ToString(String str) {
		byte[] bytesDecoded = Base64.decode(str);
		return new String(bytesDecoded);
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}
}

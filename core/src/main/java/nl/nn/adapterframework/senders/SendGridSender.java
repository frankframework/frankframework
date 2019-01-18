package nl.nn.adapterframework.senders;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.xerces.impl.dv.util.Base64;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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
 * 
 * <p>
 * Input XML Schema Definition Language (XSD):<code><pre><table border="0">
 * <tr><td>email</td><td>- recipients</td><td>&nbsp;</td><td>- recipient</td><td>[1..n]</td><td>- {@literal @}type</td><td>[0..1]</td><td>one of {to;cc;bcc}, to by default</td></tr>
 * <tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>- {@literal @}name</td><td>[0..1]</td><td>&nbsp;</td></tr>
 * <tr><td>&nbsp;</td><td>- subject</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>&nbsp;</td><td>- from</td><td>&nbsp;</td><td>- {@literal @}name</td><td>[0..1]</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>&nbsp;</td><td>- message</td><td>[0..1]</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>&nbsp;</td><td>- messageType</td><td>[0..1]</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>one of {text/plain;text/html}, text/plain by default</td></tr>
 * <tr><td>&nbsp;</td><td>- messageBase64</td><td>[0..1]</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>one of {true;false}, false by default</td></tr>
 * <tr><td>&nbsp;</td><td>- replyTo</td><td>[0..1]</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>&nbsp;</td><td>- attachments</td><td>[0..1]</td><td>- attachment</td><td>[0..n]</td><td>- {@literal @}type</td><td>[0..1]</td><td>&nbsp;</td></tr>
 * <tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>- {@literal @}name</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>- {@literal @}url</td><td>[0..1]</td><td>&nbsp;</td></tr>
 * <tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>- {@literal @}base64</td><td>[0..1]</td><td>one of {true;false}, false by default</td></tr>
 * <tr><td>&nbsp;</td><td>- uniqueArguments</td><td>[0..1]</td><td>- uniqueArgument</td><td>[1..n]</td><td>- name</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>- value</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * </table>
 * </pre></code>
 * </p>
 * <p>
 * An example can be found at /test/resources/emailSamplesXML/emailSample1.xml.
 * </p>
 * 
 * <p>
 * <b>Configuration:</b>
 * <table border="1">
 * <tr>
 * <th>attributes</th>
 * <th>description</th>
 * <th>default</th>
 * </tr>
 * <tr>
 * <td>{@link #setPassword(String) password}</td>
 * <td>password of userid on SendGrid</td>
 * <td>&nbsp;</td>
 * </tr>
 * </table>
 * </p>
 * 
 * @author alisihab
 * 
 */
public class SendGridSender extends SenderWithParametersBase {

	/** Password is the apikey itself */
	private String password;

	public void configure() throws ConfigurationException {
		super.configure();
	}

	@Override
	public String sendMessage(String correlationID, String message,
			ParameterResolutionContext prc) throws SenderException,
			TimeOutException {
		String result = null;

		SendGrid sendGrid = new SendGrid(password);

		Mail mail = createEmail(message);

		try {
			Request request = new Request();
			request.setMethod(Method.POST);
			request.setEndpoint("mail/send");
			request.setBody(mail.build());
			Response response = sendGrid.api(request);
			result = response.getBody();
		} catch (Exception e) {
			throw new SenderException(getLogPrefix()
					+ "exception sending mail with subject ["
					+ mail.getSubject() + "]", e);
		}

		return correlationID;
	}

	/**
	 * Parses XML file input to create email
	 * 
	 * @param input
	 *            : XML file content
	 * @return email
	 * @throws SenderException
	 */
	private Mail createEmail(String input) throws SenderException {
		Collection<Node> recipients = null;
		String subject = null;
		Element fromElement = null;
		String message = null;
		String messageType = null;
		String messageBase64 = null;
		String replyTo = null;
		Collection<Node> attachments = null;
		Collection<Node> uniqueArguments = null;

		Mail email = new Mail();
		Element emailElement;

		try {
			emailElement = XmlUtils.buildElement(input);

			Element recipientsElement = XmlUtils.getFirstChildTag(emailElement,
					"recipients");
			recipients = XmlUtils.getChildTags(recipientsElement, "recipient");

			subject = XmlUtils.getChildTagAsString(emailElement, "subject");
			fromElement = XmlUtils.getFirstChildTag(emailElement, "from");
			message = XmlUtils.getChildTagAsString(emailElement, "message");
			messageType = XmlUtils.getChildTagAsString(emailElement,
					"messageType");
			messageBase64 = XmlUtils.getChildTagAsString(emailElement,
					"messageBase64");
			replyTo = XmlUtils.getChildTagAsString(emailElement, "replyTo");

			Element attachmentsElement = XmlUtils.getFirstChildTag(
					emailElement, "attachments");
			attachments = attachmentsElement == null ? null : XmlUtils
					.getChildTags(attachmentsElement, "attachment");

			Element uniqueArgumentsElement = XmlUtils.getFirstChildTag(
					emailElement, "uniqueArguments");
			uniqueArguments = uniqueArgumentsElement == null ? null : XmlUtils
					.getChildTags(uniqueArgumentsElement, "uniqueArgument");

		} catch (DomBuilderException e) {
			throw new SenderException(getLogPrefix() + "exception parsing ["
					+ input + "]", e);
		}

		Iterator<Node> iter = recipients.iterator();
		Personalization personalization = new Personalization();
		while (iter.hasNext()) {
			Element recipientElement = (Element) iter.next();
			String recipient = XmlUtils.getStringValue(recipientElement);
			if (StringUtils.isNotEmpty(recipient)) {
				String typeAttr = recipientElement.getAttribute("type");
				String nameAttr = recipientElement.getAttribute("name");
				if ("cc".equalsIgnoreCase(typeAttr)) {
					Email cc = new Email();
					cc.setName(nameAttr);
					cc.setEmail(recipient);
					personalization.addCc(cc);
				} else if ("bcc".equalsIgnoreCase(typeAttr)) {
					Email bcc = new Email();
					bcc.setName(nameAttr);
					bcc.setEmail(recipient);
					personalization.addBcc(bcc);
				} else {
					Email to = new Email();
					to.setEmail(recipient);
					if (StringUtils.isNotEmpty(nameAttr)) {
						to.setName(nameAttr);
					}
					personalization.addTo(to);
				}
			} else {
				log.debug(getLogPrefix() + "empty recipient found, ignoring");
			}
		}

		if (StringUtils.isNotEmpty(subject)) {
			personalization.setSubject(subject);
			email.setSubject(subject);
		}

		String from = XmlUtils.getStringValue(fromElement);
		if (StringUtils.isNotEmpty(from)) {
			Email fromPerson = new Email();
			fromPerson.setEmail(from);
			String nameAttr = fromElement.getAttribute("name");
			if (StringUtils.isNotEmpty(nameAttr)) {
				fromPerson.setName(nameAttr);
			}
			email.setFrom(fromPerson);
		}

		if (StringUtils.isNotEmpty(message)) {
			Content content = new Content();
			if ("true".equalsIgnoreCase(messageBase64)) {
				message = decodeBase64ToString(message);
			}
			if ("text/html".equalsIgnoreCase(messageType)) {
				content.setType("text/html");
				content.setValue(message);
			} else {
				content.setType("text/plain");
				content.setValue(message);
			}
			email.addContent(content);
		}

		if (StringUtils.isNotEmpty(replyTo)) {
			Email replyToemail = new Email();
			replyToemail.setEmail(replyTo);
			email.setReplyTo(replyToemail);
		}
		getAttachments(email,attachments);
		getUniqueArguments(personalization, uniqueArguments);
		email.addPersonalization(personalization);
		return email;
	}

	private void getUniqueArguments(Personalization personalization,
			Collection<Node> uniqueArguments) {
		Iterator<Node> iter = uniqueArguments.iterator();
		if (uniqueArguments != null) {
			iter = uniqueArguments.iterator();
			if (iter.hasNext()) {
				while (iter.hasNext()) {
					Element uniqueArgumentElement = (Element) iter.next();
					String uniqueArgumentName = XmlUtils.getChildTagAsString(
							uniqueArgumentElement, "name");
					String uniqueArgumentValue = XmlUtils.getChildTagAsString(
							uniqueArgumentElement, "value");
					personalization.addHeader(uniqueArgumentName,
							uniqueArgumentValue);
				}
			}
		}
	}

	private void getAttachments(Mail email, Collection<Node> attachments) throws SenderException {
		Iterator<Node> iter = attachments.iterator();
		if (attachments != null) {
			iter = attachments.iterator();
			while (iter.hasNext()) {
				Element attachmentElement = (Element) iter.next();
				String attachmentText = XmlUtils
						.getStringValue(attachmentElement);
				String attachmentName = attachmentElement.getAttribute("name");
				String attachmentType = attachmentElement.getAttribute("type");
				String attachmentUrl = attachmentElement.getAttribute("url");
				String attachmentBase64 = attachmentElement
						.getAttribute("base64");
				log.debug(getLogPrefix() + "found attachment ["
						+ attachmentName + "] type [" + attachmentType
						+ "] url [" + attachmentUrl + "] contents ["
						+ attachmentText + "] base64 [" + attachmentBase64
						+ "]");

				String aName = null;
				if (StringUtils.isNotEmpty(attachmentType)
						&& !attachmentName.contains(".")) {
					aName = attachmentName + "." + attachmentType;
				} else {
					aName = attachmentName;
				}

				try {
					if (StringUtils.isNotEmpty(attachmentText)) {
						byte[] aTextBytes;
						if ("true".equalsIgnoreCase(attachmentBase64)) {
							aTextBytes = decodeBase64ToBytes(attachmentText);
						} else {
							aTextBytes = attachmentText.getBytes();
						}
						Attachments attachment = new Attachments();
						attachment.setContent(new String(Base64
								.encode(aTextBytes)));
						attachment.setFilename(aName);
						email.addAttachments(attachment);
					} else if (StringUtils.isNotEmpty(attachmentUrl)) {
						URL url = new URL(attachmentUrl);
						InputStream inputStream = url.openStream();
						File file;
						byte[] aText = new byte[(int) (file = new File(
								url.getPath())).length()];
						inputStream.read(aText);
						Attachments attachment = new Attachments();
						attachment.setContent(encodeFileToBase64Binary(file));
						attachment.setFilename(aName);
						email.addAttachments(attachment);
					} else {
						log.debug(getLogPrefix()
								+ "empty attachment found, ignoring");
					}
				} catch (IOException e) {
					throw new SenderException(getLogPrefix()
							+ "exception adding attachment [" + aName + "]", e);
				}
			}
		}
		
	}

	private static String encodeFileToBase64Binary(File file) {
		String encodedfile = null;
		try {
			FileInputStream fileInputStreamReader = new FileInputStream(file);
			byte[] bytes = new byte[(int) file.length()];
			fileInputStreamReader.read(bytes);
			encodedfile = new String(Base64.encode(bytes));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return encodedfile;
	}

	private String decodeBase64ToString(String str) {
		byte[] bytesDecoded = Base64.decode(str);
		return new String(bytesDecoded);
	}

	private byte[] decodeBase64ToBytes(String str) {
		byte[] bytesDecoded = Base64.decode(str);
		return bytesDecoded;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}

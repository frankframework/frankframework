/*
   Copyright 2020 WeAreFrank!

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
package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.xml.SaxElementBuilder;

public class MailFileSystemUtils {
	protected static Logger log = LogUtil.getLogger(MailFileSystemUtils.class);
	
	public static final List<String> specialHeaders = Arrays.asList(new String[] {
				IMailFileSystem.TO_RECEPIENTS_KEY, 
				IMailFileSystem.CC_RECEPIENTS_KEY,
				IMailFileSystem.BCC_RECEPIENTS_KEY,
				IMailFileSystem.FROM_ADDRESS_KEY,
				IMailFileSystem.SENDER_ADDRESS_KEY,
				IMailFileSystem.REPLY_TO_RECEPIENTS_KEY,
				IMailFileSystem.DATETIME_SENT_KEY,
				IMailFileSystem.DATETIME_RECEIVED_KEY,
				IMailFileSystem.BEST_REPLY_ADDRESS_KEY} );

	
	public static <M,A> void addEmailInfoSimple(IMailFileSystem<M,A> fileSystem, M emailMessage, SaxElementBuilder emailXml) throws FileSystemException, SAXException {
		emailXml.addElement("subject", fileSystem.getSubject(emailMessage));
	}

	public static void addPropertyAsHeader(SaxElementBuilder xml, String elementName, Object property) throws SAXException {
		addPropertyAsHeader(xml, elementName, null, null, property);
	}
	
	public static void addPropertyAsHeader(SaxElementBuilder xml, String elementName, String attributeName, String attributeValue, Object property) throws SAXException {
		if (property==null) {
			return;
		}
		if (property instanceof Iterable) {
			for (Object item:(Iterable<?>)property) {
				addPropertyAsHeader(xml, elementName, attributeName, attributeValue, item);
			}
			return;
 		}
		String value;
		if (property instanceof Date) {
			value = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format((Date)property);
		} else {
			value = property.toString();
		}
		if (attributeName!=null) {
			xml.addElement(elementName, attributeName, attributeValue, value);
		} else {
			xml.addElement(elementName, value);
		}
	}
	
	public static void addRecipientProperty(SaxElementBuilder recipientsXml, Map<String,Object> properties, String key) throws SAXException {
		addPropertyAsHeader(recipientsXml,"recipient", "type", key, properties.get(key));
	}
	
	public static <M,A> void addEmailInfo(IMailFileSystem<M,A> fileSystem, M emailMessage, SaxElementBuilder emailXml) throws FileSystemException, SAXException {
		Map<String,Object> properties = fileSystem.getAdditionalFileProperties(emailMessage);
		try (SaxElementBuilder recipientsXml = emailXml.startElement("recipients")) {
			addRecipientProperty(recipientsXml, properties, IMailFileSystem.TO_RECEPIENTS_KEY);
			addRecipientProperty(recipientsXml, properties, IMailFileSystem.CC_RECEPIENTS_KEY);
			addRecipientProperty(recipientsXml, properties, IMailFileSystem.BCC_RECEPIENTS_KEY);
		}
		addPropertyAsHeader(emailXml,IMailFileSystem.FROM_ADDRESS_KEY, properties.get(IMailFileSystem.FROM_ADDRESS_KEY));
		addPropertyAsHeader(emailXml,IMailFileSystem.SENDER_ADDRESS_KEY, properties.get(IMailFileSystem.SENDER_ADDRESS_KEY));
		addPropertyAsHeader(emailXml,IMailFileSystem.REPLY_TO_RECEPIENTS_KEY, properties.get(IMailFileSystem.REPLY_TO_RECEPIENTS_KEY));
		addPropertyAsHeader(emailXml,IMailFileSystem.BEST_REPLY_ADDRESS_KEY, properties.get(IMailFileSystem.BEST_REPLY_ADDRESS_KEY));
		emailXml.addElement("subject", fileSystem.getSubject(emailMessage));
		addPropertyAsHeader(emailXml,IMailFileSystem.DATETIME_SENT_KEY, properties.get(IMailFileSystem.DATETIME_SENT_KEY));
		addPropertyAsHeader(emailXml,IMailFileSystem.DATETIME_RECEIVED_KEY, properties.get(IMailFileSystem.DATETIME_RECEIVED_KEY));
		try {
			emailXml.addElement("message", fileSystem.readFile(emailMessage).asString());
		} catch (IOException e) {
			throw new FileSystemException("Cannot read message body",e);
		}
		try (SaxElementBuilder attachmentsXml = emailXml.startElement("attachments")) {
			
			for (Iterator<A> it = fileSystem.listAttachments(emailMessage); it!=null && it.hasNext();) {
				fileSystem.extractAttachment(it.next(), attachmentsXml);
			}
		} catch (Exception e) {
			throw new FileSystemException("Cannot extract attachment",e);
		}
		try (SaxElementBuilder headersXml = emailXml.startElement("headers")) {
			for (Map.Entry<String,Object> header: properties.entrySet()) {
				if (!specialHeaders.contains(header.getKey())) {
					addPropertyAsHeader(headersXml, "header", "name", header.getKey(), header.getValue());
				}
			}
		}
	}

	public static <M,A> void addAttachmentInfo(IMailFileSystem<M,A> fileSystem, A attachment, SaxElementBuilder attachmentsXml) throws FileSystemException, SAXException {
		try (SaxElementBuilder attachmentXml = attachmentsXml.startElement("attachment")) {
			attachmentXml.addAttribute("name", fileSystem.getAttachmentName(attachment));
			String filename = fileSystem.getAttachmentFileName(attachment);
			if (filename!=null) {
				attachmentXml.addAttribute("filename", filename);
			}
			M emailMessage = fileSystem.getFileFromAttachment(attachment);
			if (emailMessage!=null) {
				fileSystem.extractEmail(emailMessage, attachmentXml);
			}
		}
	}
	

	public static String findBestReplyAddress(Map<String,Object> headers, String replyAddressFields) {
		if (StringUtils.isEmpty(replyAddressFields)) {
			return null;
		}
		String result;
		for(String field:replyAddressFields.split(",")) {
			if (null != (result = getValidAddressFromHeader(field, headers))) {
				return result; 
			}
		}
		return null;
	}

	public static String getValidAddressFromHeader(String key, Map<String,Object> headers) {
		Object item = headers.get(key);
		if (item == null) {
			return null;
		}
		if (item instanceof List) {
			String result;
			for(Object address:(List<?>) item) {
				if (null != (result = getValidAddress(key, address.toString()))) {
					return result; 
				}
			}
			return null;
		}
		return getValidAddress(key, item.toString());
	}
	
	public static String getValidAddress(String type, String address) {
		try {
			if (StringUtils.isEmpty(address)) {
				return null;
			}
			InternetAddress[] addresses = InternetAddress.parseHeader(address, true);
			if (addresses.length==0) {
				return null;
			} 
			StringBuffer result = new StringBuffer();
			for (InternetAddress iaddress: addresses) {
				String personal = iaddress.getPersonal();
				if (personal!=null) {
					iaddress.setPersonal(iaddress.getPersonal().trim());
				}
				if (result.length()!=0) {
					result.append(", ");
				}
				result.append(iaddress.toUnicodeString());
			}
			return result.toString();
		} catch (AddressException | UnsupportedEncodingException e) {
			log.warn("type ["+type+"] address ["+address+"] is invalid: "+e.getMessage());
			return null;
		}
	}
	
}

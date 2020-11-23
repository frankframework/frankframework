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

import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.xml.SaxElementBuilder;

public class MailFileSystemUtils {
	protected static Logger log = LogUtil.getLogger(MailFileSystemUtils.class);

	public static <M,A> void addEmailInfoSimple(IMailFileSystem<M,A> fileSystem, M emailMessage, SaxElementBuilder emailXml) throws FileSystemException, SAXException {
		emailXml.addElement("subject", fileSystem.getSubject(emailMessage));
	}

	public static <M,A> void addEmailInfo(IMailFileSystem<M,A> fileSystem, M emailMessage, SaxElementBuilder emailXml) throws FileSystemException, SAXException {
		try (SaxElementBuilder recipientsXml = emailXml.startElement("recipients")) {
			for(String address:fileSystem.getToRecipients(emailMessage)) {
				recipientsXml.addElement("recipient", "type", "to", address);
			}
			for(String address:fileSystem.getCCRecipients(emailMessage)) {
				recipientsXml.addElement("recipient", "type", "cc", address);
			}
			for(String address:fileSystem.getBCCRecipients(emailMessage)) {
				recipientsXml.addElement("recipient", "type", "bcc", address);
			}
		}
		emailXml.addElement("from", fileSystem.getFrom(emailMessage));
		emailXml.addElement("subject", fileSystem.getSubject(emailMessage));
		emailXml.addElement("message", fileSystem.getMessageBody(emailMessage));
		try (SaxElementBuilder attachmentsXml = emailXml.startElement("attachments")) {
			for (Iterator<A> it = fileSystem.listAttachments(emailMessage); it.hasNext();) {
				fileSystem.extractAttachment(it.next(), attachmentsXml);
			}
		} catch (Exception e) {
			throw new FileSystemException("Cannot extract attachment",e);
		}
		try (SaxElementBuilder headersXml = emailXml.startElement("headers")) {
			Map<String,Object> properties = fileSystem.getAdditionalFileProperties(emailMessage);
			if (properties != null) {
				for (Map.Entry<String,Object> header: properties.entrySet()) {
					if (header.getValue() instanceof List) {
						for(Object value:(List<?>)header.getValue()) {
							headersXml.addElement("header", "name", header.getKey(), (String)value);
						}
					} else {
						headersXml.addElement("header", "name", header.getKey(), (String)header.getValue());
					}
				}
			}
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		emailXml.addElement("dateTimeSent", sdf.format(fileSystem.getDateTimeSent(emailMessage)));
		emailXml.addElement("dateTimeReceived", sdf.format(fileSystem.getDateTimeReceived(emailMessage)));
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
	
}

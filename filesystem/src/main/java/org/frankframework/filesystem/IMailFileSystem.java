/*
   Copyright 2020, 2024 WeAreFrank!

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
package org.frankframework.filesystem;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.xml.sax.SAXException;

import org.frankframework.stream.Message;
import org.frankframework.xml.SaxElementBuilder;

public interface IMailFileSystem<M,A> extends IBasicFileSystem<M> {

	String MAIL_MESSAGE_ID = "Message-ID";
	String RETURN_PATH_HEADER="Return-Path";

	/*
	 * IMailFileSystems should make sure the additionalProperties contain also the following keys:
	 */
	String TO_RECIPIENTS_KEY = "to";
	String CC_RECIPIENTS_KEY = "cc";
	String BCC_RECIPIENTS_KEY = "bcc";
	String FROM_ADDRESS_KEY = "from";     // originator of the message
	String SENDER_ADDRESS_KEY = "sender"; // identifies who submitted the messages, probably on behalf of the 'From'
	String REPLY_TO_RECIPIENTS_KEY = "replyTo";
	String DATETIME_SENT_KEY = "DateTimeSent";         // as Date, or in XML format: yyyy-MM-dd'T'HH:mm:ss.SSSZ
	String DATETIME_RECEIVED_KEY = "DateTimeReceived";

	String BEST_REPLY_ADDRESS_KEY = "bestReplyAddress";
	String REPLY_ADDRESS_FIELDS_DEFAULT= REPLY_TO_RECIPIENTS_KEY +','+FROM_ADDRESS_KEY+','+SENDER_ADDRESS_KEY+','+RETURN_PATH_HEADER;


	String getSubject(M emailMessage) throws FileSystemException;

	Message getMimeContent(M emailMessage) throws FileSystemException;

	void forwardMail(M emailMessage, String destination) throws FileSystemException;

	void extractEmail(M emailMessage, SaxElementBuilder emailXml) throws FileSystemException, SAXException;
	void extractAttachment(A attachment, SaxElementBuilder attachmentsXml) throws FileSystemException, SAXException;

	String getReplyAddressFields();

	Iterator<A> listAttachments(M f) throws FileSystemException;

	String getAttachmentName(A a) throws FileSystemException;

	Message readAttachment(A a) throws FileSystemException, IOException;

	long getAttachmentSize(A a) throws FileSystemException;

	String getAttachmentContentType(A a) throws FileSystemException;

	String getAttachmentFileName(A a) throws FileSystemException;

	M getFileFromAttachment(A a) throws FileSystemException;

	Map<String, Object> getAdditionalAttachmentProperties(A a) throws FileSystemException;
}

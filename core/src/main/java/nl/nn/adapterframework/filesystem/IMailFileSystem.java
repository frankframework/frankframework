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

import org.xml.sax.SAXException;

import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.xml.SaxElementBuilder;

public interface IMailFileSystem<M,A> extends IWithAttachments<M,A> {

	public final String MAIL_MESSAGE_ID = "Message-ID";
	public final String RETURN_PATH_HEADER="Return-Path";

	/*
	 * IMailFileSystems should make sure the additionalProperties contain also the following keys:
	 */
	public final String TO_RECEPIENTS_KEY = "to";
	public final String CC_RECEPIENTS_KEY = "cc";
	public final String BCC_RECEPIENTS_KEY = "bcc";
	public final String FROM_ADDRESS_KEY = "from";     // originator of the message
	public final String SENDER_ADDRESS_KEY = "sender"; // identifies who submitted the messages, probably on behalf of the 'From'
	public final String REPLY_TO_RECEPIENTS_KEY = "replyTo";
	public final String DATETIME_SENT_KEY = "DateTimeSent";         // as Date, or in XML format: yyyy-MM-dd'T'HH:mm:ss.SSSZ
	public final String DATETIME_RECEIVED_KEY = "DateTimeReceived";

	public final String BEST_REPLY_ADDRESS_KEY = "bestReplyAddress";
	public final String REPLY_ADDRESS_FIELDS_DEFAULT=REPLY_TO_RECEPIENTS_KEY+','+FROM_ADDRESS_KEY+','+SENDER_ADDRESS_KEY+','+RETURN_PATH_HEADER;


	public String getSubject(M emailMessage) throws FileSystemException;

	public Message getMimeContent(M emailMessage) throws FileSystemException;

	public void forwardMail(M emailMessage, String destination) throws FileSystemException;

	public void extractEmail(M emailMessage, SaxElementBuilder emailXml) throws FileSystemException, SAXException;
	public void extractAttachment(A attachment, SaxElementBuilder attachmentsXml) throws FileSystemException, SAXException;

	public String getReplyAddressFields();
}

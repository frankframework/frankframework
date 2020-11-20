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

import java.util.Date;

import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.xml.SaxElementBuilder;

public interface IMailFileSystem<M,A> extends IWithAttachments<M,A> {

	public String MAIL_MESSAGE_ID = "Message-ID";
	
	public Iterable<String> getToRecipients(M emailMessage) throws FileSystemException ;
	public Iterable<String> getCCRecipients(M emailMessage) throws FileSystemException;
	public Iterable<String> getBCCRecipients(M emailMessage) throws FileSystemException;
	
	public String getFrom(M emailMessage) throws FileSystemException;
	public String getSender(M emailMessage) throws FileSystemException;
	public String getReplyTo(M emailMessage) throws FileSystemException;

	public String getSubject(M emailMessage) throws FileSystemException;
	public Date getDateTimeSent(M emailMessage) throws FileSystemException;
	public Date getDateTimeReceived(M emailMessage) throws FileSystemException;

	public String getMessageBody(M emailMessage) throws FileSystemException;
	public Message getMimeContent(M emailMessage) throws FileSystemException;

	public void extractEmail(M emailMessage, SaxElementBuilder emailXml) throws FileSystemException;
	public void extractAttachment(A attachment, SaxElementBuilder attachmentsXml) throws FileSystemException;
	
}

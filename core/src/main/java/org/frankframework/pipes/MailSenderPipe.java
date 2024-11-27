/*
   Copyright 2013 Nationale-Nederlanden

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
package org.frankframework.pipes;

import org.frankframework.senders.MailSender;

/**
 * Pipe that sends a mail-message using a {@link MailSender} as its sender.
 * <br/>
 * Sample email.xml:<br/>
 * <pre>{@code
 * 	<email>
 * 	    <recipients>
 * 	        <recipient>***@natned</recipient>
 * 	        <recipient>***@nn.nl</recipient>
 * 	    </recipients>
 * 	    <from>***@nn.nl</from>
 * 	    <subject>this is the subject</subject>
 * 	    <message>dit is de message</message>
 * 	</email>
 * }</pre>
 * <br/>
 * Notice: it must be valid XML. Therefore, especially the message element
 * must be plain text or be wrapped as CDATA.<br/><br/>
 * example:<br/>
 * <pre>{@code
 * <message><![CDATA[<h1>This is a HtmlMessage</h1>]]></message>
 * }</pre>
 * <br/>
 *
 * @author Johan Verrips
 */
public class MailSenderPipe extends MessageSendingPipe {

	public MailSenderPipe() {
		super();
		setSender(new MailSender());
	}
}

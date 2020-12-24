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
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.senders.MailSender;

/**
 * Pipe that sends a mail-message using a {@link MailSender} as its sender.
 * <br/>
 * Sample email.xml:<br/><code><pre>
 *	&lt;email&gt;
 *	    &lt;recipients&gt;
 *	        &lt;recipient&gt;***@natned&lt;/recipient&gt;
 *	        &lt;recipient&gt;***@nn.nl&lt;/recipient&gt;
 *	    &lt;/recipients&gt;
 *	    &lt;from&gt;***@nn.nl&lt;/from&gt;
 *	    &lt;subject&gt;this is the subject&lt;/subject&gt;
 *	    &lt;message&gt;dit is de message&lt;/message&gt;
 *	&lt;/email&gt;
 * </pre></code> <br/>
 * Notice: it must be valid XML. Therefore, especially the message element
 * must be plain text or be wrapped as CDATA.<br/><br/>
 * example:<br/><code><pre>
 * &lt;message&gt;&lt;![CDATA[&lt;h1&gt;This is a HtmlMessage&lt;/h1&gt;]]&gt;&lt;/message&gt;
 * </pre></code><br/>
 *
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ICorrelatedPullingListener listener}</td><td>specification of a listener to listen to for replies, assuming these to arrive quickly!</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default when the message was successfully sent and no listener was specified</td></tr>
 * <tr><td>"timeout"</td><td>no data was received (timeout on listening), if a listener was specified.</td></tr>
 * </table>
 * </p>
 * @author Johan Verrips
 */

public class MailSenderPipe extends MessageSendingPipe {
		
	public MailSenderPipe() {
		super();
		setSender(new MailSender());
	}
}

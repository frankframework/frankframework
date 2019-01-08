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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.ICorrelatedPullingListener;
import nl.nn.adapterframework.jms.JmsSender;

/**
 * Sends a message (the PipeInput) to a Topic or Queue, 
 *  and receives a message from another Topic or Queue after the input message has been sent.
 *
 * If a {@link ICorrelatedPullingListener listener} is specified it waits for a reply with
 * the correct correlationID.
 * </p>
 * <p>The receiving of messages is done with a selector on the JMSCorrelationId
 * on the queue or topic specified. Therefore there no objection to define this
 * receiver on a queue already in use.</p>
 *
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link ICorrelatedPullingListener listener}</td><td>specification of queue to listen to for a reply</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default when a good message was retrieved, or the message was successfully sent and no receiver was specified</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified, and otherwise under same condition as "success"</td></tr>
 * <tr><td>"timeout"</td><td>no data was received (timeout on listening), while a receiver was specified.</td></tr>
 * </table>
 * </p>
 * @author Johan Verrips
 * @deprecated please use GenericMessageSendingPipe with JmsSender (and if necessary JmsListener), that has same functionality
 */

public class JmsCommunicator extends MessageSendingPipe {

public JmsCommunicator() {
	super();
	setSender(new JmsSender());
}

	public void configure() throws ConfigurationException {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = getLogPrefix(null)+"The class ["+getClass().getName()+"] has been deprecated. Please change to ["+GenericMessageSendingPipe.class.getName()+"]";
		configWarnings.add(log, msg);
		super.configure();
	}
	public void setListener(ICorrelatedPullingListener listener) {
		super.setListener(listener);
	}
}

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
package nl.nn.adapterframework.extensions.tibco;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.jms.JmsException;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.jms.MessagingSourceFactory;

import org.apache.commons.lang3.StringUtils;

/**
 * Dedicated sender on Tibco Destinations.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jms.JmsSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setServerUrl(String) serverUrl}</td><td>URL (hostname and port, separated by ':') of Tibco-Server</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDestinationName(String) destinationName}</td><td>name of the JMS destination (queue or topic) to use</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDestinationType(String) destinationType}</td><td>either <code>QUEUE</code> or <code>TOPIC</code></td><td><code>QUEUE</code></td></tr>
 * <tr><td>{@link #setMessageTimeToLive(long) messageTimeToLive}</td><td>the time it takes for the message to expire. If the message is not consumed before, it will be lost. Make sure to set it to a positive value for request/repy type of messages.</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setMessageType(String) messageType}</td><td>value of the JMSType field</td><td>not set by application</td></tr>
 * <tr><td>{@link #setDeliveryMode(String) deliveryMode}</td><td>controls mode that messages are sent with: either 'persistent' or 'non_persistent'</td><td>not set by application</td></tr>
 * <tr><td>{@link #setPriority(int) priority}</td><td>sets the priority that is used to deliver the message. ranges from 0 to 9. Defaults to -1, meaning not set. Effectively the default priority is set by Jms to 4</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAcknowledgeMode(String) acknowledgeMode}</td><td>&nbsp;</td><td>AUTO_ACKNOWLEDGE</td></tr>
 * <tr><td>{@link #setTransacted(boolean) transacted}</td><td>&nbsp;</td><td>false</td></tr>
 * <tr><td>{@link #setReplyToName(String) replyToName}</td><td>Name of the queue the reply is expected on. This value is send in the JmsReplyTo-header with the message.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPersistent(boolean) persistent}</td><td>rather useless attribute, and not the same as delivery mode. You probably want to use that.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSoap(boolean) soap}</td><td>when <code>true</code>, messages sent are put in a SOAP envelope</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setSoapAction(String) soapAction}</td><td>SoapAction string sent as messageproperty</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAuthAlias(String) authAlias}</td><td>alias used to obtain credentials for authentication to JMS server</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td></td>SoapAction<td><i>String</i></td><td>SoapAction. Automatically filled from attribute <code>soapAction</code></td></tr>
 * <tr><td><i>any</i></td><td><i>any</i></td><td>all parameters present are set as messageproperties</td></tr>
 * </table>
 * </p>
 
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public class TibcoSender extends JmsSender {

	private String serverUrl;

	public TibcoSender() {
		super();
		setSoap(true);
	}

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getServerUrl())) {
			throw new ConfigurationException("serverUrl must be specified");
		}
		super.configure();
	}

	@Override
	protected MessagingSourceFactory getMessagingSourceFactory() {
		return new TibcoMessagingSourceFactory(this, isUseTopicFunctions());
	}
	/*
	 * 
	 * Tibco uses serverUrl instead of connectionFactoryName.
	 */
	@Override
	public String getConnectionFactoryName() throws JmsException {
		return getServerUrl();
	}


	public void setServerUrl(String string) {
		serverUrl = string;
	}
	public String getServerUrl() {
		return serverUrl;
	}


}

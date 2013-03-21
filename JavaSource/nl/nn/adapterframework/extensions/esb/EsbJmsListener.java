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
/*
 * $Log: EsbJmsListener.java,v $
 * Revision 1.5  2013-03-06 13:37:31  europe\m168309
 * typo
 *
 * Revision 1.4  2012/09/07 13:15:17  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Messaging related changes:
 * - Use CACHE_CONSUMER by default for ESB RR
 * - Don't use JMSXDeliveryCount to determine whether message has already been processed
 * - Added maxDeliveries
 * - Delay wasn't increased when unable to write to error store (it was reset on every new try)
 * - Don't call session.rollback() when isTransacted() (it was also called in afterMessageProcessed when message was moved to error store)
 * - Some cleaning along the way like making some synchronized statements unnecessary
 * - Made BTM and ActiveMQ work for testing purposes
 *
 * Revision 1.3  2012/02/09 10:40:41  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * bugfix: wrong Destination class was used
 *
 * Revision 1.2  2012/01/05 10:25:13  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.1  2012/01/05 09:59:16  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * initial version
 *
 *
 */
package nl.nn.adapterframework.extensions.esb;

import java.util.Map;

import javax.jms.Destination;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.jms.JmsListener;

/**
 * ESB (Enterprise Service Bus) extension of JmsListener.
 *
 * <p><b>Configuration </b><i>(where deviating from JmsListener)</i><b>:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setMessageProtocol(String) messageProtocol}</td><td>protocol of ESB service to be called. Possible values 
 * <ul>
 *   <li>"FF": Fire & Forget protocol</li>
 *   <li>"RR": Request-Reply protocol</li>
 * </ul></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUseReplyTo(boolean) useReplyTo}</td><td>if messageProtocol=<code>FF</code>: </td><td>false</td></tr>
 * <tr><td>{@link #setForceMessageIdAsCorrelationId(boolean) forceMessageIdAsCorrelationId}</td><td>if messageProtocol=<code>RR</code>: </td><td><code>true</code></td></tr>
 * </table></p>
 * 
 * @author  Peter Leeuwenburgh
 * @version $Id$
 */
public class EsbJmsListener extends JmsListener {
	private final static String REQUEST_REPLY = "RR";
	private final static String FIRE_AND_FORGET = "FF";
	private final static String CACHE_CONSUMER = "CACHE_CONSUMER";

	private String messageProtocol = null;

	public void configure() throws ConfigurationException {
		if (getMessageProtocol() == null) {
			throw new ConfigurationException(getLogPrefix() + "messageProtocol must be set");
		}
		if (!getMessageProtocol().equalsIgnoreCase(REQUEST_REPLY) && !getMessageProtocol().equalsIgnoreCase(FIRE_AND_FORGET)) {
			throw new ConfigurationException(getLogPrefix() + "illegal value for messageProtocol [" + getMessageProtocol() + "], must be '" + REQUEST_REPLY + "' or '" + FIRE_AND_FORGET + "'");
		}
		if (getMessageProtocol().equalsIgnoreCase(REQUEST_REPLY)) {
			setForceMessageIdAsCorrelationId(true);
			if (CACHE_CONSUMER.equals(getCacheMode())) {
				ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
				configWarnings.add(log, "attribute [cacheMode] already has a default value [" + CACHE_CONSUMER + "]");
			}
			setCacheMode("CACHE_CONSUMER");
		} else {
			setUseReplyTo(false);
		}
		super.configure();
	}

	public void afterMessageProcessed(PipeLineResult plr, Object rawMessage, Map threadContext) throws ListenerException {
		super.afterMessageProcessed(plr, rawMessage, threadContext);
		if (getMessageProtocol().equalsIgnoreCase(REQUEST_REPLY)) {
			Destination replyTo = (Destination) threadContext.get("replyTo");
			if (replyTo == null) {
				log.warn("no replyTo address found for messageProtocol [" + getMessageProtocol() + "], response is lost");
			}
		}
	}
	
	public void setMessageProtocol(String string) {
		messageProtocol = string;
	}

	public String getMessageProtocol() {
		return messageProtocol;
	}
}

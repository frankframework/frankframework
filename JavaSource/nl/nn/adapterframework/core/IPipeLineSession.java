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
package nl.nn.adapterframework.core;

import java.security.Principal;
import java.util.Map;


/**
 * The <code>PipeLineSession</code> is an object similar to
 * a <code>session</code> object in a web-application. It stores
 * data, so that the individual <i>pipes</i> may communicate with
 * one another.
 * <p>The object is cleared each time a new message is processed,
 * and the original message (as it arrived on the <code>PipeLine</code>
 * is stored in the key identified by <code>originalMessageKey</code>.
 * The messageId is stored under the key identified by <code>messageId</code>.
 * </p>
 * 
 * @version $Id$
 * @author  Johan Verrips IOS
 * @since   version 3.2.2
 */
public interface IPipeLineSession<K, V> extends Map<K, V> {
	public static final String originalMessageKey="originalMessage";
	public static final String messageIdKey="messageId";
	public static final String businessCorrelationIdKey="cid";
	public static final String technicalCorrelationIdKey="tcid";
	public static final String tsReceivedKey="tsReceived";
	public static final String tsSentKey="tsSent";
	public static final String securityHandlerKey="securityHandler";

	/**
	 * @return the messageId that was passed to the <code>PipeLine</code> which
	 *         should be stored under <code>originalMessageKey</code>
	 */
	public String getMessageId();

	/**
	 * @return the message that was passed to the <code>PipeLine</code> which
	 *         should be stored under <code>originalMessageKey</code>
	 */
	public String getOriginalMessage();

	/*
	 * Sets securitHandler. SecurityHandler can also be set via key in PipeLineSession.
	 */
	public void setSecurityHandler(ISecurityHandler handler);

	public ISecurityHandler getSecurityHandler();

	public boolean isUserInRole(String role);
	
	public Principal getPrincipal();

}

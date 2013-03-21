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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import nl.nn.adapterframework.util.DateUtils;

import org.apache.commons.lang.NotImplementedException;


/**
 * Basic implementation of <code>IPipeLineSession</code>.
 * 
 * @version $Id$
 * @author  Johan Verrips IOS
 * @since   version 3.2.2
 */
public class PipeLineSessionBase extends HashMap implements IPipeLineSession {
	public static final String version="$RCSfile: PipeLineSessionBase.java,v $ $Revision: 1.1 $ $Date: 2012-06-01 10:52:52 $";

	private ISecurityHandler securityHandler = null;

	public PipeLineSessionBase() {
		super();
	}

	public PipeLineSessionBase(int initialCapacity) {
		super(initialCapacity);
	}

	public PipeLineSessionBase(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	public PipeLineSessionBase(Map t) {
		super(t);
	}

	public String getMessageId() {
		return (String) get(messageIdKey);
	}

	public String getOriginalMessage() {
		return (String) get(originalMessageKey);
	}

	/**
	 * Convenience method to set required parameters from listeners
	 * @param map
	 */
	public static void setListenerParameters(Map map, String messageId, String technicalCorrelationId, Date tsReceived, Date tsSent) {
		if (messageId!=null) {
			map.put("id", messageId);
		}
		map.put(technicalCorrelationIdKey, technicalCorrelationId);
		if (tsReceived==null) {
			tsReceived=new Date();
		}
		map.put(tsReceivedKey,DateUtils.format(tsReceived, DateUtils.FORMAT_FULL_GENERIC));
		if (tsSent!=null) {
			map.put(tsSentKey,DateUtils.format(tsSent, DateUtils.FORMAT_FULL_GENERIC));
		}
	}

	public void setSecurityHandler(ISecurityHandler handler) {
		securityHandler = handler;
		put(securityHandlerKey,handler);
	}

	public ISecurityHandler getSecurityHandler() throws NotImplementedException {
		if (securityHandler==null) {
			securityHandler=(ISecurityHandler)get(securityHandlerKey);
			if (securityHandler==null) {
				throw new NotImplementedException("no securityhandler found in PipeLineSession");
			}
		}
		return securityHandler;
	}

	public boolean isUserInRole(String role) throws NotImplementedException {
		ISecurityHandler handler = getSecurityHandler();
		return handler.isUserInRole(role,this);
	}

	public Principal getPrincipal() throws NotImplementedException {
		ISecurityHandler handler = getSecurityHandler();
		return handler.getPrincipal(this);
	}

}

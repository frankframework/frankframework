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
 * $Log: PipeLineSessionBase.java,v $
 * Revision 1.1  2012-06-01 10:52:52  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.15  2011/11/30 13:51:55  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.13  2010/12/13 13:23:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added setSecurityHandler method
 *
 * Revision 1.12  2010/09/07 15:55:13  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 * Revision 1.11  2009/07/03 06:27:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * setListenerParameter: messageId only set if not null (so it's not overridden for local test with testtool)
 *
 * Revision 1.10  2008/02/28 16:17:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added setListenerParameters()
 *
 * Revision 1.9  2006/08/22 06:47:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed IXAEnabled from PipeLineSession
 *
 * Revision 1.8  2005/07/05 10:45:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added entry for securityhandler
 *
 * Revision 1.7  2005/05/31 09:10:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 * Revision 1.6  2005/03/07 11:06:26  Johan Verrips <johan.verrips@ibissource.org>
 * PipeLineSession became a extension of HashMap
 *
 * Revision 1.5  2005/02/10 07:49:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed clearing of pipelinesession a start of pipeline
 *
 * Revision 1.4  2004/03/30 07:29:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.3  2004/03/23 17:51:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added methods for Transaction control
 *
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

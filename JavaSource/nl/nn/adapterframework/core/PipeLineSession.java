/*
 * $Log: PipeLineSession.java,v $
 * Revision 1.9.16.1  2008-04-03 08:10:58  europe\L190409
 * synch from HEAD
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
 * @version Id
 * @author  Johan Verrips IOS
 * @since   version 3.2.2
 */
public class PipeLineSession extends HashMap {
	public static final String version="$RCSfile: PipeLineSession.java,v $ $Revision: 1.9.16.1 $ $Date: 2008-04-03 08:10:58 $";

	public static final String originalMessageKey="originalMessage";
	public static final String messageIdKey="messageId";
	public static final String businessCorrelationIdKey="cid";
	public static final String technicalCorrelationIdKey="tcid";
	public static final String tsReceivedKey="tsReceived";
	public static final String tsSentKey="tsSent";
	public static final String securityHandlerKey="securityHandler";
//	private boolean transacted=false;
	private ISecurityHandler securityHandler = null;

	public PipeLineSession() {
		super();
	}
	public PipeLineSession(int initialCapacity) {
		super(initialCapacity);
	}
	public PipeLineSession(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}
	public PipeLineSession(Map t) {
		super(t);
	}

	
	/**
	 * @return the messageId that was passed to the <code>PipeLine</code>
	 */
	public String getMessageId() {
		return (String) get(messageIdKey);
	}
	/**
	 * @return the message that was passed to the <code>PipeLine</code>
	 */
	public String getOriginalMessage() {
		return (String) get(originalMessageKey);
	}
	/**
	 * This method is exclusively to be called by the <code>PipeLine</code>.
	 * It clears the contents of the session, and stores the message that was
	 * passed to the <code>PipeLine</code> under the key <code>orininalMessageKey</code>
	 * 
	 */
	
	protected void set(String message, String messageId) {
		// clear(); Dat moet niet meer!
		put(originalMessageKey, message);
	    put(messageIdKey, messageId);
	}

	/**
	 * Convenience method to set required parameters from listeners
	 * @param map
	 */
	public static void setListenerParameters(Map map, String messageId, String technicalCorrelationId, Date tsReceived, Date tsSent) {
		map.put("id", messageId);
		map.put(technicalCorrelationIdKey, technicalCorrelationId);
		if (tsReceived==null) {
			tsReceived=new Date();
		}
		map.put(tsReceivedKey,DateUtils.format(tsReceived, DateUtils.FORMAT_FULL_GENERIC));
		if (tsSent!=null) {
			map.put(tsSentKey,DateUtils.format(tsSent, DateUtils.FORMAT_FULL_GENERIC));
		}
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

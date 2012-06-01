/*
 * $Log: IPipeLineSession.java,v $
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
 * @version Id
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

/*
 * $Log: JavaListener.java,v $
 * Revision 1.31  2011-11-30 13:51:54  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:43  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.29  2009/03/26 14:26:45  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added throwException attribute
 *
 * Revision 1.28  2008/08/13 13:42:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused imports
 *
 * Revision 1.27  2007/12/10 10:14:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * assume usertransaction can be obtained
 *
 * Revision 1.26  2007/10/17 09:08:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * commented out unused code
 *
 * Revision 1.25  2007/10/03 08:57:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map
 *
 * Revision 1.24  2007/10/02 09:18:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added physical destination
 *
 * Revision 1.23  2007/08/29 15:10:39  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for dependency checking
 *
 * Revision 1.22  2007/06/07 15:20:46  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved logging
 *
 * Revision 1.21  2007/05/16 11:47:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved javadoc
 *
 * Revision 1.20  2007/05/07 08:34:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * explicit comments on how to avoid dependency on 
 * ibisservicedispatcher.jar on server classpath
 *
 * Revision 1.19  2007/02/12 14:03:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.18  2006/10/13 08:18:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cache UserTransaction at startup
 *
 * Revision 1.17  2006/08/24 12:23:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * call rebind() only if ServiceName not empty
 *
 * Revision 1.16  2006/08/22 06:55:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * simplified implementation
 *
 * Revision 1.15  2006/07/17 09:08:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added asynchronous-option
 *
 * Revision 1.14  2006/06/20 14:22:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added 'local' attribute
 *
 * Revision 1.13  2006/04/12 16:06:21  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added 'isolated' attribute
 *
 * Revision 1.12  2006/03/21 10:20:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed jndiName to serviceName
 *
 * Revision 1.11  2006/03/20 13:52:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * AbsoluteSingleton instead of JNDI
 *
 * Revision 1.10  2006/03/15 14:21:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated JavaDoc
 *
 * Revision 1.9  2006/03/15 14:16:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added authentication possibility used for rebinding proxy to JNDI
 *
 * Revision 1.8  2006/03/08 13:56:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reduced logging fuzz
 *
 * Revision 1.7  2006/02/28 08:46:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved logging
 *
 * Revision 1.6  2006/01/05 14:42:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc and reordered code
 *
 * Revision 1.5  2005/09/26 11:55:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected version string
 *
 * Revision 1.4  2005/09/26 11:54:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * enabeld isolated calls from IbisLocalSender to JavaListener as well as to WebServiceListener
 *
 * Revision 1.3  2004/10/05 09:59:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused code
 *
 * Revision 1.2  2004/08/23 13:10:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated JavaDoc
 *
 * Revision 1.1  2004/08/23 07:38:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed JavaPusher to JavaListener
 *
 * Revision 1.4  2004/08/16 14:10:32  unknown <unknown@ibissource.org>
 * Remove warnings
 *
 * Revision 1.3  2004/08/16 14:09:58  unknown <unknown@ibissource.org>
 * Return returnIfStopped value in case adapter is stopped
 *
 * Revision 1.2  2004/08/13 06:47:26  unknown <unknown@ibissource.org>
 * Allow usage of JavaPusher without JNDI
 *
 * Revision 1.1  2004/08/12 10:58:43  unknown <unknown@ibissource.org>
 * Replaced JavaReceiver by the JavaPusher that is to be used in a GenericPushingReceiver
 *
 * Revision 1.1  2004/04/26 06:21:38  unknown <unknown@ibissource.org>
 * Add java receiver
 *
 */
package nl.nn.adapterframework.receivers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IPushingListener;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.dispatcher.DispatcherException;
import nl.nn.adapterframework.dispatcher.DispatcherManager;
import nl.nn.adapterframework.dispatcher.DispatcherManagerFactory;
import nl.nn.adapterframework.dispatcher.RequestProcessor;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;


/** * 
 * The JavaListener listens to java requests.
 *  
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.receivers.JavaListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the listener as known to the adapter. An {@link nl.nn.adapterframework.pipes.IbisLocalSender IbisLocalSender} refers to this name in its <code>javaListener</code>-attribute.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setServiceName(String) serviceName}</td><td>(optional) name under which the JavaListener registers itself with the RequestDispatcherManager. 
 * An {@link nl.nn.adapterframework.pipes.IbisJavaSender IbisJavaSender} refers to this attribute in its <code>serviceName</code>-attribute.
 * If not empty, the IbisServiceDispatcher.jar must be on the classpath of the server.
 *     <br>N.B. If this java listener is to be only called locally (from within the same Ibis), please leave 
 * 	   this attribute empty, to avoid dependency to an IbisServiceDispatcher.jar on the server classpath.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setIsolated(boolean) isolated}</td><td>when <code>true</code>, the call is made in a separate thread, possibly using a separate transaction. 
 * 		<br>N.B. do not use this attribute, set an appropriate <code>transactionAttribute</code>, like <code>NotSupported</code> or <code>RequiresNew</code> instead</td><td>false</td></tr>
 * <tr><td>{@link #setSynchronous(boolean) synchronous}</td><td> when set <code>false</code>, the request is executed asynchronously. This implies <code>isolated=true</code>. N.B. Be aware that there is no limit on the number of threads generated</td><td>true</td></tr>
 * <tr><td>{@link #setThrowException(boolean) throwException}</td><td>Should the JavaListener throw a ListenerException when it occurs or return an error message</td><td><code>true</code></td></tr>
 * </table>
 * 
 * @author  Gerrit van Brakel
 * @version Id
 */
public class JavaListener implements IPushingListener, RequestProcessor, HasPhysicalDestination {
	public static final String version="$RCSfile: JavaListener.java,v $ $Revision: 1.31 $ $Date: 2011-11-30 13:51:54 $";
	protected Logger log = LogUtil.getLogger(this);
	
	private String name;
	private String serviceName;
	private boolean isolated=false;
	private boolean synchronous=true;
	private boolean opened=false;
	private boolean throwException = true;

	private static Map registeredListeners; 
	private IMessageHandler handler;
	
	public void configure() throws ConfigurationException {
		if (isIsolated()) {
			throw new ConfigurationException("function of attribute 'isolated' is replaced by 'transactionAttribute' on PipeLine"); 
		}
		try {
			if (handler==null) {
				throw new ConfigurationException("handler has not been set");
			}
			if (StringUtils.isEmpty(getName())) {
				throw new ConfigurationException("name has not been set");
			}
		} 
		catch (Exception e){
			throw new ConfigurationException(e);
		}
	}

	public synchronized void open() throws ListenerException {
		try {
			// add myself to local list so that IbisLocalSenders can find me 
			registerListener();
			
//			// display transaction status, to force caching of UserTransaction.
//			// UserTransaction is not found in context if looked up from javalistener.
//			try {
//				String transactionStatus = JtaUtil.displayTransactionStatus();
//				log.debug("transaction status at startup ["+transactionStatus+"]");
//			} catch (Exception e) {
//				log.warn("could not get transaction status at startup",e);
//			}
			
			// add myself to global list so that other applications in this JVM (like Everest Portal) can find me.
			// (performed only if serviceName is not empty
			if (StringUtils.isNotEmpty(getServiceName())) {
				rebind(true);
			}
			opened=true;
		} catch (Exception e) {
			throw new ListenerException("error occured while starting listener [" + getName() + "]", e);
		}
	}

	public synchronized void close() throws ListenerException {
		opened=false;
		try {
			// unregister from global list
			if (StringUtils.isNotEmpty(getServiceName())) {
				rebind(false);
			}
			// do not unregister from local list, leave it to handler to handle this
			// unregisterJavaPusher(getName());		
		}
		catch (Exception e) {
			throw new ListenerException("error occured while stopping listener [" + getName() + "]", e);
		} 
	}


	protected void rebind(boolean add) throws DispatcherException {
		if (StringUtils.isNotEmpty(getServiceName())) {
			DispatcherManager dm = DispatcherManagerFactory.getDispatcherManager();
			RequestProcessor processor = add ? this : null;
			dm.register(getServiceName(), processor); 
		}
	}



	public String processRequest(String correlationId, String message, HashMap context) throws ListenerException {
		if (!isOpen()) {
			throw new ListenerException("JavaListener [" + getName() + "] is not opened");
		}
		if (log.isDebugEnabled()) {
			log.debug("JavaListener [" + getName() + "] processing correlationId [" + correlationId + "]");
		}
		if (throwException) {
			return handler.processRequest(this, correlationId, message, context);
		} else {
			try {
				return handler.processRequest(this, correlationId, message, context);
			} 
			catch (ListenerException e) {
				return handler.formatException(null,correlationId, message,e);
			}
		}
	}

//	public String processRequest(String message) {
//		try {
//			return handler.processRequest(this, message);
//		} 
//		catch (ListenerException e) {
//			return handler.formatException(null,null, message,e);
//		}
//	}

//	public String processRequestNoException(String correlationId, String message) {
//		try {
//			if (log.isDebugEnabled())
//				log.debug("JavaListener [" + getName() + "] processing correlationId [" + correlationId + "]");
//			return handler.processRequest(this, correlationId, message);
//		} 
//		catch (ListenerException e) {
//			return handler.formatException(null,correlationId, message,e);
//		}
//	}



	/**
	 * Register listener so that it can be used by a proxy
	 * @param name
	 * @param receiver
	 */
	private void registerListener() {
		getListeners().put(getName(), this);
	}


	/**
	 * @param name
	 * @return JavaReiver registered under name
	 */
	public static JavaListener getListener(String name) {
		return (JavaListener)getListeners().get(name);
	}

	/**
	 * Get all registered JavaListeners
	 */
	private synchronized static Map getListeners() {
		if (registeredListeners == null) {
			registeredListeners = Collections.synchronizedMap(new HashMap());
		}
		return registeredListeners;
	}
	
	public void setExceptionListener(IbisExceptionListener listener) {
		// do nothing, no exceptions known
	}


	public void afterMessageProcessed(PipeLineResult processResult, Object rawMessage, Map context) throws ListenerException {
		// do nothing
	}



	public String getIdFromRawMessage(Object rawMessage, Map context) throws ListenerException {
		// do nothing
		return null;
	}

	public String getStringFromRawMessage(Object rawMessage, Map context) throws ListenerException {
		return (String)rawMessage;
	}

	public String getPhysicalDestinationName() {
		if (StringUtils.isNotEmpty(getServiceName())) {
			return "external: "+getServiceName();
		} else {
			return "internal: "+getName();
		}
	}

	
	/**
	 * The <code>toString()</code> method retrieves its value
	 * by reflection.
	 * @see org.apache.commons.lang.builder.ToStringBuilder#reflectionToString
	 *
	 **/
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
	

	public void setHandler(IMessageHandler handler) {
		this.handler = handler;
	}
	public IMessageHandler getHandler() {
		return handler;
	}


	/**
	 * @return the name under which the java receiver registers the java proxy in JNDI
	 */

	public void setServiceName(String jndiName) {
		this.serviceName = jndiName;
	}
	public String getServiceName() {
		return serviceName;
	}


	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}

	public void setLocal(String name) {
		throw new RuntimeException("do not set attribute 'local=true', just leave serviceName empty!");
	}
	
	public void setIsolated(boolean b) {
		isolated = b;
	}
	public boolean isIsolated() {
		return isolated;
	}
	
	public void setSynchronous(boolean b) {
		synchronous = b;
	}
	public boolean isSynchronous() {
		return synchronous;
	}

	public synchronized boolean isOpen() {
		return opened;
	}

	public void setThrowException(boolean throwException) {
		this.throwException = throwException;
	}
	public boolean isThrowException() {
		return throwException;
	}
}
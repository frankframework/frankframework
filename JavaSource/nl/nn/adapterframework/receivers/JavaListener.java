/*
 * $Log: JavaListener.java,v $
 * Revision 1.7  2006-02-28 08:46:23  europe\L190409
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

import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IPushingListener;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.jms.JNDIBase;
import nl.nn.adapterframework.jms.JmsRealm;

/** * 
 * The JavaListener listens to java requests.
 *  
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.receivers.JavaListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the listener as known to the adapter. An {@link nl.nn.adapterframework.pipes.IbisLocalSender IbisLocalSender} refers to this name in its <code>javaListener</code>-attribute.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJndiName(String) jndiName}</td><td>(optional) name under which the java receiver registers the java proxy in JNDI</td><td>&nbsp;</td></tr>
 * </table>
 * @author  JDekker
 * @version Id
 */
public class JavaListener implements IPushingListener {
	public static final String version="$RCSfile: JavaListener.java,v $ $Revision: 1.7 $ $Date: 2006-02-28 08:46:23 $";
	protected Logger log = Logger.getLogger(this.getClass());
	
	private String name;
	private String jndiName;

	private static Map registeredListeners; 
	private IMessageHandler handler;
	private JNDIBase jndiBase = new JNDIBase();        	
	
	
	public void configure() throws ConfigurationException {
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

	public void open() throws ListenerException {
		// add myself to list so that proxy can find me
		registerListener(getName(), this);
		try {
			if (getJndiName() != null) {
				Context context = getContext();
				Object currentJndiObject=null;
				try {
					currentJndiObject = context.lookup(jndiName);
				} catch (NamingException e) {
					log.debug("error occured while retrieving currentJndiObject", e);
				}		
				if (currentJndiObject!=null) {
					log.info("rebinding proxy under ["+getJndiName()+"], previous object was a ["+currentJndiObject.getClass().getName()+"]");
				} else {
					log.info("binding proxy under ["+getJndiName()+"]");
				}
				context.rebind(getJndiName(), new JavaProxy(this));
			}
				
		} 
		catch (NamingException e) {
			log.error("error occured while starting listener [" + getName() + "]", e);
		}		
	}

	public void close() throws ListenerException {
		if (getJndiName() != null) {
			try {
					getContext().unbind(jndiName);
					closeContext();
				}
			catch (NamingException e) {
				log.error("error occured while stopping listener [" + getName() + "]", e);
			}
		} 
		// do not unregister, leave it to handler to handle this
		// unregisterJavaPusher(getName());		
	}


	/**
	 * Register listener so that it can be used by a proxy
	 * @param name
	 * @param receiver
	 */
	private static void registerListener(String name, JavaListener listener) {
		getListeners().put(name, listener);
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


	public void afterMessageProcessed(PipeLineResult processResult, Object rawMessage, HashMap context) throws ListenerException {
		// do nothing
	}



	public String getIdFromRawMessage(Object rawMessage, HashMap context) throws ListenerException {
		// do nothing
		return null;
	}

	public String getStringFromRawMessage(Object rawMessage, HashMap context) throws ListenerException {
		return (String)rawMessage;
	}

	/**
	 * @param message
	 * @return result of processing
	 */
	public String processRequest(String message) {
		try {
			return handler.processRequest(this, message);
		} 
		catch (ListenerException e) {
			return handler.formatException(null,null, message,e);
		}
	}

	/**
	 * @param correlationId
	 * @param message
	 * @return result of processing
	 */
	public String processRequest(String correlationId, String message) {
		try {
			if (log.isDebugEnabled())
				log.debug("JavaListener [" + getName() + "] processing [" + correlationId + "]");
			return handler.processRequest(this, correlationId, message);
		} 
		catch (ListenerException e) {
			return handler.formatException(null,correlationId, message,e);
		}
	}

	public String processRequest(String correlationId, String message, HashMap context) throws ListenerException {
		if (log.isDebugEnabled()) {
			log.debug("JavaListener [" + getName() + "] processing [" + correlationId + "]");
		}
		return handler.processRequest(this, correlationId, message, context);
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
	
	private Context getContext() throws NamingException {
		return jndiBase.getContext();
	}
	
	private void closeContext() throws NamingException {
		jndiBase.closeContext();
	}


	public void setHandler(IMessageHandler handler) {
		this.handler = handler;
	}
	public IMessageHandler getHandler() {
		return handler;
	}


	public void setJmsRealm(String jmsRealmName){
		JmsRealm.copyRealm(jndiBase, jmsRealmName);
	}

	/**
	 * @return the name under which the java receiver registers the java proxy in JNDI
	 */

	public void setJndiName(String jndiName) {
		this.jndiName = jndiName;
	}
	public String getJndiName() {
		return jndiName;
	}


	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}


}
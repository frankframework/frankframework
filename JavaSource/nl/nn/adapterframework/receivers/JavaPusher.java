/*
 * $Log: JavaPusher.java,v $
 * Revision 1.2  2004-08-13 06:47:26  a1909356#db2admin
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

/**
 * @author JDekker
 * @version Id
 * 
 * The java receiver listens to java requests. 
 */
public class JavaPusher implements IPushingListener {
	private static Map registeredJavaPushers; 
	public static final String version="$Id: JavaPusher.java,v 1.2 2004-08-13 06:47:26 a1909356#db2admin Exp $";
	protected Logger log = Logger.getLogger(this.getClass());;
	private String name;
	private String jndiName;
	private IMessageHandler handler;
	private JNDIBase jndiBase;        	
	
	/** 
	 * default constructor
	 */
	public JavaPusher() {
		this.jndiBase = new JNDIBase();
	}
	
	/**
	 * @return the name under which the java receiver registers the java proxy in JNDI
	 */
	public String getJndiName() {
		return jndiName;
	}

	/**
	 * @param jndiName
	 */
	public void setJndiName(String jndiName) {
		this.jndiName = jndiName;
	}

	/**
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param string
	 */
	public void setName(String string) {
		name = string;
	}

	/**
	 * @return
	 */
	public IMessageHandler getHandler() {
		return handler;
	}

	/**
	 * Register receiver so that it can be used by a proxy
	 * @param name
	 * @param receiver
	 */
	private static void registerJavaPusher(String name, JavaPusher receiver) {
		getJavaPushers().put(name, receiver);
	}

	/**
	 * Unregister recevier, so that it can't be used by proxies
	 * @param name
	 */
	private static void unregisterJavaPusher(String name) {
		getJavaPushers().remove(name);
	}

	/**
	 * @param name
	 * @return JavaReiver registered under name
	 */
	public static JavaPusher getJavaPusher(String name) {
		return (JavaPusher)getJavaPushers().get(name);
	}

	/**
	 * Get all registered JavaReceivers
	 * @return
	 */
	private synchronized static Map getJavaPushers() {
		if (registeredJavaPushers == null) {
			registeredJavaPushers = Collections.synchronizedMap(new HashMap());
		}
		return registeredJavaPushers;
	}
	
	/* (non-Javadoc)
	 * @see nl.nn.adapterframework.core.IPushingListener#setExceptionListener(nl.nn.adapterframework.core.IbisExceptionListener)
	 */
	public void setExceptionListener(IbisExceptionListener listener) {
		// do nothing, no exceptions known
	}

	/* (non-Javadoc)
	 * @see nl.nn.adapterframework.core.IPushingListener#setHandler(nl.nn.adapterframework.core.IMessageHandler)
	 */
	public void setHandler(IMessageHandler handler) {
		this.handler = handler;
	}

	/* (non-Javadoc)
	 * @see nl.nn.adapterframework.core.IListener#afterMessageProcessed(nl.nn.adapterframework.core.PipeLineResult, java.lang.Object, java.util.HashMap)
	 */
	public void afterMessageProcessed(PipeLineResult processResult, Object rawMessage, HashMap context) throws ListenerException {
		// do nothing
	}

	/* (non-Javadoc)
	 * @see nl.nn.adapterframework.core.IListener#close()
	 */
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
		unregisterJavaPusher(getName());		
	}

	/* (non-Javadoc)
	 * @see nl.nn.adapterframework.core.IListener#configure()
	 */
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

	/* (non-Javadoc)
	 * @see nl.nn.adapterframework.core.IListener#getIdFromRawMessage(java.lang.Object, java.util.HashMap)
	 */
	public String getIdFromRawMessage(Object rawMessage, HashMap context) throws ListenerException {
		// do nothing
		return null;
	}

	/* (non-Javadoc)
	 * @see nl.nn.adapterframework.core.IListener#getStringFromRawMessage(java.lang.Object, java.util.HashMap)
	 */
	public String getStringFromRawMessage(Object rawMessage, HashMap context) throws ListenerException {
		return (String)rawMessage;
	}

	/* (non-Javadoc)
	 * @see nl.nn.adapterframework.core.IListener#open()
	 */
	public void open() throws ListenerException {
		// add myself to list so that proxy can find me
		registerJavaPusher(getName(), this);
		try {
			if (getJndiName() != null)
				getContext().rebind(jndiName, new JavaProxy(this));
		} 
		catch (NamingException e) {
			log.error("error occured while starting listener [" + getName() + "]", e);
		}		
	}

	/**
	 * @param message
	 * @return result of processing
	 */
	public String processRequest(String message) {
		String result;
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
		String result;
		try {
			if (log.isDebugEnabled())
				log.debug("javareceiver " + getName() + " processing [" + correlationId + "]");
			return handler.processRequest(this, correlationId, message);
		} 
		catch (ListenerException e) {
			return handler.formatException(null,correlationId, message,e);
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
	
	/**
	 * @return
	 * @throws NamingException
	 */
	private Context getContext() throws NamingException {
		return jndiBase.getContext();
	}
	
	/**
	 * @throws NamingException
	 */
	private void closeContext() throws NamingException {
		jndiBase.closeContext();
	}

	/**
	 * @param jmsRealmName
	 */
	public void setJmsRealm(String jmsRealmName){
		JmsRealm.copyRealm(jndiBase, jmsRealmName);
	}
}
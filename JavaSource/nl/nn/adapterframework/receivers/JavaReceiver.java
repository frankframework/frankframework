/*
 * $Log: JavaReceiver.java,v $
 * Revision 1.1  2004-04-26 06:21:38  a1909356#db2admin
 * Add java receiver
 *
 */
package nl.nn.adapterframework.receivers;

import javax.naming.Context;
import javax.naming.NamingException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.jms.JNDIBase;
import nl.nn.adapterframework.util.RunStateEnum;

/**
 * @author JDekker
 * @version Id
 * 
 * The java receiver listens to java requests. 
 */
public class JavaReceiver extends JNDIBase implements IReceiver, ServiceClient {
	public static final String version="$Id: JavaReceiver.java,v 1.1 2004-04-26 06:21:38 a1909356#db2admin Exp $";
	private ServiceListener serviceListener;
	private String jndiName;
	
	/** 
	 * default constructor
	 */
	public JavaReceiver() {
		serviceListener = new ServiceListener();
	}
	
	/* 
	 * @see nl.nn.adapterframework.core.IManagable#startRunning()
	 */
	public void startRunning() {
		try {
			getContext().rebind(jndiName, new JavaProxy(getName()));
			serviceListener.startRunning();
		} 
		catch (NamingException e) {
			log.error("error occured while starting listener [" + getName() + "]", e);
		}		
	}

	/* 
	 * @see nl.nn.adapterframework.core.IManagable#stopRunning()
	 */
	public void stopRunning() {
		try {
			getContext().unbind(jndiName);
		} 
		catch (NamingException e) {
			log.error("error occured while stopping listener [" + getName() + "]", e);
		}		
		serviceListener.stopRunning();
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

	/* 
	 * @see nl.nn.adapterframework.receivers.ServiceClient#processRequest(java.lang.String, java.lang.String)
	 */
	public String processRequest(String correlationId, String message) {
		return serviceListener.processRequest(correlationId, message);
	}

	/* 
	 * @see nl.nn.adapterframework.receivers.ServiceClient#processRequest(java.lang.String)
	 */
	public String processRequest(String message) {
		return serviceListener.processRequest(message);
	}

	/* 
	 * @see nl.nn.adapterframework.core.IReceiver#configure()
	 */
	public void configure() throws ConfigurationException {
		serviceListener.configure();
	}

	/* 
	 * @see nl.nn.adapterframework.core.IReceiver#getMessagesReceived()
	 */
	public long getMessagesReceived() {
		return serviceListener.getMessagesReceived();
	}

	/* 
	 * @see nl.nn.adapterframework.core.IReceiver#setAdapter(nl.nn.adapterframework.core.IAdapter)
	 */
	public void setAdapter(IAdapter adapter) {
		serviceListener.setAdapter(adapter);

	}

	/* 
	 * @see nl.nn.adapterframework.core.IReceiver#waitForRunState(nl.nn.adapterframework.util.RunStateEnum)
	 */
	public void waitForRunState(RunStateEnum requestedRunState) throws InterruptedException {
		serviceListener.waitForRunState(requestedRunState);
	}

	/* 
	 * @see nl.nn.adapterframework.core.IManagable#getRunState()
	 */
	public RunStateEnum getRunState() {
		return serviceListener.getRunState();
	}

	/* 
	 * @see nl.nn.adapterframework.core.INamedObject#getName()
	 */
	public String getName() {
		return serviceListener.getName();
	}

	/* 
	 * @see nl.nn.adapterframework.core.INamedObject#setName(java.lang.String)
	 */
	public void setName(String name) {
		serviceListener.setName(name);
	}

}

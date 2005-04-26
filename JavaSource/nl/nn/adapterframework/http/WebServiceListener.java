/*
 * $Log: WebServiceListener.java,v $
 * Revision 1.4  2005-04-26 09:26:52  L190409
 * added serviceNamespaceURI attribute
 *
 * Revision 1.3  2004/09/09 14:49:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused variable declarations
 *
 * Revision 1.2  2004/08/23 13:08:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated JavaDoc
 *
 * Revision 1.1  2004/08/23 07:13:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed WebServiceMessagePusher to WebServiceListener
 *
 * Revision 1.2  2004/08/09 13:54:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ServiceClient to MessageHandler
 *
 * Revision 1.1  2004/07/15 07:40:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of http package
 *
 * Revision 1.2  2004/06/30 12:34:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added (dummy) setter for exceptionlistener
 *
 * Revision 1.1  2004/06/22 12:12:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of MessagePushers and PushingReceivers
 *
 */
package nl.nn.adapterframework.http;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IPushingListener;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.receivers.ServiceClient;
import nl.nn.adapterframework.receivers.ServiceDispatcher;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Implementation of a {@link IPushingListener} that enables a {@link nl.nn.adapterframework.receivers.GenericReceiver}
 * to receive messages as a web-service.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.http.WebServiceListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the listener as known to the adapter</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setServiceNamespaceURI(String) serviceNamespaceURI}</td><td>namespace of the service that is provided by the adapter of this listener</td><td>&nbsp;</td></tr>
 * </table>
 * @version Id
 * @author Gerrit van Brakel 
 */
public class WebServiceListener  implements IPushingListener, ServiceClient, Serializable {
	public static final String version="$Id: WebServiceListener.java,v 1.4 2005-04-26 09:26:52 L190409 Exp $";
	protected Logger log = Logger.getLogger(this.getClass());;

	private IMessageHandler handler;        	
	private String name;
	private String serviceNamespaceURI;

	/**
	 * initialize listener and register <code>this</code> to the JNDI
	 */
	public void configure() throws ConfigurationException {
		try {
			if (handler==null) {
				throw new ConfigurationException("handler has not been set");
			}
			if (StringUtils.isEmpty(getServiceNamespaceURI())) {
				log.debug("registering listener ["+name+"] with ServiceDispatcher");
				ServiceDispatcher.getInstance().registerServiceClient(name, this);
			} else {
				log.debug("registering listener ["+name+"] with ServiceDispatcher by serviceNamespaceURI ["+getServiceNamespaceURI()+"]");
				ServiceDispatcher.getInstance().registerServiceClient(getServiceNamespaceURI(), this);
			}
		} catch (Exception e){
			throw new ConfigurationException(e);
		}
	}

	public void open() {
		// do nothing special
	}
	public void close() {
		// do nothing special
	}


	public String getIdFromRawMessage(Object rawMessage, HashMap threadContext)  {
		return null;
	}
	public String getStringFromRawMessage(Object rawMessage, HashMap threadContext) {
		return (String) rawMessage;
	}
	public void afterMessageProcessed(PipeLineResult processResult, Object rawMessage, HashMap threadContext) throws ListenerException {
	}



	public String processRequest(String message) {
		try {
			return handler.processRequest(this, message);
		} catch (ListenerException e) {
			return handler.formatException(null,null, message,e);
		}
	}

	public String processRequest(String correlationId, String message) {
		try {
			log.debug("WebServiceListener processing ["+correlationId+"]");
			return handler.processRequest(this, correlationId, message);
		} catch (ListenerException e) {
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
	 * Returns the name of the Listener. 
	 */
	public String getName() {
		return name;
	}
	/**
	 * Sets the name of the Listener. 
	 */
	public void setName(String name) {
		this.name=name;
	}

	public void setHandler(IMessageHandler handler) {
		this.handler=handler;
	}

	public void setExceptionListener(IbisExceptionListener listener) {
		// do nothing, no exceptions known
	}

	public String getServiceNamespaceURI() {
		return serviceNamespaceURI;
	}
	public void setServiceNamespaceURI(String string) {
		serviceNamespaceURI = string;
	}

}

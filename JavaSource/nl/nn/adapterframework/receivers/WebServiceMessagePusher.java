/*
 * $Log: WebServiceMessagePusher.java,v $
 * Revision 1.1  2004-06-22 12:12:52  L190409
 * introduction of MessagePushers and PushingReceivers
 *
 */
package nl.nn.adapterframework.receivers;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IMessagePusher;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

import java.io.Serializable;

/**
 * Implementation of a {@link nl.nn.adapterframework.core.IMessagePusher pushing listener},
 * that enables a <code>PushingReceiverBase</code> to receive messages by generic services or by web-services.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.receivers.WebServiceMessagePusher</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the listener as known to the adapter</td><td>&nbsp;</td></tr>
 * </table>
 * @version Id
 * @author Gerrit van Brakel 
 */
public class WebServiceMessagePusher  implements IMessagePusher, ServiceClient, Serializable {
	public static final String version="$Id: WebServiceMessagePusher.java,v 1.1 2004-06-22 12:12:52 L190409 Exp $";
	protected Logger log = Logger.getLogger(this.getClass());;

	private ServiceClient handler;        	
	private String name;

	/**
	 * initialize listener and register <code>this</code> to the JNDI
	 */
	public void configure() throws ConfigurationException {
		try {
		    log.debug("registering listener ["+name+"] with ServiceDispatcher");
	        ServiceDispatcher.getInstance().registerServiceClient(name, this);
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

	public String processRequest(String message) {
		return handler.processRequest(message);
	}

	public String processRequest(String correlationId, String message) {
		return handler.processRequest(correlationId, message);
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

	public void setHandler(ServiceClient handler) {
		this.handler=handler;
	}


}

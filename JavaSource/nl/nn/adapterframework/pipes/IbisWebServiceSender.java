/*
 * $Log: IbisWebServiceSender.java,v $
 * Revision 1.1  2004-06-01 13:53:22  L190409
 * eerste versie
 *
 */
package nl.nn.adapterframework.pipes;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.soap.SOAPException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.receivers.ServiceDispatcher_ServiceProxy;
import nl.nn.adapterframework.util.AppConstants;

/**
 * Posts a message to another IBIS-adapter as a WebService.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.receivers.JmsMessageReceiver</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setIbisHost(String) ibisHost}</td><td>name (or ipaddress) and optinally port of the host where the ibis to be called is running</td><td>localhost</td></tr>
 * <tr><td>{@link #setIbisInstance(String) ibisInstance}</td><td>name of the ibis instance to be called</td><td>name of the current instance</td></tr>
 * <tr><td>{@link #setServiceListenerName(String) serviceListenerName}</td><td>Name of the receiver that should be called</td><td>"serviceListener"</td></tr>
 * </table>
 * </p>
 * @version Id
 *
 * @author Gerrit van Brakel
 */
public class IbisWebServiceSender implements ISender {
	public static final String version = "$Id: IbisWebServiceSender.java,v 1.1 2004-06-01 13:53:22 L190409 Exp $";

	private String name;
	private String ibisHost = "localhost";
	private String ibisInstance = null;
	private String serviceListenerName = "serviceListener";
	private ServiceDispatcher_ServiceProxy proxy;
	
	public void configure() throws ConfigurationException {
		if (ibisInstance==null) {
			ibisInstance=AppConstants.getInstance().getResolvedProperty("instance.name");
		}
		try {
			proxy = new ServiceDispatcher_ServiceProxy();
			proxy.setEndPoint(new URL(getEndPoint()));
		} catch (MalformedURLException e) {
			throw new ConfigurationException("IbisWebServiceSender cannot find URL from ["+getEndPoint()+"]", e);
		}
	}

	public void open() throws SenderException {
	}

	public void close() throws SenderException {
	}

	public boolean isSynchronous() {
		return true;
	}

	public String sendMessage(String correlationID, String message)
		throws SenderException, TimeOutException {
		try {
			return proxy.dispatchRequest(getServiceListenerName(),correlationID,message);
		} catch (SOAPException e) {
			throw new SenderException("exception sending message ["+message+"] with correlationID ["+correlationID+"] to endPoint["+getEndPoint()+"]",e);
		}
	}

	protected String getEndPoint() {
		return "http://"+getIbisHost()+"/"+getIbisInstance()+"/servlet/rpcrouter";
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name=name;
	}
	
	public String getIbisHost() {
		return ibisHost;
	}
	public void setIbisHost(String ibisHost) {
		this.ibisHost=ibisHost;
	}
	
	public String getIbisInstance() {
		return ibisInstance;
	}
	public void setIbisInstance(String ibisInstance) {
		this.ibisInstance=ibisInstance;
	}

	public String getServiceListenerName() {
		return serviceListenerName;
	}
	public void setServiceListenerName(String serviceListenerName) {
		this.serviceListenerName=serviceListenerName;
	}

}

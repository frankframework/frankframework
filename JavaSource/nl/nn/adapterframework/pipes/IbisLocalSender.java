/*
 * $Log: IbisLocalSender.java,v $
 * Revision 1.1  2004-08-09 13:50:57  L190409
 * first version
 *
 */
package nl.nn.adapterframework.pipes;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.receivers.ServiceDispatcher;

/**
 * Posts a message to another IBIS-adapter in the same JVM.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.pipes.IbisLocalSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setServiceName(String) serviceName}</td><td>Name of the receiver that should be called</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 *
 * @author Gerrit van Brakel
 * @since 4.2
 */
public class IbisLocalSender implements ISender {
	public static final String version="$Id: IbisLocalSender.java,v 1.1 2004-08-09 13:50:57 L190409 Exp $";
	
	private String name;
	private String serviceName;

	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getServiceName())) {
			throw new ConfigurationException("no serviceName specified");
		}
	}

	public void open() {
	}

	public void close() {
	}

	public boolean isSynchronous() {
		return true;
	}

	public String sendMessage(String correlationID, String message) {
		return ServiceDispatcher.getInstance().dispatchRequest(getServiceName(), correlationID, message);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name=name;
	}

	/**
	 * @return name of the service under which the JavaReceiver is registered
	 */
	public String getServiceName() {
		return serviceName;
	}

	/**
	 * @param serviceName under which the JavaReceiver is registered
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

}

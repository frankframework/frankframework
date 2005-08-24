/*
 * $Log: IbisLocalSender.java,v $
 * Revision 1.2  2005-08-24 15:53:28  europe\L190409
 * improved error message for configuration exception
 *
 * Revision 1.1  2004/08/09 13:50:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
 * <tr><td>{@link #setServiceName(String) serviceName}</td><td>Name of the WebServiceListener or JavaListener that should be called</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 *
 * @author Gerrit van Brakel
 * @since  4.2
 */
public class IbisLocalSender implements ISender {
	public static final String version="$RCSfile: IbisLocalSender.java,v $ $Revision: 1.2 $ $Date: 2005-08-24 15:53:28 $";
	
	private String name;
	private String serviceName;

	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getServiceName())) {
			throw new ConfigurationException("IbisLocalSender ["+getName()+"] has no serviceName specified");
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


	public void setName(String name) {
		this.name=name;
	}
	public String getName() {
		return name;
	}

	/**
	 * @param serviceName under which the JavaListener or WebServiceListener is registered
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	public String getServiceName() {
		return serviceName;
	}

}

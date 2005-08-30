/*
 * $Log: IbisLocalSender.java,v $
 * Revision 1.3  2005-08-30 16:02:28  europe\L190409
 * made parameterized version
 *
 * Revision 1.2  2005/08/24 15:53:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved error message for configuration exception
 *
 * Revision 1.1  2004/08/09 13:50:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.receivers.ServiceDispatcher;

import org.apache.commons.lang.StringUtils;

import java.util.HashMap;


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
 * Any parameters are copied to the PipeLineSession of the service called.
 *
 * @author Gerrit van Brakel
 * @since  4.2
 */
public class IbisLocalSender extends SenderWithParametersBase {
	public static final String version="$RCSfile: IbisLocalSender.java,v $ $Revision: 1.3 $ $Date: 2005-08-30 16:02:28 $";
	
	private String name;
	private String serviceName;

	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getServiceName())) {
			throw new ConfigurationException(getLogPrefix()+"has no serviceName specified");
		}
	}

	public void open() {
	}

	public void close() {
	}

	public boolean isSynchronous() {
		return true;
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		HashMap context = null;
		if (paramList!=null) {
			try {
				context = prc.getValueMap(paramList);
			} catch (ParameterException e) {
				throw new SenderException(getLogPrefix()+"exception evaluating parameters",e);
			}
		}
		try {
			return ServiceDispatcher.getInstance().dispatchRequestWithExceptions(getServiceName(), correlationID, message, context);
		} catch (ListenerException e) {
			throw new SenderException(getLogPrefix()+"exception calling service ["+getServiceName()+"]",e);
		}
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

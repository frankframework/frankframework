/*
 * $Log: IbisJavaSender.java,v $
 * Revision 1.2  2007-05-16 11:46:09  europe\L190409
 * improved javadoc
 *
 * Revision 1.1  2006/03/21 10:18:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.dispatcher.DispatcherManager;
import nl.nn.adapterframework.dispatcher.DispatcherManagerFactory;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

import org.apache.commons.lang.StringUtils;

import java.util.HashMap;

/**
 * Posts a message to another IBIS-adapter in the same JVM.
 *
 * An IbisJavaSender makes a call to a Receiver a {@link nl.nn.adapterframework.receivers.JavaListener JavaListener}
 * or any other application in the same JVM that has registered a <code>RequestProcessor</code> with the IbisServiceDispatcher. 
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.pipes.IbisLocalSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setServiceName(String) serviceName}</td><td>serviceName of the 
 * {@link nl.nn.adapterframework.receivers.JavaListener JavaListener} that should be called</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * Any parameters are copied to the PipeLineSession of the service called.
 * 
 * <h4>configuring IbisJavaSender and JavaListener</h4>
 * <ul>
 *   <li>Define a GenericMessageSendingPipe with an IbisJavaSender</li>
 *   <li>Set the attribute <code>serviceName</code> to <i>yourExternalServiceName</i></li>
 * </ul>
 * In the Adapter to be called:
 * <ul>
 *   <li>Define a Receiver with a JavaListener</li>
 *   <li>Set the attribute <code>serviceName</code> to <i>yourExternalServiceName</i></li>
 * </ul>
 * N.B. Please make sure that the IbisServiceDispatcher-1.1.jar is present on the class path of the server.
 *
 * @author  Gerrit van Brakel
 * @since   4.4.5
 * @version Id
 */
public class IbisJavaSender extends SenderWithParametersBase {
	public static final String version="$RCSfile: IbisJavaSender.java,v $ $Revision: 1.2 $ $Date: 2007-05-16 11:46:09 $";
	
	private String name;
	private String serviceName;
//	private boolean isolated=false;



	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getServiceName())) {
			throw new ConfigurationException(getLogPrefix()+"must specify serviceName");
		}
	}


	public boolean isSynchronous() {
		return true;
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		HashMap context = null;
		try {
			if (paramList!=null) {
				context = prc.getValueMap(paramList);
			}			
			DispatcherManager dm = DispatcherManagerFactory.getDispatcherManager();
			return dm.processRequest(getServiceName(),correlationID, message, context);
		} catch (ParameterException e) {
			throw new SenderException(getLogPrefix()+"exception evaluating parameters",e);
		} catch (Exception e) {
			throw new SenderException(getLogPrefix()+"exception processing message using request processor ["+getServiceName()+"]",e);
		}
	}


	public void setName(String name) {
		this.name=name;
	}
	public String getName() {
		return name;
	}


	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String string) {
		serviceName = string;
	}

}

/*
 * $Log: IbisJavaSender.java,v $
 * Revision 1.4  2010-02-19 13:45:27  m00f069
 * - Added support for (sender) stubbing by debugger
 * - Added reply listener and reply sender to debugger
 * - Use IbisDebuggerDummy by default
 * - Enabling/disabling debugger handled by debugger instead of log level
 * - Renamed messageId to correlationId in debugger interface
 *
 * Revision 1.3  2009/12/04 18:23:34  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added ibisDebugger.senderAbort and ibisDebugger.pipeRollback
 *
 * Revision 1.2  2009/11/18 17:28:04  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added senders to IbisDebugger
 *
 * Revision 1.1  2008/08/06 16:36:39  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved from pipes to senders package
 *
 */
package nl.nn.adapterframework.senders;

import java.util.HashMap;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.dispatcher.DispatcherManager;
import nl.nn.adapterframework.dispatcher.DispatcherManagerFactory;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.lang.StringUtils;

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
 * <tr><td>{@link #setReturnedSessionKeys(String) returnedSessionKeys}</td><td>comma separated list of keys of session variables that should be returned to caller, for correct results as well as for erronous results. (Only for listeners that support it, like JavaListener)</td><td>&nbsp;</td></tr>
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
public class IbisJavaSender extends SenderWithParametersBase implements HasPhysicalDestination {
	
	private String name;
	private String serviceName;
	private String returnedSessionKeys=null;

	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getServiceName())) {
			throw new ConfigurationException(getLogPrefix()+"must specify serviceName");
		}
	}

	public String getPhysicalDestinationName() {
		return "JavaListener "+getServiceName();
	}

	public boolean isSynchronous() {
		return true;
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		message = ibisDebugger.senderInput(this, correlationID, message);
		String result = null;
		try {
			if (!ibisDebugger.stubSender(this, correlationID)) {
				HashMap context = null;
				try {
					if (paramList!=null) {
						context = prc.getValueMap(paramList);
					} else {
						context=new HashMap();			
					}
					DispatcherManager dm = DispatcherManagerFactory.getDispatcherManager();
					result = dm.processRequest(getServiceName(),correlationID, message, context);
				} catch (ParameterException e) {
					throw new SenderException(getLogPrefix()+"exception evaluating parameters",e);
				} catch (Exception e) {
					throw new SenderException(getLogPrefix()+"exception processing message using request processor ["+getServiceName()+"]",e);
				} finally {
					if (log.isDebugEnabled() && StringUtils.isNotEmpty(getReturnedSessionKeys())) {
						log.debug("returning values of session keys ["+getReturnedSessionKeys()+"]");
					}
					if (prc!=null) {
						Misc.copyContext(getReturnedSessionKeys(),context, prc.getSession());
					}
				}
			}
		} catch(Throwable throwable) {
			throwable = ibisDebugger.senderAbort(this, correlationID, throwable);
			throwSenderOrTimeOutException(throwable);
		}
		return ibisDebugger.senderOutput(this, correlationID, result);
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

	public void setReturnedSessionKeys(String string) {
		returnedSessionKeys = string;
	}
	public String getReturnedSessionKeys() {
		return returnedSessionKeys;
	}

}

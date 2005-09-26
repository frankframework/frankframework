/*
 * $Log: IbisLocalSender.java,v $
 * Revision 1.5  2005-09-26 11:54:05  europe\L190409
 * enabeld isolated calls from IbisLocalSender to JavaListener as well as to WebServiceListener
 *
 * Revision 1.4  2005/09/07 15:36:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added attribute "isolated", to enable sub-transactions
 *
 * Revision 1.3  2005/08/30 16:02:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
import nl.nn.adapterframework.receivers.JavaListener;
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
 * <tr><td>{@link #setServiceName(String) serviceName}</td><td>Name of the WebServiceListener that should be called</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJavaListener(String) javaListener}</td><td>Name of the JavaListener that should be called</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setIsolated(String) isolated}</td><td>when <code>true</code>, the call is made in a separate thread, possibly using separate transaction</td><td>false</td></tr>
 * </table>
 * </p>
 * Any parameters are copied to the PipeLineSession of the service called.
 *
 * @author Gerrit van Brakel
 * @since  4.2
 */
public class IbisLocalSender extends SenderWithParametersBase {
	public static final String version="$RCSfile: IbisLocalSender.java,v $ $Revision: 1.5 $ $Date: 2005-09-26 11:54:05 $";
	
	private String name;
	private String serviceName;
	private String javaListener;
	private boolean isolated=false;



	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getServiceName()) && StringUtils.isEmpty(getJavaListener())) {
			throw new ConfigurationException(getLogPrefix()+"has no serviceName or javaListener specified");
		}
		if (StringUtils.isNotEmpty(getServiceName()) && StringUtils.isNotEmpty(getJavaListener())) {
			throw new ConfigurationException(getLogPrefix()+"serviceName and javaListener cannot be specified both");
		}
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
		if (StringUtils.isNotEmpty(getServiceName())) {
			try {
				if (isIsolated()) {
					log.debug(getLogPrefix()+"calling service ["+getServiceName()+"] in separate Thread");
					return IsolatedServiceCaller.callServiceIsolated(getServiceName(), correlationID, message, context, false);
				} else {
					log.debug(getLogPrefix()+"calling service ["+getServiceName()+"] in same Thread");
					return ServiceDispatcher.getInstance().dispatchRequestWithExceptions(getServiceName(), correlationID, message, context);
				}
			} catch (ListenerException e) {
				throw new SenderException(getLogPrefix()+"exception calling service ["+getServiceName()+"]",e);
			}
		}  else {
			try {
				if (isIsolated()) {
					log.debug(getLogPrefix()+"calling JavaListener ["+getJavaListener()+"] in separate Thread");
					return IsolatedServiceCaller.callServiceIsolated(getJavaListener(), correlationID, message, context, true);
				} else {
					log.debug(getLogPrefix()+"calling JavaListener ["+getJavaListener()+"] in same Thread");
					return JavaListener.getListener(getJavaListener()).processRequest(correlationID,message,context);
				}
			} catch (ListenerException e) {
				throw new SenderException(getLogPrefix()+"exception calling JavaListener ["+getJavaListener()+"]",e);
			}
		}
	}


	public void setName(String name) {
		this.name=name;
	}
	public String getName() {
		return name;
	}

	/**
	 * serviceName under which the JavaListener or WebServiceListener is registered.
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	public String getServiceName() {
		return serviceName;
	}


	/**
	 * when <code>true</code>, the call is made in a separate thread, possibly using separate transaction. 
	 */
	public void setIsolated(boolean b) {
		isolated = b;
	}
	public boolean isIsolated() {
		return isolated;
	}


	public void setJavaListener(String string) {
		javaListener = string;
	}
	public String getJavaListener() {
		return javaListener;
	}


}

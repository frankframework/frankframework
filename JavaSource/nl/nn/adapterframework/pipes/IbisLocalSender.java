/*
 * $Log: IbisLocalSender.java,v $
 * Revision 1.11  2006-08-22 06:51:23  europe\L190409
 * corrected javadoc
 * adapted code calling IsolatedServiceCaller
 *
 * Revision 1.10  2006/07/17 09:03:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * force isolated=true if synchronous=false
 *
 * Revision 1.9  2006/07/14 10:04:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added asynchronous-option, by setting synchronous=false
 *
 * Revision 1.8  2005/12/28 08:37:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.7  2005/10/18 07:02:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * better exception handling
 *
 * Revision 1.6  2005/09/28 14:15:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added super.configure()
 *
 * Revision 1.5  2005/09/26 11:54:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
 * Posts a message to another IBIS-adapter in the same IBIS instance.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.pipes.IbisLocalSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setServiceName(String) serviceName}</td><td>Name of the WebServiceListener that should be called</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJavaListener(String) javaListener}</td><td>Name of the JavaListener that should be called</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setIsolated(boolean) isolated}</td><td>when <code>true</code>, the call is made in a separate thread, possibly using separate transaction</td><td>false</td></tr>
 * <tr><td>{@link #setSynchronous(boolean) synchronous}</td><td> when set <code>false</code>, the call is made asynchronously. This implies <code>isolated=true</code></td><td>true</td></tr>
 * </table>
 * </p>
 * Any parameters are copied to the PipeLineSession of the service called.
 *
 * @author Gerrit van Brakel
 * @since  4.2
 */
public class IbisLocalSender extends SenderWithParametersBase {
	public static final String version="$RCSfile: IbisLocalSender.java,v $ $Revision: 1.11 $ $Date: 2006-08-22 06:51:23 $";
	
	private String name;
	private String serviceName;
	private String javaListener;
	private boolean isolated=false;
	private boolean synchronous=true;



	public void configure() throws ConfigurationException {
		super.configure();
		if (!isSynchronous()) {
			setIsolated(true);
		}
		if (StringUtils.isEmpty(getServiceName()) && StringUtils.isEmpty(getJavaListener())) {
			throw new ConfigurationException(getLogPrefix()+"has no serviceName or javaListener specified");
		}
		if (StringUtils.isNotEmpty(getServiceName()) && StringUtils.isNotEmpty(getJavaListener())) {
			throw new ConfigurationException(getLogPrefix()+"serviceName and javaListener cannot be specified both");
		}
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
					if (isSynchronous()) {
						log.debug(getLogPrefix()+"calling service ["+getServiceName()+"] in separate Thread");
						return IsolatedServiceCaller.callServiceIsolated(getServiceName(), correlationID, message, context, false, false);
					} else {
						log.debug(getLogPrefix()+"calling service ["+getServiceName()+"] in asynchronously");
						IsolatedServiceCaller.callServiceAsynchronous(getServiceName(), correlationID, message, context, false, false);
						return message;
					}
				} else {
					log.debug(getLogPrefix()+"calling service ["+getServiceName()+"] in same Thread");
					return ServiceDispatcher.getInstance().dispatchRequestWithExceptions(getServiceName(), correlationID, message, context);
				}
			} catch (ListenerException e) {
				throw new SenderException(getLogPrefix()+"exception calling service ["+getServiceName()+"]",e);
			}
		}  else {
			try {
				JavaListener listener= JavaListener.getListener(getJavaListener());
				if (listener==null) {
					throw new SenderException("could not find JavaListener ["+getJavaListener()+"]");
				}
				if (isIsolated()) {
					if (isSynchronous()) {
						log.debug(getLogPrefix()+"calling JavaListener ["+getJavaListener()+"] in separate Thread");
						return IsolatedServiceCaller.callServiceIsolated(getJavaListener(), correlationID, message, context, true, false);
					} else {
						log.debug(getLogPrefix()+"calling JavaListener ["+getJavaListener()+"] in asynchronously");
						IsolatedServiceCaller.callServiceAsynchronous(getJavaListener(), correlationID, message, context, true, false);
						return message;
					}
				} else {
					log.debug(getLogPrefix()+"calling JavaListener ["+getJavaListener()+"] in same Thread");
					return listener.processRequest(correlationID,message,context);
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


	public void setSynchronous(boolean b) {
		synchronous = b;
	}
	public boolean isSynchronous() {
		return synchronous;
	}

}

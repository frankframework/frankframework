/*
 * $Log: IbisLocalSender.java,v $
 * Revision 1.2  2009-11-18 17:28:03  m00f069
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
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.debug.IbisDebugger;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.pipes.IsolatedServiceCaller;
import nl.nn.adapterframework.receivers.JavaListener;
import nl.nn.adapterframework.receivers.ServiceDispatcher;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.lang.StringUtils;

/**
 * Posts a message to another IBIS-adapter in the same IBIS instance.
 * 
 * An IbisLocalSender makes a call to a Receiver with either a {@link nl.nn.adapterframework.http.WebServiceListener WebServiceListener}
 * or a {@link nl.nn.adapterframework.receivers.JavaListener JavaListener}. 
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.pipes.IbisLocalSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setServiceName(String) serviceName}</td><td>Name of the {@link nl.nn.adapterframework.http.WebServiceListener WebServiceListener} that should be called</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJavaListener(String) javaListener}</td><td>Name of the {@link nl.nn.adapterframework.receivers.JavaListener JavaListener} that should be called</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setIsolated(boolean) isolated}</td><td>when <code>true</code>, the call is made in a separate thread, possibly using separate transaction</td><td>false</td></tr>
 * <tr><td>{@link #setCheckDependency(boolean) checkDependency}</td><td>when <code>true</code>, the sender waits upon open until the called {@link nl.nn.adapterframework.receivers.JavaListener JavaListener} is opened</td><td>true</td></tr>
 * <tr><td>{@link #setDependencyTimeOut(int) dependencyTimeOut}</td><td>maximum time (in seconds) the sender waits for the listener to start</td><td>60 s</td></tr>
 * <tr><td>{@link #setSynchronous(boolean) synchronous}</td><td> when set <code>false</code>, the call is made asynchronously. This implies <code>isolated=true</code></td><td>true</td></tr>
 * <tr><td>{@link #setReturnedSessionKeys(String) returnedSessionKeys}</td><td>comma separated list of keys of session variables that should be returned to caller, 
 *         for correct results as well as for erronous results. (Only for listeners that support it, like JavaListener)<br/>
 *         N.B. To get this working, the attribute returnedSessionKeys must also be set on the corresponding Receiver</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * Any parameters are copied to the PipeLineSession of the service called.
 * 
 * <h3>Configuration of the Adapter to be called</h3>
 * A call to another Adapter in the same IBIS instance is preferably made using the combination
 * of an IbisLocalSender and a {@link nl.nn.adapterframework.receivers.JavaListener JavaListener}. If, 
 * however, a Receiver with a {@link nl.nn.adapterframework.http.WebServiceListener WebServiceListener} is already present, that can be used in some cases, too.
 *  
 * <h4>configuring IbisLocalSender and JavaListener</h4>
 * <ul>
 *   <li>Define a GenericMessageSendingPipe with an IbisLocalSender</li>
 *   <li>Set the attribute <code>javaListener</code> to <i>yourServiceName</i></li>
 *   <li>Do not set the attribute <code>serviceName</code></li>
 * </ul>
 * In the Adapter to be called:
 * <ul>
 *   <li>Define a Receiver with a JavaListener</li>
 *   <li>Set the attribute <code>name</code> to <i>yourServiceName</i></li>
 *   <li>Do not set the attribute <code>serviceName</code>, except if the service is to be called also
 *       from applications other than this IBIS-instance</li>
 * </ul>
 * 
 * <h4>configuring IbisLocalSender and WebServiceListener</h4>
 * 
 * <ul>
 *   <li>Define a GenericMessageSendingPipe with an IbisLocalSender</li>
 *   <li>Set the attribute <code>serviceName</code> to <i>yourIbisWebServiceName</i></li>
 *   <li>Do not set the attribute <code>javaListener</code></li>
 * </ul>
 * In the Adapter to be called:
 * <ul>
 *   <li>Define a Receiver with a WebServiceListener</li>
 *   <li>Set the attribute <code>name</code> to <i>yourIbisWebServiceName</i></li>
 * </ul>
 *
 * @author Gerrit van Brakel
 * @since  4.2
 */
public class IbisLocalSender extends SenderWithParametersBase implements HasPhysicalDestination {
	
	private String name;
	private String serviceName;
	private String javaListener;
	private boolean isolated=false;
	private boolean synchronous=true;
	private boolean checkDependency=true;
	private int dependencyTimeOut=60;
	private String returnedSessionKeys=null;
	private IbisDebugger ibisDebugger;

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

	public void open() throws SenderException {
		super.open();
		if (StringUtils.isNotEmpty(getJavaListener()) && isCheckDependency()) {
			boolean listenerOpened=false;
			int loops=getDependencyTimeOut();
			while (!listenerOpened && loops>0) {
				JavaListener listener= JavaListener.getListener(getJavaListener());
				if (listener!=null) {
					listenerOpened=listener.isOpen();
				}
				if (!listenerOpened) {
					loops--;
					try {
						log.debug("waiting for JavaListener ["+getJavaListener()+"] to open");
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						throw new SenderException(e);
					}
				}
			}
		}
	}


	public String getPhysicalDestinationName() {
		if (StringUtils.isNotEmpty(getServiceName())) {
			return "WebServiceListener "+getServiceName();
		} else {
			return "JavaListener "+getJavaListener();
		}
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		if (log.isDebugEnabled() && ibisDebugger!=null) {
			message = ibisDebugger.senderInput(this, correlationID, message);
		}
		String result = null;
		HashMap context = null;
		if (paramList!=null) {
			try {
				context = prc.getValueMap(paramList);
			} catch (ParameterException e) {
				throw new SenderException(getLogPrefix()+"exception evaluating parameters",e);
			}
		} else {
			if (StringUtils.isNotEmpty(getReturnedSessionKeys())) {
				context = new HashMap();
			}
		}
		if (StringUtils.isNotEmpty(getServiceName())) {
			try {
				if (isIsolated()) {
					if (isSynchronous()) {
						log.debug(getLogPrefix()+"calling service ["+getServiceName()+"] in separate Thread");
						result = IsolatedServiceCaller.callServiceIsolated(getServiceName(), correlationID, message, context, false, ibisDebugger);
					} else {
						log.debug(getLogPrefix()+"calling service ["+getServiceName()+"] in asynchronously");
						IsolatedServiceCaller.callServiceAsynchronous(getServiceName(), correlationID, message, context, false, ibisDebugger);
						result = message;
					}
				} else {
					log.debug(getLogPrefix()+"calling service ["+getServiceName()+"] in same Thread");
					result = ServiceDispatcher.getInstance().dispatchRequestWithExceptions(getServiceName(), correlationID, message, context);
				}
			} catch (ListenerException e) {
				throw new SenderException(getLogPrefix()+"exception calling service ["+getServiceName()+"]",e);
			} finally {
				if (log.isDebugEnabled() && StringUtils.isNotEmpty(getReturnedSessionKeys())) {
					log.debug("returning values of session keys ["+getReturnedSessionKeys()+"]");
				}
				if (prc!=null) {
					Misc.copyContext(getReturnedSessionKeys(),context, prc.getSession());
				}
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
						result = IsolatedServiceCaller.callServiceIsolated(getJavaListener(), correlationID, message, context, true, ibisDebugger);
					} else {
						log.debug(getLogPrefix()+"calling JavaListener ["+getJavaListener()+"] in asynchronously");
						IsolatedServiceCaller.callServiceAsynchronous(getJavaListener(), correlationID, message, context, true, ibisDebugger);
						result = message;
					}
				} else {
					log.debug(getLogPrefix()+"calling JavaListener ["+getJavaListener()+"] in same Thread");
					result = listener.processRequest(correlationID,message,context);
				}
			} catch (ListenerException e) {
				throw new SenderException(getLogPrefix()+"exception calling JavaListener ["+getJavaListener()+"]",e);
			} finally {
				if (log.isDebugEnabled() && StringUtils.isNotEmpty(getReturnedSessionKeys())) {
					log.debug("returning values of session keys ["+getReturnedSessionKeys()+"]");
				}
				if (prc!=null) {
					Misc.copyContext(getReturnedSessionKeys(),context, prc.getSession());
				}
			}
		}
		if (log.isDebugEnabled() && ibisDebugger!=null) {
			result = ibisDebugger.senderOutput(this, correlationID, result);
		}
		return result;
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


	public void setCheckDependency(boolean b) {
		checkDependency = b;
	}
	public boolean isCheckDependency() {
		return checkDependency;
	}


	public void setDependencyTimeOut(int i) {
		dependencyTimeOut = i;
	}
	public int getDependencyTimeOut() {
		return dependencyTimeOut;
	}

	public void setReturnedSessionKeys(String string) {
		returnedSessionKeys = string;
	}
	public String getReturnedSessionKeys() {
		return returnedSessionKeys;
	}

	public void setIbisDebugger(IbisDebugger ibisDebugger) {
		this.ibisDebugger = ibisDebugger;
	}

}

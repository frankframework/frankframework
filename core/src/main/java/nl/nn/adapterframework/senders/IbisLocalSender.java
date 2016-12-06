/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.senders;

import java.util.HashMap;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.pipes.IsolatedServiceCaller;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.pipes.MessageSendingPipeAware;
import nl.nn.adapterframework.receivers.JavaListener;
import nl.nn.adapterframework.receivers.ServiceDispatcher;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * Posts a message to another IBIS-adapter in the same IBIS instance.
 * 
 * An IbisLocalSender makes a call to a Receiver with either a {@link nl.nn.adapterframework.http.WebServiceListener WebServiceListener}
 * or a {@link nl.nn.adapterframework.receivers.JavaListener JavaListener}. 
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.senders.IbisLocalSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setServiceName(String) serviceName}</td><td>Name of the {@link nl.nn.adapterframework.http.WebServiceListener WebServiceListener} that should be called</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJavaListener(String) javaListener}</td><td>Name of the {@link nl.nn.adapterframework.receivers.JavaListener JavaListener} that should be called (will be ignored when javaListenerSessionKey is set)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJavaListenerSessionKey(String) javaListenerSessionKey}</td><td>Name of the sessionKey which holds the name of the {@link nl.nn.adapterframework.receivers.JavaListener JavaListener} that should be called</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setIsolated(boolean) isolated}</td><td>when <code>true</code>, the call is made in a separate thread, possibly using separate transaction</td><td>false</td></tr>
 * <tr><td>{@link #setCheckDependency(boolean) checkDependency}</td><td>when <code>true</code>, the sender waits upon open until the called {@link nl.nn.adapterframework.receivers.JavaListener JavaListener} is opened</td><td>true</td></tr>
 * <tr><td>{@link #setDependencyTimeOut(int) dependencyTimeOut}</td><td>maximum time (in seconds) the sender waits for the listener to start. A value of -1 indicates to wait indefinitely</td><td>60 s</td></tr>
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
public class IbisLocalSender extends SenderWithParametersBase implements HasPhysicalDestination, MessageSendingPipeAware, SenderWrapperAware {
	
	private String name;
	private MessageSendingPipe messageSendingPipe;
	private SenderWrapper senderWrapper;
	private Configuration configuration;
	private String serviceName;
	private String javaListener;
	private String javaListenerSessionKey;
	private boolean isolated=false;
	private boolean synchronous=true;
	private boolean checkDependency=true;
	private int dependencyTimeOut=60;
	private String returnedSessionKeys=null;
	private IsolatedServiceCaller isolatedServiceCaller;

	public void configure() throws ConfigurationException {
		super.configure();
		if (!isSynchronous()) {
			setIsolated(true);
		}
		if (StringUtils.isEmpty(getServiceName())
				&& StringUtils.isEmpty(getJavaListener())
				&& StringUtils.isEmpty(getJavaListenerSessionKey())) {
			throw new ConfigurationException(getLogPrefix()+"has no serviceName or javaListener specified");
		}
		if (StringUtils.isNotEmpty(getServiceName())
				&& (StringUtils.isNotEmpty(getJavaListener())
						|| StringUtils.isNotEmpty(getJavaListenerSessionKey()))) {
			throw new ConfigurationException(getLogPrefix()+"serviceName and javaListener cannot be specified both");
		}
		if (configuration == null) {
			if (messageSendingPipe != null) {
				configuration = messageSendingPipe.getPipeLine().getAdapter()
						.getConfiguration();
			} else {
				if (senderWrapper != null) {
					if (senderWrapper instanceof MessageSendingPipeAware) {
						configuration = ((MessageSendingPipeAware) senderWrapper)
								.getMessageSendingPipe().getPipeLine()
								.getAdapter().getConfiguration();
					}
				}
			}
		}
	}

	public void open() throws SenderException {
		super.open();
		if (StringUtils.isNotEmpty(getJavaListener()) && isCheckDependency()) {
			boolean listenerOpened=false;
			int loops = getDependencyTimeOut();
			while (!listenerOpened
					&& !configuration.isUnloadInProgressOrDone()
					&& (loops == -1 || loops > 0)) {
				JavaListener listener= JavaListener.getListener(getJavaListener());
				if (listener!=null) {
					listenerOpened=listener.isOpen();
				}
				if (!listenerOpened && !configuration.isUnloadInProgressOrDone()) {
					if (loops != -1) {
						loops--;
					}
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
		} else if (StringUtils.isNotEmpty(getJavaListenerSessionKey())) {
			return "JavaListenerSessionKey "+getJavaListenerSessionKey();
		} else {
			return "JavaListener "+getJavaListener();
		}
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
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
						result = isolatedServiceCaller.callServiceIsolated(getServiceName(), correlationID, message, context, false);
					} else {
						log.debug(getLogPrefix()+"calling service ["+getServiceName()+"] in asynchronously");
						isolatedServiceCaller.callServiceAsynchronous(getServiceName(), correlationID, message, context, false);
						result = message;
					}
				} else {
					log.debug(getLogPrefix()+"calling service ["+getServiceName()+"] in same Thread");
					result = ServiceDispatcher.getInstance().dispatchRequest(getServiceName(), correlationID, message, context);
				}
			} catch (ListenerException e) {
				if (ExceptionUtils.getRootCause(e) instanceof TimeOutException) {
					throw new TimeOutException(getLogPrefix()+"timeout calling service ["+getServiceName()+"]",e);
				} else {
					throw new SenderException(getLogPrefix()+"exception calling service ["+getServiceName()+"]",e);
				}
			} finally {
				if (log.isDebugEnabled() && StringUtils.isNotEmpty(getReturnedSessionKeys())) {
					log.debug("returning values of session keys ["+getReturnedSessionKeys()+"]");
				}
				if (prc!=null) {
					Misc.copyContext(getReturnedSessionKeys(),context, prc.getSession());
				}
			} 
		} else {
			String javaListener;
			if (StringUtils.isNotEmpty(getJavaListenerSessionKey())) {
				javaListener = (String)prc.getSession().get(getJavaListenerSessionKey());
			} else {
				javaListener = getJavaListener();
			}
			try {
				JavaListener listener= JavaListener.getListener(javaListener);
				if (listener==null) {
					throw new SenderException("could not find JavaListener ["+javaListener+"]");
				}
				if (isIsolated()) {
					if (isSynchronous()) {
						log.debug(getLogPrefix()+"calling JavaListener ["+javaListener+"] in separate Thread");
						result = isolatedServiceCaller.callServiceIsolated(javaListener, correlationID, message, context, true);
					} else {
						log.debug(getLogPrefix()+"calling JavaListener ["+javaListener+"] in asynchronously");
						isolatedServiceCaller.callServiceAsynchronous(javaListener, correlationID, message, context, true);
						result = message;
					}
				} else {
					log.debug(getLogPrefix()+"calling JavaListener ["+javaListener+"] in same Thread");
					result = listener.processRequest(correlationID,message,context);
				}
			} catch (ListenerException e) {
				if (ExceptionUtils.getRootCause(e) instanceof TimeOutException) {
					throw new TimeOutException(getLogPrefix()+"timeout calling JavaListener ["+javaListener+"]",e);
				} else {
					throw new SenderException(getLogPrefix()+"exception calling JavaListener ["+javaListener+"]",e);
				}
			} finally {
				if (log.isDebugEnabled() && StringUtils.isNotEmpty(getReturnedSessionKeys())) {
					log.debug("returning values of session keys ["+getReturnedSessionKeys()+"]");
				}
				if (prc!=null) {
					Misc.copyContext(getReturnedSessionKeys(),context, prc.getSession());
				}
			}
		}
		return result;
	}


	public void setName(String name) {
		this.name=name;
	}
	public String getName() {
		return name;
	}

	public void setMessageSendingPipe(MessageSendingPipe messageSendingPipe) {
		this.messageSendingPipe = messageSendingPipe;
	}
	public MessageSendingPipe getMessageSendingPipe() {
		return messageSendingPipe;
	}

	public void setSenderWrapper(SenderWrapper senderWrapper) {
		this.senderWrapper = senderWrapper;
	}
	
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
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


	public void setJavaListenerSessionKey(String string) {
		javaListenerSessionKey = string;
	}
	public String getJavaListenerSessionKey() {
		return javaListenerSessionKey;
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

	public void setIsolatedServiceCaller(IsolatedServiceCaller isolatedServiceCaller) {
		this.isolatedServiceCaller = isolatedServiceCaller;
	}

}

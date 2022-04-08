/*
   Copyright 2013, 2016-2018 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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

import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLine.ExitState;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.pipes.IsolatedServiceCaller;
import nl.nn.adapterframework.receivers.JavaListener;
import nl.nn.adapterframework.receivers.ServiceDispatcher;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.Misc;

/**
 * Posts a message to another IBIS-adapter in the same IBIS instance.
 * 
 * An IbisLocalSender makes a call to a Receiver with either a {@link nl.nn.adapterframework.http.WebServiceListener WebServiceListener}
 * or a {@link JavaListener JavaListener}.
 *
 * Any parameters are copied to the PipeLineSession of the service called.
 * 
 * <h3>Configuration of the Adapter to be called</h3>
 * A call to another Adapter in the same IBIS instance is preferably made using the combination
 * of an IbisLocalSender and a {@link JavaListener JavaListener}. If,
 * however, a Receiver with a {@link nl.nn.adapterframework.http.WebServiceListener WebServiceListener} is already present, that can be used in some cases, too.
 *  
 * <h4>configuring IbisLocalSender and JavaListener</h4>
 * <ul>
 *   <li>Define a SenderPipe with an IbisLocalSender</li>
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
 *   <li>Define a SenderPipe with an IbisLocalSender</li>
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

	private final @Getter(onMethod = @__(@Override)) String domain = "Local";

	private Configuration configuration;
	private @Getter String serviceName;
	private @Getter String javaListener;
	private @Getter String javaListenerSessionKey;
	private @Getter boolean isolated=false;
	private @Getter(onMethod = @__({@Override})) boolean synchronous=true;
	private @Getter boolean checkDependency=true;
	private @Getter int dependencyTimeOut=60;
	private @Getter String returnedSessionKeys=null;
	private IsolatedServiceCaller isolatedServiceCaller;
	private @Getter boolean throwJavaListenerNotFoundException = true;

	@Override
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

		if(!(getApplicationContext() instanceof Configuration)) {
			throw new ConfigurationException(getLogPrefix()+"unable to determine configuration");
		}
		configuration = (Configuration) getApplicationContext();
	}

	@Override
	public void open() throws SenderException {
		super.open();
		if (StringUtils.isNotEmpty(getJavaListener()) && isCheckDependency()) {
			boolean listenerOpened=false;
			int loops = getDependencyTimeOut();
			while (!listenerOpened
					&& !configuration.isUnloadInProgressOrDone()
					&& (loops == -1 || loops > 0)) {
				JavaListener listener = JavaListener.getListener(getJavaListener());
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
				if(loops == 0 && (listener==null || !listener.isOpen())) {
					log.warn("Unable to open JavaListener ["+getJavaListener()+"] in "+getDependencyTimeOut()+" seconds. Make sure that the listener ["+getJavaListener()+"] exists or increase the timeout so that the sub-adapter may start before timeout limit.");
				}
			}
		}
	}

	@Override
	public String getPhysicalDestinationName() {
		if (StringUtils.isNotEmpty(getServiceName())) {
			return "WebServiceListener "+getServiceName();
		} else if (StringUtils.isNotEmpty(getJavaListenerSessionKey())) {
			return "JavaListenerSessionKey "+getJavaListenerSessionKey();
		} else {
			return "JavaListener "+getJavaListener();
		}
	}

	@Override
	public Message sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		String correlationID = session==null ? null : session.getMessageId();
		Message result = null;
		HashMap<String,Object> context = null;
		if (paramList!=null) {
			try {
				context = (HashMap<String,Object>) paramList.getValues(message, session).getValueMap();
			} catch (ParameterException e) {
				throw new SenderException(getLogPrefix()+"exception evaluating parameters",e);
			}
		}
		if (context==null) {
			context = new HashMap<>();
		}
		String serviceIndication;
		if (StringUtils.isNotEmpty(getServiceName())) {
			serviceIndication="service ["+getServiceName()+"]";
			try {
				if (isIsolated()) {
					if (isSynchronous()) {
						log.debug(getLogPrefix()+"calling "+serviceIndication+" in separate Thread");
						result = isolatedServiceCaller.callServiceIsolated(getServiceName(), correlationID, message, context, false);
					} else {
						log.debug(getLogPrefix()+"calling "+serviceIndication+" in asynchronously");
						isolatedServiceCaller.callServiceAsynchronous(getServiceName(), correlationID, message, context, false);
						result = message;
					}
				} else {
					log.debug(getLogPrefix()+"calling "+serviceIndication+" in same Thread");
					result = new Message(ServiceDispatcher.getInstance().dispatchRequest(getServiceName(), correlationID, message.asString(), context));
				}
			} catch (ListenerException | IOException e) {
				if (ExceptionUtils.getRootCause(e) instanceof TimeoutException) {
					throw new TimeoutException(getLogPrefix()+"timeout calling "+serviceIndication+"",e);
				}
				throw new SenderException(getLogPrefix()+"exception calling "+serviceIndication+"",e);
			} finally {
				if (log.isDebugEnabled() && StringUtils.isNotEmpty(getReturnedSessionKeys())) {
					log.debug("returning values of session keys ["+getReturnedSessionKeys()+"]");
				}
				if (session!=null) {
					Misc.copyContext(getReturnedSessionKeys(), context, session, this);
				}
			} 
		} else {
			String javaListener;
			if (StringUtils.isNotEmpty(getJavaListenerSessionKey())) {
				try {
					javaListener = session.getMessage(getJavaListenerSessionKey()).asString();
				} catch (IOException e) {
					throw new SenderException("unable to resolve session key ["+getJavaListenerSessionKey()+"]", e);
				}
			} else {
				javaListener = getJavaListener();
			}
			serviceIndication="JavaListener ["+javaListener+"]";
			try {
				JavaListener listener= JavaListener.getListener(javaListener);
				if (listener==null) {
					String msg = "could not find JavaListener ["+javaListener+"]";
					if (isThrowJavaListenerNotFoundException()) {
						throw new SenderException(msg);
					}
					log.info(getLogPrefix()+msg);
					return new Message("<error>"+msg+"</error>");
				}
				if (isIsolated()) {
					if (isSynchronous()) {
						log.debug(getLogPrefix()+"calling "+serviceIndication+" in separate Thread");
						result = isolatedServiceCaller.callServiceIsolated(javaListener, correlationID, message, context, true);
					} else {
						log.debug(getLogPrefix()+"calling "+serviceIndication+" in asynchronously");
						isolatedServiceCaller.callServiceAsynchronous(javaListener, correlationID, message, context, true);
						result = message;
					}
				} else {
					log.debug(getLogPrefix()+"calling "+serviceIndication+" in same Thread");
					result = new Message(listener.processRequest(correlationID,message.asString(),context));
				}
			} catch (ListenerException | IOException e) {
				if (ExceptionUtils.getRootCause(e) instanceof TimeoutException) {
					throw new TimeoutException(getLogPrefix()+"timeout calling "+serviceIndication,e);
				}
				throw new SenderException(getLogPrefix()+"exception calling "+serviceIndication,e);
			} finally {
				if (log.isDebugEnabled() && StringUtils.isNotEmpty(getReturnedSessionKeys())) {
					log.debug("returning values of session keys ["+getReturnedSessionKeys()+"]");
				}
				if (session!=null) {
					Misc.copyContext(getReturnedSessionKeys(), context, session, this);
				}
			}
		}
		
		ExitState exitState = (ExitState)context.remove(PipeLineSession.EXIT_STATE_CONTEXT_KEY);
		Object exitCode = context.remove(PipeLineSession.EXIT_CODE_CONTEXT_KEY);
		if (exitState!=null && exitState!=ExitState.SUCCESS) {
			context.put("originalResult", result);
			throw new SenderException(getLogPrefix()+"call to "+serviceIndication+" resulted in exitState ["+exitState+"] exitCode ["+exitCode+"]");
		}
		return result;
	}

	/**
	 * Sets a serviceName under which the JavaListener or WebServiceListener is registered.
	 */
	@IbisDoc({"name of the {@link nl.nn.adapterframework.http.WebServiceListener WebServiceListener} that should be called", ""})
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	/**
	 * When <code>true</code>, the call is made in a separate thread, possibly using separate transaction. 
	 */
	@IbisDoc({"when <code>true</code>, the call is made in a separate thread, possibly using separate transaction", "false"})
	public void setIsolated(boolean b) {
		isolated = b;
	}

	@IbisDoc({"name of the sessionkey which holds the name of the {@link nl.nn.adapterframework.receivers.JavaListener JavaListener} that should be called", ""})
	public void setJavaListenerSessionKey(String string) {
		javaListenerSessionKey = string;
	}

	@IbisDoc({"name of the {@link nl.nn.adapterframework.receivers.JavaListener JavaListener} that should be called (will be ignored when javaListenerSessionKey is set)", ""})
	public void setJavaListener(String string) {
		javaListener = string;
	}

	@IbisDoc({" when set <code>false</code>, the call is made asynchronously. this implies <code>isolated=true</code>", "true"})
	public void setSynchronous(boolean b) {
		synchronous = b;
	}

	@IbisDoc({"when <code>true</code>, the sender waits upon open until the called {@link nl.nn.adapterframework.receivers.JavaListener JavaListener} is opened", "true"})
	public void setCheckDependency(boolean b) {
		checkDependency = b;
	}

	@IbisDoc({"maximum time (in seconds) the sender waits for the listener to start. A value of -1 indicates to wait indefinitely", "60"})
	public void setDependencyTimeOut(int i) {
		dependencyTimeOut = i;
	}

	@IbisDoc({"comma separated list of keys of session variables that should be returned to caller, for correct results as well as for erronous results. (Only for listeners that support it, like JavaListener)<br/>N.B. To get this working, the attribute returnedSessionKeys must also be set on the corresponding Receiver", ""})
	public void setReturnedSessionKeys(String string) {
		returnedSessionKeys = string;
	}

	public void setIsolatedServiceCaller(IsolatedServiceCaller isolatedServiceCaller) {
		this.isolatedServiceCaller = isolatedServiceCaller;
	}

	@IbisDoc({"when set <code>false</code>, the xml-string \"&lt;error&gt;could not find JavaListener [...]&lt;/error&gt;\" is returned instead of throwing a senderexception", "true"})
	public void setThrowJavaListenerNotFoundException(boolean b) {
		throwJavaListenerNotFoundException = b;
	}

}

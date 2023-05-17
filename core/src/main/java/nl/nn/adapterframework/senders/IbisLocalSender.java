/*
   Copyright 2013, 2016-2018 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLine.ExitState;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.doc.Category;
import nl.nn.adapterframework.http.WebServiceListener;
import nl.nn.adapterframework.receivers.JavaListener;
import nl.nn.adapterframework.receivers.ServiceClient;
import nl.nn.adapterframework.receivers.ServiceDispatcher;
import nl.nn.adapterframework.stream.IThreadCreator;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.ThreadLifeCycleEventListener;
import nl.nn.adapterframework.util.Misc;

/**
 * Posts a message to another IBIS-adapter in the same IBIS instance. If the callee exits with an &lt;<code>exit</code>&gt;
 * that has state {@link nl.nn.adapterframework.core.PipeLine.ExitState#ERROR}, an error is considered to happen
 * in the caller which means that the <code>exception</code> forward is followed if it is present.
 * <p/>
 * <p/>
 * Returns exit.code as forward name to SenderPipe provided that exit.code can be parsed as integer.
 * For example, if the called adapter has an exit state with code
 * <code>2</code>, then the {@link nl.nn.adapterframework.pipes.SenderPipe} supports a forward with name <code>2</code>
 * that is followed when the called adapter exits with the mentioned exit. This does not work if the code is for example <code>c2</code>.
 * <p/>
 * <p/>
 * An IbisLocalSender makes a call to a Receiver with either a {@link WebServiceListener}
 * or a {@link JavaListener JavaListener}.
 *
 *
 *
 * <h3>Configuration of the Adapter to be called</h3>
 * A call to another Adapter in the same IBIS instance is preferably made using the combination
 * of an IbisLocalSender and a {@link JavaListener JavaListener}. If,
 * however, a Receiver with a {@link WebServiceListener} is already present, that can be used in some cases, too.
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
 * @ff.parameters All parameters are copied to the PipeLineSession of the service called.
 * @ff.forward "&lt;Exit.code&gt;" default
 *
 * @author Gerrit van Brakel
 * @since  4.2
 */
@Category("Basic")
public class IbisLocalSender extends SenderWithParametersBase implements HasPhysicalDestination, IThreadCreator{

	private final @Getter(onMethod = @__(@Override)) String domain = "Local";

	private Configuration configuration;
	private @Getter String serviceName;
	private @Getter String javaListener;
	private @Getter String javaListenerSessionKey;
	private @Getter boolean isolated=false;
	private @Getter(onMethod = @__({@Override})) boolean synchronous=true;
	private @Getter boolean checkDependency=true;
	private @Getter int dependencyTimeOut=60;
	private @Getter String returnedSessionKeys=""; // do not initialize with null, returned session keys must be set explicitly
	private @Setter IsolatedServiceCaller isolatedServiceCaller;
	private @Getter boolean throwJavaListenerNotFoundException = true;

	protected @Setter ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener;

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

	private boolean isJavaListener() {
		return StringUtils.isEmpty(getServiceName());
	}

	private String getServiceIndication(PipeLineSession session) throws SenderException {
		return (isJavaListener() ? "JavaListener [" : "Service [") + getActualServiceName(session) + "]";
	}

	private ServiceClient getServiceImplementation(PipeLineSession session) throws SenderException {
		String actualServiceName = getActualServiceName(session);
		if (isJavaListener()) {
			if (!JavaListener.getListenerNames().contains(actualServiceName)) {
				throw new SenderException("could not find JavaListener [" + actualServiceName + "]");
			}
			return JavaListener.getListener(actualServiceName);
		} else {
			if (!ServiceDispatcher.getInstance().isRegisteredServiceListener(actualServiceName)) {
				throw new SenderException("No service with name [" + actualServiceName + "] has been registered");
			}
			return ServiceDispatcher.getInstance().getListener(actualServiceName);
		}
	}

	private String getActualServiceName(PipeLineSession session) throws SenderException {
		if (isJavaListener()) {
			final String actualJavaListenerName;
			if (StringUtils.isNotEmpty(getJavaListenerSessionKey())) {
				try {
					actualJavaListenerName = session.getMessage(getJavaListenerSessionKey()).asString();
				} catch (IOException e) {
					throw new SenderException("unable to resolve session key [" + getJavaListenerSessionKey() + "]", e);
				}
			} else {
				actualJavaListenerName = getJavaListener();
			}
			return actualJavaListenerName;
		} else {
			return getServiceName();
		}
	}

	@Override
	public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		String correlationID = session == null ? null : session.getCorrelationId();
		SenderResult result;
		try (PipeLineSession context = new PipeLineSession()) {
			if (paramList!=null) {
				try {
					Map<String,Object> paramValues = paramList.getValues(message, session).getValueMap();
					if (paramValues!=null) {
						context.putAll(paramValues);
					}
				} catch (ParameterException e) {
					throw new SenderException(getLogPrefix()+"exception evaluating parameters",e);
				}
			}
			final ServiceClient serviceClient;
			try {
				serviceClient = getServiceImplementation(session);
			} catch (SenderException e) {
				if (isThrowJavaListenerNotFoundException()) {
					throw e;
				}
				log.info("{} {}", getLogPrefix(), e.getMessage());
				return new SenderResult(new Message("<error>" + e.getMessage() + "</error>"), e.getMessage());
			}
			final String serviceIndication = getServiceIndication(session);

			try {
				if (isIsolated()) {
					if (isSynchronous()) {
						log.debug("{} calling {} in separate Thread", this::getLogPrefix,() -> serviceIndication);
						result = isolatedServiceCaller.callServiceIsolated(serviceClient, message, context, threadLifeCycleEventListener);
					} else {
						// We return same message as we send, so it should be preserved in case it's not repeatable
						message.preserve();
						log.debug("{} calling {} in asynchronously", this::getLogPrefix, () -> serviceIndication);
						isolatedServiceCaller.callServiceAsynchronous(serviceClient, message, context, threadLifeCycleEventListener);
						result = new SenderResult(message);
					}
				} else {
					log.debug("{} calling {} in same Thread", this::getLogPrefix, () -> serviceIndication);
					result = new SenderResult(serviceClient.processRequest(message, context));
				}
			} catch (ListenerException | IOException e) {
				if (ExceptionUtils.getRootCause(e) instanceof TimeoutException) {
					throw new TimeoutException(getLogPrefix()+"timeout calling "+serviceIndication,e);
				}
				throw new SenderException(getLogPrefix()+"exception calling "+serviceIndication,e);
			} finally {
				if (session != null && StringUtils.isNotEmpty(getReturnedSessionKeys())) {
					log.debug("returning values of session keys [{}]", getReturnedSessionKeys());
					Misc.copyContext(getReturnedSessionKeys(), context, session, this);
				}
			}

			ExitState exitState = (ExitState)context.remove(PipeLineSession.EXIT_STATE_CONTEXT_KEY);
			Object exitCode = context.remove(PipeLineSession.EXIT_CODE_CONTEXT_KEY);

			String forwardName = Objects.toString(exitCode, null);
			result.setForwardName(forwardName);
			result.setSuccess(exitState==null || exitState==ExitState.SUCCESS);
			result.setErrorMessage("exitState="+exitState);

			result.getResult().unscheduleFromCloseOnExitOf(context);
			result.getResult().closeOnCloseOf(session, this);

			return result;
		}
	}

	/** Name of the {@link WebServiceListener} that should be called */
	@Deprecated
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	/** Name of the {@link JavaListener} that should be called (will be ignored when javaListenerSessionKey is set) */
	public void setJavaListener(String string) {
		javaListener = string;
	}

	/** Name of the sessionKey which holds the name of the {@link JavaListener} that should be called */
	public void setJavaListenerSessionKey(String string) {
		javaListenerSessionKey = string;
	}

	/**
	 * Comma separated list of keys of session variables that will be returned to caller, for correct results as well as for erroneous results.
	 * The set of available sessionKeys to be returned might be limited by the returnedSessionKeys attribute of the corresponding JavaListener.
	 */
	public void setReturnedSessionKeys(String string) {
		returnedSessionKeys = string;
	}

	/**
	 * If set <code>false</code>, the call is made asynchronously. This implies isolated=<code>true</code>
	 * @ff.default true
	 */
	public void setSynchronous(boolean b) {
		synchronous = b;
	}

	/**
	 * If <code>true</code>, the call is made in a separate thread, possibly using separate transaction
	 * @ff.default false
	 */
	public void setIsolated(boolean b) {
		isolated = b;
	}

	/**
	 * If <code>true</code>, the sender waits upon open until the called {@link JavaListener} is opened
	 * @ff.default true
	 */
	public void setCheckDependency(boolean b) {
		checkDependency = b;
	}

	/**
	 * Maximum time (in seconds) the sender waits for the listener to start. A value of -1 indicates to wait indefinitely
	 * @ff.default 60
	 */
	public void setDependencyTimeOut(int i) {
		dependencyTimeOut = i;
	}

	/**
	 * If set <code>false</code>, the xml-string \"&lt;error&gt;could not find JavaListener [...]&lt;/error&gt;\" is returned instead of throwing a senderexception
	 * @ff.default true
	 */
	public void setThrowJavaListenerNotFoundException(boolean b) {
		throwJavaListenerNotFoundException = b;
	}

}

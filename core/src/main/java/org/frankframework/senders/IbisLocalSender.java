/*
   Copyright 2013, 2016-2018 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
package org.frankframework.senders;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.ConfigurationAware;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.DestinationType;
import org.frankframework.core.DestinationType.Type;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.ListenerException;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLine.ExitState;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.doc.Category;
import org.frankframework.doc.Forward;
import org.frankframework.http.WebServiceListener;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.pipes.SenderPipe;
import org.frankframework.receivers.JavaListener;
import org.frankframework.receivers.ServiceClient;
import org.frankframework.receivers.ServiceDispatcher;
import org.frankframework.stream.Message;
import org.frankframework.threading.IThreadCreator;
import org.frankframework.threading.ThreadLifeCycleEventListener;
import org.frankframework.util.MessageUtils;

/**
 * Posts a message to another Frank!Framework-adapter in the same Frank!Framework instance. If the callee exits with an &lt;<code>exit</code>&gt;
 * that has state {@link PipeLine.ExitState#ERROR}, an error is considered to happen
 * in the caller which means that the <code>exception</code> forward is followed if it is present.
 * <p>
 * The IbisLocalSender is now considered to be legacy. The new way to call another adapter from your own
 * adapter is by using the {@link FrankSender}.
 * </p>
 * <p>
 * Returns exit.code as forward name to {@link SenderPipe} provided that exit.code can be parsed as integer.
 * For example, if the called adapter has an exit state with code
 * <code>2</code>, then the {@link SenderPipe} supports a forward with name <code>2</code>
 * that is followed when the called adapter exits with the mentioned exit. This does not work if the code is for example <code>c2</code>.
 * </p>
 * <p>
 * An IbisLocalSender makes a call to a {@link org.frankframework.receivers.Receiver} with either a {@link WebServiceListener}
 * or a {@link JavaListener JavaListener}.
 * </p>
 *
 *
 * <h3>Configuration of the Adapter to be called</h3>
 * A call to another Adapter in the same Frank!Framework instance is preferably made using the combination
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
 *       from applications other than this Frank!Framework-instance</li>
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
 *
 * @author Gerrit van Brakel
 * @since  4.2
 */
@DestinationType(Type.ADAPTER)
@Forward(name = "*", description = "Exit code")
@Category(Category.Type.BASIC)
public class IbisLocalSender extends AbstractSenderWithParameters implements HasPhysicalDestination, IThreadCreator, ConfigurationAware {

	private @Setter Configuration configuration;
	private @Getter String serviceName;
	private @Getter String javaListener;
	private @Getter String javaListenerSessionKey;
	private @Getter boolean isolated=false;
	private @Getter boolean synchronous=true;
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
			throw new ConfigurationException("has no serviceName or javaListener specified");
		}
		if (StringUtils.isNotEmpty(getServiceName())
				&& (StringUtils.isNotEmpty(getJavaListener())
						|| StringUtils.isNotEmpty(getJavaListenerSessionKey()))) {
			throw new ConfigurationException("serviceName and javaListener cannot be specified both");
		}
	}

	@Override
	public void start() {
		super.start();
		if (StringUtils.isNotEmpty(getJavaListener()) && isCheckDependency()) {
			boolean listenerOpened=false;
			long sleepDelay = 25L;
			long timeoutAt = getDependencyTimeOut() == -1 ? -1L : System.currentTimeMillis() + 1000L * getDependencyTimeOut();
			while (!listenerOpened
					&& !configuration.isUnloadInProgressOrDone()
					&& (timeoutAt == -1L || System.currentTimeMillis() < timeoutAt)) {
				JavaListener<?> listener = JavaListener.getListener(getJavaListener());
				if (listener!=null) {
					listenerOpened=listener.isOpen();
				}
				if (!listenerOpened && !configuration.isUnloadInProgressOrDone()) {
					try {
						log.debug("waiting for JavaListener [{}] to open", getJavaListener());
						Thread.sleep(sleepDelay);
						if (sleepDelay < 1000L) sleepDelay = sleepDelay * 2L;
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						throw new LifecycleException(e);
					}
				}
				if(System.currentTimeMillis() >= timeoutAt && (listener==null || !listener.isOpen())) {
					log.warn("Unable to open JavaListener [{}] in {} seconds. Make sure that the listener [{}] exists or increase the timeout so that the sub-adapter may start before timeout limit.", getJavaListener(), getDependencyTimeOut(), getJavaListener());
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

	@Nonnull
	private String getActualServiceName(PipeLineSession session) throws SenderException {
		if (isJavaListener()) {
			String actualJavaListenerName;
			if (StringUtils.isNotEmpty(getJavaListenerSessionKey())) {
				try {
					actualJavaListenerName = session.getString(getJavaListenerSessionKey());
				} catch (Exception e) {
					log.warn("unable to resolve session key [{}]", getJavaListenerSessionKey(), e);
					actualJavaListenerName = null;
				}
				if (actualJavaListenerName == null) {
					throw new SenderException("unable to resolve session key [" + getJavaListenerSessionKey() + "]");
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
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
		SenderResult result;
		try (PipeLineSession subAdapterSession = new PipeLineSession()) {
			subAdapterSession.put(PipeLineSession.MANUAL_RETRY_KEY, session.get(PipeLineSession.MANUAL_RETRY_KEY, false));
			String correlationId = session.getCorrelationId();
			if (correlationId != null) {
				subAdapterSession.put(PipeLineSession.CORRELATION_ID_KEY, correlationId);
			}
			subAdapterSession.put(PipeLineSession.MESSAGE_ID_KEY, MessageUtils.generateMessageId());
			try {
				Map<String, Object> paramValues = paramList.getValues(message, session).getValueMap();
				subAdapterSession.putAll(paramValues);
			} catch (ParameterException e) {
				throw new SenderException("exception evaluating parameters", e);
			}
			final ServiceClient serviceClient;
			try {
				serviceClient = getServiceImplementation(session);
			} catch (SenderException e) {
				if (isThrowJavaListenerNotFoundException()) {
					throw e;
				}
				log.info(e.getMessage());
				return new SenderResult(new Message("<error>" + e.getMessage() + "</error>"), e.getMessage());
			}
			final String serviceIndication = getServiceIndication(session);

			try {
				if (isIsolated()) {
					if (isSynchronous()) {
						log.debug("calling {} in separate Thread", () -> serviceIndication);
						result = isolatedServiceCaller.callServiceIsolated(serviceClient, message, subAdapterSession, threadLifeCycleEventListener);
					} else {
						log.debug("calling {} in asynchronously", () -> serviceIndication);
						isolatedServiceCaller.callServiceAsynchronous(serviceClient, message, subAdapterSession, threadLifeCycleEventListener);
						result = new SenderResult(message);
					}
				} else {
					log.debug("calling {} in same Thread", () -> serviceIndication);
					result = new SenderResult(serviceClient.processRequest(message, subAdapterSession));
				}

			} catch (ListenerException | IOException e) {
				if (ExceptionUtils.getRootCause(e) instanceof TimeoutException) {
					throw new TimeoutException("timeout calling "+serviceIndication,e);
				}
				throw new SenderException("exception calling "+serviceIndication,e);
			} finally {
				if (StringUtils.isNotEmpty(getReturnedSessionKeys())) {
					log.debug("returning values of session keys [{}]", getReturnedSessionKeys());
				}

				// The original message will be set by the InputOutputPipeLineProcessor, which add it to the autocloseables list.
				// The input message should not be managed by this sub-PipelineSession but rather the original pipeline
				subAdapterSession.unscheduleCloseOnSessionExit(message);
				subAdapterSession.mergeToParentSession(getReturnedSessionKeys(), session);
			}

			ExitState exitState = (ExitState)subAdapterSession.remove(PipeLineSession.EXIT_STATE_CONTEXT_KEY);
			Object exitCode = subAdapterSession.remove(PipeLineSession.EXIT_CODE_CONTEXT_KEY);

			String forwardName = Objects.toString(exitCode, null);
			result.setForwardName(forwardName);
			result.setSuccess(exitState==null || exitState==ExitState.SUCCESS);
			result.setErrorMessage("exitState="+exitState);

			return result;
		}
	}

	/** Name of the {@link WebServiceListener} that should be called */
	@Deprecated(forRemoval = true, since = "7.9.0")
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
	 * If {@code false}, the call is made asynchronously. This implies <code>isolated=true</code>
	 * @ff.default true
	 */
	public void setSynchronous(boolean b) {
		synchronous = b;
	}

	/**
	 * If {@code true}, the call is made in a separate thread, possibly using separate transaction
	 * @ff.default false
	 */
	public void setIsolated(boolean b) {
		isolated = b;
	}

	/**
	 * If {@code true}, the sender waits upon open until the called {@link JavaListener} is opened
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
	 * If {@code false}, the xml-string "&lt;error&gt;could not find JavaListener [...]&lt;/error&gt;" is returned instead of throwing a senderexception
	 * @ff.default true
	 */
	public void setThrowJavaListenerNotFoundException(boolean b) {
		throwJavaListenerNotFoundException = b;
	}

}

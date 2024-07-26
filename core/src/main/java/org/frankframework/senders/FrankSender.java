/*
   Copyright 2024 WeAreFrank!

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
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.frankframework.configuration.AdapterManager;
import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.IbisManager;
import org.frankframework.core.Adapter;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.doc.Category;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.pipes.SenderPipe;
import org.frankframework.receivers.ServiceClient;
import org.frankframework.stream.IThreadCreator;
import org.frankframework.stream.Message;
import org.frankframework.stream.ThreadLifeCycleEventListener;
import org.springframework.beans.factory.annotation.Autowired;

import nl.nn.adapterframework.dispatcher.DispatcherManager;

/**
 * Sender to send a message to another Frank! Adapter, or an external program running in the same JVM as the Frank!Framework.
 * <p>
 * Sends a message to another Frank!Framework-adapter in the same Frank!Framework instance, or an external program running in
 * the same JVM as the Frank!Framework. If the callee exits with an {@code <Exit/>} that has state {@link PipeLine.ExitState#ERROR},
 * an error is considered to happen in the caller which means that the {@code exception} forward is followed if it is present.
 * </p>
 * <p>
 * Returns {@code exit.code} as forward name to the {@link SenderPipe}, provided that {@code exit.code} can be parsed as integer.
 * For example, if the called adapter has an exit state with code
 * {@code 2}, then the {@link SenderPipe} supports a forward with name {@code 2}
 * that is followed when the called adapter exits with the mentioned exit. This does not work if the code is for example {@code c2}.
 * </p>
 * <p>
 * A FrankSender makes a call to either an {@link Adapter} or an external program by setting the {@link #scope}. By default the scope is {@code ADAPTER}.
 * </p>
 *
 *
 * <h3>Configuration of the Adapter to be called</h3>
 * A call to another Adapter in the same Frank!Framework instance is preferably made using the combination
 * of a FrankSender configured with the name of the adapter.
 *
 * <h4>Configuring FrankSender and Adapter</h4>
 * <ul>
 *   <li>Define a {@link SenderPipe} with a FrankSender</li>
 *   <li>Set the attribute {@code target} to <i>targetAdapterName</i></li>
 *   <li>If the adapter is in another Configuration deployed in the same Frank!Framework instance, then set {@code target} to {@code targetConfigurationName/targetAdapterName}</li>
 * </ul>
 * In the Adapter to be called:
 * <ul>
 *   <li>The adapter does not need to have a receiver configured to be called from a FrankSender,</li>
 *   <li>The adapter will run in the same transaction as the calling adapter,</li>
 *   <li>If the called adapter does not to run in its own transaction, set the transaction attributes on the {@link PipeLine} attribute of this adapter
 *   or on the {@link SenderPipe} that contains this {@code FrankSender}.</li>
 * </ul>
 *
 * <h4>Configuring FrankSender and Remote Application</h4>
 * <em>NB:</em> Please make sure that the IbisServiceDispatcher-1.4.jar or newer is present on the class path of the server. For more information, see:
 * <ul>
 *     <li>Define a {@link SenderPipe} with a FrankSender</li>
 *     <li>Set the attribute {@code scope} to either {@code JVM} for a Java application, or to {@code DLL} for code loaded from a DLL</li>
 *     <li>Set the attribute {@code target} to the service-name the other application used to register itself</li>
 * </ul>
 * In the other application:
 * <ul>
 *     <li>Implement the interface {@code nl.nn.adapterframework.dispatcher.RequestProcessor} from the IbisServiceDispatcher library</li>
 *     <li>Register the instance with the {@code nl.nn.adapterframework.dispatcher.DispatcherManager} obtained via the {@code nl.nn.adapterframework.dispatcher.DispatcherManagerFactory}</li>
 *     <li>See the implementation code of the {@code JavaListener} in the Frank!Framework for an example</li>
 * </ul>
 * See also the repository of the IbisServiceDispatcher:
 *  <a href="https://github.com/frankframework/servicedispatcher">https://github.com/frankframework/servicedispatcher</a>
 *
 * @ff.parameter {@code scope} Determine scope dynamically at runtime. If the parameter value is empty, fall back to the scope configured via the attribute, or the default scope {@code ADAPTER}.
 * @ff.parameter {@code target} Determine target dynamically at runtime. If the parameter value is empty, fall back to the target configured via the attribute.
 * @ff.parameters All parameters except {@code scope} and {@code target} are copied to the {@link PipeLineSession} of the adapter called.
 * @ff.forward "{@code <Exit.code>}" default
 *
 */
@Category("Basic")
public class FrankSender extends SenderWithParametersBase implements HasPhysicalDestination, IThreadCreator {

	public static final String TARGET_PARAM_NAME = "target";
	public static final String SCOPE_PARAM_NAME = "scope";

	/**
	 * Scope for {@link FrankSender} call: Another Frank!Framework Adapter, or another Java application running in the same JVM.
	 * {@code DLL} is a special way of invoking the other application, loading the service via a DLL. See the
	 * documentation of the <a href="https://github.com/frankframework/servicedispatcher">IbisServiceDispatcher</a> library for further details on how implement a Java program or DLL running
	 * alongside the Frank!Framework that can be called from the Frank!Framework.
	 *
	 * <h4>See</h4>
	 *  <a href="https://github.com/frankframework/servicedispatcher">https://github.com/frankframework/servicedispatcher</a>
	 */
	public enum Scope { JVM, DLL, ADAPTER }

	private @Getter Scope scope = Scope.ADAPTER;
	private @Getter String target;
	private @Getter String returnedSessionKeys=""; // do not initialize with null, returned session keys must be set explicitly

	private @Getter boolean synchronous=true;

	private @Autowired @Setter IsolatedServiceCaller isolatedServiceCaller;
	private @Autowired @Setter ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener;
	private @Autowired @Setter AdapterManager adapterManager;
	private @Autowired @Setter IbisManager ibisManager;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		ParameterList pl = getParameterList();
		if (StringUtils.isBlank(getTarget()) && (pl == null || !pl.hasParameter(TARGET_PARAM_NAME))) {
			throw new ConfigurationException("'target' required, either as parameter or as attribute in the configuration");
		}
		if (StringUtils.isNotBlank(getTarget()) && getScope() == Scope.ADAPTER) {
			try {
				findAdapter(getTarget());
			} catch (SenderException e) {
				throw new ConfigurationException("Cannot find adapter specified in configuration", e);
			}
		}
	}

	@Override
	public String getPhysicalDestinationName() {
		StringBuilder result = new StringBuilder();
		ParameterList pl = getParameterList();
		if (pl != null && pl.hasParameter(SCOPE_PARAM_NAME)) {
			result.append("param:scope");
		} else {
			result.append(getScope());
		}
		result.append("/");
		if (pl != null && pl.hasParameter(TARGET_PARAM_NAME)) {
			result.append("param:target");
		} else {
			result.append(getTarget());
		}
		return result.toString();
	}

	@Override
	public String getDomain() {
		ParameterList pl = getParameterList();
		if (pl != null && pl.hasParameter(SCOPE_PARAM_NAME)) {
			return "Dynamic";
		} else {
			return getScope().name();
		}
	}

	@Override
	public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		ParameterValueList pvl = getParameterValueList(message, session);
		Scope actualScope = determineActualScope(pvl);
		String actualTarget = determineActualTarget(pvl);
		log.info("{}Sending message to {} [{}]", this::getLogPrefix, ()->actualScope, ()->actualTarget);
		ServiceClient serviceClient = switch (actualScope) {
			case ADAPTER -> getAdapterServiceClient(actualTarget);
			case JVM, DLL -> getJvmDispatcherServiceClient(actualScope, actualTarget);
		};
		return invokeService(serviceClient, actualScope, actualTarget, message, session, pvl);
	}

	private SenderResult invokeService(ServiceClient serviceClient, Scope scope, String target, Message message, PipeLineSession parentSession, ParameterValueList pvl) throws SenderException, TimeoutException {
		try (PipeLineSession childSession = new PipeLineSession()) {
			setupChildSession(parentSession, pvl, childSession);

			Message resultMessage = doCallService(serviceClient, scope, target, message, parentSession, childSession);

			// Detach the result message from the child session, and attach to the parent session
			resultMessage.unscheduleFromCloseOnExitOf(childSession);
			resultMessage.closeOnCloseOf(parentSession, this);

			return createSenderResult(resultMessage, childSession);
		}
	}

	private static SenderResult createSenderResult(Message resultMessage, PipeLineSession childSession) {
		PipeLine.ExitState exitState = (PipeLine.ExitState) childSession.remove(PipeLineSession.EXIT_STATE_CONTEXT_KEY);
		Object exitCode = childSession.remove(PipeLineSession.EXIT_CODE_CONTEXT_KEY);
		String forwardName = Objects.toString(exitCode, null);

		SenderResult result = new SenderResult(resultMessage);
		result.setForwardName(forwardName);
		result.setSuccess(exitState == null || exitState == PipeLine.ExitState.SUCCESS);
		result.setErrorMessage("exitState=" + exitState);

		return result;
	}

	private Message doCallService(ServiceClient serviceClient, Scope scope, String target, Message message, PipeLineSession parentSession, PipeLineSession childSession) throws TimeoutException, SenderException {
		try {
			if (isSynchronous()) {
				return serviceClient.processRequest(message, childSession);
			} else {
				isolatedServiceCaller.callServiceAsynchronous(serviceClient, message, parentSession, threadLifeCycleEventListener);
				return Message.nullMessage();
			}
		} catch (ListenerException | IOException e) {
			if (ExceptionUtils.getRootCause(e) instanceof TimeoutException) {
				throw new TimeoutException(getLogPrefix()+"timeout calling " + scope + " [" + target + "]",e);
			}
			throw new SenderException(getLogPrefix()+"exception calling " + scope + " [" + target + "]",e);
		} finally {
			if (StringUtils.isNotEmpty(getReturnedSessionKeys())) {
				log.debug("returning values of session keys [{}]", getReturnedSessionKeys());
			}

			// The session-key originalMessage will be set by the InputOutputPipeLineProcessor, which adds it to the auto-closeable session resources list.
			// The input message should not be managed by this sub-PipelineSession but rather the original pipeline and so it should be removed again.
			childSession.unscheduleCloseOnSessionExit(message);
			childSession.mergeToParentSession(getReturnedSessionKeys(), parentSession);
		}
	}

	private static void setupChildSession(PipeLineSession session, ParameterValueList pvl, PipeLineSession childSession) {
		childSession.put(PipeLineSession.MANUAL_RETRY_KEY, session.get(PipeLineSession.MANUAL_RETRY_KEY, false));
		String correlationId = session.getCorrelationId();
		if (correlationId != null) {
			childSession.put(PipeLineSession.CORRELATION_ID_KEY, correlationId);
		}
		if (pvl != null) {
			Map<String, Object> valueMap = pvl.getValueMap();
			valueMap.remove(TARGET_PARAM_NAME);
			valueMap.remove(SCOPE_PARAM_NAME);
			childSession.putAll(valueMap);
		}
	}

	private ServiceClient getJvmDispatcherServiceClient(Scope scope, String target) throws SenderException {
		DispatcherManager dm = getDispatcherManager(scope == Scope.DLL);
		return (message, session) -> {
			try {
				return new Message(dm.processRequest(target, session.getCorrelationId(), message.asString(), session));
			} catch (Exception e) {
				throw new ListenerException(getLogPrefix() + "Exception sending message to [" + target + "]", e);
			}
		};
	}

	private DispatcherManager getDispatcherManager(boolean dllDispatch) throws SenderException {
		DispatcherManager dm;
		try {
			Class<?> c = Class.forName("nl.nn.adapterframework.dispatcher.DispatcherManagerFactory");

			if (dllDispatch) {
				String version = nl.nn.adapterframework.dispatcher.Version.version;
				if (version.contains("IbisServiceDispatcher 1.3"))
					throw new SenderException("IBIS-ServiceDispatcher out of date! Please update to version 1.4 or higher");

				Method getDispatcherManager = c.getMethod("getDispatcherManager", String.class);
				dm = (DispatcherManager) getDispatcherManager.invoke(null, "DLL");
			} else {
				Method getDispatcherManager = c.getMethod("getDispatcherManager");
				dm = (DispatcherManager) getDispatcherManager.invoke(null, (Object[]) null);
			}
		} catch (SenderException e) {
			throw e;
		} catch (Exception e) {
			throw new SenderException("Could not load DispatcherManager", e);
		}

		return dm;
	}

	private ServiceClient getAdapterServiceClient(String target) throws SenderException {
		Adapter adapter = findAdapter(target);
		return (message, session) -> {
			PipeLineResult plr = adapter.processMessageDirect(session.getMessageId(), message, session);
			session.setExitState(plr);
			if (plr.getState() == PipeLine.ExitState.ERROR) {
				// Adapter.processMessageDirect() catches ListenerException and makes an error message from it, but
				// we want to actually throw it.
				String errorResult;
				try {
					errorResult = plr.getResult().asString();
				} catch (IOException e) {
					throw new ListenerException(getLogPrefix() + "Call resulted in error, but cannot get error message:", e);
				}
				if (errorResult != null && errorResult.contains("ListenerException")) {
					throw new ListenerException(getLogPrefix() + errorResult);
				}
			}
			return plr.getResult();
		};
	}

	@Nonnull
	Adapter findAdapter(String target) throws SenderException {
		AdapterManager actualAdapterManager;
		String adapterName;
		int configNameSeparator = target.indexOf('/');
		if (configNameSeparator > 0) {
			adapterName = target.substring(configNameSeparator + 1);
			String configurationName = target.substring(0, configNameSeparator);
			Configuration configuration = ibisManager.getConfiguration(configurationName);
			if (configuration == null) {
				throw new SenderException(getLogPrefix()+"Configuration [" + configurationName + "] not found");
			}
			actualAdapterManager = configuration.getAdapterManager();
		} else if (configNameSeparator == 0) {
			adapterName = target.substring(1);
			actualAdapterManager = adapterManager;
		} else {
			adapterName = target;
			actualAdapterManager = adapterManager;
		}
		Adapter adapter = actualAdapterManager.getAdapter(adapterName);
		if (adapter == null) {
			throw new SenderException(getLogPrefix() + "Cannot find adapter specified by [" + target + "]");
		}
		return adapter;
	}

	Scope determineActualScope(@Nullable ParameterValueList pvl) {
		ParameterValue scopeParam = pvl != null ? pvl.findParameterValue(SCOPE_PARAM_NAME) : null;
		if (scopeParam != null) {
			return Scope.valueOf(scopeParam.asStringValue(getScope().name()));
		}
		return getScope();
	}

	String determineActualTarget(@Nullable ParameterValueList pvl) {
		ParameterValue targetParam = pvl != null ? pvl.findParameterValue(TARGET_PARAM_NAME) : null;
		if (targetParam != null) {
			return targetParam.asStringValue(getTarget());
		}
		return getTarget();
	}

	/**
	 * Synchronous or Asynchronous execution of the call to other adapter or system.
	 * <br/>
	 * Set to <code>false</code> to make the call asynchronously. This means that the current adapter
	 * continues with the next pipeline and the result of the sub-adapter that was called, or other system that was called,
	 * is ignored. Instead, the input message will be returned as the result message.
	 *
	 * @ff.default true
	 */
	public void setSynchronous(boolean b) {
		synchronous = b;
	}

	/**
	 * {@link Scope} decides if the FrankSender calls another adapter, or another Java program running in the same JVM.
	 * <br/>
	 * It is possible to set this via a parameter. If the parameter is defined but the value at runtime
	 * is empty, then the value set via this attribute will be used as default.
	 *
	 * @param scope Either {@code ADAPTER}, {@code  JVM} or {@code DLL}.
	 *
	 * @ff.default ADAPTER
	 */
	public void setScope(Scope scope) {
		this.scope = scope;
	}

	/**
	 * Target: service-name of service in other application that should be called, or name of adapter to be called.
	 * If the adapter is in another configuration, prefix the adapter name with the name of that configuration and a "/".
	 * <br/>
	 * It is possible to set a target at runtime via a parameter.
	 * <br/>
	 * If a parameter with name {@value #TARGET_PARAM_NAME} exists but has no value, then the target configured
	 * via the attribute will be used as a default.
	 * 
	 * @param target Name of the target, adapter or registered service.
	 */
	public void setTarget(String target) {
		this.target = target;
	}

	/**
	 * Comma separated list of keys of session variables that will be returned to caller, for correct results as well as for erroneous results.
	 * The set of available sessionKeys to be returned might be limited by the returnedSessionKeys attribute of the corresponding JavaListener.
	 */
	public void setReturnedSessionKeys(String string) {
		returnedSessionKeys = string;
	}
}

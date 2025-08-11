/*
   Copyright 2024-2025 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.Getter;
import lombok.Setter;

import nl.nn.adapterframework.dispatcher.DispatcherManager;

import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.ConfigurationAware;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.IbisManager;
import org.frankframework.core.Adapter;
import org.frankframework.core.DestinationType;
import org.frankframework.core.DestinationType.Type;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.doc.Category;
import org.frankframework.doc.Forward;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.pipes.SenderPipe;
import org.frankframework.receivers.FrankListener;
import org.frankframework.receivers.ServiceClient;
import org.frankframework.stream.Message;
import org.frankframework.threading.IThreadCreator;
import org.frankframework.threading.ThreadLifeCycleEventListener;
import org.frankframework.util.MessageUtils;

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
 * <p/>
 *
 * <h3>Configuration of the Adapter to be called</h3>
 * <p>
 * A call to another Adapter in the same Frank!Framework instance is preferably made using the combination
 * of a FrankSender configured with the name of the adapter.
 * </p>
 * <h4>Configuring FrankSender and Adapter</h4>
 * <ul>
 *   <li>Define a {@link SenderPipe} with a FrankSender</li>
 *   <li>Set the attribute {@code target} to <i>targetAdapterName</i></li>
 *   <li>If the adapter is in another Configuration deployed in the same Frank!Framework instance, then set {@code target} to {@code targetConfigurationName/targetAdapterName} (note the slash-separator between Configuration name and Adapter name).</li>
 * </ul>
 * In the Adapter to be called:
 * <ul>
 *   <li>The adapter does not need to have a dedicated receiver configured to be called from a FrankSender.</li>
 *   <li>The adapter will run in the same transaction as the calling adapter.</li>
 *   <li>If the called adapter does not to run in its own transaction, set the transaction attributes on the {@link PipeLine} attribute of this adapter
 *   or on the {@link SenderPipe} that contains this {@code FrankSender}.</li>
 * </ul>
 *
 * <h4>Configuring FrankSender with FrankListener</h4>
 * <ul>
 *   <li>Define a {@link SenderPipe} with a FrankSender</li>
 *   <li>In the target adapter, define a {@link org.frankframework.receivers.Receiver} with a {@link FrankListener}</li>
 *   <li>Give a unique name to the listener: {@link FrankListener#setName(String)}. If the name is not set, the name of the {@link Adapter} will be used.</li>
 *   <li>Set the {@link #setScope(Scope)} to {@code LISTENER} and the {@link #setTarget(String)} to the listener name as per previous point</li>
 *   <li>If the listener is in a different configuration, prefix the listener name with the name of the configuration and a slash ({@code /}) as separator between configuration and listener name</li>
 * </ul>
 *
 * <h4>Configuring FrankSender and Remote Application</h4>
 * <p>
 * <em>NB:</em> Please make sure that the IbisServiceDispatcher-1.4.jar or newer is present on the class path of the server. For more information, see:
 * </p>
 * <ul>
 *     <li>Define a {@link SenderPipe} with a FrankSender</li>
 *     <li>Set the attribute {@code scope} to either {@code JVM} for a Java application, or to {@code DLL} for code loaded from a DLL</li>
 *     <li>Set the attribute {@code target} to the service-name the other application used to register itself</li>
 * </ul>
 * <p>
 * In the other application:
 * <ul>
 *     <li>Implement the interface {@code nl.nn.adapterframework.dispatcher.RequestProcessor} from the IbisServiceDispatcher library</li>
 *     <li>Register the instance with the {@code nl.nn.adapterframework.dispatcher.DispatcherManager} obtained via the {@code nl.nn.adapterframework.dispatcher.DispatcherManagerFactory}</li>
 *     <li>See the implementation code of the {@code JavaListener} in the Frank!Framework for an example</li>
 * </ul>
 * </p>
 * <p>
 * See also the repository of the IbisServiceDispatcher:
 *  <a href="https://github.com/frankframework/servicedispatcher">https://github.com/frankframework/servicedispatcher</a>
 * </p>
 *
 * <h4>Using FrankSender to call an adapter from Larva tests</h4>
 * <p>
 * You can configure a FrankSender in Larva property files to use the FrankSender to invoke an adapter to test. When doing this, keep the following in mind:
 * <ul>
 *     <li>If you leave the default scope as {@code ADAPTER}, then the {@code target} property needs to have both configuration name and adapter name, separated by a {@code /} character</li>
 *     <li>When scope is left as default, the receiver and JavaListener are skipped and no transaction is started unless it is set on the adapter's {@code PipeLine}</li>
 *     <li>If you do need a transaction and the adapter has a JavaListener that has {@link org.frankframework.receivers.JavaListener#setServiceName(String)} defined, you can use the FrankSender with scope {@code JVM}
 *     and set the {@code target} attribute to the {@code serviceName} attribute of the {@code JavaListener}.</li>
 * </ul>
 * </p>
 *
 * <h3>Migrating Existing Configurations</h3>
 * <p>
 * When one adapter (named A) needs to call another adapter (named B) like a subroutine, you will usually have an {@link IbisLocalSender} or an {@link IbisJavaSender}
 * in adapter A, and a {@link org.frankframework.receivers.JavaListener} in adapter B.
 * </p>
 * <p>
 *     <em>NB:</em> For the example it is assumed that all adapters are defined in the same configuration.
 * </p>
 *
 * <h4>Example of Existing Configuration</h4>
 * The existing configuration might look like this in the calling adapter:
 * <pre>{@code
 * <module>
 *     <adapter name="Adapter A">
 *         <receiver name="Adapter A Receiver">
 *             <listener name="Adapter A Listener"
 *                 className="org.frankframework..." etc/>
 *         </receiver>
 *  	   <pipeline firstPipe="...">
 *  	       <pipe name="send" className="org.frankframework.pipes.SenderPipe">
 *  	           <sender className="org.frankframework.senders.IbisJavaSender"
 *  	               serviceName="service-Adapter-B" />
 *                 <forward name="success" path="..." />
 *  	       </pipe>
 *         </pipeline>
 *     </adapter>
 * </module>
 * }</pre>
 *
 * Or like using the modern XML XSD and an IbisLocalSender instead:
 * <pre>{@code
 * <Module>
 *     <Adapter name="Adapter A">
 *         <Receiver name="Adapter A Receiver">
 *             ... Listener setup and other configuration
 *         </Receiver>
 *         <Pipeline>
 *             <SenderPipe name="send">
 *                 <IbisLocalSender name="call Adapter B"
 *                     javaListener="Adapter B Listener"/>
 *                 <Forward name="success" path="EXIT" />
 *             </SenderPipe>
 *         </Pipeline>
 *     </Adapter>
 * </Module>
 * }</pre>
 *
 * In the receiving adapter B the listener would have been configured like this:
 * <pre>{@code
 * <Module>
 *     <Adapter name="adapter B">
 *         <Receiver name="Receiver B">
 *             <JavaListener name="Adapter B Listener" serviceName="service-Adapter-B"/>
 *         </Receiver>
 *         <Pipeline>
 *             ...
 *         </Pipeline>
 *     </Adapter>
 * </Module>
 * }</pre>
 * <p/>
 *
 * <h4>Rewritten Example Configuration With FrankSender</h4>
 * This example shows the most simple way of using the FrankSender to call another adapter with least amount of overhead.
 *
 * <pre>{@code
 * <Module>
 *     <Adapter name="Adapter A">
 *         <Receiver name="Adapter A Receiver">
 *             ... Listener setup and other configuration
 *         </Receiver>
 *         <Pipeline>
 *             <SenderPipe name="send">
 *                 <!-- when scope="ADAPTER", then target is directly the name of the adapter you want to call -->
 *                 <FrankSender name="call Adapter C"
 *                     scope="ADAPTER"
 *                     target="adapter B"
 *                 />
 *                 <Forward name="success" path="EXIT" />
 *             </SenderPipe>
 *         </Pipeline>
 *     </Adapter>
 *     <Adapter name="adapter B">
 *         <!-- No receiver needed for FrankSender in this scenario -->
 *         <Pipeline>
 *             ... Exits, Pipes etc
 *         </Pipeline>
 *     </Adapter>
 * </Module>
 * }</pre>
 *
 * <h4>Rewritten Example Configuration With FrankSender and FrankListener</h4>
 * This example shows why you might want to call the other adapter via the FrankListener. This adds a bit more overhead to the call
 * of the sub-adapter for the extra error-handling done by the target receiver.
 *
 * <pre>{@code
 * <Module>
 *    <Adapter name="Adapter A">
 *        <Receiver name="Adapter A Receiver">
 *         ... Listener setup and other configuration
 * 		  </Receiver>
 * 		  <Pipeline>
 *            <SenderPipe name="send">
 *                <!-- when scope="LISTENER", then target is directly the name of the FrankListener in the adapter you want to call -->
 *                <FrankSender
 *                    scope="LISTENER"
 *                    target="Adapter B Listener"/>
 *                <Forward name="success" path="EXIT" />
 *            </SenderPipe>
 *        </Pipeline>
 *     </Adapter>
 *     <Adapter name="adapter B">
 *         <!-- Messages will only be sent to the error storage if:
 *             - The target receiver is not transactional, and has maxTries="0", or
 *             - The target receiver is transaction, and the Sender is set up to retry sending on error
 *             For internal adapters, sending / receiving with retries might not make sense so the example does not show that.
 *         -->
 *         <Receiver name="Receiver B" maxRetries="0" transactionAttribute="NotSupported">
 *             <!-- Listener name is optional, defaults to Adapter name -->
 *             <FrankListener name="Adapter B Listener"/>
 *                 <!-- This adapter now has an error storage -- without Receiver and FrankListener the sub-adapter couldn't have that -->
 *             <JdbcErrorStorage slotId="Adapter B - Errors" />
 *         </Receiver>
 *         <!-- If transactions are required, set transaction-attribute on the Pipeline -->
 *         <Pipeline transactionAttribute="RequiresNew">
 *             ... Exits, Pipes etc
 *         </Pipeline>
 *    </Adapter>
 * </Module>
 * }</pre>
 *
 * @ff.parameter code Determine scope dynamically at runtime. If the parameter value is empty, fall back to the scope configured via the attribute, or the default scope {@code ADAPTER}.
 * @ff.parameter target Determine target dynamically at runtime. If the parameter value is empty, fall back to the target configured via the attribute.
 * @ff.parameters All parameters except {@code scope} and {@code target} are copied to the {@link PipeLineSession} of the adapter called.
 */
@DestinationType(Type.ADAPTER)
@Forward(name = "*", description = "Exit code")
@Category(Category.Type.BASIC)
public class FrankSender extends AbstractSenderWithParameters implements HasPhysicalDestination, IThreadCreator, ConfigurationAware {

	public static final String TARGET_PARAM_NAME = "target";
	public static final String SCOPE_PARAM_NAME = "scope";
	public static final String TEST_TOOL_LISTENER_PREFIX = "testtool";

	/**
	 * Scope for {@link FrankSender} call: Another Frank!Framework Adapter, or another Java application running in the same JVM.
	 * <ul>
	 * <li>{@code ADAPTER} is the most efficient, low-overhead method to call another adapter directly</li>
	 * <li>{@code LISTENER} calls another adapter via a configured {@link FrankListener}. Use this when you need all message logging and error handling that is
	 * in the {@link org.frankframework.receivers.Receiver}.</li>
	 * <li>{@code JVM} is the regular way of invoking another Java application in the same JVM.</li>
	 * <li>{@code DLL} is a special way of invoking the other application, calling code loaded from a DLL that has been registered as service.</li>
	 * </ul>
	 *
	 * For {@code JVM} and {@code DLL}, see the documentation of the <a href="https://github.com/frankframework/servicedispatcher">IbisServiceDispatcher</a> library
	 * for further details on how implement a Java program or DLL running alongside the Frank!Framework that can be called from the Frank!Framework.
	 *
	 * <h4>See</h4>
	 *  <a href="https://github.com/frankframework/servicedispatcher">https://github.com/frankframework/servicedispatcher</a>
	 */
	public enum Scope { JVM, DLL, ADAPTER, LISTENER }

	private @Getter Scope scope = Scope.ADAPTER;
	private @Getter String target;
	private @Getter String returnedSessionKeys=""; // do not initialize with null, returned session keys must be set explicitly

	private @Getter boolean synchronous=true;

	private @Autowired @Setter IsolatedServiceCaller isolatedServiceCaller;
	private @Autowired(required = false) @Setter ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener;
	private @Setter Configuration configuration;
	private @Autowired @Setter IbisManager ibisManager;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		ParameterList pl = getParameterList();
		if (StringUtils.isBlank(getTarget()) && (pl == null || !pl.hasParameter(TARGET_PARAM_NAME))) {
			throw new ConfigurationException("[target] required, either as parameter or as attribute in the configuration");
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
		if (pl.hasParameter(SCOPE_PARAM_NAME)) {
			result.append("param:scope");
		} else {
			result.append(getScope());
		}
		result.append("/");
		if (pl.hasParameter(TARGET_PARAM_NAME)) {
			result.append("param:target");
		} else {
			result.append(getTarget());
		}
		return result.toString();
	}

	@Override
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
		ParameterValueList pvl = getParameterValueList(message, session);
		Scope actualScope = determineActualScope(pvl);
		String actualTarget = determineActualTarget(pvl);
		log.info("Sending message to {} [{}]", ()->actualScope, ()->actualTarget);
		ServiceClient serviceClient = switch (actualScope) {
			case ADAPTER -> getAdapterServiceClient(actualTarget);
			case LISTENER -> getFrankListener(actualTarget);
			case JVM, DLL -> getJvmDispatcherServiceClient(actualScope, actualTarget);
		};
		return invokeService(serviceClient, actualScope, actualTarget, message, session, pvl);
	}

	private SenderResult invokeService(ServiceClient serviceClient, Scope scope, String target, Message message, PipeLineSession parentSession, ParameterValueList pvl) throws SenderException, TimeoutException {
		try (PipeLineSession childSession = new PipeLineSession()) {
			setupChildSession(parentSession, pvl, childSession);

			Message resultMessage = doCallService(serviceClient, scope, target, message, parentSession, childSession);
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
		if (!result.isSuccess()) {
			result.setErrorMessage("exitState=" + exitState);
		}
		return result;
	}

	private Message doCallService(ServiceClient serviceClient, Scope scope, String target, Message message, PipeLineSession parentSession, PipeLineSession childSession) throws TimeoutException, SenderException {
		try {
			if (isSynchronous()) {
				return serviceClient.processRequest(message, childSession);
			} else {
				isolatedServiceCaller.callServiceAsynchronous(serviceClient, message, childSession, threadLifeCycleEventListener);
				return Message.nullMessage();
			}
		} catch (ListenerException | IOException e) {
			if (ExceptionUtils.getRootCause(e) instanceof TimeoutException) {
				throw new TimeoutException("timeout calling " + scope + " [" + target + "]",e);
			}
			throw new SenderException("exception calling " + scope + " [" + target + "]",e);
		} finally {
			if (StringUtils.isNotEmpty(getReturnedSessionKeys())) {
				log.debug("returning values of session keys [{}]", getReturnedSessionKeys());
			}

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
		childSession.put(PipeLineSession.MESSAGE_ID_KEY, MessageUtils.generateMessageId());
	}

	private ServiceClient getJvmDispatcherServiceClient(Scope scope, String target) throws SenderException {
		DispatcherManager dm = getDispatcherManager(scope == Scope.DLL);
		return (message, session) -> {
			try {
				return new Message(dm.processRequest(target, session.getCorrelationId(), message.asString(), session));
			} catch (Exception e) {
				throw new ListenerException("Exception sending message to [" + target + "]", e);
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
			// TODO: The code here seems rather dubious actually. Check if we really want to keep this special behaviour on having the text "ListenerException" in our error message.
			if (plr.getState() == PipeLine.ExitState.ERROR) {
				// Adapter.processMessageDirect() catches ListenerException and makes an error message from it, but
				// we want to actually throw it.
				String errorResult;
				try {
					errorResult = plr.getResult().asString();
				} catch (IOException e) {
					throw new ListenerException("Call resulted in error, but cannot get error message:", e);
				}
				if (errorResult != null && errorResult.contains("ListenerException")) {
					throw new ListenerException(errorResult);
				}
			}
			return plr.getResult();
		};
	}

	protected @Nonnull ServiceClient getFrankListener(@Nonnull String target) throws SenderException {
		String fullFrankListenerName = getFullFrankListenerName(target);
		ServiceClient result = FrankListener.getListener(fullFrankListenerName);
		if (result == null) {
			throw new SenderException(getLogPrefix() + "Listener [" + target + "] not found");
		}
		return result;
	}

	private @Nonnull String getFullFrankListenerName(@Nonnull String target) {
		int configNameSeparator = target.indexOf('/');
		if (configuration == null || configNameSeparator > 0) {
			return target;
		} else if (configNameSeparator == 0) {
			return configuration.getName() + target;
		} else {
			return configuration.getName() + "/" + target;
		}
	}

	@Nonnull
	protected Adapter findAdapter(String target) throws SenderException {
		Configuration actualConfiguration;
		String adapterName;
		int configNameSeparator = target.indexOf('/');
		if (configNameSeparator > 0) {
			adapterName = target.substring(configNameSeparator + 1);
			String configurationName = target.substring(0, configNameSeparator);
			actualConfiguration = ibisManager.getConfiguration(configurationName);
			if (actualConfiguration == null) {
				throw new SenderException("Configuration [" + configurationName + "] not found");
			}
		} else if (configNameSeparator == 0) {
			adapterName = target.substring(1);
			actualConfiguration = configuration;
		} else {
			adapterName = target;
			actualConfiguration = configuration;
		}
		Adapter adapter = actualConfiguration.getRegisteredAdapter(adapterName);
		if (adapter == null) {
			throw new SenderException("Cannot find adapter specified by [" + target + "]");
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
	 * Set to {@code false} to make the call asynchronously. This means that the current adapter
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
	 * @ff.default ADAPTER
	 */
	public void setScope(Scope scope) {
		this.scope = scope;
	}

	/**
	 * Target: service-name of service in other application that should be called, or name of adapter to be called.
	 * If the adapter is in another configuration, prefix the adapter name with the name of that configuration and a slash ("{@code /}").
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

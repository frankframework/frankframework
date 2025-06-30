/*
   Copyright 2013 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
package org.frankframework.receivers;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;

import nl.nn.adapterframework.dispatcher.DispatcherManagerFactory;
import nl.nn.adapterframework.dispatcher.RequestProcessor;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.IMessageHandler;
import org.frankframework.core.IPushingListener;
import org.frankframework.core.IbisExceptionListener;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.RequestReplyListener;
import org.frankframework.doc.Category;
import org.frankframework.doc.Mandatory;
import org.frankframework.errormessageformatters.ErrorMessageFormatter;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.senders.IbisJavaSender;
import org.frankframework.senders.IbisLocalSender;
import org.frankframework.stream.Message;
import org.frankframework.util.LogUtil;


// TODO: When anchors are supported by the Frank!Doc, link to https://github.com/frankframework/servicedispatcher
/**
 * Use this listener to receive messages from other adapters or a scheduler within the same Frank-application or from other components residing in the same JVM.
 * JavaListeners can receive calls made via de ibis-servicedispatcher, which should be located on the JVM classpath to receive calls from other components in the JVM. If you want to call an adapter in the same Frank-application, consider using the IbisLocalSender.
 * <br/>
 * To understand what this listener does exactly, please remember that the Frank!Framework is a Java application.
 * The JavaListener listens to Java method calls. You can issue Java method calls using a {@link IbisJavaSender} (external call)
 * or {@link IbisLocalSender} (internal call).
 * For more information see the ibis-servicedispatcher project.
 *
 * @author  Gerrit van Brakel
 */
@Category(Category.Type.BASIC)
public class JavaListener<M> implements RequestReplyListener, IPushingListener<M>, RequestProcessor, HasPhysicalDestination, ServiceClient {

	private final @Getter String domain = "JVM";
	protected Logger log = LogUtil.getLogger(this);
	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;
	private @Getter @Setter ExceptionHandlingMethod exceptionHandlingMethod = ExceptionHandlingMethod.RETHROW;

	private @Getter String name;
	private @Getter String serviceName;
	private @Getter String returnedSessionKeys=null;
	private @Getter boolean httpWsdl = false;

	private @Getter boolean open=false;
	private static Map<String, JavaListener<?>> registeredListeners;
	private @Getter @Setter IMessageHandler<M> handler;

	@Override
	public void configure() throws ConfigurationException {
		if (handler==null) {
			throw new ConfigurationException("handler has not been set");
		}
	}

	@Override
	public synchronized void start() {
		try {
			// add myself to local list so that IbisLocalSenders can find me
			registerListener();

			// add myself to global list so that other applications in this JVM (like Everest Portal) can find me.
			// (performed only if serviceName is not empty
			if (StringUtils.isNotEmpty(getServiceName())) {
				DispatcherManagerFactory.getDispatcherManager().register(getServiceName(), this);
			}

			open = true;
		} catch (Exception e) {
			throw new LifecycleException("error occurred while starting listener [" + getName() + "]", e);
		}
	}

	@Override
	public synchronized void stop() {
		open = false;

		try {
			// unregister from local list
			unregisterListener();

			// unregister from global list
			if (StringUtils.isNotEmpty(getServiceName())) {
				DispatcherManagerFactory.getDispatcherManager().unregister(getServiceName());
			}
		} catch (Exception e) {
			throw new LifecycleException("error occurred while stopping listener [" + getName() + "]", e);
		}
	}

	@Override
	public RawMessageWrapper<M> wrapRawMessage(M rawMessage, PipeLineSession session) {
		return new RawMessageWrapper<>(rawMessage, session.getMessageId(), session.getCorrelationId());
	}

	// ### RequestProcessor
	@Override
	@SuppressWarnings("unchecked")
	public String processRequest(String correlationId, String rawMessage, HashMap context) throws ListenerException {
		try (PipeLineSession processContext = new PipeLineSession()) {
			if (context != null) {
				processContext.putAll(context);
			}
			processContext.put(PipeLineSession.CORRELATION_ID_KEY, correlationId);
			try (Message message = new Message(rawMessage);
				Message result = processRequest(new MessageWrapper<>(message, null, correlationId), processContext)) {
				return result.asString();
			} finally {
				if (context != null) {
					context.putAll(processContext);
					processContext.values().stream()
							.filter(AutoCloseable.class::isInstance)
							.map(AutoCloseable.class::cast)
							.forEach(processContext::unscheduleCloseOnSessionExit);
				}
			}
		} catch (IOException e) {
			throw new ListenerException("cannot convert stream", e);
		}
	}

	// ### ServiceClient
	@Override
	public Message processRequest(Message message, @Nonnull PipeLineSession session) throws ListenerException {
		MessageWrapper<M> messageWrapper = new MessageWrapper<>(message, session.getMessageId(), session.getCorrelationId());
		Message response = processRequest(messageWrapper, session);
		response.closeOnCloseOf(session);
		return  response;
	}

	private Message processRequest(@Nonnull MessageWrapper<M> messageWrapper, @Nonnull PipeLineSession parentSession) throws ListenerException {
		if (!isOpen()) {
			throw new ListenerException("JavaListener [" + getName() + "] is not opened");
		}
		log.debug("JavaListener [{}] processing correlationId [{}]", this::getName, messageWrapper::getCorrelationId);

		try (PipeLineSession session = new PipeLineSession(parentSession)) {
			Message message = messageWrapper.getMessage();

			try {
				return handler.processRequest(this, messageWrapper, session);
			} finally {
				session.unscheduleCloseOnSessionExit(message); // The input message should not be managed by this PipelineSession but rather the method invoker
				session.mergeToParentSession(getReturnedSessionKeys(), parentSession);
			}
		}
	}

	/**
	 * Register listener so that it can be used by a proxy
	 */
	private void registerListener() {
		getListeners().put(getName(), this);
	}

	private void unregisterListener() {
		getListeners().remove(getName());
	}

	/**
	 * Returns JavaListener registered under the given name
	 */
	public static JavaListener<?> getListener(String name) {
		return getListeners().get(name);
	}

	/**
	 * Get all registered JavaListeners
	 */
	private static synchronized Map<String, JavaListener<?>> getListeners() {
		if (registeredListeners == null) {
			registeredListeners = Collections.synchronizedMap(new HashMap<>());
		}
		return registeredListeners;
	}

	public static Set<String> getListenerNames() {
		return getListeners().keySet();
	}

	@Override
	public void setExceptionListener(IbisExceptionListener listener) {
		// do nothing, no exceptions known
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<M> rawMessage, PipeLineSession pipeLineSession) throws ListenerException {
		// do nothing
	}

	@Override
	public Message extractMessage(@Nonnull RawMessageWrapper<M> rawMessage, @Nonnull Map<String, Object> context) throws ListenerException {
		return Message.asMessage(rawMessage.getRawMessage());
	}

	@Override
	public String getPhysicalDestinationName() {
		if (StringUtils.isNotEmpty(getServiceName())) {
			return "external: "+getServiceName();
		}
		return "internal: "+getName();
	}

	/** Internal name of the listener, as known to the adapter. An IbisLocalSender refers to this name in its <code>javaListener</code>-attribute. */
	@Override
	@Mandatory
	public void setName(String name) {
		this.name = name;
	}

	/** External Name of the listener. An IbisJavaSender refers to this name in its <code>serviceName</code>-attribute. */
	public void setServiceName(String jndiName) {
		this.serviceName = jndiName;
	}

	/**
	 * Comma separated list of keys of session variables that should be returned to caller, for correct results as well as for erroneous results.
	 * If not set (not even to an empty value), all session keys can be returned.
	 * @ff.default all session keys can be returned
	 */
	public void setReturnedSessionKeys(String string) {
		returnedSessionKeys = string;
	}

	/**
	 * Should the JavaListener throw a ListenerException when it occurs or return an error message.
	 * Please consider using an {@link ErrorMessageFormatter} instead of disabling Exception from being thrown.
	 * 
	 * @ff.default true
	 */
	@Deprecated(since = "9.2")
	@ConfigurationWarning("Replaced with 'exceptionHandlingMethod'")
	public void setThrowException(boolean throwException) {
		this.exceptionHandlingMethod = throwException ? ExceptionHandlingMethod.RETHROW : ExceptionHandlingMethod.FORMAT_AND_RETURN;
	}

	/**
	 * If <code>true</code>, the WSDL of the service provided by this listener will available for download
	 * @ff.default false
	 */
	public void setHttpWsdl(boolean httpWsdl) {
		this.httpWsdl = httpWsdl;
	}
}

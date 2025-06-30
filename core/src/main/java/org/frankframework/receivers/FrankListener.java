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
package org.frankframework.receivers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.ConfigurationAware;
import org.frankframework.core.Adapter;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.IMessageHandler;
import org.frankframework.core.IPushingListener;
import org.frankframework.core.IbisExceptionListener;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.RequestReplyListener;
import org.frankframework.doc.Category;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.stream.Message;


/**
 * Listener to receive messages sent by the {@link org.frankframework.senders.FrankSender}, for situations where
 * calling an {@link Adapter} directly is not desired. This could be because message / error logging is required for messages
 * sent to the subadapter.
 * <br/>
 * See the {@link org.frankframework.senders.FrankSender} documentation for more information.
 */
@Category(Category.Type.BASIC)
@Log4j2
public class FrankListener implements RequestReplyListener, IPushingListener<Message>, HasPhysicalDestination, ServiceClient, ConfigurationAware {

	private static final ConcurrentMap<String, FrankListener> listeners = new ConcurrentHashMap<>();

	private final @Getter String domain = "JVM";
	private @Setter @Getter ApplicationContext applicationContext;
	private @Setter Configuration configuration;
	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ExceptionHandlingMethod exceptionHandlingMethod = ExceptionHandlingMethod.RETHROW;

	private @Getter String name;
	private String fullName;

	private @Getter boolean open=false;
	private @Getter @Setter IMessageHandler<Message> handler;

	public static @Nullable FrankListener getListener(String name) {
		return listeners.get(name);
	}

	@Override
	public String getPhysicalDestinationName() {
		return fullName;
	}

	@Override
	public void setExceptionListener(IbisExceptionListener listener) {
		// Do nothing, does not throw exceptions for receiver
	}

	@Override
	public MessageWrapper<Message> wrapRawMessage(Message rawMessage, PipeLineSession session) {
		return new MessageWrapper<>(rawMessage, session.getMessageId(), session.getCorrelationId());
	}

	@Override
	public void configure() {
		if (StringUtils.isBlank(getName())) {
			Adapter adapter = getAdapter();
			setName(adapter.getName());
			log.debug("name was not configured, defaulting to adapter name [{}]", this::getName);
		}
		fullName = getNamePrefix() + getName();
		log.debug("FrankListener instance will be registered under full name [{}]", fullName);
	}

	private String getNamePrefix() {
		if (configuration != null) {
			return configuration.getName() + "/";
		}
		log.debug("FrankListener without Configuration, will assume it is running Larva test-tool. No prefix will be used, given name should be unique.");
		return "";
	}

	// TODO this should be the ApplicationContext...?
	private Adapter getAdapter() {
		return ((Receiver<?>) getHandler()).getAdapter();
	}

	@Override
	public void start() {
		FrankListener putResult = listeners.putIfAbsent(fullName, this);
		if (putResult != null && putResult != this) {
			throw new LifecycleException("Duplicate registration [" + fullName + "] for adapter [" + getAdapter().getName() + "], FrankListener [" + getName() + "]");
		}
		open = true;
	}

	@Override
	public void stop() {
		listeners.remove(fullName);
		open = false;
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<Message> rawMessage, PipeLineSession pipeLineSession) {
		// Do nothing
	}

	@Override
	public Message extractMessage(@Nonnull RawMessageWrapper<Message> rawMessage, @Nonnull Map<String, Object> context) {
		return rawMessage.getRawMessage();
	}

	/**
	 * Name of the listener by which it can be found by the {@link org.frankframework.senders.FrankSender}. If this
	 * is not configured, the name will default to the name of the {@link org.frankframework.core.Adapter}.
	 * The name of the {@code FrankListener} must be unique across the configuration.
	 *
	 * @param name Name of the listener. If not set, will default to {@link org.frankframework.core.Adapter} name.
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public Message processRequest(Message message, PipeLineSession session) throws ListenerException {
		if (!isOpen()) {
			throw new ListenerException("JavaListener [" + getName() + "] is not opened");
		}

		return getHandler().processRequest(this, wrapRawMessage(message, session), session);
	}
}

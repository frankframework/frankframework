/*
   Copyright 2013-2019 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
package org.frankframework.http;

import java.util.Map;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.IMessageHandler;
import org.frankframework.core.IPushingListener;
import org.frankframework.core.IbisExceptionListener;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.RequestReplyListener;
import org.frankframework.doc.Protected;
import org.frankframework.receivers.MessageWrapper;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.receivers.Receiver;
import org.frankframework.receivers.ServiceClient;
import org.frankframework.stream.Message;
import org.frankframework.util.LogUtil;

/**
 * Baseclass of a {@link IPushingListener IPushingListener} that enables a {@link Receiver}
 * to receive messages from Servlets.
 *
 * @author  Gerrit van Brakel
 * @since   4.12
 */
public class PushingListenerAdapter implements RequestReplyListener, IPushingListener<Message>, ServiceClient {
	protected Logger log = LogUtil.getLogger(this);

	private @Getter String name;
	private @Getter boolean running;
	private @Getter @Setter ExceptionHandlingMethod onException = ExceptionHandlingMethod.RETHROW;

	private IMessageHandler<Message> handler;

	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;

	/**
	 * initialize listener and register <code>this</code> to the JNDI
	 */
	@Override
	public void configure() throws ConfigurationException {
		if (handler==null) {
			throw new ConfigurationException("handler has not been set");
		}
	}

	@Override
	public void start() {
		setRunning(true);
	}

	@Override
	public void stop() {
		setRunning(false);
	}


	@Override
	public RawMessageWrapper<Message> wrapRawMessage(Message rawMessage, PipeLineSession session) {
		return new RawMessageWrapper<>(rawMessage, session.getMessageId(), session.getCorrelationId());
	}

	@Override
	public Message extractMessage(@Nonnull RawMessageWrapper<Message> rawMessage, @Nonnull Map<String, Object> context) {
		return rawMessage.getRawMessage();
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<Message> rawMessage, PipeLineSession pipeLineSession) {
		// descendants can override this method when specific actions are required
	}

	@Override
	public Message processRequest(Message rawMessage, PipeLineSession session) throws ListenerException {
		MessageWrapper<Message> messageWrapper = new MessageWrapper<>(rawMessage, session.getMessageId(), session.getCorrelationId());
		try {
			log.debug("PushingListenerAdapter.processRequest() for correlationId [{}]", session::getCorrelationId);
			return handler.processRequest(this, messageWrapper, session);
		} finally {
			ThreadContext.clearAll();
		}
	}

	@Override
	public String toString() {
		// Including the handler causes StackOverflowExceptions on Receiver.toString() which also prints the listener
		return ReflectionToStringBuilder.toStringExclude(this, "handler");
	}

	/**
	 * Name of the listener as known to the adapter
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void setHandler(IMessageHandler<Message> handler) {
		this.handler=handler;
	}

	@Override
	public void setExceptionListener(IbisExceptionListener exceptionListener) {
		//	Not Implemented
	}

	@Deprecated(since = "9.2")
	@ConfigurationWarning("Replaced with 'exceptionHandlingMethod'")
	public void setApplicationFaultsAsExceptions(boolean applicationFaultsAsExceptions) {
		this.onException = applicationFaultsAsExceptions ? ExceptionHandlingMethod.RETHROW : ExceptionHandlingMethod.FORMAT_AND_RETURN;
	}

	@Protected
	public void setRunning(boolean running) {
		this.running = running;
	}
}

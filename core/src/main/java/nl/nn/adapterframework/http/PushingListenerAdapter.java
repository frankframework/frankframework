/*
   Copyright 2013, 2017, 2019 Nationale-Nederlanden, 2020, 2022-2023 WeAreFrank!

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
package nl.nn.adapterframework.http;

import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IPushingListener;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.doc.Protected;
import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.receivers.ServiceClient;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Baseclass of a {@link IPushingListener IPushingListener} that enables a {@link Receiver}
 * to receive messages from Servlets.
 * </table>
 * @author  Gerrit van Brakel
 * @since   4.12
 */
public abstract class PushingListenerAdapter implements IPushingListener<Message>, ServiceClient {
	protected Logger log = LogUtil.getLogger(this);

	private @Getter String name;
	private @Getter boolean applicationFaultsAsExceptions=true;
	private @Getter boolean running;

	private IMessageHandler<Message> handler;

	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
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
	public void open() throws ListenerException {
		setRunning(true);
	}
	@Override
	public void close() {
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
	public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<Message> rawMessage, PipeLineSession pipeLineSession) throws ListenerException {
		// descendants can override this method when specific actions are required
	}

	@Override
	public Message processRequest(Message rawMessage, PipeLineSession session) throws ListenerException {
		RawMessageWrapper<Message> rawMessageWrapper = new RawMessageWrapper<>(rawMessage, session.getMessageId(), session.getCorrelationId());
		// NB: This seems pointless, but I guess that a subclass could override extractMessage() and make it do something more revolutionary.
		Message message = extractMessage(rawMessageWrapper, session);
		try {
			log.debug("PushingListenerAdapter.processRequest() for correlationId [{}]", session::getCorrelationId);
			return handler.processRequest(this, rawMessageWrapper, message, session);
		} catch (ListenerException e) {
			if (isApplicationFaultsAsExceptions()) {
				log.debug("PushingListenerAdapter.processRequest() rethrows ListenerException...");
				throw e;
			}
			log.debug("PushingListenerAdapter.processRequest() formats ListenerException to errormessage");
			String correlationId = session.getCorrelationId();
			return handler.formatException(null,correlationId, message, e);
		} finally {
			ThreadContext.clearAll();
		}
	}


	@Override
	public String toString() {
		//Including the handler causes StackOverflowExceptions on Receiver.toString() which also prints the listener
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
//		this.exceptionListener=exceptionListener;
	}

	public void setApplicationFaultsAsExceptions(boolean b) {
		applicationFaultsAsExceptions = b;
	}

	@Protected
	public void setRunning(boolean running) {
		this.running = running;
	}
}

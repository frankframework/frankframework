/*
   Copyright 2013, 2017, 2019 Nationale-Nederlanden, 2020 WeAreFrank!

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

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IPushingListener;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.receivers.ServiceClient;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Baseclass of a {@link IPushingListener IPushingListener} that enables a {@link nl.nn.adapterframework.receivers.Receiver}
 * to receive messages from Servlets.
 * </table>
 * @author  Gerrit van Brakel 
 * @since   4.12
 */
public abstract class PushingListenerAdapter implements IPushingListener<Message>, ServiceClient {
	protected Logger log = LogUtil.getLogger(this);
	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;

	private IMessageHandler<Message> handler;
	private String name;
	private boolean applicationFaultsAsExceptions=true;
	private boolean running;

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
	public String getIdFromRawMessage(Message rawMessage, Map<String, Object> threadContext) {
		return null;
	}
	@Override
	public Message extractMessage(Message rawMessage, Map<String, Object> threadContext) {
		return rawMessage;
	}
	@Override
	public void afterMessageProcessed(PipeLineResult processResult, Object rawMessageOrWrapper, Map<String, Object> threadContext) throws ListenerException {
		// descendants can override this method when specific actions are required
	}

	@Override
	public Message processRequest(String correlationId, Message rawMessage, Map<String, Object> requestContext) throws ListenerException {
		Message message = extractMessage(rawMessage, requestContext);
		try {
			log.debug("PushingListenerAdapter.processRequerawMmessagest() for correlationId ["+correlationId+"]");
			return handler.processRequest(this, correlationId, rawMessage, message, requestContext);
		} catch (ListenerException e) {
			if (isApplicationFaultsAsExceptions()) {
				log.debug("PushingListenerAdapter.processRequest() rethrows ListenerException...");
				throw e;
			} 
			log.debug("PushingListenerAdapter.processRequest() formats ListenerException to errormessage");
			return handler.formatException(null,correlationId, message, e);
		}
	}


	@Override
	public String toString() {
		//Including the handler causes StackOverflowExceptions on Receiver.toString() which also prints the listener
		return ReflectionToStringBuilder.toStringExclude(this, "handler");
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	@IbisDoc({"name of the listener as known to the adapter", ""})
	public void setName(String name) {
		this.name=name;
	}

	@Override
	public void setHandler(IMessageHandler<Message> handler) {
		this.handler=handler;
	}
	@Override
	public void setExceptionListener(IbisExceptionListener exceptionListener) {
//		this.exceptionListener=exceptionListener;
	}

	public boolean isApplicationFaultsAsExceptions() {
		return applicationFaultsAsExceptions;
	}
	public void setApplicationFaultsAsExceptions(boolean b) {
		applicationFaultsAsExceptions = b;
	}

	public boolean isRunning() {
		return running;
	}
	public void setRunning(boolean running) {
		this.running = running;
	}
}

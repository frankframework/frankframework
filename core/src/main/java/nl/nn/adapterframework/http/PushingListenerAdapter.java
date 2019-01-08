/*
   Copyright 2013, 2017 Nationale-Nederlanden

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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.IPushingListener;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.receivers.ServiceClient;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

/**
 * Baseclass of a {@link IPushingListener IPushingListener} that enables a {@link nl.nn.adapterframework.receivers.GenericReceiver}
 * to receive messages from Servlets.
 * </table>
 * @author  Gerrit van Brakel 
 * @since   4.12
 */
public class PushingListenerAdapter implements IPushingListener, ServiceClient {
	protected Logger log = LogUtil.getLogger(this);

	private IMessageHandler handler;
	private String name;
	private boolean applicationFaultsAsExceptions=true;
//	private IbisExceptionListener exceptionListener;
	private boolean running;

	/**
	 * initialize listener and register <code>this</code> to the JNDI
	 */
	public void configure() throws ConfigurationException {
		if (handler==null) {
			throw new ConfigurationException("handler has not been set");
		}
	}

	public void open() throws ListenerException {
		setRunning(true);
	}
	public void close() {
		setRunning(false);
	}


	public String getIdFromRawMessage(Object rawMessage, Map threadContext) {
		return null;
	}
	public String getStringFromRawMessage(Object rawMessage, Map threadContext) {
		return (String) rawMessage;
	}
	public void afterMessageProcessed(PipeLineResult processResult, Object rawMessage, Map threadContext) throws ListenerException {
	}

	@Override
	public String processRequest(String correlationId, String message, Map requestContext) throws ListenerException {
		try {
			log.debug("PushingListenerAdapter.processRequest() for correlationId ["+correlationId+"]");
			return handler.processRequest(this, correlationId, message, requestContext);
		} catch (ListenerException e) {
			if (isApplicationFaultsAsExceptions()) {
				log.debug("PushingListenerAdapter.processRequest() rethrows ListenerException...");
				throw e;
			} 
			log.debug("PushingListenerAdapter.processRequest() formats ListenerException to errormessage");
			return handler.formatException(null,correlationId, message,e);
		}
	}


	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public String getName() {
		return name;
	}

	@IbisDoc({"name of the listener as known to the adapter", ""})
	public void setName(String name) {
		this.name=name;
	}

	public void setHandler(IMessageHandler handler) {
		this.handler=handler;
	}
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

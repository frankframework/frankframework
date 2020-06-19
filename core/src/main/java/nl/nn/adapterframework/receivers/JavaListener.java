/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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
package nl.nn.adapterframework.receivers;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.IPushingListener;
import nl.nn.adapterframework.core.ISecurityHandler;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.dispatcher.DispatcherManagerFactory;
import nl.nn.adapterframework.dispatcher.RequestProcessor;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.http.HttpSecurityHandler;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.LogUtil;


/** *
 * The JavaListener listens to java requests.
 *
 * @author  Gerrit van Brakel
 */
public class JavaListener implements IPushingListener<String>, RequestProcessor, HasPhysicalDestination {
	protected Logger log = LogUtil.getLogger(this);

	private String name;
	private String serviceName;
	private boolean isolated=false;
	private boolean synchronous=true;
	private boolean opened=false;
	private boolean throwException = true;
	private boolean httpWsdl = false;

	private static Map<String, JavaListener> registeredListeners;
	private IMessageHandler<String> handler;

	@Override
	public void configure() throws ConfigurationException {
		if (isIsolated()) {
			throw new ConfigurationException("function of attribute 'isolated' is replaced by 'transactionAttribute' on PipeLine");
		}
		try {
			if (handler==null) {
				throw new ConfigurationException("handler has not been set");
			}
			if (StringUtils.isEmpty(getName())) {
				throw new ConfigurationException("name has not been set");
			}
		}
		catch (Exception e){
			throw new ConfigurationException(e);
		}
	}

	@Override
	public synchronized void open() throws ListenerException {
		try {
			// add myself to local list so that IbisLocalSenders can find me
			registerListener();

//			// display transaction status, to force caching of UserTransaction.
//			// UserTransaction is not found in context if looked up from javalistener.
//			try {
//				String transactionStatus = JtaUtil.displayTransactionStatus();
//				log.debug("transaction status at startup ["+transactionStatus+"]");
//			} catch (Exception e) {
//				log.warn("could not get transaction status at startup",e);
//			}

			// add myself to global list so that other applications in this JVM (like Everest Portal) can find me.
			// (performed only if serviceName is not empty
			if (StringUtils.isNotEmpty(getServiceName())) {
				DispatcherManagerFactory.getDispatcherManager().register(getServiceName(), this);
			}
			opened=true;
		} catch (Exception e) {
			throw new ListenerException("error occured while starting listener [" + getName() + "]", e);
		}
	}

	@Override
	public synchronized void close() throws ListenerException {
		opened=false;
		try {
			// unregister from local list
			unregisterListener();
			// unregister from global list
			if (StringUtils.isNotEmpty(getServiceName())) {
				// Current DispatcherManager (version 1.3) doesn't have an
				// unregister method, instead a call to register with a null
				// value is done.
				DispatcherManagerFactory.getDispatcherManager().register(getServiceName(), null);
			}
		}
		catch (Exception e) {
			throw new ListenerException("error occured while stopping listener [" + getName() + "]", e);
		}
	}

	@Override
	public String processRequest(String correlationId, String rawMessage, HashMap context) throws ListenerException {
		if (!isOpen()) {
			throw new ListenerException("JavaListener [" + getName() + "] is not opened");
		}
		if (log.isDebugEnabled()) {
			log.debug("JavaListener [" + getName() + "] processing correlationId [" + correlationId + "]");
		}
		if (context != null) {
			Object object = context.get("httpRequest");
			if (object != null) {
				if (object instanceof HttpServletRequest) {
					ISecurityHandler securityHandler = new HttpSecurityHandler((HttpServletRequest)object);
					context.put(IPipeLineSession.securityHandlerKey, securityHandler);
				} else {
					log.warn("No securityHandler added for httpRequest [" + object.getClass() + "]");
				}
			}
		}
		Message message =  new Message(rawMessage);
		if (throwException) {
			try {
				return handler.processRequest(this, correlationId, rawMessage, message, (Map<String,Object>)context).asString();
			} catch (IOException e) {
				throw new ListenerException("cannot convert stream", e);
			}
		} else {
			try {
				return handler.processRequest(this, correlationId, rawMessage, message, context).asString();
			}
			catch (ListenerException | IOException e) {
				return handler.formatException(null,correlationId, message, e);
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
	public static JavaListener getListener(String name) {
		return (JavaListener)getListeners().get(name);
	}

	/**
	 * Get all registered JavaListeners
	 */
	private static synchronized Map<String, JavaListener> getListeners() {
		if (registeredListeners == null) {
			registeredListeners = Collections.synchronizedMap(new HashMap<String,JavaListener>());
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
	public void afterMessageProcessed(PipeLineResult processResult, Object rawMessage, Map<String,Object> context) throws ListenerException {
		// do nothing
	}


	@Override
	public String getIdFromRawMessage(String rawMessage, Map<String,Object> context) throws ListenerException {
		// do nothing
		return null;
	}

	@Override
	public Message extractMessage(String rawMessage, Map<String,Object> context) throws ListenerException {
		return new Message(rawMessage);
	}

	@Override
	public String getPhysicalDestinationName() {
		if (StringUtils.isNotEmpty(getServiceName())) {
			return "external: "+getServiceName();
		} else {
			return "internal: "+getName();
		}
	}


	/**
	 * The <code>toString()</code> method retrieves its value
	 * by reflection.
	 * @see org.apache.commons.lang.builder.ToStringBuilder#reflectionToString
	 *
	 **/
	@Override
	public String toString() {
		return super.toString();
		// This gives stack overflows:
		// return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public void setHandler(IMessageHandler<String> handler) {
		this.handler = handler;
	}
	public IMessageHandler<String> getHandler() {
		return handler;
	}

	public void setServiceName(String jndiName) {
		this.serviceName = jndiName;
	}
	public String getServiceName() {
		return serviceName;
	}

	@IbisDoc({"name of the listener as known to the adapter. an {@link nl.nn.adapterframework.pipes.ibislocalsender ibislocalsender} refers to this name in its <code>javalistener</code>-attribute.", ""})
	@Override
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
	}

	public void setLocal(String name) {
		throw new RuntimeException("do not set attribute 'local=true', just leave serviceName empty!");
	}

	public void setIsolated(boolean b) {
		isolated = b;
	}
	public boolean isIsolated() {
		return isolated;
	}

	@IbisDoc({" when set <code>false</code>, the request is executed asynchronously. this implies <code>isolated=true</code>. n.b. be aware that there is no limit on the number of threads generated", "true"})
	public void setSynchronous(boolean b) {
		synchronous = b;
	}
	public boolean isSynchronous() {
		return synchronous;
	}

	public synchronized boolean isOpen() {
		return opened;
	}

	@IbisDoc({"should the javalistener throw a listenerexception when it occurs or return an error message", "<code>true</code>"})
	public void setThrowException(boolean throwException) {
		this.throwException = throwException;
	}
	public boolean isThrowException() {
		return throwException;
	}
	
	@IbisDoc({"when <code>true</code>, the wsdl of the service provided by this listener is available for download ", "<code>false</code>"})
	public void setHttpWsdl(boolean httpWsdl) {
		this.httpWsdl = httpWsdl;
	}
	public boolean isHttpWsdl() {
		return httpWsdl;
	}
}

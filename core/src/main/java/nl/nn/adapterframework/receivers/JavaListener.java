/*
   Copyright 2013 Nationale-Nederlanden

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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

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
import nl.nn.adapterframework.http.HttpSecurityHandler;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;


/** *
 * The JavaListener listens to java requests.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.receivers.JavaListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the listener as known to the adapter. An {@link nl.nn.adapterframework.pipes.IbisLocalSender IbisLocalSender} refers to this name in its <code>javaListener</code>-attribute.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setServiceName(String) serviceName}</td><td>(optional) name under which the JavaListener registers itself with the RequestDispatcherManager.
 * An {@link nl.nn.adapterframework.pipes.IbisJavaSender IbisJavaSender} refers to this attribute in its <code>serviceName</code>-attribute.
 * If not empty, the IbisServiceDispatcher.jar must be on the classpath of the server.
 *     <br>N.B. If this java listener is to be only called locally (from within the same Ibis), please leave
 * 	   this attribute empty, to avoid dependency to an IbisServiceDispatcher.jar on the server classpath.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setIsolated(boolean) isolated}</td><td>when <code>true</code>, the call is made in a separate thread, possibly using a separate transaction.
 * 		<br>N.B. do not use this attribute, set an appropriate <code>transactionAttribute</code>, like <code>NotSupported</code> or <code>RequiresNew</code> instead</td><td>false</td></tr>
 * <tr><td>{@link #setSynchronous(boolean) synchronous}</td><td> when set <code>false</code>, the request is executed asynchronously. This implies <code>isolated=true</code>. N.B. Be aware that there is no limit on the number of threads generated</td><td>true</td></tr>
 * <tr><td>{@link #setThrowException(boolean) throwException}</td><td>Should the JavaListener throw a ListenerException when it occurs or return an error message</td><td><code>true</code></td></tr>
 * <tr><td>{@link #setHttpWsdl(boolean)}</td><td>when <code>true</code>, the WSDL of the service provided by this listener is available for download </td><td><code>false</code></td></tr>
 * </table>
 *
 * @author  Gerrit van Brakel
 */
public class JavaListener implements IPushingListener, RequestProcessor, HasPhysicalDestination {
	protected Logger log = LogUtil.getLogger(this);

	private String name;
	private String serviceName;
	private boolean isolated=false;
	private boolean synchronous=true;
	private boolean opened=false;
	private boolean throwException = true;
	private boolean httpWsdl = false;

	private static Map<String, JavaListener> registeredListeners;
	private IMessageHandler handler;

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
	public String processRequest(String correlationId, String message, HashMap context) throws ListenerException {
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
		if (throwException) {
			return handler.processRequest(this, correlationId, message, context);
		} else {
			try {
				return handler.processRequest(this, correlationId, message, context);
			}
			catch (ListenerException e) {
				return handler.formatException(null,correlationId, message,e);
			}
		}
	}


	/**
	 * Register listener so that it can be used by a proxy
	 * @param name
	 * @param receiver
	 */
	private void registerListener() {
		getListeners().put(getName(), this);
	}

	private void unregisterListener() {
		getListeners().remove(getName());
	}

	/**
	 * @param name
	 * @return JavaReiver registered under name
	 */
	public static JavaListener getListener(String name) {
		return (JavaListener)getListeners().get(name);
	}

	/**
	 * Get all registered JavaListeners
	 */
	private static synchronized Map<String, JavaListener> getListeners() {
		if (registeredListeners == null) {
			registeredListeners = Collections.synchronizedMap(new HashMap());
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
	public void afterMessageProcessed(PipeLineResult processResult, Object rawMessage, Map context) throws ListenerException {
		// do nothing
	}


	@Override
	public String getIdFromRawMessage(Object rawMessage, Map context) throws ListenerException {
		// do nothing
		return null;
	}

	@Override
	public String getStringFromRawMessage(Object rawMessage, Map context) throws ListenerException {
		return (String)rawMessage;
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
	public void setHandler(IMessageHandler handler) {
		this.handler = handler;
	}
	public IMessageHandler getHandler() {
		return handler;
	}

	public void setServiceName(String jndiName) {
		this.serviceName = jndiName;
	}
	public String getServiceName() {
		return serviceName;
	}

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

	public void setSynchronous(boolean b) {
		synchronous = b;
	}
	public boolean isSynchronous() {
		return synchronous;
	}

	public synchronized boolean isOpen() {
		return opened;
	}

	public void setThrowException(boolean throwException) {
		this.throwException = throwException;
	}
	public boolean isThrowException() {
		return throwException;
	}
	
	public void setHttpWsdl(boolean httpWsdl) {
		this.httpWsdl = httpWsdl;
	}
	public boolean isHttpWsdl() {
		return httpWsdl;
	}
}

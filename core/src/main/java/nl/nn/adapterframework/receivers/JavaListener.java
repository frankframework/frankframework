/*
   Copyright 2013 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IPushingListener;
import nl.nn.adapterframework.core.ISecurityHandler;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.dispatcher.DispatcherManagerFactory;
import nl.nn.adapterframework.dispatcher.RequestProcessor;
import nl.nn.adapterframework.doc.Category;
import nl.nn.adapterframework.doc.Mandatory;
import nl.nn.adapterframework.http.HttpSecurityHandler;
import nl.nn.adapterframework.senders.IbisJavaSender;
import nl.nn.adapterframework.senders.IbisLocalSender;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

// TODO: When anchors are supported by the Frank!Doc, link to https://github.com/ibissource/ibis-servicedispatcher
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
@Category("Basic")
public class JavaListener implements IPushingListener<String>, RequestProcessor, HasPhysicalDestination {

	private final @Getter(onMethod = @__(@Override)) String domain = "JVM";
	protected Logger log = LogUtil.getLogger(this);
	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;

	private @Getter String name;
	private @Getter String serviceName;
	private @Getter boolean synchronous=true;
	private @Getter String returnedSessionKeys=null;
	private @Getter boolean throwException = true;
	private @Getter boolean httpWsdl = false;

	private @Getter boolean open=false;
	private static Map<String, JavaListener> registeredListeners;
	private @Getter @Setter IMessageHandler<String> handler;

	@Override
	public void configure() throws ConfigurationException {
		if (handler==null) {
			throw new ConfigurationException("handler has not been set");
		}
	}

	@Override
	public synchronized void open() throws ListenerException {
		try {
			// add myself to local list so that IbisLocalSenders can find me
			registerListener();

			// add myself to global list so that other applications in this JVM (like Everest Portal) can find me.
			// (performed only if serviceName is not empty
			if (StringUtils.isNotEmpty(getServiceName())) {
				DispatcherManagerFactory.getDispatcherManager().register(getServiceName(), this);
			}
			open=true;
		} catch (Exception e) {
			throw new ListenerException("error occured while starting listener [" + getName() + "]", e);
		}
	}

	@Override
	public synchronized void close() throws ListenerException {
		open=false;
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
		log.debug("JavaListener [{}] processing correlationId [{}]" , getName(), correlationId);
		if (context != null) {
			Object object = context.get("httpRequest");
			if (object != null) {
				if (object instanceof HttpServletRequest) {
					ISecurityHandler securityHandler = new HttpSecurityHandler((HttpServletRequest)object);
					context.put(PipeLineSession.securityHandlerKey, securityHandler);
				} else {
					log.warn("No securityHandler added for httpRequest [" + object.getClass() + "]");
				}
			}
		}
		try (PipeLineSession session = new PipeLineSession(context)) {
			session.put(PipeLineSession.correlationIdKey, correlationId);
			Message message =  new Message(rawMessage);
			try {
				if (throwException) {
					try {
						return handler.processRequest(this, rawMessage, message, session).asString();
					} catch (IOException e) {
						throw new ListenerException("cannot convert stream", e);
					}
				}
				try {
					return handler.processRequest(this, rawMessage, message, session).asString();
				} catch (ListenerException | IOException e) {
					try {
						return handler.formatException(null,correlationId, message, e).asString();
					} catch (IOException e1) {
						e.addSuppressed(e1);
						throw new ListenerException(e);
					}
				}
			} finally {
				Misc.copyContext(getReturnedSessionKeys(), session, context, this);
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
		return getListeners().get(name);
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

	@Deprecated
	public void setLocal(String name) {
		throw new IllegalArgumentException("do not set attribute 'local=true', just leave serviceName empty!");
	}

	@Deprecated
	public void setIsolated(boolean b) {
		throw new IllegalArgumentException("function of attribute 'isolated' is replaced by 'transactionAttribute' on PipeLine");
	}

	@Deprecated
	/**
	 * If set <code>false</code>, the request is executed asynchronously. N.B. be aware that there is no limit on the number of threads generated
	 * @ff.default true
	 */
	public void setSynchronous(boolean b) {
		synchronous = b;
	}

	/** 
	 * Comma separated list of keys of session variables that should be returned to caller, for correct results as well as for erronous results. 
	 * If not set (not even to an empty value), all session keys can be returned.
	 * @ff.default all session keys can be returned
	 */
	public void setReturnedSessionKeys(String string) {
		returnedSessionKeys = string;
	}

	/**
	 * Should the JavaListener throw a ListenerException when it occurs or return an error message
	 * @ff.default true
	 */
	public void setThrowException(boolean throwException) {
		this.throwException = throwException;
	}

	/**
	 * If <code>true</code>, the WSDL of the service provided by this listener will available for download 
	 * @ff.default false
	 */
	public void setHttpWsdl(boolean httpWsdl) {
		this.httpWsdl = httpWsdl;
	}
}

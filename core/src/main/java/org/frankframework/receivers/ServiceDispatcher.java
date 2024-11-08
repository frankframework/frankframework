/*
   Copyright 2013, 2018-2020 Nationale-Nederlanden, 2020, 2022 WeAreFrank!

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

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.stream.Message;
import org.frankframework.util.LogUtil;

/**
 * Singleton class that knows about the ServiceListeners that are active.
 * <br/>
 * This class is to be used as a facade for different services that implement
 * the <code>ServiceClient</code> interface.<br/>
 * This class is exposed as a webservice, to be able to provide a single point
 * of entry to all adapters that have a ServiceListener as a IReceiver.
 *
 * @author Johan Verrips
 * @author Niels Meijer
 *
 * @see ServiceClient
 */
public class ServiceDispatcher  {
	protected Logger log = LogUtil.getLogger(this);

	private final ConcurrentSkipListMap<String, ServiceClient> registeredListeners = new ConcurrentSkipListMap<>();
	private static ServiceDispatcher self = null;

	/**
	 * Use this method to get hold of the <code>ServiceDispatcher</code>
	 * @return an instance of this class
	 */
	public static synchronized ServiceDispatcher getInstance() {
		if (self == null) {
			self = new ServiceDispatcher();
		}
		return self;
	}

	/**
	 * Dispatch a request {@link Message} to a service by its configured name.
	 *
	 * @param serviceName ServiceName given to the {@link ServiceClient} implementation that is to be called
	 * @param message {@link Message} to be processed
	 * @param session Existing {@link PipeLineSession}.
	 * @return {@link Message} with the result of the requested adapter execution.
	 * @throws ListenerException If there was an error in request execution.
	 */
	public Message dispatchRequest(String serviceName, Message message, PipeLineSession session) throws ListenerException {
		log.debug("dispatchRequest for service [{}] correlationId [{}] message [{}]", serviceName, session != null ? session.getCorrelationId() : null, message);

		ServiceClient client = registeredListeners.get(serviceName);
		if (client == null) {
			throw new ListenerException("service ["+ serviceName +"] is not registered");
		}
		return client.processRequest(message, session);
	}

	/**
	 * Retrieve the names of the registered listeners in alphabetical order.
	 * @return Set with the names.
	 */
	public SortedSet<String> getRegisteredListenerNames() {
		SortedSet<String> sortedKeys = new TreeSet<>(registeredListeners.keySet());
		return Collections.unmodifiableSortedSet(sortedKeys);
	}

	/**
	 * Check whether a serviceName is registered at the <code>ServiceDispatcher</code>.
	 * @return true if the service is registered at this dispatcher, otherwise false
	 */
	public boolean isRegisteredServiceListener(String name) {
		return registeredListeners.get(name) != null;
	}

	public synchronized void registerServiceClient(String name, ServiceClient listener) {
		if(StringUtils.isEmpty(name)) {
			throw new LifecycleException("Cannot register a ServiceClient without name");
		}
		if (isRegisteredServiceListener(name)) {
			throw new LifecycleException("Dispatcher already has a ServiceClient registered under name ["+name+"]");
		}

		registeredListeners.put(name, listener);
		log.info("Listener [{}] registered at ServiceDispatcher", name);
	}

	public void unregisterServiceClient(String name) {
		if (!isRegisteredServiceListener(name)) {
			log.warn("listener [{}] not registered with ServiceDispatcher", name);
		} else {
			registeredListeners.remove(name);
			log.info("Listener [{}] unregistered from ServiceDispatcher", name);
		}
	}

	public ServiceClient getListener(String name) {
		return registeredListeners.get(name);
	}
}

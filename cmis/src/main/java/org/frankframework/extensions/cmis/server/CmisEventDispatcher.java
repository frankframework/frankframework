/*
   Copyright 2019 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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
package org.frankframework.extensions.cmis.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.chemistry.opencmis.commons.exceptions.CmisBaseException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;

import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.extensions.cmis.CmisEventListener;
import org.frankframework.extensions.cmis.CmisUtils;
import org.frankframework.receivers.JavaListener;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.DomBuilderException;
import org.frankframework.util.LogUtil;
import org.frankframework.util.XmlUtils;

public class CmisEventDispatcher {
	private final Logger log = LogUtil.getLogger(this);

	private static CmisEventDispatcher self = null;
	public static final String CMIS_EVENT_KEY = "CmisEvent";
	private final Map<CmisEvent, CmisEventListener> eventListeners = new ConcurrentHashMap<>();
	private final String dispatcherName = AppConstants.getInstance().getProperty(RepositoryConnectorFactory.CMIS_BRIDGE_PROPERTY_PREFIX + "adapterDispatcher");

	public static synchronized CmisEventDispatcher getInstance() {
		if(self == null) {
			self = new CmisEventDispatcher();
		}
		return self;
	}

	public void registerEventListener(CmisEventListener listener)  {
		CmisEvent event = listener.getEvent();

		// This is already checked in CmisEventListener#configure()
		Objects.requireNonNull(event);

		log.info("registering CmisEvent [{}] on dispatcher", event::name);
		eventListeners.put(event, listener);
	}

	public void unregisterEventListener(CmisEventListener listener) {
		CmisEvent event = listener.getEvent();
		eventListeners.remove(event);
		log.info("unregistered CmisEvent [{}] from dispatcher", event::name);
	}

	/**
	 * Convenience method to create a PipeLineSession and set the cmis CallContext
	 */
	public Element trigger(CmisEvent event, String message, CallContext callContext) {
		PipeLineSession context = new PipeLineSession();
		context.put(CmisUtils.CMIS_CALLCONTEXT_KEY, callContext);

		return trigger(event, message, context);
	}

	public Element trigger(CmisEvent event, String message, PipeLineSession messageContext) {
		if(!eventListeners.containsKey(event))
			throw new CmisRuntimeException("event ["+event.name()+"] not registered");

		if(log.isDebugEnabled()) log.debug("bridging CmisEvent [{}]", event::name);
		CmisUtils.populateCmisAttributes(messageContext);

		try {
			messageContext.put(CMIS_EVENT_KEY, event.getLabel());
			CmisEventListener listener = eventListeners.get(event);
			String result = listener.processRequest(new Message(message), messageContext).asString();
			if(StringUtils.isEmpty(result))
				return XmlUtils.buildElement("<cmis/>");
			else if (XmlUtils.isWellFormed(result, "cmis")) {
				return XmlUtils.buildElement(result);
			}
			else {
				throw new CmisRuntimeException("invalid or unparsable result");
			}
		} catch (IOException e) {
			throw new CmisRuntimeException("invalid or unparsable result", e);
		}

		//Try and catch the original CMIS exception and throw that instead
		catch (ListenerException e) {
			if(e.getCause() instanceof PipeRunException pre) {
				if(pre != null && pre.getCause() instanceof CmisBaseException cbe)
					throw cbe;
			}
			throw new CmisRuntimeException(e.getMessage(), e);
		}
		catch (DomBuilderException e) {
			throw new CmisRuntimeException("error building domdoc from result", e);
		}
	}

	public boolean contains(CmisEvent event) {
		if(StringUtils.isNotEmpty(dispatcherName)) {
			JavaListener<?> listener = JavaListener.getListener(dispatcherName);
			if(listener == null) {
				throw new CmisRuntimeException("unable to bridge cmis request, dispatcher offline"); //Adapter registered but not started
			}

			HashMap<String, Object> messageContext = new HashMap<>();
			messageContext.put(CMIS_EVENT_KEY, event.getLabel());

			try {
				String result = listener.processRequest(null, event.getLabel(), messageContext);
				return Boolean.parseBoolean(result); // Result should determine if we should proceed, an exception may be thrown.
			} catch (ListenerException e) {
				throw new CmisRuntimeException("unable to bridge cmis request: " + e.getMessage(), e); //Append the message so it becomes visible in the soap-fault (when using WS)
			}
		} else {
			return eventListeners.containsKey(event);
		}
	}

	public boolean hasEventListeners() {
		return !eventListeners.isEmpty();
	}
}

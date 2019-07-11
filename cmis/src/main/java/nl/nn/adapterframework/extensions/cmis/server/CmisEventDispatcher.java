/*
   Copyright 2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.cmis.server;

import java.util.HashMap;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.exceptions.CmisBaseException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.extensions.cmis.CmisEventListener;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.XmlUtils;

public class CmisEventDispatcher {
	private static CmisEventDispatcher self = null;
	private Map<CmisEvent, CmisEventListener> eventListeners = new HashMap<CmisEvent, CmisEventListener>();

	public static synchronized CmisEventDispatcher getInstance() {
		if(self == null) {
			self = new CmisEventDispatcher();
		}
		return self;
	}

	public void registerEventListener(CmisEventListener listener) throws ListenerException {
		CmisEvent event = listener.getEvent();
		if(event == null)
			throw new ListenerException("cannot register EventListener without event to listen on");

		eventListeners.put(listener.getEvent(), listener);
	}

	public void unregisterEventListener(CmisEventListener listener) {
		eventListeners.remove(listener.getEvent());
	}

	public Element trigger(CmisEvent event, String message) {
		return trigger(event, message, new PipeLineSessionBase());
	}

	public Element trigger(CmisEvent event, String message, IPipeLineSession messageContext) {
		if(!eventListeners.containsKey(event))
			throw new CmisRuntimeException("event ["+event.toString()+"] not found");

		try {
			messageContext.put("CmisEvent", event.toString());
			String result = eventListeners.get(event).processRequest(null, message, messageContext);
			if(StringUtils.isEmpty(result))
				return XmlUtils.buildElement("<cmis/>");
			else if (XmlUtils.isWellFormed(result, "cmis")) {
				return XmlUtils.buildElement(result);
			}
			else {
				throw new CmisRuntimeException("invalid or unparsable result");
			}
		}
		//Try and catch the original CMIS exception and throw that instead
		catch (ListenerException e) {
			if(e.getCause() instanceof PipeRunException) {
				PipeRunException pre = (PipeRunException) e.getCause();
				if(pre.getCause() != null && pre.getCause() instanceof CmisBaseException)
					throw (CmisBaseException)pre.getCause();
			}
			throw new CmisRuntimeException(e.getMessage(), e);
		}
		catch (DomBuilderException e) {
			throw new CmisRuntimeException("error building domdoc from result", e);
		}
	}

	public boolean contains(CmisEvent event) {
		return eventListeners.containsKey(event);
	}
}

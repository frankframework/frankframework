/*
   Copyright 2020 Integration Partners
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
package nl.nn.adapterframework.larva;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

/**
 * Message handler for JavaListener and WebServiceListener.
 * 
 * @author Jaco de Groot
 */
public class ListenerMessageHandler implements IMessageHandler {
	private static Logger log = LogUtil.getLogger(ListenerMessageHandler.class);
	private List requestMessages = new ArrayList();
	private List responseMessages = new ArrayList();
	private long requestTimeOut = TestTool.DEFAULT_TIMEOUT;
	private long responseTimeOut = TestTool.DEFAULT_TIMEOUT;

	public String processRequest(IListener origin, String correlationId, String message, Map context) throws ListenerException {
		ListenerMessage listenerMessage = new ListenerMessage(correlationId, message, context);
		putRequestMessage(listenerMessage);
		String response = null;
		listenerMessage = getResponseMessage();
		if (listenerMessage != null) {
			response = listenerMessage.getMessage();
		}
		return response;
	}
	
	public void putRequestMessage(ListenerMessage listenerMessage) {
		if (listenerMessage != null) {
			synchronized(requestMessages) {
				requestMessages.add(listenerMessage);
			}
		} else {
			log.error("listenerMessage is null");
		}
	}

	public ListenerMessage getRequestMessage() {
		return getRequestMessage(requestTimeOut);
	}

	public ListenerMessage getRequestMessage(long timeOut) {
		ListenerMessage listenerMessage = null;
		long startTime = System.currentTimeMillis();
		while (listenerMessage == null && System.currentTimeMillis() < startTime + timeOut) {
			synchronized(requestMessages) {
				if (requestMessages.size() > 0) {
					listenerMessage = (ListenerMessage)requestMessages.remove(0);
				}
			}
			if (listenerMessage == null) {
				try {
					Thread.sleep(100);
				} catch(InterruptedException e) {
				}
			}
		}
		return listenerMessage;
	}
	
	public void putResponseMessage(ListenerMessage listenerMessage) {
		if (listenerMessage != null) {
			synchronized (responseMessages) {
				responseMessages.add(listenerMessage);
			}
		} else {
			log.error("listenerMessage is null");
		}
	}
	
	public ListenerMessage getResponseMessage() {
		return getResponseMessage(responseTimeOut);
	}
		
	public ListenerMessage getResponseMessage(long timeOut) {
		ListenerMessage listenerMessage = null;
		long startTime = System.currentTimeMillis();
		while (listenerMessage == null && System.currentTimeMillis() < startTime + timeOut) {
			synchronized(responseMessages) {
				if (responseMessages.size() > 0) {
					listenerMessage = (ListenerMessage)responseMessages.remove(0);
				}
			}
			if (responseMessages == null) {
				try {
					Thread.sleep(100);
				} catch(InterruptedException e) {
				}
			}
		}
		return listenerMessage;
	}
	
	public void setRequestTimeOut(long requestTimeOut) {
		this.requestTimeOut = requestTimeOut;
	}
	
	public void setResponseTimeOut(long responseTimeOut) {
		this.responseTimeOut = responseTimeOut;
	}

	public void processRawMessage(IListener origin, Object rawMessage, Map threadContext) throws ListenerException {
		String correlationId = origin.getIdFromRawMessage(rawMessage, threadContext);
		String message = origin.getStringFromRawMessage(rawMessage, threadContext);
		processRequest(origin, correlationId, message, threadContext);
	}

	public void processRawMessage(IListener origin, Object rawMessage, Map threadContext, long waitingTime) throws ListenerException {
		processRawMessage(origin, rawMessage, threadContext);
	}

	public void processRawMessage(IListener origin, Object rawMessage) throws ListenerException {
		processRawMessage(origin, rawMessage, null);
	}

	public String processRequest(IListener origin, String message) throws ListenerException {
		return processRequest(origin, null, message, null);
	}

	public String processRequest(IListener origin, String correlationId, String message) throws ListenerException {
		return processRequest(origin, correlationId, message, null);
	}

	public String processRequest(IListener origin, String correlationId, String message, HashMap context) throws ListenerException {
		return processRequest(origin, correlationId, message, (Map)context);
	}

	public String processRequest(IListener origin, String correlationId, String message, Map context, long waitingTime) throws ListenerException {
		return processRequest(origin, correlationId, message, context);
	}

	public String formatException(String origin, String arg1, String arg2, Throwable arg3) {
		log.error("formatException(String arg0, String arg1, String arg2, Throwable arg3) not implemented");
		return null;
	}
}

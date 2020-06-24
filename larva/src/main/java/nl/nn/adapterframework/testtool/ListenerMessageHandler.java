package nl.nn.adapterframework.testtool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.LogUtil;

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

	@Override
	public Message processRequest(IListener origin, String correlationId, Object rawMessage, Message message, Map context) throws ListenerException {
		ListenerMessage listenerMessage;
		try {
			listenerMessage = new ListenerMessage(correlationId, message.asString(), context);
		} catch (IOException e) {
			throw new ListenerException("cannot convert message to string",e);
		}
		putRequestMessage(listenerMessage);
		Message response = null;
		listenerMessage = getResponseMessage();
		if (listenerMessage != null) {
			response = new Message(listenerMessage.getMessage());
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

	@Override
	public void processRawMessage(IListener origin, Object rawMessage, Map threadContext) throws ListenerException {
		String correlationId = origin.getIdFromRawMessage(rawMessage, threadContext);
		Message message = origin.extractMessage(rawMessage, threadContext);
		processRequest(origin, correlationId, rawMessage, message, threadContext);
	}

	@Override
	public void processRawMessage(IListener origin, Object rawMessage, Map threadContext, long waitingTime) throws ListenerException {
		processRawMessage(origin, rawMessage, threadContext);
	}

	@Override
	public void processRawMessage(IListener origin, Object rawMessage) throws ListenerException {
		processRawMessage(origin, rawMessage, null);
	}

	@Override
	public Message processRequest(IListener origin, Object rawMessage, Message message) throws ListenerException {
		return processRequest(origin, null, rawMessage, message, null);
	}

	@Override
	public Message processRequest(IListener origin, String correlationId, Object rawMessage, Message message) throws ListenerException {
		return processRequest(origin, correlationId, rawMessage, message, null);
	}

	@Override
	public Message processRequest(IListener origin, String correlationId, Object rawMessage, Message message, Map context, long waitingTime) throws ListenerException {
		return processRequest(origin, correlationId, rawMessage, message, context);
	}

	@Override
	public String formatException(String origin, String arg1, Message arg2, Throwable arg3) {
		log.error("formatException(String arg0, String arg1, String arg2, Throwable arg3) not implemented");
		return null;
	}
}

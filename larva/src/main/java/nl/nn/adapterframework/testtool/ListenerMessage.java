package nl.nn.adapterframework.testtool;

import java.util.Map;

import nl.nn.adapterframework.core.IListener;

/**
 * @author Jaco de Groot
 */
public class ListenerMessage {
	private String correlationId;
	private String message;
	private Map context;

	public ListenerMessage(String correlationId, String message, Map context) {
		this.correlationId = correlationId;
		this.message = message;
		this.context = context;
	}
	
	public String getCorrelationId() {
		return correlationId;
	}
	
	public String getMessage() {
		return message;
	}
	
	public Map getContext() {
		return context;
	}
}

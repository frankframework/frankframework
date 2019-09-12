/**
 * 
 */
package nl.nn.adapterframework.testtool;

import java.util.Map;

/**
 * @author Murat Kaan Meral
 *
 * Simple Message structure for message listener.
 */
class Message{
	Map<String, String> message;
	int logLevel;
	long timestamp;
	
	public Message(Map<String, String> message, int logLevel, long timestamp) {
		super();
		this.message = message;
		this.logLevel = logLevel;
		this.timestamp = timestamp;
	}
}

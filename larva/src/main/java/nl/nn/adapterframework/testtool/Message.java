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
	String testName;
	int logLevel;
	long timestamp;
	
	Message(String testName, Map<String, String> message, int logLevel, long timestamp) {
		super();
		this.testName = testName;
		this.message = message;
		this.logLevel = logLevel;
		this.timestamp = timestamp;
	}
}

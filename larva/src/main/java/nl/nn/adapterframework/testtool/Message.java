package nl.nn.adapterframework.testtool;

import java.util.Map;

/**
 * Simple Message structure for message listener.
 *
 * @author Murat Kaan Meral
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

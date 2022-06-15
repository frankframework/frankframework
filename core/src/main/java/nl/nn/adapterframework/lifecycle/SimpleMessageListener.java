package nl.nn.adapterframework.lifecycle;

public class SimpleMessageListener {

	public String onMessage(String message) {
		System.err.println(message);
		return message;
	}
}

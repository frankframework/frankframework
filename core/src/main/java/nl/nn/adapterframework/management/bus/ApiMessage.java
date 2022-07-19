package nl.nn.adapterframework.management.bus;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

public class ApiMessage<T> implements Message<T> {

	@Override
	public T getPayload() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MessageHeaders getHeaders() {
		// TODO Auto-generated method stub
		return null;
	}

}

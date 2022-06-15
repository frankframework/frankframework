package nl.nn.adapterframework.lifecycle;

import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.messaging.Message;

public class Gateway<T> extends MessagingGatewaySupport {

	@SuppressWarnings("unchecked")
	public Message<T> execute(Message<T> in) {
		return (Message<T>) super.sendAndReceiveMessage(in);
	}
}

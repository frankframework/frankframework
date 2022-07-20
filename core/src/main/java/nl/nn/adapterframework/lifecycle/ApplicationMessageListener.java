package nl.nn.adapterframework.lifecycle;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

import lombok.Setter;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.configuration.IbisManager.IbisAction;
import nl.nn.adapterframework.management.bus.RequestMessage;
import nl.nn.adapterframework.management.bus.ResponseMessage;
import nl.nn.adapterframework.util.EnumUtils;

public class ApplicationMessageListener extends AbstractReplyProducingMessageHandler implements ApplicationContextAware {
	private @Setter ApplicationContext applicationContext;
	private @Setter IbisManager ibisManager;

	public ResponseMessage onMessage(Message<?> message) {
//		IbisAction action = EnumUtils.parse(IbisAction.class, (String) message.getPayload());
		IbisAction action = (IbisAction) message.getPayload();
		MessageHeaders headers = message.getHeaders();
		String configurationName = (String) headers.get("configuration");
		String adapterName = (String) headers.get("adapter");
		String receiverName = (String) headers.get("receiver");
		String issuedBy = (String) headers.get("issuedBy");
		ibisManager.handleAction(action, configurationName, adapterName, receiverName, issuedBy, true);
		return ResponseMessage.create("test");
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		System.out.println("whats this " + requestMessage.toString());

		String result = "";
		IbisManager ibisManager = applicationContext.getBean("ibisManager", IbisManager.class);
		for (Configuration configuration : ibisManager.getConfigurations()) {
			result = result + configuration.getOriginalConfiguration();
		}
		Map<String, Object> headers = new HashMap<>();
		headers.put("test123", "asdf");
		return new GenericMessage<>(result, headers);
	}
}

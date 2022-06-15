package nl.nn.adapterframework.lifecycle;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

import lombok.Setter;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisManager;

public class ConfigurationMessageListener implements ApplicationContextAware {
	private @Setter ApplicationContext applicationContext;

	public Message<?> onMessage(String message) {
		if(message == "getConfigurations") {
			String result = "";
			IbisManager ibisManager = applicationContext.getBean("ibisManager", IbisManager.class);
			for (Configuration configuration : ibisManager.getConfigurations()) {
				result = result + configuration.getOriginalConfiguration();
			}
			Map<String, Object> headers = new HashMap<>();
			headers.put("test123", "s");
			return new GenericMessage<>(result, headers);
		}

		return new ErrorMessage(new IllegalArgumentException("error"));
	}
}

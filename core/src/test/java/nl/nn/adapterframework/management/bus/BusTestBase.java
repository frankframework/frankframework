package nl.nn.adapterframework.management.bus;

import static org.junit.Assert.fail;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.lifecycle.Gateway;
import nl.nn.adapterframework.testutil.TestConfiguration;

public class BusTestBase {

	private static Configuration configuration;
	private static ApplicationContext parentContext;

	private final ApplicationContext getParentContext() {
		if(parentContext == null) {
			ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();
			applicationContext.setConfigLocation("testBusApplicationContext.xml");
			applicationContext.setDisplayName("Parent [testBusApplicationContext]");
			applicationContext.refresh();
			parentContext = applicationContext;
		}
		return parentContext;
	}

	protected final Configuration getConfiguration() {
		if(configuration == null) {
			Configuration config = new TestConfiguration(TestConfiguration.TEST_CONFIGURATION_FILE);
			getParentContext().getAutowireCapableBeanFactory().autowireBeanProperties(config, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
			configuration = (Configuration) getParentContext().getAutowireCapableBeanFactory().initializeBean(config, TestConfiguration.TEST_CONFIGURATION_NAME);

			try {
				configuration.configure();
			} catch (ConfigurationException e) {
				fail("unable to create "+TestConfiguration.TEST_CONFIGURATION_NAME);
			}

			configuration.setLoadedConfiguration("loaded");
			configuration.setOriginalConfiguration("original");
		}
		return configuration;
	}

	public final Message<?> callSyncGateway(MessageBuilder<?> input) {
		Gateway gateway = getConfiguration().getBean("gateway", Gateway.class);
		Message<?> response = gateway.sendSyncMessage(input.build());
		if(response != null) {
			return response;
		}
		throw new IllegalStateException("expected a reply");
	}

	public final void callAsyncGateway(MessageBuilder<?> input) {
		Gateway gateway = getConfiguration().getBean("gateway", Gateway.class);
		gateway.sendAsyncMessage(input.build());
	}

	protected final <T> MessageBuilder<T> createRequestMessage(T payload, BusTopic topic) {
		return createRequestMessage(payload, topic, null);
	}

	protected final <T> MessageBuilder<T> createRequestMessage(T payload, BusTopic topic, BusAction action) {
		DefaultMessageBuilderFactory factory = getConfiguration().getBean("messageBuilderFactory", DefaultMessageBuilderFactory.class);
		MessageBuilder<T> builder = factory.withPayload(payload);
		builder.setHeader(TopicSelector.TOPIC_HEADER_NAME, topic.name());
		if(action != null) {
			builder.setHeader(ActionSelector.ACTION_HEADER_NAME, action.name());
		}
		return builder;
	}
}

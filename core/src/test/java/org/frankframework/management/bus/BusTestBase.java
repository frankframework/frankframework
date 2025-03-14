package org.frankframework.management.bus;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.spy;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.testutil.QuerySenderPostProcessor;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.LogUtil;

public class BusTestBase {

	private Configuration configuration;
	private ApplicationContext parentContext;
	private final QuerySenderPostProcessor qsPostProcessor = new QuerySenderPostProcessor();

	protected ApplicationContext getParentContext() {
		if(parentContext == null) {
			ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();
			applicationContext.setConfigLocation("testBusApplicationContext.xml");
			applicationContext.setDisplayName("Parent [testBusApplicationContext]");

			applicationContext.refresh();

			// Add Custom Pre-Instantiation Processor to mock statically created FixedQuerySenders.
			qsPostProcessor.setApplicationContext(applicationContext);
			applicationContext.getBeanFactory().addBeanPostProcessor(qsPostProcessor);

			parentContext = applicationContext;
		}
		return parentContext;
	}

	protected final Configuration getConfiguration() {
		if(configuration == null) {
			Configuration config = new TestConfiguration(TestConfiguration.TEST_CONFIGURATION_FILE);
			try {
				getParentContext().getAutowireCapableBeanFactory().autowireBeanProperties(config, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
				configuration = (Configuration) getParentContext().getAutowireCapableBeanFactory().initializeBean(config, TestConfiguration.TEST_CONFIGURATION_NAME);
			} catch (Exception e) {
				LogUtil.getLogger(this).error("unable to create "+TestConfiguration.TEST_CONFIGURATION_NAME, e);
				fail("unable to create "+TestConfiguration.TEST_CONFIGURATION_NAME);
			}

			try {
				configuration.configure();
			} catch (ConfigurationException e) {
				LogUtil.getLogger(this).error("unable to configure "+TestConfiguration.TEST_CONFIGURATION_NAME, e);
				fail("unable to configure "+TestConfiguration.TEST_CONFIGURATION_NAME);
			}

			configuration.setLoadedConfiguration("<loaded authAlias=\"test\" />"); // AuthAlias is used in BusTopic.SECURITY_ITEMS
			configuration.setOriginalConfiguration("<original authAlias=\"test\" />");
		}
		return configuration;
	}

	@BeforeEach
	public void setUp() throws Exception {
		getConfiguration(); // Create configuration
	}

	@AfterEach
	public void tearDown() {
		getConfiguration().close();
	}

	/**
	 * Add the ability to mock FixedQuerySender ResultSets. Enter the initial query and a mocked
	 * ResultSet using a {@link org.frankframework.testutil.mock.FixedQuerySenderMock.ResultSetBuilder ResultSetBuilder}.
	 */
	public void mockFixedQuerySenderResult(String query, ResultSet resultSet) {
		qsPostProcessor.addFixedQuerySenderMock(query, resultSet);
	}

	/**
	 * Add the ability to mock DirectQuerySender ResultSets. Enter the name of the DirectQuerySender and a mocked Response Message.
	 * NB: the response message must be well formed XML.
	 */
	public void mockDirectQuerySenderResult(String name, org.frankframework.stream.Message message) {
		qsPostProcessor.addDirectQuerySenderMock(name, message);
	}

	public final Message<?> callSyncGateway(MessageBuilder<?> input) {
		OutboundGateway gateway = getParentContext().getBean("gateway", LocalGateway.class);
		Message<?> response = gateway.sendSyncMessage(input.build());
		if(response != null) {
			return response;
		}
		String topic = input.getHeader("topic", String.class);
		String action = input.getHeader("action", String.class);
		throw new IllegalStateException("expected a reply while sending a message to topic ["+topic+"] action ["+action+"]");
	}

	public final void callAsyncGateway(MessageBuilder<?> input) {
		OutboundGateway gateway = getParentContext().getBean("gateway", LocalGateway.class);
		gateway.sendAsyncMessage(input.build());
	}

	protected final <T> MessageBuilder<T> createRequestMessage(T payload, BusTopic topic) {
		return createRequestMessage(payload, topic, null);
	}

	protected final <T> MessageBuilder<T> createRequestMessage(T payload, BusTopic topic, BusAction action) {
		MessageBuilder<T> builder = spy(new MessageBuilder<>(payload));
		builder.setHeader(BusTopic.TOPIC_HEADER_NAME, topic.name());
		if(action != null) {
			builder.setHeader(BusAction.ACTION_HEADER_NAME, action.name());
		}

		return builder;
	}

	public static class MessageBuilder<T> {
		private final T payload;
		private Map<String, Object> headers = new HashMap<>();
		public MessageBuilder(T payload) {
			this.payload = payload;
		}

		@SuppressWarnings("unchecked")
		public <C> C getHeader(String key, Class<C> clazz) {
			return (C) headers.get(key);
		}

		public void setHeader(String headerName, Object headerValue) {
			if(!"topic".equals(headerName) && !"action".equals(headerName)) {
				headerName = "meta-"+headerName;
			}
			headers.put(headerName, headerValue);
		}

		public Message<T> build() {
			return new GenericMessage<>(payload, headers);
		}
	}
}

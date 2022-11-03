package nl.nn.adapterframework.management.bus;

import static org.junit.Assert.fail;

import java.sql.ResultSet;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.lifecycle.Gateway;
import nl.nn.adapterframework.testutil.QuerySenderPostProcessor;
import nl.nn.adapterframework.testutil.TestConfiguration;

public class BusTestBase {

	private static Configuration configuration;
	private static ApplicationContext parentContext;
	private QuerySenderPostProcessor qsPostProcessor = new QuerySenderPostProcessor();

	private final ApplicationContext getParentContext() {
		if(parentContext == null) {
			ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();
			applicationContext.setConfigLocation("testBusApplicationContext.xml");
			applicationContext.setDisplayName("Parent [testBusApplicationContext]");

			applicationContext.refresh();

			//Add Custom Pre-Instantiation Processor to mock statically created FixedQuerySenders.
			qsPostProcessor.setApplicationContext(applicationContext);
			applicationContext.getBeanFactory().addBeanPostProcessor(qsPostProcessor);

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

			configuration.setLoadedConfiguration("<loaded authAlias=\"test\" />"); //AuthAlias is used in BusTopic.SECURITY_ITEMS
			configuration.setOriginalConfiguration("<original authAlias=\"test\" />");
		}
		return configuration;
	}

	/**
	 * Add the ability to mock FixedQuerySender ResultSets. Enter the initial query and a mocked 
	 * ResultSet using a {@link nl.nn.adapterframework.testutil.mock.FixedQuerySenderMock.ResultSetBuilder ResultSetBuilder}.
	 */
	public void mockFixedQuerySenderResult(String query, ResultSet resultSet) {
		qsPostProcessor.addFixedQuerySenderMock(query, resultSet);
	}

	/**
	 * Add the ability to mock DirectQuerySender ResultSets. Enter the name of the DirectQuerySender and a mocked Response Message.
	 * NB: the response message must be well formed XML.
	 */
	public void mockDirectQuerySenderResult(String name, nl.nn.adapterframework.stream.Message message) {
		qsPostProcessor.addDirectQuerySenderMock(name, message);
	}

	public final Message<?> callSyncGateway(MessageBuilder<?> input) {
		Gateway gateway = getParentContext().getBean("gateway", Gateway.class);
		gateway.setErrorChannel(null); //Somehow Spring is setting an ErrorChannel we do not want!
		Message<?> response = gateway.sendSyncMessage(input.build());
		if(response != null) {
			return response;
		}
		String topic = input.getHeader("topic", String.class);
		String action = input.getHeader("action", String.class);
		throw new IllegalStateException("expected a reply while sending a message to topic ["+topic+"] action ["+action+"]");
	}

	public final void callAsyncGateway(MessageBuilder<?> input) {
		Gateway gateway = getParentContext().getBean("gateway", Gateway.class);
		gateway.setErrorChannel(null); //Somehow Spring is setting an ErrorChannel we do not want!
		gateway.sendAsyncMessage(input.build());
	}

	protected final <T> MessageBuilder<T> createRequestMessage(T payload, BusTopic topic) {
		return createRequestMessage(payload, topic, null);
	}

	protected final <T> MessageBuilder<T> createRequestMessage(T payload, BusTopic topic, BusAction action) {
		DefaultMessageBuilderFactory factory = getParentContext().getBean("messageBuilderFactory", DefaultMessageBuilderFactory.class);
		MessageBuilder<T> builder = factory.withPayload(payload);
		builder.setHeader(TopicSelector.TOPIC_HEADER_NAME, topic.name());
		if(action != null) {
			builder.setHeader(ActionSelector.ACTION_HEADER_NAME, action.name());
		}
		return builder;
	}
}

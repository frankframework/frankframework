package nl.nn.adapterframework.management.bus;

import static org.junit.jupiter.api.Assertions.fail;

import java.sql.ResultSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.testutil.QuerySenderPostProcessor;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.util.LogUtil;

public class BusTestBase {

	private Configuration configuration;
	private ApplicationContext parentContext;
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

			configuration.setLoadedConfiguration("<loaded authAlias=\"test\" />"); //AuthAlias is used in BusTopic.SECURITY_ITEMS
			configuration.setOriginalConfiguration("<original authAlias=\"test\" />");
		}
		return configuration;
	}

	@BeforeEach
	public void setUp() throws Exception {
		getConfiguration(); //Create configuration
	}

	@AfterEach
	public void tearDown() throws Exception {
		getConfiguration().close();
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
		IntegrationGateway gateway = getParentContext().getBean("gateway", LocalGateway.class);
		Message<?> response = gateway.sendSyncMessage(input.build());
		if(response != null) {
			return response;
		}
		String topic = input.getHeader("topic", String.class);
		String action = input.getHeader("action", String.class);
		throw new IllegalStateException("expected a reply while sending a message to topic ["+topic+"] action ["+action+"]");
	}

	public final void callAsyncGateway(MessageBuilder<?> input) {
		IntegrationGateway gateway = getParentContext().getBean("gateway", LocalGateway.class);
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

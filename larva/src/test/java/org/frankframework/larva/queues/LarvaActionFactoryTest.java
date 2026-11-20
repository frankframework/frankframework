package org.frankframework.larva.queues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.support.AbstractApplicationContext;

import org.frankframework.configuration.ClassLoaderException;
import org.frankframework.configuration.ClassLoaderFactory;
import org.frankframework.configuration.IbisContext;
import org.frankframework.larva.LarvaConfig;
import org.frankframework.larva.LarvaTool;
import org.frankframework.larva.Scenario;
import org.frankframework.larva.actions.LarvaActionFactory;
import org.frankframework.larva.actions.LarvaScenarioAction;
import org.frankframework.larva.actions.LarvaScenarioContext;
import org.frankframework.larva.actions.SenderAction;
import org.frankframework.larva.output.LarvaWriter;
import org.frankframework.larva.output.PlainTextScenarioOutputRenderer;
import org.frankframework.stream.Message;

class LarvaActionFactoryTest {

	private LarvaActionFactory actionFactory;
	private LarvaTool larvaTool;
	private LarvaScenarioContext scenarioContext;

	@BeforeEach
	void setUp() throws ClassLoaderException {
		larvaTool = new LarvaTool(null, new LarvaConfig());
		actionFactory = new LarvaActionFactory(larvaTool, new PlainTextScenarioOutputRenderer(new LarvaWriter(larvaTool.getLarvaConfig(), System.out)));

		AutowireCapableBeanFactory beanFactory = mock();
		AbstractApplicationContext applicationContext = mock();
		when(applicationContext.getAutowireCapableBeanFactory()).thenReturn(beanFactory);
		IbisContext ibisContext = mock();
		when(ibisContext.getApplicationContext()).thenReturn(applicationContext);
		ClassLoaderFactory classLoaderFactory = new ClassLoaderFactory();
		classLoaderFactory.setIbisContext(ibisContext);
		classLoaderFactory.setApplicationContext(ibisContext.getApplicationContext());
		when(applicationContext.getBean(ClassLoaderFactory.class)).thenReturn(classLoaderFactory);

		scenarioContext = new LarvaScenarioContext(applicationContext, "/");
	}

	@Test
	void openActions() throws Exception {
		// Arrange
		Properties props = new Properties();
		props.load(this.getClass().getResourceAsStream("/actionCreatorTest.properties"));
		Scenario scenario = new Scenario(new File("/actionCreatorTest.properties"), "test", "test", props);
		// Act
		Map<String, LarvaScenarioAction> actions = actionFactory.createLarvaActions(scenario, scenarioContext, "cid");

		// Assert
		assertNotNull(actions);
		assertTrue(actions.containsKey("thisIsAnAction"));
		assertTrue(actions.containsKey("this.is.another.action"));
		assertFalse(actions.containsKey("prop.without"));

		LarvaScenarioAction action = actions.get("thisIsAnAction");
		assertNotNull(action);
		SenderAction senderAction = assertInstanceOf(SenderAction.class, action);

		long start = System.currentTimeMillis();
		senderAction.executeWrite(new Message("input"), null, null);
		Message message = senderAction.executeRead(props);
		long duration = System.currentTimeMillis() - start;
		assertTrue(duration >= 100, "delay should be >= 100ms but was " + duration);
		assertNotNull(message);
		assertEquals("input", message.asString());
	}
}

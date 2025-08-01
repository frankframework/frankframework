package org.frankframework.larva.queues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import org.frankframework.configuration.ClassLoaderException;
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
	private ApplicationContext applicationContext;

	@BeforeEach
	void setUp() throws ClassLoaderException {
		larvaTool = new LarvaTool(null, new LarvaConfig());
		actionFactory = new LarvaActionFactory(larvaTool, new PlainTextScenarioOutputRenderer(new LarvaWriter(larvaTool.getLarvaConfig(), System.out)));
		applicationContext = new LarvaScenarioContext(null, "/");
	}

	@Test
	void openQueues() throws Exception {
		// Arrange
		Properties props = new Properties();
		props.load(this.getClass().getResourceAsStream("/queueCreatorTest.properties"));
		Scenario scenario = new Scenario(new File("/queueCreatorTest.properties"), "test", "test", props);
		// Act
		Map<String, LarvaScenarioAction> queues = actionFactory.createLarvaActions(scenario, applicationContext, "cid");

		// Assert
		assertNotNull(queues);
		assertTrue(queues.containsKey("thisIsAQueue"));
		assertTrue(queues.containsKey("this.is.another.queue"));
		assertFalse(queues.containsKey("prop.without"));

		LarvaScenarioAction action = queues.get("thisIsAQueue");
		assertNotNull(action);
		SenderAction senderAction = assertInstanceOf(SenderAction.class, action);

		long start = System.currentTimeMillis();
		senderAction.executeWrite(new Message("input"), null, null);
		Message message = senderAction.executeRead(props);
		long duration = System.currentTimeMillis() - start;
		assertTrue(duration > 100, "delay should be > 100ms but was " + duration);
		assertNotNull(message);
		assertEquals("input", message.asString());
	}
}

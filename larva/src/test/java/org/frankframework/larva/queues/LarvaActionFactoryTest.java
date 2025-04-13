package org.frankframework.larva.queues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.IbisContext;
import org.frankframework.larva.LarvaTool;
import org.frankframework.larva.actions.LarvaActionFactory;
import org.frankframework.larva.actions.LarvaScenarioAction;
import org.frankframework.larva.actions.SenderAction;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassUtils;

class LarvaActionFactoryTest {

	private LarvaActionFactory actionFactory;
	private LarvaTool larvaTool;
	private IbisContext ibisContext;

	@BeforeEach
	void setUp() {
		larvaTool = new LarvaTool();
		actionFactory = new LarvaActionFactory(larvaTool);
		ibisContext = mock();

		// This is far from perfect but at least allows us to test simple things...
		// At some point we should remove the IbisContext from Larva and replace it with a Spring ApplicationContext.
		when(ibisContext.createBeanAutowireByName(any())).thenAnswer(a -> {
			Class<?> clazz = a.getArgument(0);
			Constructor<?> con = ClassUtils.getConstructorOnType(clazz, new Class[] {});
			return con.newInstance();
		});
	}

	@Test
	void openQueues() throws Exception {
		// Arrange
		Properties props = new Properties();
		props.load(this.getClass().getResourceAsStream("/queueCreatorTest.properties"));

		// Act
		Map<String, LarvaScenarioAction> queues = actionFactory.createLarvaActions("/", props, ibisContext, "cid");

		// Assert
		assertNotNull(queues);
		assertTrue(queues.containsKey("thisIsAQueue"));
		assertTrue(queues.containsKey("this.is.another.queue"));
		assertFalse(queues.containsKey("prop.without"));

		LarvaScenarioAction action = queues.get("thisIsAQueue");
		assertNotNull(action);
		SenderAction senderAction = assertInstanceOf(SenderAction.class, action);

		String stepName = "thisIsAQueue";
		long start = System.currentTimeMillis();
		assertEquals(LarvaTool.RESULT_OK, senderAction.executeWrite(stepName, new Message("input"), null, null));
		Message message = senderAction.executeRead(stepName, stepName, props, null, Message.nullMessage());
		long duration = System.currentTimeMillis() - start;
		assertTrue(duration > 100, "delay should be > 100ms but was " + duration);
		assertNotNull(message);
		assertEquals("input", message.asString());
	}
}

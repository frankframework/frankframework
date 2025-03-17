package org.frankframework.larva.queues;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.IbisContext;
import org.frankframework.larva.LarvaTool;
import org.frankframework.larva.TestConfig;
import org.frankframework.util.ClassUtils;

class QueueCreatorTest {

	private QueueCreator queueCreator;
	private TestConfig testConfig;
	private LarvaTool larvaTool;
	private IbisContext ibisContext;

	@BeforeEach
	void setUp() {
		testConfig = new TestConfig();
		larvaTool = new LarvaTool();
		queueCreator = new QueueCreator(testConfig, larvaTool);
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
	void openQueues() throws IOException {
		// Arrange
		Properties props = new Properties();
		props.load(this.getClass().getResourceAsStream("/queueCreatorTest.properties"));

		// Act
		Map<String, Queue> queues = queueCreator.openQueues("/", props, ibisContext, "cid");

		// Assert
		assertNotNull(queues);
		assertTrue(queues.containsKey("thisIsAQueue"));
		assertTrue(queues.containsKey("this.is.another.queue"));
		assertFalse(queues.containsKey("prop.without"));
	}
}

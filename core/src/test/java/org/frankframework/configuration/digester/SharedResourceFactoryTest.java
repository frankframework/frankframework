package org.frankframework.configuration.digester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.CanUseSharedResource;
import org.frankframework.core.SharedResource;
import org.frankframework.testutil.TestConfiguration;

public class SharedResourceFactoryTest {

	private static final String TEST_RESOURCE_NAME = "mySharedDummyResourceName";
	private static final String TEST_RESOURCE_VALUE = "mySharedDummyResourceValue";

	@Test
	public void testLowercaseClassname() throws Exception {
		try (TestConfiguration config = new TestConfiguration()) {
			SharedResourceFactory factory = config.createBean(SharedResourceFactory.class);
			Digester digester = mock(Digester.class);
			factory.setDigester(digester);
			Map<String, String> attributes = new HashMap<>();
			attributes.put("name", TEST_RESOURCE_NAME);
			attributes.put("classname", SharedClass.class.getTypeName());
			IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> factory.createBean(config, attributes));
			assertEquals("invalid attribute [classname]. Did you mean [className]?", e.getMessage());
		}
	}

	@Test
	public void testNoBeanName() throws Exception {
		try (TestConfiguration config = new TestConfiguration()) {
			SharedResourceFactory factory = config.createBean(SharedResourceFactory.class);
			Digester digester = mock(Digester.class);
			factory.setDigester(digester);
			Map<String, String> attributes = new HashMap<>();
			attributes.put("className", SharedClass.class.getTypeName());
			IllegalStateException e = assertThrows(IllegalStateException.class, () -> factory.createBean(config, attributes));
			assertEquals("Shared Resource must have a name", e.getMessage());
		}
	}

	@Test
	public void testBeanRegisteredWithConfiguration() throws Exception {
		try (TestConfiguration config = new TestConfiguration()) {
			SharedResourceFactory factory = config.createBean(SharedResourceFactory.class);
			Digester digester = mock(Digester.class);
			factory.setDigester(digester);
			Map<String, String> attributes = new HashMap<>();
			attributes.put("name", TEST_RESOURCE_NAME);
			attributes.put("className", SharedClass.class.getTypeName());
			Object sharedResource = factory.createBean(config, attributes);

			assertTrue(config.containsBean(SharedResource.SHARED_RESOURCE_PREFIX+TEST_RESOURCE_NAME));
			assertEquals(sharedResource, config.getBean(SharedResource.SHARED_RESOURCE_PREFIX+TEST_RESOURCE_NAME));
			assertEquals(TEST_RESOURCE_VALUE, ((SharedClass) sharedResource).getSharedResource());
			assertEquals(TEST_RESOURCE_VALUE, config.createBean(DummyClass.class).getSharedResource(TEST_RESOURCE_NAME));
		}
	}

	@Test
	public void testSharedResourceWithWrongType() throws Exception {
		try (TestConfiguration config = new TestConfiguration()) {
			SharedResourceFactory factory = config.createBean(SharedResourceFactory.class);
			Digester digester = mock(Digester.class);
			factory.setDigester(digester);
			Map<String, String> attributes = new HashMap<>();
			attributes.put("name", TEST_RESOURCE_NAME);
			attributes.put("className", SharedClass.class.getTypeName());
			Object sharedResource = factory.createBean(config, attributes);

			assertTrue(config.containsBean(SharedResource.SHARED_RESOURCE_PREFIX+TEST_RESOURCE_NAME));
			assertEquals(sharedResource, config.getBean(SharedResource.SHARED_RESOURCE_PREFIX+TEST_RESOURCE_NAME)); // SharedClass

			assertEquals(TEST_RESOURCE_VALUE, ((SharedClass) sharedResource).getSharedResource()); // String

			DummyClass2 dummyClass2 = config.createBean(DummyClass2.class);
			IllegalStateException e = assertThrows(IllegalStateException.class, () -> dummyClass2.getSharedResource(TEST_RESOURCE_NAME));
			assertEquals("Shared Resource ["+TEST_RESOURCE_NAME+"] may not be used here", e.getMessage());
		}
	}

	public static class SharedClass extends DummyClass implements SharedResource<String> {

		@Override
		public String getSharedResource() {
			return TEST_RESOURCE_VALUE;
		}

	}

	public static class DummyClass implements CanUseSharedResource<String> {

		private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
		private @Getter @Setter String name;
		private @Getter @Setter ApplicationContext applicationContext;
		private @Getter @Setter String localResource;

		@Override
		public void configure() throws ConfigurationException {
			// Nothing to configure
		}

		@Override
		public void start() {
			// Nothing to start
		}

		@Override
		public void stop() {
			// Nothing to stop
		}

		@Override
		public void setSharedResourceRef(String sharedResourceName) {
			// Nothing to set
		}

		@Override
		public Class<String> getObjectType() {
			return String.class;
		}

		@Override
		public boolean isRunning() {
			return false;
		}
	}

	public static class DummyClass2 implements CanUseSharedResource<Boolean> {

		private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
		private @Getter @Setter String name;
		private @Getter @Setter ApplicationContext applicationContext;
		private @Getter @Setter Boolean localResource;

		@Override
		public void configure() throws ConfigurationException {
			// Nothing to configure
		}

		@Override
		public void start() {
			// Nothing to start
		}

		@Override
		public void stop() {
			// Nothing to stop
		}

		@Override
		public void setSharedResourceRef(String sharedResourceName) {
			// Nothing to set
		}

		@Override
		public Class<Boolean> getObjectType() {
			return Boolean.class;
		}

		@Override
		public boolean isRunning() {
			return false;
		}
	}
}

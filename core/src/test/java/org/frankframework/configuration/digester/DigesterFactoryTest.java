package org.frankframework.configuration.digester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.pipes.EchoPipe;
import org.frankframework.pipes.PutInSessionPipe;
import org.frankframework.testutil.TestConfiguration;

public class DigesterFactoryTest {
	private TestConfiguration configuration;
	private GenericFactory factory;

	@BeforeEach
	public void setup() {
		configuration = new TestConfiguration();

		factory = new GenericFactory();
		factory.setDigesterRule(mock(DigesterRule.class));
	}

	@Test
	public void wrongClassNameAttribute() throws ClassNotFoundException {
		Map<String, String> attrs = new HashMap<>();
		attrs.put("classname", EchoPipe.class.getCanonicalName());
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> factory.createBean(configuration, attrs));
		assertEquals("invalid attribute [classname]. Did you mean [className]?", e.getMessage());
	}

	@Test
	public void noClassNameAttribute() throws ClassNotFoundException {
		Map<String, String> attrs = new HashMap<>();
		attrs.put("class", EchoPipe.class.getCanonicalName());
		IllegalStateException e = assertThrows(IllegalStateException.class, () -> factory.createBean(configuration, attrs));
		assertEquals("bean is missing mandatory attribute className", e.getMessage());
	}

	@Test
	public void testExplicitClassName() throws ClassNotFoundException {
		Map<String, String> attrs = new HashMap<>();
		attrs.put("className", EchoPipe.class.getCanonicalName());
		assertInstanceOf(EchoPipe.class, factory.createBean(configuration, attrs));
		assertTrue(configuration.getConfigurationWarnings().isEmpty());
	}

	@Test
	public void testImplicitClassName() throws ClassNotFoundException {
		Map<String, String> attrs = new HashMap<>();
		attrs.put("className", "org.frankframework.pipes.PutInSession");
		assertInstanceOf(PutInSessionPipe.class, factory.createBean(configuration, attrs));
		String expected = "PutInSessionPipe [org.frankframework.pipes.PutInSession] has been renamed to [org.frankframework.pipes.PutInSessionPipe]"
				+ ". Please use the new syntax or change the className attribute.";
		assertEquals(expected, configuration.getConfigurationWarnings().get(0));
	}
}

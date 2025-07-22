package org.frankframework.configuration.digester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.pipes.EchoPipe;
import org.frankframework.pipes.GetFromSessionPipe;
import org.frankframework.pipes.JsonWellFormedCheckerPipe;
import org.frankframework.pipes.PutInSessionPipe;
import org.frankframework.pipes.RemoveFromSessionPipe;
import org.frankframework.pipes.XmlWellFormedCheckerPipe;
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
	public void wrongClassNameAttribute() {
		Map<String, String> attrs = new HashMap<>();
		attrs.put("classname", EchoPipe.class.getCanonicalName());
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> factory.createBean(configuration, attrs));
		assertEquals("invalid attribute [classname]. Did you mean [className]?", e.getMessage());
	}

	@Test
	public void noClassNameAttribute() {
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

	@ParameterizedTest
	@MethodSource
	public void testImplicitClassName(String originalClassName, Class<?> expectedClass) throws ClassNotFoundException {
		String expectedClassName = originalClassName + "Pipe";
		String expectedPipeName = StringUtils.substringAfterLast(expectedClassName, ".");

		Map<String, String> attrs = new HashMap<>();
		attrs.put("className", originalClassName);
		assertInstanceOf(expectedClass, factory.createBean(configuration, attrs));

		String expected = "%s [%s] has been renamed to [%s]. Please use the new syntax or change the className attribute."
				.formatted(expectedPipeName, originalClassName, expectedClassName);

		assertEquals(expected, configuration.getConfigurationWarnings().get(0));
	}

	public static Stream<Arguments> testImplicitClassName() {
		return Stream.of(
			Arguments.of("org.frankframework.pipes.PutInSession", PutInSessionPipe.class),
			Arguments.of("org.frankframework.pipes.RemoveFromSession", RemoveFromSessionPipe.class),
			Arguments.of("org.frankframework.pipes.GetFromSession", GetFromSessionPipe.class),
			Arguments.of("org.frankframework.pipes.XmlWellFormedChecker", XmlWellFormedCheckerPipe.class),
			Arguments.of("org.frankframework.pipes.JsonWellFormedChecker", JsonWellFormedCheckerPipe.class)
		);
	}
}

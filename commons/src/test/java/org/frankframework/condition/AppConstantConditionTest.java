package org.frankframework.condition;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConstantConditionTest {

	@Test
	void testMatchesConditionTrue() {
		// Arrange
		AppConstantCondition condition = new AppConstantCondition();
		ConditionContext context = Mockito.mock(ConditionContext.class);
		AnnotatedTypeMetadata metadata = Mockito.mock(AnnotatedTypeMetadata.class);
		Environment environment = Mockito.mock(Environment.class);

		// Mock the environment behavior
		Mockito.when(context.getEnvironment()).thenReturn(environment);
		Mockito.when(environment.getProperty("test.property", "default")).thenReturn("expectedValue");

		// Mock the metadata behavior
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("name", "test.property");
		attributes.put("value", "expectedValue");
		attributes.put("defaultValue", "default");
		Mockito.when(metadata.getAnnotationAttributes("org.frankframework.condition.ConditionalOnAppConstants")).thenReturn(attributes);

		// Act
		boolean matches = condition.matches(context, metadata);

		// Assert
		assertTrue(matches);
	}

	@Test
	void testMatchesConditionFalse() {
		// Arrange
		AppConstantCondition condition = new AppConstantCondition();
		ConditionContext context = Mockito.mock(ConditionContext.class);
		AnnotatedTypeMetadata metadata = Mockito.mock(AnnotatedTypeMetadata.class);
		Environment environment = Mockito.mock(Environment.class);

		// Mock the environment behavior
		Mockito.when(context.getEnvironment()).thenReturn(environment);
		Mockito.when(environment.getProperty("test.property", "default")).thenReturn("unexpectedValue");

		// Mock the metadata behavior
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("name", "test.property");
		attributes.put("value", "expectedValue");
		attributes.put("defaultValue", "default");
		Mockito.when(metadata.getAnnotationAttributes("org.frankframework.condition.ConditionalOnAppConstants")).thenReturn(attributes);

		// Act
		boolean matches = condition.matches(context, metadata);

		// Assert
		assertFalse(matches);
	}

	@Test
	void testMatchesConditionWithMissingAttributes() {
		// Arrange
		AppConstantCondition condition = new AppConstantCondition();
		ConditionContext context = Mockito.mock(ConditionContext.class);
		AnnotatedTypeMetadata metadata = Mockito.mock(AnnotatedTypeMetadata.class);

		// Mock the metadata behavior to return null or incomplete attributes
		Mockito.when(metadata.getAnnotationAttributes("org.frankframework.condition.ConditionalOnAppConstants")).thenReturn(null);

		// Act
		boolean matches = condition.matches(context, metadata);

		// Assert
		assertFalse(matches);
	}

}

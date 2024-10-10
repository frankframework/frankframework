package org.frankframework.testutil.junit;

import static org.junit.platform.commons.util.AnnotationUtils.findRepeatableAnnotations;
import static org.junit.platform.commons.util.AnnotationUtils.isAnnotated;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.AnnotationConsumerInitializer;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.commons.util.ReflectionUtils;

public class JUnitDatabaseExtension implements TestTemplateInvocationContextProvider {

	static final Namespace NAMESPACE = Namespace.create(DatabaseTest.class);
	static final String DB_INSTANCE = "DB_CONTEXT";

	@Override
	public boolean supportsTestTemplate(ExtensionContext context) {
		if (context.getTestMethod().isEmpty()) {
			return false;
		}

		Method testMethod = context.getRequiredTestMethod();
		if (!isAnnotated(testMethod, DatabaseTest.class)) {
			throw new JUnitException("Missing DatabaseTest annotation");
		}

		if(findRepeatableAnnotations(testMethod, ArgumentsSource.class).isEmpty()) {
			throw new JUnitException("Missing Database/TX Execution Matrix");
		}

		return true;
	}

	@Override
	public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
		Method templateMethod = context.getRequiredTestMethod();

		return findRepeatableAnnotations(templateMethod, ArgumentsSource.class)
				.stream()
				.map(ArgumentsSource::value)
				.map(ReflectionUtils::newInstance)
				.map(provider -> AnnotationConsumerInitializer.initialize(templateMethod, provider))
				.flatMap(provider -> arguments(provider, context))
				.map(Arguments::get)
				.map(arguments -> new DatabaseTestInvocationContext(templateMethod, arguments));
	}

	protected static Stream<? extends Arguments> arguments(ArgumentsProvider provider, ExtensionContext context) {
		try {
			return provider.provideArguments(context);
		} catch (Exception e) {
			throw ExceptionUtils.throwAsUncheckedException(e);
		}
	}
}

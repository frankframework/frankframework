package nl.nn.adapterframework.testutil.junit;

import static org.junit.platform.commons.util.AnnotationUtils.findRepeatableAnnotations;
import static org.junit.platform.commons.util.AnnotationUtils.isAnnotated;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.AnnotationConsumerInitializer;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.commons.util.ReflectionUtils;

public class JUnitDatabaseExtension implements TestTemplateInvocationContextProvider {

	static final Namespace NAMESPACE = Namespace.create(DatabaseTest.class);
	static final String DB_INSTANCE = "DB_CONTEXT";

	@Override
	public boolean supportsTestTemplate(ExtensionContext context) {
		if (!context.getTestMethod().isPresent()) {
			return false;
		}

		Method testMethod = context.getTestMethod().get();
		if (!isAnnotated(testMethod, DatabaseTest.class)) {
			return false;
		}

		return true;
	}

	@Override
	public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
		AtomicLong invocationCount = new AtomicLong(0);
		Method templateMethod = context.getRequiredTestMethod();

		return findRepeatableAnnotations(templateMethod, ArgumentsSource.class)
				.stream()
				.map(ArgumentsSource::value)
				.map(ReflectionUtils::newInstance)
				.map(provider -> AnnotationConsumerInitializer.initialize(templateMethod, provider))
				.flatMap(provider -> arguments(provider, context))
				.map(Arguments::get)
				.map(arguments -> {
					invocationCount.incrementAndGet();
					return new DatabaseTestInvocationContext(templateMethod, arguments, invocationCount.intValue());
				});
	}

	protected static Stream<? extends Arguments> arguments(ArgumentsProvider provider, ExtensionContext context) {
		try {
			return provider.provideArguments(context);
		} catch (Exception e) {
			throw ExceptionUtils.throwAsUncheckedException(e);
		}
	}
}

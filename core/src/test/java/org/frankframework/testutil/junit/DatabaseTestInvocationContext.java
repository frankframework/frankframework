package org.frankframework.testutil.junit;

import static java.util.Collections.singletonList;
import static org.junit.platform.commons.util.ReflectionUtils.makeAccessible;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.AnnotationUtils;
import org.junit.platform.commons.util.ReflectionUtils;

import lombok.extern.log4j.Log4j2;

import org.frankframework.testutil.TransactionManagerType;

class DatabaseTestInvocationContext implements TestTemplateInvocationContext {

	private final Method testMethod;
	private final Object[] arguments;

	public DatabaseTestInvocationContext(Method testMethod, Object[] arguments) {
		this.testMethod = testMethod;
		this.arguments = arguments;
	}

	@Override
	public List<Extension> getAdditionalExtensions() {
		return singletonList(new DatabaseTestParameterResolver(testMethod, arguments));
	}

	@Override
	public String getDisplayName(int invocationIndex) {
		return Arrays.stream(arguments).map(Objects::toString).collect(Collectors.joining(" - "));
	}

	@Log4j2
	private static class DatabaseTestParameterResolver implements ParameterResolver, BeforeEachCallback, AfterEachCallback {
		private final Object[] arguments;
		private final boolean cleanupAfterUse;
		private final DatabaseTestEnvironment dte;

		public DatabaseTestParameterResolver(Method testMethod, Object[] arguments) {
			this.arguments = arguments;

			DatabaseTestOptions annotation = AnnotationUtils.findAnnotation(testMethod, DatabaseTestOptions.class)
					.orElseThrow(()->new JUnitException("unable to find DatabaseTest annotation"));
			boolean cleanupBeforeUse = annotation.cleanupBeforeUse();
			cleanupAfterUse = annotation.cleanupAfterUse();

			if(cleanupBeforeUse) {
				log.info("cleanup TransactionManager before executing test");
				TransactionManagerType.closeAllConfigurationContexts();
			}

			this.dte = new DatabaseTestEnvironment((TransactionManagerType) arguments[0], (String)arguments[1]);
		}

		@Override
		public void beforeEach(ExtensionContext context) throws Exception {
			Object testInstance = context.getRequiredTestInstances().getInnermostInstance();
			setAnnotatedFields(testInstance, testInstance.getClass());

			//Always store the database context, so it's closed after each test.
			getStore(context).put(JUnitDatabaseExtension.DB_INSTANCE, dte);
		}

		@Override
		public void afterEach(ExtensionContext context) throws Exception {
			if(cleanupAfterUse) {
				log.info("cleanup TransactionManager after executing test");
				TransactionManagerType.closeAllConfigurationContexts();
			}
		}

		private void setAnnotatedFields(Object instance, Class<?> testClass) {
			AnnotationUtils.findAnnotatedFields(testClass, DatabaseTestOptions.Parameter.class, ReflectionUtils::isNotStatic).forEach(field -> {
				assertNonFinalField(field);
				int index = field.getDeclaredAnnotation(DatabaseTestOptions.Parameter.class).value();

				Object valueToSet = arguments[index];
				if(valueToSet != null) { //Skip null values
					if(!field.getType().isAssignableFrom(valueToSet.getClass())) {
						throw new ExtensionConfigurationException("Unable to set @Parameter field ["+field.getName()+"] to value ["+valueToSet+"] type mismatch.");
					}

					try {
						makeAccessible(field).set(instance, valueToSet);
					} catch (IllegalArgumentException | IllegalAccessException e) {
						throw new ExtensionConfigurationException("Unable to set @Parameter field ["+field.getName()+"] to value ["+valueToSet+"].", e);
					}
				}
			});
		}

		private static void assertNonFinalField(Field field) {
			if (ReflectionUtils.isFinal(field)) {
				throw new ExtensionConfigurationException("@Parameter field [" + field + "] must not be declared as final.");
			}
		}

		@Override
		public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
			Parameter p = parameterContext.getParameter();
			return p.getType().isAssignableFrom(DatabaseTestEnvironment.class);
		}

		@Override
		public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
			return dte;
		}
	}

	private static ExtensionContext.Store getStore(ExtensionContext context) {
		return context.getStore(Namespace.create(DatabaseTestEnvironment.class, context.getRequiredTestMethod()));
	}

	static DatabaseTestEnvironment getDatabaseTestEnvironment(ExtensionContext context) {
		return getStore(context).get(JUnitDatabaseExtension.DB_INSTANCE, DatabaseTestEnvironment.class);
	}
}

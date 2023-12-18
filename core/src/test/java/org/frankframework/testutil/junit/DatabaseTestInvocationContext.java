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

	private static class DatabaseTestParameterResolver implements ParameterResolver, BeforeEachCallback {
		private final Object[] arguments;
		private final boolean cleanupBeforeUse;

		public DatabaseTestParameterResolver(Method testMethod, Object[] arguments) {
			this.arguments = arguments;

			DatabaseTest annotation = AnnotationUtils.findAnnotation(testMethod, DatabaseTest.class)
					.orElseThrow(()->new JUnitException("unable to find DatabaseTest annotation"));
			cleanupBeforeUse = annotation.cleanupBeforeUse();
		}

		@Override
		public void beforeEach(ExtensionContext context) throws Exception {
			Object testInstance = context.getRequiredTestInstances().getInnermostInstance();
			setAnnotatedFields(testInstance, testInstance.getClass());

			if(cleanupBeforeUse) {
				TransactionManagerType.closeAllConfigurationContexts();
			}
		}

		private void setAnnotatedFields(Object instance, Class<?> testClass) {
			AnnotationUtils.findAnnotatedFields(testClass, DatabaseTest.Parameter.class, ReflectionUtils::isNotStatic).forEach(field -> {
				assertNonFinalField(field);
				int index = field.getDeclaredAnnotation(DatabaseTest.Parameter.class).value();

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
			DatabaseTestEnvironment dbEnv = new DatabaseTestEnvironment((TransactionManagerType) arguments[0], (String)arguments[1]);
			getStore(extensionContext).put(JUnitDatabaseExtension.DB_INSTANCE, dbEnv);
			return dbEnv;
		}
	}

	private static ExtensionContext.Store getStore(ExtensionContext context) {
		return context.getStore(Namespace.create(DatabaseTestEnvironment.class, context.getRequiredTestMethod()));
	}

	static DatabaseTestEnvironment getDatabaseTestEnvironment(ExtensionContext context) {
		return getStore(context).get(JUnitDatabaseExtension.DB_INSTANCE, DatabaseTestEnvironment.class);
	}
}

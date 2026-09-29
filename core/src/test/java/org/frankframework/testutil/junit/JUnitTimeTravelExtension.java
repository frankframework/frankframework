package org.frankframework.testutil.junit;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import org.frankframework.util.CloseUtils;
import org.frankframework.util.TimeProvider;

@NullMarked
public class JUnitTimeTravelExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

	private static final ExtensionContext.Namespace TIME_TRAVEL_NAMESPACE = ExtensionContext.Namespace.create(JUnitTimeTravelExtension.class);
	private static final String TIME_TRAVELLER_STORE_KEY = "timeTraveller";

	@Override
	public void beforeEach(ExtensionContext context) {
		TimeProvider.TimeTraveller timeTraveller = TimeProvider.timeTraveller();
		context.getStore(TIME_TRAVEL_NAMESPACE).put(TIME_TRAVELLER_STORE_KEY, timeTraveller);
	}

	@Override
	public void afterEach(ExtensionContext context) {
		TimeProvider.TimeTraveller timeTraveller = (TimeProvider.TimeTraveller) context.getStore(TIME_TRAVEL_NAMESPACE).get(TIME_TRAVELLER_STORE_KEY);
		CloseUtils.closeSilently(timeTraveller);
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
		return TimeProvider.TimeTraveller.class.isAssignableFrom(parameterContext.getParameter().getType());
	}

	@Override
	public @Nullable Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
		return extensionContext.getStore(TIME_TRAVEL_NAMESPACE).get(TIME_TRAVELLER_STORE_KEY);
	}
}

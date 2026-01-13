package org.frankframework.util.testresolvers;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import org.frankframework.util.AdditionalStringResolver;

/**
 * Test implementation of {@link AdditionalStringResolver} which always returns {@code null}.
 */
@NullMarked
public class NullResolver implements AdditionalStringResolver {
	@Override
	public Optional<String> resolve(String key, Map<?, ?> props1, @Nullable Map<?, ?> props2, @Nullable Set<String> propsToHide, String delimStart, String delimStop, boolean resolveWithPropertyName) {
		return Optional.empty();
	}
}

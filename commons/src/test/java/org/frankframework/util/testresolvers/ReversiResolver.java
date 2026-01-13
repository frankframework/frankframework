package org.frankframework.util.testresolvers;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import org.frankframework.util.AdditionalStringResolver;

/**
 * Test implementation of {@link AdditionalStringResolver} which reverses the key as output if
 * the key was input-value {@code "reversi"}.
 */
@NullMarked
public class ReversiResolver implements AdditionalStringResolver {
	@Override
	public Optional<String> resolve(String key, Map<?, ?> props1, @Nullable Map<?, ?> props2, @Nullable Set<String> propsToHide, String delimStart, String delimStop, boolean resolveWithPropertyName) {
		if ("reversi".equals(key)) {
			return Optional.of(StringUtils.reverse(key));
		} else {
			return Optional.empty();
		}
	}
}

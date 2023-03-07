package nl.nn.adapterframework.util.testresolvers;

import java.util.List;
import java.util.Map;

import nl.nn.adapterframework.util.AdditionalStringResolver;

/**
 * Test implementation of {@link AdditionalStringResolver} which always returns {@code null}.
 */
public class NullResolver implements AdditionalStringResolver {
	@Override
	public String resolve(String key, Map<?, ?> props1, Map<?, ?> props2, List<String> propsToHide, String delimStart, String delimStop, boolean resolveWithPropertyName) {
		return null;
	}
}

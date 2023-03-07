package nl.nn.adapterframework.util.testresolvers;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.util.AdditionalStringResolver;

/**
 * Test implementation of {@link AdditionalStringResolver} which reverses the key as output if
 * the key was input-value {@code "reversi"}.
 */
public class ReversiResolver implements AdditionalStringResolver {
	@Override
	public String resolve(String key, Map<?, ?> props1, Map<?, ?> props2, List<String> propsToHide, String delimStart, String delimStop, boolean resolveWithPropertyName) {
		if ("reversi".equals(key)) {
			return StringUtils.reverse(key);
		} else {
			return null;
		}
	}
}

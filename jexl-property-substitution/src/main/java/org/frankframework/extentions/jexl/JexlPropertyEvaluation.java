package org.frankframework.extentions.jexl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;

import org.apache.commons.collections4.map.CompositeMap;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlFeatures;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.apache.commons.lang3.StringUtils;

import lombok.extern.log4j.Log4j2;

import org.frankframework.util.AdditionalStringResolver;
import org.frankframework.util.StringUtil;

/**
 * Try to evaluate the key as a JEXL expression.
 *
 * @see <a href="https://commons.apache.org/proper/commons-jexl/">Apache JEXL Homepage</a>
 */
@Log4j2
public class JexlPropertyEvaluation implements AdditionalStringResolver {

	private final JexlEngine jexl;

	private final Set<String> failedExpressions = new HashSet<>();

	public JexlPropertyEvaluation() {
		JexlFeatures features = new JexlFeatures()
				.loops(false)
				.sideEffectGlobal(false)
				.sideEffect(false)
				.annotation(false)
				.importPragma(true);

		JexlPermissions jexlPermissions = JexlPermissions.RESTRICTED
				.compose("org.frankframework.*")
				.compose("nl.nn.adapterframework.*")
				.compose("org.apache.commons.lang3.*")
				.compose("org.apache.commons.collections4.*")
				;

		jexl = new JexlBuilder()
				.features(features)
				.permissions(jexlPermissions)
				.create();
	}

	@Override
	public Optional<String> resolve(String key, Map<?, ?> props1, Map<?, ?> props2, Set<String> propsToHide, String delimStart, String delimStop, boolean resolveWithPropertyName) {
		// Don't retry keys which already gave an exception once
		if (failedExpressions.contains(key)) {
			return Optional.empty();
		}

		try {
			JexlScript expression = jexl.createScript(key);
			@SuppressWarnings("unchecked")
			JexlContext context = createJexlContext((Map<String, Object>) props1, (Map<String, Object>) props2);

			Object result = expression.execute(context);
			if (result == null) {
				return Optional.empty();
			} else {
				return Optional.of(result.toString());
			}
		} catch (Exception e) {
			// This probably wasn't a JEXL script. Pass on to other string resolvers.
			log.debug(() -> "Cannot evaluate [%s] as JEXL expression".formatted(key), e);
			failedExpressions.add(key);
			return Optional.empty();
		}
	}

	@Nonnull
	private static JexlContext createJexlContext(Map<String, Object> props1, Map<String, Object> props2) {
		// Create basic context
		Map<String, Object> contextCustomValues = new HashMap<>();
		CompositeMap.MapMutator<String, Object> mutator = new CompositeMap.MapMutator<>() {

			@Override
			public Object put(CompositeMap<String, Object> map, Map<String, Object>[] composited, String key, Object value) {
				return contextCustomValues.put(key, value);
			}

			@Override
			public void putAll(CompositeMap<String, Object> map, Map<String, Object>[] composited, Map<? extends String, ?> mapToAdd) {
				contextCustomValues.putAll(mapToAdd);
			}

			@Override
			public void resolveCollision(CompositeMap<String, Object> composite, Map<String, Object> existing, Map<String, Object> added, Collection<String> intersect) {
				throw new UnsupportedOperationException();
			}
		};
		CompositeMap<String, Object> contextBackingMap;
		if (props2 == null) {
			contextBackingMap = new CompositeMap<>(contextCustomValues, props1);
		} else {
			// The 'props' maps might be "smart" map-like objects like PropertyLoader that do value-substitution. So they should not be copied into a single map, but queried directly.
			contextBackingMap = new CompositeMap<>(contextCustomValues, props1, props2);
		}
		contextBackingMap.setMutator(mutator);
		JexlContext context = new MapContext(contextBackingMap);

		// Make static methods from some common util classes easily available
		context.set("Collections", Collections.class);
		context.set("Collectors", Collectors.class);
		context.set("Math", Math.class);
		context.set("StringUtils", StringUtils.class);
		context.set("StringUtil", StringUtil.class);

		return context;
	}
}

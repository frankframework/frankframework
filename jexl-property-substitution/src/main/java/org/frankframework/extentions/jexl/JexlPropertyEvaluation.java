/*
   Copyright 2025 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.extentions.jexl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	private static final String EXPRESSION_START_TOKEN = "=";

	private final JexlEngine jexl;

	private final Set<String> invalidExpressions = new HashSet<>();

	public JexlPropertyEvaluation() {
		JexlFeatures features = new JexlFeatures()
				.loops(false)
				.sideEffectGlobal(true)
				.sideEffect(true)
				.annotation(false)
				.lambda(true)
				.arrayReferenceExpr(true)
				.methodCall(true)
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
				.antish(true)
				.create();
	}

	@Override
	public Optional<String> resolve(String key, Map<?, ?> props1, Map<?, ?> props2, Set<String> propsToHide, String delimStart, String delimStop, boolean resolveWithPropertyName) {
		// Don't retry keys which already gave an exception once
		if (invalidExpressions.contains(key) || !key.startsWith(EXPRESSION_START_TOKEN)) {
			return Optional.empty();
		}

		JexlScript expression;
		try {
			expression = jexl.createScript(key.substring(EXPRESSION_START_TOKEN.length()));
		} catch (Exception e) {
			// This probably wasn't a JEXL script. Pass on to other string resolvers, don't try same key again.
			log.error(() -> "Cannot parse [%s] as JEXL expression".formatted(key), e);
			invalidExpressions.add(key);
			return Optional.empty();
		}

		try {
			@SuppressWarnings("unchecked")
			JexlContext context = createJexlContext((Map<String, Object>) props1, (Map<String, Object>) props2);

			Object result = expression.execute(context);
			if (result == null) {
				return Optional.empty();
			} else {
				return Optional.of(result.toString());
			}
		} catch (Exception e) {
			// Script was probably valid but not in the context of variables given
			log.error(() -> "Cannot evaluate JEXL expression [%s]".formatted(key), e);
			return Optional.empty();
		}
	}

	@Nonnull
	private static JexlContext createJexlContext(Map<String, Object> props1, Map<String, Object> props2) {
		// Create basic context
		CompositeMap<String, Object> contextMap = createContextMap(props1, props2);
		JexlContext context = new StreamContext(contextMap);

		// Make static methods from some common util classes easily available
		context.set("Collections", Collections.class);
		context.set("Collectors", Collectors.class);
		context.set("Math", Math.class);
		context.set("StringUtils", StringUtils.class);
		context.set("StringUtil", StringUtil.class);
		context.set("log", log);

		return context;
	}

	@SuppressWarnings("unchecked")
	@Nonnull
	private static CompositeMap<String, Object> createContextMap(Map<String, Object> props1, Map<String, Object> props2) {
		Map<String, Object> contextCustomValues = new HashMap<>();
		CompositeMap.MapMutator<String, Object> mutator = new BackingMapMutator(contextCustomValues);

		CompositeMap<String, Object> contextMap;
		if (props2 == null) {
			contextMap = new CompositeMap<>(contextCustomValues, props1);
		} else {
			// The 'props' maps might be "smart" map-like objects like PropertyLoader that do value-substitution. So they should not be copied into a single map, but queried directly.
			contextMap = new CompositeMap<>(contextCustomValues, props1, props2);
		}
		contextMap.setMutator(mutator);
		return contextMap;
	}

	private static class BackingMapMutator implements CompositeMap.MapMutator<String, Object> {

		private final transient Map<String, Object> contextCustomValues;

		public BackingMapMutator(Map<String, Object> contextCustomValues) {
			this.contextCustomValues = contextCustomValues;
		}

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
	}

	/**
	 * A MapContext that can operate on streams.
	 *
	 * Based on example in https://commons.apache.org/proper/commons-jexl/
	 */
	public static class StreamContext extends MapContext {

		public StreamContext(Map<String, Object> contextMap) {
			super(contextMap);
		}

		/**
		 * This allows using a JEXL lambda as a mapper.
		 * @param stream the stream
		 * @param mapper the lambda to use as mapper
		 * @return the mapped stream
		 */
		public Stream<?> map(Stream<?> stream, final JexlScript mapper) {
			return stream.map( x -> mapper.execute(this, x));
		}

		/**
		 * This allows using a JEXL lambda as a filter.
		 * @param stream the stream
		 * @param filter the lambda to use as filter
		 * @return the filtered stream
		 */
		public Stream<?> filter(Stream<?> stream, final JexlScript filter) {
			return stream.filter(x -> Boolean.TRUE.equals(filter.execute(this, x)));
		}
	}
}

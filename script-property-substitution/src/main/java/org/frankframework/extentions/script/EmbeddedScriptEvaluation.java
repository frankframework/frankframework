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
package org.frankframework.extentions.script;

import java.util.Arrays;
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
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import lombok.extern.log4j.Log4j2;

import org.frankframework.util.AdditionalStringResolver;
import org.frankframework.util.StringUtil;

/**
 * Evaluate a JEXL expression embedded in a string substitution performed by {@link org.frankframework.util.StringResolver}.
 * For a string substitution to be evaluated as expression, it has to be prefixed with a {@value EXPRESSION_START_TOKEN} inside
 * the string substitution delimiters, so by default this would be {@code ${=...}}.
 * <p>
 *     Inside the expression you can reference other properties from the source maps, either as nested string substitutions
 *     or as direct references. Since Java properties are loaded as string values, numerical or boolean values would be
 *     strings if referenced directly and this can have confusing effects when using them in calculations or boolean evaluations.
 * </p>
 * <h3>Some examples</h3>
 * <p>
 *     <pre>{@code
 *     my.custom.value=Hello!
 *     padding.size=5
 *     total.length=${=my.custom.value.length() + ${padding.size}}
 *     }</pre>
 *     The result of evaluating {@code total.length} would be 11. The padding size is used here as an embedded string
 *     substitution, which is evaluated before the expression is evaluated. As you can see, it is possible to use regular Java string
 *     functions inside the expressions.
 * </p>
 * <p>
 *     <pre>{@code
 *     value1=5
 *     value2=10
 *     sum=${=value1 + value2}
 *     }</pre>
 *     Because direct references are strings when loaded as Java properties, the result of this expression is the string-concatenations {@literal 510} instead of the sum {@literal 15}.
 *     To get the correct outcome, you can write either:
 *     <pre>{@code
 *     value1=5
 *     value2=10
 *     sum=${=Integer.parseInt(value1) + Integer.parseInt(value2)}
 *     }</pre>
 *     Or, perhaps easier:
 *     <pre>{@code
 *     value1=5
 *     value2=10
 *     sum=${=${value1} + ${value2}}
 *     }</pre>
 * </p>
 * <p>
 *     Here is an example with a conditional evaluation giving a warning:
 *     <pre>{@code
 *     remote.host=
 *     remote.port=
 *     remote.url=${= if (StringUtils.isBlank(remote.host) || StringUtils.isBlank(remote.port) { ApplicationWarnings.add(log, "properties remote.host and remote.port should be set"} else { return "http://$s:$s/api/".formatted(remote.host, remote.port); }}
 *     }</pre>
 * </p>
 *
 * <h3>Available Classes</h3>
 * The following classes are available in the evaluation context so that static methods of these classes can be
 * used in expressions:
 * <ul>
 *     <li>{@link java.lang.String}</li>
 *     <li>{@link java.lang.Boolean}</li>
 *     <li>{@link java.lang.Integer}</li>
 *     <li>{@link java.lang.Long}</li>
 *     <li>{@link java.lang.Double}</li>
 *     <li>{@link java.lang.Math}</li>
 *     <li>{@link java.util.Arrays}</li>
 *     <li>{@link java.util.Collections}</li>
 *     <li>{@link java.util.stream.Collectors}</li>
 *     <li>{@link org.apache.commons.lang3.Strings}</li>
 *     <li>{@link org.apache.commons.lang3.StringUtils}</li>
 *     <li>{@link org.frankframework.util.StringUtil}</li>
 *     <li>{@link org.frankframework.util.Misc}</li>
 *     <li>{@link org.frankframework.configuration.ApplicationWarnings}</li>
 * </ul>
 * You should not use the package names when using these classes, just the class name and the method name. See the examples above.
 *
 * @see <a href="https://commons.apache.org/proper/commons-jexl/">Apache JEXL Homepage</a>
 */
@Log4j2
public class EmbeddedScriptEvaluation implements AdditionalStringResolver {
	private static final String EXPRESSION_START_TOKEN = "=";

	private final JexlEngine jexl;

	private final Set<String> invalidExpressions = new HashSet<>();

	public EmbeddedScriptEvaluation() {
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
				.debug(true)
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
			log.error(() -> "Cannot parse [%s] as JEXL expression".formatted(key.substring(EXPRESSION_START_TOKEN.length())), e);
			invalidExpressions.add(key);
			return Optional.empty();
		}

		try {
			@SuppressWarnings("unchecked")
			JexlContext context = createScriptContext((Map<String, Object>) props1, (Map<String, Object>) props2);

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
	private static JexlContext createScriptContext(Map<String, Object> props1, Map<String, Object> props2) {
		// Create basic context
		CompositeMap<String, Object> contextMap = createContextMap(props1, props2);
		JexlContext context = new FrankScriptContext(contextMap);

		// Make static methods from some common types and util classes easily available
		addClasses(context, String.class, Boolean.class, Integer.class, Long.class, Double.class, Math.class,
				Arrays.class, Collections.class, Collectors.class, Strings.class, StringUtils.class, StringUtil.class);

		context.set("log", log); // log is needed for method calls on ApplicationWarnings

		// Some classes to load dynamically from other modules
		tryAddClassesDynamically(context, "org.frankframework.configuration.ApplicationWarnings", "org.frankframework.util.Misc");
		return context;
	}

	private static void addClasses(@Nonnull JexlContext context, Class<?>... classes) {
		for (Class<?> clazz : classes) {
			addClass(context, clazz);
		}
	}

	private static void tryAddClassesDynamically(@Nonnull JexlContext context, @Nonnull String... classNames) {
		for (String className : classNames) {
			tryAddClassDynamically(context, className);
		}
	}

	private static void addClass(@Nonnull JexlContext context, @Nonnull Class<?> clazz) {
		context.set(clazz.getSimpleName(), clazz);
	}

	private static void tryAddClassDynamically(@Nonnull JexlContext context, @Nonnull String className) {
		try {
			Class<?> cls = EmbeddedScriptEvaluation.class.getClassLoader().loadClass(className);
			context.set(cls.getSimpleName(), cls);
		} catch (Exception e) {
			log.info("Cannot load class [{}]: {}",  className, e.getMessage());
		}
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

		public BackingMapMutator(@Nonnull Map<String, Object> contextCustomValues) {
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
}

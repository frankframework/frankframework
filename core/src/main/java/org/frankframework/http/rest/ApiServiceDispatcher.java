/*
Copyright 2017-2021e2025 WeAreFrank!

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
package org.frankframework.http.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.json.JsonObject;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.IPipe;
import org.frankframework.core.PipeLine;
import org.frankframework.http.openapi.OpenApiGenerator;
import org.frankframework.pipes.Json2XmlValidator;

/**
 * This class registers dispatches requests to the proper registered ApiListeners.
 * The dispatcher does not handle nor does it process messages!
 *
 * @author Niels Meijer
 *
 */
@Log4j2
public class ApiServiceDispatcher {

	private final ConcurrentSkipListMap<String, ApiDispatchConfig> patternClients = new ConcurrentSkipListMap<>();
	private static ApiServiceDispatcher self = null;

	public static synchronized ApiServiceDispatcher getInstance() {
		if( self == null ) {
			self = new ApiServiceDispatcher();
		}
		return self;
	}

	/**
	 * Find all {@link ApiDispatchConfig} instances for a given URI for purpose of generating
	 * OpenAPI spec.
	 * <br/>
	 * For this purpose, dispatch configurations for matching-patterns ending with {@code /**}
	 * are not included in the output of this method.
	 * <br/>
	 * URI patterns are matched up to have full or partial match for the length of the request URI,
	 * and returned URI patterns may have more segments than the request.
	 *
	 * @param uri URI for which to find matching {@link ApiDispatchConfig} instances.
	 * @return List of {@link ApiDispatchConfig} instances matching the start of this request URI. (Nonnull, may be empty).
	 */
	@Nonnull
	public List<ApiDispatchConfig> findAllMatchingConfigsForUri(String uri) {
		return findMatchingConfigsForUri(uri, false);
	}

	/**
	 * Find an {@link ApiDispatchConfig} that has an exact match with the request URI, for purpose
	 * of generating the OpenAPI spec from it.
	 * Therefor no dispatch configurations for matching-patterns ending with {@code /**} are returned.
	 *
	 * @param uri The full URI for which to generate an OpenAPI spec.
	 * @return The {@link ApiDispatchConfig} from which to generate an OpenAPI spec.
	 */
	@Nullable
	public ApiDispatchConfig findExactMatchingConfigForUri(@Nonnull String uri) {
		List<ApiDispatchConfig> configs = findMatchingConfigsForUri(uri, true);
		return configs.stream()
				.filter(cfg -> !cfg.getUriPattern().endsWith("/**"))
				.max(Comparator.comparingInt(cfg -> scoreUriPattern(cfg.getUriPattern())))
				.orElse(null);
	}

	/**
	 * Find the {@link ApiDispatchConfig} best matching a given request, consisting of the
	 * HTTP request method and request URI.
	 * <p>
	 *     This method will return the {@link ApiDispatchConfig} that has the most specific match
	 *     with the request URI, and supports the requested HTTP method.
	 * </p>
	 * <p>
	 *     {@link org.frankframework.http.rest.ApiListener.HttpMethod#OPTIONS} requests and requests for methods that are not supported by the configuration are always matched to the most specific URI pattern match.
	 *     This is so that the {@code OPTIONS} request will return correct results, and requests for unsupported methods will return HTTP status code
	 *     {@code 405} instead of {@code 404}.
	 * </p>
	 * <p>
	 *     So for instance if a configuration would have the following {@link ApiListener}s installed:
	 *     <lu>
	 *         <li>ApiListener1: GET on uri /user/**</li>
	 *         <li>ApiListener2: GET on uri /user/{userId}/department/{departmentId}</li>
	 *         <li>ApiListener3: POST on uri /user/{userId}/avatar</li>
	 *     </lu>
	 *     Then:
	 *     <lu>
	 *         <li>A request {@code GET /user/usr123/department/dept456} would return the {@link ApiDispatchConfig} for /user/{userId}/department/{departmentId} containing ApiListener2</li>
	 *         <li>A request {@code OPTIONS /user/usr123/department/dept456} would return the {@link ApiDispatchConfig} for /user/{userId}/department/{departmentId} containing ApiListener2</li>
	 *         <li>A request {@code GET /user/usr123/avatar} would return the {@link ApiDispatchConfig} for /user/** containing ApiListener1</li>
	 *         <li>A request {@code POST /user/usr123/avatar} would return the {@link ApiDispatchConfig} for /user/{userId}/avatar containing ApiListener3</li>
	 *         <li>A request {@code PUT /user/usr123/avatar} (method PUT has not been configured!) would return the {@link ApiDispatchConfig} for /user/{userId}/avatar containing ApiListener3. Error handling can then be specific about the URL hit not supporting this method.</li>
	 *     </lu>
	 * </p>
	 *
	 * @param method {@link ApiListener.HttpMethod} of the HTTP request received
	 * @param requestUri URI of the HTTP request received
	 * @return The best matching {@link ApiDispatchConfig}, or {@code null}.
	 */
	@Nullable
	public ApiDispatchConfig findConfigForRequest(@Nonnull ApiListener.HttpMethod method, @Nonnull String requestUri) {
		List<ApiDispatchConfig> configs = findMatchingConfigsForUri(requestUri, true);
		return configs.stream()
				.max(Comparator.comparingInt(cfg -> scoreRequestMethodMatch(method, cfg) + scoreUriPattern(cfg.getUriPattern())))
				.orElse(null);
	}

	/**
	 * Calculate a numerical score for how well a given HTTP request {@link org.frankframework.http.rest.ApiListener.HttpMethod} is matched
	 * by the given {@link ApiDispatchConfig}.
	 * <p>
	 *     <lu>
	 *         <li>
	 *             The score is positive 10 when the {@code HttpMethod} is {@link org.frankframework.http.rest.ApiListener.HttpMethod#OPTIONS} or
	 *             {@link ApiDispatchConfig#hasMethod(ApiListener.HttpMethod)} returns {@code true}.
	 *         </li>
	 *         <li>
	 *             In all other cases the score is negative 10.
	 *         </li>
	 *     </lu>
	 * </p>
	 * @param requestMethod The {@link org.frankframework.http.rest.ApiListener.HttpMethod} of the request
	 * @param config The {@link ApiDispatchConfig} against which to match the request method
	 * @return The calculated score of the match.
	 */
	public static int scoreRequestMethodMatch(ApiListener.HttpMethod requestMethod, ApiDispatchConfig config) {
		if (requestMethod == ApiListener.HttpMethod.OPTIONS || config.hasMethod(requestMethod)) return 10;
		else return -10;
	}

	/**
	 * Calculate a numerical score for a URI pattern indicating how specific it is, based on the number of segments and wildcards.
	 * <p>
	 *     The intent is to have a higher score the more specific a URI pattern is, thus the more segments
	 *     the more specific the higher the score but the more wildcards, the less specific a patter is relative
	 *     to another pattern of the same number of segments.
	 * </p>
	 * <p>
	 *     Patterns ending with a {@code /**} "match all" wildcard are always scored as
	 *     less specific than patterns which do not have the "match all" wildcard.
	 * </p>
	 * <p>
	 *     Scoring rules:
	 *     <lu>
	 *         <li>The more slashes the longer the match the more specific</li>
	 *         <li>The more wildcards in the pattern the less specific</li>
	 *         <li>"Match-all" patterns ending with /** are penalized with a -10 starting score</li>
	 *     </lu>
	 * </p>
	 * @param uriPattern A pattern of a URI containing wildcards
	 * @return Numerical score calculated for the URI based on the rules above.
	 */
	public static int scoreUriPattern(@Nonnull String uriPattern) {
		int startValue = uriPattern.endsWith("/**") ? -10 : 0;
		return uriPattern.chars()
				.reduce(startValue, (cnt, chr) -> switch ((char) chr) {
					case '/' -> cnt + 1;
					case '*' -> cnt - 1;
					default -> cnt;
				});
	}

	@Nonnull
	private List<ApiDispatchConfig> findMatchingConfigsForUri(@Nonnull String uri, boolean matchFullPattern) {
		List<ApiDispatchConfig> results = new ArrayList<>();

		String[] uriSegments = uri.split("/");

		for (Map.Entry<String,ApiDispatchConfig> entry : patternClients.entrySet()) {
			String uriPattern = entry.getKey();
			if (log.isTraceEnabled()) log.trace("comparing uri [{}] to pattern [{}]", uri, uriPattern);

			String[] patternSegments = uriPattern.split("/");

			if (!isPotentialMatch(matchFullPattern, patternSegments, uriSegments)) continue;

			if (isMatch(uriSegments, patternSegments)) {
				ApiDispatchConfig result = entry.getValue();
				results.add(result);
			}
		}
		return results;
	}

	private static boolean isMatch(String[] uriSegments, String[] patternSegments) {
		int matchingSegmentCount = 0;
		for (int i = 0; i < uriSegments.length && i < patternSegments.length; i++) {
			if (matchingSegmentCount == i && i == patternSegments.length - 1 && "**".equals(patternSegments[i])) {
				// Check for match on ** only if all segments before matched, and we're matching last segment of pattern
				return true;
			} else if (patternSegments[i].equals(uriSegments[i]) || "*".equals(patternSegments[i])) {
				matchingSegmentCount++;
			} else {
				// No match on the segment, so this pattern cannot match rest of the Request URI. Bail out without checking more.
				break;
			}
		}

		return matchingSegmentCount == uriSegments.length;
	}

	private static boolean isPotentialMatch(boolean matchFullPattern, String[] patternSegments, String[] uriSegments) {
		if (matchFullPattern && (patternSegments.length == uriSegments.length
				|| ("**".equals(patternSegments[patternSegments.length - 1]) && patternSegments.length <= uriSegments.length))) {
			// This pattern will never be a match
			return true;
		}
		// When not wanting full pattern matches we never want matches on "/**" match-all wildcard
		return !matchFullPattern && !"**".equals(patternSegments[patternSegments.length - 1]);
	}

	public void registerServiceClient(ApiListener listener) {
		String uriPattern = listener.getCleanPattern();

		// This is already checked in ApiListener#configure()
		Objects.requireNonNull(uriPattern);

		synchronized(patternClients) {
			for(ApiListener.HttpMethod method : listener.getAllMethods()){
				patternClients.computeIfAbsent(uriPattern, ApiDispatchConfig::new).register(method, listener);

				if (log.isTraceEnabled()) {
					log.trace("ApiServiceDispatcher successfully registered uriPattern [{}] method [{}]", uriPattern, method);
				}
			}
		}
	}

	public void unregisterServiceClient(ApiListener listener) {
		String uriPattern = listener.getCleanPattern();
		if (uriPattern == null) {
			log.warn("uriPattern cannot be null or empty, unable to unregister ServiceClient");
		} else {
			listener.getAllMethods()
					.forEach(method -> clearMethod(method, uriPattern));
		}
	}

	private void clearMethod(ApiListener.HttpMethod httpMethod, String uriPattern) {
		boolean success = false;

		synchronized (patternClients) {
			ApiDispatchConfig dispatchConfig = patternClients.get(uriPattern);
			if (dispatchConfig != null) {
				if (dispatchConfig.getMethods().size() == 1) {
					patternClients.remove(uriPattern); // Remove the entire config if there's only 1 ServiceClient registered
				} else {
					dispatchConfig.remove(httpMethod); // Only remove the ServiceClient as there are multiple registered
				}
				success = true;
			}
		}

		// Keep log statements out of synchronized block
		if (success) {
			if (log.isTraceEnabled()) log.trace("ApiServiceDispatcher successfully unregistered uriPattern [{}] method [{}]", uriPattern, httpMethod);
		} else {
			log.warn("unable to find DispatchConfig for uriPattern [{}]", uriPattern);
		}
	}

	public SortedMap<String, ApiDispatchConfig> getPatternClients() {
		return Collections.unmodifiableSortedMap(patternClients);
	}

	public JsonObject generateOpenApiJsonSchema(String endpoint) {
		return OpenApiGenerator.generateOpenApiJsonSchema(getPatternClients().values(), endpoint);
	}

	public JsonObject generateOpenApiJsonSchema(ApiDispatchConfig client, String endpoint) {
		List<ApiDispatchConfig> clientList = List.of(client);
		return OpenApiGenerator.generateOpenApiJsonSchema(clientList, endpoint);
	}

	public static Optional<Json2XmlValidator> getJsonInputValidator(PipeLine pipeLine) {
		IPipe inputValidator = pipeLine.getInputValidator();

		if (inputValidator == null) {
			inputValidator = pipeLine.getPipe(pipeLine.getFirstPipe());
		}

		if (inputValidator instanceof Json2XmlValidator json2XmlValidator) {
			return Optional.of(json2XmlValidator);
		}

		return Optional.empty();
	}

	public static Optional<Json2XmlValidator> getJsonOutputValidator(PipeLine pipeline, String exit) {
		IPipe validator = pipeline.getOutputValidator();

		if (validator == null) {
			validator = determineValidator(pipeline, exit);
		}

		if (validator instanceof Json2XmlValidator xmlValidator) {
			return Optional.of(xmlValidator);
		}

		return Optional.empty();
	}

	private static Json2XmlValidator determineValidator(PipeLine pipeline, String exit) {
		IPipe firstPipe = pipeline.getPipe(pipeline.getFirstPipe());

		// Find the optional last pipe of type Json2XmlValidator
		Optional<Json2XmlValidator> optionalLastPipe = pipeline.getPipes().stream()
				.filter(Json2XmlValidator.class::isInstance)
				.map(Json2XmlValidator.class::cast)
				.filter(pipe -> pipe.hasRegisteredForward(exit))
				.reduce((first, second) -> second);

		// If there's no last pipe and the first pipe is an XmlValidator with a response root, use the first pipe
		if (optionalLastPipe.isEmpty() && firstPipe instanceof Json2XmlValidator isXmlValidator
				&& isXmlValidator.getResponseRoot() != null) {
			return isXmlValidator;
		}

		// If the validator is still null, and the optional last pipe is present, use that - or else return null
		return optionalLastPipe.orElse(null);
	}

	public void clear() {
		for (String uriPattern : patternClients.keySet()) {
			ApiDispatchConfig config = patternClients.remove(uriPattern);
			if (config != null) config.clear();
		}
		if (!patternClients.isEmpty()) {
			log.warn("unable to gracefully unregister [{}] DispatchConfigs", patternClients.size());
			patternClients.clear();
		}
	}
}

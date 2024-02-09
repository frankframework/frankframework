/*
Copyright 2017-2021, 2024 WeAreFrank!

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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.xs.XSModel;
import org.frankframework.align.XmlTypeToJsonSchemaConverter;
import org.frankframework.core.IAdapter;
import org.frankframework.core.IPipe;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineExit;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.Parameter.ParameterType;
import org.frankframework.pipes.Json2XmlValidator;
import org.frankframework.util.AppConstants;
import org.frankframework.util.DateFormatUtils;
import org.springframework.util.MimeType;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import lombok.extern.log4j.Log4j2;

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
	 *     So for instance if a configuration would have the following {@link ApiListener}s installed:
	 *     <lu>
	 *         <li>ApiListener1: GET on uri /user/**</li>
	 *         <li>ApiListener2: GET on uri /user/{userId}/department/{departmentId}</li>
	 *         <li>ApiListener3: POST on uri /user/{userId}/avatar</li>
	 *     </lu>
	 *     Then:
	 *     <lu>
	 *         <li>A request {@code GET /user/usr123/department/dept456} would return the {@link ApiDispatchConfig} for /user/{userId}/department/{departmentId} containing ApiListener2</li>
	 *         <li>A request {@code GET /user/usr123/avatar} would return the {@link ApiDispatchConfig} for /user/** containing ApiListener1</li>
	 *         <li>A request {@code POST /user/usr123/avatar} would return the {@link ApiDispatchConfig} for /user/{userId}/avatar containing ApiListener3</li>
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
				.filter(cfg -> cfg.hasMethod(method))
				.max(Comparator.comparingInt(cfg -> scoreUriPattern(cfg.getUriPattern())))
				.orElse(null);
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
		// Scoring rules:
		// - The more slashes the longer the match the more specific
		// - The more wildcards in the pattern the less specific
		// - "Match-all" patterns ending with /** are penalized with a -10 starting score
		int startValue = uriPattern.endsWith("/**") ? -10 : 0;
		return uriPattern.chars()
				.reduce(startValue, (cnt, chr) -> {
					switch ((char)chr) {
						case '/': return cnt + 1;
						case '*': return cnt - 1;
						default: return cnt;
					}
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
			if (matchingSegmentCount == i && i == patternSegments.length - 1 && patternSegments[i].equals("**")) {
				// Check for match on ** only if all segments before matched and we're matching last segment of pattern
				return true;
			} else if (patternSegments[i].equals(uriSegments[i]) || patternSegments[i].equals("*")) {
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
				|| (patternSegments[patternSegments.length - 1].equals("**") && patternSegments.length <= uriSegments.length))) {
			// This pattern will never be a match
			return true;
		}
		// When not wanting full pattern matches we never want matches on "/**" match-all wildcard
		return !matchFullPattern && !patternSegments[patternSegments.length - 1].equals("**");
	}

	public void registerServiceClient(ApiListener listener) throws ListenerException {
		String uriPattern = listener.getCleanPattern();
		if (StringUtils.isBlank(uriPattern))
			throw new ListenerException("uriPattern cannot be null or empty");

		synchronized(patternClients) {
			for(ApiListener.HttpMethod method : listener.getAllMethods()){
				patternClients.computeIfAbsent(uriPattern, ApiDispatchConfig::new).register(method, listener);
				if(log.isTraceEnabled()) log.trace("ApiServiceDispatcher successfully registered uriPattern [{}] method [{}}]", uriPattern, method);
			}
		}
	}

	public void unregisterServiceClient(ApiListener listener) {
		String uriPattern = listener.getCleanPattern();
		if(uriPattern == null) {
			log.warn("uriPattern cannot be null or empty, unable to unregister ServiceClient");
		}
		else {
			for(ApiListener.HttpMethod method : listener.getAllMethods()){
				boolean success = false;
				synchronized (patternClients) {
					ApiDispatchConfig dispatchConfig = patternClients.get(uriPattern);
					if(dispatchConfig != null) {
						if(dispatchConfig.getMethods().size() == 1) {
							patternClients.remove(uriPattern); //Remove the entire config if there's only 1 ServiceClient registered
						} else {
							dispatchConfig.remove(method); //Only remove the ServiceClient as there are multiple registered
						}
						success = true;
					}
				}

				//keep log statements out of synchronized block
				if(success) {
					if(log.isTraceEnabled()) log.trace("ApiServiceDispatcher successfully unregistered uriPattern [{}] method [{}}]", uriPattern, method);
				} else {
					log.warn("unable to find DispatchConfig for uriPattern [{}]", uriPattern);
				}
			}
		}
	}

	public SortedMap<String, ApiDispatchConfig> getPatternClients() {
		return Collections.unmodifiableSortedMap(patternClients);
	}

	public JsonObject generateOpenApiJsonSchema(String endpoint) {
		return generateOpenApiJsonSchema(getPatternClients().values(), endpoint);
	}

	public JsonObject generateOpenApiJsonSchema(ApiDispatchConfig client, String endpoint) {
		List<ApiDispatchConfig> clientList = List.of(client);
		return generateOpenApiJsonSchema(clientList, endpoint);
	}

	protected JsonObject generateOpenApiJsonSchema(Collection<ApiDispatchConfig> clients, String endpoint) {
		JsonObjectBuilder root = Json.createObjectBuilder();
		root.add("openapi", "3.0.0");
		String instanceName = AppConstants.getInstance().getProperty("instance.name");
		String environment = AppConstants.getInstance().getString("dtap.stage", "LOC");
		JsonObjectBuilder info = Json.createObjectBuilder();
		info.add("title", instanceName);
		info.add("description", "OpenApi auto-generated at "+ DateFormatUtils.getTimeStamp()+" for "+instanceName+" ("+environment+")");
		info.add("version", "unknown");
		root.add("info", info);
		root.add("servers", mapServers(endpoint));

		JsonObjectBuilder paths = Json.createObjectBuilder();
		JsonObjectBuilder schemas = Json.createObjectBuilder();

		for (ApiDispatchConfig config : clients) {
			JsonObjectBuilder methods = Json.createObjectBuilder();
			ApiListener listener = null;
			for (ApiListener.HttpMethod method : config.getMethods()) {
				JsonObjectBuilder methodBuilder = Json.createObjectBuilder();
				listener = config.getApiListener(method);
				if(listener != null && listener.getReceiver() != null) {
					IAdapter adapter = listener.getReceiver().getAdapter();
					if (StringUtils.isNotEmpty(adapter.getDescription())) {
						methodBuilder.add("summary", adapter.getDescription());
					}
					if(StringUtils.isNotEmpty(listener.getOperationId())) {
						methodBuilder.add("operationId", listener.getOperationId());
					}
					// GET and DELETE methods cannot have a requestBody according to the specs.
					if(method != ApiListener.HttpMethod.GET && method != ApiListener.HttpMethod.DELETE) {
						mapRequest(adapter, listener.getConsumes(), methodBuilder);
					}
					mapParamsInRequest(adapter, listener, methodBuilder);

					methodBuilder.add("responses", mapResponses(adapter, listener.getContentType(), schemas));
				}
				methods.add(method.name().toLowerCase(), methodBuilder);
			}
			if(listener != null) {
				paths.add(listener.getUriPattern(), methods);
			}
		}
		root.add("paths", paths.build());
		JsonObjectBuilder components = Json.createObjectBuilder();
		components.add("schemas", schemas);
		root.add("components", components);

		return root.build();
	}

	private JsonArrayBuilder mapServers(String url) {
		JsonArrayBuilder serversArray = Json.createArrayBuilder();
		String servletPath = AppConstants.getInstance().getString("servlet.ApiListenerServlet.urlMapping", "/api");

		// Get load balancer url if exists
		String loadBalancerUrl = AppConstants.getInstance().getProperty("loadBalancer.url", null);
		if(StringUtils.isNotEmpty(loadBalancerUrl)) {
			serversArray.add(Json.createObjectBuilder().add("url", loadBalancerUrl + servletPath).add("description", "load balancer"));
		} else if(StringUtils.isNotBlank(url)) { // fall back to the request url
			serversArray.add(Json.createObjectBuilder().add("url", url));
		}

		return serversArray;
	}

	public static Json2XmlValidator getJsonValidator(PipeLine pipeline, boolean forOutputValidation) {
		IPipe validator = forOutputValidation ? pipeline.getOutputValidator() : pipeline.getInputValidator();
		if(validator == null) {
			validator = pipeline.getPipe(pipeline.getFirstPipe());
		}
		if(validator instanceof Json2XmlValidator) {
			return (Json2XmlValidator) validator;
		}
		return null;
	}

	private void mapParamsInRequest(IAdapter adapter, ApiListener listener, JsonObjectBuilder methodBuilder) {
		String uriPattern = listener.getUriPattern();
		JsonArrayBuilder paramBuilder = Json.createArrayBuilder();
		mapPathParameters(paramBuilder, uriPattern);
		List<String> paramsFromHeaderAndCookie = mapHeaderAndParams(paramBuilder, listener);

		// query params
		Json2XmlValidator inputValidator = getJsonValidator(adapter.getPipeLine(), false);
		if(inputValidator != null && !inputValidator.getParameterList().isEmpty()) {
			for (Parameter parameter : inputValidator.getParameterList()) {
				String parameterSessionkey = parameter.getSessionKey();
				if(StringUtils.isNotEmpty(parameterSessionkey) && !parameterSessionkey.equals("headers") && !paramsFromHeaderAndCookie.contains(parameterSessionkey)) {
					ParameterType parameterType = parameter.getType() != null ? parameter.getType() : ParameterType.STRING;
					paramBuilder.add(addParameterToSchema(parameterSessionkey, "query", false, Json.createObjectBuilder().add("type", parameterType.toString().toLowerCase())));
				}
			}
		}
		JsonArray paramBuilderArray = paramBuilder.build();
		if(!paramBuilderArray.isEmpty()) {
			methodBuilder.add("parameters", paramBuilderArray);
		}
	}

	private List<String> mapHeaderAndParams(JsonArrayBuilder paramBuilder, ApiListener listener) {
		List<String> paramsFromHeaderAndCookie = new ArrayList<>();
		// header parameters
		if(StringUtils.isNotEmpty(listener.getHeaderParams())) {
			String[] params = listener.getHeaderParams().split(",");
			for (String parameter : params) {
				paramBuilder.add(addParameterToSchema(parameter, "header", false, Json.createObjectBuilder().add("type", "string")));
				paramsFromHeaderAndCookie.add(parameter);
			}
		}
		if(StringUtils.isNotEmpty(listener.getMessageIdHeader())) {
			paramBuilder.add(addParameterToSchema(listener.getMessageIdHeader(), "header", false, Json.createObjectBuilder().add("type", "string")));
		}

		return paramsFromHeaderAndCookie;
	}

	private void mapPathParameters(JsonArrayBuilder paramBuilder, String uriPattern) {
		// path parameters
		if(uriPattern.contains("{")) {
			Pattern p = Pattern.compile("[^{/}]+(?=})");
			Matcher m = p.matcher(uriPattern);
			while(m.find()) {
				paramBuilder.add(addParameterToSchema(m.group(), "path", true, Json.createObjectBuilder().add("type", "string")));
			}
		}
	}

	private JsonObjectBuilder addParameterToSchema(String name, String in, boolean required, JsonObjectBuilder schema) {
		JsonObjectBuilder param = Json.createObjectBuilder();
		param.add("name", name);
		param.add("in", in);
		if(required) {
			param.add("required", required);
		}
		param.add("schema", schema);
		return param;
	}

	private void mapRequest(IAdapter adapter, MediaTypes consumes, JsonObjectBuilder methodBuilder) {
		PipeLine pipeline = adapter.getPipeLine();
		Json2XmlValidator inputValidator = getJsonValidator(pipeline,false);
		if(inputValidator != null && StringUtils.isNotEmpty(inputValidator.getRoot())) {
			JsonObjectBuilder requestBodyContent = Json.createObjectBuilder();
			JsonObjectBuilder schemaBuilder = Json.createObjectBuilder().add("schema", Json.createObjectBuilder().add("$ref", XmlTypeToJsonSchemaConverter.SCHEMA_DEFINITION_PATH+inputValidator.getRoot()));
			requestBodyContent.add("content", Json.createObjectBuilder().add(mimeTypeToString(consumes.getMimeType()), schemaBuilder));
			methodBuilder.add("requestBody", requestBodyContent);
		}
	}

	//ContentType may have more parameters such as charset and formdata-boundry, strip those
	private String mimeTypeToString(MimeType mimeType) {
		return mimeType.getType() + "/" + mimeType.getSubtype();
	}

	private JsonObjectBuilder mapResponses(IAdapter adapter, MimeType contentType, JsonObjectBuilder schemas) {
		JsonObjectBuilder responses = Json.createObjectBuilder();

		PipeLine pipeline = adapter.getPipeLine();
		Json2XmlValidator inputValidator = getJsonValidator(pipeline, false);
		Json2XmlValidator outputValidator = getJsonValidator(pipeline, true);

		JsonObjectBuilder schema = null;
		String schemaReferenceElement = null;
		List<XSModel> models = new ArrayList<>();
		if(inputValidator != null) {
			models.addAll(inputValidator.getXSModels());
			schemaReferenceElement = inputValidator.getMessageRoot(true);
		}
		if(outputValidator != null) {
			models.addAll(outputValidator.getXSModels());
			schemaReferenceElement = outputValidator.getRoot();	// all non-empty exits should refer to this element
		}

		if(!models.isEmpty()) {
			schema = Json.createObjectBuilder();
		}
		addComponentsToTheSchema(schemas, models);

		Map<String, PipeLineExit> pipeLineExits = pipeline.getPipeLineExits();
		for(String exitPath : pipeLineExits.keySet()) {
			PipeLineExit ple = pipeLineExits.get(exitPath);
			int exitCode = ple.getExitCode();
			if(exitCode == 0) {
				exitCode = 200;
			}

			JsonObjectBuilder exit = Json.createObjectBuilder();

			Status status = Status.fromStatusCode(exitCode);
			exit.add("description", status.getReasonPhrase());
			if(!ple.isEmptyResult()) {
				JsonObjectBuilder content = Json.createObjectBuilder();
				if(StringUtils.isNotEmpty(schemaReferenceElement)){
					String reference = null;
					if(StringUtils.isNotEmpty(ple.getResponseRoot()) && outputValidator == null) {
						reference = ple.getResponseRoot();
					} else {
						List<String> references = List.of(schemaReferenceElement.split(","));
						if(ple.isSuccessExit()) {
							reference = references.get(0);
						} else {
							reference = references.get(references.size()-1);
						}
					}
					// JsonObjectBuilder add method consumes the schema
					schema.add("schema", Json.createObjectBuilder().add("$ref", XmlTypeToJsonSchemaConverter.SCHEMA_DEFINITION_PATH+reference));
					content.add(mimeTypeToString(contentType), schema);
				}
				exit.add("content", content);
			}

			responses.add(""+exitCode, exit);
		}
		return responses;
	}

	private void addComponentsToTheSchema(JsonObjectBuilder schemas, List<XSModel> models) {
		XmlTypeToJsonSchemaConverter converter = new XmlTypeToJsonSchemaConverter(models, true, XmlTypeToJsonSchemaConverter.SCHEMA_DEFINITION_PATH);
		JsonObject jsonSchema = converter.getDefinitions();
		if(jsonSchema != null) {
			for (Entry<String,JsonValue> entry: jsonSchema.entrySet()) {
				schemas.add(entry.getKey(), entry.getValue());
			}
		}
	}

	public void clear() {
		for (Iterator<String> it = patternClients.keySet().iterator(); it.hasNext();) {
			String uriPattern = it.next();
			ApiDispatchConfig config = patternClients.remove(uriPattern);
			if(config != null) config.clear();
		}
		if(!patternClients.isEmpty()) {
			log.warn("unable to gracefully unregister [{}] DispatchConfigs", patternClients.size());
			patternClients.clear();
		}
	}
}

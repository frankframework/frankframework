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

import jakarta.json.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.xerces.xs.XSModel;
import org.frankframework.align.XmlTypeToJsonSchemaConverter;
import org.frankframework.core.*;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.Parameter.ParameterType;
import org.frankframework.pipes.Json2XmlValidator;
import org.frankframework.util.AppConstants;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.LogUtil;
import org.springframework.util.MimeType;

import javax.ws.rs.core.Response.Status;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class registers dispatches requests to the proper registered ApiListeners.
 * The dispatcher does not handle nor does it process messages!
 *
 * @author Niels Meijer
 *
 */
public class ApiServiceDispatcher {

	private final Logger log = LogUtil.getLogger(this);
	private final ConcurrentSkipListMap<String, ApiDispatchConfig> patternClients = new ConcurrentSkipListMap<>(new ApiUriComparator());
	private static ApiServiceDispatcher self = null;

	public static synchronized ApiServiceDispatcher getInstance() {
		if( self == null ) {
			self = new ApiServiceDispatcher();
		}
		return self;
	}

	public ApiDispatchConfig findConfigForUri(String uri) {
		List<ApiDispatchConfig> configs = findMatchingConfigsForUri(uri, true);
		return configs.isEmpty()? null : configs.get(0);
	}

	public List<ApiDispatchConfig> findMatchingConfigsForUri(String uri) {
		return findMatchingConfigsForUri(uri, false);
	}

	private List<ApiDispatchConfig>  findMatchingConfigsForUri(String uri, boolean exactMatch) {
		List<ApiDispatchConfig> results = new ArrayList<>();

		String[] uriSegments = uri.split("/");

		for (Iterator<String> it = patternClients.keySet().iterator(); it.hasNext();) {
			String uriPattern = it.next();

			if (log.isTraceEnabled()) log.trace("comparing uri [{}] to pattern [{}]", uri, uriPattern);

			String[] patternSegments = uriPattern.split("/");

			if (exactMatch && patternSegments.length != uriSegments.length || patternSegments.length < uriSegments.length) {
				continue;
			}

			int matches = 0;
			for (int i = 0; i < uriSegments.length; i++) {
				if (patternSegments[i].equals(uriSegments[i]) || patternSegments[i].equals("*")) {
					matches++;
				}
			}
			if (matches == uriSegments.length) {
				ApiDispatchConfig result = patternClients.get(uriPattern);
				results.add(result);
				if (exactMatch) {
					return results;
				}
			}

			if (uri.contains("**")) {
				ApiDispatchConfig result = new ApiDispatchConfig(matchUri(uriPattern, uri));
				results.add(result);
				return results;
			}
		}
		return results;
	}

	public static String matchUri(String uri, String wildcard) {
		//Check if wildcard is present and if the uri contains the part of the uri in the wildcard
		if (wildcard.contains("**") && uri.contains(wildcard.replace("*", ""))) {
			return uri;
		}
		return "";
	}

	public void registerServiceClient(ApiListener listener) throws ListenerException {
		String uriPattern = listener.getCleanPattern();
		if(uriPattern == null)
			throw new ListenerException("uriPattern cannot be null or empty");

		synchronized(patternClients) {
			for(ApiListener.HttpMethod method : listener.getAllMethods()){
				patternClients.computeIfAbsent(uriPattern, pattern -> new ApiDispatchConfig(pattern)).register(method, listener);
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

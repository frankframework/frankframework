/*
Copyright 2017-2021 WeAreFrank!

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
package nl.nn.adapterframework.http.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.pipes.Json2XmlValidator;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

/**
 * This class registers dispatches requests to the proper registered ApiListeners.
 * The dispatcher does not handle nor does it process messages!
 * 
 * @author Niels Meijer
 *
 */
public class ApiServiceDispatcher {

	private Logger log = LogUtil.getLogger(this);
	private ConcurrentSkipListMap<String, ApiDispatchConfig> patternClients = new ConcurrentSkipListMap<String, ApiDispatchConfig>(new ApiUriComparator());
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

		String uriSegments[] = uri.split("/");

		for (Iterator<String> it = patternClients.keySet().iterator(); it.hasNext();) {
			String uriPattern = it.next();
			log.trace("comparing uri ["+uri+"] to pattern ["+uriPattern+"]");

			String patternSegments[] = uriPattern.split("/");
			if (exactMatch && patternSegments.length != uriSegments.length || patternSegments.length < uriSegments.length) {
				continue;
			}

			int matches = 0;
			for (int i = 0; i < uriSegments.length; i++) {
				if(patternSegments[i].equals(uriSegments[i]) || patternSegments[i].equals("*")) {
					matches++;
				} else {
					continue;
				}
			}
			if(matches == uriSegments.length) {
				ApiDispatchConfig result = patternClients.get(uriPattern); 
				results.add(result);
				if (exactMatch) {
					return results;
				}
			}
		}
		return results;
	}

	public synchronized void registerServiceClient(ApiListener listener) throws ListenerException {
		String uriPattern = listener.getCleanPattern();
		if(uriPattern == null)
			throw new ListenerException("uriPattern cannot be null or empty");

		String method = listener.getMethod();

		ApiDispatchConfig dispatchConfig = null;
		if(patternClients.containsKey(uriPattern))
			dispatchConfig = patternClients.get(uriPattern);
		else
			dispatchConfig = new ApiDispatchConfig(uriPattern);

		dispatchConfig.register(method, listener);

		patternClients.put(uriPattern, dispatchConfig);
		log.trace("ApiServiceDispatcher successfully registered uriPattern ["+uriPattern+"] method ["+method+"]");
	}

	public synchronized void unregisterServiceClient(ApiListener listener) {
		String method = listener.getMethod();
		String uriPattern = listener.getCleanPattern();
		if(uriPattern == null) {
			log.warn("uriPattern cannot be null or empty, unable to unregister ServiceClient");
		}
		else {
			ApiDispatchConfig dispatchConfig = patternClients.get(uriPattern);
			if(dispatchConfig == null) {
				log.warn("unable to find DispatchConfig for uriPattern ["+uriPattern+"]");
			} else {
				dispatchConfig.destroy(method);

				log.trace("ApiServiceDispatcher successfully unregistered uriPattern ["+uriPattern+"] method ["+method+"]");
			}
		}
	}

	public SortedMap<String, ApiDispatchConfig> getPatternClients() {
		return patternClients;
	}

	protected JsonObject generateOpenApiJsonSchema(HttpServletRequest request) {
		return generateOpenApiJsonSchema(getPatternClients().values(), request);
	}

	protected JsonObject generateOpenApiJsonSchema(ApiDispatchConfig client, HttpServletRequest request) {
		List<ApiDispatchConfig> clientList = Arrays.asList(client);
		return generateOpenApiJsonSchema(clientList, request);
	}

	protected JsonObject generateOpenApiJsonSchema(Collection<ApiDispatchConfig> clients, HttpServletRequest request) {

		JsonObjectBuilder root = Json.createObjectBuilder();
		root.add("openapi", "3.0.0");
		String instanceName = AppConstants.getInstance().getProperty("instance.name");
		JsonObjectBuilder info = Json.createObjectBuilder();
		info.add("title", "Sample API");
		info.add("description", "OpenApi document auto-generated by Frank!Framework for "+instanceName+"");
		info.add("version", "0.1.9");
		root.add("info", info);
		JsonArrayBuilder serverObjectArray = mapServers(request);
		root.add("servers", serverObjectArray);

		JsonObjectBuilder paths = Json.createObjectBuilder();
		JsonObjectBuilder schemas = Json.createObjectBuilder();

		for (ApiDispatchConfig config : clients) {
			JsonObjectBuilder methods = Json.createObjectBuilder();
			ApiListener listener = null;
			for (String method : config.getMethods()) {
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
					if(!method.equals("GET") && !method.equals("DELETE")) {
						mapRequest(adapter, listener.getConsumesEnum(), methodBuilder);
					}
					mapParamsInRequest(request, adapter, listener, methodBuilder);

					//ContentType may have more parameters such as charset and formdata-boundry
					MediaTypes produces = listener.getProducesEnum();
					methodBuilder.add("responses", mapResponses(adapter, produces, schemas));
				}
				methods.add(method.toLowerCase(), methodBuilder);
			}
			if(listener != null) {
				paths.add(listener.getUriPattern(), methods);
			}
		}
		root.add("paths", paths.build());
		root.add("components", Json.createObjectBuilder().add("schemas", schemas));

		return root.build();
	}

	private JsonArrayBuilder mapServers(HttpServletRequest request) {
		JsonArrayBuilder serversArray = Json.createArrayBuilder();
		String protocol = request.isSecure() ? "https://" : "http://";
		String suffix = "/api";
		String env = AppConstants.getInstance().getString("dtap.stage", "LOC");
		int port = request.getServerPort();
		// Get load balancer url if exists
		String loadBalancerUrl = AppConstants.getInstance().getProperty("load.balancer.url", null);
		if(StringUtils.isNotEmpty(loadBalancerUrl)) {
			serversArray.add(Json.createObjectBuilder().add("url", loadBalancerUrl + suffix).add("description", "load balancer for " + env + " server"));
		}
		// Get details from properties
		String hostname = AppConstants.getInstance().getString("hostname", null);
		if(StringUtils.isNotEmpty(hostname)) {
			String webContextPath = AppConstants.getInstance().getString("web.contextpath", null);
			hostname += (port != 0 ? ":" + port : "") + (webContextPath != null ? webContextPath : "");
			String restBaseUrl = hostname.toLowerCase() + suffix;
			serversArray.add(Json.createObjectBuilder().add("url", protocol + restBaseUrl).add("description", env + " server hostname"));
		}
		// Get details from the request
		String requestServerName = request.getServerName();
		if(StringUtils.isNotEmpty(requestServerName) && !requestServerName.equals(hostname) && !requestServerName.equals(loadBalancerUrl)) {
			String restBaseUrl = requestServerName + (port != 0 ? ":" + port : "") + request.getContextPath() + suffix;
			serversArray.add(Json.createObjectBuilder().add("url", protocol + restBaseUrl).add("description", env + " server"));
		}
		return serversArray;
	}

	public static Json2XmlValidator getJsonValidator(PipeLine pipeline) {
		IPipe validator = pipeline.getInputValidator();
		if(validator == null) {
			validator = pipeline.getPipe(pipeline.getFirstPipe());
		}
		if(validator instanceof Json2XmlValidator) {
			return (Json2XmlValidator) validator;
		}
		return null;
	}

	private void mapParamsInRequest(HttpServletRequest request, IAdapter adapter, ApiListener listener, JsonObjectBuilder methodBuilder) {
		String uriPattern = listener.getUriPattern();
		JsonArrayBuilder paramBuilder = Json.createArrayBuilder();
		// path parameters
		if(uriPattern.contains("{")) {
			Pattern p = Pattern.compile("[^{/}]+(?=})");
			Matcher m = p.matcher(uriPattern);
			while(m.find()) {
				addParameterToSchema(paramBuilder, m.group(), "path", true, Json.createObjectBuilder().add("type", "string"));
			}
		}
		List<String> paramsFromHeaderAndCookie = new ArrayList<String>();
		// header parameters
		if(StringUtils.isNotEmpty(listener.getHeaderParams())) {
			String params[] = listener.getHeaderParams().split(",");
			for (String parameter : params) {
				addParameterToSchema(paramBuilder, parameter, "header", false, Json.createObjectBuilder().add("type", "string"));
				paramsFromHeaderAndCookie.add(parameter);
			}
		}
		if(StringUtils.isNotEmpty(listener.getMessageIdHeader())) {
			String messageIdHeader = request.getHeader(listener.getMessageIdHeader());
			if(StringUtils.isNotEmpty(messageIdHeader)) {
				addParameterToSchema(paramBuilder, listener.getMessageIdHeader(), "header", false, Json.createObjectBuilder().add("type", "string"));
			}
		}
		// cookie params
		if(StringUtils.isNotEmpty(listener.getCookieParams())) {
			String params[] = listener.getCookieParams().split(",");
			for (String parameter : params) {
				addParameterToSchema(paramBuilder, parameter, "cookie", false, Json.createObjectBuilder().add("type", "string"));
				paramsFromHeaderAndCookie.add(parameter);
			}
		}
		// query params
		Json2XmlValidator validator = getJsonValidator(adapter.getPipeLine());
		if(validator != null && !validator.getParameterList().isEmpty()) {
			for (Parameter parameter : validator.getParameterList()) {
				String parameterSessionkey = parameter.getSessionKey();
				if(StringUtils.isNotEmpty(parameterSessionkey) && !paramsFromHeaderAndCookie.contains(parameterSessionkey)) {
					String parameterType = parameter.getType() != null ? parameter.getType() : "string";
					addParameterToSchema(paramBuilder, parameterSessionkey, "query", false, Json.createObjectBuilder().add("type", parameterType));
				}
			}
		}
		JsonArray paramBuilderArray = paramBuilder.build();
		if(!paramBuilderArray.isEmpty()) {
			methodBuilder.add("parameters", paramBuilderArray);
		}
	}
	
	private void addParameterToSchema(JsonArrayBuilder paramBuilder, String name, String in, boolean required, JsonObjectBuilder schema) {
		JsonObjectBuilder param = Json.createObjectBuilder();
		param.add("name", name);
		param.add("in", in);
		if(required) {
			param.add("required", required);
		}
		param.add("schema", schema);
		paramBuilder.add(param);
	}

	private void mapRequest(IAdapter adapter, MediaTypes consumes, JsonObjectBuilder methodBuilder) {
		PipeLine pipeline = adapter.getPipeLine();
		Json2XmlValidator validator = getJsonValidator(pipeline);
		if(validator != null && StringUtils.isNotEmpty(validator.getRoot())) {
			JsonObjectBuilder requestBodyContent = Json.createObjectBuilder();
			JsonObjectBuilder schemaBuilder = Json.createObjectBuilder().add("schema", Json.createObjectBuilder().add("$ref", "#/components/schemas/"+validator.getRoot()));
			requestBodyContent.add("content", Json.createObjectBuilder().add(consumes.getContentType(), schemaBuilder));
			methodBuilder.add("requestBody", requestBodyContent);
		}
	}

	private JsonObjectBuilder mapResponses(IAdapter adapter, MediaTypes contentType, JsonObjectBuilder schemas) {
		JsonObjectBuilder responses = Json.createObjectBuilder();

		PipeLine pipeline = adapter.getPipeLine();
		Json2XmlValidator validator = getJsonValidator(pipeline);
		JsonObjectBuilder schema = null;
		String ref = null;
		if(validator != null) {
			JsonObject jsonSchema = validator.createJsonSchemaDefinitions("#/components/schemas/");
			if(jsonSchema != null) {
				for (Entry<String,JsonValue> entry: jsonSchema.entrySet()) {
					schemas.add(entry.getKey(), entry.getValue());
				}
				ref = validator.getMessageRoot(true);
				schema = Json.createObjectBuilder();
			}
		}

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
			if(!ple.getEmptyResult()) {
				JsonObjectBuilder content = Json.createObjectBuilder();
				if(StringUtils.isNotEmpty(ref)){
					String reference = null;
					if(StringUtils.isNotEmpty(ple.getResponseRoot())) {
						reference = ple.getResponseRoot();
					} else {
						List<String> references = Arrays.asList(ref.split(","));
						if(ple.getState().equals("success")) {
							reference = references.get(0);
						} else {
							reference = references.get(references.size()-1);
						}
					}
					// JsonObjectBuilder add method consumes the schema
					schema.add("schema", Json.createObjectBuilder().add("$ref", "#/components/schemas/"+reference));
					content.add(contentType.getContentType(), schema);
				}
				exit.add("content", content);
			}

			responses.add(""+exitCode, exit);
		}
		return responses;
	}

	public void clear() {
		for (Iterator<String> it = patternClients.keySet().iterator(); it.hasNext();) {
			String uriPattern = it.next();
			ApiDispatchConfig config = patternClients.remove(uriPattern);
			if(config != null) config.clear();
		}
		if(!patternClients.isEmpty()) {
			log.warn("unable to gracefully unregister "+patternClients.size()+" DispatchConfigs");
			patternClients.clear();
		}
	}
}

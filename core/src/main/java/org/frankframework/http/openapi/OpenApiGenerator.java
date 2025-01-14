/*
Copyright 2024 WeAreFrank!

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
package org.frankframework.http.openapi;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.xs.XSModel;
import org.springframework.util.MimeType;

import lombok.NoArgsConstructor;

import org.frankframework.align.XmlTypeToJsonSchemaConverter;
import org.frankframework.core.Adapter;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineExit;
import org.frankframework.http.rest.ApiDispatchConfig;
import org.frankframework.http.rest.ApiListener;
import org.frankframework.http.rest.ApiServiceDispatcher;
import org.frankframework.http.rest.MediaTypes;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.ParameterType;
import org.frankframework.pipes.Json2XmlValidator;
import org.frankframework.util.AppConstants;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.StringUtil;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class OpenApiGenerator {

	public static JsonObject generateOpenApiJsonSchema(Collection<ApiDispatchConfig> clients, String endpoint) {
		JsonObjectBuilder root = Json.createObjectBuilder();
		root.add("openapi", "3.0.0");
		String instanceName = AppConstants.getInstance().getProperty("instance.name");
		String environment = AppConstants.getInstance().getString("dtap.stage", "LOC");
		JsonObjectBuilder info = Json.createObjectBuilder();
		info.add("title", instanceName);
		info.add("description", "OpenApi auto-generated at " + DateFormatUtils.getTimeStamp() + " for " + instanceName + " (" + environment + ")");
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
				if (listener != null && listener.getReceiver() != null) {
					Adapter adapter = listener.getReceiver().getAdapter();
					if (StringUtils.isNotEmpty(adapter.getDescription())) {
						methodBuilder.add("summary", adapter.getDescription());
					}
					if (StringUtils.isNotEmpty(listener.getOperationId())) {
						methodBuilder.add("operationId", listener.getOperationId());
					}
					// GET and DELETE methods cannot have a requestBody according to the specs.
					if (method != ApiListener.HttpMethod.GET && method != ApiListener.HttpMethod.DELETE) {
						mapRequest(adapter, listener.getConsumes(), methodBuilder);
					}
					mapParamsInRequest(adapter, listener, methodBuilder);

					methodBuilder.add("responses", mapResponses(adapter, listener.getContentType(), schemas));
				}
				methods.add(method.name().toLowerCase(), methodBuilder);
			}
			if (listener != null) {
				paths.add(listener.getUriPattern(), methods);
			}
		}
		root.add("paths", paths.build());
		JsonObjectBuilder components = Json.createObjectBuilder();
		components.add("schemas", schemas);
		root.add("components", components);

		return root.build();
	}

	private static JsonArrayBuilder mapServers(String endpoint) {
		JsonArrayBuilder serversArray = Json.createArrayBuilder();
		String servletPath = AppConstants.getInstance().getString("servlet.ApiListenerServlet.urlMapping", "/api");

		// Get load balancer.url if exists
		String loadBalancerUrl = AppConstants.getInstance().getProperty("loadBalancer.url", null);
		if (StringUtils.isNotEmpty(loadBalancerUrl)) {
			serversArray.add(Json.createObjectBuilder().add("url", loadBalancerUrl + servletPath).add("description", "load balancer"));
		} else if (StringUtils.isNotBlank(endpoint)) { // fall back to the request endpoint
			serversArray.add(Json.createObjectBuilder().add("url", endpoint));
		}
		return serversArray;
	}

	private static void mapParamsInRequest(Adapter adapter, ApiListener listener, JsonObjectBuilder methodBuilder) {
		JsonArrayBuilder paramBuilder = Json.createArrayBuilder();
		mapPathParameters(paramBuilder, listener.getUriPattern());

		List<String> paramsFromHeaderAndCookie = getParamsFromHeader(listener);

		mapHeaderAndParams(paramBuilder, paramsFromHeaderAndCookie, listener.getMessageIdHeader());

		// query params
		ApiServiceDispatcher.getJsonInputValidator(adapter.getPipeLine()).ifPresent(inputValidator -> {
			assert inputValidator.getParameterList() != null;

			for (IParameter parameter : inputValidator.getParameterList()) {
				String parameterSessionKey = parameter.getSessionKey();
				if (StringUtils.isNotEmpty(parameterSessionKey) && !"headers".equals(parameterSessionKey) && !paramsFromHeaderAndCookie.contains(parameterSessionKey)) {
					ParameterType parameterType = parameter.getType() != null ? parameter.getType() : ParameterType.STRING;
					paramBuilder.add(addParameterToSchema(parameterSessionKey, "query", false, Json.createObjectBuilder()
							.add("type", parameterType.toString().toLowerCase())));
				}
			}
		});

		JsonArray paramBuilderArray = paramBuilder.build();
		if (!paramBuilderArray.isEmpty()) {
			methodBuilder.add("parameters", paramBuilderArray);
		}
	}

	private static List<String> getParamsFromHeader(ApiListener listener) {
		if (StringUtils.isNotEmpty(listener.getHeaderParams())) {
			return StringUtil.split(listener.getHeaderParams());
		}

		return Collections.emptyList();
	}

	private static void mapHeaderAndParams(JsonArrayBuilder paramBuilder, List<String> headerParams, String messageIdHeader) {
		headerParams.forEach(parameter -> paramBuilder.add(addParameterToSchema(parameter, "header", false, Json.createObjectBuilder().add("type", "string"))));

		if (StringUtils.isNotEmpty(messageIdHeader)) {
			paramBuilder.add(addParameterToSchema(messageIdHeader, "header", false, Json.createObjectBuilder().add("type", "string")));
		}
	}

	private static void mapPathParameters(JsonArrayBuilder paramBuilder, String uriPattern) {
		// path parameters
		if (uriPattern.contains("{")) {
			Pattern p = Pattern.compile("[^{/}]+(?=})");
			Matcher m = p.matcher(uriPattern);
			while (m.find()) {
				paramBuilder.add(addParameterToSchema(m.group(), "path", true, Json.createObjectBuilder().add("type", "string")));
			}
		}
	}

	private static JsonObjectBuilder addParameterToSchema(String name, String in, boolean required, JsonObjectBuilder schema) {
		JsonObjectBuilder param = Json.createObjectBuilder();
		param.add("name", name);
		param.add("in", in);
		if (required) {
			param.add("required", required);
		}
		param.add("schema", schema);
		return param;
	}

	private static void mapRequest(Adapter adapter, MediaTypes consumes, JsonObjectBuilder methodBuilder) {
		PipeLine pipeline = adapter.getPipeLine();
		ApiServiceDispatcher.getJsonInputValidator(pipeline).ifPresent(inputValidator -> {
			if (StringUtils.isNotEmpty(inputValidator.getRoot())) {
				JsonObjectBuilder requestBodyContent = Json.createObjectBuilder();
				JsonObjectBuilder schemaBuilder = Json.createObjectBuilder()
						.add("schema", Json.createObjectBuilder().add("$ref", XmlTypeToJsonSchemaConverter.SCHEMA_DEFINITION_PATH + inputValidator.getRoot()));
				requestBodyContent.add("content", Json.createObjectBuilder().add(mimeTypeToString(consumes.getMimeType()), schemaBuilder));
				methodBuilder.add("requestBody", requestBodyContent);
			}
		});
	}

	//ContentType may have more parameters such as charset and formdata-boundary, strip those
	private static String mimeTypeToString(MimeType mimeType) {
		return mimeType.getType() + "/" + mimeType.getSubtype();
	}

	private static JsonObjectBuilder mapResponses(Adapter adapter, MimeType contentType, JsonObjectBuilder schemas) {
		JsonObjectBuilder responses = Json.createObjectBuilder();

		PipeLine pipeline = adapter.getPipeLine();
		Optional<Json2XmlValidator> inputValidator = ApiServiceDispatcher.getJsonInputValidator(pipeline);
		inputValidator.ifPresent(validator -> addComponentsToTheSchema(schemas, validator.getXSModels()));

		JsonObjectBuilder schema = Json.createObjectBuilder();

		for (Map.Entry<String, PipeLineExit> entry : pipeline.getPipeLineExits().entrySet()) {
			PipeLineExit pipeLineExit = entry.getValue();

			int exitCode = pipeLineExit.getExitCode();
			if (exitCode == 0) {
				exitCode = 200;
			}

			JsonObjectBuilder exit = Json.createObjectBuilder();

			Response.Status status = Response.Status.fromStatusCode(exitCode);
			exit.add("description", status.getReasonPhrase());

			Optional<Json2XmlValidator> outputValidator = ApiServiceDispatcher.getJsonOutputValidator(pipeline, pipeLineExit.getName());
			outputValidator.ifPresent(validator -> addComponentsToTheSchema(schemas, validator.getXSModels()));

			if (!pipeLineExit.isEmptyResult()) {
				JsonObjectBuilder content = Json.createObjectBuilder();

				getSchemaReferenceElement(inputValidator.orElse(null), outputValidator.orElse(null))
						.ifPresent(schemaReferenceElement -> {
							String reference = getReference(pipeLineExit, outputValidator.isPresent(), schemaReferenceElement);

							// JsonObjectBuilder add method consumes the schema
							schema.add("schema", Json.createObjectBuilder().add("$ref", XmlTypeToJsonSchemaConverter.SCHEMA_DEFINITION_PATH + reference));
							content.add(mimeTypeToString(contentType), schema);
						});
				exit.add("content", content);
			}

			responses.add(String.valueOf(exitCode), exit);
		}
		return responses;
	}

	private static Optional<String> getSchemaReferenceElement(Json2XmlValidator inputValidator, Json2XmlValidator outputValidator) {
		if (outputValidator != null) {
			return Optional.ofNullable(outputValidator.getRoot()); // all non-empty exits should refer to this element
		}

		if (inputValidator != null) {
			return Optional.ofNullable(inputValidator.getMessageRoot(true));
		}

		return Optional.empty();
	}

	private static String getReference(PipeLineExit pipeLineExit, boolean outputValidatorPresent, String schemaReferenceElement) {
		if (StringUtils.isNotEmpty(pipeLineExit.getResponseRoot()) && !outputValidatorPresent) {
			return pipeLineExit.getResponseRoot();
		} else {
			List<String> references = StringUtil.split(schemaReferenceElement);

			if (pipeLineExit.isSuccessExit()) {
				return references.get(0);
			}

			return references.get(references.size() - 1);
		}
	}

	private static void addComponentsToTheSchema(JsonObjectBuilder schemas, List<XSModel> models) {
		XmlTypeToJsonSchemaConverter converter = new XmlTypeToJsonSchemaConverter(models, true, XmlTypeToJsonSchemaConverter.SCHEMA_DEFINITION_PATH);
		JsonObject jsonSchema = converter.getDefinitions();
		jsonSchema.forEach(schemas::add);
	}
}

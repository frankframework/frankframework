/*
   Copyright 2022-2024 WeAreFrank!

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
package org.frankframework.management.bus.endpoints;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import javax.xml.stream.XMLStreamException;

import jakarta.annotation.security.RolesAllowed;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.Adapter;
import org.frankframework.core.IListener;
import org.frankframework.http.RestListener;
import org.frankframework.http.WebServiceListener;
import org.frankframework.http.rest.ApiDispatchConfig;
import org.frankframework.http.rest.ApiListener;
import org.frankframework.http.rest.ApiListener.HttpMethod;
import org.frankframework.http.rest.ApiServiceDispatcher;
import org.frankframework.management.bus.ActionSelector;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusAware;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.TopicSelector;
import org.frankframework.management.bus.message.BinaryMessage;
import org.frankframework.management.bus.message.JsonMessage;
import org.frankframework.management.bus.message.StringMessage;
import org.frankframework.receivers.Receiver;
import org.frankframework.soap.WsdlGenerator;
import org.frankframework.soap.WsdlGeneratorUtils;
import org.frankframework.util.EnumUtils;

@Log4j2
@BusAware("frank-management-bus")
@TopicSelector(BusTopic.WEBSERVICES)
public class WebServices extends BusEndpointBase {
	private enum ServiceType {
		WSDL, OPENAPI
	}

	@ActionSelector(BusAction.GET)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public Message<String> getWebServices(Message<?> message) {
		Map<String, Object> returnMap = new HashMap<>();

		returnMap.put("services", getRestListeners());
		returnMap.put("wsdls", getWsdls());
		returnMap.put("apiListeners", getApiListeners());

		return new JsonMessage(returnMap);
	}

	@ActionSelector(BusAction.DOWNLOAD)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public Message<?> getService(Message<?> message) {
		ServiceType type = BusMessageUtils.getEnumHeader(message, "type", ServiceType.class);
		if(type == ServiceType.OPENAPI) {
			return getOpenApiSpec(message);
		} else {
			return getWSDL(message);
		}
	}

	public StringMessage getOpenApiSpec(Message<?> message) {
		String uri = BusMessageUtils.getHeader(message, "uri", null);
		JsonObject jsonSchema;
		ApiServiceDispatcher dispatcher = ApiServiceDispatcher.getInstance();
		if(uri != null) {
			ApiDispatchConfig apiConfig = dispatcher.findExactMatchingConfigForUri(uri);
			if(apiConfig == null) {
				throw new BusException("unable to find Dispatch configuration for uri");
			}
			jsonSchema = dispatcher.generateOpenApiJsonSchema(apiConfig, null); // endpoint should be resolved from loadbalancer.url property
		} else {
			jsonSchema = dispatcher.generateOpenApiJsonSchema(null);
		}

		Map<String, Boolean> config = new HashMap<>();
		config.put(JsonGenerator.PRETTY_PRINTING, true);
		JsonWriterFactory factory = Json.createWriterFactory(config);
		StringWriter writer = new StringWriter();
		try (JsonWriter jsonWriter = factory.createWriter(writer)) {
			jsonWriter.write(jsonSchema);
		}

		return new StringMessage(writer.toString(), MediaType.APPLICATION_JSON);
	}

	public BinaryMessage getWSDL(Message<?> message) {
		boolean indent = BusMessageUtils.getBooleanHeader(message, "indent", true);
		boolean useIncludes = BusMessageUtils.getBooleanHeader(message, "useIncludes", false);
		boolean zip = BusMessageUtils.getBooleanHeader(message, "zip", false);

		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY);
		String adapterName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_ADAPTER_NAME_KEY);
		Adapter adapter = getAdapterByName(configurationName, adapterName);

		String generationInfo = "by FrankConsole";
		WsdlGenerator wsdl;
		try {
			wsdl = new WsdlGenerator(adapter.getPipeLine(), generationInfo);
			wsdl.setIndent(indent);
			wsdl.setUseIncludes(useIncludes||zip);
			wsdl.init();
		} catch (Exception e) {
			throw new BusException("unable to create WSDL generator", e);
		}

		try {
			String servletName = getServiceEndpoint(adapter);
			ByteArrayOutputStream boas = new ByteArrayOutputStream();
			if (zip) {
				wsdl.zip(boas, servletName);

				BinaryMessage response = new BinaryMessage(boas.toByteArray(), MediaType.APPLICATION_OCTET_STREAM);
				response.setFilename(adapterName+".zip");
				return response;

			} else {
				wsdl.wsdl(boas, servletName);

				return new BinaryMessage(boas.toByteArray(), MediaType.APPLICATION_XML);
			}
		} catch (IOException | ConfigurationException | XMLStreamException e) {
			throw new BusException("unable to generate WSDL", e);
		}
	}

	private String getServiceEndpoint(Adapter adapter) {
		String endpoint = "external address of ibis";
		for(Receiver<?> receiver : adapter.getReceivers()) {
			IListener<?> listener = receiver.getListener();
			if(listener instanceof WebServiceListener serviceListener) {
				String address = serviceListener.getAddress();
				if(StringUtils.isNotEmpty(address)) {
					endpoint = address;
				} else {
					endpoint = "rpcrouter";
				}
				return "/services/" + endpoint;
			}
		}
		return endpoint;
	}

	private List<ListenerDAO> getApiListeners() {
		List<ListenerDAO> apiListeners = new ArrayList<>();
		SortedMap<String, ApiDispatchConfig> patternClients = ApiServiceDispatcher.getInstance().getPatternClients();
		for (Entry<String, ApiDispatchConfig> client : patternClients.entrySet()) {
			ApiDispatchConfig config = client.getValue();
			ApiListener listener = config.getApiListener(config.getMethods().iterator().next()); // The first httpMethod will resolve in the right listener
			Receiver<?> receiver = listener.getReceiver();
			Adapter adapter = receiver == null ? null : receiver.getAdapter();
			ListenerDAO dao = new ListenerDAO(listener);
			if (adapter != null) dao.setAdapter(adapter);
			if (receiver != null) dao.setReceiver(receiver);

			apiListeners.add(dao);
		}
		return apiListeners;
	}

	private List<Map<String, Object>> getWsdls() {
		List<Map<String, Object>> wsdls = new ArrayList<>();
		for (Configuration config : getIbisManager().getConfigurations()) {
			for (Adapter adapter : config.getRegisteredAdapters()) {
				Map<String, Object> wsdlMap = new HashMap<>(3);
				wsdlMap.put("configuration", config.getName());
				wsdlMap.put("adapter", adapter.getName());
				try {
					if(WsdlGeneratorUtils.canProvideWSDL(adapter)) { // check eligibility
						WsdlGenerator wsdl = new WsdlGenerator(adapter.getPipeLine());
						wsdlMap.put("name", wsdl.getName());
						wsdls.add(wsdlMap);
					}
				} catch (Exception e) {
					wsdlMap.put("error", e.getMessage() != null ? e.getMessage() : e.toString());
					wsdls.add(wsdlMap);
				}

			}
		}
		return wsdls;
	}

	public List<ListenerDAO> getRestListeners() {
		List<ListenerDAO> restListeners = new ArrayList<>();

		for (Configuration config : getIbisManager().getConfigurations()) {
			for (Adapter adapter : config.getRegisteredAdapters()) {
				for (Receiver<?> receiver: adapter.getReceivers()) {
					IListener<?> listener = receiver.getListener();
					if (listener instanceof RestListener restListener) {
						ListenerDAO dao = new ListenerDAO(restListener);
						dao.setAdapter(adapter);
						dao.setReceiver(receiver);
						restListeners.add(dao);
					}
				}
			}
		}
		return restListeners;
	}

	@JsonInclude(Include.NON_NULL)
	public class ListenerDAO {
		private final @Getter String name;
		private final @Getter List<HttpMethod> methods;
		private final @Getter String uriPattern;
		private @Getter String receiver;
		private @Getter String adapter;

		public ListenerDAO(RestListener listener) {
			this.name = listener.getName();

			HttpMethod tempMethod = null;
			try {
				tempMethod = EnumUtils.parse(HttpMethod.class, listener.getMethod());
			} catch (IllegalArgumentException e) {
				log.warn("Invalid method supplied [{}] for listener [{}]", listener.getMethod(), listener.getName());
			}
			methods = tempMethod != null ? List.of(tempMethod) : Collections.emptyList();

			this.uriPattern = listener.getUriPattern();
		}

		public ListenerDAO(ApiListener listener) {
			this.name = listener.getName();
			this.methods = listener.getAllMethods();
			this.uriPattern = listener.getUriPattern();
		}

		public void setReceiver(Receiver<?> receiver) {
			this.receiver = receiver.getName();
		}

		public void setAdapter(Adapter adapter) {
			this.adapter = adapter.getName();
		}
	}
}

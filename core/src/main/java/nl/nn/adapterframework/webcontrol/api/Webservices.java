/*
Copyright 2016-2017, 2019-2021 WeAreFrank!

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
package nl.nn.adapterframework.webcontrol.api;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.StringUtils;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.http.RestListener;
import nl.nn.adapterframework.http.WebServiceListener;
import nl.nn.adapterframework.http.rest.ApiDispatchConfig;
import nl.nn.adapterframework.http.rest.ApiListener;
import nl.nn.adapterframework.http.rest.ApiListener.HttpMethod;
import nl.nn.adapterframework.http.rest.ApiServiceDispatcher;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.soap.WsdlGenerator;
import nl.nn.adapterframework.soap.WsdlGeneratorUtils;
import nl.nn.adapterframework.util.StreamUtil;

/**
 * Shows all monitors.
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public final class Webservices extends Base {

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/webservices")
	@Relation("webservices")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getWebServices() throws ApiException {

		Map<String, Object> returnMap = new HashMap<String, Object>();

		List<Map<String, Object>> webServices = new ArrayList<Map<String, Object>>();
		for (Adapter adapter : getIbisManager().getRegisteredAdapters()) {
			for (Receiver receiver: adapter.getReceivers()) {
				IListener listener = receiver.getListener();
				if (listener instanceof RestListener) {
					RestListener rl = (RestListener) listener;
					Map<String, Object> service = new HashMap<String, Object>();
					service.put("name", adapter.getName() + " "+  receiver.getName());
					service.put("method", rl.getMethod());
					service.put("view", rl.isView());
					service.put("uriPattern", rl.getUriPattern());
					webServices.add(service);
				}
			}
		}
		returnMap.put("services", webServices);

		List<Map<String, Object>> wsdls = new ArrayList<Map<String, Object>>();
		for (Adapter adapter : getIbisManager().getRegisteredAdapters()) {
			Map<String, Object> wsdlMap = null;
			try {
				if(WsdlGeneratorUtils.canProvideWSDL(adapter)) { // check eligibility
					wsdlMap = new HashMap<String, Object>(2);
					WsdlGenerator wsdl = new WsdlGenerator(adapter.getPipeLine());
					wsdlMap.put("name", wsdl.getName());
					wsdlMap.put("extension", getWsdlExtension());
				}
			} catch (Exception e) {
				wsdlMap.put("name", adapter.getName());

				if (e.getMessage() != null) {
					wsdlMap.put("error", e.getMessage());
				} else {
					wsdlMap.put("error", e.toString());
				}
			}
			if(wsdlMap != null) {
				wsdls.add(wsdlMap);
			}
		}
		returnMap.put("wsdls", wsdls);

		//ApiListeners
		List<Map<String, Object>> apiListeners = new LinkedList<Map<String, Object>>();
		SortedMap<String, ApiDispatchConfig> patternClients = ApiServiceDispatcher.getInstance().getPatternClients();
		for (Entry<String, ApiDispatchConfig> client : patternClients.entrySet()) {
			ApiDispatchConfig config = client.getValue();

			Set<HttpMethod> methods = config.getMethods();
			for (HttpMethod method : methods) {
				ApiListener listener = config.getApiListener(method);
				Receiver receiver = listener.getReceiver();
				IAdapter adapter = receiver == null? null : receiver.getAdapter();
				Map<String, Object> endpoint = new HashMap<>();
				String uriPattern = listener.getUriPattern();
				endpoint.put("uriPattern", uriPattern);
				endpoint.put("method", method);
				if (adapter!=null) endpoint.put("adapter", adapter.getName());
				if (receiver!=null) endpoint.put("receiver", receiver.getName());

				apiListeners.add(endpoint);
			}
		}
		returnMap.put("apiListeners", apiListeners);

		return Response.status(Response.Status.OK).entity(returnMap).build();
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/webservices/openapi.json")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getOpenApiSpec(@QueryParam("uri") String specUri) {
		ApiServiceDispatcher dispatcher = ApiServiceDispatcher.getInstance();
		JsonObject jsonSchema = null;
		if(StringUtils.isNotBlank(specUri)) {
			ApiDispatchConfig apiConfig = dispatcher.findConfigForUri(specUri);
			if(apiConfig != null) {
				jsonSchema = dispatcher.generateOpenApiJsonSchema(apiConfig, request);
			}
		} else {
			jsonSchema = dispatcher.generateOpenApiJsonSchema(request);
		}
		if(jsonSchema == null) {
			throw new ApiException("unable to find Dispatch configuration for uri");
		}

		return Response.ok(createStreamingOutput(jsonSchema)).build();
	}

	private StreamingOutput createStreamingOutput(final JsonObject jsonSchema) {
		return new StreamingOutput() {
			@Override
			public void write(OutputStream out) throws IOException, WebApplicationException {
				Map<String, Boolean> config = new HashMap<>();
				config.put(JsonGenerator.PRETTY_PRINTING, true);
				JsonWriterFactory factory = Json.createWriterFactory(config);
				try (JsonWriter jsonWriter = factory.createWriter(out, StreamUtil.DEFAULT_CHARSET)) {
					jsonWriter.write(jsonSchema);
				}
			}
		};
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/webservices/{resourceName}")
	@Relation("webservices")
	@Produces(MediaType.APPLICATION_XML)
	public Response getWsdl(
		@PathParam("resourceName") String resourceName,
		@DefaultValue("true") @QueryParam("indent") boolean indent,
		@DefaultValue("false") @QueryParam("useIncludes") boolean useIncludes) throws ApiException {

		String adapterName;
		boolean zip;
		int dotPos=resourceName.lastIndexOf('.');
		if (dotPos>=0) {
			adapterName=resourceName.substring(0,dotPos);
			zip=resourceName.substring(dotPos).equals(".zip");
		} else {
			adapterName=resourceName;
			zip=false;
		}

		if (StringUtils.isEmpty(adapterName)) {
			return Response.status(Response.Status.BAD_REQUEST).entity("<error>no adapter specified</error>").build();
		}
		IAdapter adapter = getIbisManager().getRegisteredAdapter(adapterName);
		if (adapter == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity("<error>adapter not found</error>").build();
		}
		try {
			String servletName = getServiceEndpoint(adapter);
			String generationInfo = "by FrankConsole";
			WsdlGenerator wsdl = new WsdlGenerator(adapter.getPipeLine(), generationInfo);
			wsdl.setIndent(indent);
			wsdl.setUseIncludes(useIncludes||zip);
			wsdl.init();
			StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream out) throws IOException, WebApplicationException {
					try {
						if (zip) {
							wsdl.zip(out, servletName);
						} else {
							wsdl.wsdl(out, servletName);
						}
					} catch (ConfigurationException | XMLStreamException e) {
						throw new WebApplicationException(e);
					}
				}
			};
			ResponseBuilder responseBuilder = Response.ok(stream);
			if (zip) {
				responseBuilder.type(MediaType.APPLICATION_OCTET_STREAM);
				responseBuilder.header("Content-Disposition", "attachment; filename=\""+adapterName+".zip\"");
			}
			return responseBuilder.build();

		} catch (Exception e) {
			throw new ApiException("exception on retrieving wsdl", e);
		}
	}

	private String getServiceEndpoint(IAdapter adapter) {
		String endpoint = "external address of ibis";
		Iterator it = adapter.getReceivers().iterator();
		while(it.hasNext()) {
			IListener listener = ((Receiver) it.next()).getListener();
			if(listener instanceof WebServiceListener) {
				String address = ((WebServiceListener) listener).getAddress();
				if(StringUtils.isNotEmpty(address)) {
					endpoint = address;
				} else {
					endpoint = "rpcrouter";
				}
				String protocol = request.isSecure() ? "https://" : "http://";
				int port = request.getServerPort();
				String restBaseUrl = protocol + request.getServerName() + (port != 0 ? ":" + port : "") + request.getContextPath() + "/services/";
				endpoint = restBaseUrl + endpoint;
				break;	//what if there are more than 1 WebServiceListener
			}
		}
		return endpoint;
	}

	private String getWsdlExtension() {
		return ".wsdl";
	}
}

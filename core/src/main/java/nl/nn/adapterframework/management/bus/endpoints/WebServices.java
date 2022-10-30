/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.management.bus.endpoints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import org.springframework.messaging.Message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.http.RestListener;
import nl.nn.adapterframework.http.rest.ApiDispatchConfig;
import nl.nn.adapterframework.http.rest.ApiListener;
import nl.nn.adapterframework.http.rest.ApiListener.HttpMethod;
import nl.nn.adapterframework.http.rest.ApiServiceDispatcher;
import nl.nn.adapterframework.management.bus.BusAware;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.ResponseMessage;
import nl.nn.adapterframework.management.bus.TopicSelector;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.soap.WsdlGenerator;
import nl.nn.adapterframework.soap.WsdlGeneratorUtils;

@BusAware("frank-management-bus")
public class WebServices extends BusEndpointBase {
	private static final String WSDL_EXTENSION = ".wsdl";

	@TopicSelector(BusTopic.WEBSERVICES)
	public Message<String> getWebServices(Message<?> message) {
		Map<String, Object> returnMap = new HashMap<>();

		returnMap.put("services", getRestListeners());
		returnMap.put("wsdls", getWsdls());
		returnMap.put("apiListeners", getApiListeners());

		return ResponseMessage.ok(returnMap);
	}

	private List<ListenerDAO> getApiListeners() {
		List<ListenerDAO> apiListeners = new LinkedList<>();
		SortedMap<String, ApiDispatchConfig> patternClients = ApiServiceDispatcher.getInstance().getPatternClients();
		for (Entry<String, ApiDispatchConfig> client : patternClients.entrySet()) {
			ApiDispatchConfig config = client.getValue();

			Set<HttpMethod> methods = config.getMethods();
			for (HttpMethod method : methods) {
				ApiListener listener = config.getApiListener(method);
				Receiver<?> receiver = listener.getReceiver();
				IAdapter adapter = receiver == null? null : receiver.getAdapter();
				ListenerDAO dao = new ListenerDAO(listener);
				if (adapter!=null) dao.setAdapter(adapter);
				if (receiver!=null) dao.setReceiver(receiver);

				apiListeners.add(dao);
			}
		}
		return apiListeners;
	}

	private List<Map<String, Object>> getWsdls() {
		List<Map<String, Object>> wsdls = new ArrayList<>();
		for (Configuration config : getIbisManager().getConfigurations()) {
			for (Adapter adapter : config.getRegisteredAdapters()) {
				Map<String, Object> wsdlMap = null;
				try {
					if(WsdlGeneratorUtils.canProvideWSDL(adapter)) { // check eligibility
						wsdlMap = new HashMap<>(2);
						WsdlGenerator wsdl = new WsdlGenerator(adapter.getPipeLine());
						wsdlMap.put("name", wsdl.getName());
						wsdlMap.put("extension", WSDL_EXTENSION);
					}
				} catch (Exception e) {
					wsdlMap = new HashMap<>(2);
					wsdlMap.put("name", adapter.getName());
					wsdlMap.put("error", e.getMessage() != null ? e.getMessage() : e.toString());
				}
				if(wsdlMap != null) {
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
				for (Receiver receiver: adapter.getReceivers()) {
					IListener listener = receiver.getListener();
					if (listener instanceof RestListener) {
						ListenerDAO dao = new ListenerDAO((RestListener) listener);
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
		private final @Getter String method;
		private final @Getter String uriPattern;
		private @Getter String receiver;
		private @Getter String adapter;

		public ListenerDAO(RestListener listener) {
			this.name = listener.getName();
			this.method = listener.getMethod();
			this.uriPattern = listener.getUriPattern();
		}

		public ListenerDAO(ApiListener listener) {
			this.name = listener.getName();
			this.method = listener.getMethod().name();
			this.uriPattern = listener.getUriPattern();
		}

		public void setReceiver(Receiver<?> receiver) {
			this.receiver = receiver.getName();
		}

		public void setAdapter(IAdapter adapter) {
			this.adapter = adapter.getName();
		}
	}
}

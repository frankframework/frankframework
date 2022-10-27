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

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.IbisManager;
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
public class WebServices {
	private @Getter @Setter IbisManager ibisManager;
	private static final String WSDL_EXTENSION = ".wsdl";

	@TopicSelector(BusTopic.WEBSERVICES)
	public Message<String> getWebServices(Message<?> message) {
		Map<String, Object> returnMap = new HashMap<>();

		List<Map<String, Object>> webServices = new ArrayList<>();
		for (Adapter adapter : getIbisManager().getRegisteredAdapters()) {
			for (Receiver receiver: adapter.getReceivers()) {
				IListener listener = receiver.getListener();
				if (listener instanceof RestListener) {
					RestListener rl = (RestListener) listener;
					Map<String, Object> service = new HashMap<>();
					service.put("name", adapter.getName() + " "+  receiver.getName());
					service.put("method", rl.getMethod());
					service.put("view", rl.isView());
					service.put("uriPattern", rl.getUriPattern());
					webServices.add(service);
				}
			}
		}
		returnMap.put("services", webServices);

		List<Map<String, Object>> wsdls = new ArrayList<>();
		for (Adapter adapter : getIbisManager().getRegisteredAdapters()) {
			Map<String, Object> wsdlMap = null;
			try {
				if(WsdlGeneratorUtils.canProvideWSDL(adapter)) { // check eligibility
					wsdlMap = new HashMap<>(2);
					WsdlGenerator wsdl = new WsdlGenerator(adapter.getPipeLine());
					wsdlMap.put("name", wsdl.getName());
					wsdlMap.put("extension", WSDL_EXTENSION);
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
		List<Map<String, Object>> apiListeners = new LinkedList<>();
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

		return ResponseMessage.ok(returnMap);
	}
}

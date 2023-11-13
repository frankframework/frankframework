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

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.messaging.Message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IProvidesMessageBrowsers;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.ProcessState;
import nl.nn.adapterframework.management.bus.BusAware;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.JsonResponseMessage;
import nl.nn.adapterframework.management.bus.TopicSelector;
import nl.nn.adapterframework.receivers.Receiver;

@BusAware("frank-management-bus")
public class InlineStorage extends BusEndpointBase {

	@TopicSelector(BusTopic.INLINESTORAGE_SUMMARY)
	public Message<String> getProcessStores(Message<?> message) {
		Map<String, InlineStoreStateItem> storeItemsGroupedByProcessState = new LinkedHashMap<>();
		Stream.of(ProcessState.values()).forEach(t -> storeItemsGroupedByProcessState.put(t.getName(), new InlineStoreStateItem())); // init item for each process state

		for(Configuration config : getIbisManager().getConfigurations()) {
			for(Adapter adapter : config.getRegisteredAdapters()) {
				for (Receiver<?> receiver : adapter.getReceivers()) {
					IListener<?> listener=receiver.getListener();
					if (listener instanceof IProvidesMessageBrowsers) {
						for(ProcessState state : receiver.knownProcessStates()) {
							IMessageBrowser<?> browser = receiver.getMessageBrowser(state);
							if(browser != null) {
								try {
									int count = browser.getMessageCount();
									if(count > 0) {
										InlineStoreItem item = new InlineStoreItem(config.getName(), adapter.getName(), receiver.getName(), count);
										storeItemsGroupedByProcessState.get(state.getName()).getItems().add(item);
										storeItemsGroupedByProcessState.get(state.getName()).setTotalMessageCount(storeItemsGroupedByProcessState.get(state.getName()).getTotalMessageCount() + count);
									}
								} catch(ListenerException e) {
									log.warn("Cannot determine number of messages in process state ["+state+"]", e);
								}
							}
						}
					}
				}
			}
		}

		return new JsonResponseMessage(storeItemsGroupedByProcessState);
	}

	private static class InlineStoreStateItem {
		private @Getter List<InlineStoreItem> items = new LinkedList<>();
		private @Getter @Setter int totalMessageCount;
	}

	@AllArgsConstructor
	private static class InlineStoreItem {
		private @Getter @Setter String configurationName;
		private @Getter @Setter String adapterName;
		private @Getter @Setter String receiverName;
		private @Getter @Setter int messageCount;
	}
}

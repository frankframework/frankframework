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
package org.frankframework.management.bus.endpoints;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.annotation.security.RolesAllowed;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.frankframework.configuration.Configuration;
import org.frankframework.core.Adapter;
import org.frankframework.core.IListener;
import org.frankframework.core.IMessageBrowser;
import org.frankframework.core.IProvidesMessageBrowsers;
import org.frankframework.core.ListenerException;
import org.frankframework.core.ProcessState;
import org.frankframework.management.bus.BusAware;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.TopicSelector;
import org.frankframework.management.bus.message.JsonMessage;
import org.frankframework.receivers.Receiver;
import org.springframework.messaging.Message;

@BusAware("frank-management-bus")
public class InlineStorage extends BusEndpointBase {

	@TopicSelector(BusTopic.INLINESTORAGE_SUMMARY)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
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
										InlineStoreItem item = new InlineStoreItem(adapter.getName(), receiver.getName(), count);
										storeItemsGroupedByProcessState.get(state.getName()).getItems().add(item);
										storeItemsGroupedByProcessState.get(state.getName()).setTotalMessageCount(storeItemsGroupedByProcessState.get(state.getName()).getTotalMessageCount() + count);
									}
								} catch(ListenerException e) {
									log.warn("Cannot determine number of messages in process state [{}]", state, e);
								}
							}
						}
					}
				}
			}
		}

		return new JsonMessage(storeItemsGroupedByProcessState);
	}

	private static class InlineStoreStateItem {
		private final @Getter List<InlineStoreItem> items = new ArrayList<>();
		private @Getter @Setter int totalMessageCount;
	}

	@AllArgsConstructor
	private static class InlineStoreItem {
		private @Getter @Setter String adapterName;
		private @Getter @Setter String receiverName;
		private @Getter @Setter int messageCount;
	}
}

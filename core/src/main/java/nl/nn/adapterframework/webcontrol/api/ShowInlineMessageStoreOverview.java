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
package nl.nn.adapterframework.webcontrol.api;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IProvidesMessageBrowsers;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.ProcessState;
import nl.nn.adapterframework.receivers.Receiver;

@Path("/")
public final class ShowInlineMessageStoreOverview extends Base {

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("inlinestores/overview")
	@Produces(MediaType.APPLICATION_JSON)
	public Response execute() throws ApiException {

		Map<String, InlineStoreStateItem> storeItemsGroupedByProcessState = new LinkedHashMap<>();
		Stream.of(ProcessState.values()).forEach(t -> storeItemsGroupedByProcessState.put(t.getName(), new InlineStoreStateItem())); // init item for each process state

		for(Adapter adapter : getIbisManager().getRegisteredAdapters()) {
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
								log.warn("Cannot determine number of messages in process state ["+state+"]", e);
							}
						}
					}
				}
			}
		}

		return Response.status(Response.Status.CREATED).entity(storeItemsGroupedByProcessState).build();
	}

	private class InlineStoreStateItem {
		private @Getter List<InlineStoreItem> items = new LinkedList<>();
		private @Getter @Setter int totalMessageCount;
	}

	@AllArgsConstructor
	private class InlineStoreItem {
		private @Getter @Setter String adapterName;
		private @Getter @Setter String receiverName;
		private @Getter @Setter int messageCount;
	}
}

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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.receivers.Receiver;

@Path("/")
public class ShowConnectionOverview extends Base {

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/connections")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getConnections() throws ApiException {
		List<Object> connectionsIncoming = new LinkedList<>();

		for(Adapter adapter: getIbisManager().getRegisteredAdapters()) {
			for (Receiver<?> receiver: adapter.getReceivers()) {
				IListener<?> listener=receiver.getListener();
				if (listener instanceof HasPhysicalDestination) {
					String destination = ((HasPhysicalDestination)receiver.getListener()).getPhysicalDestinationName();
					String domain = ((HasPhysicalDestination)receiver.getListener()).getDomain();
					connectionsIncoming.add(addToMap(adapter.getName(), destination, listener.getName(), "Inbound", domain));
				}
			}

			PipeLine pipeline = adapter.getPipeLine();
			for (IPipe pipe : pipeline.getPipes()) {
				if (pipe instanceof MessageSendingPipe) {
					MessageSendingPipe msp=(MessageSendingPipe)pipe;
					ISender sender = msp.getSender();
					if (sender instanceof HasPhysicalDestination) {
						String destination = ((HasPhysicalDestination)sender).getPhysicalDestinationName();
						String domain = ((HasPhysicalDestination)sender).getDomain();
						connectionsIncoming.add(addToMap(adapter.getName(), destination, sender.getName(), "Outbound", domain));
					}
					IListener<?> listener = msp.getListener();
					if (listener instanceof HasPhysicalDestination) {
						String destination = ((HasPhysicalDestination)listener).getPhysicalDestinationName();
						String domain = ((HasPhysicalDestination)listener).getDomain();
						connectionsIncoming.add(addToMap(adapter.getName(), destination, listener.getName(), "Inbound", domain));
					}
				}
			}
		}

		Map<String, List<Object>> allConnections = new LinkedHashMap<>();
		allConnections.put("data", connectionsIncoming);

		return Response.status(Response.Status.OK).entity(allConnections).build();
	}

	private Map<String, Object> addToMap(String adapterName, String destination, String name, String direction, String domain) {
		Map<String, Object> connection = new HashMap<>();
		connection.put("adapterName", adapterName);
		connection.put("destination", destination);
		connection.put("componentName", name);
		connection.put("direction", direction);
		connection.put("domain", domain);
		return connection;
	}
}

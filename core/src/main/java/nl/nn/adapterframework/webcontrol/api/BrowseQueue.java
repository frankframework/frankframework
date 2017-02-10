/*
Copyright 2016 Integration Partners B.V.

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.jms.JmsMessageBrowser;
import nl.nn.adapterframework.jms.JmsRealmFactory;

/**
* Send a message with JMS.
* 
* @author	Niels Meijer
*/

@Path("/")
public final class BrowseQueue extends Base {

	@Context ServletConfig servletConfig;

	@GET
	@RolesAllowed({"ObserverAccess", "IbisTester"})
	@Path("jms")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getBrowseQueue() throws ApiException {
		Map<String, Object> returnMap = new HashMap<String, Object>();

		initBase(servletConfig);

		List<String> jmsRealms=JmsRealmFactory.getInstance().getRegisteredRealmNamesAsList();
		if (jmsRealms.size()==0) jmsRealms.add("no realms defined");
		returnMap.put("jmsRealms", jmsRealms);

		return Response.status(Response.Status.OK).entity(returnMap).build();
	}

	@POST
	@Path("jms/browse")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response putBrowseQueue(LinkedHashMap<String, Object> json) throws ApiException {

		initBase(servletConfig);

		Map<String, Object> returnMap = new HashMap<String, Object>();

		String jmsRealm = null, destination = null, type = null;

		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			if(key.equalsIgnoreCase("realm")) {
				jmsRealm = entry.getValue().toString();
			}
			if(key.equalsIgnoreCase("destination")) {
				destination = entry.getValue().toString();
			}
			if(key.equalsIgnoreCase("type")) {
				type = entry.getValue().toString();
			}
		}

		if(jmsRealm == null)
			throw new ApiException("No realm provided");
		if(destination == null)
			throw new ApiException("No destination provided");
		if(type == null)
			throw new ApiException("No type provided");

		IMessageBrowsingIterator it = null;

		try {
			JmsMessageBrowser jmsBrowser = new JmsMessageBrowser();
			jmsBrowser.setName("BrowseQueueAction");
			jmsBrowser.setJmsRealm(jmsRealm);
			jmsBrowser.setDestinationName(destination);
			jmsBrowser.setDestinationType(type);
			IMessageBrowser browser = jmsBrowser;

			it = browser.getIterator();
			List<Map<String, Object>> messages = new ArrayList<Map<String, Object>>();
			while (it.hasNext()) {
				IMessageBrowsingIteratorItem item = it.next();
				Map<String, Object> message = new HashMap<String, Object>();
				message.put("comment", item.getCommentString());
				message.put("correlationId", item.getCorrelationId());
				message.put("expiryDate", item.getExpiryDate());
				message.put("host", item.getHost());
				message.put("id", item.getId());
				message.put("insertDate", item.getInsertDate());
				message.put("type", item.getType());
				message.put("label", item.getLabel());

				messages.add(message);
			}

			log.debug("Browser returned " + messages.size() + " messages");
			returnMap.put("numberOfMessages", messages.size());
			returnMap.put("messages", messages);

		}
		catch (Exception e) {
			throw new ApiException("Error occured browsing messages: " + e.getMessage());
		}
		finally {
			try {
				if (it!=null)
					it.close();
			} catch (ListenerException e) {
				log.error(e);
			}
		}

		return Response.status(Response.Status.OK).entity(returnMap).build();
	}
}
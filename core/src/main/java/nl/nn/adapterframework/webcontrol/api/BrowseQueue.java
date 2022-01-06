/*
   Copyright 2016-2021 WeAreFrank!

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
import nl.nn.adapterframework.jms.JMSFacade.DestinationType;
import nl.nn.adapterframework.jms.JmsBrowser;
import nl.nn.adapterframework.jms.JmsMessageBrowserIteratorItem;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.jndi.JndiConnectionFactoryFactory;
import nl.nn.adapterframework.util.EnumUtils;

/**
 * Send a message with JMS.
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public final class BrowseQueue extends Base {

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("jms")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getBrowseQueue() throws ApiException {
		Map<String, Object> returnMap = new HashMap<String, Object>();
		JndiConnectionFactoryFactory connectionFactoryFactory = getIbisContext().getBean("connectionFactoryFactory", JndiConnectionFactoryFactory.class);
		Set<String> connectionFactories = new LinkedHashSet<String>();
		// connection factories used in configured jmsSenders etc.
		connectionFactories.addAll(connectionFactoryFactory.getConnectionFactoryNames());
		// configured jms realm
		connectionFactories.addAll(JmsRealmFactory.getInstance().getConnectionFactoryNames());
		if (connectionFactories.size()==0) connectionFactories.add("no connection factories found");
		returnMap.put("connectionFactories", connectionFactories);

		return Response.status(Response.Status.OK).entity(returnMap).build();
	}

	@POST
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("jms/browse")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response putBrowseQueue(LinkedHashMap<String, Object> json) throws ApiException {

		Map<String, Object> returnMap = new HashMap<String, Object>();

		String connectionFactory = null,
				destination = null;
		boolean rowNumbersOnly = false,
				showPayload = false,
				lookupDestination=false;
		DestinationType type = null;

		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			if(key.equalsIgnoreCase("connectionFactory")) {
				connectionFactory = entry.getValue().toString();
			}
			if(key.equalsIgnoreCase("destination")) {
				destination = entry.getValue().toString();
			}
			if(key.equalsIgnoreCase("type")) {
				type = EnumUtils.parse(DestinationType.class, entry.getValue().toString());
			}
			if(key.equalsIgnoreCase("rowNumbersOnly")) {
				rowNumbersOnly = Boolean.parseBoolean(entry.getValue().toString());
			}
			if(key.equalsIgnoreCase("payload")) {
				showPayload = Boolean.parseBoolean(entry.getValue().toString());
			}
			if(key.equalsIgnoreCase("lookupDestination")) {
				lookupDestination = Boolean.parseBoolean(entry.getValue().toString());
			}
		}

		if(connectionFactory == null)
			throw new ApiException("No connection factory provided");
		if(destination == null)
			throw new ApiException("No destination provided");
		if(type == null)
			throw new ApiException("No type provided");

		try {
			JmsBrowser<javax.jms.Message> jmsBrowser = getIbisContext().createBeanAutowireByName(JmsBrowser.class);
			jmsBrowser.setName("BrowseQueueAction");
			if(type.equals("QUEUE")) {
				jmsBrowser.setQueueConnectionFactoryName(connectionFactory);
			} else {
				jmsBrowser.setTopicConnectionFactoryName(connectionFactory);
			}
			jmsBrowser.setDestinationName(destination);
			jmsBrowser.setDestinationType(type);
			jmsBrowser.setLookupDestination(lookupDestination);

			List<Map<String, Object>> messages = new ArrayList<Map<String, Object>>();
			try (IMessageBrowsingIterator it = jmsBrowser.getIterator()) {
				while (it.hasNext()) {
					IMessageBrowsingIteratorItem item = it.next();
					Map<String, Object> message = new HashMap<String, Object>();
					message.put("comment", item.getCommentString());
					message.put("correlationId", item.getCorrelationId());
					try {
						message.put("expiryDate", item.getExpiryDate());
					} catch (Exception e) {
						log.warn("Could not get expiryDate",e);
					}
					message.put("host", item.getHost());
					message.put("id", item.getId());
					try {
						message.put("insertDate", item.getInsertDate());
					} catch (Exception e) {
						log.warn("Could not get insertDate",e);
					}
					if(showPayload && item instanceof JmsMessageBrowserIteratorItem) {
						message.put("text", ((JmsMessageBrowserIteratorItem) item).getText());
					}
	
					messages.add(message);
				}
			}

			log.debug("Browser returned " + messages.size() + " messages");
			returnMap.put("numberOfMessages", messages.size());

			if(!rowNumbersOnly) {
				returnMap.put("messages", messages);
			}
		}
		catch (Exception e) {
			throw new ApiException("Error occured browsing messages", e);
		}

		return Response.status(Response.Status.OK).entity(returnMap).build();
	}
}

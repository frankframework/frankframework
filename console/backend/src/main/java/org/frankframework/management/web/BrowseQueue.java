/*
   Copyright 2016-2023 WeAreFrank!

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
package org.frankframework.management.web;

import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import jakarta.annotation.security.RolesAllowed;
import org.apache.commons.lang3.StringUtils;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;

import org.frankframework.util.RequestUtils;

/**
 * Send a message with JMS.
 *
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public final class BrowseQueue extends FrankApiBase {

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("jms")
	@Produces(MediaType.APPLICATION_JSON)
	@Relation("queuebrowser")
	@Description("view a list of all JMS QueueConnectionFactories")
	public Response getQueueConnectionFactories() {
		return callSyncGateway(RequestMessageBuilder.create(this, BusTopic.QUEUE, BusAction.GET));
	}

	@POST
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("jms/browse")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Relation("queuebrowser")
	@Description("view a list of messages on a specific JMS queue")
	public Response browseQueue(Map<String, Object> json) {

		String connectionFactory = RequestUtils.getValue(json, "connectionFactory");
		String destination = RequestUtils.getValue(json, "destination");
		Boolean rowNumbersOnly = RequestUtils.getBooleanValue(json, "rowNumbersOnly");
		Boolean showPayload = RequestUtils.getBooleanValue(json, "payload");
		Boolean lookupDestination = RequestUtils.getBooleanValue(json, "lookupDestination");
		String type = RequestUtils.getValue(json, "type");

		if(StringUtils.isNotEmpty(destination))
			throw new ApiException("No destination provided");
		if(StringUtils.isNotEmpty(type))
			throw new ApiException("No type provided");

		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.QUEUE, BusAction.FIND);
		builder.addHeader(BusMessageUtils.HEADER_CONNECTION_FACTORY_NAME_KEY, connectionFactory);
		builder.addHeader("destination", destination);
		builder.addHeader("type", type);
		if(rowNumbersOnly != null) {
			builder.addHeader("rowNumbersOnly", rowNumbersOnly);
		}
		if(showPayload != null) {
			builder.addHeader("showPayload", showPayload);
		}
		if(lookupDestination != null) {
			builder.addHeader("lookupDestination", lookupDestination);
		}
		return callSyncGateway(builder);
	}
}

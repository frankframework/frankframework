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

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
	public Response getBrowseQueue() {
		return callSyncGateway(RequestMessageBuilder.create(this, BusTopic.QUEUE, BusAction.GET));
	}

	@POST
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("jms/browse")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response putBrowseQueue(Map<String, Object> json) {

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

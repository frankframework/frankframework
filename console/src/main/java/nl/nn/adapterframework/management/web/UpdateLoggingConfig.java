/*
   Copyright 2016-2022 WeAreFrank!

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
package nl.nn.adapterframework.management.web;

import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.Level;

import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusTopic;

/**
 * Read and update logging configuration
 *
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public class UpdateLoggingConfig extends FrankApiBase {

	@GET
	@PermitAll
	@Path("/server/logging")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLogConfiguration() {
		return callSyncGateway(RequestMessageBuilder.create(this, BusTopic.LOG_CONFIGURATION, BusAction.GET));
	}

	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/server/logging")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateLogConfiguration(Map<String, Object> json) {
		Level loglevel = Level.toLevel(getValue(json, "loglevel"), null);
		Boolean logIntermediaryResults = getBooleanValue(json, "logIntermediaryResults");
		Integer maxMessageLength = getIntegerValue(json, "maxMessageLength");
		Boolean enableDebugger = getBooleanValue(json, "enableDebugger");

		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.LOG_CONFIGURATION, BusAction.MANAGE);
		builder.addHeader("logLevel", loglevel==null?null:loglevel.name());
		builder.addHeader("logIntermediaryResults", logIntermediaryResults);
		builder.addHeader("maxMessageLength", maxMessageLength);
		builder.addHeader("enableDebugger", enableDebugger);
		return callAsyncGateway(builder);
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/server/logging/settings")
	@Relation("logging")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLogSettings(@QueryParam("filter") String filter) {
		RequestMessageBuilder request = RequestMessageBuilder.create(this, BusTopic.LOG_DEFINITIONS, BusAction.GET);
		request.addHeader("filter", filter);
		return callSyncGateway(request);
	}

	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/server/logging/settings")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateLogger(Map<String, Object> json) {
		RequestMessageBuilder request = RequestMessageBuilder.create(this, BusTopic.LOG_DEFINITIONS, BusAction.MANAGE);

		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if(key.equalsIgnoreCase("level")) {
				Level level = Level.toLevel(""+value, null);
				if(level != null) {
					request.addHeader("level", level.name());
				}
			} else if(key.equalsIgnoreCase("logger")) {
				String logPackage = (String) value;
				request.addHeader("logPackage", logPackage);
			} else if(key.equalsIgnoreCase("reconfigure")) {
				boolean reconfigure = Boolean.parseBoolean(""+value);
				request.addHeader("reconfigure", reconfigure);
			}
		}


		return callSyncGateway(request);
	}
}

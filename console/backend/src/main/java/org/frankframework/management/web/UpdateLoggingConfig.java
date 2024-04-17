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
package org.frankframework.management.web;

import java.util.Map;
import java.util.Map.Entry;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import jakarta.annotation.security.RolesAllowed;
import org.apache.logging.log4j.Level;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTopic;

import org.frankframework.util.RequestUtils;

/**
 * Read and update logging configuration
 *
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public class UpdateLoggingConfig extends FrankApiBase {

	@GET
	@RolesAllowed({ "IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/server/logging")
	@Produces(MediaType.APPLICATION_JSON)
	@Relation("logging")
	@Description("view the application log configuration")
	public Response getLogConfiguration() {
		return callSyncGateway(RequestMessageBuilder.create(this, BusTopic.LOG_CONFIGURATION, BusAction.GET));
	}

	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/server/logging")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Relation("logging")
	@Description("update the application log configuration")
	public Response updateLogConfiguration(Map<String, Object> json) {
		Level loglevel = Level.toLevel(RequestUtils.getValue(json, "loglevel"), null);
		Boolean logIntermediaryResults = RequestUtils.getBooleanValue(json, "logIntermediaryResults");
		Integer maxMessageLength = RequestUtils.getIntegerValue(json, "maxMessageLength");
		Boolean enableDebugger = RequestUtils.getBooleanValue(json, "enableDebugger");

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
	@Description("view the log definitions, default loggers and their corresponding levels")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLogDefinitions(@QueryParam("filter") String filter) {
		RequestMessageBuilder request = RequestMessageBuilder.create(this, BusTopic.LOG_DEFINITIONS, BusAction.GET);
		request.addHeader("filter", filter);
		return callSyncGateway(request);
	}

	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/server/logging/settings")
	@Consumes(MediaType.APPLICATION_JSON)
	@Relation("logging")
	@Description("update the loglevel of a specific logger")
	public Response updateLogDefinition(Map<String, Object> json) {
		RequestMessageBuilder request = RequestMessageBuilder.create(this, BusTopic.LOG_DEFINITIONS, BusAction.MANAGE);

		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if("level".equalsIgnoreCase(key)) {
				Level level = Level.toLevel(""+value, null);
				if(level != null) {
					request.addHeader("level", level.name());
				}
			} else if("logger".equalsIgnoreCase(key)) {
				String logPackage = (String) value;
				request.addHeader("logPackage", logPackage);
			} else if("reconfigure".equalsIgnoreCase(key)) {
				boolean reconfigure = Boolean.parseBoolean(""+value);
				request.addHeader("reconfigure", reconfigure);
			}
		}


		return callSyncGateway(request);
	}
}

/*
Copyright 2016-2017, 2019, 2021 WeAreFrank!

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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.springframework.context.ApplicationContext;

import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.web.ApiException;
import nl.nn.adapterframework.management.web.RequestMessageBuilder;
import nl.nn.adapterframework.monitoring.AdapterFilter;
import nl.nn.adapterframework.monitoring.EventType;
import nl.nn.adapterframework.monitoring.ITrigger;
import nl.nn.adapterframework.monitoring.ITrigger.TriggerType;
import nl.nn.adapterframework.monitoring.Monitor;
import nl.nn.adapterframework.monitoring.MonitorManager;
import nl.nn.adapterframework.monitoring.Severity;
import nl.nn.adapterframework.monitoring.SourceFiltering;
import nl.nn.adapterframework.monitoring.Trigger;
import nl.nn.adapterframework.util.EnumUtils;
import nl.nn.adapterframework.util.SpringUtils;

/**
 * Shows all monitors.
 * 
 * @since 7.0-B1
 * @author Niels Meijer
 */

@Path("/configurations/{configuration}/monitors")
public class ShowMonitors extends Base {

	private MonitorManager getMonitorManager(String configurationName) {
		ApplicationContext applicationContext = getIbisManager().getConfiguration(configurationName);
		if(applicationContext == null) {
			throw new IllegalStateException("configuration [" + configurationName + "] not found");
		}

		return getMonitorManager(applicationContext);
	}

	private MonitorManager getMonitorManager(ApplicationContext applicationContext) {
		return applicationContext.getBean("monitorManager", MonitorManager.class);
	}

	@GET
	@Path("/")
	@RolesAllowed({ "IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	public Response getMonitors(@PathParam("configuration") String configurationName, @DefaultValue("false") @QueryParam("xml") boolean showConfigXml) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MONITORING, BusAction.GET);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configurationName);
		builder.addHeader("xml", showConfigXml);
		return callSyncGateway(builder);
	}

	@GET
	@RolesAllowed({ "IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/{monitorName}")
	@Produces()
	public Response getMonitor(@PathParam("configuration") String configurationName, @PathParam("monitorName") String monitorName, @DefaultValue("false") @QueryParam("xml") boolean showConfigXml) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MONITORING, BusAction.GET);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configurationName);
		builder.addHeader("monitor", monitorName);
		builder.addHeader("xml", showConfigXml);
		return callSyncGateway(builder, true);
	}

	@PUT
	@RolesAllowed({ "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/{monitorName}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateMonitor(@PathParam("configuration") String configName, @PathParam("monitorName") String monitorName, Map<String, Object> json) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MONITORING, BusAction.GET);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configName);
		builder.addHeader("monitor", monitorName);

		for(Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			if(key.equalsIgnoreCase("state")) {
				builder.addHeader("state", String.valueOf(entry.getValue()));
			} else if(key.equalsIgnoreCase("name")) {
				builder.addHeader("name", String.valueOf(entry.getValue()));
			} else if(key.equalsIgnoreCase("type")) {
				builder.addHeader("type", String.valueOf(entry.getValue()));
			} else if(key.equalsIgnoreCase("destinations")) {
				if(entry.getValue() instanceof List<?>) {
					String destinations = String.join(",", ((List<String>) entry.getValue()));
					builder.addHeader("destinations", destinations);
				}
				throw new ApiException("cannot parse destinations");
			}
		}

		return callSyncGateway(builder);
	}

	@SuppressWarnings("unchecked")
	private Set<String> parseDestinations(Object entry) {
		List<String> destinations = new ArrayList<>();
		try {
			destinations.addAll((ArrayList<String>) entry);
		} catch (Exception e) {
			throw new ApiException("failed to parse destinations", e);
		}

		if(destinations.isEmpty()) {
			return null;
		}

		return new HashSet<>(destinations);
	}

	@DELETE
	@RolesAllowed({ "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/{monitorName}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteMonitor(@PathParam("configuration") String configurationName, @PathParam("monitorName") String monitorName) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MONITORING, BusAction.DELETE);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configurationName);
		builder.addHeader("monitor", monitorName);
		return callSyncGateway(builder);
	}

	@POST
	@RolesAllowed({ "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/{monitorName}/triggers")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateTrigger(@PathParam("configuration") String configName, @PathParam("monitorName") String monitorName, Map<String, Object> json) {

		MonitorManager mm = getMonitorManager(configName);
		Monitor monitor = mm.findMonitor(monitorName);

		if(monitor == null) {
			throw new ApiException("Monitor not found!", Status.NOT_FOUND);
		}

		ITrigger trigger = SpringUtils.createBean(mm.getApplicationContext(), Trigger.class);
		handleTrigger(trigger, json);
		monitor.registerTrigger(trigger);
		monitor.configure();

		return Response.status(Status.OK).build();
	}

	@GET
	@RolesAllowed({ "IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/{monitorName}/triggers/{triggerId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTriggers(@PathParam("configuration") String configurationName, @PathParam("monitorName") String monitorName, @PathParam("triggerId") Integer id) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MONITORING, BusAction.GET);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configurationName);
		builder.addHeader("monitor", monitorName);
		builder.addHeader("trigger", id);
		return callSyncGateway(builder, true);
	}

	@PUT
	@RolesAllowed({ "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/{monitorName}/triggers/{trigger}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateTrigger(@PathParam("configuration") String configName, @PathParam("monitorName") String monitorName, @PathParam("trigger") int index, Map<String, Object> json) throws ApiException {

		MonitorManager mm = getMonitorManager(configName);
		Monitor monitor = mm.findMonitor(monitorName);

		if(monitor == null) {
			throw new ApiException("Monitor not found!", Status.NOT_FOUND);
		}

		ITrigger trigger = monitor.getTrigger(index);
		if(trigger == null) {
			throw new ApiException("Trigger not found!", Status.NOT_FOUND);
		}

		handleTrigger(trigger, json);

		return Response.status(Status.OK).build();
	}

	@SuppressWarnings("unchecked")
	private void handleTrigger(ITrigger trigger, Map<String, Object> json) {
		List<String> eventList = null;
		TriggerType type = null;
		Severity severity = null;
		int threshold = 0;
		int period = 0;
		SourceFiltering filter = null;
		List<String> adapters = null;
		Map<String, List<String>> sources = null;

		for(Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			if(key.equalsIgnoreCase("events") && entry.getValue() instanceof List<?>) {
				eventList = (List<String>) entry.getValue();
			} else if(key.equalsIgnoreCase("type")) {
				type = EnumUtils.parse(TriggerType.class, entry.getValue().toString());
			} else if(key.equalsIgnoreCase("severity")) {
				severity = EnumUtils.parse(Severity.class, entry.getValue().toString());
			} else if(key.equalsIgnoreCase("threshold")) {
				threshold = (Integer.parseInt("" + entry.getValue()));
				if(threshold < 0) {
					throw new ApiException("threshold must be a positive number");
				}
			} else if(key.equalsIgnoreCase("period")) {
				period = (Integer.parseInt("" + entry.getValue()));
				if(period < 0) {
					throw new ApiException("period must be a positive number");
				}
			} else if(key.equalsIgnoreCase("filter")) {
				filter = EnumUtils.parse(SourceFiltering.class, entry.getValue().toString());
			} else if(key.equalsIgnoreCase("adapters") && entry.getValue() instanceof List<?>) {
				adapters = (List<String>) entry.getValue();
			} else if(key.equalsIgnoreCase("sources") && entry.getValue() instanceof Map<?, ?>) {
				sources = (Map<String, List<String>>) entry.getValue();
			}
		}

		// If no parse errors have occured we can continue!
		trigger.setEventCodes(eventList.toArray(new String[eventList.size()]));
		trigger.setTriggerType(type);
		trigger.setSeverity(severity);
		trigger.setThreshold(threshold);
		trigger.setPeriod(period);
		trigger.setSourceFiltering(filter);

		trigger.clearAdapterFilters();
		if(SourceFiltering.ADAPTER.equals(filter)) {
			for(String adapter : adapters) {
				AdapterFilter adapterFilter = new AdapterFilter();
				adapterFilter.setAdapter(adapter);
				trigger.registerAdapterFilter(adapterFilter);
			}
		} else if(SourceFiltering.SOURCE.equals(filter)) {
			for(Map.Entry<String, List<String>> entry : sources.entrySet()) {
				AdapterFilter adapterFilter = new AdapterFilter();
				adapterFilter.setAdapter(entry.getKey());
				for(String subObject : entry.getValue()) {
					adapterFilter.registerSubObject(subObject);
				}
				trigger.registerAdapterFilter(adapterFilter);
			}
		}
	}

	@DELETE
	@RolesAllowed({ "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/{monitorName}/triggers/{trigger}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteTrigger(@PathParam("configuration") String configurationName, @PathParam("monitorName") String monitorName, @PathParam("trigger") int id) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MONITORING, BusAction.DELETE);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configurationName);
		builder.addHeader("monitor", monitorName);
		builder.addHeader("trigger", id);
		return callSyncGateway(builder);
	}

	@POST
	@RolesAllowed({ "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response addMonitor(@PathParam("configuration") String configurationName, LinkedHashMap<String, Object> json) throws ApiException {

		String name = null;
		EventType type = null;
		Set<String> destinations = null;

		for(Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			if(key.equalsIgnoreCase("name")) {
				name = entry.getValue().toString();
			}
			else if(key.equalsIgnoreCase("type")) {
				type = EnumUtils.parse(EventType.class, entry.getValue().toString());
			}
			else if(key.equalsIgnoreCase("destinations")) {
				destinations = parseDestinations(entry);
			}
		}
		if(name == null)
			throw new ApiException("Name not set!", Status.BAD_REQUEST);
		if(type == null)
			throw new ApiException("Type not set!", Status.BAD_REQUEST);

		MonitorManager mm = getMonitorManager(configurationName);

		Monitor monitor = new Monitor();
		monitor.setName(name);
		monitor.setType(type);
		monitor.setDestinationSet(destinations);

		mm.addMonitor(monitor);

		return Response.status(Response.Status.CREATED).build();
	}
}

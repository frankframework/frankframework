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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;

import nl.nn.adapterframework.monitoring.AdapterFilter;
import nl.nn.adapterframework.monitoring.EventThrowing;
import nl.nn.adapterframework.monitoring.EventType;
import nl.nn.adapterframework.monitoring.ITrigger;
import nl.nn.adapterframework.monitoring.ITrigger.TriggerType;
import nl.nn.adapterframework.monitoring.Monitor;
import nl.nn.adapterframework.monitoring.MonitorException;
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
public final class ShowMonitors extends Base {
	private @Context Request request;

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
	@RolesAllowed({ "IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/")
	public Response getMonitors(@PathParam("configuration") String configurationName, @QueryParam("xml") boolean showConfigXml) throws ApiException {

		Map<String, Object> returnMap = new HashMap<>();
		MonitorManager mm = getMonitorManager(configurationName);

		if(showConfigXml) {
			String xml = mm.toXml().toXML();
			return Response.status(Status.OK).type(MediaType.APPLICATION_XML).entity(xml).build();
		}

		List<Map<String, Object>> monitors = new ArrayList<Map<String, Object>>();
		for(int i = 0; i < mm.getMonitors().size(); i++) {
			Monitor monitor = mm.getMonitor(i);

			monitors.add(mapMonitor(monitor));
		}

		returnMap.put("monitors", monitors);
		returnMap.put("enabled", new Boolean(mm.isEnabled()));
		returnMap.put("eventTypes", EnumUtils.getEnumList(EventType.class));
		returnMap.put("destinations", mm.getDestinations().keySet());

		return Response.status(Status.OK).type(MediaType.APPLICATION_JSON).entity(returnMap).build();
	}

	private Map<String, Object> mapMonitor(Monitor monitor) {
		Map<String, Object> monitorMap = new HashMap<String, Object>();
		monitorMap.put("name", monitor.getName());
		monitorMap.put("type", monitor.getType());
		monitorMap.put("destinations", monitor.getDestinationSet());
		monitorMap.put("lastHit", monitor.getLastHit());

		boolean isRaised = monitor.isRaised();
		monitorMap.put("raised", isRaised);
		monitorMap.put("changed", monitor.getStateChangeDt());
		monitorMap.put("hits", monitor.getAdditionalHitCount());

		if(isRaised) {
			Map<String, Object> alarm = new HashMap<>();
			alarm.put("severity", monitor.getAlarmSeverity());
			EventThrowing source = monitor.getAlarmSource();
			if(source != null) {
				String name = "";
				if(source.getAdapter() != null) {
					name = String.format("%s / %s", source.getAdapter().getName(), source.getEventSourceName());
				} else {
					name = source.getEventSourceName();
				}
				alarm.put("source", name);
			}
			monitorMap.put("alarm", alarm);
		}

		List<Map<String, Object>> triggers = new ArrayList<Map<String, Object>>();
		List<ITrigger> listOfTriggers = monitor.getTriggers();
		for(ITrigger trigger : listOfTriggers) {

			Map<String, Object> map = mapTrigger(trigger);
			map.put("id", listOfTriggers.indexOf(trigger));

			triggers.add(map);
		}
		monitorMap.put("triggers", triggers);

		List<String> destinations = new ArrayList<>();
		Set<String> d = monitor.getDestinationSet();
		for(Iterator<String> it = d.iterator(); it.hasNext();) {
			destinations.add(it.next());
		}
		monitorMap.put("destinations", destinations);

		return monitorMap;
	}

	private Map<String, Object> mapTrigger(ITrigger trigger) {
		Map<String, Object> triggerMap = new HashMap<String, Object>();

		triggerMap.put("type", trigger.getTriggerType().name());
		triggerMap.put("events", trigger.getEventCodes());
		triggerMap.put("severity", trigger.getSeverity());
		triggerMap.put("threshold", trigger.getThreshold());
		triggerMap.put("period", trigger.getPeriod());

		if(trigger.getAdapterFilters() != null) {
			Map<String, List<String>> sources = new HashMap<>();
			if(trigger.getSourceFilteringEnum() != SourceFiltering.NONE) {
				for(Iterator<String> it1 = trigger.getAdapterFilters().keySet().iterator(); it1.hasNext();) {
					String adapterName = it1.next();

					AdapterFilter af = trigger.getAdapterFilters().get(adapterName);
					sources.put(adapterName, af.getSubObjectList());
				}
			}
			triggerMap.put("filter", trigger.getSourceFiltering());
			triggerMap.put("sources", sources);
		}
		return triggerMap;
	}

	@GET
	@RolesAllowed({ "IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/{monitorName}")
	@Produces()
	public Response getMonitor(@PathParam("configuration") String configName, @PathParam("monitorName") String monitorName, @QueryParam("xml") boolean showConfigXml) throws ApiException {

		MonitorManager mm = getMonitorManager(configName);
		Monitor monitor = mm.findMonitor(monitorName);

		if(monitor == null) {
			throw new ApiException("Monitor not found!", Status.NOT_FOUND);
		}

		if(showConfigXml) {
			String xml = monitor.toXml().toXML();
			return Response.status(Status.OK).type(MediaType.APPLICATION_XML).entity(xml).build();
		}

		Map<String, Object> monitorInfo = mapMonitor(monitor);// Calculate the ETag on last modified date of user resource
		EntityTag etag = new EntityTag(monitorInfo.hashCode() + "");

		Response.ResponseBuilder response = null;
		// Verify if it matched with etag available in http request
		response = request.evaluatePreconditions(etag);

		// If ETag matches the response will be non-null;
		if(response != null) {
			return response.tag(etag).build();
		}

		return Response.status(Status.OK).type(MediaType.APPLICATION_JSON).entity(monitorInfo).tag(etag).build();
	}

	@PUT
	@RolesAllowed({ "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/{monitorName}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateMonitor(@PathParam("configuration") String configName, @PathParam("monitorName") String monitorName, LinkedHashMap<String, Object> json) {

		MonitorManager mm = getMonitorManager(configName);
		Monitor monitor = mm.findMonitor(monitorName);

		if(monitor == null) {
			throw new ApiException("Monitor not found!", Status.NOT_FOUND);
		}

		String action = null;
		for(Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			if(key.equalsIgnoreCase("action")) {
				action = entry.getValue().toString();
			}
		}

		if(StringUtils.isEmpty(action)) {
			throw new ApiException("Missing query parameter [action]", Status.BAD_REQUEST);
		} else if(action.equals("clear")) {
			try {
				log.info("clearing monitor [" + monitor.getName() + "]");
				monitor.changeState(new Date(), false, Severity.WARNING, null, null, null);
			} catch (MonitorException e) {
				throw new ApiException("Failed to change monitor state", e);
			}
		} else if(action.equals("raise")) {
			try {
				log.info("raising monitor [" + monitor.getName() + "]");
				monitor.changeState(new Date(), true, Severity.WARNING, null, null, null);
			} catch (MonitorException e) {
				throw new ApiException("Failed to change monitor state", e);
			}
		} else if(action.equals("edit")) {
			for(Entry<String, Object> entry : json.entrySet()) {
				String key = entry.getKey();
				if(key.equalsIgnoreCase("name")) {
					monitor.setName(entry.getValue().toString());
				} else if(key.equalsIgnoreCase("type")) {
					monitor.setType(entry.getValue().toString());
				} else if(key.equalsIgnoreCase("destinations")) {
					monitor.setDestinationSet(parseDestinations(entry.getValue()));
				}
			}
		}

		return Response.status(Status.ACCEPTED).build();
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
	public Response deleteMonitor(@PathParam("configuration") String configurationName, @PathParam("monitorName") String monitorName) throws ApiException {

		MonitorManager mm = getMonitorManager(configurationName);
		Monitor monitor = mm.findMonitor(monitorName);

		if(monitor == null) {
			throw new ApiException("Monitor not found!", Status.NOT_FOUND);
		}

		log.info("removing monitor [" + monitor.getName() + "]");

		mm.removeMonitor(monitor);
		return Response.status(Status.OK).build();
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
	@Path("/{monitorName}/triggers")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTriggers(@PathParam("configuration") String configName, @PathParam("monitorName") String monitorName) throws ApiException {
		return getTriggers(configName, monitorName, null);
	}

	@GET
	@RolesAllowed({ "IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/{monitorName}/triggers/{triggerId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTriggers(@PathParam("configuration") String configName, @PathParam("monitorName") String monitorName, @PathParam("triggerId") Integer id) throws ApiException {

		MonitorManager mm = getMonitorManager(configName);
		Monitor monitor = mm.findMonitor(monitorName);

		if(monitor == null) {
			throw new ApiException("Monitor not found!", Status.NOT_FOUND);
		}

		Map<String, Object> returnMap = new HashMap<>();

		if(id != null) {
			ITrigger trigger = monitor.getTrigger(id);
			if(trigger == null) {
				throw new ApiException("Trigger not found!", Status.NOT_FOUND);
			} else {
				returnMap.put("trigger", mapTrigger(trigger));
			}
		}

		returnMap.put("severities", EnumUtils.getEnumList(Severity.class));
		returnMap.put("events", mm.getEvents());

		EntityTag etag = new EntityTag(returnMap.hashCode() + "");

		Response.ResponseBuilder response = null;
		// Verify if it matched with etag available in http request
		response = request.evaluatePreconditions(etag);

		// If ETag matches the response will be non-null;
		if(response != null) {
			return response.tag(etag).build();
		}

		return Response.status(Status.OK).entity(returnMap).tag(etag).build();
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
		String filter = null;
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
				filter = entry.getValue().toString();
			} else if(key.equalsIgnoreCase("adapters") && entry.getValue() instanceof List<?>) {
				adapters = (List<String>) entry.getValue();
			} else if(key.equalsIgnoreCase("sources") && entry.getValue() instanceof Map<?, ?>) {
				sources = (Map<String, List<String>>) entry.getValue();
			}
		}

		// If no parse errors have occured we can continue!
		trigger.setEventCodes(eventList.toArray(new String[eventList.size()]));
		trigger.setTriggerType(type);
		trigger.setSeverityEnum(severity);
		trigger.setThreshold(threshold);
		trigger.setPeriod(period);

		trigger.clearAdapterFilters();
		if("adapter".equals(filter)) {
			trigger.setSourceFilteringEnum(SourceFiltering.ADAPTER);

			for(String adapter : adapters) {
				AdapterFilter adapterFilter = new AdapterFilter();
				adapterFilter.setAdapter(adapter);
				trigger.registerAdapterFilter(adapterFilter);
			}
		} else if("source".equals(filter)) {
			trigger.setSourceFilteringEnum(SourceFiltering.SOURCE);

			for(Map.Entry<String, List<String>> entry : sources.entrySet()) {
				AdapterFilter adapterFilter = new AdapterFilter();
				adapterFilter.setAdapter(entry.getKey());
				for(String subObject : entry.getValue()) {
					adapterFilter.registerSubObject(subObject);
				}
				trigger.registerAdapterFilter(adapterFilter);
			}
		} else {
			trigger.setSourceFilteringEnum(SourceFiltering.NONE);
		}
	}

	@DELETE
	@RolesAllowed({ "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/{monitorName}/triggers/{trigger}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteTrigger(@PathParam("configuration") String configurationName, @PathParam("monitorName") String monitorName, @PathParam("trigger") int index) throws ApiException {

		MonitorManager mm = getMonitorManager(configurationName);
		Monitor monitor = mm.findMonitor(monitorName);

		if(monitor == null) {
			throw new ApiException("Monitor not found!", Status.NOT_FOUND);
		}

		ITrigger trigger = monitor.getTrigger(index);

		if(trigger == null) {
			throw new ApiException("Trigger not found!", Status.NOT_FOUND);
		}

		log.info("removing trigger [" + trigger + "]");
		monitor.removeTrigger(trigger);

		return Response.status(Status.OK).build();
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
		monitor.setTypeEnum(type);
		monitor.setDestinationSet(destinations);

		mm.addMonitor(monitor);

		return Response.status(Response.Status.CREATED).build();
	}
}

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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletConfig;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import nl.nn.adapterframework.monitoring.AdapterFilter;
import nl.nn.adapterframework.monitoring.EventThrowing;
import nl.nn.adapterframework.monitoring.EventTypeEnum;
import nl.nn.adapterframework.monitoring.Monitor;
import nl.nn.adapterframework.monitoring.MonitorException;
import nl.nn.adapterframework.monitoring.MonitorManager;
import nl.nn.adapterframework.monitoring.SeverityEnum;
import nl.nn.adapterframework.monitoring.Trigger;

/**
* Shows all monitors.
* 
* @author	Niels Meijer
*/

@Path("/")
public final class ShowMonitors extends Base {
	@Context ServletConfig servletConfig;

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/monitors")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMonitors() throws ApiException {
		initBase(servletConfig);

		Map<String, Object> returnMap = new HashMap<String, Object>();
		MonitorManager mm = MonitorManager.getInstance();

		List<Map<String, Object>> monitors = new ArrayList<Map<String, Object>>();
		for (int i=0;i<mm.getMonitors().size();i++) {
			Map<String, Object> monitorMap = new HashMap<String, Object>();
			Monitor monitor = mm.getMonitor(i);

			monitorMap.put("name", monitor.getName());
			monitorMap.put("type", monitor.getType());
			monitorMap.put("lastHit", monitor.getLastHit());

			monitorMap.put("raised", monitor.isRaised());
			monitorMap.put("changed", monitor.getStateChangeDt());
			monitorMap.put("hits", monitor.getAdditionalHitCount());
			String eventSource = null;
			EventThrowing source = monitor.getAlarmSource();
			if (source!=null) {
				eventSource = source.getEventSourceName();
			}
			monitorMap.put("source", eventSource);
			monitorMap.put("severity", monitor.getAlarmSeverity());

			List<Map<String, Object>> triggers = new ArrayList<Map<String, Object>>();
			for (Iterator<Trigger> it=monitor.getTriggers().iterator();it.hasNext();) {
				Trigger trigger=(Trigger)it.next();
				Map<String, Object> triggerMap = new HashMap<String, Object>();

				triggerMap.put("type", trigger.getType());
				triggerMap.put("eventCodes", trigger.getEventCodes());
				triggerMap.put("sources", trigger.getSourceList());
				triggerMap.put("severity", trigger.getSeverity());
				triggerMap.put("threshold", trigger.getThreshold());
				triggerMap.put("period", trigger.getPeriod());
				
				if (trigger.getAdapterFilters()!=null) {
					List<Map<String, Object>> filters = new ArrayList<Map<String, Object>>();
					if (trigger.getSourceFiltering()!=Trigger.SOURCE_FILTERING_NONE) {
						for (Iterator<String> it1=trigger.getAdapterFilters().keySet().iterator(); it1.hasNext(); ) {
							Map<String, Object> adapter = new HashMap<String, Object>(2);

							String adapterName = it1.next();
							AdapterFilter af = (AdapterFilter) trigger.getAdapterFilters().get(adapterName);
							adapter.put("name", adapterName);
							if (trigger.isFilterOnLowerLevelObjects()) {
								adapter.put("sources", af.getSubObjectList());
							}
							filters.add(adapter);
						}
					}
					triggerMap.put("filterExclusive",trigger.isFilterExclusive());
					triggerMap.put("filters", filters);
				}
				
				triggers.add(triggerMap);
			}
			monitorMap.put("triggers", triggers);

			List<String> destinations=new ArrayList<String>();
			Set<String> d = monitor.getDestinationSet();
			for (Iterator<String> it=d.iterator();it.hasNext();) {
				destinations.add(it.next());
			}
			monitorMap.put("destinations", destinations);

			monitors.add(monitorMap);
		}

		returnMap.put("monitors", monitors);
		returnMap.put("enabled", new Boolean(mm.isEnabled()));
		returnMap.put("severities", SeverityEnum.getNames());
		returnMap.put("eventTypes", EventTypeEnum.getNames());
		returnMap.put("destinations", mm.getDestinations().keySet());

		return Response.status(Response.Status.OK).entity(returnMap).build();
	}

	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/monitors/{monitorName}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response raiseMonitor(@PathParam("monitorName") String monitorName, @QueryParam("action") String action) throws ApiException {
		initBase(servletConfig);

		MonitorManager mm = MonitorManager.getInstance();
		Monitor monitor = mm.findMonitor(monitorName);

		if(monitor == null) {
			throw new ApiException("Monitor not found!");
		}

		if (action.equals("clearMonitor")) {
			try {
				log.info("clearing monitor ["+monitor.getName()+"]");
				monitor.changeState(new Date(),false,SeverityEnum.WARNING,null,null,null);
			} catch (MonitorException e) {
				throw new ApiException("Failed to change monitor state!");
			}
		}
		
		if (action.equals("raiseMonitor")) {
			try {
				log.info("raising monitor ["+monitor.getName()+"]");
				monitor.changeState(new Date(), true, SeverityEnum.WARNING, null, null, null);
			} catch (MonitorException e) {
				throw new ApiException("Failed to change monitor state!");
			}
		}

		return Response.status(Response.Status.OK).build();
	}

	@DELETE
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/monitors/{monitorName}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteMonitor(@PathParam("monitorName") String monitorName) throws ApiException {
		initBase(servletConfig);

		MonitorManager mm = MonitorManager.getInstance();
		Monitor monitor = mm.findMonitor(monitorName);

		if(monitor == null) {
			throw new ApiException("Monitor not found!");
		}

		if (monitor!=null) {
			int index = mm.getMonitors().indexOf(monitor);
			log.info("removing monitor nr ["+index+"] name ["+monitor.getName()+"]");
			mm.removeMonitor(index);
		}

		return Response.status(Response.Status.OK).build();
	}

	@SuppressWarnings("unchecked")
	@POST
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/monitors")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response addMonitor(LinkedHashMap<String, Object> json) throws ApiException {
		initBase(servletConfig);

		String name = null;
		EventTypeEnum type = null;
		ArrayList<String> destinations = new ArrayList<String>();

		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			if(key.equalsIgnoreCase("name")) {
				name = entry.getValue().toString();
			}
			if(key.equalsIgnoreCase("type")) {
				type = EventTypeEnum.getEnum(entry.getValue().toString());
			}
			if(key.equalsIgnoreCase("destinations")) {
				try {
					destinations.addAll((ArrayList<String>) entry.getValue());
				}
				catch(Exception e) {
					throw new ApiException("Failed to parse destinations!");
				}
			}
		}
		if(name == null)
			throw new ApiException("Name not set!");
		if(type == null)
			throw new ApiException("Type not set!");

		MonitorManager mm = MonitorManager.getInstance();

		Monitor monitor = new Monitor();
		monitor.setName(name);
		monitor.setTypeEnum(type);

		//Check if destination is set, and if it actually exists...
		if(destinations != null && destinations.size() > 0) {
			List<String> tmp = new ArrayList<String>();

			for (Iterator<String> i = destinations.iterator(); i.hasNext();) {
				String destination = (String) i.next();
				if(mm.getDestination(destination) != null) {
					tmp.add(destination);
				}
			}

			String[] result=new String[tmp.size()];
			result = (String[])tmp.toArray(result);
			monitor.setDestinations(result);
		}

		mm.addMonitor(monitor);

		return Response.status(Response.Status.CREATED).build();
	}
}

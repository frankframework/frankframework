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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.http.RestListener;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.soap.Wsdl;

/**
* Shows all monitors.
* 
* @author	Niels Meijer
*/

@Path("/")
public final class Webservices extends Base {
	@Context ServletConfig servletConfig;

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/webservices")
	@Relation("webservices")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLogDirectory() throws ApiException {
		initBase(servletConfig);

		Map<String, Object> returnMap = new HashMap<String, Object>();

		List<Map<String, Object>> webServices = new ArrayList<Map<String, Object>>();
		for (IAdapter a : ibisManager.getRegisteredAdapters()) {
			Adapter adapter = (Adapter) a;
			Iterator<IReceiver> recIt = adapter.getReceiverIterator();
			while (recIt.hasNext()) {
				IReceiver receiver = recIt.next();
				if (receiver instanceof ReceiverBase) {
					ReceiverBase rb = (ReceiverBase) receiver;
					IListener listener = rb.getListener();
					if (listener instanceof RestListener) {
						RestListener rl = (RestListener) listener;
						if (rl.isView()) {
							Map<String, Object> service = new HashMap<String, Object>(2);
							service.put("name", rb.getName());
							service.put("uriPattern", rl.getUriPattern());
							webServices.add(service);
						}
					}
				}
			}
		}
		returnMap.put("services", webServices);

		List<Map<String, Object>> wsdls = new ArrayList<Map<String, Object>>();
		for (IAdapter a : ibisManager.getRegisteredAdapters()) {
			Map<String, Object> wsdlMap = new HashMap<String, Object>(2);
			try {
				Adapter adapter = (Adapter) a;
				Wsdl wsdl = new Wsdl(adapter.getPipeLine());
				wsdlMap.put("name", wsdl.getName());
				wsdlMap.put("extention", getWsdlExtention());
			} catch (Exception e) {
				wsdlMap.put("name", a.getName());

				if (e.getMessage() != null) {
					wsdlMap.put("error", e.getMessage());
				} else {
					wsdlMap.put("error", e.toString());
				}
			}
			wsdls.add(wsdlMap);
		}
		returnMap.put("wsdls", wsdls);

		return Response.status(Response.Status.OK).entity(returnMap).build();
	}

	private String getWsdlExtention() {
		return ".wsdl";
	}
}

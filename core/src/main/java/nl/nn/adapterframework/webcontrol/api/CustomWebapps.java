/*
Copyright 2016-2017 Integration Partners B.V.

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
 * GET Endpoint used to retrieve all registered custom web app extensions.
 * 
 * @author	Laurens Mäkel
 */

@Path("/")
public final class CustomWebapps extends Base {
	@Context ServletConfig servletConfig;

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/webapps")
	@Relation("webapps")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getWebApps() throws ApiException {
		initBase(servletConfig);

		Map<String, Object> returnMap = new HashMap<String, Object>();

		List<Map<String, Object>> webapps = new ArrayList<Map<String, Object>>();
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
						if (rl.isView() && rl.isCustomWebapp()) {
							Map<String, Object> webapp = new HashMap<String, Object>(2);
							webapp.put("name", rb.getName());
							webapp.put("uriPattern", rl.getUriPattern());
							webapp.put("showInMenu", rl.isVisibleInMenu());
							webapp.put("showInWebservices", rl.isVisibleInWebservices());
                            webapps.add(webapp);
						}
					}
				}
            }
        }

        returnMap.put("webapps", webapps);

		return Response.status(Response.Status.OK).entity(returnMap).build();
	}
}

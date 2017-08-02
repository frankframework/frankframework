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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.core.ResourceInvoker;
import org.jboss.resteasy.core.ResourceMethodRegistry;

import nl.nn.adapterframework.util.AppConstants;

/**
 * Root collection for API.
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public class Init extends Base {

	@Context Dispatcher dispatcher;
	@Context HttpServletRequest httpServletRequest;
	@GET
	@PermitAll
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllResources(@QueryParam("allowedRoles") boolean displayAllowedRoles){
		List<Object> resources = new ArrayList<Object>();

		ResourceMethodRegistry registry = (ResourceMethodRegistry) dispatcher.getRegistry();

		StringBuffer requestPath = httpServletRequest.getRequestURL();
		if(requestPath.substring(requestPath.length()-1).equals("/"))
			requestPath.setLength(requestPath.length()-1);

		for (Map.Entry<String, List<ResourceInvoker>> entry : registry.getBounded().entrySet()) {
			for (ResourceInvoker invoker : entry.getValue()) {
				Method method = invoker.getMethod();

				if(method.getDeclaringClass() == getClass()) {
					continue;
				}
				if(method.getDeclaringClass().getName().endsWith("ShowMonitors") && 
					!AppConstants.getInstance().getBoolean("monitoring.enabled", false)) {
					continue;
				}

				Map<String, Object> resource = new HashMap<String, Object>(4);

				if(method.isAnnotationPresent(GET.class))
					resource.put("type", "GET");
				else if(method.isAnnotationPresent(POST.class))
					resource.put("type", "POST");
				else if(method.isAnnotationPresent(PUT.class))
					resource.put("type", "PUT");
				else if(method.isAnnotationPresent(DELETE.class))
					resource.put("type", "DELETE");

				if(method.isAnnotationPresent(Relation.class))
					resource.put("rel", method.getAnnotation(Relation.class).value());

				Path path = method.getAnnotation(Path.class);
				if(path != null) {
					String p = path.value();
					if(!p.startsWith("/")) p = "/" + p;
					resource.put("href", requestPath + p);
				}

				RolesAllowed rolesAllowed = method.getAnnotation(RolesAllowed.class);
				if(rolesAllowed != null && displayAllowedRoles) {
					resource.put("allowed", rolesAllowed.value());
				}

				resources.add(resource);
			}
		}
		Map<String, Object> ret = new HashMap<String, Object>(1);
		ret.put("links", resources);

		return Response.status(Response.Status.CREATED).entity(ret).build();
	}
}

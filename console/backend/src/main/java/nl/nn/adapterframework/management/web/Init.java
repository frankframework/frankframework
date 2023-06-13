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
package nl.nn.adapterframework.management.web;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.MethodDispatcher;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;

/**
 * Root collection for API.
 *
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public class Init extends FrankApiBase {

	private String getHATEOASImplementation() {
		return getProperty("ibis-api.hateoasImplementation", "default");
	}

	private boolean isMonitoringEnabled() {
		return getProperty("monitoring.enabled", false);
	}

	@GET
	@PermitAll
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllResources(@QueryParam("allowedRoles") boolean displayAllowedRoles) {
		List<Object> JSONresources = new ArrayList<>();
		Map<String, Object> HALresources = new HashMap<>();
		Map<String, Object> resources = new HashMap<>(1);

		StringBuffer requestPath = getServletRequest().getRequestURL();
		if(requestPath.substring(requestPath.length()-1).equals("/"))
			requestPath.setLength(requestPath.length()-1);

		for (ClassResourceInfo cri : getJAXRSService().getClassResourceInfo()) {
			MethodDispatcher methods = cri.getMethodDispatcher();
			for (OperationResourceInfo operation : methods.getOperationResourceInfos()) {
				Method method = operation.getMethodToInvoke();
				String relation = null;

				if(method.getDeclaringClass() == getClass()) {
					continue;
				}
				if(method.getDeclaringClass().getName().endsWith("ShowMonitors") && !isMonitoringEnabled()) {
					continue;
				}

				Map<String, Object> resource = new HashMap<>(4);

				if(method.isAnnotationPresent(GET.class))
					resource.put("type", "GET");
				else if(method.isAnnotationPresent(POST.class))
					resource.put("type", "POST");
				else if(method.isAnnotationPresent(PUT.class))
					resource.put("type", "PUT");
				else if(method.isAnnotationPresent(DELETE.class))
					resource.put("type", "DELETE");

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


				if(("hal".equalsIgnoreCase(getHATEOASImplementation()))) {
					if(method.isAnnotationPresent(Relation.class))
						relation = method.getAnnotation(Relation.class).value();

					if(relation != null) {
						if(HALresources.containsKey(relation)) {
							Object prevRelation = HALresources.get(relation);
							List<Object> tmpList = null;
							if(prevRelation instanceof List)
								tmpList = (List) prevRelation;
							else {
								tmpList = new ArrayList<>();
								tmpList.add(prevRelation);
							}

							tmpList.add(resource);
							HALresources.put(relation, tmpList);
						}
						else
							HALresources.put(relation, resource);
					}
				}
				else {
					if(method.isAnnotationPresent(Relation.class))
						resource.put("rel", method.getAnnotation(Relation.class).value());

					JSONresources.add(resource);
				}
			}
		}

		if("hal".equalsIgnoreCase(getHATEOASImplementation()))
			resources.put("_links", HALresources);
		else
			resources.put("links", JSONresources);

		return Response.status(Response.Status.CREATED).entity(resources).build();
	}
}

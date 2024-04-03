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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
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

	private boolean isMonitoringEnabled() {
		return getProperty("monitoring.enabled", false);
	}

	@GET
	@PermitAll
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllResources(@QueryParam("allowedRoles") boolean displayAllowedRoles, @QueryParam("hateoas") @DefaultValue("default") String hateoasImpl) {
		List<Object> JSONresources = new ArrayList<>();
		Map<String, Object> HALresources = new HashMap<>();
		Map<String, Object> resources = new HashMap<>(1);

		String requestPath = getServletRequest().getRequestURL().toString();
		if(requestPath.endsWith("/")) {
			requestPath = requestPath.substring(0, requestPath.length()-1);
		}

		for (ClassResourceInfo cri : getJAXRSService().getClassResourceInfo()) {
			MethodDispatcher methods = cri.getMethodDispatcher();
			Path basePathAnnotation = cri.getPath();
			final String basePath = basePathAnnotation != null ? basePathAnnotation.value() : "/";
			for (OperationResourceInfo operation : methods.getOperationResourceInfos()) {
				Method method = operation.getMethodToInvoke();
				String relation = null;

				if(method.getDeclaringClass() == getClass()) {
					continue;
				}
				if(method.getDeclaringClass().getName().endsWith("ShowMonitors") && !isMonitoringEnabled()) {
					continue;
				}
				boolean deprecated = method.getAnnotation(Deprecated.class) != null;

				Map<String, Object> resource = new HashMap<>(6);
				resource.put("name", method.getName());

				if(deprecated) {
					if(!allowDeprecatedEndpoints()) continue; // Skip all

					resource.put("deprecated", true);
				}

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
					resource.put("href", computePath(requestPath, basePath, path.value()));
				}

				RolesAllowed rolesAllowed = method.getAnnotation(RolesAllowed.class);
				if(rolesAllowed != null && displayAllowedRoles) {
					resource.put("roles", rolesAllowed.value());
				}
				Description description = method.getAnnotation(Description.class);
				if(description != null) {
					resource.put("description", description.value());
				}

				if("hal".equalsIgnoreCase(hateoasImpl)) {
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

		if("hal".equalsIgnoreCase(hateoasImpl))
			resources.put("_links", HALresources);
		else
			resources.put("links", JSONresources);

		return Response.status(Response.Status.CREATED).entity(resources).build();
	}

	/**
	 * The basepath is usually a '/', but path may also start with a slash.
	 * Ensure a valid path is returned without double slashes.
	 */
	private static String computePath(String requestPath, String basePath, String path) {
		StringBuilder pathToUse = new StringBuilder(requestPath);
		if(!basePath.startsWith("/")) {
			pathToUse.append("/");
		}
		pathToUse.append(basePath);
		pathToUse.append( basePath.endsWith("/") && path.startsWith("/") ? path.substring(1) : path);
		return pathToUse.toString();
	}
}

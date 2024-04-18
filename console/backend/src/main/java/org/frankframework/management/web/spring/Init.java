package org.frankframework.management.web.spring;

import org.frankframework.management.web.Description;
import org.frankframework.management.web.Relation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class Init extends FrankApiBase{

	private boolean isMonitoringEnabled() {
		return getProperty("monitoring.enabled", false);
	}

	@GetMapping(value = "/", produces = "application/json")
	@PermitAll
	public Response getAllResources(@RequestParam("allowedRoles") boolean displayAllowedRoles, @RequestParam(value = "hateoas", defaultValue = "default") String hateoasImpl) {
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
		pathToUse.append((basePath.endsWith("/") && path.startsWith("/")) ? path.substring(1) : path);
		return pathToUse.toString();
	}

}
